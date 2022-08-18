package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.ConfigurationDefaults.DOCKER_URI_KEY;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.SendMail;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.DataProcessor.ProcessResult;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.InputData;
import fr.ens.biologie.genomique.aozan.aozan3.dataprovider.RunDataProvider;
import fr.ens.biologie.genomique.aozan.aozan3.dataprovider.RunDataProviderService;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLoggerFactory;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;

/**
 * This class define a recipe.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class Recipe {

  private String name;
  private String description;
  private final Configuration conf = new Configuration();

  private final DataStorageRegistry storages = new DataStorageRegistry();
  private final SendMail sendMail;
  private final Set<RunDataProvider> providers = new HashSet<>();
  private final Set<RunDataProvider> inProgressProviders = new HashSet<>();
  private final List<Step> steps = new ArrayList<>();

  private final AozanLogger logger;

  private boolean initialized;

  //
  // Getters
  //

  /**
   * Get the recipe name
   * @return the name of the recipe
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get the recipe name
   * @return the name of the recipe
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Get the logger.
   * @return the logger
   */
  public AozanLogger getLogger() {
    return this.logger;
  }

  /**
   * Get the configuration.
   * @return a copy of the configuration of the recipe
   */
  public Configuration getConfiguration() {
    return new Configuration(this.conf);
  }

  /**
   * Get storages
   * @return the storage registry
   */
  public DataStorageRegistry getStorages() {
    return this.storages;
  }

  //
  // Recipe management
  //

  /**
   * Add a storage.
   * @param name name of the storage
   * @param storage storage to add
   */
  public void addStorage(final String name, DataStorage storage) {

    checkNotInitialized();
    this.storages.add(name, storage);
  }

  /**
   * Add a data provider.
   * @param providerName provider name
   * @param storageName storage used by the provider
   * @param inProgress true if in progress data must be used
   * @param conf provider configuration
   * @throws Aozan3Exception if an error occurs while setting the provider
   */
  public void addDataProvider(final String providerName,
      final String storageName, final boolean inProgress,
      final Configuration conf) throws Aozan3Exception {

    checkNotInitialized();
    requireNonNull(providerName);
    requireNonNull(storageName);

    // Create provider instance
    RunDataProvider provider =
        RunDataProviderService.getInstance().newService(providerName);
    if (provider == null) {
      throw new Aozan3Exception("Unknown run data provider: " + providerName);
    }

    // Get storage
    DataStorage storage = this.storages.get(storageName);

    // Set configuration
    Configuration providerConf = getConfiguration();
    if (conf != null) {
      providerConf.set(conf);
    }

    // Initialize the provider
    provider.init(storage, providerConf, this.logger);

    addDataProvider(provider);

    if (inProgress) {
      this.inProgressProviders.add(provider);
    }
  }

  /**
   * Set an existing data provider
   * @param dataProvider provider to set
   */
  public void addDataProvider(final RunDataProvider dataProvider) {

    checkNotInitialized();
    requireNonNull(dataProvider);

    // TODO Check if the same provider has been already set

    this.providers.add(dataProvider);
  }

  /**
   * Add steps.
   * @param steps a collection of step to add
   */
  public void addSteps(Collection<Step> steps) {

    requireNonNull(steps);

    for (Step step : steps) {
      addStep(step);
    }
  }

  /**
   * Add a step.
   * @param step the step to add
   */
  public void addStep(Step step) {

    checkNotInitialized();
    requireNonNull(step);

    // TODO check the step if exists

    this.steps.add(step);
  }

  //
  // Recipe execution
  //

  /**
   * Initialize the steps of the recipe
   * @throws Aozan3Exception if an error occurs while initializing the steps
   */
  public void init() throws Aozan3Exception {

    checkNotInitialized();

    // Check if a provide has been set
    if (this.providers.isEmpty()) {
      throw new Aozan3Exception("No run data provider has not been defined.");
    }

    // Log steps of the recipe
    List<String> stepNames = new ArrayList<>();
    for (Step step : this.steps) {
      stepNames.add(step.getName() + " [" + step.getProcessorName() + "]");
    }
    this.logger.debug("Recipe \""
        + getName() + "\" step(s): " + String.join(", ", stepNames));

    this.logger.debug("Initiliaze step(s)");

    for (Step step : this.steps) {
      step.init();
    }

    this.logger.debug("Successful initiliazation of step(s)");

    this.initialized = true;
  }

  /**
   * Use the recipe to process run provided by the run data provider
   * @throws Aozan3Exception if an error occurs while executing the recipe
   */
  public void execute() {

    try {
      List<InputData> runs = runDataToProcess();

      if (runs.isEmpty()) {
        this.logger.info("No run to process found");
      } else {
        this.logger.info("Found " + runs.size() + " runs to process");
      }

      process(runs);
    } catch (Aozan3Exception e) {
      exitAozan(e);
    }
  }

  /**
   * Execute the recipe for a run.
   * @param runId the run identifier
   * @throws Aozan3Exception if an error occurs while executing the recipe
   */
  public boolean execute(String runId) {

    requireNonNull(runId);

    return execute(Collections.singletonList(runId)).isEmpty() ? false : true;
  }

  /**
   * Execute a list of run Ids.
   * @param runIds a list of run ids
   */
  public Set<RunId> execute(List<String> runIds) {

    requireNonNull(runIds);

    try {
      return process(runDataToProcess(runIds));
    } catch (Aozan3Exception e) {
      exitAozan(e);
    }

    // Non reachable code
    return null;
  }

  /**
   * Get available runs.
   * @return a set with available runs.
   * @throws Aozan3Exception if an error occurs while getting the available runs
   */
  public Set<RunId> availableRuns() throws Aozan3Exception {

    Set<RunId> result = new HashSet<>();

    for (InputData inputData : runDataToProcess()) {

      for (RunData runData : inputData.entries()) {
        result.add(runData.getRunId());
      }
    }

    return result;
  }

  //
  // Internal method execution
  //

  /**
   * Execute the recipe on a list of run data
   * @param runs input runs
   * @return a set with the processed runs
   * @throws Aozan3Exception if an error occurs while executing the recipe
   */
  private Set<RunId> process(List<InputData> runs) throws Aozan3Exception {

    requireNonNull(runs);
    Set<RunId> result = new HashSet<>();

    for (InputData run : runs) {
      if (process(run)) {
        result.add(run.getLastRunData().getRunId());
      }
    }

    return result;
  }

  /**
   * Execute the recipe on a run data.
   * @param runs input runs
   * @return true if a run has been processed
   * @throws Aozan3Exception if an error occurs while executing the recipe
   */
  private boolean process(InputData data) throws Aozan3Exception {

    requireNonNull(data);

    String runId = data.getLastRunData().getRunId().getId();
    boolean runProcessed = false;

    // Check if the recipe has been initialized
    if (!this.initialized) {
      throw new IllegalStateException();
    }

    for (Step step : this.steps) {

      InputData stepInputData =
          createInputDataForStep(data, step.getInputRequirements());

      if (!stepInputData.isEmpty()) {

        this.logger.info(
            "Start step \"" + step.getName() + "\" for run " + runId + ".");

        ProcessResult result = step.process(stepInputData);

        // Update data
        data.add(result.getRunData());

        // Send email
        this.sendMail.sendMail(result.getEmail());

        this.logger.info(
            "End of step \"" + step.getName() + "\" for run " + runId + ".");
        runProcessed = true;
      }
    }

    return runProcessed;
  }

  //
  // Other methods
  //

  /**
   * Requires that the recipe has been initialized.
   */
  private void checkNotInitialized() {

    if (this.initialized) {
      throw new IllegalStateException();
    }
  }

  /**
   * Create the list of the run data to process
   * @return a list of RunData object
   * @throws Aozan3Exception if a run identifier does not exists
   */
  private List<InputData> runDataToProcess() throws Aozan3Exception {

    return runDataToProcess(null);
  }

  /**
   * Create the list of the run data to process
   * @param runIds run identifiers to process, can be null
   * @return a list of RunData object
   * @throws Aozan3Exception if a run identifier does not exists
   */
  private List<InputData> runDataToProcess(Collection<String> runIds)
      throws Aozan3Exception {

    // Create a map with runIds
    Multimap<String, RunData> map = ArrayListMultimap.create();
    List<InputData> result = new ArrayList<>();

    for (RunDataProvider provider : this.providers) {

      this.logger
          .debug("Looks for run in: " + provider.getDataStorage().getPath());

      List<RunData> runs = this.inProgressProviders.contains(provider)
          ? provider.listInProgressRunData() : provider.listCompletedRunData();

      for (RunData r : runs) {

        String runId = r.getRunId().getId();
        map.put(runId, r);
      }
    }

    if (runIds == null) {

      // Handle all available runs
      for (String runId : map.keySet()) {
        result.add(new InputData(map.get(runId)));
      }
    } else {

      // Filter the runs to process
      for (String runId : runIds) {

        if (runId == null) {
          continue;
        }

        if (!map.containsKey(runId)) {
          throw new Aozan3Exception("Unknown run: " + runId);
        }
        result.add(new InputData(map.get(runId)));
      }
    }

    return result;
  }

  /**
   * Exit Aozan with an error.
   * @param t exception
   */
  private void exitAozan(Throwable t) {

    this.logger.error(t);
    this.sendMail.sendMail(t);
    System.exit(1);
  }

  /**
   * Create an InputData object for step processing.
   * @param inputData all input InputData of the recipe
   * @param filters DataType filters to apply
   * @return a new InputData object for step processing
   */
  private InputData createInputDataForStep(InputData inputData,
      Set<DataTypeFilter> filters) {

    requireNonNull(inputData);
    requireNonNull(filters);

    InputData result = new InputData();

    for (DataTypeFilter filter : filters) {

      if (filter == null) {
        continue;
      }

      InputData filterResult = inputData.filter(filter);

      // Each filter must only return one element
      if (filterResult.size() != 1) {
        return new InputData();
      }

      result.add(filterResult);
    }

    return result;
  }

  /**
   * Get the list of available runs.
   * @return a list with available runs
   * @throws Aozan3Exception if an error occurs while getting the available runs
   */
  public List<String> getAvalaibleRuns() throws Aozan3Exception {

    List<String> result = new ArrayList<>();

    for (InputData runData : runDataToProcess()) {
      result.add(runData.getLastRunData().getRunId().getId());
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param recipeName recipe name
   * @param description description of the recipe
   * @param conf configuration
   * @param logger logger
   * @throws Aozan3Exception if an error occurs while initializing the email
   *           sending
   */
  public Recipe(String recipeName, String description, Configuration conf,
      AozanLogger logger) throws Aozan3Exception {

    requireNonNull(recipeName);
    requireNonNull(description);

    this.name = recipeName;
    this.description = description;

    // Set the configuration
    if (conf != null) {
      this.conf.set(conf);
    }

    // Configure the logger
    this.logger = AozanLoggerFactory.newLogger(this.conf, logger);

    // Configure emails
    this.sendMail = new SendMail(conf, this.logger);

    // Set Docker URI
    if (conf.containsKey(DOCKER_URI_KEY)) {
      EoulsanRuntime.getSettings()
          .setDockerConnectionURI(conf.get(DOCKER_URI_KEY));
    }
  }

}
