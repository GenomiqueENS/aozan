package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;

/**
 * This interface define a run data provider.
 * @author Laurent Jourdren
 * @since 3.0
 */
public interface RunDataProvider {

  /**
   * Test if provider can provide RunData
   * @return true if provider can provide RunData
   */
  boolean canProvideRunData();

  /**
   * List available runs in progress.
   * @return a list of RunData
   */
  List<RunData> listInProgressRunData();

  /**
   * List available completed runs.
   * @return a list of RunData
   */
  List<RunData> listCompletedRunData();

  /**
   * Get the data storage of used by the provider.
   * @return the DataStorage used by the provider
   */
  DataStorage getDataStorage();

}
