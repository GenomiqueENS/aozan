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
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * The class realize the creating of array of compressed fastq files in a
 * temporary files, in multitasking mode.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class UncompressFastqCollector extends AbstractFastqCollector {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  public static final String COLLECTOR_NAME = "uncompressfastq";
  private long uncompressedSizeFiles = 0l;

  private int numberThreads = Runtime.getRuntime().availableProcessors();

  /**
   * Get collector name
   * @return name
   */
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
    result.add(FastQCCollector.COLLECTOR_NAME);

    return Collections.unmodifiableList(result);

  }

  @Override
  public void configure(final Properties properties) {
    super.configure(properties);

    // Set the number of threads
    if (properties.containsKey(Settings.QC_CONF_THREADS_KEY)) {

      try {
        int confThreads =
            Integer.parseInt(properties.getProperty(Settings.QC_CONF_THREADS_KEY)
                .trim());
        if (confThreads > 0)
          this.numberThreads = confThreads;

      } catch (NumberFormatException e) {
      }
    }

  }

  @Override
  public void collect(final RunData data) throws AozanException {
    super.collect(data);
    controlPreCollect();

  }

  @Override
  public AbstractFastqProcessThread collectSample(final RunData data,
      final FastqSample fastqSample, final File reportDir, final boolean runPE)
      throws AozanException {

    if (fastqSample == null
        || fastqSample.getFastqFiles() == null
        || fastqSample.getFastqFiles().isEmpty()) {
      return null;
    }

    // Check if the uncompressed fastq file exists
    if (fastqStorage.tmpFileExists(fastqSample))
      return null;

    // Create the thread object
    return new UncompressFastqThread(fastqSample);
  }

  /**
   * No data file to save in UncompressCollector
   */
  @Override
  protected void saveResultPart(final FastqSample fastqSample,
      final RunData data) {
    return;
  }

  @Override
  protected int getThreadsNumber() {
    return numberThreads;
  }

  /**
   * Realize all preliminary control before execute AbstractFastqCollector, it
   * check if the free space in tmp directory is enough for save all
   * uncompressed fastq files.
   * @throws AozanException
   */
  private void controlPreCollect() throws AozanException {

    LOGGER.fine("Collector uncompressed fastq files : step preparation");

    // Count size from all fastq files used
    long freeSpace = new File(this.tmpPath).getFreeSpace();
    freeSpace = freeSpace / (1024 * 1024 * 1024);

    for (FastqSample fastqSample : fastqSamples) {
      // Check temporary fastq files exists
      if (!(new File(tmpPath + "/" + fastqSample.getNameTemporaryFastqFiles())
          .exists())) {
        this.uncompressedSizeFiles += fastqSample.getUncompressedSize();
      }

    }

    // Estimate used space : needed space + 5%
    long uncompressedSizeNeeded = (long) (this.uncompressedSizeFiles * 1.05);
    uncompressedSizeNeeded = uncompressedSizeNeeded / (1024 * 1024 * 1024);

    if (uncompressedSizeNeeded > freeSpace)
      throw new AozanException(
          "Not enough disk space to store uncompressed fastq files for step fastqScreen. We are "
              + freeSpace
              + " Go in directory "
              + new File(this.tmpPath).getAbsolutePath()
              + ", and we need "
              + uncompressedSizeNeeded + " Go. Fail Aozan");

    LOGGER
        .fine("Enough disk space to store uncompressed fastq files for step fastqScreen. We are "
            + freeSpace
            + " Go in directory "
            + new File(this.tmpPath).getAbsolutePath()
            + ", and we need "
            + uncompressedSizeNeeded + " Go.");

  }
}
