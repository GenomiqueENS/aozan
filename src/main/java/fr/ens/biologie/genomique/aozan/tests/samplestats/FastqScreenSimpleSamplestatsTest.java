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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.FastqScreenCollector;
import fr.ens.biologie.genomique.aozan.collectors.stats.SampleStatisticsCollector;
import fr.ens.biologie.genomique.aozan.fastqscreen.FastqScreenGenomes;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;

/**
 * The class add in the qc report html values from FastqScreen for each sample,
 * after compile replica data and for each reference genomes. The list of
 * references genomes contains default references genomes defined in aozan
 * configuration file. It add the genomes sample for the run included in
 * Bcl2fastq samplesheet file, only if it can be used for mapping with Bowtie.
 * The alias genomes file make the correspondence between the genome sample and
 * the reference genome used with bowtie, if it exists. The class retrieve the
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
        SampleStatisticsCollector.COLLECTOR_NAME);
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
  private String getSampleKey(final int sampleId) {

    final String value = this.isGenomeContamination
        ? ".mapped.percent" : ".one.hit.one.library.percent";

    return "fastqscreen.sample"
        + sampleId + ".read1." + this.genomeReference + value;
  }

  @Override
  public Class<?> getValueType() {
    return Double.class;
  }

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
      throws AozanException {

    if (conf == null || conf.isEmpty()) {
      throw new NullPointerException("The properties object is null or empty");
    }

    // Initialization fastqScreenGenomeMapper object
    final FastqScreenGenomes fqsm = FastqScreenGenomes.newInstance(conf);

    final Set<String> sampleGenomes = fqsm.getSampleGenomes();
    final Set<String> contaminantGenomes = fqsm.getContaminantGenomes();

    final List<AozanTest> list = new ArrayList<>();

    for (final String genome : sampleGenomes) {

      // Create an new AozanTest for each reference genome
      final FastqScreenSimpleSamplestatsTest testGenome =
          new FastqScreenSimpleSamplestatsTest(genome,
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
  public FastqScreenSimpleSamplestatsTest() {
    this(null, false);
  }

  /**
   * Public constructor, specific for a reference genome.
   * @param genome name of reference genome
   */
  public FastqScreenSimpleSamplestatsTest(final String genome,
      final boolean isGenomeContamination) {
    super("samplestats.samplestatsfsqmapped", "",
        "FastqScreen "
            + (isGenomeContamination ? "" : "single ") + "mapped on " + genome,
        "%");
    this.genomeReference = genome;
    this.isGenomeContamination = isGenomeContamination;
  }

}
