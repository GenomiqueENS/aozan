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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;

public class FastqScreenSimpleSampleTest extends AbstractSimpleSampleTest {

  private static final String TITLE_COLUMN_GENOME_CASAVA_FILE = "\"SampleRef\"";
  private static final String KEY_GENOMES = "qc.conf.fastqscreen.genomes";

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
    String genomeSampleReference =
        FastqScreenCollector.AliasGenomeFile
            .getGenomeReferenceCorresponding(genomeSample);

    if (this.genomeReference.equals(genomeSampleReference))
      return (9 - score);
    else
      return score;
  }

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    String genomesPerDefault = properties.get(KEY_GENOMES);

    if (genomesPerDefault == null || genomesPerDefault.length() == 0)
      throw new AozanException(
          "AozanTest FastqScreen : none default genome reference for tests define");

    // Create an new AozanTest for each reference genome
    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();
    Set<String> genomes = Sets.newLinkedHashSet(s.split(genomesPerDefault));

    // Set list of genome from samples to use in fastqscreen
    Set<String> genomesSamples = setGenomesNameReferenceSample(properties);
    genomes.addAll(genomesSamples);

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
    super.configure(properties);
  }

  /**
   * Set list of genomes names reference for a run. Retrieve list of genomes
   * sample from casava design file and filtered them from alias genome file in
   * FastqScreenCollector.
   * @param properties of configuration
   * @return list genomes references used in FastqScreenCollector
   */
  private static Set<String> setGenomesNameReferenceSample(
      final Map<String, String> properties) {

    final String casavaDesignPath = properties.get(QC.CASAVA_DESIGN_PATH);

    Set<String> genomesFromCasavaDesign = Sets.newHashSet();

    BufferedReader br = null;
    boolean firstLane = true;
    String line = null;

    try {
      br = new BufferedReader(new FileReader(new File(casavaDesignPath)));

      while ((line = br.readLine()) != null) {

        String genome = line.split(",")[3];

        // Check first lane
        if (firstLane) {
          if (genome.equals(TITLE_COLUMN_GENOME_CASAVA_FILE)) {
            firstLane = false;
          } else {
            br.close();
            // Column not found, return empty list
            return genomesFromCasavaDesign;
          }

        } else {

          genome = genome.replaceAll("\"", "").trim().toLowerCase();

          genomesFromCasavaDesign.add(genome);
        }
      }

      br.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Retrieve list of corresponding genome reference from casava design file
    return FastqScreenCollector.AliasGenomeFile
        .convertListToGenomeReferenceName(properties, genomesFromCasavaDesign);
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
