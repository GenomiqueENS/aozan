package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.aozan3.DataType.Category.RAW;
import static fr.ens.biologie.genomique.aozan.aozan3.DataType.SequencingTechnology.ILLUMINA;
import static fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger.info;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.sizeToHumanReadable;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataLocation;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.SequencerNames;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.CategoryDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.MultiDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.PartialDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.TechnologyDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.legacy.IndexGenerator;
import fr.ens.biologie.genomique.aozan.aozan3.util.Tar;
import fr.ens.biologie.genomique.kenetre.log.DummyLogger;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define an Illumina end run data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class EndIlluminaRunDataProcessor implements DataProcessor {

  public static final String PROCESSOR_NAME = "illumina_end_run";

  private static String SEQUENCER_LOG_PREFIX = "hiseq_log_";
  private static String REPORT_PREFIX = "report_";
  private static long GIGA = 1024 * 1024 * 1024;

  private GenericLogger logger = new DummyLogger();

  private DataStorage outputStorage;
  private String dataDescription;
  private boolean initialized;

  @Override
  public String getName() {
    return PROCESSOR_NAME;
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
      this.logger = logger;
    }

    final DataStorage outputStorage =
        DataStorage.deSerializeFromJson(conf.get("output.storage"));

    // Check if directory is writable
    if (!outputStorage.isWritable()) {
      throw new Aozan3Exception(
          "The output demultiplexing directory is not writable: "
              + outputStorage);
    }

    this.outputStorage = outputStorage;
    this.dataDescription = conf.get("data.description", "no description");

    this.initialized = true;
  }

  @Override
  public Set<DataTypeFilter> getInputRequirements() {

    DataTypeFilter filter = new MultiDataTypeFilter(
        new CategoryDataTypeFilter(RAW), new TechnologyDataTypeFilter(ILLUMINA),
        new PartialDataTypeFilter(false));

    return Collections.singleton(filter);
  }

  @Override
  public ProcessResult process(InputData inputData, RunConfiguration conf)
      throws Aozan3Exception {

    requireNonNull(inputData);

    RunData inputRunData = inputData.getTheOnlyElement();

    // Check if object has been initialized
    if (!this.initialized) {
      throw new IllegalStateException();
    }

    RunId runId = inputRunData.getRunId();
    DataLocation inputLocation = inputRunData.getLocation();
    Path inputPath = inputLocation.getPath();

    long endTime = runEndTime(inputLocation.getPath());

    // Sequencer name
    SequencerNames sequencerNames = new SequencerNames(conf);
    String sequencerName = sequencerNames.getIlluminaSequencerName(runId);
    if (sequencerName == null) {
      sequencerName = "unknown sequencer";
    }

    DataLocation outputLocation =
        this.outputStorage.newDataLocation(runId.getId());
    Path outputDir = outputLocation.getPath();

    try {

      // Check if input directory exists
      inputLocation.checkReadableDirectory("input synchronization");

      // Check if final output directory already exists
      outputLocation
          .checkIfNotExists(this.dataDescription + " output already exists");

      // Create output directory
      Files.createDirectories(outputLocation.getPath());

      //
      // Create sequencer log tar file
      //

      Path hiseqLogArchiveFile = Paths.get(outputDir.toString(),
          SEQUENCER_LOG_PREFIX + runId.getId() + ".tar.bz2");

      createTar(hiseqLogArchiveFile, inputPath, Arrays.asList("InterOp",
          "RunInfo.xml", "runParameters.xml", "RunParameters.xml", "*.csv"));

      //
      // Create sequencer report tar file
      //

      Path reportArchiveFile = Paths.get(outputDir.toString(),
          REPORT_PREFIX + runId.getId() + ".tar.bz2");

      createTar(reportArchiveFile, inputPath,
          Arrays.asList("Data/Status_Files", "Data/reports", "Data/Status.htm",
              "First_Base_Report.htm", "Config", "Recipe", "RTALogs",
              "RTAConfiguration.xml", "RunCompletionStatus.xml", "RTA3.cfg"));

      // Create index.html at run of run directory in legacy mode
      IndexGenerator.createIndexRun(outputLocation.getPath(), runId.getId(),
          Arrays.asList("hiseq.step"));

      // Log disk usage and disk free space
      long outputSize = outputLocation.getDiskUsage();
      long outputFreeSize = outputLocation.getStorage().getUsableSpace();
      info(this.logger, runId, "output disk free after demux: "
          + sizeToHumanReadable(outputFreeSize));
      info(this.logger, runId,
          "space used by demux: " + sizeToHumanReadable(outputSize));

      //
      // Create email
      //

      info(this.logger, inputRunData,
          "Ending run detection "
              + runId.getId() + " on "
              + sequencerNames.getIlluminaSequencerName(runId));

      String emailContent = format("A new run (%s) is finished on %s at %s.\n",
          runId.getId(), sequencerName, new Date(endTime).toString())
          + format("Data for this run can be found at: %s\n\n", outputDir)
          + format("For this task %.2f GB has been used and %.2f GB still free",
              1.0 * outputSize / GIGA, 1.0 * outputFreeSize / GIGA);

      // Create success message
      EmailMessage email = new EmailMessage("Ending run "
          + runId.getId() + " on "
          + sequencerNames.getIlluminaSequencerName(runId), emailContent);

      return new SimpleProcessResult(inputRunData, email);
    } catch (IOException | AozanException e) {
      throw new Aozan3Exception(inputRunData.getRunId(), e);
    }
  }

  /**
   * Create tar file.
   * @param outputFile output tar file
   * @param runDir run directory
   * @param filenames filenames of the file to put in the tar archive
   * @throws IOException if an error occurs while creating the tar file
   * @throws Aozan3Exception if an error occurs while creating the tar file
   */
  private static void createTar(Path outputFile, Path runDir,
      Collection<String> filenames) throws IOException, Aozan3Exception {

    requireNonNull(outputFile);
    requireNonNull(runDir);
    requireNonNull(filenames);

    Tar tar = new Tar(runDir, outputFile);
    for (String f : filenames) {

      // Handle sub directories
      int lastIndex = f.lastIndexOf('/');
      Path dir = lastIndex != -1
          ? Paths.get(runDir.toString(), f.substring(0, lastIndex)) : runDir;
      String filename = lastIndex != -1 ? f.substring(lastIndex + 1) : f;

      try (DirectoryStream<Path> stream =
          Files.newDirectoryStream(dir, filename)) {

        for (Path p : stream) {
          tar.addIncludePattern(runDir.relativize(p).toString());
        }
      }
    }

    tar.execute();
  }

  /**
   * Get the run end time.
   * @param runDir run directory
   * @return run end time since epoch
   */
  private long runEndTime(Path runDir) {

    requireNonNull(runDir);

    long result = 0L;

    File dir = runDir.toFile();
    for (String filename : dir.list()) {

      File f = new File(dir, filename);
      if (!f.isFile()) {
        continue;
      }

      if (f.lastModified() > result) {
        result = f.lastModified();
      }
    }

    return result;
  }

}
