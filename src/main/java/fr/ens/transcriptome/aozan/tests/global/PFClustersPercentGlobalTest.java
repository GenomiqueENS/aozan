package fr.ens.transcriptome.aozan.tests.global;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.GlobalStatsCollector;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.TestResult;
import fr.ens.transcriptome.aozan.util.ScoreInterval;

/**
 * This class is a test on passing filter clusters in a lane.
 * @since 1.x
 * @author Laurent Jourdren
 */
public class PFClustersPercentGlobalTest extends AbstractGlobalTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(GlobalStatsCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data) {

    try {

      final long clusterRaw = data.getLong("globalstats.clusters.raw");
      final long clusterPF = data.getLong("globalstats.clusters.pf");

      final double percent = (double) clusterPF / (double) clusterRaw;

      return new TestResult(this.interval.getScore(percent), percent, true);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  //
  // Other methods
  //

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    this.interval.configureDoubleInterval(properties);

    return Collections.singletonList((AozanTest) this);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PFClustersPercentGlobalTest() {

    super("globalpfclusterspercent", "", "PF Clusters", "%");
  }

}
