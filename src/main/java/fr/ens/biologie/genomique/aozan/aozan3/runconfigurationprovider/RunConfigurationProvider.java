package fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;

/**
 * This interface define a run configuration provider.
 * @author Laurent Jourdren
 * @since 3.0
 */
public interface RunConfigurationProvider {

  /**
   * Get the name of the run configuration provider.
   * @return the name of the run configuration provider
   */
  public String getName();

  /**
   * Get the run configuration for a run data
   * @param runData run data
   * @return a RunConfiguration object
   * @throws Aozan3Exception if an error occurs while getting the configuration
   */
  RunConfiguration getRunConfiguration(RunData runData) throws Aozan3Exception;

}
