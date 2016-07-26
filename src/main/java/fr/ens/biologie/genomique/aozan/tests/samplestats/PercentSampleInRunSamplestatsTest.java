/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.tests.samplestats;

import static fr.ens.biologie.genomique.aozan.collectors.stats.SampleStatisticsCollector.COLLECTOR_PREFIX;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.stats.SampleStatisticsCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * The class define test to compute the percent reads passing filter on all
 * samples replica in run.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class PercentSampleInRunSamplestatsTest extends AbstractSampleStatsTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(SampleStatisticsCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int pooledSampleId) {

    // Build key for run data
    final String rawClusterSumKey = COLLECTOR_PREFIX
        + ".pooledsample" + pooledSampleId + ".raw.cluster.sum";

    final int laneCount = data.getLaneCount();

    final long rawClusterSampleSum = data.getLong(rawClusterSumKey);

    long rawClusterInRunSum = 0;

    try {
      // Parse all samples, each lane should contain same sample
      for (int lane = 1; lane <= laneCount; lane++) {

        final String key = "demux.lane" + lane + ".all.read1.raw.cluster.count";

        rawClusterInRunSum += data.getLong(key);
      }

      // Compute percent sample in project
      final double percent =
          (double) rawClusterSampleSum / (double) rawClusterInRunSum;

      if (interval == null)
        return new TestResult(percent, true);

      return new TestResult(this.interval.getScore(percent), percent, true);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
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
  public PercentSampleInRunSamplestatsTest() {
    super("percentsampleinrun", "", "Sample in Run", "%");
  }

}
