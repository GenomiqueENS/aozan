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

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FlowcellDemuxSummaryCollector;
import fr.ens.transcriptome.aozan.util.DoubleInterval;
import fr.ens.transcriptome.aozan.util.Interval;

/**
 * This class define a percent passing filter sample test.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PercentPFSampleTest extends AbstractSampleTest {

  private Interval interval;

  @Override
  public String[] getCollectorsNamesRequiered() {

    return new String[] {FlowcellDemuxSummaryCollector.COLLECTOR_NAME};
  }

  @Override
  public TestResult test(final RunData data, final int re2ad,
      final int readSample, final int lane, final String sampleName) {

    final String prefix;

    if (sampleName == null)
      prefix =
          "demux.lane" + lane + ".sample.lane" + lane + ".read" + readSample;
    else
      prefix =
          "demux.lane" + lane + ".sample." + sampleName + ".read" + readSample;

    final long raw = data.getLong(prefix + ".raw.cluster.count");
    final long pf = data.getLong(prefix + ".pf.cluster.count");
    final double percent = (double) pf / (double) raw;

    if (interval == null || sampleName == null)
      return new TestResult(percent, true);

    return new TestResult(this.interval.isInInterval(percent) ? 9 : 0, percent,
        true);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PercentPFSampleTest() {
    super("percentpfsample", "", "Passing filter", "%");
    this.interval = new DoubleInterval(0.6, 1.0);
  }

}
