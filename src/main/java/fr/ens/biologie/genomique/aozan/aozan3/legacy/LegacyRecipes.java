package fr.ens.biologie.genomique.aozan.aozan3.legacy;

import static java.util.Arrays.asList;
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
import fr.ens.biologie.genomique.aozan.aozan3.SendMail;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.Aozan2QCDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.Bcl2FastqIlluminaDemuxDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.BclConvertIlluminaDemuxDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.DiscoverNewIlluminaRunDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.EndIlluminaRunDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.IlluminaSyncDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprovider.IlluminaProcessedRunDataProvider;
import fr.ens.biologie.genomique.aozan.aozan3.dataprovider.IlluminaRawRunDataProvider;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLoggerFactory;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.Recipe;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.Step;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.IlluminaSamplesheetRunConfigurationProvider;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.RunConfigurationProvider;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define recipes that perform like Aozan 2 steps.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class LegacyRecipes {

  private Recipe newRunStepRecipe;
  private Recipe endRunStepRecipe;
  private Recipe syncStepRecipe;
  private Recipe demuxStepRecipe;
  private Recipe qcStepRecipe;
  private SendMail sendMail;
  private Path varPath;
  private Path mainLockPath;
  private Map<Recipe, Path> lockPaths = new HashMap<>();
  private Map<Recipe, String> recipeDoneFilename = new HashMap<>();
  private final boolean aozanEnabled;

  //
  // Getters
  //

  /**
   * Test if Aozan is enabled.
   * @return true if Aozan is enabled
   */
  public boolean isAozanEnabled() {

    return this.aozanEnabled;
  }

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
   * Get the new run step recipe.
   * @return the new run step recipe or null is not enabled
   */
  public Recipe getNewRunStepRecipe() {

    return this.newRunStepRecipe;
  }

  /**
   * Get the new run step recipe.
   * @return the new run step recipe or null is not enabled
   */
  public Recipe getEndRunStepRecipe() {

    return this.endRunStepRecipe;
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
   * @return the demux step recipe or null is not enabled
   */
  public Recipe getDemuxStepRecipe() {

    return this.demuxStepRecipe;
  }

  /**
   * Get the QC step recipe.
   * @return the QC step recipe or null is not enabled
   */
  public Recipe getQCStepRecipe() {

    return this.qcStepRecipe;
  }

  /**
   * Get the path where creating locks of a recipe.
   * @param recipe the recipe
   * @return the lock directory path of the recipe
   */
  public Path getLockDirectory(Recipe recipe) {

    Objects.requireNonNull(recipe);

    return this.lockPaths.get(recipe);
  }

  /**
   * Get the done filename for a recipe.
   * @param recipe the recipe
   * @return the done filename
   */
  public String getRecipeDoneDenyFilename(Recipe recipe) {

    Objects.requireNonNull(recipe);

    if (!this.recipeDoneFilename.containsKey(recipe)) {
      return recipe.getName();
    }

    return this.recipeDoneFilename.get(recipe);
  }

  /**
   * Return SendMail object.
   * @return the SendMail object
   */
  public SendMail getSendMail() {

    return this.sendMail;
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

    table.put("casava.design.format", Settings.SAMPLESHEET_FORMAT_KEY);
    table.put("casava.samplesheet.format", Settings.SAMPLESHEET_FORMAT_KEY);
    table.put("casava.design.prefix.filename", Settings.SAMPLESHEET_PREFIX_KEY);
    table.put("casava.samplesheet.prefix.filename",
        Settings.SAMPLESHEET_PREFIX_KEY);
    table.put("casava.designs.path", Settings.SAMPLESHEET_PATH_KEY);
    table.put("casava.samplesheets.path", Settings.SAMPLESHEET_PATH_KEY);

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

    table.put("casava.threads", Settings.DEMUX_THREADS_KEY);
    table.put("casava.with.failed.reads",
        Settings.BCL2FASTQ_WITH_FAILED_READS_KEY);
    table.put("casava.design.generator.command",
        Settings.SAMPLESHEET_GENERATOR_COMMAND_KEY);
    table.put("casava.samplesheet.generator.command",
        Settings.SAMPLESHEET_GENERATOR_COMMAND_KEY);
    table.put("demux.use.docker.enable", Settings.BCL2FASTQ_USE_DOCKER_KEY);

    for (Map.Entry<String, String> e : conf.toMap().entrySet()) {
      String key =
          table.containsKey(e.getKey()) ? table.get(e.getKey()) : e.getKey();
      result.set(key, e.getValue());
    }

    return result;
  }

  /**
   * Convert Aozan 2 settings key names to aozan 3 key names.
   * @param conf aozan 3 configuration
   * @return a Configuration object with Aozan 3 setting key names
   */
  private Configuration aozan2Compatibility(Configuration conf) {

    Configuration result = new Configuration();

    Map<String, String> table = new HashMap<>();

    table.put("bcl2fastq.samplesheet.path", Settings.SAMPLESHEET_PATH_KEY);
    table.put("bcl2fastq.samplesheet.prefix.filename",
        Settings.SAMPLESHEET_PREFIX_KEY);
    table.put("bcl2fastq.samplesheet.format", Settings.SAMPLESHEET_FORMAT_KEY);
    table.put("bcl2fastq.samplesheet.generator.command",
        Settings.SAMPLESHEET_GENERATOR_COMMAND_KEY);
    table.put("bcl2fastq.use.docker", Settings.BCL2FASTQ_USE_DOCKER_KEY);

    table.put("bcl2fastq.threads", Settings.DEMUX_THREADS_KEY);

    table.put("smtp.server", "mail.smtp.host");
    table.put("smtp.port", "mail.smtp.port");
    table.put("smtp.use.starttls", "mail.smtp.starttls.enable");
    table.put("smtp.use.ssl", "mail.smtp.ssl.enable");

    table.put("smtp.login", "mail.smtp.login");
    table.put("smtp.password", "mail.smtp.password");

    for (Map.Entry<String, String> e : conf.toMap().entrySet()) {
      String key =
          table.containsKey(e.getKey()) ? table.get(e.getKey()) : e.getKey();
      result.set(key, e.getValue());
    }

    return result;
  }

  private static boolean parseCommonConfiguration(Configuration conf,
      GenericLogger logger, Configuration aozan2Conf) {

    // Check if Aozan is enabled
    if (!aozan2Conf.getBoolean("aozan.enable")) {
      return false;
    }

    // Sequencer names
    for (Map.Entry<String, String> e : aozan2Conf.toMap().entrySet()) {

      String key = e.getKey();

      if (key != null && key.startsWith("sequencer.name.")) {
        conf.set(e.getKey(), e.getValue());
      }
    }

    // Enable email sending
    setSetting(conf, aozan2Conf, "send.mail", "send.mail", "False");

    // Set email source and destinations
    for (String key : asList("mail.from", "mail.to", "mail.error.to")) {
      setSetting(conf, aozan2Conf, key, key);
    }

    // Set SMTP configuration
    for (String key : aozan2Conf.toMap().keySet()) {
      if (key.startsWith("mail.smtp.")) {
        setSetting(conf, aozan2Conf, key, key);
      }
    }

    return true;
  }

  private Recipe createNewRunStepRecipe(Configuration conf,
      GenericLogger logger, Configuration aozan2Conf) throws Aozan3Exception {

    // Check if the sync step is enabled
    if (!aozan2Conf.getBoolean("first.base.report.step", true)) {
      return null;
    }

    Recipe recipe = new Recipe("newrun", "New run step", conf, logger);

    boolean inProgress = true;
    final String inputStoragePrefix = "nasStorage";
    final String outputStorageName = "nullStorage";

    // Set input storages
    createStorages(recipe, inputStoragePrefix,
        aozan2Conf.get("hiseq.data.path"));

    // Set input storages as run providers
    for (String storageName : recipe.getStorages().names()) {
      recipe.addDataProvider(IlluminaRawRunDataProvider.PROVIDER_NAME,
          storageName, inProgress, conf);
    }

    // Set output storage
    recipe.addStorage(outputStorageName,
        new DataStorage("local", Paths.get("/dev/null")));

    // Run configuration provider
    RunConfigurationProvider runConfProvider =
        createRunConfProvider(recipe, aozan2Conf);

    // Define step configuration
    Configuration stepConf = new Configuration();

    Step newRunStep = new Step(recipe, "newrunstep",
        DiscoverNewIlluminaRunDataProcessor.PROCESSOR_NAME, outputStorageName,
        stepConf, runConfProvider, new DefaultRunIdGenerator());

    recipe.addStep(newRunStep);

    return recipe;
  }

  private Recipe createEndRunStepRecipe(Configuration conf,
      GenericLogger logger, Configuration aozan2Conf) throws Aozan3Exception {

    // Check if the sync step is enabled
    if (!aozan2Conf.getBoolean("hiseq.step", true)) {
      return null;
    }

    Recipe recipe = new Recipe("endrun", "End run step", conf, logger);
    this.recipeDoneFilename.put(recipe, "hiseq");

    boolean inProgress = false;
    final String inputStoragePrefix = "nasStorage";
    final String outputStorageName = "qcStorage";

    // Set input storages
    createStorages(recipe, inputStoragePrefix,
        aozan2Conf.get("hiseq.data.path"));

    // Set input storages as run providers
    for (String storageName : recipe.getStorages().names()) {
      recipe.addDataProvider(IlluminaRawRunDataProvider.PROVIDER_NAME,
          storageName, inProgress, conf);
    }

    // Set output storage
    Path qcPath = Paths.get(aozan2Conf.get("reports.data.path").trim());
    recipe.addStorage(outputStorageName, new DataStorage("local", qcPath));
    this.lockPaths.put(recipe, qcPath);

    // Run configuration provider
    RunConfigurationProvider runConfProvider =
        createRunConfProvider(recipe, aozan2Conf);

    // Define step configuration
    Configuration stepConf = new Configuration();

    Step syncStep = new Step(recipe, "endrunstep",
        EndIlluminaRunDataProcessor.PROCESSOR_NAME, outputStorageName, stepConf,
        runConfProvider, new DefaultRunIdGenerator());

    recipe.addStep(syncStep);

    return recipe;
  }

  private Recipe createSyncStepRecipe(Configuration conf, GenericLogger logger,
      Configuration aozan2Conf) throws Aozan3Exception {

    // Check if the sync step is enabled
    if (!aozan2Conf.getBoolean("sync.step", true)) {
      return null;
    }

    Recipe recipe = new Recipe("sync", "Sync step", conf, logger);

    // boolean inProgress = aozan2Conf.getBoolean("sync.continuous.sync",
    // false);
    boolean inProgress = false;
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
    this.lockPaths.put(recipe, bclPath);

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

  private Recipe createDemuxStep(Configuration conf, GenericLogger logger,
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
    this.lockPaths.put(recipe, fastqPath);

    // Run configuration provider
    RunConfigurationProvider runConfProvider =
        createRunConfProvider(recipe, aozan2Conf);

    // Define step configuration
    Configuration stepConf = new Configuration();
    stepConf.setFromOtherConfIfExists(aozan2Conf, "reports.url");
    stepConf.setFromOtherConfIfExists(aozan2Conf, "read.only.output.files");

    // Select the demux tool to use
    String demuxProcessorName;
    switch (aozan2Conf.get("demux.tool.name", "bcl2fastq").trim()
        .toLowerCase()) {

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

  private Recipe createQCStep(Configuration conf, GenericLogger logger,
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
    this.lockPaths.put(recipe, qcPath);

    // Run configuration provider
    RunConfigurationProvider runConfProvider =
        createRunConfProvider(recipe, aozan2Conf, true);

    // Define step configuration
    Configuration stepConf = new Configuration(aozan2Conf);
    stepConf.set("legacy.output", true);
    if (aozan2Conf.containsKey("reports.url")) {
      stepConf.set("reports.url", aozan2Conf.get("reports.url"));
    }

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

    return createRunConfProvider(recipe, aozan2Conf, false);
  }

  private static RunConfigurationProvider createRunConfProvider(Recipe recipe,
      Configuration aozan2Conf, boolean searchSamplesheetInRunDirFirst)
      throws Aozan3Exception {

    // Run configuration provider
    RunConfigurationProvider result =
        new IlluminaSamplesheetRunConfigurationProvider();

    // Set run configuration provider conf
    Configuration stepConf = new Configuration();
    setSetting(stepConf, aozan2Conf, Settings.SAMPLESHEET_PATH_KEY);
    setSetting(stepConf, aozan2Conf, Settings.SAMPLESHEET_FORMAT_KEY,
        Settings.SAMPLESHEET_FORMAT_KEY, "xls");
    setSetting(stepConf, aozan2Conf, Settings.SAMPLESHEET_PREFIX_KEY,
        Settings.SAMPLESHEET_PREFIX_KEY, "samplesheet");
    setSetting(stepConf, aozan2Conf, "samplesheet.generator.command");
    setSetting(stepConf, aozan2Conf, "index.sequences");

    // For the QC step, first search samplesheet in the FASTQ output
    // directory
    if (searchSamplesheetInRunDirFirst) {
      stepConf.set("samplesheet.search.in.run.dir.first", true);
    }

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
      String key) {

    setSetting(conf, aozan2Conf, key, key, null);
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

  private static GenericLogger createLogger(Configuration aozan2Conf,
      GenericLogger currentLogger) throws Aozan3Exception {

    if (aozan2Conf.containsKey("aozan.log.path")) {

      Configuration logConf = new Configuration();
      logConf.set("aozan.logger", "file");
      logConf.set("aozan.log", aozan2Conf.get("aozan.log.path"));
      logConf.set("aozan.log.level", aozan2Conf.get("aozan.log.level", "info"));

      return AozanLoggerFactory.newLogger(logConf, currentLogger);
    }

    return currentLogger;
  }

  //
  // Constructor
  //

  public LegacyRecipes(Configuration conf, GenericLogger logger,
      Path aozan2ConfFile) throws Aozan3Exception {

    requireNonNull(aozan2ConfFile);

    Configuration aozan2Conf = new Configuration();
    aozan2Conf.load(aozan2ConfFile, true);
    aozan2Conf = aozan2Compatibility(aozan1Compatibility(aozan2Conf));

    this.aozanEnabled = parseCommonConfiguration(conf, logger, aozan2Conf);

    if (!this.aozanEnabled) {
      // Aozan is not enabled, nothing to do
      return;
    }

    // Change logger if required in Aozan 2 conf
    logger = createLogger(aozan2Conf, logger);

    // Get the var path
    this.varPath = Paths.get(checkAndGetSetting(aozan2Conf, "aozan.var.path"));

    // Set last error file
    conf.set("mail.last.error.file", this.varPath + "/lasterror.msg");

    // Get the main lock path
    this.mainLockPath =
        Paths.get(checkAndGetSetting(aozan2Conf, "aozan.lock.file"));

    // TODO Check removed steps

    this.newRunStepRecipe = createNewRunStepRecipe(conf, logger, aozan2Conf);
    this.endRunStepRecipe = createEndRunStepRecipe(conf, logger, aozan2Conf);
    this.syncStepRecipe = createSyncStepRecipe(conf, logger, aozan2Conf);
    this.demuxStepRecipe = createDemuxStep(conf, logger, aozan2Conf);
    this.qcStepRecipe = createQCStep(conf, logger, aozan2Conf);
    this.sendMail = new SendMail(aozan2Conf, logger);
  }

}
