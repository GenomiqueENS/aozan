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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;

public class FastqScreenSimpleSampleTest extends AbstractSimpleSampleTest {

  private static final String KEY_ALIAS_GENOME_PATH =
      "qc.conf.genome.alias.path";

  // map which does correspondence between genome of sample and reference genome
  private static final Map<String, String> aliasGenome =
      new HashMap<String, String>();
  private static String aliasGenomePath;

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
   * @param value value to transform
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

    // add genome of the sample if it doesn't in reference file
    if (!aliasGenome.containsKey(genomeSample)) {

      aliasGenome.put(genomeSample, "");
      updateAliasGenomeFile(genomeSample);
    }

    // reverse the score if genome of sample is the same that reference genome
    if (this.genomeReference.equals(aliasGenome.get(genomeSample)))
      return (9 - score);
    else
      return score;
  }

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    // Retrieve list of reference genomes from FastqScreen class contains
    // genomes defined in aozan.conf file and the genomes of the samples.
    List<String> genomes = FastqScreen.getListGenomeReferenceSample();

    // retrieve the genome of sample
    aliasGenomePath = properties.get(KEY_ALIAS_GENOME_PATH);
    createMapAliasGenome();

    List<AozanTest> list = new ArrayList<AozanTest>();

    for (String genome : genomes) {

      final FastqScreenSimpleSampleTest testGenome =
          new FastqScreenSimpleSampleTest(genome);

      testGenome.internalConfigure(properties);

      list.add(testGenome);
    }

    return list;
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

  private void internalConfigure(final Map<String, String> properties)
      throws AozanException {

    aliasGenomePath = properties.get(KEY_ALIAS_GENOME_PATH);
    super.configure(properties);
  }

  /**
   * Create a map which does correspondance between genome of sample and
   * reference genome from a file
   */
  private void createMapAliasGenome() {
    try {

      if (aliasGenomePath != null) {

        final BufferedReader br =
            new BufferedReader(new FileReader(new File(aliasGenomePath)));
        String line = null;

        while ((line = br.readLine()) != null) {

          final int pos = line.indexOf('=');
          if (pos == -1)
            continue;

          final String key = line.substring(0, pos);
          final String value = line.substring(pos + 1);

          aliasGenome.put(key, value);
        }
        br.close();
      }
    } catch (IOException io) {
    }
  }

  /**
   * Add the genome of the sample in the file which does correspondance with
   * reference genome
   * @param genomeSample name genome
   */
  private void updateAliasGenomeFile(final String genomeSample) {

    try {
      if (aliasGenomePath != null) {

        final FileWriter fw = new FileWriter(aliasGenomePath, true);

        fw.write(genomeSample + "=\n");
        fw.close();
      }
    } catch (IOException io) {
      System.out.println(io.getMessage());
    }
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
  public FastqScreenSimpleSampleTest(String genome) {

    super("fsqmapped", "", "fastqscreen mapped on " + genome, "%");
    this.genomeReference = genome;
  }

}
