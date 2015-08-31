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
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.tests.samplestats;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.stats.SampleStatistics;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.TestResult;
import fr.ens.transcriptome.aozan.util.ScoreInterval;

/**
 * The class define test to compute the percent reads passing filter on all
 * samples replica in run.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class MeanQualityScoreSamplestatsTest extends AbstractSampleTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(SampleStatistics.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(RunData data, String sampleName) {

    if (sampleName == null) {
      return new TestResult("NA");
    }

    // Compile all Q30 value on sample,
    final int laneCount = data.getLaneCount();
    final int readCount = data.getReadCount();

    long qualityScoreSum = 0;
    long yieldSum = 0;
    int readIndexedCount = 0;
    
    try {
      for (int read = 1; read <= readCount; read++) {

        if (data.isReadIndexed(read))
          continue;
        
        readIndexedCount++;
        
        for (int lane = 1; lane <= laneCount; lane++) {

          final String prefix = buildPrefixRundata(sampleName, lane, readIndexedCount);
          qualityScoreSum += data.getLong(prefix + ".pf.quality.score.sum");
          yieldSum += data.getLong(prefix + ".pf.yield");
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

  private String buildPrefixRundata(final String sampleName, final int lane,
      final int read) {

    if (sampleName.equals(SampleStatistics.UNDETERMINED_SAMPLE))
      return "demux.lane" + lane + ".sample.lane" + lane + ".read" + read;

    return "demux.lane" + lane + ".sample." + sampleName + ".read" + read;

  }

  @Override
  public List<AozanTest> configure(Map<String, String> properties)
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
  public MeanQualityScoreSamplestatsTest() {
    super("meanqualityscorepfsamplestats", "", "Mean quality score base PF");
  }

}
