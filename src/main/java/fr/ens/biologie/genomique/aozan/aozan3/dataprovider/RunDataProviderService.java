package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.eoulsan.util.ServiceNameLoader;

/**
 * This class define a service to retrieve a RunDataProvider
 * @since 3.0
 * @author Laurent Jourdren
 */
public class RunDataProviderService extends ServiceNameLoader<RunDataProvider> {

  private static RunDataProviderService service;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of an
   * RunConfigurationProviderService.
   * @return A ActionService instance
   */
  public static synchronized RunDataProviderService getInstance() {

    if (service == null) {
      service = new RunDataProviderService();
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
  protected RunDataProviderService() {

    super(RunDataProvider.class);
  }

}
