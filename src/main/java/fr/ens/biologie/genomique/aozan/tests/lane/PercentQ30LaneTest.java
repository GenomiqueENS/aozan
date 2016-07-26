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
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.MathUtils;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * This class define a Q30 percent lane test.
 * @since 2.0
 * @author Cyril Firmo
 */
public class PercentQ30LaneTest extends AbstractLaneTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
      throws AozanException {

    if (conf == null) {
      throw new NullPointerException("The properties object is null");
    }

    this.interval.configureDoubleInterval(conf);

    return Collections.singletonList((AozanTest) this);
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ReadCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(RunData data, int read, boolean indexedRead,
      int lane) {
    long[] values =
        data.getLongArray("qualitymetrics.lane" + lane + ".read" + read);
    double q30 = 0;

    // Do the test ?
    q30 = MathUtils.computeQ30(values);

    if (this.interval == null) {
      return new TestResult(q30, true);
    }

    return new TestResult(this.interval.getScore(q30), q30, true);

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PercentQ30LaneTest() {

    super("lanepercentq30", "", "Q30", "%");
  }

}
