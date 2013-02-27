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
import java.io.IOException;
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
  public String[] getCollectorsNamesRequiered() {
    return super.getCollectorsNamesRequiered();
  }

  @Override
  public void configure(final Properties properties) {

    super.configure(properties);

    System.out.println("fsqC configure");

    System.setProperty("java.awt.headless", "true");
    System.setProperty("fastqc.unzip", "true");

  }

  @Override
  public void collectSample(final RunData data, final FastqSample fastqSample)
      throws AozanException {

    if (!(isExistRunDir && isExistBackupResults(data, fastqSample, true))) {

      // Process sample FASTQ(s)
      final SeqFileThread sft = processFile(data, fastqSample);

      if (sft != null) {
        System.out.println("fsc collect sample "
            + fastqSample.getProjectName() + "  nb thread "
            + getNumberThreads());

        threads.add(sft);
        futureThreads.add(executor.submit(sft, sft));
      }
    }
  }

  protected boolean isExistBackupResults(RunData data,
      final FastqSample fastqSample) {

    boolean isExist = false;

    // Check if results are save in temporary directory
    File qcreportFile =
        new File(qcReportOutputPath
            + "/Project_" + fastqSample.getProjectName() + "/"
            + fastqSample.getKeyFastqSample() + "-" + getName());

    if (!qcreportFile.exists() || !qcreportFile.isDirectory()) {
      isExist = false;

      System.out.println("verify exists back-up for \n\t"
          + qcreportFile.getAbsolutePath() + " " + qcreportFile.exists());
    } else {

      // Check for data file
      File dataFile =
          new File(qcReportOutputPath
              + "/Project_" + fastqSample.getProjectName() + "/" + getName()
              + "_" + fastqSample.getKeyFastqSample() + ".data");

      System.out.println("verify exists back-up for \n\t"
          + dataFile.getAbsolutePath() + "  " + dataFile.exists());

      if (!dataFile.exists()) {

        isExist = false;
      } else {

        try {
          // Restore results in data
          System.out.print("size rundata " + data.size());
          data.addDataFileInRundata(dataFile);
          System.out.println("\t\t add data file " + data.size());

          isExist = true;
        } catch (IOException io) {
          isExist = false;

        }
      }
    }
    return isExist;
  }

  /**
   * Process a FASTQ file.
   * @param data Run data
   * @param fastqSample FastqSample
   * @throws AozanException if an error occurs while processing a FASTQ file
   */
  public SeqFileThread processFile(final RunData data,
      final FastqSample fastqSample) throws AozanException {

    final File[] fastqFiles = fastqSample.getFastqFiles();

    if (fastqFiles == null || fastqFiles.length == 0) {
      return null;
    }

    // Create the thread object
    return new SeqFileThread(fastqSample, this.ignoreFilteredSequences,
        qcReportOutputPath, qcReportOutputPath);
  }

  //
  // Getters & Setters
  //

  public int getNumberThreads() {
    return numberThreads;
  }

  @Override
  public void setNumberThreads(final int number_threads) {
    numberThreads = number_threads;
  }
}
