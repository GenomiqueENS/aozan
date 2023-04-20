package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This interface define a run data provider.
 * @author Laurent Jourdren
 * @since 3.0
 */
public interface RunDataProvider {

  /**
   * Get the name of the provider.
   * @return the name of the provider
   */
  public String getName();

  /**
   * Initialize the provider.
   * @param storage used for the source
   * @param conf the configuration of the provider
   * @param logger the logger to use
   * @throws Aozan3Exception if an error occurs while initialize the provider
   */
  public void init(DataStorage storage, Configuration conf,
      GenericLogger logger) throws Aozan3Exception;

  /**
   * Test if provider can provide RunData
   * @return true if provider can provide RunData
   */
  boolean canProvideRunData();

  /**
   * List available runs in progress.
   * @param excludedRuns runs to exclude
   * @return a list of RunData
   */
  List<RunData> listInProgressRunData(Collection<RunId> excludedRuns);

  /**
   * List available runs in progress.
   * @return a list of RunData
   */
  default List<RunData> listInProgressRunData() {

    return listInProgressRunData(Collections.emptyList());
  }

  /**
   * List available runs in progress.
   * @param excludedRuns runs to exclude
   * @return a list of RunData
   */
  List<RunData> listCompletedRunData(Collection<RunId> excludedRuns);

  /**
   * List available completed runs.
   * @return a list of RunData
   */
  default List<RunData> listCompletedRunData() {

    return listCompletedRunData(Collections.emptyList());
  }

  /**
   * Get the data storage of used by the provider.
   * @return the DataStorage used by the provider
   */
  DataStorage getDataStorage();

}
