package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.aozan3.DataType.BCL;
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.sizeToHumanReadable;
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.toTimeHumanReadable;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataLocation;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.DataType;
import fr.ens.biologie.genomique.aozan.aozan3.DataType.Category;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.SimpleDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.log.DummyAzoanLogger;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheetUtils;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVWriter;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerImageInstance;
import fr.ens.biologie.genomique.eoulsan.util.process.FallBackDockerClient;
import fr.ens.biologie.genomique.eoulsan.util.process.SimpleProcess;
import fr.ens.biologie.genomique.eoulsan.util.process.SystemSimpleProcess;

/**
 * This class define an abstract Illumina demultiplexing data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public abstract class AbstractIlluminaDemuxDataProcessor
    implements DataProcessor {

  private static final boolean USE_DOCKER = true;

  private AozanLogger logger = new DummyAzoanLogger();

  private DataStorage outputStorage;
  private String dataDescription;
  private final RunConfiguration conf = new RunConfiguration();
  private boolean initialized;

  @Override
  public Set<DataTypeFilter> getInputRequirements() {

    return Collections
        .singleton((DataTypeFilter) new SimpleDataTypeFilter(BCL));
  }

  @Override
  public void init(final Configuration conf, final AozanLogger logger)
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

    // Default configuration
    this.conf.set(conf);
    this.conf.setIfNotExists(getConfPrefix() + ".use.docker", USE_DOCKER);
    this.conf.setIfNotExists("tmp.dir", System.getProperty("java.io.tmpdir"));

    // Demultiplexing tool specific configuration
    additionalInit(this.conf);

    this.initialized = true;
  }

  @Override
  public ProcessResult process(final InputData inputData,
      final RunConfiguration runConf) throws Aozan3Exception {

    requireNonNull(inputData);
    requireNonNull(runConf);

    // Check if object has been initialized
    if (!this.initialized) {
      throw new IllegalStateException();
    }

    RunData inputRunData = inputData.get(DataType.BCL);
    RunId runId = inputRunData.getRunId();

    RunConfiguration conf = new RunConfiguration(this.conf);
    conf.set(runConf);

    DataLocation inputLocation = inputRunData.getLocation();
    DataLocation outputLocation =
        this.outputStorage.newDataLocation(runId.getId());

    try {

      // Check if the input and output storage are equals
      if (this.outputStorage.getPath()
          .equals(inputRunData.getLocation().getStorage().getPath())) {
        throw new IOException(
            "Input and output storage are the same: " + this.outputStorage);
      }

      // Check if input directory exists
      inputLocation.checkReadableDirectory(
          this.dataDescription + " input already exists");

      // Check if final output directory already exists
      outputLocation
          .checkIfNotExists(this.dataDescription + " output already exists");

      // Check if enough disk space
      long inputSize = inputLocation.getDiskUsage();
      long requiredSize =
          (long) (inputSize * conf.getDouble("disk.usage.factor", 1.0));
      this.outputStorage.checkIfEnoughSpace(requiredSize,
          "Not enough space on " + this.dataDescription + " output");

      // Check if a samplesheet exists
      if (!conf.containsKey("illumina.samplesheet")) {
        throw new IOException("No samplesheet found");
      }

      SampleSheet samplesheet = SampleSheetUtils
          .parseCSVSamplesheet(conf.get("illumina.samplesheet"));

      final Path samplesheetPath;

      // Create output directory before demux if required
      if (isOutputMustExists()) {
        Files.createDirectories(outputLocation.getPath());
        samplesheetPath =
            Paths.get(outputLocation.getPath().toString(), "SampleSheet.csv");
      } else {
        samplesheetPath = Files.createTempFile(
            new File(conf.get("tmp.dir")).toPath(), "samplesheet-", ".cvs");
      }

      // Save samplesheet
      saveSampleSheet(runId, samplesheet, samplesheetPath);

      long startTime = System.currentTimeMillis();

      // Launch demultiplexing
      launchDemux(runId, inputLocation.getPath(), outputLocation.getPath(),
          samplesheetPath, conf);

      long endTime = System.currentTimeMillis();

      if (!isOutputMustExists()) {
        Files.delete(samplesheetPath);
      }

      // TODO chmod on fastq

      // TODO archive stats and samplesheet (2 new data processors)

      // # Create index.hml file
      // common.create_html_index_file(conf, run_id, [Settings.HISEQ_STEP_KEY,
      // Settings.DEMUX_STEP_KEY])

      // Log disk usage and disk free space
      long outputSize = outputLocation.getDiskUsage();
      long outputFreeSize = outputLocation.getStorage().getUsableSpace();
      this.logger.info(runId, "output disk free after demux: "
          + sizeToHumanReadable(outputFreeSize));
      this.logger.info(runId,
          "space used by demux: " + sizeToHumanReadable(outputSize));

      // Report URL in email message
      String reportLocationMessage = runConf.containsKey("reports.url")
          ? "\n\nRun reports can be found at following location:\n  "
              + runConf.get("reports.url") + '/' + runId.getId()
          : "";

      String emailContent = String.format("Ending demultiplexing "
          + "for run %s.\n" + "Job finished at %s without error in %s.\n"
          + "FASTQ files for this run can be found in the following directory: %s\n%s"
          + "\nFor this task %s has been used and %s GB still free.",
          runId.getId(), new Date(endTime).toString(),
          toTimeHumanReadable(endTime - startTime), outputLocation.getPath(),
          reportLocationMessage, sizeToHumanReadable(outputSize),
          sizeToHumanReadable(outputFreeSize));

      // Create success message
      EmailMessage email = new EmailMessage(
          "Ending demultiplexing for run "
              + runId.getId() + " on " + inputRunData.getSource(),
          emailContent);

      return new SimpleProcessResult(inputRunData.newLocation(outputLocation)
          .newCategory(Category.PROCESSED), email);

    } catch (IOException e) {
      throw new Aozan3Exception(runId, e);
    }
  }

  //
  // Abstract methods
  //

  /**
   * Get the demultiplexing tool name.
   * @return the demultiplexing tool name
   */
  protected abstract String getDemuxToolName();

  /**
   * Get the configuration prefix of the processor.
   * @return the configuration prefix of the processor
   */
  protected abstract String getConfPrefix();

  /**
   * Additional initialization.
   * @param conf run configuration
   */
  protected abstract void additionalInit(RunConfiguration conf);

  /**
   * Parse demultiplexing tool version.
   * @param line line with the version of tool
   * @return the parsed version
   */
  protected abstract String parseDemuxToolVersion(String line);

  /**
   * Test if output directory must exists before launching demultiplexing.
   * @return true if output directory must exists before launching
   *         demultiplexing
   */
  protected abstract boolean isOutputMustExists();

  /**
   * Create the command line to execute the demultiplexing.
   * @param inputPath input path with BCL files
   * @param outputPath output path with FASTQ files
   * @param samplesheetPath path to the samplesheet file
   * @param toolVersion demultiplexing tool version
   * @param runConf run configuration
   * @return a list with the command line arguments
   * @throws IOException if an error occurs while creating the command line
   */
  protected abstract List<String> createDemuxCommandLine(Path inputPath,
      Path outputPath, Path samplesheetPath, String toolVersion,
      RunConfiguration runConf) throws IOException;

  //
  // Other methods
  //

  private void launchDemux(RunId runId, Path inputPath, Path outputPath,
      Path samplesheetPath, final RunConfiguration conf) throws IOException {

    requireNonNull(inputPath);
    requireNonNull(outputPath);
    requireNonNull(samplesheetPath);
    requireNonNull(conf);

    // Get demultiplexing tool version
    String toolVersion = getDemuxToolVersion(runId, conf);

    // Create command line
    List<String> commandLine = createDemuxCommandLine(inputPath, outputPath,
        samplesheetPath, toolVersion, conf);

    File executionOutputPath = isOutputMustExists()
        ? outputPath.toFile() : outputPath.toFile().getParentFile();

    // define stdout and stderr files
    File stdoutFile = new File(executionOutputPath,
        getConfPrefix() + "_output_" + runId.getId() + ".out");
    File stderrFile = new File(executionOutputPath,
        getConfPrefix() + "_output_" + runId.getId() + ".err");

    // Get temporary directory
    File temporaryDirectory = new File(conf.get("tmp.dir"));

    this.logger.info(runId, getDemuxToolName() + ": " + toolVersion);
    this.logger.info(runId, "Demultiplexing using the following command line: "
        + String.join(" ", commandLine));

    long startTime = System.currentTimeMillis();

    final int exitValue =
        newSimpleProcess(runId, conf, true).execute(commandLine,
            executionOutputPath, temporaryDirectory, stdoutFile, stderrFile,
            inputPath.toFile(), executionOutputPath, temporaryDirectory);

    long endTime = System.currentTimeMillis();

    if (exitValue != 0) {
      throw new IOException("Error while running "
          + getDemuxToolName() + ", exit code is: " + exitValue);
    }

    this.logger.info(runId, "Successful demultiplexing in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));
  }

  /**
   * Create a new process.
   * @param runId run id for logging
   * @param runConf run configuration
   * @param enableLogging enable logging
   * @return a new SimpleProcess
   * @throws IOException if an error occurs while creating the process
   */
  private SimpleProcess newSimpleProcess(final RunId runId,
      final RunConfiguration runConf, boolean enableLogging)
      throws IOException {

    boolean dockerMode =
        runConf.getBoolean(getConfPrefix() + ".use.docker", false);

    if (enableLogging) {
      this.logger.info(runId,
          dockerMode
              ? "Use Docker for executing " + getDemuxToolName()
              : "Use installed version of " + getDemuxToolName());
    }

    if (!dockerMode) {
      return new SystemSimpleProcess();
    }

    String dockerImage =
        runConf.get(getConfPrefix() + ".docker.image", "").trim();

    if (dockerImage.isEmpty()) {
      throw new IOException(
          "No docker image defined for " + getDemuxToolName());
    }

    if (enableLogging) {
      this.logger.info(runId,
          "Docker image to use for " + getDemuxToolName() + ": " + dockerImage);
    }

    // TODO The Spotify Docker client in Eoulsan does not seems to work anymore
    // Use fallback Docker client
    DockerImageInstance result =
        new FallBackDockerClient().createConnection(dockerImage);

    // Pull Docker image if not exists
    result.pullImageIfNotExists();

    return result;
  }

  /**
   * Get demultiplexing executable version.
   * @param runId the run Id
   * @param runConf run configuration
   * @return a string with the demultiplexing tool version
   * @throws IOException if an error occurs while getting tool version
   */
  private String getDemuxToolVersion(final RunId runId,
      final RunConfiguration runConf) throws IOException {

    File tmpDir = new File(runConf.get("tmp.dir"));
    List<String> commandLine =
        asList(runConf.get(getConfPrefix() + ".path", getDemuxToolName()),
            "--version");

    File stdoutFile = Files
        .createTempFile(tmpDir.toPath(), getConfPrefix() + "-version", ".out")
        .toFile();
    File stderrFile = Files
        .createTempFile(tmpDir.toPath(), getConfPrefix() + "-version", ".err")
        .toFile();

    final int exitValue = newSimpleProcess(runId, runConf, false)
        .execute(commandLine, tmpDir, tmpDir, stdoutFile, stderrFile, tmpDir);

    // Launch demultiplexing tool
    if (exitValue != 0) {
      Files.delete(stdoutFile.toPath());
      Files.delete(stderrFile.toPath());
      throw new IOException("Unable to launch "
          + getDemuxToolName() + " to get software version");
    }

    // Parse stderr file
    String result = null;
    for (String line : Files.readAllLines(stderrFile.toPath())) {

      if (line.startsWith(getDemuxToolName())) {
        result = parseDemuxToolVersion(line);
        break;
      }
    }

    // Delete output files
    Files.delete(stdoutFile.toPath());
    Files.delete(stderrFile.toPath());

    if (result == null) {
      throw new IOException("Unable to get " + getDemuxToolName() + " version");
    }

    return result;
  }

  private void saveSampleSheet(final RunId runId, final SampleSheet samplesheet,
      Path outputFile) throws IOException {

    // Write CSV samplesheet file in the samplesheet version 2 format
    try (SampleSheetCSVWriter writer =
        new SampleSheetCSVWriter(outputFile.toFile())) {
      writer.setVersion(2);
      writer.writer(samplesheet);
    } catch (IOException e) {
      this.logger.error(runId, "Error while writing Illumina samplesheet: "
          + outputFile + "(" + e.getMessage() + ")");
      throw new IOException(e);
    }
  }

}
