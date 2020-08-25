package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.SendMail;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.DataProcessor.ProcessResult;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.InputData;
import fr.ens.biologie.genomique.aozan.aozan3.dataprovider.RunDataProvider;
import fr.ens.biologie.genomique.aozan.aozan3.dataprovider.RunDataProviderService;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLoggerFactory;

/**
 * This class define a recipe.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class Recipe {

  private String name;
  private final Configuration conf = new Configuration();

  private final DataStorageRegistry storages = new DataStorageRegistry();
  private final SendMail sendMail;
  private RunDataProvider provider;
  private final List<Step> steps = new ArrayList<>();
  private boolean useInProgressData;

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
   * Set the data provider.
   * @param providerName provider name
   * @param storageName storage used by the provider
   * @param conf provider configuration
   * @throws Aozan3Exception if an error occurs while setting the provider
   */
  public void setDataProvider(final String providerName,
      final String storageName, final Configuration conf)
      throws Aozan3Exception {

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

    setDataProvider(provider);
  }

  /**
   * Set an existing data provider
   * @param dataProvider provider to set
   */
  public void setDataProvider(final RunDataProvider dataProvider) {

    checkNotInitialized();
    requireNonNull(dataProvider);

    // Check if a data provider has been already set
    if (this.provider != null) {
      throw new IllegalStateException();
    }

    this.provider = dataProvider;
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

  /**
   * Set if in progress run data must be used for the recipe.
   * @param inProgress true if in progress data must be used
   */
  public void setUseInProgressData(boolean inProgress) {

    checkNotInitialized();

    this.useInProgressData = inProgress;
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
    if (this.provider == null) {
      throw new Aozan3Exception("Run Data provider has not been defined.");
    }

    for (Step step : this.steps) {
      step.init();
    }

    this.initialized = true;
  }

  /**
   * Use the recipe to process run provided by the run data provider
   * @throws Aozan3Exception if an error occurs while executing the recipe
   */
  public void execute() {

    try {
      process(runDataToProcess());
    } catch (Aozan3Exception e) {
      exitAozan(e);
    }
  }

  /**
   * Execute the recipe for a run.
   * @param runId the run identifier
   * @throws Aozan3Exception if an error occurs while executing the recipe
   */
  public void execute(String runId) {

    requireNonNull(runId);

    execute(Collections.singletonList(runId));
  }

  public void execute(List<String> runIds) {

    requireNonNull(runIds);

    try {
      process(runDataToProcess(runIds));
    } catch (Aozan3Exception e) {
      exitAozan(e);
    }
  }

  //
  // Internal method execution
  //

  /**
   * Execute the recipe on a list of run data
   * @param runs input runs
   * @throws Aozan3Exception if an error occurs while executing the recipe
   */
  private void process(List<RunData> runs) throws Aozan3Exception {

    requireNonNull(runs);

    for (RunData run : runs) {
      process(run);
    }
  }

  /**
   * Execute the recipe on a run data.
   * @param runs input runs
   * @throws Aozan3Exception if an error occurs while executing the recipe
   */
  private void process(RunData run) throws Aozan3Exception {

    requireNonNull(run);

    // Map<DataType, RunData> data = new HashMap<>();
    // data.put(run.getType(), run);

    InputData data = new InputData(run);

    // Check if the recipe has been initialized
    if (!this.initialized) {
      throw new IllegalStateException();
    }

    for (Step step : this.steps) {

      InputData stepInputData =
          createInputDataForStep(data, step.getInputRequirements());

      if (!stepInputData.isEmpty()) {

        ProcessResult result = step.process(stepInputData);

        // Update data
        data.add(result.getRunData());

        // Send email
        this.sendMail.sendMail(result.getEmail());
      }
    }

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
  private List<RunData> runDataToProcess() throws Aozan3Exception {

    return runDataToProcess(null);
  }

  /**
   * Create the list of the run data to process
   * @param runIds run identifiers to process, can be null
   * @return a list of RunData object
   * @throws Aozan3Exception if a run identifier does not exists
   */
  private List<RunData> runDataToProcess(Collection<String> runIds)
      throws Aozan3Exception {

    List<RunData> runs = this.useInProgressData
        ? this.provider.listInProgressRunData()
        : this.provider.listCompletedRunData();

    if (runIds == null) {
      return runs;
    }

    // Create a map with runIds
    Map<String, RunData> map = new HashMap<>();
    for (RunData r : runs) {
      map.put(r.getRunId().getId(), r);
    }

    List<RunData> result = new ArrayList<>();

    // Filter the runs to process
    for (String runId : runIds) {

      if (runId == null) {
        continue;
      }

      if (!map.containsKey(runId)) {
        throw new Aozan3Exception("Unknown run: " + runId);
      }

      result.add(map.get(runId));
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

    for (RunData runData : runDataToProcess()) {
      result.add(runData.getRunId().getId());
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param recipeName recipe name
   * @param conf configuration
   * @param logger logger
   * @throws Aozan3Exception if an error occurs while initializing the email
   *           sending
   */
  public Recipe(String recipeName, Configuration conf, AozanLogger logger)
      throws Aozan3Exception {

    requireNonNull(recipeName);

    this.name = recipeName;

    // Set the configuration
    if (conf != null) {
      this.conf.set(conf);
    }

    // Configure the logger
    this.logger = AozanLoggerFactory.newLogger(this.conf, logger);

    // Configure emails
    this.sendMail = new SendMail(conf, this.logger);
  }

}
