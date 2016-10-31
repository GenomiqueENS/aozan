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

package fr.ens.biologie.genomique.aozan.tests.pooledsample;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.DemultiplexingCollector;
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
public class Q30PercentPooledSampleTest extends AbstractPooledSampleTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(SampleStatisticsCollector.COLLECTOR_NAME,
        DemultiplexingCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int pooledSampleId) {

    // Compile all Q30 value on sample,
    final int readCount = data.getReadCount();

    long q30Cumul = 0;
    long rawCumul = 0;
    int readIndexedCount = 0;

    try {
      for (int read = 1; read <= readCount; read++) {

        if (data.isReadIndexed(read)) {
          continue;
        }

        readIndexedCount++;

        for (int sampleId : data.getSamplesInPooledSample(pooledSampleId)) {

          final String prefix =
              // buildPrefixRundata(sampleName, lane, readIndexedCount);
              "demux.sample" + sampleId + ".read" + readIndexedCount;

          final String q30Key = prefix + ".pf.yield.q30";
          final String rawKey = prefix + ".pf.yield";

          // Sample must exist in the lane
          if (data.contains(q30Key) && data.contains(rawKey)) {
            q30Cumul += data.getLong(q30Key);
            rawCumul += data.getLong(rawKey);
          }
        }
      }

      // Compute mean

      final double percent = (double) q30Cumul / (double) rawCumul;

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
  public Q30PercentPooledSampleTest() {

    super("pooledsample.q30.percent", "Q30", "Q30", "%");
  }

}
