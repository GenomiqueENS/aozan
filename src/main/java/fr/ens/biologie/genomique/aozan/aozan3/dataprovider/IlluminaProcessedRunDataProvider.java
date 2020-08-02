package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;

/**
 * This class define a processed data provider for Illumina runs.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IlluminaProcessedRunDataProvider implements RunDataProvider {

  @Override
  public boolean canProvideRunData() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<RunData> listInProgressRunData() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<RunData> listCompletedRunData() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DataStorage getDataStorage() {
    // TODO Auto-generated method stub
    return null;
  }

}
