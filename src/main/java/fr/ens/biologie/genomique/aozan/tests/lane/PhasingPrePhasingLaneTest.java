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

package fr.ens.biologie.genomique.aozan.tests.lane;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.DesignCollector;
import fr.ens.biologie.genomique.aozan.collectors.PhasingCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.DoubleInterval;
import fr.ens.biologie.genomique.aozan.util.Interval;

/**
 * This class define a lane test on phasing / prephasing.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class PhasingPrePhasingLaneTest extends AbstractLaneTest {

  private Interval phasingInterval;
  private Interval prephasingInterval;

  @Override
  public TestResult test(final RunData data, final int read,
      final boolean indexedRead, final int lane) {

    try {

      final String keyPrefix = "read" + read + ".lane" + lane;
      final double phasing = data.getDouble(keyPrefix + ".phasing") / 100.0;
      final double prephasing =
          data.getDouble(keyPrefix + ".prephasing") / 100.0;

      final List<String> sampleNames = data.getSamplesNameListInLane(lane);

      final boolean control =
          sampleNames.size() == 1
              && data.isLaneControl(lane, sampleNames.get(0));

      final String message =
          String.format("%,.3f%% / %,.3f%%", phasing * 100.0,
              prephasing * 100.0) + (control ? " (C)" : "");

      // No score for indexed read
      if (indexedRead
          || this.phasingInterval == null || this.prephasingInterval == null)
        return new TestResult(message);

      final boolean result =
          phasingInterval.isInInterval(phasing)
              || prephasingInterval.isInInterval(prephasing);

      return new TestResult(result ? 9 : 0, message);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(PhasingCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME);
  }

  //
  // Other methods
  //

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    for (Map.Entry<String, String> e : properties.entrySet()) {

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

  public PhasingPrePhasingLaneTest() {

    super("phasingprephasing", "", "Phasing / Prephasing");
  }

}
