package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define an empty run data provider.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class EmptyRunDataProvider implements RunDataProvider {

  public static final String PROVIDER_NAME = "empty";

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public void init(DataStorage storage, Configuration conf, GenericLogger logger)
      throws Aozan3Exception {
  }

  @Override
  public boolean canProvideRunData() {
    return false;
  }

  @Override
  public List<RunData> listInProgressRunData() {
    return Collections.emptyList();
  }

  @Override
  public List<RunData> listCompletedRunData() {
    return Collections.emptyList();
  }

  @Override
  public DataStorage getDataStorage() {
    return null;
  }

}
