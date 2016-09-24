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

package fr.ens.biologie.genomique.aozan.collectors;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.fastqscreen.FastqScreen;
import fr.ens.biologie.genomique.aozan.fastqscreen.GenomeAliases;
import fr.ens.biologie.genomique.aozan.io.FastqSample;

/**
 * This class manages the execution of Fastq Screen for a full run according to
 * the properties defined in the configuration file Aozan, which define the list
 * of references genomes. Each sample are mapped on list of references genomes
 * and the genome of sample if it is available for Aozan.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenCollector extends AbstractFastqCollector {

  /** Collector name. */
  public static final String COLLECTOR_NAME = "fastqscreen";

  private FastqScreen fastqscreen;

  private boolean skipControlLane;
  private boolean ignorePairedMode;
  private File fastqscreenXSLFile;
  private boolean isProcessUndeterminedIndicesSamples;

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collectors to execute before fastqscreen Collector.
   * @return list of names collector
   */
  @Override
  public List<String> getCollectorsNamesRequiered() {

    final List<String> result = super.getCollectorsNamesRequiered();
    result.add(SubsetFastqCollector.COLLECTOR_NAME);

    return Collections.unmodifiableList(result);

  }

  @Override
  public void configure(final QC qc, final CollectorConfiguration conf) {

    super.configure(qc, conf);

    this.fastqscreen = new FastqScreen(conf);

    try {
      this.ignorePairedMode = conf.getBoolean(
          Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_END_MODE_KEY);

    } catch (final Exception e) {
      // Default value
      this.ignorePairedMode = false;
    }

    try {
      this.skipControlLane = conf.getBoolean(
          Settings.QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY);
    } catch (final Exception e) {
      // Default value
      this.skipControlLane = true;
    }

    try {
      final File file = conf.getFile(Settings.QC_CONF_FASTQSCREEN_XSL_FILE_KEY);
      if (file != null && file.exists()) {
        this.fastqscreenXSLFile = file;
      }
    } catch (final Exception e) {
      // Call default xsl file
      this.fastqscreenXSLFile = null;
    }

    // Check if process undetermined indices samples specify in Aozan
    // configuration
    this.isProcessUndeterminedIndicesSamples = conf.getBoolean(
        Settings.QC_CONF_FASTQSCREEN_PROCESS_UNDETERMINED_SAMPLES_KEY);
  }

  @Override
  public AbstractFastqProcessThread collectSample(final RunData data,
      final FastqSample fastqSample, final File reportDir,
      final boolean isRunPE) throws AozanException {

    if (fastqSample.getFastqFiles() == null
        || fastqSample.getFastqFiles().isEmpty()) {

      throw new AozanException("No fastq files defined for fastqSample "
          + fastqSample.getKeyFastqSample());
    }

    // Create the thread object only if the fastq sample correspond to a R1
    if (fastqSample.getRead() != 1) {
      return null;
    }

    final boolean controlLane = data.isLaneControl(fastqSample.getSampleId());

    // Skip the control lane
    if (controlLane && this.skipControlLane) {
      return null;
    }

    if (fastqSample.isUndeterminedIndex()) {
      return createInterminedIndicesFastqScreenThread(data, fastqSample,
          reportDir, isRunPE);
    }

    return createStandardFastqScreenThread(data, fastqSample, reportDir,
        isRunPE);
  }

  /**
   * Collect data for a fastqSample for standard sample.
   * @param data result data object
   * @param fastqSample sample object
   * @param reportDir report directory
   * @param isRunPE true if it is a run PE else false
   * @return process thread instance
   * @throws AozanException if an error occurs while execution
   */
  private AbstractFastqProcessThread createStandardFastqScreenThread(
      final RunData data, final FastqSample fastqSample, final File reportDir,
      final boolean isRunPE) throws AozanException {

    final Set<String> contaminantGenomes =
        this.fastqscreen.getFastqScreenGenomes().getContaminantGenomes();

    final Set<String> genomes = new HashSet<>(contaminantGenomes);

    // Set mode for FastqScreen
    final boolean isPairedMode = isRunPE && !this.ignorePairedMode;

    // Retrieve genome sample from run data
    final String sampleRef = data.getSampleGenome(fastqSample.getSampleId());

    final String sampleGenomeName;
    if (sampleRef != null) {

      // Get corresponding valid genome name for mapping
      sampleGenomeName = GenomeAliases.getInstance().get(sampleRef);

      Common.getLogger()
          .info("FASTQSCREEN: Genome ref of sample \""
              + fastqSample.getSampleName() + "\" is \"" + sampleRef
              + "\". search related name in genome available on mapping "
              + (sampleGenomeName == null ? "NoFOUND" : sampleGenomeName));

      // Genome can be use for mapping
      if (sampleGenomeName != null) {
        genomes.add(sampleGenomeName);
      }
    } else {
      sampleGenomeName = null;
    }

    // In mode paired FastqScreen should be launched with R1 and R2
    // together.
    if (isPairedMode) {
      // Search fasqtSample which corresponding to fastqSample R1
      final String prefixRead2 = fastqSample.getPrefixRead2();

      // Search FastSample instance corresponding to read2 for the sample
      for (final FastqSample fastqSampleR2 : getFastqSamples()) {
        if (fastqSampleR2.getKeyFastqSample().equals(prefixRead2)) {

          return new FastqScreenProcessThread(fastqSample, fastqSampleR2,
              this.fastqscreen, data, genomes, sampleGenomeName, reportDir,
              isPairedMode, isRunPE, this.fastqscreenXSLFile);
        }
      }
    }

    // Call with a mode single-end for mapping
    return new FastqScreenProcessThread(fastqSample, this.fastqscreen, data,
        genomes, sampleGenomeName, reportDir, isPairedMode, isRunPE,
        this.fastqscreenXSLFile);
  }

  /**
   * Collect data for a fastqSample for indetermined indices sample.
   * @param data result data object
   * @param fastqSample sample object
   * @param isRunPE true if the run is PE else false
   * @return process thread instance
   * @throws AozanException if an error occurs while execution
   */
  private AbstractFastqProcessThread createInterminedIndicesFastqScreenThread(
      final RunData data, final FastqSample fastqSample, final File reportDir,
      final boolean isRunPE) throws AozanException {

    // Retrieve all genomes used by mapping for Undetermined Indexed sample
    final Set<String> genomesToSampleTest =
        this.fastqscreen.getFastqScreenGenomes().getSampleGenomes();

    return new FastqScreenProcessThread(fastqSample, this.fastqscreen, data,
        genomesToSampleTest, null, reportDir, false, isRunPE,
        this.fastqscreenXSLFile);
  }

  //
  // Getters & Setters
  //

  /**
   * Get number of thread.
   * @return number of thread
   */
  @Override
  protected int getThreadsNumber() {
    return 1;
  }

  @Override
  protected boolean isProcessUndeterminedIndicesSamples() {
    return this.isProcessUndeterminedIndicesSamples;
  }

}
