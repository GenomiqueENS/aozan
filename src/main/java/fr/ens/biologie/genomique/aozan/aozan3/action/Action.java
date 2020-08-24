package fr.ens.biologie.genomique.aozan.aozan3.action;

import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;

/**
 * This interface define an action.
 * @since 3.0
 * @author Laurent Jourdren
 */
public interface Action {

  /**
   * Get the name of the action.
   * @return the name of the action
   */
  String getName();

  /**
   * Get action description.
   * @return the description description
   */
  String getDescription();

  /**
   * Execute action.
   * @param conf configuration
   * @param arguments arguments of the action
   * @param logger logger
   */
  void action(Configuration conf, List<String> arguments, AozanLogger logger);

  /**
   * Test if the action must be hidden from the list of available actions.
   * @return true if the action must be hidden
   */
  boolean isHidden();

}
