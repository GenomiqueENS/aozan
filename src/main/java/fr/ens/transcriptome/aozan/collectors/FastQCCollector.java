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
import java.util.Properties;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.fastqc.OverrepresentedSequencesBlast;
import fr.ens.transcriptome.aozan.fastqc.RuntimePatchFastQC;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * This class define a FastQC Collector
 * @since 0.8
 * @author Laurent Jourdren
 */
public class FastQCCollector extends AbstractFastqCollector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "fastqc";

  /** Retrieve parameters of FastQC qc.conf.+ key_fastqc */

  private int numberThreads = Runtime.getRuntime().availableProcessors();

  private final boolean ignoreFilteredSequences = false;
  private boolean isStepBlastEnabled = false;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public void configure(final Properties properties) {

    super.configure(properties);

    // Define parameters of FastQC
    System.setProperty("java.awt.headless", "true");
    System.setProperty("fastqc.unzip", "true");

    // Check contaminant file specify in configuration Aozan for module
    // OverRepresented
    if (properties.getProperty(Settings.QC_CONF_FASTQC_CONTAMINANT_FILE_KEY) != null
        && properties.getProperty(Settings.QC_CONF_FASTQC_CONTAMINANT_FILE_KEY)
            .length() > 0) {

      System.setProperty("fastqc.contaminant_file",
          properties.getProperty(Settings.QC_CONF_FASTQC_CONTAMINANT_FILE_KEY));
    }

    if (properties.getProperty(Settings.QC_CONF_FASTQC_KMER_SIZE_KEY) != null
        && properties.getProperty(Settings.QC_CONF_FASTQC_KMER_SIZE_KEY)
            .length() > 0)

      System.setProperty("fastqc.kmer_size",
          properties.getProperty(Settings.QC_CONF_FASTQC_KMER_SIZE_KEY));

    if (properties.getProperty(Settings.QC_CONF_FASTQC_NOGROUP_KEY) != null
        && properties.getProperty(Settings.QC_CONF_FASTQC_NOGROUP_KEY).length() > 0)

      System.setProperty("fastqc.nogroup",
          properties.getProperty(Settings.QC_CONF_FASTQC_NOGROUP_KEY));

    // Set the number of threads
    if (properties.containsKey(Settings.QC_CONF_THREADS_KEY)) {

      try {
        int confThreads =
            Integer.parseInt(properties.getProperty(
                Settings.QC_CONF_THREADS_KEY).trim());
        if (confThreads > 0)
          this.numberThreads = confThreads;

      } catch (NumberFormatException ignored) {
      }
    }

    // Check if step blast needed and configure
    OverrepresentedSequencesBlast.getInstance().configure(properties);
    this.isStepBlastEnabled =
        OverrepresentedSequencesBlast.getInstance().isStepBlastEnabled();

  }

  @Override
  public void collect(final RunData data) throws AozanException {

    // Rewriting code of the method ContaminantFinder for read the contaminant
    // list in fastqc-0.10.1 jar
    RuntimePatchFastQC.runPatchFastQC(this.isStepBlastEnabled);

    super.collect(data);
  }

  @Override
  public AbstractFastqProcessThread collectSample(final RunData data,
      final FastqSample fastqSample, final File reportDir, final boolean runPE)
      throws AozanException {

    if (fastqSample.getFastqFiles().isEmpty()) {
      return null;
    }

    // Create the thread object
    return new FastQCProcessThread(fastqSample, this.ignoreFilteredSequences,
        reportDir);
  }

  //
  // Getters & Setters
  //

  @Override
  protected int getThreadsNumber() {
    return numberThreads;
  }

}
