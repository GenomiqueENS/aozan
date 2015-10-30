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

package fr.ens.transcriptome.aozan.tests.projectstats;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.UndeterminedIndexesCollector;
import fr.ens.transcriptome.aozan.collectors.stats.ProjectStatistics;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.TestResult;
import fr.ens.transcriptome.aozan.util.ScoreInterval;

/**
 * The class define test to compute the percent recoverable raw clusters for a
 * project.
 * @author Sandrine Perrin
 * @since 1.4
 */
public class PercentRecoverableRawClusterProjectTest extends
    AbstractProjectTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(UndeterminedIndexesCollector.COLLECTOR_NAME,
        ProjectStatistics.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(RunData data, String projectName) {

    if (projectName == null) {
      return new TestResult("NA");
    }

    // Build key for run data
    final String recoveryCountKey =
        ProjectStatistics.COLLECTOR_PREFIX
            + projectName + ".raw.cluster.recovery.sum";
    final String rawClusterSumKey =
        ProjectStatistics.COLLECTOR_PREFIX
            + projectName + ".raw.cluster.sum";

    try {
      // Set raw cluster sum for a project
      final long rawClusterCount = data.getLong(rawClusterSumKey);

      // Set recoverable raw cluster sum for a project
      final long recoveryCount;

      if (data.get(recoveryCountKey)==null) {
        recoveryCount = 0;
      } else {
        recoveryCount = data.getLong(recoveryCountKey);
      }

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
  public PercentRecoverableRawClusterProjectTest() {
    super("recoverablerawclusterpercent", "", "Recoverable raw cluster ", "%");
  }

}
