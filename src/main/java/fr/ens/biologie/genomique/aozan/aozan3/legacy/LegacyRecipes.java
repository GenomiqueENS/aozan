package fr.ens.biologie.genomique.aozan.aozan3.legacy;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.DefaultRunIdGenerator;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.Aozan2QCDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.Bcl2FastqIlluminaDemuxDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.IlluminaSyncDataProcessor;
import fr.ens.biologie.genomique.aozan.aozan3.dataprovider.IlluminaProcessedRunDataProvider;
import fr.ens.biologie.genomique.aozan.aozan3.dataprovider.IlluminaRawRunDataProvider;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.Recipe;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.Step;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.EmptyRunConfigurationProvider;
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

  //
  // Getters
  //

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

  //
  // Other methods
  //

  private static boolean parseCommonConfiguration(Configuration conf,
      AozanLogger logger, Configuration aozan2Conf) {

    // Check if Aozan is enabled
    if (!aozan2Conf.getBoolean("aozan.enable")) {
      return false;
    }

    // Mode debug
    // aozan.debug=False

    // Enable email sending
    setSetting(conf, aozan2Conf, "send.mail", "send.mail", "False");

    // SMTP server name
    setSetting(conf, aozan2Conf, "smtp.host", "mail.smtp.host");

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

  private static Recipe createSyncStepRecipe(Configuration conf,
      AozanLogger logger, Configuration aozan2Conf) throws Aozan3Exception {

    Recipe recipe = new Recipe("sync", "Sync step", conf, logger);

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
    recipe.addStorage(outputStorage,
        new DataStorage("local", aozan2Conf.get("bcl.data.path").trim(), null));

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

  private static Recipe createDemuxStep(Configuration conf, AozanLogger logger,
      Configuration aozan2Conf) throws Aozan3Exception {

    Recipe recipe = new Recipe("demux", "Demux step", conf, logger);

    boolean inProgress = false;
    final String inputStorageName = "bclStorage";
    final String outputStorageName = "fastqStorage";

    // Set input storages
    recipe.addStorage(inputStorageName,
        new DataStorage("local", aozan2Conf.get("bcl.data.path").trim(), null));
    recipe.addDataProvider(IlluminaRawRunDataProvider.PROVIDER_NAME,
        inputStorageName, inProgress, conf);

    // Set output storage
    recipe.addStorage(outputStorageName, new DataStorage("local",
        aozan2Conf.get("fastq.data.path").trim(), null));

    // Run configuration provider
    RunConfigurationProvider runConfProvider =
        createRunConfProvider(recipe, aozan2Conf);

    // Define step configuration
    Configuration stepConf = new Configuration();

    Step demuxStep = new Step(recipe, "demuxstep",
        Bcl2FastqIlluminaDemuxDataProcessor.PROCESSOR_NAME, outputStorageName,
        stepConf, runConfProvider, new DefaultRunIdGenerator());

    recipe.addStep(demuxStep);

    return recipe;
  }

  private static Recipe createQCStep(Configuration conf, AozanLogger logger,
      Configuration aozan2Conf) throws Aozan3Exception {

    Recipe recipe = new Recipe("qc", "QC step", conf, logger);

    boolean inProgress = false;
    final String inputStorageName1 = "bclStorage";
    final String inputStorageName2 = "fastqStorage";
    final String outputStorageName = "qcStorage";

    // Set bcl input storage
    recipe.addStorage(inputStorageName1,
        new DataStorage("local", aozan2Conf.get("bcl.data.path").trim(), null));
    recipe.addDataProvider(IlluminaRawRunDataProvider.PROVIDER_NAME,
        inputStorageName1, inProgress, conf);

    // Set fastq input storage
    recipe.addStorage(inputStorageName2, new DataStorage("local",
        aozan2Conf.get("fastq.data.path").trim(), null));
    recipe.addDataProvider(IlluminaProcessedRunDataProvider.PROVIDER_NAME,
        inputStorageName2, inProgress, conf);

    // Set output storage
    recipe.addStorage(outputStorageName, new DataStorage("local",
        aozan2Conf.get("reports.data.path").trim(), null));

    // Define step configuration
    Configuration stepConf = new Configuration(aozan2Conf);

    Step qcStep = new Step(recipe, "qcstep",
        Aozan2QCDataProcessor.PROCESSOR_NAME, outputStorageName, stepConf,
        new EmptyRunConfigurationProvider(), new DefaultRunIdGenerator());

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

      result.add(new DataStorage("local", s.trim(), null));
    }

    int i = 0;
    for (DataStorage storage : result) {
      recipe.addStorage(prefix + (result.size() > 1 ? (++i) : ""), storage);
    }
  }

  //
  // Constructor
  //

  public LegacyRecipes(Configuration conf, AozanLogger logger,
      Path aozan2ConfFile) throws Aozan3Exception {

    requireNonNull(aozan2ConfFile);

    Configuration aozan2Conf = new Configuration();
    aozan2Conf.load(aozan2ConfFile, true);

    if (!parseCommonConfiguration(conf, logger, aozan2Conf)) {

      // Aozan is not enabled, nothing to do
      return;
    }

    // TODO Check removed steps

    this.syncStepRecipe = createSyncStepRecipe(conf, logger, aozan2Conf);
    this.demuxStepRecipe = createDemuxStep(conf, logger, aozan2Conf);
    this.qcStepRecipe = createQCStep(conf, logger, aozan2Conf);
  }

}
