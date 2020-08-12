package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;

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
  public void init(DataStorage storage, Configuration conf, AozanLogger logger)
      throws Aozan3Exception;

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
