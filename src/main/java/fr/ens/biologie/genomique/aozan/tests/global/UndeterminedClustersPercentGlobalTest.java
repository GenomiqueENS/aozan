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

package fr.ens.biologie.genomique.aozan.tests.global;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.DemultiplexingCollector;
import fr.ens.biologie.genomique.aozan.collectors.GlobalStatsCollector;
import fr.ens.biologie.genomique.aozan.collectors.RunInfoCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

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
        DemultiplexingCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data) {

    try {

      final int laneCount = data.getLaneCount();
      final int tiles = data.getTilesCount();

      long rawClusterLaneIndexedSum = 0;
      long undeterminedClusterSum = 0;

      for (int lane = 1; lane <= laneCount; lane++) {

        // Check Undetermined fastq exist for this lane
        final String undeterminedBarcode =
            data.get("demux.lane" + lane + ".sample.lane" + lane + ".barcode");
        if (undeterminedBarcode != null) {

          // Sum raw cluster undetermined
          undeterminedClusterSum += data.getLong("demux.lane"
              + lane + ".sample.lane" + lane + ".read1.raw.cluster.count");

          rawClusterLaneIndexedSum +=
              data.getReadRawClusterCount(lane, 1) * tiles;
        }
      }

      // Percent undetermined in run only for the indexed lane
      final double undeterminedPrc =
          ((double) undeterminedClusterSum / rawClusterLaneIndexedSum);

      return new TestResult(this.interval.getScore(undeterminedPrc),
          undeterminedPrc, true);

    } catch (final NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  //
  // Other methods
  //

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
      throws AozanException {

    if (conf == null) {
      throw new NullPointerException("The properties object is null");
    }

    this.interval.configureDoubleInterval(conf);

    return Collections.singletonList((AozanTest) this);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public UndeterminedClustersPercentGlobalTest() {

    super("global.globalprcundeterminedcluster", "",
        "Undetermined Cluster Est.", "%");
  }

}
