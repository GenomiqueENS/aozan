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

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.AliasGenomeFile;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * This class manages the execution of Fastq Screen for a full run according to
 * the properties defined in the configuration file Aozan, which define the list
 * of references genomes. Each sample are mapped on list of references genomes
 * and the genome of sample if it is available for Aozan.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenCollector extends AbstractFastqCollector {

  public static final String COLLECTOR_NAME = "fastqscreen";
  private static final String KEY_GENOMES = "qc.conf.fastqscreen.genomes";
  public static final String KEY_SKIP_CONTROL_LANE =
      "qc.conf.skip.control.lane";
  public static final String KEY_IGNORE_PAIRED_MODE =
      "qc.conf.ignore.paired.mode";

  private FastqScreen fastqscreen;

  private boolean skipControlLane;
  private boolean ignorePairedMode;

  // List of genome for fastqscreen specific of a sample
  private List<String> genomesConfiguration = new ArrayList<String>();

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collectors to execute before fastqscreen Collector
   * @return list of names collector
   */
  @Override
  public List<String> getCollectorsNamesRequiered() {

    List<String> result = super.getCollectorsNamesRequiered();
    result.add(TemporaryPartialFastqCollector.COLLECTOR_NAME);

    return Collections.unmodifiableList(result);

  }

  /**
   * Configure fastqScreen with properties from file aozan.conf.
   * @param properties
   */
  @Override
  public void configure(final Properties properties) {

    super.configure(properties);

    this.fastqscreen = new FastqScreen(properties);

    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();

    // Set list of reference genome for fastqscreen
    for (String g : s.split(properties.getProperty(KEY_GENOMES))) {
      genomesConfiguration.add(g);
    }

    try {
      this.ignorePairedMode =
          Boolean.parseBoolean(properties.getProperty(KEY_IGNORE_PAIRED_MODE));

    } catch (Exception e) {
      // Default value
      this.ignorePairedMode = false;
    }

    try {
      this.skipControlLane =
          Boolean.parseBoolean(properties.getProperty(KEY_SKIP_CONTROL_LANE));
    } catch (Exception e) {
      // Default value
      this.skipControlLane = true;
    }
  }

  @Override
  public AbstractFastqProcessThread collectSample(RunData data,
      final FastqSample fastqSample, final File reportDir, final boolean isRunPE)
      throws AozanException {

    if (fastqSample.getFastqFiles() == null
        || fastqSample.getFastqFiles().isEmpty()) {

      throw new AozanException("No fastq files defined for fastqSample "
          + fastqSample.getKeyFastqSample());
    }

    // Create the thread object only if the fastq sample correspond to a R1
    if (fastqSample.getRead() == 2)
      return null;

    // Retrieve genome sample
    final String genomeSample =
        data.get("design.lane"
            + fastqSample.getLane() + "." + fastqSample.getSampleName()
            + ".sample.ref");

    final String genomeReferenceSample =
        AliasGenomeFile.getInstance().getGenomeReferenceCorresponding(
            genomeSample);

    final boolean controlLane =
        data.getBoolean("design.lane"
            + fastqSample.getLane() + "." + fastqSample.getSampleName()
            + ".control");

    // Skip the control lane
    if (controlLane && skipControlLane)
      return null;

    final boolean isPairedMode = isRunPE && !ignorePairedMode;

    if (isRunPE && isPairedMode) {

      // in mode paired FastqScreen should be launched with R1 and R2
      // together.
      // Search fasqtSample which corresponding to fastqSample R1
      String prefixRead2 = fastqSample.getPrefixRead2();

      for (FastqSample fastqSampleR2 : fastqSamples) {
        if (fastqSampleR2.getKeyFastqSample().equals(prefixRead2)) {

          return new FastqScreenProcessThread(fastqSample, fastqSampleR2,
              fastqscreen, data, genomesConfiguration, genomeReferenceSample,
              reportDir, isPairedMode, isRunPE);
        }
      }
    }

    // Call with a mode single-end for mapping
    return new FastqScreenProcessThread(fastqSample, fastqscreen, data,
        genomesConfiguration, genomeReferenceSample, reportDir, isPairedMode,
        isRunPE);
  }

  //
  // Getters & Setters
  //

  /**
   * Get number of thread
   * @return number of thread
   */
  @Override
  protected int getThreadsNumber() {
    return 1;
  }

}
