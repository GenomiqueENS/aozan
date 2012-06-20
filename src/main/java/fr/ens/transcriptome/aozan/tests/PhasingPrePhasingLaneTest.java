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
import fr.ens.transcriptome.aozan.collectors.PhasingCollector;
import fr.ens.transcriptome.aozan.util.DoubleInterval;
import fr.ens.transcriptome.aozan.util.Interval;

/**
 * This class define a lane test on phasing / prephasing.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PhasingPrePhasingLaneTest extends AbstractLaneTest {

  private Interval phasingInterval;
  private Interval prephasingInterval;

  @Override
  public TestResult test(final RunData data, final int read,
      final boolean indexedRead, final int lane) {

    try {

      final String keyPrefix = "phasing.read" + read + ".lane" + lane;
      final double phasing = data.getDouble(keyPrefix + ".phasing");
      final double prephasing = data.getDouble(keyPrefix + ".prephasing");

      final List<String> sampleNames =
          Lists.newArrayList(Splitter.on(',').split(
              data.get("design.lane" + lane + ".samples.names")));

      final boolean control =
          sampleNames.size() == 1
              && data.getBoolean("design.lane"
                  + lane + "." + sampleNames.get(0) + ".control");

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
  public String[] getCollectorsNamesRequiered() {

    return new String[] {PhasingCollector.COLLECTOR_NAME};
  }

  //
  // Other methods
  //

  @Override
  public void configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    for (Map.Entry<String, String> e : properties.entrySet()) {

      if ("phasing.interval".equals(e.getKey().trim().toLowerCase()))
        this.phasingInterval = new DoubleInterval(e.getValue());

      if ("prephasing.interval".equals(e.getKey().trim().toLowerCase()))
        this.prephasingInterval = new DoubleInterval(e.getValue());
    }

  }

  //
  // Constructor
  //

  public PhasingPrePhasingLaneTest() {

    super("phasingprephasing", "", "Phasing / Prephasing");
  }

}
