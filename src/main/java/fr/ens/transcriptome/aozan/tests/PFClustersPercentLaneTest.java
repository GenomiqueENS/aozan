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
 * This class a test on passing filter cluster in a lane.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PFClustersPercentLaneTest extends AbstractLaneTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public String[] getCollectorsNamesRequiered() {

    return new String[] {ReadCollector.COLLECTOR_NAME};
  }

  @Override
  public TestResult test(final RunData data, final int read,
      final boolean indexedRead, final int lane) {

    final long clusterRaw =
        data.getLong("read" + read + ".lane" + lane + ".clusters.raw");

    final long clusterPF =
        data.getLong("read" + read + ".lane" + lane + ".clusters.pf");

    final double percent = (double) clusterPF / (double) clusterRaw;

    // No score for indexed read
    if (indexedRead)
      return new TestResult(percent, true);

    return new TestResult(this.interval.getScore(percent), percent, true);
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
  public PFClustersPercentLaneTest() {

    super("pfclusterspercent", "", "PF Clusters", "%");
  }

}
