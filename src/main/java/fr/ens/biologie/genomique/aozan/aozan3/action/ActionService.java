package fr.ens.biologie.genomique.aozan.aozan3.action;

import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.kenetre.util.ServiceNameLoader;

/**
 * This class define a service to retrieve an Action.
 * @since 3.0
 * @author Laurent Jourdren
 */
public class ActionService extends ServiceNameLoader<Action> {

  private static ActionService service;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of an DataProcessorService.
   * @return A ActionService instance
   */
  public static synchronized ActionService getInstance() {

    if (service == null) {
      service = new ActionService();
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
  // Instance methods
  //

  /**
   * Get the list of actions available.
   * @return a list with all the available actions
   */
  public List<Action> getActions() {

    final List<Action> result = new ArrayList<>();

    for (String actionName : service.getServiceClasses().keySet()) {
      result.add(newService(actionName));
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  protected ActionService() {

    super(Action.class);
  }

}
