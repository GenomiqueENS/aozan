package fr.ens.biologie.genomique.aozan.tests.lane;

import static fr.ens.biologie.genomique.aozan.collectors.ReadCollector.READ_DATA_PREFIX;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.ReadCollector;

/**
 * This class define an occupancy percent lane test.
 * @since 3.0
 * @author Laurent Jourdren
 */
public class OccupancyPercentLaneTest extends AbstractSimpleLaneTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ReadCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(int read, boolean indexedRead, int lane) {

    return READ_DATA_PREFIX + ".read" + read + ".lane" + lane + ".prc.occupied";
  }

  @Override
  protected Class<?> getValueType() {

    return Double.class;
  }

  @Override
  protected Number transformValue(final Number value, final RunData data,
      final int read, final boolean indexedRead, final int lane) {

    return value.doubleValue() * 100.0;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public OccupancyPercentLaneTest() {

    super("lane.occupancy.percent", "Occupancy percent", "Occupancy", "%");
  }

}
