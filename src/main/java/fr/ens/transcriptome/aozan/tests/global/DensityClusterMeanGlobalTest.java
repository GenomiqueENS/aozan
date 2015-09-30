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
import fr.ens.transcriptome.aozan.collectors.GlobalStatsCollector;
import fr.ens.transcriptome.aozan.collectors.ReadCollector;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.TestResult;
import fr.ens.transcriptome.aozan.util.ScoreInterval;

/**
 * The class define test to compute the density cluster mean for the run.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class DensityClusterMeanGlobalTest extends AbstractGlobalTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(GlobalStatsCollector.COLLECTOR_NAME,
        ReadCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(RunData data) {

    // Compute error rate per lane
    final int laneCount = data.getLaneCount();
    final double densityRatio = data.getDouble("read1.density.ratio");

    long clusterRawSum = 0;

    for (int lane = 1; lane <= laneCount; lane++) {

      clusterRawSum += data.getLong("read1.lane" + lane + ".clusters.raw");

    }

    try {
      final long clusterRawMean = clusterRawSum / laneCount;
      final double density = clusterRawMean * densityRatio / 1000.0;

      if (interval == null)
        return new TestResult(density);

      return new TestResult(this.interval.getScore(density), density);

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
  public DensityClusterMeanGlobalTest() {
    super("globaldensitycluster", "", "Density Cluster");
  }

}
