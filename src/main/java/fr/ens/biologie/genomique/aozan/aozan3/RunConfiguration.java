package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

/**
 * This class define a run configuration.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RunConfiguration extends Configuration {

  //
  // Constructors
  //

  /**
   * Default constructor.
   */
  public RunConfiguration() {
  }

  /**
   * Constructor.
   * @param conf configuration to copy
   */
  public RunConfiguration(RunConfiguration conf) {

    requireNonNull(conf);

    set(conf);
  }

}
