package fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.IlluminaRunIdWrapper;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.SequencingTechnology;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.illumina.RunInfo;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheetCheck;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheetUtils;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetReader;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetXLSReader;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetXLSXReader;

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

    SampleSheetReader getReader(Path path) throws FileNotFoundException {

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

  private AozanLogger logger;

  private Path samplesheetsPath;
  private String samplesheetPrefix;
  private final Map<String, String> indexSequences = new HashMap<>();

  private SamplesheetFormat sampleSheetFormat;
  private String samplesheetCreationCommand;
  private boolean initialized;

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public void init(Configuration conf, AozanLogger logger)
      throws Aozan3Exception {

    requireNonNull(conf);

    // Check if object has not been already initialized
    if (this.initialized) {
      throw new IllegalStateException();
    }

    // Set logger
    if (logger != null) {
      this.logger = logger;
    }

    this.samplesheetsPath = conf.getPath("samplesheet.path");
    this.sampleSheetFormat = SamplesheetFormat
        .parse(conf.get("samplesheet.format", DEFAULT_SAMPLESHEET_FORMAT));
    this.samplesheetPrefix =
        conf.get("samplesheet.prefix.filename", DEFAULT_SAMPLESHEET_PREFIX);
    this.samplesheetCreationCommand =
        conf.get("samplesheet.generator.command", "");

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
        .equals(runId.getSequencingTechnology())) {
      throw new IllegalArgumentException(
          "Excepting an Illumina run id: " + runId);
    }

    // Get run information
    IlluminaRunIdWrapper illuminaRunId = new IlluminaRunIdWrapper(runId);
    int runNumber = illuminaRunId.getRunNumber();
    String instrumentNumber = illuminaRunId.getInstrumentSerialNumber();

    // Load RunInfo object
    RunInfo runInfo = loadRunInfo(runData);

    String samplesheetFilename = String.format("%s_%s_%04d",
        this.samplesheetPrefix, instrumentNumber, runNumber);

    // Load samplesheet
    SampleSheet samplesheet = loadSamplesheet(runData, samplesheetFilename);

    // Get the number of mismatches if defined in samplesheet
    int mismatches = getBcl2fastqMismatches(samplesheet, runId);
    if (mismatches != -1) {
      this.logger.info(runData.getRunId(),
          "Custom number of allowed mismatches in demultiplexing: "
              + mismatches);
      result.set("bcl2fastq.allowed.mismatches", mismatches);
    }

    // Update samplesheet
    this.logger.info(runData.getRunId(), "Update samplesheet");
    updateSamplesheet(samplesheet, runId, runInfo.getFlowCellLaneCount());

    // Check samplesheet
    checkSamplesheet(samplesheet, runId, runInfo.getFlowCell());

    // Save samplesheet in Run configuration
    result.set("bcl2fastq.samplesheet",
        SampleSheetUtils.toSampleSheetV2CSV(samplesheet));

    return result;
  }

  //
  // RunInfo and SampleSheet loading methods
  //

  private RunInfo loadRunInfo(final RunData runData) throws Aozan3Exception {

    File runInfoFile =
        new File(runData.getLocation().getPath().toFile(), "RunInfo.xml");

    try {
      return RunInfo.parse(runInfoFile);
    } catch (ParserConfigurationException | SAXException | IOException e) {

      this.logger.error(runData.getRunId(),
          "Unable to load RunInfo.xml file: " + runInfoFile);
      throw new Aozan3Exception(
          "Unable to load RunInfo.xml file: " + runInfoFile, e);
    }
  }

  private SampleSheet loadSamplesheet(final RunData runData,
      final String samplesheetFilename) throws Aozan3Exception {

    requireNonNull(runData);
    requireNonNull(samplesheetFilename);

    RunId runId = runData.getRunId();

    // Path of the samplesheet if in run directory
    Path samplesheetFileInRunDirectory = Paths
        .get(runData.getLocation().getPath().toString(), "SampleSheet.csv");

    // Test if a samplesheet file exists in run directory
    if (Files.exists(samplesheetFileInRunDirectory)) {
      this.logger.info(runId,
          "Use existing SampleSheet.csv file in run directory");
      return loadSamplesheet(runId, samplesheetFileInRunDirectory.getParent(),
          "SampleSheet", SamplesheetFormat.CSV, this.logger);
    }

    if (this.samplesheetCreationCommand != null
        && !this.samplesheetCreationCommand.isEmpty()) {
      this.logger.info(runId,
          "Use an external command to retrieve samplehseet");
      return loadSampleSheetFromCommand(runId, this.samplesheetCreationCommand);
    }

    this.logger.info(runId,
        "Load a samplesheet in directory: " + this.samplesheetsPath);
    return loadSamplesheet(runId, this.samplesheetsPath, samplesheetFilename,
        this.sampleSheetFormat, this.logger);
  }

  private static SampleSheet loadSamplesheet(final RunId runId,
      final Path samplesheetsPath, String samplesheetFilename,
      SamplesheetFormat format, AozanLogger logger) throws Aozan3Exception {

    Path samplesheetFile = Paths.get(samplesheetsPath.toString(),
        samplesheetFilename + format.getExtension());
    logger.info(runId, "Load samplesheet file: " + samplesheetFile);

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

    List<String> list = samplesheet.getMetadata("Settings", "MismatchCount");

    if (list == null || list.size() == 0) {
      return -1;
    }

    int result = -1;

    for (String value : list) {

      try {
        result = Integer.parseInt(value.trim().replace(" ", ""));

      } catch (NumberFormatException e) {
        String errorMessage = "Error while reading Bcl2fastq samplesheet. "
            + "No number of allowed mismatches found in samplesheet: " + value;
        this.logger.error(runId, errorMessage);
        throw new Aozan3Exception(errorMessage);
      }
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
          this.indexSequences.put(fields.get(0), fields.get(1));
        }

      }
    } catch (IOException e) {
      throw new Aozan3Exception(e);
    }

  }

  private void updateSamplesheet(SampleSheet samplesheet, RunId runId,
      int laneCount) throws Aozan3Exception {

    // Replace index sequence shortcuts by sequences
    try {
      SampleSheetUtils.replaceIndexShortcutsBySequences(samplesheet,
          this.indexSequences);
    } catch (AozanException e) {
      this.logger.error("Error while updating samplesheet for run " + runId);
      throw new Aozan3Exception(e);
    }

    // Set the lane field if does not set
    SampleSheetUtils.duplicateSamplesIfLaneFieldNotSet(samplesheet, laneCount);
  }

  private void checkSamplesheet(SampleSheet samplesheet, RunId runId,
      String flowCellId) throws Aozan3Exception {

    try {
      // Check values of samplesheet file
      List<String> samplesheetWarnings =
          SampleSheetCheck.checkSampleSheet(samplesheet, flowCellId);

      this.logger.warn(runId, "Bcl2fastq samplesheet warnings: "
          + String.join(" ", samplesheetWarnings));
    } catch (AozanException e) {
      throw new Aozan3Exception(e);
    }
  }

}
