package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.aozan3.DataType.FAST5_TAR;
import static fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger.info;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataLocation;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.ExternalTool.ExecutionUser;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.SimpleDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.nanopore.BasecallingModelSelector;
import fr.ens.biologie.genomique.aozan.aozan3.util.CopyAndMergeGuppyOutput;
import fr.ens.biologie.genomique.aozan.aozan3.util.DiskUtils;
import fr.ens.biologie.genomique.aozan.aozan3.util.UnTar;
import fr.ens.biologie.genomique.kenetre.log.DummyLogger;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetCSVWriter;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetReader;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetWriter;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;
import fr.ens.biologie.genomique.kenetre.util.Version;

/**
 * This class implements a Dorado data processor.
 * @author Laurent Jourdren
 * @since 3.1
 */
public class DoradoONTBasecallingDataProcessor implements DataProcessor {

  public static final String PROCESSOR_NAME = "dorado_basecaller";
  private static final String CONF_PREFIX = "dorado";
  private static final String DEFAULT_DORADO_DOCKER_REPO =
      "genomicpariscentre/dorado";
  private static final String DEFAULT_DORADO_VERSION = "0.7.2";
  private static final String DEFAULT_MODEL_SELECTION_COMPLEX = "sup";

  private static final boolean USE_DOCKER = true;

  private GenericLogger logger = new DummyLogger();

  private DataStorage outputStorage;
  private String dataDescription;
  private final RunConfiguration conf = new RunConfiguration();
  private Path modelsPath;
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

    // Default configuration
    this.conf.set(conf);
    this.conf.setIfNotExists(CONF_PREFIX + ".use.docker", USE_DOCKER);
    this.conf.setIfNotExists("tmp.dir", System.getProperty("java.io.tmpdir"));

    if (!this.conf.containsKey(CONF_PREFIX + ".models.path")) {
      throw new Aozan3Exception("No Dorado models path defined.");
    }

    this.modelsPath = this.conf.getPath(CONF_PREFIX + ".models.path");
    if (!Files.isDirectory(modelsPath)) {
      throw new Aozan3Exception(
          "Dorado models path not found: " + this.modelsPath);
    }

    this.initialized = true;
  }

  @Override
  public Set<DataTypeFilter> getInputRequirements() {

    return Collections
        .singleton((DataTypeFilter) new SimpleDataTypeFilter(FAST5_TAR));
  }

  @Override
  public ProcessResult process(InputData inputData, RunConfiguration runConf)
      throws Aozan3Exception {

    requireNonNull(inputData);
    requireNonNull(runConf);

    // Check if object has been initialized
    if (!this.initialized) {
      throw new IllegalStateException();
    }

    RunData inputRunData = inputData.get(FAST5_TAR);
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

      // Check if input file exists
      inputLocation.checkReadableFile("FAST5 tar");

      // Check if final output directory already exists
      outputLocation
          .checkIfNotExists(this.dataDescription + " output already exists");

      // Check if enough disk space
      long inputSize = inputLocation.getDiskUsage();
      long requiredSize =
          (long) (inputSize * conf.getDouble("disk.usage.factor", 1.0));
      this.outputStorage.checkIfEnoughSpace(requiredSize,
          "Not enough space on " + this.dataDescription + " output");

      // TODO call pipeline

      return null;
    } catch (IOException e) {
      throw new Aozan3Exception(runId, e);
    }
  }

  /**
   * Convenient method to launch Dorado outside Aozan workflow.
   * @param runId run Id
   * @param inputTarPath input tar with Fast5 files path
   * @param outputPath output path
   * @param tmpPath temporary directory path
   * @param modelsPath model path
   * @param runConf run configuration
   * @param keepTemporaryFiles keep temporary files
   * @param logger Aozan logger
   * @throws Aozan3Exception if an error occurs while executing Dorado
   */
  private static void tarPipeline(RunId runId, Path inputTarPath,
      Path outputPath, Path tmpPath, final RunConfiguration runConf,
      boolean keepTemporaryFiles, GenericLogger logger) throws Aozan3Exception {

    requireNonNull(inputTarPath);
    requireNonNull(outputPath);
    requireNonNull(tmpPath);
    requireNonNull(runConf);

    try {

      // Untar FAST5 files
      System.out.println("* Uncompress FAST5/POD5 Tar file");
      Path inputDirPath = Files.createTempDirectory(tmpPath, "raw-fast5-");
      UnTar untar = new UnTar(inputTarPath, inputDirPath);
      untar.execute();
      DiskUtils.changeDirectoryMode(inputDirPath, "777");

      directoryPipeline(runId, inputDirPath, outputPath, tmpPath, runConf,
          keepTemporaryFiles, logger);

      // Delete temporary untarred FAST5 tar
      if (!keepTemporaryFiles) {
        System.out.println("* Remove uncompressed FAST5/POD5 files");
        deleteDirectory(inputDirPath);
      }

    } catch (IOException e) {
      throw new Aozan3Exception(e);
    }
  }

  /**
   * Convenient method to launch Dorado outside Aozan workflow.
   * @param runId run Id
   * @param inputDirPath input directory with Fast5 files path
   * @param outputPath output path
   * @param tmpPath temporary directory path
   * @param modelsPath model path
   * @param runConf run configuration
   * @param keepTemporaryFiles keep temporary files
   * @param logger Aozan logger
   * @throws Aozan3Exception if an error occurs while executing Dorado
   */
  private static void directoryPipeline(RunId runId, Path inputDirPath,
      Path outputPath, Path tmpPath, final RunConfiguration runConf,
      boolean keepTemporaryFiles, GenericLogger logger) throws Aozan3Exception {

    requireNonNull(inputDirPath);
    requireNonNull(outputPath);
    requireNonNull(tmpPath);
    requireNonNull(runConf);

    try {

      // Launch Dorado
      System.out.println("* Launch Dorado");

      // Create a temporary directory writable by all users
      Path outputDirPath = Files.createTempDirectory(tmpPath, "fastq-");
      DiskUtils.changeDirectoryMode(outputDirPath, "777");

      Path bamPath =
          Paths.get(outputDirPath.toString(), runId.getId() + ".bam");
      Path summaryPath =
          Paths.get(outputDirPath.toString(), "sequencing_summary.txt");
      Path fastqPath = Paths.get(outputDirPath.toString(), "fastq");

      // Create temporary sample sheet if needed
      if (runConf.containsKey(CONF_PREFIX + ".sample.sheet.path")) {

        Path inputSampleSheet =
            runConf.getPath(CONF_PREFIX + ".sample.sheet.path");
        Path outputSampleSheet =
            generateStrictSampleSheet(inputSampleSheet, outputDirPath);

        // Sample sheet is not generated if there is no barcode
        if (outputSampleSheet == null) {
          runConf.remove(CONF_PREFIX + ".sample.sheet.path");
        } else {
          runConf.set(CONF_PREFIX + ".sample.sheet.path",
              outputSampleSheet.toString());
        }
      }

      // Launch Dorado
      launchDoradoBasecaller(runId, outputDirPath, inputDirPath, bamPath,
          runConf, logger);

      // Launch Summary
      launchDoradoSummary(runId, outputDirPath, bamPath, summaryPath, runConf,
          logger);

      // Launch Demultiplexing
      Files.createDirectories(fastqPath);
      DiskUtils.changeDirectoryMode(fastqPath, "777");
      launchDoradoDemux(runId, outputDirPath, bamPath, fastqPath, runConf,
          logger);

      // Copy and merge FAST5 files to the output directory
      Path mergedFastqPath = Paths.get(outputPath.toString(), runId.getId());
      CopyAndMergeGuppyOutput merger =
          new CopyAndMergeGuppyOutput(outputDirPath, mergedFastqPath);
      merger.setFastqMerging(false);
      merger.setCompressSequencingSummary(runConf
          .getBoolean(CONF_PREFIX + ".compress.sequencing.summary", true));
      merger.execute();

      // Delete unmerged Fastq directory
      if (!keepTemporaryFiles) {
        System.out.println("* Remove unmerged FASTQ directory");
        deleteDirectory(outputDirPath);
      }

    } catch (IOException e) {
      throw new Aozan3Exception(e);
    }
  }

  /**
   * Launch Dorado in basecaller mode.
   * @param runId run Id
   * @param workingPath working directory
   * @param inputPath input path
   * @param bamPath output BAM file path
   * @param sampleSheetPath sample sheet path
   * @param modelsPath model path
   * @param runConf run configuration
   * @param logger Aozan logger
   * @throws Aozan3Exception if run configuration is invalid
   * @throws IOException if an error occurs while executing Dorado
   */
  private static void launchDoradoBasecaller(RunId runId, Path workingPath,
      Path inputPath, Path bamPath, RunConfiguration runConf,
      GenericLogger logger) throws Aozan3Exception, IOException {

    // Define external tool
    ExternalTool tool = new ExternalTool("dorado",
        runConf.getBoolean(CONF_PREFIX + ".use.docker", false),
        runConf.get(CONF_PREFIX + ".docker.image", ""),
        runConf.getBoolean(CONF_PREFIX + ".use.docker", false),
        ExecutionUser.NOBODY, logger);

    // Get demultiplexing tool version
    String toolVersion = tool.getToolVersion(runId, runConf.get("tmp.dir"),
        asList(runConf.get(CONF_PREFIX + ".path", "dorado"), "-v"), true,
        DoradoONTBasecallingDataProcessor::parseDoradoVersion);

    File outputDir = workingPath.toFile();

    // define stdout and stderr files
    File stdoutFile = bamPath.toFile();
    File stderrFile =
        new File(outputDir, "dorado_ballercaller_" + runId.getId() + ".err");
    File devnullFile = new File("/dev/null");
    File modelsDir = runConf
        .getPath(CONF_PREFIX + ".models.path", devnullFile.toPath()).toFile();
    File sampleSheetFile = runConf
        .getPath(CONF_PREFIX + ".sample.sheet.path", devnullFile.toPath())
        .toFile();

    // Create command line
    List<String> commandLine = createDoradoBasecallerCommandLine(inputPath,
        modelsDir.toPath(), runConf);

    info(logger, runId, "Dorado: " + toolVersion);
    info(logger, runId, "Demultiplexing using the following command line: "
        + String.join(" ", commandLine));

    long startTime = System.currentTimeMillis();

    final int exitValue = tool.newSimpleProcess(runId, true).execute(
        commandAsBash(commandLine, stdoutFile, stderrFile), outputDir,
        outputDir, devnullFile, devnullFile, inputPath.toFile(),
        sampleSheetFile, outputDir, modelsDir);

    long endTime = System.currentTimeMillis();

    if (exitValue != 0) {
      throw new IOException(
          "Error while running dorado, exit code is: " + exitValue);
    }

    info(logger, runId, "Successful demultiplexing in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));
  }

  /**
   * Launch Dorado in summary mode.
   * @param runId run Id
   * @param workingPath working directory
   * @param bamPath input BAM file path
   * @param summaryPath summary output file path
   * @param runConf run configuration
   * @param logger Aozan logger
   * @throws Aozan3Exception if run configuration is invalid
   * @throws IOException if an error occurs while executing Dorado
   */
  private static void launchDoradoSummary(RunId runId, Path workingPath,
      Path bamPath, Path summaryPath, RunConfiguration runConf,
      GenericLogger logger) throws Aozan3Exception, IOException {

    // Define external tool
    ExternalTool tool = new ExternalTool("dorado",
        runConf.getBoolean(CONF_PREFIX + ".use.docker", false),
        runConf.get(CONF_PREFIX + ".docker.image", ""),
        runConf.getBoolean(CONF_PREFIX + ".use.docker", false),
        ExecutionUser.NOBODY, logger);

    // Create command line
    List<String> commandLine =
        Arrays.asList(runConf.get(CONF_PREFIX + ".path", "dorado"), "summary",
            bamPath.toString());

    File outputDir = workingPath.toFile();

    // define stdout and stderr files
    File stdoutFile = summaryPath.toFile();
    File stderrFile =
        new File(outputDir, "dorado_summary_" + runId.getId() + ".err");
    File devnullFile = new File("/dev/null");

    info(logger, runId, "Creating summary using the following command line: "
        + String.join(" ", commandLine));

    long startTime = System.currentTimeMillis();

    final int exitValue = tool.newSimpleProcess(runId, true).execute(
        commandAsBash(commandLine, stdoutFile, stderrFile), outputDir,
        outputDir, devnullFile, devnullFile, bamPath.toFile(), outputDir);

    long endTime = System.currentTimeMillis();

    if (exitValue != 0) {
      throw new IOException(
          "Error while running dorado, exit code is: " + exitValue);
    }

    info(logger, runId, "Successful creating summary in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));
  }

  /**
   * Launch Dorado in summary mode.
   * @param runId run Id
   * @param workingPath working directory
   * @param bamPath input BAM file path
   * @param fastqDirPath FASTQ output dir path
   * @param sampleSheetPath sample sheet path
   * @param runConf run configuration
   * @param logger Aozan logger
   * @throws Aozan3Exception if run configuration is invalid
   * @throws IOException if an error occurs while executing Dorado
   */
  private static void launchDoradoDemux(RunId runId, Path workingPath,
      Path bamPath, Path fastqDirPath, RunConfiguration runConf,
      GenericLogger logger) throws Aozan3Exception, IOException {

    // Define external tool
    ExternalTool tool = new ExternalTool("dorado",
        runConf.getBoolean(CONF_PREFIX + ".use.docker", false),
        runConf.get(CONF_PREFIX + ".docker.image", ""),
        runConf.getBoolean(CONF_PREFIX + ".use.docker", false),
        ExecutionUser.NOBODY, logger);

    // Create command line
    List<String> commandLine = new ArrayList<>();
    commandLine.addAll(Arrays.asList(
        runConf.get(CONF_PREFIX + ".path", "dorado"), "demux", "--no-classify",
        "--emit-fastq", "--output-dir", fastqDirPath.toString()));

    if (runConf.containsKey(CONF_PREFIX + ".sample.sheet.path")) {
      commandLine.add("--sample-sheet");
      commandLine.add(runConf.get(CONF_PREFIX + ".sample.sheet.path"));
    }

    commandLine.add(bamPath.toString());

    File outputDir = workingPath.toFile();

    // define stdout and stderr files
    File stdoutFile =
        new File(outputDir, "dorado_demux_" + runId.getId() + ".out");
    File stderrFile =
        new File(outputDir, "dorado_demux_" + runId.getId() + ".err");
    File devnullFile = new File("/dev/null");
    File sampleSheetFile = runConf
        .getPath(CONF_PREFIX + ".sample.sheet.path", devnullFile.toPath())
        .toFile();

    info(logger, runId, "Demultiplexing using the following command line: "
        + String.join(" ", commandLine));

    long startTime = System.currentTimeMillis();

    final int exitValue = tool.newSimpleProcess(runId, true).execute(
        commandAsBash(commandLine, stdoutFile, stderrFile), outputDir,
        outputDir, devnullFile, devnullFile, bamPath.toFile(), sampleSheetFile,
        outputDir);

    long endTime = System.currentTimeMillis();

    if (exitValue != 0) {
      throw new IOException(
          "Error while running dorado, exit code is: " + exitValue);
    }

    info(logger, runId, "Successful creating summary in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));
  }

  /**
   * Create the command the Dorado line arguments.
   * @param inputPath input path
   * @param modelsPath path for models
   * @param runConf run configuration
   * @return a list with Dorado arguments
   * @throws Aozan3Exception if configuration is invalid
   */
  private static List<String> createDoradoBasecallerCommandLine(Path inputPath,
      Path modelsPath, RunConfiguration runConf) throws Aozan3Exception {

    requireNonNull(inputPath);
    requireNonNull(modelsPath);
    requireNonNull(runConf);

    // Get parameter values
    String finalCommandPath = runConf.get(CONF_PREFIX + ".path", "dorado");

    //  List arg
    List<String> result = new ArrayList<>();

    result.add(finalCommandPath);

    // Command
    result.add("basecaller");

    // Cuda device
    result.add("--device");
    result.add(runConf.get(CONF_PREFIX + ".cuda.device", "auto"));

    result.add("--recursive");

    // Barcodes
    if (runConf.containsKey(CONF_PREFIX + ".kit.name")
        && !runConf.get(CONF_PREFIX + ".kit.name").startsWith("SQK-RNA")) {
      result.add("--kit-name");
      result.add(runConf.get(CONF_PREFIX + ".kit.name"));

      if (!runConf.getBoolean(CONF_PREFIX + ".trim.barcodes", true)) {
        result.add("--no-trim");
      }

      if (runConf.containsKey(CONF_PREFIX + ".sample.sheet.path")) {
        result.add("--sample-sheet");
        result.add(runConf.get(CONF_PREFIX + ".sample.sheet.path"));
      }

    }

    // Min qscore
    if (runConf.containsKey(CONF_PREFIX + ".min.qscore")) {
      result.add("--min-qscore");
      result.add(runConf.get(CONF_PREFIX + ".min.qscore"));
    }

    // Model selection complex
    if (new Version(runConf.get(CONF_PREFIX + ".dorado.version"))
        .greaterThanOrEqualTo(new Version("0.5.0"))) {
      result.add(runConf.get(CONF_PREFIX + ".model.selection.complex",
          DEFAULT_MODEL_SELECTION_COMPLEX));
    } else {
      result.add(getModelPath(modelsPath, runConf).toString());
    }

    // Batch size
    if (runConf.containsKey(CONF_PREFIX + ".batch.size")) {
      result.add("--batchsize");
      result.add(runConf.get(CONF_PREFIX + ".batch.size"));
    }

    // Chunck size
    if (runConf.containsKey(CONF_PREFIX + ".chunk.size")) {
      result.add("--chunksize");
      result.add(runConf.get(CONF_PREFIX + ".chunk.size"));
    }

    // PolyA estimation
    if (runConf.containsKey(CONF_PREFIX + ".estimate.poly.a")) {
      result.add("--estimate-poly-a");
      result.add(runConf.get(CONF_PREFIX + ".estimate.poly.a"));
    }

    result.add(inputPath.toString());

    return result;
  }

  /**
   * Get the model path.
   * @param modelsPath path of the models
   * @param runConf run configuration
   * @return the model path
   * @throws Aozan3Exception if an error occurs when getting the model path
   */
  private static Path getModelPath(Path modelsPath, RunConfiguration runConf)
      throws Aozan3Exception {

    requireNonNull(modelsPath);
    requireNonNull(modelsPath);

    Path result = null;

    if (!runConf.containsKey(CONF_PREFIX + ".model.path")) {

      if (!runConf.containsKey(CONF_PREFIX + ".flow.cell")) {
        throw new Aozan3Exception("Flow cell missing in configuration");
      }

      if (!runConf.containsKey(CONF_PREFIX + ".kit")) {
        throw new Aozan3Exception("Kit missing in configuration");
      }

      String analyte = "dna";
      String flowCellRef = runConf.get(CONF_PREFIX + ".flow.cell");
      String kit = runConf.get(CONF_PREFIX + ".kit");
      String speed = runConf.get(CONF_PREFIX + ".speed", "");
      String modelType = "sup";

      result = new BasecallingModelSelector(modelsPath).withAnalyteType(analyte)
          .withFlowcellReference(flowCellRef).withKitReference(kit)
          .withTranslocationSpeed(speed).withModelType(modelType)
          .withNoModification().select();

      if (result == null) {
        throw new Aozan3Exception(
            "Unable to find a suiatable model for the following parameters: "
                + "analyte=" + analyte + ", flowCellRef=" + flowCellRef
                + ", kit=" + kit + ", speed=" + speed + ", modelType="
                + modelType);
      }

    } else {
      result = runConf.getPath(CONF_PREFIX + ".model.path");
    }

    if (!Files.isDirectory(result)) {
      throw new Aozan3Exception("Unable to find model: " + result);
    }

    return result;
  }

  /**
   * Convert a command line to a command executed by bash.
   * @param cmd original command
   * @param stdout stdout file
   * @param stderr stderr file
   * @return a list of arguments to launch Bash to execute the original command
   */
  private static List<String> commandAsBash(List<String> cmd, File stdout,
      File stderr) {

    if (cmd == null) {
      return null;
    }

    List<String> result = new ArrayList<>();
    result.add("bash");
    result.add("-c");

    StringBuilder sb = new StringBuilder();
    for (String s : cmd) {
      sb.append(StringUtils.bashEscaping(s));
      sb.append(' ');
    }
    sb.append(" > ");
    sb.append(StringUtils.bashEscaping(stdout.getAbsolutePath()));
    sb.append(" 2> ");
    sb.append(StringUtils.bashEscaping(stderr.getAbsolutePath()));

    result.add(sb.toString());

    return result;
  }

  /**
   * Parse Dorado version from Dorado output.
   * @param lines lines to parse
   * @return a String with the Dorado version
   */
  private static String parseDoradoVersion(List<String> lines) {

    if (lines == null || lines.isEmpty()) {
      return null;
    }

    if (!lines.isEmpty()) {
      return lines.get(lines.size() - 1);
    }

    return null;
  }

  /**
   * Delete a directory and its content recursively.
   * @param path Path of the directory to remove
   * @throws IOException if an error occurs while removing the directory
   */
  private static void deleteDirectory(Path path) throws IOException {

    if (!Files.isDirectory(path)) {
      throw new IOException(
          "Path to remove does not exists or is not a directory: " + path);
    }

    Files.walk(path).map(Path::toFile).sorted((o1, o2) -> -o1.compareTo(o2))
        .forEach(File::delete);
  }

  private static Path generateStrictSampleSheet(Path inputSampleSheet,
      Path outputDirectory) throws Aozan3Exception {

    SampleSheet sampleSheet;

    // Read the input sample sheet
    try (
        SampleSheetReader reader = new SampleSheetCSVReader(inputSampleSheet)) {
      reader.addAllowedFields("sample_ref", "project");
      sampleSheet = reader.read();
    } catch (IOException e) {
      throw new Aozan3Exception("Error while reading sample sheet: "
          + inputSampleSheet + " caused by: " + e.getMessage());
    }

    // Remove non standard fields
    sampleSheet.removeOtherFields();
    sampleSheet.removeBarcodeDescription();

    // Do not generate a sample sheet if there is no barcode
    if (sampleSheet.getBarcodes().isEmpty()) {
      return null;
    }

    File outputSampleSheet =
        new File(outputDirectory.toFile(), "samplesheet.csv");

    try (SampleSheetWriter writer =
        new SampleSheetCSVWriter(outputSampleSheet)) {
      writer.writer(sampleSheet);
    } catch (IOException e) {
      throw new Aozan3Exception("Error while writing temporary sample sheet: "
          + inputSampleSheet + " caused by: " + e.getMessage());
    }

    return outputSampleSheet.toPath();
  }

  //
  // Main
  //

  /**
   * @param inputTar input tar file
   * @param outputPath output directory
   * @param modelsPath directory with models for Dorado
   * @param sampleSheetPath sample sheet path
   * @param runId run Id
   * @param doradoVersion dorado version
   * @param tmpPath tempoary directory
   * @param flowcellType flowcell type
   * @param kit kit type
   * @param barcodeKits barcode kit used
   * @param trimBarcodes true if barcode must be trimmed
   * @param minQscore minimal Q score
   * @param model model to use
   * @param cudaDevice CUDA device
   * @param batchSize batch size
   * @param chunkSize chunk size
   * @param keepTemporaryFiles true to keep temporary files
   * @param logger logger to use
   * @throws Aozan3Exception if an error occurs while executing the basecalling
   */
  public static void run(Path inputTar, Path outputPath, String runId,
      Path tmpPath, String cudaDevice, boolean keepTemporaryFiles,
      Configuration doradoConf, GenericLogger logger) throws Aozan3Exception {

    requireNonNull(inputTar);
    requireNonNull(outputPath);
    requireNonNull(runId);
    requireNonNull(tmpPath);
    requireNonNull(cudaDevice);
    requireNonNull(doradoConf);
    requireNonNull(logger);

    if (runId.trim().isEmpty()) {

      if (Files.isDirectory(inputTar)) {
        runId = inputTar.toFile().getName();
      } else {
        runId = inputTar.toFile().getName().replace(".tar", "");
      }
    }

    if (runId.trim().isEmpty()) {
      throw new IllegalArgumentException("runId cannot be empty");
    }

    if (!Files.isDirectory(outputPath)) {
      throw new Aozan3Exception(
          "Output directory does not exists: " + outputPath);
    }

    if (!Files.isDirectory(tmpPath)) {
      throw new Aozan3Exception(
          "Temporary directory does not exists: " + tmpPath);
    }

    if (!Files.isRegularFile(inputTar) && !Files.isDirectory(inputTar)) {
      throw new Aozan3Exception(
          "Input file/directory does not exists: " + inputTar);
    }

    RunConfiguration runConf = new RunConfiguration();
    runConf.set(doradoConf);

    String doradoVersion =
        runConf.get(CONF_PREFIX + ".version", DEFAULT_DORADO_VERSION);

    runConf.set("tmp.dir", tmpPath.toString());
    runConf.set(CONF_PREFIX + ".use.docker", "true");
    runConf.set(CONF_PREFIX + ".dorado.version", doradoVersion);
    runConf.set(CONF_PREFIX + ".docker.image",
        DEFAULT_DORADO_DOCKER_REPO + ':' + doradoVersion);

    if (!cudaDevice.trim().isEmpty()) {
      runConf.set(CONF_PREFIX + ".cuda.device", cudaDevice.trim());
    }

    if (runConf.containsKey(CONF_PREFIX + ".model")) {

      if (!runConf.containsKey(CONF_PREFIX + ".models.path")) {
        throw new Aozan3Exception(
            "The directory for the models has not been set ("
                + CONF_PREFIX + ".models.path).");
      }
      runConf.set(CONF_PREFIX + ".model.path",
          new File(runConf.get(CONF_PREFIX + ".models.path"),
              runConf.get(CONF_PREFIX + ".model")).toString());

    }

    if (new Version(doradoVersion).greaterThanOrEqualTo(new Version("0.5.0"))
        && !runConf.containsKey(CONF_PREFIX + ".model.selection.complex")) {

      runConf.set(CONF_PREFIX + ".model.selection.complex",
          DEFAULT_MODEL_SELECTION_COMPLEX);
    }

    System.out.println("### START ###");

    RunId basecallingRunId = new RunId(runId);

    if (Files.isRegularFile(inputTar)) {
      tarPipeline(basecallingRunId, inputTar, outputPath, tmpPath, runConf,
          keepTemporaryFiles, logger);
    } else {
      directoryPipeline(basecallingRunId, inputTar, outputPath, tmpPath,
          runConf, keepTemporaryFiles, logger);
    }

    System.out.println("### END ###");
  }

}
