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

package fr.ens.biologie.genomique.aozan.tests.sample;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.SamplesheetCollector;
import fr.ens.biologie.genomique.aozan.collectors.FlowcellDemuxSummaryCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestResult;

/**
 * This class define a sample percent in lane test.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class PercentInLaneSampleTest extends AbstractSampleTest {

  private static final String MARGE_PERCENT_IN_LANE_KEY = "distance";
  private double distance = 0.0;

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(FlowcellDemuxSummaryCollector.COLLECTOR_NAME,
        SamplesheetCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int read,
      final int readSample, final int lane, final String sampleName) {

    final String rawSampleKey;

    if (sampleName == null) {
      rawSampleKey =
          "demux.lane"
              + lane + ".sample.lane" + lane + ".read" + readSample
              + ".raw.cluster.count";
    } else {
      rawSampleKey =
          "demux.lane"
              + lane + ".sample." + sampleName + ".read" + readSample
              + ".raw.cluster.count";
    }

    final String rawAll =
        "demux.lane" + lane + ".all.read" + readSample + ".raw.cluster.count";

    try {

      final long raw = data.getLong(rawSampleKey);
      final long all = data.getLong(rawAll);

      final double percent = (double) raw / (double) all;

      // Configure score
      final double homogeneityInLane =
          1 / data.getSamplesNameListInLane(lane).size();

      if (this.distance >= homogeneityInLane) {
        this.distance = 0.0;
      }

      final double min = homogeneityInLane - this.distance;
      final double max = homogeneityInLane + this.distance;

      // If distance not set, score = -1
      final int score =
          (this.distance == 0.0 ? -1 : (percent > max || percent < min) ? 4 : 9);

      if (sampleName == null) {
        return new TestResult(percent, true);
      }

      return new TestResult(score, percent, true);

    } catch (final NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  //
  // Other methods
  //

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null) {
      throw new NullPointerException("The properties object is null");
    }

    final String d = properties.get(MARGE_PERCENT_IN_LANE_KEY);
    if (d != null) {
      this.distance =
          Double.parseDouble(properties.get(MARGE_PERCENT_IN_LANE_KEY));

      if (this.distance >= 1.0) {
        this.distance = 0.0;
      }
    }

    return Collections.singletonList((AozanTest) this);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PercentInLaneSampleTest() {
    super("percentinlanesample", "", "Sample in lane", "%");
  }

}
