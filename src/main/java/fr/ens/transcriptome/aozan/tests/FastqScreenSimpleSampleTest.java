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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;

public class FastqScreenSimpleSampleTest extends AbstractSimpleSampleTest {

  // map which does correspondence between genome of sample and reference genome
  private static Map<String, String> aliasGenomes =
      new HashMap<String, String>();

  private String genomeReference;

  @Override
  public String[] getCollectorsNamesRequiered() {
    return new String[] {FastqScreenCollector.COLLECTOR_NAME};
  }

  @Override
  public String getKey(int read, int readSample, int lane, String sampleName) {

    return "fastqscreen.lane"
        + lane + ".sample." + sampleName + ".read" + readSample + "."
        + sampleName + "." + genomeReference + ".mapped.percent";
  }

  public Class<?> getValueType() {
    return Double.class;
  }

  /**
   * Transform the score : if genome of sample is the same as reference genome
   * then the score is reverse for change the color in QC report
   * @param score value to transform
   * @param data run data
   * @param read index of read
   * @param readSample index of read without indexed reads
   * @param lane lane index
   * @param sampleName sample name
   * @return the transformed score
   */
  @Override
  protected int transformScore(final int score, final RunData data,
      final int read, int readSample, final int lane, final String sampleName) {

    String keyGenomeSample =
        "design.lane" + lane + "." + sampleName + ".sample.ref";
    String genomeSample = data.get(keyGenomeSample);

    genomeSample = genomeSample.trim().toLowerCase();
    genomeSample = genomeSample.replace('"', '\0');

    // reverse the score if genome of sample is the same that reference genome
    if (this.genomeReference.equals(aliasGenomes.get(genomeSample)))
      return (9 - score);
    else
      return score;

  }

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    // Retrieve list of reference genomes from FastqScreenCollector class
    // contains genomes defined in aozan.conf file and all genomes from samples.
    List<String> genomes = FastqScreenCollector.getGenomesReferenceSample();
    aliasGenomes = FastqScreenCollector.getAliasGenomes();

    List<AozanTest> list = new ArrayList<AozanTest>();

    for (String genome : genomes) {

      // for each genome reference in fastqscreen, create a specific Aozantest
      final FastqScreenSimpleSampleTest testGenome =
          new FastqScreenSimpleSampleTest(genome);

      testGenome.internalConfigure(properties);

      list.add(testGenome);
    }

    return list;
  }

  private void internalConfigure(final Map<String, String> properties)
      throws AozanException {

    super.configure(properties);
  }

  @Override
  public boolean isValuePercent() {
    return true;
  }

  /**
   * Get name of reference genome
   * @return name of reference genome
   */
  public String getNameGenome() {
    return this.genomeReference;
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   */
  public FastqScreenSimpleSampleTest() {
    this(null);
  }

  /**
   * Public constructor, specific for a reference genome
   * @param genome name of reference genome
   */
  private FastqScreenSimpleSampleTest(String genome) {

    super("fsqmapped", "", "fastqscreen mapped on " + genome, "%");
    this.genomeReference = genome;
  }

}
