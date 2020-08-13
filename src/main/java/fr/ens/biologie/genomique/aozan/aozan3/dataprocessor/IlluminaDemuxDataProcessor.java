package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.sizeToHumanReadable;
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.toTimeHumanReadable;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataLocation;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunData.Type;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.log.DummyAzoanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheetUtils;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVWriter;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerManager;
import fr.ens.biologie.genomique.eoulsan.util.process.SimpleProcess;
import fr.ens.biologie.genomique.eoulsan.util.process.SystemSimpleProcess;

/**
 * This class define a demultiplexing data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IlluminaDemuxDataProcessor implements DataProcessor {

  public static final String PROCESSOR_NAME = "illumina_demux";

  private static final boolean USE_DOCKER = true;
  private static final String DEFAULT_BCL2FASTQ_VERSION = "2.18.0.12";
  private static final String DEFAULT_DOCKER_IMAGE =
      "genomicpariscentre/bcl2fastq2:" + DEFAULT_BCL2FASTQ_VERSION;

  private static final int DEFAULT_MISMATCHES = 0;

  private AozanLogger logger = new DummyAzoanLogger();

  private DataStorage outputStorage;
  private String dataDescription;
  private final RunConfiguration conf = new RunConfiguration();
  private boolean initialized;

  @Override
  public String getName() {
    return PROCESSOR_NAME;
  }

  @Override
  public boolean accept(Type type, boolean partialData) {

    return type == RunData.Type.RAW && !partialData;
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
          "The output synchronization directory is not writable: "
              + outputStorage);
    }

    this.outputStorage = outputStorage;
    this.dataDescription = conf.get("data.description", "no description");

    // Default configuration
    this.conf.set(conf);
    this.conf.setIfNotExists("bcl2fastq.use.docker", USE_DOCKER);
    this.conf.setIfNotExists("bcl2fastq.docker.image", DEFAULT_DOCKER_IMAGE);
    this.conf.setIfNotExists("bcl2fastq.path", "/usr/local/bin/bcl2fastq");
    this.conf.setIfNotExists("tmp.dir", System.getProperty("java.io.tmpdir"));

    this.initialized = true;
  }

  @Override
  public ProcessResult process(final RunData inputRunData,
      final RunConfiguration runConf) throws Aozan3Exception {

    requireNonNull(inputRunData);
    requireNonNull(runConf);

    // Check if object has been initialized
    if (!this.initialized) {
      throw new IllegalStateException();
    }

    RunId runId = inputRunData.getRunId();

    RunConfiguration conf = new RunConfiguration(this.conf);
    conf.set(runConf);

    DataLocation inputLocation = inputRunData.getLocation();
    DataLocation outputLocation =
        this.outputStorage.newDataLocation(runId.getId());

    try {

      // Check if the input and ouput storage are equals
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
      if (!conf.containsKey("bcl2fastq.samplesheet")) {
        throw new IOException("No samplesheet found");
      }

      SampleSheet samplesheet = SampleSheetUtils
          .parseCSVSamplesheet(conf.get("bcl2fastq.samplesheet"));

      // Create output directory
      Files.createDirectories(outputLocation.getPath());

      Path samplesheetPath =
          saveSampleSheet(runId, samplesheet, outputLocation.getPath());

      long startTime = System.currentTimeMillis();

      // Launch demultiplexing
      launchBcl2Fastq(runId, inputLocation.getPath(), outputLocation.getPath(),
          samplesheetPath, conf);

      long endTime = System.currentTimeMillis();

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

      String emailContent = String.format("Ending demultiplexing with %d"
          + " mismatch(es) for run %s.\n"
          + "Job finished at %s without error in %s.\n"
          + "FASTQ files for this run can be found in the following directory: %s\n%s"
          + "\nFor this task %s has been used and %s GB still free.",
          runConf.getInt("bcl2fastq.allowed.mismatches", DEFAULT_MISMATCHES),
          runId.getId(), new Date(endTime).toString(),
          toTimeHumanReadable(endTime - startTime), outputLocation.getPath(),
          reportLocationMessage, sizeToHumanReadable(outputSize),
          sizeToHumanReadable(outputFreeSize));

      // Create success message
      EmailMessage email = new EmailMessage(
          "Ending demultiplexing for run "
              + runId.getId() + " on " + inputRunData.getSource(),
          emailContent);

      return new SimpleProcessResult(
          inputRunData.newLocation(outputLocation).newType(Type.PROCESSED),
          email);

    } catch (IOException e) {
      throw new Aozan3Exception(runId, e);
    }
  }

  private void launchBcl2Fastq(RunId runId, Path inputPath, Path outputPath,
      Path samplesheetPath, final RunConfiguration conf) throws IOException {

    requireNonNull(inputPath);
    requireNonNull(outputPath);
    requireNonNull(samplesheetPath);
    requireNonNull(conf);

    // Get Bcl2fastq version
    String bcl2fastqVersion = getBcl2FastqVersion(runId, conf);

    // Create command line
    List<String> commandLine = createBcl2fastqCommandLine(inputPath, outputPath,
        samplesheetPath, bcl2fastqVersion, conf);

    // define stdout and stderr files
    File stdoutFile = new File(outputPath.toFile(),
        "bcl2fastq_output_" + runId.getId() + ".out");
    File stderrFile = new File(outputPath.toFile(),
        "bcl2fastq_output_" + runId.getId() + ".err");

    // Get temporary directory
    File temporaryDirectory = new File(conf.get("tmp.dir"));

    this.logger.info(runId, "Bcl2fastq version: " + bcl2fastqVersion);
    this.logger.info(runId, "Demultiplexing using the following command line: "
        + String.join(" ", commandLine));

    long startTime = System.currentTimeMillis();

    final int exitValue =
        newSimpleProcess(runId, conf, true).execute(commandLine,
            outputPath.toFile(), temporaryDirectory, stdoutFile, stderrFile,
            inputPath.toFile(), outputPath.toFile(), temporaryDirectory);

    long endTime = System.currentTimeMillis();

    if (exitValue != 0) {
      throw new IOException(
          "Error while running bcl2fastq, exit code is: " + exitValue);
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

    boolean dockerMode = runConf.getBoolean("bcl2fastq.use.docker", false);

    if (enableLogging) {
      this.logger.info(runId,
          dockerMode
              ? "Use Docker for executing bcl2fastq"
              : "Use installed version of bcl2fastq");
    }

    if (!dockerMode) {
      return new SystemSimpleProcess();
    }

    String dockerImage = runConf.get("bcl2fastq.docker.image", "").trim();

    if (dockerImage.isEmpty()) {
      throw new IOException("No docker image defined for bcl2fastq");
    }

    if (enableLogging) {
      this.logger.info(runId,
          "Docker image to use for bcl2fastq: " + dockerImage);
    }

    return DockerManager.getInstance().createImageInstance(dockerImage);
  }

  /**
   * Create the command line to execute the demultiplexing.
   * @param inputPath input path with BCL files
   * @param outputPath output path with FASTQ files
   * @param samplesheetPath path to the samplesheet file
   * @param bcl2fastqVersion bcl2fastq version
   * @param runConf run configuration
   * @return a list with the command line arguments
   * @throws IOException if an error occurs while creating the command line
   */
  private static List<String> createBcl2fastqCommandLine(Path inputPath,
      Path outputPath, Path samplesheetPath, String bcl2fastqVersion,
      RunConfiguration runConf) throws IOException {

    // Get parameter values
    String finalCommandPath = runConf.get("bcl2fastq.path", "bcl2fastq");
    int threadCount = runConf.getInt("bcl2fastq.threads",
        Runtime.getRuntime().availableProcessors());
    int mismatchCount =
        runConf.getInt("bcl2fastq.allowed.mismatches", DEFAULT_MISMATCHES);
    int bcl2fastqMinorVersion =
        Integer.parseInt(bcl2fastqVersion.split("\\.")[1]);

    //  List arg
    List<String> args = new ArrayList<>();

    args.add(finalCommandPath);
    args.addAll(asList("--loading-threads", "" + threadCount));
    args.addAll(asList("--processing-threads", "" + threadCount));
    args.addAll(asList("--writing-threads", "" + threadCount));

    if (bcl2fastqMinorVersion < 19) {
      args.addAll(asList("--demultiplexing-threads", "" + threadCount));
    }

    args.addAll(asList("--sample-sheet", samplesheetPath.toString()));
    args.addAll(asList("--barcode-mismatches", "" + mismatchCount));

    // Common parameter, setting per default
    args.addAll(
        asList("--input-dir", inputPath + "/Data/Intensities/BaseCalls"));
    args.addAll(asList("--output-dir", outputPath.toString()));

    if (runConf.getBoolean("bcl2fastq.with.failed.reads", true)) {
      args.add("--with-failed-reads");
    }

    // Specific parameter
    args.addAll(asList("--runfolder-dir", inputPath.toString()));
    args.addAll(asList("--interop-dir", outputPath + "/InterOp"));
    args.addAll(asList("--min-log-level", "TRACE"));
    args.addAll(asList("--stats-dir", outputPath + "/Stats"));
    args.addAll(asList("--reports-dir", outputPath + "/Reports"));

    // Set the compression level

    if (runConf.containsKey("bcl2fastq.compression.level")) {

      int level = runConf.getInt("bcl2fastq.compression.level");
      if (level < 0 || level > 10) {
        throw new IOException("Invalid Bcl2fastq compression level: " + level);
      }

      args.addAll(asList("--fastq-compression-level", "" + level));
    }

    if (runConf.containsKey("bcl2fastq.additionnal.arguments")) {

      String additionalArguments =
          runConf.get("bcl2fastq.additionnal.arguments");
      args.addAll(StringUtils.splitShellCommandLine(additionalArguments));
    }

    return args;
  }

  /**
   * Get bcl2fastq executable version.
   * @param runId the run Id
   * @param runConf run configuration
   * @return a string with the bcl2fastq version
   * @throws IOException if an error occurs while getting bcl2fastq version
   */
  private String getBcl2FastqVersion(final RunId runId,
      final RunConfiguration runConf) throws IOException {

    File tmpDir = new File(runConf.get("tmp.dir"));
    List<String> commandLine =
        asList(runConf.get("bcl2fastq.path", "bcl2fastq"), "--version");

    File stdoutFile = Files
        .createTempFile(tmpDir.toPath(), "bcl2fastq-versrion", ".out").toFile();
    File stderrFile = Files
        .createTempFile(tmpDir.toPath(), "bcl2fastq-versrion", ".err").toFile();

    final int exitValue = newSimpleProcess(runId, runConf, false)
        .execute(commandLine, tmpDir, tmpDir, stdoutFile, stderrFile, tmpDir);

    // Launch bcl2fastq
    if (exitValue != 0) {
      Files.delete(stdoutFile.toPath());
      Files.delete(stderrFile.toPath());
      throw new IOException(
          "Unable to launch bcl2fastq to get software version");
    }

    // Parse stderr file
    String result = null;
    for (String line : Files.readAllLines(stderrFile.toPath())) {

      if (line.startsWith("bcl2fastq")) {
        result = line.substring("bcl2fastq v".length());
        break;
      }
    }

    // Delete output files
    Files.delete(stdoutFile.toPath());
    Files.delete(stderrFile.toPath());

    if (result == null) {
      throw new IOException("Unable to get bcl2fastq version");
    }

    return result;
  }

  private Path saveSampleSheet(final RunId runId, final SampleSheet samplesheet,
      Path outputDirectory) throws IOException {

    File outputFile = new File(outputDirectory.toFile(), "SampleSheet.csv");

    // Write CSV samplesheet file in BCL2FASTQ2 format
    try (SampleSheetCSVWriter writer = new SampleSheetCSVWriter(outputFile)) {
      writer.setVersion(2);
      writer.writer(samplesheet);
    } catch (IOException e) {
      this.logger.error(runId, "Error while writing Bcl2fastq samplesheet: "
          + outputFile + "(" + e.getMessage() + ")");
      throw new IOException(e);
    }
    return outputFile.toPath();
  }

}
