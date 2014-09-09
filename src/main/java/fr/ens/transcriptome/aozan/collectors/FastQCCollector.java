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
import java.util.List;
import java.util.Map;
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

  public final static void initFastQC(Map<String, String> globalConf) {

    // Define parameters of FastQC
    System.setProperty("java.awt.headless", "true");
    System.setProperty("fastqc.unzip", "true");

    // Contaminant file
    final String contaminantFile =
        globalConf.get(Settings.QC_CONF_FASTQC_CONTAMINANT_FILE_KEY);

    if (contaminantFile != null && contaminantFile.trim().length() > 0) {
      System.setProperty("fastqc.contaminant_file", contaminantFile);
    }

    // Adapter file
    final String adapterFile =
        globalConf.get(Settings.QC_CONF_FASTQC_ADAPTER_FILE_KEY);

    if (adapterFile != null && adapterFile.trim().length() > 0) {
      System.setProperty("fastqc.adapter_file", adapterFile);
    }

    // Limits file
    final String limitsFile =
        globalConf.get(Settings.QC_CONF_FASTQC_LIMITS_FILE_KEY);

    if (limitsFile != null && limitsFile.trim().length() > 0) {
      System.setProperty("fastqc.limits_file", limitsFile);
    }

    // Kmer Size
    final String kmerSize = globalConf.get(Settings.QC_CONF_FASTQC_KMER_SIZE_KEY);
    if (kmerSize != null && kmerSize.trim().length() > 0) {
      System.setProperty("fastqc.kmer_size", kmerSize);
    }

    // Set fastQC nogroup
    final String fastqcNoGroup = globalConf.get(Settings.QC_CONF_FASTQC_NOGROUP_KEY);
    if (fastqcNoGroup != null && fastqcNoGroup.trim().length() > 0) {
      System.setProperty("fastqc.nogroup", fastqcNoGroup);
    }

    // Set fastQC expgroup
    final String fastqcExpgroup = globalConf.get(Settings.QC_CONF_FASTQC_EXPGROUP_KEY);
    if (fastqcExpgroup != null && fastqcExpgroup.trim().length() > 0) {
      System.setProperty("fastqc.expgroup", fastqcExpgroup);
    }

    // Set the number of threads of FastQC at one
    System.setProperty("fastqc.threads", "1");

  }

  @Override
  public void configure(final Properties properties) {

    super.configure(properties);

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

    final List<File> fastqFiles = fastqSample.getFastqFiles();

    if (fastqFiles.isEmpty()) {
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
