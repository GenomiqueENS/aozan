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
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * The class realize the creating of array of compressed fastq files in a
 * temporary files, in multitasking mode.
 * @author Sandrine Perrin
 */
public class UncompressFastqCollector extends AbstractFastqCollector {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final String COLLECTOR_NAME = "uncompressfastqscreen";

  private static int numberThreads = Runtime.getRuntime().availableProcessors();

  @Override
  public void configure(Properties properties) {
    super.configure(properties);

    System.out.println("uncompress configure");
  }

  @Override
  /**
   * Uncompress fastq files from each sample in temporaries files
   * @param data
   * @param casavaOutputPath fastq file path
   * @param compressionExtension extension of file
   * @throws AozanException if an error occurs while creating thread
   */
  public void collectSample(final RunData data, final FastqSample fastqSample)
      throws AozanException {

    if (!(isExistRunDir && isExistBackupResults(data, fastqSample, false))) {

      // Process sample FASTQ(s)
      final AbstractFastqProcessThread uft = processFile(fastqSample);

      if (uft != null) {

        threads.add(uft);
        futureThreads.add(executor.submit(uft, uft));
      }
    }
  }

  protected boolean isExistBackupResults(RunData data,
      final FastqSample fastqSample) throws AozanException {

    // Check for report file
    File qcreportFile =
        new File(qcReportOutputPath
            + "/Project_" + fastqSample.getProjectName() + "/"
            + fastqSample.getKeyFastqSample() + "-fastqscreen.txt");

    // Check for data file
    File dataFile =
        new File(qcReportOutputPath
            + "/Project_" + fastqSample.getProjectName() + "/fastqscreen_"
            + fastqSample.getKeyFastqSample() + ".data");

    System.out.println("verify exists back-up for \n\t"
        + qcreportFile.getAbsolutePath() + " " + qcreportFile.exists() + "\n\t"
        + dataFile.getAbsolutePath() + "  " + dataFile.exists());

    return dataFile.exists() && qcreportFile.exists();

  }

  /**
   * Process a FASTQ file. Create a thread which create a uncompressed fastq
   * file if not exists, or null if one fastq to uncompress or if the
   * uncompressed fastq files exists
   * @param data
   * @param fastqSample
   * @return a thread uncompress object or null
   * @throws AozanException if an error occurs while processing a FASTQ file
   */
  public UncompressFastqThread processFile(final FastqSample fastqSample)
      throws AozanException {

    if (fastqSample.getFastqFiles() == null
        || fastqSample.getFastqFiles().length == 0) {
      return null;
    }

    // Check if the uncompressed fastq file exists
    if (fastqStorage.tmpFileExist(fastqSample.getKeyFastqFiles()))
      return null;

    // Create the thread object
    return new UncompressFastqThread(fastqSample);
  }

  @Override
  public int getNumberThreads() {
    return numberThreads;
  }

  @Override
  public void setNumberThreads(final int number_threads) {
    numberThreads = number_threads;
  }

  @Override
  /**
   * Get collector name
   * @return name 
   */
  public String getName() {
    return COLLECTOR_NAME;
  }

}
