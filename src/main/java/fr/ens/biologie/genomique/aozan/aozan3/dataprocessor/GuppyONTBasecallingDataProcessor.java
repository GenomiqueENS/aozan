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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataLocation;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.DataType;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.SimpleDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.util.CopyAndMergeGuppyOutput;
import fr.ens.biologie.genomique.aozan.aozan3.util.UnTar;
import fr.ens.biologie.genomique.kenetre.log.DummyLogger;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;

/**
 * This class implements a Guppy data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class GuppyONTBasecallingDataProcessor implements DataProcessor {

  public static final String PROCESSOR_NAME = "guppy_basecaller";
  private static final String CONF_PREFIX = "guppy";
  private static final String DEFAULT_GUPPY_VERSION = "5.0.16";

  private static final boolean USE_DOCKER = true;

  private GenericLogger logger = new DummyLogger();

  private DataStorage outputStorage;
  private String dataDescription;
  private final RunConfiguration conf = new RunConfiguration();
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
   * Convienient method to launch guppy outside Aozan workflow.
   * @param runId run Id
   * @param inputTarPath input tar with Fast5 files path
   * @param outputPath output path
   * @param tmpPath temporary directory path
   * @param runConf run configuration
   * @param logger Aozan logger
   * @throws Aozan3Exception if an error occurs while executing Guppy
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
      System.out.println("* Uncompress FAST5 Tar file");
      Path inputDirPath = Files.createTempDirectory(tmpPath, "raw-fast5-");
      UnTar untar = new UnTar(inputTarPath, inputDirPath);
      untar.execute();

      directoryPipeline(runId, inputDirPath, outputPath, tmpPath, runConf,
          keepTemporaryFiles, logger);

      // Delete temporary untarred FAST5 tar
      if (!keepTemporaryFiles) {
        System.out.println("* Remove uncompressed FAST5 files");
        deleteDirectory(inputDirPath);
      }

    } catch (IOException e) {
      throw new Aozan3Exception(e);
    }
  }

  /**
   * Convienient method to launch guppy outside Aozan workflow.
   * @param runId run Id
   * @param inputDirPath input directory with Fast5 files path
   * @param outputPath output path
   * @param tmpPath temporary directory path
   * @param runConf run configuration
   * @param logger Aozan logger
   * @throws Aozan3Exception if an error occurs while executing Guppy
   */
  private static void directoryPipeline(RunId runId, Path inputDirPath,
      Path outputPath, Path tmpPath, final RunConfiguration runConf,
      boolean keepTemporaryFiles, GenericLogger logger) throws Aozan3Exception {

    requireNonNull(inputDirPath);
    requireNonNull(outputPath);
    requireNonNull(tmpPath);
    requireNonNull(runConf);

    try {

      // Launch Guppy
      System.out.println("* Launch Guppy");
      Path outputDirPath = Files.createTempDirectory(tmpPath, "fastq-");
      launchGuppy(runId, inputDirPath, outputDirPath, runConf, logger);

      // Copy and merge FAST5 files to the output directory
      Path mergedFastqPath = Paths.get(outputPath.toString(), runId.getId());
      CopyAndMergeGuppyOutput merger =
          new CopyAndMergeGuppyOutput(outputDirPath, mergedFastqPath);
      merger.setFastqMerging(runConf.getBoolean("guppy.merge.fastq", true));
      merger.setLogMerging(runConf.getBoolean("guppy.merge.logs", true));
      merger.setCompressSequencingSummary(
          runConf.getBoolean("guppy.compress.sequencing.summary", false));
      merger.setCompressTelemetry(
          runConf.getBoolean("guppy.compress.sequencing.telemetry", false));
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
   * Launch Guppy.
   * @param runId run Id
   * @param inputPath input path
   * @param outputPath output path
   * @param runConf run configuration
   * @param logger Aozan logger
   * @throws Aozan3Exception if run configuration is invalid
   * @throws IOException if an error occurs while executing Guppy
   */
  private static void launchGuppy(RunId runId, Path inputPath, Path outputPath,
      RunConfiguration runConf, GenericLogger logger)
      throws Aozan3Exception, IOException {

    // Define external tool
    ExternalTool tool =
        new ExternalTool("guppy", runConf.getBoolean("guppy.use.docker", false),
            runConf.get("guppy.docker.image", ""),
            runConf.getBoolean("guppy.use.docker", false), logger);

    // Get demultiplexing tool version
    String toolVersion = tool.getToolVersion(runId, runConf.get("tmp.dir"),
        asList(runConf.get("guppy.path", "guppy_basecaller"), "--version"),
        false, GuppyONTBasecallingDataProcessor::parseGuppyVersion);

    // TODO get guppy version

    // Create command line
    List<String> commandLine =
        createGuppyCommandLine(inputPath, outputPath, runConf);

    File outputDir = outputPath.toFile();

    // define stdout and stderr files
    File stdoutFile =
        new File(outputDir, "guppy_output_" + runId.getId() + ".out");
    File stderrFile =
        new File(outputDir, "guppy_output_" + runId.getId() + ".err");

    info(logger, runId, "Guppy: " + toolVersion);
    info(logger, runId, "Demultiplexing using the following command line: "
        + String.join(" ", commandLine));

    System.out.println("Guppy version: " + toolVersion);
    System.out.println("Command line: " + commandLine);

    long startTime = System.currentTimeMillis();

    final int exitValue =
        tool.newSimpleProcess(runId, true).execute(commandLine, outputDir,
            outputDir, stdoutFile, stderrFile, inputPath.toFile(), outputDir);

    long endTime = System.currentTimeMillis();

    if (exitValue != 0) {
      throw new IOException(
          "Error while running guppy, exit code is: " + exitValue);
    }

    info(logger, runId, "Successful demultiplexing in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));
  }

  /**
   * Create the command the Guppy line arguments.
   * @param inputPath input path
   * @param outputPath output path
   * @param runConf run configuration
   * @return a list with gGuppy arguments
   * @throws Aozan3Exception if configuration is invalid
   */
  private static List<String> createGuppyCommandLine(Path inputPath,
      Path outputPath, RunConfiguration runConf) throws Aozan3Exception {

    requireNonNull(inputPath);
    requireNonNull(outputPath);
    requireNonNull(runConf);

    // TODO move this
    if (!runConf.containsKey("guppy.config")) {
      if (!runConf.containsKey("guppy.kit")) {
        throw new Aozan3Exception("Kit missing in configuration");
      }

      // TODO move this
      if (!runConf.containsKey("guppy.flowcell")) {
        throw new Aozan3Exception("Flowcell missing in configuration");
      }
    }

    // Get parameter values
    String finalCommandPath = runConf.get("guppy.path", "guppy_basecaller");

    //  List arg
    List<String> result = new ArrayList<>();

    result.add(finalCommandPath);

    // Input path
    result.add("--input_path");
    result.add(inputPath.toString());
    result.add("--recursive");

    // Output path
    result.add("--save_path");
    result.add(outputPath.toString());

    // Compress Fastq
    result.add("--compress_fastq");

    // Fast5 output
    if (runConf.containsKey("guppy.fast5.output")) {
      result.add("--fast5_out");
    }

    // Config
    if (runConf.containsKey("guppy.config")) {
      result.add("--config");
      result.add(runConf.get("guppy.config"));
    } else {
      // Kit
      result.add("--kit");
      result.add(runConf.get("guppy.kit"));

      // Flowcell
      result.add("--flowcell");
      result.add(runConf.get("guppy.flowcell"));
    }

    // Barcodes
    if (runConf.containsKey("guppy.barcode.kits")) {
      result.add("--barcode_kits");
      result.add(runConf.get("guppy.barcode.kits"));

      if (runConf.getBoolean("guppy.trim.barcodes", false)) {
        result.add("--trim_barcode");
      }
    }

    if (runConf.containsKey("guppy.min.qscore")) {
      result.add("--min_qscore");
      result.add(runConf.get("guppy.min.qscore"));
    }

    // Cuda device
    result.add("--device");
    result.add(runConf.get("guppy.cuda.device", "auto"));

    // GPU runner per device
    if (runConf.containsKey("guppy.gpu.runners.per.device")) {
      result.add("--gpu_runners_per_device");
      result.add(runConf.get("guppy.gpu.runners.per.device"));
    }

    // Chuncks per runner
    if (runConf.containsKey("guppy.chunks.per.runner")) {
      result.add("--chunks_per_runner");
      result.add(runConf.get("guppy.chunks.per.runner"));
    }

    // Caller
    result.add("--num_callers");
    result.add(runConf.get("guppy.num.callers", "4"));

    result.add("--records_per_fastq");
    result.add(runConf.get("guppy.records.per.fastq", "4000"));

    result.add("--disable_pings");

    result.add("--calib_detect");

    return result;
  }

  /**
   * Parse guppy version from Guppy output.
   * @param lines lines to parse
   * @return a String with the Guppy version
   */
  private static String parseGuppyVersion(List<String> lines) {

    if (lines == null || lines.isEmpty()) {
      return null;
    }

    String firstLine = lines.get(0);

    if (!firstLine.contains("Version ")) {
      return null;
    }

    return firstLine.substring("Version ".length());
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

  //
  // main
  //

  public static void run(Path inputTar, Path outputPath, String runId,
      String guppyVersion, Path tmpPath, String flowcellType, String kit,
      String barcodeKits, boolean trimBarcodes, String minQscore, String config,
      String cudaDevice, int gpuRunnersPerDevice, int chunksPerRunner,
      boolean fast5Output, boolean keepTemporaryFiles, GenericLogger logger)
      throws Aozan3Exception {

    requireNonNull(inputTar);
    requireNonNull(outputPath);
    requireNonNull(runId);
    requireNonNull(guppyVersion);
    requireNonNull(tmpPath);
    requireNonNull(flowcellType);
    requireNonNull(kit);
    requireNonNull(barcodeKits);
    requireNonNull(config);
    requireNonNull(cudaDevice);
    requireNonNull(logger);

    if (runId.trim().isEmpty()) {

      if (Files.isDirectory(inputTar)) {
        runId = inputTar.toFile().getName();
      } else {
        runId = inputTar.toFile().getName().replace(".tar", "");
      }
    }

    if (flowcellType.trim().isEmpty()) {
      flowcellType = "FLO-MIN106";
    }

    if (kit.trim().isEmpty()) {
      flowcellType = "SQK-PBK004";
    }

    if (runId.trim().isEmpty()) {
      throw new IllegalArgumentException("runId cannot be empty");
    }

    if (guppyVersion.trim().isEmpty()) {
      guppyVersion = DEFAULT_GUPPY_VERSION;
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
    runConf.set("tmp.dir", tmpPath.toString());
    runConf.set("guppy.use.docker", "true");
    runConf.set("guppy.docker.image",
        "genomicpariscentre/guppy-gpu:" + guppyVersion);
    // runConf.set("guppy.flowcell.sn", "FA035147");

    if (!cudaDevice.trim().isEmpty()) {
      runConf.set("guppy.cuda.device", cudaDevice.trim());
    }

    if (gpuRunnersPerDevice > 0) {
      runConf.set("guppy.gpu.runners.per.device", gpuRunnersPerDevice);
    }

    if (chunksPerRunner > 0) {
      runConf.set("guppy.chunks.per.runner", chunksPerRunner);
    }

    if (!config.trim().isEmpty()) {
      runConf.set("guppy.config", config.trim());
    } else {
      runConf.set("guppy.kit", kit.trim());
      runConf.set("guppy.flowcell", flowcellType.trim());
    }

    if (!barcodeKits.trim().isEmpty()) {
      runConf.set("guppy.barcoding", "true");
      runConf.set("guppy.barcode.kits", barcodeKits.trim());

      if (trimBarcodes) {
        runConf.set("guppy.trim.barcodes", "true");
      }
    }

    if (!minQscore.trim().isEmpty()) {
      runConf.set("guppy.min.qscore", minQscore);
    }

    if (fast5Output) {
      runConf.set("guppy.fast5.output", "true");
    }

    System.out.println("### START ###");

    RunId basecallingRunId = new RunId(runId + "-guppy-" + guppyVersion);

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
