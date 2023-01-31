package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import fr.ens.biologie.genomique.kenetre.util.ServiceNameLoader;

/**
 * This class define a service to retrieve a DataProcessor
 * @since 3.0
 * @author Laurent Jourdren
 */
public class DataProcessorService extends ServiceNameLoader<DataProcessor> {

  private static DataProcessorService service;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of an DataProcessorService.
   * @return A ActionService instance
   */
  public static synchronized DataProcessorService getInstance() {

    if (service == null) {
      service = new DataProcessorService();
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
  protected DataProcessorService() {

    super(DataProcessor.class);
  }

}
