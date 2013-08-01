/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 3 or
 * later and CeCILL. This should be distributed with the code.
 * If you do not have a copy, see:
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
 * or to join the Aozan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;

public class TemporaryPartialFastqCollector extends AbstractFastqCollector {

  public static final String COLLECTOR_NAME = "tmppartialfastq";
  public static final String KEY_IGNORE_PAIRED_MODE =
      "qc.conf.ignore.paired.mode";

  private boolean ignorePairedMode;
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
    result.add(FlowcellDemuxSummaryCollector.COLLECTOR_NAME);

    return Collections.unmodifiableList(result);

  }

  @Override
  public void configure(final Properties properties) {
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

    try {
      this.ignorePairedMode =
          Boolean.parseBoolean(properties.getProperty(KEY_IGNORE_PAIRED_MODE));

    } catch (Exception e) {
      // Default value
      this.ignorePairedMode = true;
    }

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

    // Check mode defined
    final boolean singleMode = !runPE || ignorePairedMode;

    // Ignore R2 fastq files
    if (singleMode && fastqSample.getRead() == 2) {
      return null;
    }

    // Check if the temporary partial fastq file exists
    if (fastqStorage.tmpFileExists(fastqSample))
      return null;

    // Retrieve number of passing filter Illumina reads for this fastq
    // files
    String prefix =
        "demux.lane"
            + fastqSample.getLane() + ".sample." + fastqSample.getSampleName()
            + ".read" + fastqSample.getRead();

    int pfClusterCount = data.getInt(prefix + ".pf.cluster.count");
    int rawClusterCount = data.getInt(prefix + ".raw.cluster.count");

    // Create the thread object
    return new TemporaryPartialFastqThread(fastqSample, rawClusterCount,
        pfClusterCount);
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

}
