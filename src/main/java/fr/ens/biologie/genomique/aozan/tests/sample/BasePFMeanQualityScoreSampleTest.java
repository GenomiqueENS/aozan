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

package fr.ens.biologie.genomique.aozan.tests.sample;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.DemultiplexingCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * This class define a percent quality mean score sample test.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class BasePFMeanQualityScoreSampleTest extends AbstractSampleTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(DemultiplexingCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int read,
      final int readSample, final int sampleId) {

    final boolean undetermined = data.isUndeterminedSample(sampleId);

    final String prefix = "demux.sample" + sampleId + ".read" + readSample;

    try {
      final long qualityScoreSum =
          data.getLong(prefix + ".pf.quality.score.sum", 0);
      final long yield = data.getLong(prefix + ".pf.yield", 0);

      if (yield == 0) {
        return new TestResult("NA");
      }

      final double mean = (double) qualityScoreSum / (double) yield;

      if (interval == null || undetermined)
        return new TestResult(mean);

      return new TestResult(this.interval.getScore(mean), mean);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  //
  // Other methods
  //

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
  public BasePFMeanQualityScoreSampleTest() {

    super("sample.base.pf.mean.quality.score", "Base PF Mean Quality Score",
        "Base PF Mean Quality Score");
  }

}
