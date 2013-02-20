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

import java.util.Properties;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;

public class UncompressFastqCollector extends AbstractFastqCollector {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final String COLLECTOR_NAME = "uncompressfastqscreen";
  private int numberThreads = Runtime.getRuntime().availableProcessors();

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
  public void collectSample(final RunData data, final int read, final int lane,
      final String projectName, final String sampleName, final String index,
      final int readSample) throws AozanException {

  }

  //
  // // LOGGER
  // // .fine("Start uncompressed all fastq Files before execute fastqscreen.");
  // //
  // // System.out
  // //
  // .println("Start uncompressed all fastq Files before execute fastqscreen.");
  //
  // final long startTime = System.currentTimeMillis();
  //
  // // Process sample FASTQ(s)
  // final AbstractFastqProcessThread uft =
  // processFile(data, casavaOutputPath, projectName, sampleName, index,
  // lane, readSample, COMPRESSION_EXTENSION);
  //
  // System.out.println("uncompress collecte "
  // + projectName + "  nb thread " + getNumberThreads());
  //
  // if (uft != null) {
  //
  // threads.add(uft);
  // futureThreads.add(executor.submit(uft, uft));
  //
  // }

  public void collectSample(final RunData data, final FastqSample fastqSample)
      throws AozanException {

    // LOGGER
    // .fine("Start uncompressed all fastq Files before execute fastqscreen.");
    //
    // System.out
    // .println("Start uncompressed all fastq Files before execute fastqscreen.");

    final long startTime = System.currentTimeMillis();

    // Process sample FASTQ(s)
    final AbstractFastqProcessThread uft = processFile(data, fastqSample);

    System.out.println("uncompress collecte "
        + fastqSample.getProjectName() + "  nb thread " + getNumberThreads());

    if (uft != null) {

      threads.add(uft);
      futureThreads.add(executor.submit(uft, uft));

    }

    // System.out
    // .println("End uncompressed "
    // + /* countFileDecompressed +
    // */" fastq files before execute fastqscreen in "
    // + toTimeHumanReadable(System.currentTimeMillis() - startTime));
    //
    // LOGGER
    // .fine("End uncompressed "
    // + /* countFileDecompressed +
    // */" fastq Files before execute fastqscreen in "
    // + toTimeHumanReadable(System.currentTimeMillis() - startTime));

  }

  /**
   * Process a FASTQ file.
   * @param data Run data
   * @param projectName name of the project
   * @param sampleName name of the sample
   * @param index sequence index
   * @param lane lane number
   * @param read read number
   * @throws AozanException if an error occurs while processing a FASTQ file
   */
  // public UncompressFastqThread processFile(final RunData data,
  // final String casavaOutputPath, final String projectName,
  // final String sampleName, final String index, final int lane,
  // final int read, final String compressionExtension) throws AozanException {
  //
  // // System.out.println("data "
  // // + data.size() + " " + casavaOutputPath + " " + projectName + " "
  // // + sampleName + " " + index + " " + lane + " " + COMPRESSION_EXTENSION);
  //
  // // Set the list of the files for the FASTQ data
  // final File[] fastqFiles =
  // fastqStorage.createListFastqFiles(casavaOutputPath, read, lane,
  // projectName, sampleName, index);
  //
  // if (fastqFiles == null || fastqFiles.length == 0)
  // return null;
  //
  // // Control the fastq files have been treated, if true return null
  // String key = fastqStorage.keyFiles(fastqFiles);
  // if (fastqStorage.tmpFileExist(key))
  // return null;
  //
  // // Create the thread object
  // return new UncompressFastqThread(fastqFiles, read, lane, projectName,
  // sampleName, key);
  // }

  public UncompressFastqThread processFile(final RunData data,
      final FastqSample fastqSample) throws AozanException {

    if (fastqSample.getFastqFiles() == null
        || fastqSample.getFastqFiles().length == 0) {
      return null;
    }

    if (fastqStorage.tmpFileExist(fastqSample.getKeyFastqFiles()))
      return null;

    // Create the thread object
    return new UncompressFastqThread(fastqSample);
  }

  @Override
  public int getNumberThreads() {
    return this.numberThreads;
  }

  @Override
  public void setNumberThreads(final int numberThreads) {
    this.numberThreads = numberThreads;
  }

}
