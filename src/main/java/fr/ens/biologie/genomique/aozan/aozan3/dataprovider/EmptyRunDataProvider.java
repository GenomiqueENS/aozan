package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;

/**
 * This class define an empty run data provider.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class EmptyRunDataProvider implements RunDataProvider {

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
