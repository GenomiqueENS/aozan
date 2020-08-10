package fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider;

import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;

/**
 * This class define an empty run configuration provider
 * @author Laurent Jourdren
 * @since 3.0
 */
public class EmptyRunConfigurationProvider implements RunConfigurationProvider {

  public static final String PROVIDER_NAME = "empty";

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public RunConfiguration getRunConfiguration(final RunData runData) {

    return new RunConfiguration();
  }

}
