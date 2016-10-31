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

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.fastqc.OverrepresentedSequencesBlast;
import fr.ens.biologie.genomique.aozan.io.FastqSample;

/**
 * This class define a FastQC Collector.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class FastQCCollector extends AbstractFastqCollector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "fastqc";

  /** Retrieve parameters of FastQC qc.conf.+ key_fastqc. */
  private static final boolean INGORE_FILTERED_SEQUENCES = false;

  private int numberThreads = Runtime.getRuntime().availableProcessors();
  private boolean isProcessUndeterminedIndicesSamples = false;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public void configure(final QC qc, final CollectorConfiguration conf) {

    super.configure(qc, conf);

    // Set the number of threads
    if (conf.containsKey(Settings.QC_CONF_THREADS_KEY)) {

      try {
        final int confThreads = conf.getInt(Settings.QC_CONF_THREADS_KEY, -1);

        if (confThreads > 0) {
          this.numberThreads = confThreads;
        }
      } catch (final NumberFormatException ignored) {
      }
    }

    // Check if process undetermined indices samples specify in Aozan
    // configuration
    this.isProcessUndeterminedIndicesSamples = conf
        .getBoolean(Settings.QC_CONF_FASTQC_PROCESS_UNDETERMINED_SAMPLES_KEY);

    // Check if step blast needed and configure
    OverrepresentedSequencesBlast.getInstance().configure(conf,
        qc.getSettings().get(Settings.DOCKER_URI_KEY));

  }

  @Override
  public AbstractFastqProcessThread collectSample(final RunData data,
      final FastqSample fastqSample, final File reportDir, final boolean runPE)
      throws AozanException {

    Common.getLogger().info("Process sample for FastQC: " + fastqSample.toString());

    if (fastqSample.getFastqFiles() == null
        || fastqSample.getFastqFiles().isEmpty()) {

      throw new AozanException("No FASTQ file defined for the fastqSample: "
          + fastqSample.getKeyFastqSample());
    }

    // Create the thread object
    return new FastQCProcessThread(fastqSample, INGORE_FILTERED_SEQUENCES,
        reportDir);
  }

  //
  // Getters & Setters
  //

  @Override
  protected int getThreadsNumber() {
    return this.numberThreads;
  }

  @Override
  protected boolean isProcessUndeterminedIndicesSamples() {
    return this.isProcessUndeterminedIndicesSamples;
  }
}
