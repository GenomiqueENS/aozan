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

package fr.ens.transcriptome.aozan.tests.global;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FlowcellDemuxSummaryCollector;
import fr.ens.transcriptome.aozan.collectors.GlobalStatsCollector;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.TestResult;
import fr.ens.transcriptome.aozan.util.ScoreInterval;

/**
 * This class define a projects count global test.
 * @since 1.3
 * @author Sandrine Perrin
 */
public class UndeterminedClustersPercentGlobalTest extends AbstractGlobalTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(GlobalStatsCollector.COLLECTOR_NAME,
        RunInfoCollector.COLLECTOR_NAME,
        FlowcellDemuxSummaryCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data) {

    try {

      final int laneCount = data.getLaneCount();
      final int tiles = data.getTilesCount();

      final long rawClusterCount =
          data.getLong("globalstats.clusters.raw.count") * tiles;

      long undeterminedClusterSum = 0;

      for (int lane = 1; lane <= laneCount; lane++) {
        // Sum raw cluster undetermined
        undeterminedClusterSum +=
            data.getLong("demux.lane"
                + lane + ".sample.lane" + lane + ".read1.raw.cluster.count");
      }

      // Percent undetermined in run
      final double undeterminedPrc =
          ((double) undeterminedClusterSum / rawClusterCount);

      return new TestResult(this.interval.getScore(undeterminedPrc),
          undeterminedPrc, true);

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
  public UndeterminedClustersPercentGlobalTest() {

    super("globalprcundeterminedcluster", "", "Undetermined Cluster Est.", "%");
  }

}