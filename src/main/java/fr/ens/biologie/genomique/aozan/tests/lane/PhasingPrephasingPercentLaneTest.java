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

import static fr.ens.biologie.genomique.aozan.collectors.ReadCollector.READ_DATA_PREFIX;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.ReadCollector;
import fr.ens.biologie.genomique.aozan.collectors.SamplesheetCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.DoubleInterval;
import fr.ens.biologie.genomique.aozan.util.Interval;

/**
 * This class define a lane test on phasing / prephasing.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class PhasingPrephasingPercentLaneTest extends AbstractLaneTest {

  private Interval phasingInterval;
  private Interval prephasingInterval;

  @Override
  public TestResult test(final RunData data, final int read,
      final boolean indexedRead, final int lane) {

    try {

      final String keyPrefix =
          READ_DATA_PREFIX + ".read" + read + ".lane" + lane;
      final double phasing = data.getDouble(keyPrefix + ".phasing") / 100.0;
      final double prephasing =
          data.getDouble(keyPrefix + ".prephasing") / 100.0;

      final List<Integer> sampleIds = data.getSamplesInLane(lane);

      final boolean control =
          sampleIds.size() == 1 && data.isLaneControl(sampleIds.get(0));

      final String message = String.format("%,.3f%% / %,.3f%%", phasing * 100.0,
          prephasing * 100.0) + (control ? " (C)" : "");

      // No score for indexed read
      if (indexedRead
          || this.phasingInterval == null || this.prephasingInterval == null)
        return new TestResult(message);

      final boolean result = phasingInterval.isInInterval(phasing)
          || prephasingInterval.isInInterval(prephasing);

      return new TestResult(result ? 9 : 0, message);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ReadCollector.COLLECTOR_NAME,
        SamplesheetCollector.COLLECTOR_NAME);
  }

  //
  // Other methods
  //

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
      throws AozanException {

    if (conf == null)
      throw new NullPointerException("The properties object is null");

    for (Map.Entry<String, String> e : conf.entrySet()) {

      if ("phasing.interval".equals(e.getKey().trim().toLowerCase()))
        this.phasingInterval = new DoubleInterval(e.getValue());

      if ("prephasing.interval".equals(e.getKey().trim().toLowerCase()))
        this.prephasingInterval = new DoubleInterval(e.getValue());
    }

    return Collections.singletonList((AozanTest) this);

  }

  //
  // Constructor
  //

  public PhasingPrephasingPercentLaneTest() {

    super("lane.phasing.prephasing.percent", "Phasing / Prephasing",
        "Phasing / Prephasing");
  }

}
