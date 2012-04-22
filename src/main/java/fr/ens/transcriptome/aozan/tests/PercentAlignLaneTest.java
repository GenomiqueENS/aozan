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

import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.DesignCollector;
import fr.ens.transcriptome.aozan.collectors.ReadCollector;
import fr.ens.transcriptome.aozan.util.DoubleInterval;
import fr.ens.transcriptome.aozan.util.Interval;

/**
 * This class define a lane test on PhiX percent aligned.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PercentAlignLaneTest extends AbstractLaneTest {

  private final Interval interval;

  @Override
  public String[] getCollectorsNamesRequiered() {

    return new String[] {ReadCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME};
  }

  @Override
  public TestResult test(final RunData data, final int read,
      final boolean indexedRead, final int lane) {

    final double alignPhix =
        data.getDouble("read" + read + ".lane" + lane + ".prc.align") / 100.0;

    final List<String> sampleNames =
        Lists.newArrayList(Splitter.on(',').split(
            data.get("design.lane" + lane + ".samples.names")));

    final boolean control =
        sampleNames.size() == 1
            && data.getBoolean("design.lane"
                + lane + "." + sampleNames.get(0) + ".control");

    // No score for indexed read
    if (indexedRead || !control)
      return new TestResult(alignPhix, true);

    return new TestResult(this.interval.isInInterval(alignPhix) ? 9 : 0,
        alignPhix, true);

  }

  //
  // Other methods
  //

  @Override
  public void configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PercentAlignLaneTest() {

    super("percentalign", "", "PhiX Align", "%");
    this.interval = new DoubleInterval(0.75, 1.0);
  }

}
