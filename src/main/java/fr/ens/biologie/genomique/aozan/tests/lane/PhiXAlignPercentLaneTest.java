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

package fr.ens.biologie.genomique.aozan.tests.lane;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.ReadCollector;
import fr.ens.biologie.genomique.aozan.collectors.SamplesheetCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * This class define a lane test on PhiX percent aligned.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class PhiXAlignPercentLaneTest extends AbstractLaneTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ReadCollector.COLLECTOR_NAME,
        SamplesheetCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int read,
      final boolean indexedRead, final int lane) {

    try {

      final double alignPhix = data.getReadPrcAlign(lane, read) / 100.0;

      final List<Integer> sampleIds = data.getSamplesInLane(lane);

      final boolean control =
          sampleIds.size() == 1 && data.isLaneControl(sampleIds.get(0));

      // No score for indexed read
      if (indexedRead || !control)
        return new TestResult(alignPhix, true);

      return new TestResult(this.interval.getScore(alignPhix), alignPhix, true);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  //
  // Other methods
  //

  @Override
  public List<AozanTest> configure(final TestConfiguration oonf)
      throws AozanException {

    if (oonf == null)
      throw new NullPointerException("The conf object is null");

    this.interval.configureDoubleInterval(oonf);

    return Collections.singletonList((AozanTest) this);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PhiXAlignPercentLaneTest() {

    super("lane.phix.align.percent", "PhiX Aligned", "PhiX Aligned", "%");
  }

}
