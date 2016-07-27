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
import fr.ens.biologie.genomique.aozan.tests.TestResult;

/**
 * The class adds in the qc report html one result from fastqScreen for each
 * sample, after compile replicat data. It print the percent of reads which
 * mapped on at least one genomes except the genome sample.
 * @since 2.0
 * @author Sandrine Perrin
 */
public class HitNoLibrariesFastqScreenSamplestatsTest
    extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return ImmutableList.of(FastqScreenCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int pooledSampleId) {

    double value = 0;
    int sampleCount = 0;

    for (int sampleId : data.getSamplesInPooledSample(pooledSampleId)) {

      final String key = getSampleKey(sampleId);
      sampleCount++;

      if (data.contains(key)) {
        value += data.getDouble(key);
      }

      // Check value exist
      if (data.get(key) == null) {
        return new TestResult("NA");
      }
    }

    try {
      final double percent = value / (double) sampleCount;

      if (getInterval() == null)
        return new TestResult(percent);

      return new TestResult(getInterval().getScore(percent), percent, true);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  @Override
  public String getKey(final int pooledSampleId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the the key in the RunData object for the value to test.
   * @param sampleId the sample Id
   * @return a String with the required key
   */
  public String getSampleKey(final int sampleId) {
    return "fastqscreen.sample" + sampleId + ".read1.mappedexceptgenomesample";
  }

  @Override
  protected boolean isValuePercent() {
    return true;
  }

  @Override
  protected Class<?> getValueType() {
    return Double.class;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public HitNoLibrariesFastqScreenSamplestatsTest() {
    super("samplestathitnolibrariessum", "",
        "FastqScreen mapped except genome sample", "%");
  }
}
