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
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * This class define a FastQC Collector
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FastQCCollector extends AbstractFastqCollector {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /** The collector name. */
  public static final String COLLECTOR_NAME = "fastqc";

  private int numberThreads = Runtime.getRuntime().availableProcessors();

  private boolean ignoreFilteredSequences = false;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public void configure(final Properties properties) {

    super.configure(properties);

    System.setProperty("java.awt.headless", "true");
    System.setProperty("fastqc.unzip", "true");

    // TODO REVIEW: Insert here the code to set the number of threads from
  }

  @Override
  public AbstractFastqProcessThread collectSample(final RunData data,
      final FastqSample fastqSample, final File reportDir)
      throws AozanException {

    final List<File> fastqFiles = fastqSample.getFastqFiles();

    if (fastqFiles.isEmpty()) {
      return null;
    }

    // Create the thread object
    return new FastQCProcessThread(fastqSample, this.ignoreFilteredSequences,
        reportDir);
  }

  // TODO REVIEW:
  // All the backup code must be in the super class and must be
  // reused by all child classes
  // A class that inherits from AbstractFastqCollector must be extremely simple:
  // protected AbstractFastqProcessThread createFastqProcessThread(
  // final RunData data, final FastqSample fastqSample) {
  // return null;
  // }

  //
  // Getters & Setters
  //

  @Override
  // TODO REVIEW: Why is this method is there and not in its super class ?
  public int getThreadsNumber() {
    return numberThreads;
  }

  @Override
  // TODO REVIEW: Why is this method is there and not in its super class ?
  public void setThreadsNumber(final int number_threads) {
    numberThreads = number_threads;
  }
}
