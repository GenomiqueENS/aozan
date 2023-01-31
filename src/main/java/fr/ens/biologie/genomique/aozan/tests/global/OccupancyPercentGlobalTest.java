package fr.ens.biologie.genomique.aozan.tests.global;

import static fr.ens.biologie.genomique.aozan.collectors.ReadCollector.READ_DATA_PREFIX;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.ReadCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * This class define an occupancy percent global test.
 * @since 3.0
 * @author Laurent Jourdren
 */
public class OccupancyPercentGlobalTest extends AbstractGlobalTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return ImmutableList.of(ReadCollector.COLLECTOR_NAME);
  }

  @Override
  public List<AozanTest> configure(TestConfiguration conf)
      throws AozanException {

    if (conf == null) {
      throw new NullPointerException("The conf object is null");
    }

    this.interval.configureDoubleInterval(conf);

    return Collections.singletonList((AozanTest) this);
  }

  @Override
  public TestResult test(RunData data) {

    final int laneCount = data.getLaneCount();
    final int readCount = data.getReadCount();

    double sum = 0;

    for (int lane = 1; lane <= laneCount; lane++) {
      for (int read = 1; read <= readCount; read++) {

        sum += data.getDouble(READ_DATA_PREFIX
            + ".read" + read + ".lane" + lane + ".prc.occupied");
      }
    }

    try {

      final double occupancy = sum / (laneCount * readCount) * 100.0;

      if (interval == null)
        return new TestResult(occupancy);

      return new TestResult(this.interval.getScore(occupancy), occupancy);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public OccupancyPercentGlobalTest() {

    super("global.occupancy.percent", "Occupancy percent", "Occupancy", "%");
  }

}
