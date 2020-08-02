package fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider;

import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;

public class EmptyRunConfigurationProvider implements RunConfigurationProvider {

  @Override
  public RunConfiguration getRunConfiguration(final RunData runData) {

    return new RunConfiguration();
  }

}
