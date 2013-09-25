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
import java.util.Properties;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
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
  public static final String KEY_FASTQC_CONTAMINANT_FILE =
      "qc.conf.fastqc.contaminant.file";
  public static final String KEY_FASTQC_KMER_SIZE = "qc.conf.fastqc.kmer.size";
  public static final String KEY_FASTQC_NOGROUP = "qc.conf.fastqc.nogroup";

  private int numberThreads = Runtime.getRuntime().availableProcessors();

  private boolean ignoreFilteredSequences = false;

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
    if (properties.getProperty(KEY_FASTQC_CONTAMINANT_FILE) != null
        && properties.getProperty(KEY_FASTQC_CONTAMINANT_FILE).length() > 0) {

      System.setProperty("fastqc.contaminant_file",
          properties.getProperty(KEY_FASTQC_CONTAMINANT_FILE));
    }

    if (properties.getProperty(KEY_FASTQC_KMER_SIZE) != null
        && properties.getProperty(KEY_FASTQC_KMER_SIZE).length() > 0)

      System.setProperty("fastqc.kmer_size",
          properties.getProperty(KEY_FASTQC_KMER_SIZE));

    if (properties.getProperty(KEY_FASTQC_NOGROUP) != null
        && properties.getProperty(KEY_FASTQC_NOGROUP).length() > 0)

      System.setProperty("fastqc.nogroup",
          properties.getProperty(KEY_FASTQC_NOGROUP));

    // Set the number of threads
    if (properties.containsKey("qc.conf.fastqc.threads")) {

      try {
        int confThreads =
            Integer.parseInt(properties.getProperty("qc.conf.fastqc.threads")
                .trim());
        if (confThreads > 0)
          this.numberThreads = confThreads;

      } catch (NumberFormatException e) {
      }
    }
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    // Rewriting code of the method ContaminantFinder for read the contaminant
    // list in fastqc-0.10.1 jar
    RuntimePatchFastQC.runPatchFastQC();

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
