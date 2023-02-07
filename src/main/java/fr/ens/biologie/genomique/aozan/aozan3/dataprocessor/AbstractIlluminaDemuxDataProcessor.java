package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.aozan3.DataType.BCL;
import static fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger.error;
import static fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger.info;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.sizeToHumanReadable;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toTimeHumanReadable;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import fr.ens.biologie.genomique.aozan.aozan3.util.DiskUtils;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetCSVWriter;
import fr.ens.biologie.genomique.kenetre.log.DummyLogger;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;

/**
 * This class define an abstract Illumina demultiplexing data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public abstract class AbstractIlluminaDemuxDataProcessor
    implements DataProcessor {

  private static final boolean USE_DOCKER = true;

  private GenericLogger logger = new DummyLogger();

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
  public void init(final Configuration conf, final GenericLogger logger)
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

      SampleSheet samplesheet =
          SampleSheetUtils.deSerialize(conf.get("illumina.samplesheet"));

      processSampleSheet(samplesheet);

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

      // Chmod on output directory
      DiskUtils.changeDirectoryMode(outputLocation.getPath(), "u+r,g+r,o+r");
      if (conf.getBoolean("read.only.output.files", false)) {
        DiskUtils.changeDirectoryMode(outputLocation.getPath(), "u-w,g-w,o-w");
      }

      // TODO archive stats and samplesheet (2 new data processors)

      // # Create index.hml file
      // common.create_html_index_file(conf, run_id, [Settings.HISEQ_STEP_KEY,
      // Settings.DEMUX_STEP_KEY])

      // Log disk usage and disk free space
      long outputSize = outputLocation.getDiskUsage();
      long outputFreeSize = outputLocation.getStorage().getUsableSpace();
      info(this.logger, runId, "output disk free after demux: "
          + sizeToHumanReadable(outputFreeSize));
      info(this.logger, runId,
          "space used by demux: " + sizeToHumanReadable(outputSize));

      // Report URL in email message
      String reportLocationMessage = conf.containsKey("reports.url")
          ? "\nRun reports can be found at following location:\n  "
              + conf.get("reports.url") + '/' + runId.getId() + "\n"
          : "";

      String emailContent = String.format(
          "Ending demultiplexing "
              + "for run %s.\n" + "Job finished at %s without error in %s.\n\n"
              + "FASTQ files for this run can be found in the following "
              + "directory:\n  %s\n%s"
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
  protected abstract String parseDemuxToolVersion(List<String> line);

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

    // Define external tool
    ExternalTool tool = new ExternalTool(getDemuxToolName(),
        conf.getBoolean(getConfPrefix() + ".use.docker", false),
        conf.get(getConfPrefix() + ".docker.image", ""), false, this.logger);

    // Get demultiplexing tool version
    String toolVersion = tool.getToolVersion(runId, conf.get("tmp.dir"),
        asList(conf.get(getConfPrefix() + ".path", getDemuxToolName()),
            "--version"),
        true, this::parseDemuxToolVersion);

    // Create command line
    List<String> commandLine = createDemuxCommandLine(inputPath, outputPath,
        samplesheetPath, toolVersion, conf);

    // If output directory must not exists before demux, the working directory
    // will be the parent directory of the output directory
    File workingDirectory = isOutputMustExists()
        ? outputPath.toFile() : outputPath.toFile().getParentFile();

    // define stdout and stderr files
    File stdoutFile = new File(workingDirectory,
        getConfPrefix() + "_output_" + runId.getId() + ".out");
    File stderrFile = new File(workingDirectory,
        getConfPrefix() + "_output_" + runId.getId() + ".err");

    // Get temporary directory
    File temporaryDirectory = new File(conf.get("tmp.dir"));

    info(this.logger, runId, getDemuxToolName() + ": " + toolVersion);
    info(this.logger, runId, "Demultiplexing using the following command line: "
        + String.join(" ", commandLine));

    long startTime = System.currentTimeMillis();

    final int exitValue = tool.newSimpleProcess(runId, true).execute(
        commandLine, workingDirectory, temporaryDirectory, stdoutFile,
        stderrFile, inputPath.toFile(), workingDirectory, temporaryDirectory);

    long endTime = System.currentTimeMillis();

    // If output directory must not exists before demux, move in output
    // directory after demux stdout
    // and stderr files
    if (!isOutputMustExists()) {
      stdoutFile.renameTo(new File(outputPath.toFile(), stdoutFile.getName()));
      stderrFile.renameTo(new File(outputPath.toFile(), stderrFile.getName()));
    }

    if (exitValue != 0) {
      throw new IOException("Error while running "
          + getDemuxToolName() + ", exit code is: " + exitValue);
    }

    // Create a copy of the sample at the root of the output directory
    Path sampleSheetCopyFile =
        Paths.get(outputPath.toString(), "SampleSheet.csv");
    if (!Files.isRegularFile(sampleSheetCopyFile)) {
      Files.copy(samplesheetPath, sampleSheetCopyFile,
          StandardCopyOption.COPY_ATTRIBUTES);
    }

    info(this.logger, runId, "Successful demultiplexing in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));
  }

  private void saveSampleSheet(final RunId runId, final SampleSheet samplesheet,
      Path outputFile) throws IOException {

    // Write CSV samplesheet file in the samplesheet version 2 format
    try (SampleSheetCSVWriter writer =
        new SampleSheetCSVWriter(outputFile.toFile())) {
      writer.setVersion(2);
      writer.writer(samplesheet);
    } catch (IOException e) {
      error(this.logger, runId, "Error while writing Illumina samplesheet: "
          + outputFile + "(" + e.getMessage() + ")");
      throw new IOException(e);
    }
  }

  protected void addCommandLineArgument(List<String> args,
      RunConfiguration conf, String longArgName) {

    addCommandLineArgument(args, conf, longArgName, null);
  }

  protected void addCommandLineArgument(List<String> args,
      RunConfiguration conf, String longArgName, String defaultValue) {

    String confKey =
        getConfPrefix() + '.' + longArgName.replace("--", "").replace('-', '.');

    if (conf.containsKey(confKey)) {
      args.add(longArgName);
      args.add(conf.get(confKey));
    } else if (defaultValue != null) {
      args.add(longArgName);
      args.add(defaultValue);
    }

  }

  /**
   * Process sample sheet.
   * @param samplesheet sample sheet to process
   */
  protected void processSampleSheet(SampleSheet samplesheet)
      throws Aozan3Exception {
  }

}
