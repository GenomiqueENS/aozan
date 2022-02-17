package fr.ens.biologie.genomique.aozan.aozan3.legacy;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.DefaultRunIdGenerator;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.Aozan2QCDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.Bcl2FastqIlluminaDemuxDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.BclConvertIlluminaDemuxDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.IlluminaSyncDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprovider.IlluminaProcessedRunDataProvider;
import fr.ens.biologie.genomique.aozan.aozan3.dataprovider.IlluminaRawRunDataProvider;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLoggerFactory;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.Recipe;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.Step;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.IlluminaSamplesheetRunConfigurationProvider;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.RunConfigurationProvider;

/**
 * This class define recipes that perform like Aozan 2 steps.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class LegacyRecipes {

  private Recipe syncStepRecipe;
  private Recipe demuxStepRecipe;
  private Recipe qcStepRecipe;
  private Path varPath;
  private Path mainLockPath;
  private Map<Recipe, Path> outputPaths = new HashMap<>();

  //
  // Getters
  //

  /**
   * Get the path for var files.
   * @return the path for var files
   */
  public Path getVarPath() {

    return this.varPath;
  }

  /**
   * Get the main lock path.
   * @return the main lock path
   */
  public Path getMainLockPath() {

    return this.mainLockPath;
  }

  /**
   * Get the synchronization step recipe.
   * @return the synchronization step recipe or null is not enable
   */
  public Recipe getSyncStepRecipe() {

    return this.syncStepRecipe;
  }

  /**
   * Get the demux step recipe.
   * @return the demux step recipe or null is not enable
   */
  public Recipe getDemuxStepRecipe() {

    return this.demuxStepRecipe;
  }

  /**
   * Get the QC step recipe.
   * @return the QC step recipe or null is not enable
   */
  public Recipe getQCStepRecipe() {

    return this.qcStepRecipe;
  }

  /**
   * Get the output path of a recipe.
   * @param recipe the recipe
   * @return the output path of the recipe
   */
  public Path getOutputPath(Recipe recipe) {

    Objects.requireNonNull(recipe);

    if (!this.outputPaths.containsKey(recipe)) {
      throw new IllegalArgumentException("Unknown recipe: " + recipe.getName());
    }

    return this.outputPaths.get(recipe);
  }

  //
  // Other methods
  //

  /**
   * Convert Aozan 1 settings key names to aozan 2 key names.
   * @param conf aozan 2 configuration
   * @return a Configuration object with Aozan 2 setting key names
   */
  private Configuration aozan1Compatibility(Configuration conf) {

    Configuration result = new Configuration();

    Map<String, String> table = new HashMap<>();

    table.put("lock.file", "aozan.lock.file");

    table.put("casava.design.format",
        Settings.BCL2FASTQ_SAMPLESHEET_FORMAT_KEY);
    table.put("casava.samplesheet.format",
        Settings.BCL2FASTQ_SAMPLESHEET_FORMAT_KEY);
    table.put("casava.design.prefix.filename",
        Settings.BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY);
    table.put("casava.samplesheet.prefix.filename",
        Settings.BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY);
    table.put("casava.designs.path", Settings.BCL2FASTQ_SAMPLESHEETS_PATH_KEY);
    table.put("casava.samplesheets.path",
        Settings.BCL2FASTQ_SAMPLESHEETS_PATH_KEY);

    table.put("casava.adapter.fasta.file.path",
        Settings.BCL2FASTQ_ADAPTER_FASTA_FILE_PATH_KEY);
    table.put("casava.additionnal.arguments",
        Settings.BCL2FASTQ_ADDITIONNAL_ARGUMENTS_KEY);
    table.put("casava.compression", Settings.BCL2FASTQ_COMPRESSION_KEY);
    table.put("casava.compression.level",
        Settings.BCL2FASTQ_COMPRESSION_LEVEL_KEY);
    table.put("casava.fastq.cluster.count",
        Settings.BCL2FASTQ_FASTQ_CLUSTER_COUNT_KEY);
    table.put("casava.mismatches", Settings.BCL2FASTQ_MISMATCHES_KEY);
    table.put("casava.path", Settings.BCL2FASTQ_PATH_KEY);

    table.put("casava.threads", Settings.BCL2FASTQ_THREADS_KEY);
    table.put("casava.with.failed.reads",
        Settings.BCL2FASTQ_WITH_FAILED_READS_KEY);
    table.put("casava.design.generator.command",
        Settings.BCL2FASTQ_SAMPLESHEET_GENERATOR_COMMAND_KEY);
    table.put("casava.samplesheet.generator.command",
        Settings.BCL2FASTQ_SAMPLESHEET_GENERATOR_COMMAND_KEY);
    table.put("demux.use.docker.enable", Settings.BCL2FASTQ_USE_DOCKER_KEY);

    for (Map.Entry<String, String> e : conf.toMap().entrySet()) {
      String key =
          table.containsKey(e.getKey()) ? table.get(e.getKey()) : e.getKey();
      result.set(key, e.getValue());
    }

    return result;
  }

  private static boolean parseCommonConfiguration(Configuration conf,
      AozanLogger logger, Configuration aozan2Conf) {

    // Check if Aozan is enabled
    if (!aozan2Conf.getBoolean("aozan.enable")) {
      return false;
    }

    // Mode debug
    // aozan.debug=False

    // Sequencer names
    for (Map.Entry<String, String> e : aozan2Conf.toMap().entrySet()) {

      String key = e.getKey();

      if (key != null && key.startsWith("sequencer.name.")) {
        conf.set(e.getKey(), e.getValue());
      }
    }

    // Enable email sending
    setSetting(conf, aozan2Conf, "send.mail", "send.mail", "False");

    // SMTP server name
    setSetting(conf, aozan2Conf, "smtp.server", "mail.smtp.host");

    // SMTP server port
    setSetting(conf, aozan2Conf, "smtp.port", "mail.smtp.port", "25");

    // Use StartTLS to connect to the SMTP server
    setSetting(conf, aozan2Conf, "smtp.use.starttls",
        "mail.smtp.starttls.enable", "False");

    // Use SSL to connect to the SMTP server
    setSetting(conf, aozan2Conf, "smtp.use.ssl", "mail.smtp.ssl.enable",
        "False");

    // Login to use for the connection to the SMTP server
    // #smtp.login=yourlogin

    // Password to use for the connection to the SMTP server
    // #smtp.password=yourpassword

    // From field of email
    setSetting(conf, aozan2Conf, "mail.from", "mail.from", "");

    // To field of email
    // mail.to=me@example.com
    setSetting(conf, aozan2Conf, "mail.to", "mail.to", "");

    // Email recipient when an error occurs during Aozan (Optional)
    // mail.error.to=aozan-errors@example.com

    return true;
  }

  private Recipe createSyncStepRecipe(Configuration conf, AozanLogger logger,
      Configuration aozan2Conf) throws Aozan3Exception {

    // Check if the sync step is enabled
    if (!aozan2Conf.getBoolean("sync.step", true)) {
      return null;
    }

    Recipe recipe = new Recipe("sync", "Sync step", conf, logger);

    boolean inProgress = aozan2Conf.getBoolean("sync.continuous.sync", false);
    final String inputStoragePrefix = "nasStorage";
    final String outputStorage = "bclStorage";

    // Set input storages
    createStorages(recipe, inputStoragePrefix,
        aozan2Conf.get("hiseq.data.path"));

    // Set input storages as run providers
    for (String storageName : recipe.getStorages().names()) {
      recipe.addDataProvider(IlluminaRawRunDataProvider.PROVIDER_NAME,
          storageName, inProgress, conf);
    }

    // Set output storage
    Path bclPath = Paths.get(aozan2Conf.get("bcl.data.path").trim());
    recipe.addStorage(outputStorage, new DataStorage("local", bclPath));
    this.outputPaths.put(recipe, bclPath);

    // Run configuration provider
    RunConfigurationProvider runConfProvider =
        createRunConfProvider(recipe, aozan2Conf);

    // Define step configuration
    Configuration stepConf = new Configuration();

    Step syncStep = new Step(recipe, "syncstep",
        IlluminaSyncDataProcessor.PROCESSOR_NAME, outputStorage, stepConf,
        runConfProvider, new DefaultRunIdGenerator());

    recipe.addStep(syncStep);

    return recipe;
  }

  private Recipe createDemuxStep(Configuration conf, AozanLogger logger,
      Configuration aozan2Conf) throws Aozan3Exception {

    // Check if the demux step is enabled
    if (!aozan2Conf.getBoolean("demux.step", true)) {
      return null;
    }

    Recipe recipe = new Recipe("demux", "Demux step", conf, logger);

    final String outputStorageName = "fastqStorage";

    // Create input BCL storages
    createInputBclStorages(conf, recipe, aozan2Conf);

    // Set output storage
    Path fastqPath = Paths.get(aozan2Conf.get("fastq.data.path").trim());
    recipe.addStorage(outputStorageName, new DataStorage("local", fastqPath));
    this.outputPaths.put(recipe, fastqPath);

    // Run configuration provider
    RunConfigurationProvider runConfProvider =
        createRunConfProvider(recipe, aozan2Conf);

    // Define step configuration
    Configuration stepConf = new Configuration();

    // Select the demux tool to use
    String demuxProcessorName;
    switch (conf.get("demux.tool.name", "bcl2fastq").trim().toLowerCase()) {

    case "bclconvert":
      demuxProcessorName = BclConvertIlluminaDemuxDataProcessor.PROCESSOR_NAME;
      break;

    default:
    case "bcl2fastq":
      demuxProcessorName = Bcl2FastqIlluminaDemuxDataProcessor.PROCESSOR_NAME;
      break;

    }

    Step demuxStep =
        new Step(recipe, "demuxstep", demuxProcessorName, outputStorageName,
            stepConf, runConfProvider, new DefaultRunIdGenerator());

    recipe.addStep(demuxStep);

    return recipe;
  }

  private Recipe createQCStep(Configuration conf, AozanLogger logger,
      Configuration aozan2Conf) throws Aozan3Exception {

    // Check if the qc step is enabled
    if (!aozan2Conf.getBoolean("qc.step", true)) {
      return null;
    }

    Recipe recipe = new Recipe("qc", "QC step", conf, logger);

    boolean inProgress = false;
    final String inputFastqStorageName = "fastqStorage";
    final String outputStorageName = "qcStorage";

    // Create input BCL storages
    createInputBclStorages(conf, recipe, aozan2Conf);

    // Set fastq input storage
    recipe.addStorage(inputFastqStorageName, new DataStorage("local",
        aozan2Conf.get("fastq.data.path").trim(), null));
    recipe.addDataProvider(IlluminaProcessedRunDataProvider.PROVIDER_NAME,
        inputFastqStorageName, inProgress, conf);

    // Set output storage
    Path qcPath = Paths.get(aozan2Conf.get("reports.data.path").trim());
    recipe.addStorage(outputStorageName, new DataStorage("local", qcPath));
    this.outputPaths.put(recipe, qcPath);

    // Run configuration provider
    RunConfigurationProvider runConfProvider =
        createRunConfProvider(recipe, aozan2Conf);

    // Define step configuration
    Configuration stepConf = new Configuration(aozan2Conf);

    Step qcStep = new Step(recipe, "qcstep",
        Aozan2QCDataProcessor.PROCESSOR_NAME, outputStorageName, stepConf,
        runConfProvider, new DefaultRunIdGenerator());

    recipe.addStep(qcStep);

    return recipe;
  }

  //
  // Utility methods
  //

  private static RunConfigurationProvider createRunConfProvider(Recipe recipe,
      Configuration aozan2Conf) throws Aozan3Exception {

    // Run configuration provider
    RunConfigurationProvider result =
        new IlluminaSamplesheetRunConfigurationProvider();

    // Set run configuration provider conf
    Configuration stepConf = new Configuration();
    setSetting(stepConf, aozan2Conf, "bcl2fastq.samplesheet.path",
        "samplesheet.path");
    setSetting(stepConf, aozan2Conf, "bcl2fastq.samplesheet.format",
        "samplesheet.format", "xls");
    setSetting(stepConf, aozan2Conf, "bcl2fastq.samplesheet.prefix.filename",
        "samplesheet.prefix.filename", "samplesheet");
    setSetting(stepConf, aozan2Conf, "bcl2fastq.samplesheet.generator.command",
        "samplesheet.generator.command");
    setSetting(stepConf, aozan2Conf, "index.sequences", "index.sequences");

    result.init(stepConf, recipe.getLogger());

    return result;
  }

  /**
   * Create input BCL storages.
   * @param conf configuration
   * @param recipe the recipe
   * @param aozan2Conf Aozan2 configuration
   * @throws Aozan3Exception if an error occurs while creating the storages
   */
  private static void createInputBclStorages(Configuration conf, Recipe recipe,
      Configuration aozan2Conf) throws Aozan3Exception {

    boolean inProgress = false;
    final String inputStorageName = "bclStorage";

    // Set input storages
    if (aozan2Conf.getBoolean("demux.use.hiseq.output", false)) {

      // Without sync step

      createStorages(recipe, "nasStorage", aozan2Conf.get("hiseq.data.path"));

      // Set input storages as run providers
      for (String storageName : recipe.getStorages().names()) {
        recipe.addDataProvider(IlluminaRawRunDataProvider.PROVIDER_NAME,
            storageName, inProgress, conf);
      }
    } else {

      // With sync step

      recipe.addStorage(inputStorageName, new DataStorage("local",
          aozan2Conf.get("bcl.data.path").trim(), null));
      recipe.addDataProvider(IlluminaRawRunDataProvider.PROVIDER_NAME,
          inputStorageName, inProgress, conf);
    }

  }

  private static void setSetting(Configuration conf, Configuration aozan2Conf,
      String oldKey, String newKey) {

    setSetting(conf, aozan2Conf, oldKey, newKey, null);
  }

  private static void setSetting(Configuration conf, Configuration aozan2Conf,
      String oldKey, String newKey, String defaultValue) {

    if (aozan2Conf.containsKey(oldKey)) {

      conf.set(newKey, aozan2Conf.get(oldKey));
    } else {

      if (!conf.containsKey(newKey) && defaultValue != null) {
        conf.set(newKey, defaultValue);
      }

    }
  }

  /**
   * Create storages.
   * @param recipe recipe
   * @param prefix storage name prefix
   * @param storageString storage string
   */
  private static void createStorages(Recipe recipe, String prefix,
      String storageString) {

    List<DataStorage> result = new ArrayList<>();

    for (String s : storageString.split(":")) {

      result.add(new DataStorage("local", Paths.get(s.trim())));
    }

    int i = 0;
    for (DataStorage storage : result) {
      String storageName = prefix + (result.size() > 1 ? (++i) : "");
      recipe.addStorage(storageName, storage);
    }
  }

  private static String checkAndGetSetting(Configuration aozan2Conf, String key)
      throws Aozan3Exception {

    if (aozan2Conf.containsKey(key)) {
      return aozan2Conf.get(key);
    }
    throw new Aozan3Exception(
        "The \"" + key + "\" setting has not been defined");
  }

  private static AozanLogger createLogger(Configuration aozan2Conf,
      AozanLogger currentLogger) throws Aozan3Exception {

    if (aozan2Conf.containsKey("aozan.log.path")) {

      Configuration logConf = new Configuration();
      logConf.set("aozan.logger", "file");
      logConf.set("aozan.log", aozan2Conf.get("aozan.log.path"));

      // TODO Handle log level in aozan.log.level setting

      return AozanLoggerFactory.newLogger(logConf, currentLogger);
    }

    return currentLogger;
  }

  //
  // Constructor
  //

  public LegacyRecipes(Configuration conf, AozanLogger logger,
      Path aozan2ConfFile) throws Aozan3Exception {

    requireNonNull(aozan2ConfFile);

    Configuration aozan2Conf = new Configuration();
    aozan2Conf.load(aozan2ConfFile, true);
    aozan2Conf = aozan1Compatibility(aozan2Conf);

    if (!parseCommonConfiguration(conf, logger, aozan2Conf)) {

      // Aozan is not enabled, nothing to do
      return;
    }

    // Change logger if required in Aozan 2 conf
    logger = createLogger(aozan2Conf, logger);

    // Get the var path
    this.varPath = Paths.get(checkAndGetSetting(aozan2Conf, "aozan.var.path"));

    // Get the main lock path
    this.mainLockPath =
        Paths.get(checkAndGetSetting(aozan2Conf, "aozan.lock.file"));

    // TODO Check removed steps

    this.syncStepRecipe = createSyncStepRecipe(conf, logger, aozan2Conf);
    this.demuxStepRecipe = createDemuxStep(conf, logger, aozan2Conf);
    this.qcStepRecipe = createQCStep(conf, logger, aozan2Conf);
  }

}
