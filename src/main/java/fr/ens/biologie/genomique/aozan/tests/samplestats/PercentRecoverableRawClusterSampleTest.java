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
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.stats.SampleStatistics;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * The class define test to compute the percent recoverable raw clusters for a
 * project.
 * @author Sandrine Perrin
 * @since 1.4
 */
public class PercentRecoverableRawClusterSampleTest extends AbstractSampleTest {

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

    // Build key for run data
    final String recoveryCountKey =
        SampleStatistics.COLLECTOR_PREFIX
            + sampleName + ".raw.cluster.recovery.sum";
    final String rawClusterSumKey =
        SampleStatistics.COLLECTOR_PREFIX + sampleName + ".raw.cluster.sum";

    try {
      // Set raw cluster sum for a project
      final long rawClusterCount = data.getLong(rawClusterSumKey);

      // Set recoverable raw cluster sum for a project
      final long recoveryCount = data.getLong(recoveryCountKey);

      // Compute percent
      final double percent = (double) recoveryCount / (double) rawClusterCount;

      if (interval == null)
        return new TestResult(percent, true);

      return new TestResult(this.interval.getScore(percent), percent, true);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }

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
  public PercentRecoverableRawClusterSampleTest() {
    super("samplestatsrecoverablerawclusterpercent", "",
        "Recoverable raw cluster ", "%");
  }

}
