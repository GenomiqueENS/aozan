package fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider;

import fr.ens.biologie.genomique.eoulsan.util.ServiceNameLoader;

/**
 * This class define a service to retrieve a RunConfigurationProvider
 * @since 3.0
 * @author Laurent Jourdren
 */
public class RunConfigurationProviderService
    extends ServiceNameLoader<RunConfigurationProvider> {

  private static RunConfigurationProviderService service;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of an
   * RunConfigurationProviderService.
   * @return A ActionService instance
   */
  public static synchronized RunConfigurationProviderService getInstance() {

    if (service == null) {
      service = new RunConfigurationProviderService();
    }

    return service;
  }

  //
  // Protected methods
  //

  @Override
  protected boolean accept(final Class<?> clazz) {

    return true;
  }

  @Override
  protected String getMethodName() {

    return "getName";
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  protected RunConfigurationProviderService() {

    super(RunConfigurationProvider.class);
  }

}
