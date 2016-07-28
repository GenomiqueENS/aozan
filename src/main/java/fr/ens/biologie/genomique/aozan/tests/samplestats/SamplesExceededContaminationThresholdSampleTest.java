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
 * The class define test on samples count on a project which exceed a
 * contaminant threshold on percent mapped reads on dataset contaminants
 * genomes. The default value of threshold is 0.10, can be redefine in
 * configuration file.
 * @author Sandrine Perrin
 * @since 1.4
 */
public class SamplesExceededContaminationThresholdSampleTest
    extends AbstractSampleStatsTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(SampleStatisticsCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int pooledSampleId) {

    // Do no process undetermined samples
    if (data.isUndeterminedPooledSample(pooledSampleId)) {
      return new TestResult("NA");
    }

    try {
      final String key = COLLECTOR_PREFIX
          + ".pooledsample" + pooledSampleId
          + ".samples.exceeded.contamination.threshold.count";

      final int val = data.getInt(key);

      if (interval == null)
        return new TestResult(val);

      return new TestResult(this.interval.getScore(val), val);

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
  public SamplesExceededContaminationThresholdSampleTest() {
    super("samplestats.samplestatexceededcontaminationthreshold", "",
        "Sample(s) exceeded contamination threshold");
  }

}
