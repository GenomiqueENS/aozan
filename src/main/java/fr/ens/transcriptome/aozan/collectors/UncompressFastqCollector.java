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

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * The class realize the creating of array of compressed fastq files in a
 * temporary files, in multitasking mode.
 * @author Sandrine Perrin
 */
public class UncompressFastqCollector extends AbstractFastqCollector {

  public static final String COLLECTOR_NAME = "uncompressfastq";

  private int numberThreads = Runtime.getRuntime().availableProcessors();

  @Override
  /**
   * Get collector name
   * @return name 
   */
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collectors to execute before fastqscreen Collector
   * @return list of names collector
   */
  @Override
  public String[] getCollectorsNamesRequiered() {

    List<String> result =
        Lists.newArrayList(super.getCollectorsNamesRequiered());
    result.add(FastQCCollector.COLLECTOR_NAME);

    return result.toArray(new String[] {});

  }

  @Override
  public void configure(Properties properties) {
    super.configure(properties);

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
  public AbstractFastqProcessThread collectSample(final RunData data,
      final FastqSample fastqSample, final File reportDir)
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

  @Override
  /**
   * No data file to save in UncompressCollector
   */
  protected void saveResultPart(final FastqSample fastqSample,
      final RunData data) {
    return;
  }

  @Override
  public int getThreadsNumber() {
    return numberThreads;
  }

}
