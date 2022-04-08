package fr.ens.biologie.genomique.aozan.tests.global;

import static fr.ens.biologie.genomique.aozan.collectors.ReadCollector.READ_DATA_PREFIX;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.GlobalStatsCollector;
import fr.ens.biologie.genomique.aozan.collectors.ReadCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * The class define test to compute the density cluster mean passing filtre for
 * the run.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class ClusterDensityPFGlobalTest extends AbstractGlobalTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(GlobalStatsCollector.COLLECTOR_NAME,
        ReadCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(RunData data) {

    final int laneCount = data.getLaneCount();
    final int readCount = data.getReadCount();

    double densitySum = 0;

    for (int lane = 1; lane <= laneCount; lane++) {
      for (int read = 1; read <= readCount; read++) {

        densitySum += data.getDouble(
            READ_DATA_PREFIX + ".read" + read + ".lane" + lane + ".density.pf");
      }
    }

    try {

      final double density = densitySum / (laneCount * readCount) / 1000.0;

      if (interval == null)
        return new TestResult(density);

      return new TestResult(this.interval.getScore(density), density);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  @Override
  public List<AozanTest> configure(TestConfiguration conf)
      throws AozanException {

    if (conf == null)
      throw new NullPointerException("The conf object is null");

    this.interval.configureDoubleInterval(conf);

    return Collections.singletonList((AozanTest) this);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public ClusterDensityPFGlobalTest() {

    super("global.cluster.density.pf", "Cluster Density passing filter",
        "Cluster Density PF", "k/mm²");
  }

}
