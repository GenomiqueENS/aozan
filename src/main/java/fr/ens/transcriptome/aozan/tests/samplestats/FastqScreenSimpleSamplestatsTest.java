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

package fr.ens.transcriptome.aozan.tests.samplestats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;
import fr.ens.transcriptome.aozan.collectors.stats.SampleStatistics;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenGenomeMapper;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.TestResult;

/**
 * The class add in the qc report html values from FastqScreen for each sample,
 * after compile replica data and for each reference genomes. The list of
 * references genomes contains default references genomes defined in aozan
 * configuration file. It add the genomes sample for the run included in casava
 * design file, only if it can be used for mapping with bowtie. The alias
 * genomes file make the correspondence between the genome sample and the
 * reference genome used with bowtie, if it exists. The class retrieve the
 * percent of reads mapped on each reference genomes.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class FastqScreenSimpleSamplestatsTest extends AbstractSimpleSampleTest {

  private final String genomeReference;
  private final boolean isGenomeContamination;

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return ImmutableList.of(FastqScreenCollector.COLLECTOR_NAME,
        SampleStatistics.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final String sampleName) {

    // Compute error rate per lane
    final int laneCount = data.getLaneCount();

    double value = 0;

    for (int lane = 1; lane <= laneCount; lane++) {

      final String key = getKey(sampleName, lane);
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

    final String value =
        this.isGenomeContamination
            ? ".mapped.percent" : ".one.hit.one.library.percent";

    // Check undetermined indexed sample
    if (sampleName == null
        || sampleName.equals(SampleStatistics.UNDETERMINED_SAMPLE)) {
      return "fastqscreen.lane"
          + lane + ".undetermined.read1." + this.genomeReference + value;
    }
    return "fastqscreen.lane"
        + lane + ".sample." + sampleName + ".read1." + sampleName + "."
        + this.genomeReference + value;
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
      final int read, final int readSample, final int lane,
      final String sampleName) {

    final String keyGenomeSample =
        "design.lane" + lane + "." + sampleName + ".sample.ref";

    // Set genome sample
    final String genomeSample = data.get(keyGenomeSample);

    // Set reference genome corresponding of genome sample if it exists
    String genomeSampleReference = null;
    try {
      genomeSampleReference =
          FastqScreenGenomeMapper.getInstance()
              .getGenomeReferenceCorresponding(genomeSample);
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
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null || properties.isEmpty()) {
      throw new NullPointerException("The properties object is null or empty");
    }

    // Initialization fastqScreenGenomeMapper object
    final FastqScreenGenomeMapper fqsm =
        FastqScreenGenomeMapper.getInstance(properties);

    //
    final Set<String> genomes = fqsm.getGenomesToMapping();

    final List<AozanTest> list = new ArrayList<AozanTest>();

    for (final String genome : genomes) {

      // Create an new AozanTest for each reference genome
      final FastqScreenSimpleSamplestatsTest testGenome =
          new FastqScreenSimpleSamplestatsTest(genome,
              fqsm.isGenomeContamination(genome));

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
   * Get name of reference genome.
   * @return name of reference genome
   */
  public String getNameGenome() {
    return this.genomeReference;
  }

  private void internalConfigure(final Map<String, String> properties)
      throws AozanException {
    super.configure(properties);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public FastqScreenSimpleSamplestatsTest() {
    this(null, false);
  }

  /**
   * Public constructor, specific for a reference genome.
   * @param genome name of reference genome
   */
  public FastqScreenSimpleSamplestatsTest(final String genome,
      final boolean isGenomeContamination) {
    super("samplestatsfsqmapped", "", "fastqscreen "
        + (isGenomeContamination ? "" : "single ") + "mapped on " + genome, "%");
    this.genomeReference = genome;
    this.isGenomeContamination = isGenomeContamination;
  }

}
