package fr.ens.transcriptome.aozan.tests;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.GlobalStatsCollector;
import fr.ens.transcriptome.aozan.collectors.ReadCollector;

/**
 * This class define a Passing Filter cluster global test.
 * @since 1.x
 * @author Laurent Jourdren
 */
public class PFClustersGlobalTest extends AbstractSimpleGlobalTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(GlobalStatsCollector.COLLECTOR_NAME,
        ReadCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey() {

    return "globalstats.clusters.pf";
  }

  @Override
  protected Class<?> getValueType() {

    return Long.class;
  }

  @Override
  protected Number transformValue(final Number value, final RunData data) {

    final int tiles = data.getInt("read1.lane1.tile.count");

    return value.longValue() * tiles;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PFClustersGlobalTest() {

    super("globalpfclusters", "", "PF Clusters Est.");
  }

}
