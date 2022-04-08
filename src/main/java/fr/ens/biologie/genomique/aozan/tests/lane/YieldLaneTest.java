package fr.ens.biologie.genomique.aozan.tests.lane;

import static fr.ens.biologie.genomique.aozan.collectors.ReadCollector.READ_DATA_PREFIX;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.ReadCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;

/**
 * This class define a yield lane test.
 * @since 3.0
 * @author Laurent Jourdren
 */
public class YieldLaneTest extends AbstractSimpleLaneTest {

  private static final String ILLUMINA_COMPUTATION_MODE = "legacy";
  private boolean illuminaComputation = false;

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ReadCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(int read, boolean indexedRead, int lane) {

    return READ_DATA_PREFIX + ".read" + read + ".lane" + lane + ".clusters.raw";
  }

  @Override
  protected Class<?> getValueType() {

    return Double.class;
  }

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
      throws AozanException {

    List<AozanTest> result = super.configure(conf);

    if (conf.containsKey(ILLUMINA_COMPUTATION_MODE)) {
      this.illuminaComputation =
          Boolean.parseBoolean(conf.get(ILLUMINA_COMPUTATION_MODE).trim());
    }

    return result;
  }

  @Override
  protected Number transformValue(final Number value, final RunData data,
      final int read, final boolean indexedRead, final int lane) {

    final int tiles = data.getTilesCount();
    final int readLength =
        data.getReadCyclesCount(read) - (this.illuminaComputation ? 1 : 0);

    return value.longValue() * tiles * readLength / 1_000_000_000.0;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public YieldLaneTest() {

    super("lane.yield", "Yield", "Yield", "Gb");
  }

}
