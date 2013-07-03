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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;
import fr.ens.transcriptome.aozan.fastqscreen.AliasGenomeFile;
import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.illumina.io.CasavaDesignCSVReader;

/**
 * The class add in the qc report html values from FastqScreen for each sample
 * and for each reference genomes. The list of references genomes contains
 * default references genomes defined in aozan configuration file. It add the
 * genomes sample for the run included in casava design file, only if it can be
 * used for mapping with bowtie. The alias genomes file make the correspondence
 * between the genome sample and the reference genome used with bowtie, if it
 * exists. The class retrieve the percent of reads mapped on each reference
 * genomes.
 * @since 1.0
 * @author Sandrine Perrin
 */
/**
 * @author sperrin
 */
public class FastqScreenSimpleSampleTest extends AbstractSimpleSampleTest {

  private static final String KEY_GENOMES = "qc.conf.fastqscreen.genomes";
  // Key for retrieve the path of alias file
  private final String KEY_ALIAS_GENOME_PATH = "qc.conf.genome.alias.path";

  private String genomeReference;
  private boolean isGenomeContamination;

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return ImmutableList.of(FastqScreenCollector.COLLECTOR_NAME);
  }

  @Override
  public String getKey(int read, int readSample, int lane, String sampleName) {

    String value =
        isGenomeContamination
            ? ".mapped.percent" : ".one.hit.one.library.percent";

    return "fastqscreen.lane"
        + lane + ".sample." + sampleName + ".read" + readSample + "."
        + sampleName + "." + genomeReference + value;
  }

  @Override
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

    // Set genome sample
    String genomeSample = data.get(keyGenomeSample);

    // Set reference genome corresponding of genome sample if it exists
    String genomeSampleReference =
        AliasGenomeFile.getInstance().getGenomeReferenceCorresponding(
            genomeSample);

    // If genome sample are used like reference genome in FastqScreen, the score
    // are inverse. The value must be near 100% if it had no contamination.
    if (this.genomeReference.equals(genomeSampleReference))
      return (9 - score);

    return score;
  }

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    // Set reference genomes defined in configuration aozan file
    String genomesPerDefault = properties.get(KEY_GENOMES);

    if (genomesPerDefault == null || genomesPerDefault.length() == 0)
      throw new AozanException(
          "AozanTest FastqScreen : none default genome reference for tests define");

    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();
    Set<String> genomes = Sets.newLinkedHashSet(s.split(genomesPerDefault));

    // Set list of genome from samples to use in fastqscreen
    Set<String> genomesSamples =
        setGenomesNameReferenceSample(properties.get(QC.CASAVA_DESIGN_PATH),
            properties.get(KEY_ALIAS_GENOME_PATH));

    // Set a global list of the run
    genomes.addAll(genomesSamples);

    List<AozanTest> list = new ArrayList<AozanTest>();

    for (String genome : genomes) {

      // Create an new AozanTest for each reference genome
      final FastqScreenSimpleSampleTest testGenome =
          new FastqScreenSimpleSampleTest(genome,
              genomesPerDefault.contains(genome));

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
   * Set reference genomes for the samples of a run. Retrieve list of genomes
   * sample from casava design file and filtered them compared to alias genome
   * file.
   * @param casavaDesignPath absolute path of the casava design file
   * @param genomeAliasFile absolute path of the alias genomes file
   * @return list genomes references used in FastqScreenCollector
   */
  private Set<String> setGenomesNameReferenceSample(
      final String casavaDesignPath, final String genomeAliasFile) {

    Set<String> genomesFromCasavaDesign = Sets.newHashSet();

    CasavaDesignCSVReader casavaReader;
    CasavaDesign casavaDesign;
    try {
      // Reading casava design file in format csv
      casavaReader = new CasavaDesignCSVReader(casavaDesignPath);
      casavaDesign = casavaReader.read();

    } catch (Exception e) {
      // Return empty list
      return genomesFromCasavaDesign;
    }

    // Retrieve all genome sample included in casava design file
    for (CasavaSample casavaSample : casavaDesign) {
      String genomeSample =
          casavaSample.getSampleRef().replaceAll("\"", "").trim().toLowerCase();
      genomesFromCasavaDesign.add(genomeSample);
    }

    // Retrieve list of corresponding reference genome from casava design file
    return AliasGenomeFile.getInstance().convertListToGenomeReferenceName(
        genomeAliasFile, genomesFromCasavaDesign);
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   */
  public FastqScreenSimpleSampleTest() {
    this(null, false);
  }

  /**
   * Public constructor, specific for a reference genome
   * @param genome name of reference genome
   */
  public FastqScreenSimpleSampleTest(String genome,
      boolean isGenomeContamination) {
    super("fsqmapped", "", "fastqscreen "
        + (isGenomeContamination ? "" : "uniq ") + "mapped on " + genome, "%");
    this.genomeReference = genome;
    this.isGenomeContamination = isGenomeContamination;
  }

}
