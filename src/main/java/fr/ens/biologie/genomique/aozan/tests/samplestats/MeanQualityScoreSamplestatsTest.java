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
public class MeanQualityScoreSamplestatsTest extends AbstractSampleStatsTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(SampleStatisticsCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int pooledSampleId) {

    // Compile all Q30 value on sample,
    final int readCount = data.getReadCount();

    long qualityScoreSum = 0;
    long yieldSum = 0;
    int readIndexedCount = 0;

    try {
      for (int read = 1; read <= readCount; read++) {

        if (data.isReadIndexed(read))
          continue;

        readIndexedCount++;

        for (int sampleId : data.getSamplesInPooledSample(pooledSampleId)) {

          final String prefix =
              "demux.sample" + sampleId + ".read" + readIndexedCount;
          final String qualityScoreKey = prefix + ".pf.quality.score.sum";
          final String yieldKey = prefix + ".pf.yield";

          // Sample must exist in the lane
          if (data.contains(qualityScoreKey) && data.contains(yieldKey)) {
            qualityScoreSum += data.getLong(qualityScoreKey);
            yieldSum += data.getLong(yieldKey);
          }
        }
      }

      // Compute mean
      final double mean = (double) qualityScoreSum / (double) yieldSum;

      if (interval == null)
        return new TestResult(mean);

      return new TestResult(this.interval.getScore(mean), mean);

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
  public MeanQualityScoreSamplestatsTest() {
    super("meanqualityscorepfsamplestats", "", "Mean quality score base PF");
  }

}
