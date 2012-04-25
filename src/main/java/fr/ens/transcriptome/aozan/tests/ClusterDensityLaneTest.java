/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan.tests;

import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.ReadCollector;
import fr.ens.transcriptome.aozan.util.ScoreInterval;

/**
 * This class define a lane test on cluster density.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ClusterDensityLaneTest extends AbstractLaneTest {

  private ScoreInterval interval = new ScoreInterval();

  @Override
  public String[] getCollectorsNamesRequiered() {

    return new String[] {ReadCollector.COLLECTOR_NAME};
  }

  @Override
  public TestResult test(final RunData data, final int read,
      final boolean indexedRead, final int lane) {

    final long clusterRaw =
        data.getLong("read" + read + ".lane" + lane + ".clusters.raw");

    final double densityRatio =
        data.getDouble("read" + read + ".density.ratio");

    final double density = clusterRaw * densityRatio / 1000.0;

    if (interval == null || indexedRead)
      return new TestResult(density);

    return new TestResult(this.interval.getScore(density), density);
  }

  //
  // Other methods
  //

  @Override
  public void configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    this.interval.configureDoubleInterval(properties);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public ClusterDensityLaneTest() {

    super("clusterdensity", "", "Cluster Density", "k/mm2");
  }
}
