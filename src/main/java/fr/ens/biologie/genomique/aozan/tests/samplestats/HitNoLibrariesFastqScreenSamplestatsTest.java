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

package fr.ens.biologie.genomique.aozan.tests.samplestats;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.FastqScreenCollector;
import fr.ens.biologie.genomique.aozan.collectors.stats.SampleStatistics;
import fr.ens.biologie.genomique.aozan.tests.TestResult;

/**
 * The class adds in the qc report html one result from fastqScreen for each
 * sample, after compile replicat data. It print the percent of reads which
 * mapped on at least one genomes except the genome sample.
 * @since 2.0
 * @author Sandrine Perrin
 */
public class HitNoLibrariesFastqScreenSamplestatsTest extends
    AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return ImmutableList.of(FastqScreenCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final String sampleName) {

    // Compute error rate per lane
    final int laneCount = data.getLaneCount();

    double value = 0;

    for (int lane = 1; lane <= laneCount; lane++) {

      final String key = getKey(sampleName, lane);

      // Check value exist
      if (data.get(key) == null) {
        return new TestResult("NA");
      }

      value += data.getDouble(key);

    }

    try {
      final double percent = value / (double) laneCount;

      if (getInterval() == null)
        return new TestResult(percent);

      return new TestResult(getInterval().getScore(percent), percent, true);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  @Override
  public String getKey(final String sampleName) {
    throw new UnsupportedOperationException();
  }

  public String getKey(final String sampleName, final int lane) {

    // Check undetermined indexed sample
    if (sampleName == null
        || sampleName.equals(SampleStatistics.UNDETERMINED_SAMPLE)) {
      return "fastqscreen.lane"
          + lane + ".undetermined.read1.mappedexceptgenomesample";
    }

    return "fastqscreen.lane"
        + lane + ".sample." + sampleName + ".read1." + sampleName
        + ".mappedexceptgenomesample";

  }

  @Override
  protected boolean isValuePercent() {
    return true;
  }

  @Override
  protected Class<?> getValueType() {
    return Double.class;
  }

  @SuppressWarnings("unused")
  protected Number transformValue(final Number value, final RunData data,
      final int read, final boolean indexedRead, final int lane) {

    return value.doubleValue() * 100.0;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public HitNoLibrariesFastqScreenSamplestatsTest() {
    super("samplestathitnolibrariessum", "",
        "fastqScreen mapped except genome sample", "%");
  }
}
