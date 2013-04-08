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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FlowcellDemuxSummaryCollector;
import fr.ens.transcriptome.aozan.util.ScoreInterval;

/**
 * This class define a percent passing filter sample test.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PercentPFSampleTest extends AbstractSampleTest {

  private ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(FlowcellDemuxSummaryCollector.COLLECTOR_NAME);
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

    try {
      final long raw = data.getLong(prefix + ".raw.cluster.count");
      final long pf = data.getLong(prefix + ".pf.cluster.count");
      final double percent = (double) pf / (double) raw;

      if (interval == null || sampleName == null)
        return new TestResult(percent, true);

      return new TestResult(this.interval.getScore(percent), percent, true);

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
  public PercentPFSampleTest() {
    super("percentpfsample", "", "Passing filter", "%");
  }

}
