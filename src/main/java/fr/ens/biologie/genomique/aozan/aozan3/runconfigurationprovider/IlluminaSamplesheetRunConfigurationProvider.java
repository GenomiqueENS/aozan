package fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider;

import static fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger.newAozanLogger;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataType.SequencingTechnology;
import fr.ens.biologie.genomique.aozan.aozan3.IlluminaRunIdWrapper;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.illumina.DemultiplexingStats;
import fr.ens.biologie.genomique.kenetre.illumina.RunInfo;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetCheck;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetReader;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetXLSReader;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetXLSXReader;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * Get the run configuration from an Illumina samplesheet.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IlluminaSamplesheetRunConfigurationProvider
    implements RunConfigurationProvider {

  public static final String PROVIDER_NAME = "illumina_samplesheet";

  private static final String DEFAULT_SAMPLESHEET_PREFIX = "design";
  private static final String DEFAULT_SAMPLESHEET_FORMAT = "xls";

  private enum SamplesheetFormat {
    CSV, XLS, XLSX;

    String getExtension() {

      switch (this) {
      case CSV:
        return ".csv";
      case XLS:
        return ".xls";
      case XLSX:
        return ".xlsx";

      default:
        throw new IllegalStateException();
      }
    }

    SampleSheetReader getReader(Path path) throws IOException {

      Objects.requireNonNull(path);
      File file = path.toFile();

      switch (this) {
      case CSV:
        return new SampleSheetCSVReader(file);

      case XLS:
        return new SampleSheetXLSReader(file);

      case XLSX:
        return new SampleSheetXLSXReader(file);

      default:
        throw new IllegalStateException();
      }
    }

    static SamplesheetFormat parse(String format) {

      requireNonNull(format);

      switch (format) {
      case "csv":
        return CSV;
      case "xls":
        return XLS;
      case "xlsx":
        return XLSX;

      default:
        throw new IllegalArgumentException(format);
      }

    }

  };

  private Aozan3Logger logger;

  private Path samplesheetsPath;
  private String samplesheetPrefix;
  private final Map<String, String> indexSequences = new HashMap<>();

  private SamplesheetFormat sampleSheetFormat;
  private String samplesheetCreationCommand;
  private boolean allowUnderscoresInSampleIds;
  private boolean searchInRunDirectoryFirst;
  private boolean initialized;

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public void init(Configuration conf, GenericLogger logger)
      throws Aozan3Exception {

    requireNonNull(conf);

    // Check if object has not been already initialized
    if (this.initialized) {
      throw new IllegalStateException();
    }

    // Set logger
    if (logger != null) {
      this.logger = newAozanLogger(logger);
    }

    this.samplesheetsPath = conf.getPath("samplesheet.path");
    this.sampleSheetFormat = SamplesheetFormat
        .parse(conf.get("samplesheet.format", DEFAULT_SAMPLESHEET_FORMAT));
    this.samplesheetPrefix =
        conf.get("samplesheet.prefix.filename", DEFAULT_SAMPLESHEET_PREFIX);
    this.samplesheetCreationCommand =
        conf.get("samplesheet.generator.command", "");
    this.allowUnderscoresInSampleIds =
        conf.getBoolean("samplesheet.allow.underscores.in.sample.ids", false);
    this.searchInRunDirectoryFirst =
        conf.getBoolean("samplesheet.search.in.run.dir.first", false);

    // Load index sequences
    if (conf.containsKey("index.sequences")) {
      loadIndexSequences(conf.getPath("index.sequences"));
    }

    this.initialized = true;
  }

  @Override
  public RunConfiguration getRunConfiguration(final RunData runData)
      throws Aozan3Exception {

    requireNonNull(runData);

    // Check if object has been initialized
    if (!this.initialized) {
      throw new IllegalStateException();
    }

    RunConfiguration result = new RunConfiguration();
    RunId runId = runData.getRunId();

    if (!SequencingTechnology.ILLUMINA
        .equals(runData.getType().getSequencingTechnology())) {
      throw new IllegalArgumentException(
          "Excepting an Illumina run data: " + runId);
    }

    // Get run information
    IlluminaRunIdWrapper illuminaRunId = new IlluminaRunIdWrapper(runId);
    int runNumber = illuminaRunId.getRunNumber();
    String instrumentNumber = illuminaRunId.getInstrumentSerialNumber();

    String samplesheetFilename = String.format("%s_%s_%04d",
        this.samplesheetPrefix, instrumentNumber, runNumber);

    // Load samplesheet
    SampleSheet samplesheet = loadSamplesheet(runData, samplesheetFilename,
        this.searchInRunDirectoryFirst);

    // Get the number of mismatches if defined in samplesheet
    int mismatches = getBcl2fastqMismatches(samplesheet, runId);
    if (mismatches != -1) {
      this.logger.info(runData.getRunId(),
          "Custom number of allowed mismatches in demultiplexing: "
              + mismatches);
      result.set("illumina.demux.allowed.mismatches", mismatches);
    }

    // Update samplesheet
    this.logger.info(runData.getRunId(), "Update samplesheet");
    updateSamplesheet(samplesheet, runId, getFlowCellLaneCount(runData),
        this.allowUnderscoresInSampleIds);

    // Check samplesheet
    checkSamplesheet(samplesheet, runId, illuminaRunId.getFlowCellId());

    // Save samplesheet in Run configuration
    result.set("illumina.samplesheet",
        SampleSheetUtils.toSampleSheetV2CSV(samplesheet));

    return result;
  }

  //
  // RunInfo and SampleSheet loading methods
  //

  private int getFlowCellLaneCount(final RunData runData)
      throws Aozan3Exception {

    RunId runId = runData.getRunId();
    File dataDir = runData.getLocation().getPath().toFile();

    // Raw data
    File runInfoFile = new File(dataDir, "RunInfo.xml");
    if (runInfoFile.exists()) {
      return getFlowCellLaneCountFromRunInfo(runInfoFile, runId);
    }

    // BCLConvert
    runInfoFile = new File(dataDir, "Reports/RunInfo.xml");
    if (runInfoFile.exists()) {
      return getFlowCellLaneCountFromRunInfo(runInfoFile, runId);
    }

    // Bcl2fastq 2
    File demuxStatFile = new File(dataDir, "Stats/DemultiplexingStats.xml");
    if (demuxStatFile.exists()) {

      try {
        DemultiplexingStats demuxStats = new DemultiplexingStats(demuxStatFile);

        int laneCount = -1;
        for (DemultiplexingStats.Entry e : demuxStats.entries()) {
          laneCount = Math.max(laneCount, e.getLaneNumber());
        }

        if (laneCount > 0) {
          return laneCount;
        }

      } catch (IOException e) {
        this.logger.error(runId,
            "Unable to load DemultiplexingStats.xml file: " + runInfoFile);
        throw new Aozan3Exception(
            "Unable to load DemultiplexingStats.xml file: " + runInfoFile, e);
      }
    }

    throw new Aozan3Exception(
        "Unable to find flowcell lane count for run: " + runId);
  }

  private int getFlowCellLaneCountFromRunInfo(final File runInfoFile,
      RunId runId) throws Aozan3Exception {

    try {
      return RunInfo.parse(runInfoFile).getFlowCellLaneCount();
    } catch (ParserConfigurationException | SAXException | IOException e) {

      this.logger.error(runId,
          "Unable to load RunInfo.xml file: " + runInfoFile);
      throw new Aozan3Exception(
          "Unable to load RunInfo.xml file: " + runInfoFile, e);
    }
  }

  private SampleSheet loadSamplesheet(final RunData runData,
      final String samplesheetFilename, boolean searchInRunDirFirst)
      throws Aozan3Exception {

    requireNonNull(runData);
    requireNonNull(samplesheetFilename);

    if (searchInRunDirFirst) {

      SampleSheet result = sampleSheetInRunDirectory(runData);
      if (result != null) {
        return result;
      }
    }

    RunId runId = runData.getRunId();
    Path samplesheetFile = Paths.get(samplesheetsPath.toString(),
        samplesheetFilename + this.sampleSheetFormat.getExtension());

    // If a sample sheet exists a dedicated directory
    if (Files.isRegularFile(samplesheetFile)) {

      this.logger.info(runId,
          "Load a samplesheet in directory: " + this.samplesheetsPath);

      return loadSamplesheet(runId, samplesheetFile, this.sampleSheetFormat,
          this.logger.getLogger());
    }

    // Use an external command if defined
    if (this.samplesheetCreationCommand != null
        && !this.samplesheetCreationCommand.isEmpty()) {
      this.logger.info(runId,
          "Use an external command to retrieve samplehseet");
      return loadSampleSheetFromCommand(runId, this.samplesheetCreationCommand);
    }

    // Test if a samplesheet file exists in run directory
    SampleSheet result = sampleSheetInRunDirectory(runData);
    if (result != null) {
      return result;
    }

    throw new Aozan3Exception(runId, "No sample sheet found");
  }

  private SampleSheet sampleSheetInRunDirectory(final RunData runData)
      throws Aozan3Exception {

    requireNonNull(runData);
    RunId runId = runData.getRunId();

    Path samplesheetFileInRunDirectory =
        searchSamplesheetInRunDir(runData.getLocation().getPath(),
            Arrays.asList("samplesheet", this.samplesheetPrefix));

    // Test if a samplesheet file exists in run directory
    if (samplesheetFileInRunDirectory != null
        && Files.exists(samplesheetFileInRunDirectory)) {

      this.logger.info(runId, "Use existing "
          + samplesheetFileInRunDirectory + " file in run directory");
      return loadSamplesheet(runId, samplesheetFileInRunDirectory,
          SamplesheetFormat.CSV, this.logger.getLogger());
    }

    return null;
  }

  private static SampleSheet loadSamplesheet(final RunId runId,
      Path samplesheetFile, SamplesheetFormat format, GenericLogger logger)
      throws Aozan3Exception {

    Aozan3Logger.info(logger, runId,
        "Load samplesheet file: " + samplesheetFile);

    try (SampleSheetReader reader = format.getReader(samplesheetFile)) {
      return reader.read();
    } catch (IOException e) {
      throw new Aozan3Exception(runId, "Unable to load samplesheet", e);
    }

  }

  private static SampleSheet loadSampleSheetFromCommand(final RunId runId,
      final String command) throws Aozan3Exception {

    Objects.requireNonNull(command);

    ProcessBuilder pb = new ProcessBuilder(
        Arrays.asList("/bin/sh", "-c", command + " " + runId));

    try {
      Process p = pb.start();

      try (SampleSheetCSVReader reader =
          new SampleSheetCSVReader(p.getInputStream())) {
        return reader.read();
      }

    } catch (IOException e) {
      throw new Aozan3Exception(runId, "Unable to load samplesheet", e);
    }
  }

  private int getBcl2fastqMismatches(SampleSheet samplesheet, RunId runId)
      throws Aozan3Exception {

    int result = -1;

    if (!samplesheet.containsPropertySection("Settings")) {
      return result;
    }

    String value =
        samplesheet.getPropertySection("Settings").get("MismatchCount", "-1");

    try {
      result = Integer.parseInt(value.trim().replace(" ", ""));

    } catch (NumberFormatException e) {
      String errorMessage = "Error while reading Bcl2fastq samplesheet. "
          + "No number of allowed mismatches found in samplesheet: " + value;
      this.logger.error(runId, errorMessage);
      throw new Aozan3Exception(errorMessage);
    }

    return result;
  }

  /**
   * Load the map of the index sequences.
   * @throws Aozan3Exception if an error occurs while reading the file
   */
  private void loadIndexSequences(Path indexSequenceFile)
      throws Aozan3Exception {

    try {

      if (!Files.isRegularFile(indexSequenceFile)) {
        throw new IOException(
            "Cannot read index sequence file: " + indexSequenceFile);
      }

      Splitter splitter = Splitter.on('=').trimResults();
      for (String line : Files.readAllLines(indexSequenceFile)) {

        List<String> fields = splitter.splitToList(line);
        if (fields.size() == 2) {
          this.indexSequences.put(fields.get(0).toLowerCase(), fields.get(1));
        }

      }
    } catch (IOException e) {
      throw new Aozan3Exception(e);
    }

  }

  private void updateSamplesheet(SampleSheet samplesheet, RunId runId,
      int laneCount, boolean allowUnderscoresInSampleIds)
      throws Aozan3Exception {

    try {

      // Replace underscores by dashes
      if (!allowUnderscoresInSampleIds) {
        SampleSheetUtils.replaceUnderscoresByDashesInSampleIds(samplesheet);
      }

      // Replace index sequence shortcuts by sequences
      SampleSheetUtils.replaceIndexShortcutsBySequences(samplesheet,
          this.indexSequences);

    } catch (KenetreException e) {
      this.logger.error("Error while updating samplesheet for run " + runId);
      throw new Aozan3Exception(e);
    }

    // Set the lane field if does not set
    SampleSheetUtils.duplicateSamplesIfLaneFieldNotSet(samplesheet, laneCount);
  }

  private void checkSamplesheet(SampleSheet samplesheet, RunId runId,
      String flowCellId) throws Aozan3Exception {

    try {

      // Do nothing if there is no sample
      if (SampleSheetUtils.getCheckedDemuxTableSection(samplesheet)
          .size() == 0) {
        return;
      }

      // Check values of samplesheet file
      List<String> samplesheetWarnings =
          SampleSheetCheck.checkSampleSheet(samplesheet, flowCellId);

      this.logger.warn(runId, "Bcl2fastq samplesheet warnings: "
          + String.join(" ", samplesheetWarnings));
    } catch (KenetreException e) {
      throw new Aozan3Exception(e);
    }
  }

  private static Path searchSamplesheetInRunDir(Path runPath,
      List<String> prefixes) {

    File[] files = runPath.toFile().listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {

        for (String prefix : prefixes) {
          if (name.toLowerCase().startsWith(prefix) && name.endsWith(".csv")) {
            return true;
          }
        }

        return false;
      }
    });

    if (files == null || files.length == 0) {
      return null;
    }

    Arrays.sort(files, new Comparator<File>() {

      @Override
      public int compare(File o1, File o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });

    return files[0].toPath();
  }

}
