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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.FastqScreenCollector;
import fr.ens.biologie.genomique.aozan.fastqscreen.FastqScreenGenomes;
import fr.ens.biologie.genomique.aozan.fastqscreen.GenomeAliases;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;

/**
 * The class add in the qc report html values from FastqScreen for each sample
 * and for each reference genomes. The list of references genomes contains
 * default references genomes defined in aozan configuration file. It add the
 * genomes sample for the run included in Bcl2fastq samplesheet file, only if it
 * can be used for mapping with bowtie. The alias genomes file make the
 * correspondence between the genome sample and the reference genome used with
 * bowtie, if it exists. The class retrieve the percent of reads mapped on each
 * reference genomes.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenSimpleSampleTest extends AbstractSimpleSampleTest {

  private final String genomeReference;
  private final boolean isGenomeContamination;

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return ImmutableList.of(FastqScreenCollector.COLLECTOR_NAME);
  }

  @Override
  public String getKey(final int read, final int readSample, final int sampleId,
      final int lane, final boolean undetermined) {

    final String value = this.isGenomeContamination
        ? ".mapped.percent" : ".one.hit.one.library.percent";

    // Check undetermined indexed sample
    if (undetermined) {
      return "fastqscreen.sample"
          + sampleId + ".read1." + this.genomeReference + value;
    }

    return "fastqscreen.sample"
        + sampleId + ".read" + readSample + "." + this.genomeReference + value;
  }

  @Override
  public Class<?> getValueType() {
    return Double.class;
  }

  /**
   * Transform the score : if genome of sample is the same as reference genome.
   * then the score is reverse for change the color in QC report
   * @param data run data
   * @param read index of read
   * @param readSample index of read without indexed reads
   * @param lane lane index
   * @param sampleName sample name
   * @return the transformed score
   */
  @Override
  protected int transformScore(final int score, final RunData data,
      final int read, final int readSample, final int sampleId) {

    // Set genome sample
    final String genomeSample = data.getSampleGenome(sampleId);

    // Set reference genome corresponding of genome sample if it exists
    String genomeSampleReference = null;
    try {
      genomeSampleReference = GenomeAliases.getInstance().get(genomeSample);
    } catch (final AozanException e) {
    }

    // If genome sample are used like reference genome in FastqScreen, the score
    // are inverse. The value must be near 100% if it had no contamination.
    if (this.genomeReference.equals(genomeSampleReference)) {
      return (9 - score);
    }

    return score;
  }

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
      throws AozanException {

    if (conf == null || conf.isEmpty()) {
      throw new NullPointerException("The conf object is null or empty");
    }

    // Initialization fastqScreenGenomeMapper object
    final FastqScreenGenomes fqsm = FastqScreenGenomes.newInstance(conf);

    final Set<String> sampleGenomes = fqsm.getSampleGenomes();
    final Set<String> contaminantGenomes = fqsm.getContaminantGenomes();

    final List<AozanTest> list = new ArrayList<>();

    for (final String genome : sampleGenomes) {

      // Create an new AozanTest for each reference genome
      final FastqScreenSimpleSampleTest testGenome =
          new FastqScreenSimpleSampleTest(genome,
              contaminantGenomes.contains(genome));

      testGenome.internalConfigure(conf);

      list.add(testGenome);
    }

    return list;
  }

  @Override
  public boolean isValuePercent() {
    return true;
  }

  /**
   * Get name of reference genome.
   * @return name of reference genome
   */
  public String getNameGenome() {
    return this.genomeReference;
  }

  private void internalConfigure(final TestConfiguration conf)
      throws AozanException {
    super.configure(conf);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public FastqScreenSimpleSampleTest() {
    this(null, false);
  }

  /**
   * Public constructor, specific for a reference genome.
   * @param genome name of reference genome
   */
  public FastqScreenSimpleSampleTest(final String genome,
      final boolean isGenomeContamination) {
    super("fsqmapped", "",
        "FastqScreen "
            + (isGenomeContamination ? "" : "single ") + "mapped on " + genome,
        "%");
    this.genomeReference = genome;
    this.isGenomeContamination = isGenomeContamination;
  }

}
