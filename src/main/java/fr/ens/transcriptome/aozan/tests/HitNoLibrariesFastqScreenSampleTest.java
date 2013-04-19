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

package fr.ens.transcriptome.aozan.tests;

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;

/**
 * The class add in the qc report html one result from fastqScreen for each
 * sample. It print the percent of reads which mapped on at least one genomes
 * except the genome sample.
 * @author Sandrine Perrin
 */
public class HitNoLibrariesFastqScreenSampleTest extends
    AbstractSimpleSampleTest {

  @Override
  public String[] getCollectorsNamesRequiered() {
    return new String[] {FastqScreenCollector.COLLECTOR_NAME};
  }

  @Override
  protected String getKey(int read, int readSample, int lane, String sampleName) {

    return "fastqscreen.lane"
        + lane + ".sample." + sampleName + ".read" + read + "." + sampleName
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

  protected Number transformValue(final Number value, final RunData data,
      final int read, final boolean indexedRead, final int lane) {

    return value.doubleValue() * 100.0;
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   */
  public HitNoLibrariesFastqScreenSampleTest() {
    super("hitnolibraries", "", "fastqScreen mapped except genome sample", "%");
  }
}
