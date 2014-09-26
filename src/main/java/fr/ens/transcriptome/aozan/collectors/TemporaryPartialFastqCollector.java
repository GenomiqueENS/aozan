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
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * This class manages the creation of temporary partial fastq file for
 * contamination research.
 * @since 1.1
 * @author Sandrine Perrin
 */
public class TemporaryPartialFastqCollector extends AbstractFastqCollector {

  public static final String COLLECTOR_NAME = "tmppartialfastq";

  /** Parameters configuration */
  private boolean skipControlLane;
  private boolean ignorePairedMode;

  // count reads pf necessary for create a temporary partial fastq
  private int countReadsPFtoCopy;
  // Limit parsing of the initial fastq file
  // if it is -1 parse integral fastq file
  private int maxReadsPFtoParse;

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

    try {
      this.skipControlLane =
          Boolean
              .parseBoolean(properties
                  .getProperty(Settings.QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY));
    } catch (Exception e) {
      // Default value
      this.skipControlLane = true;
    }

    try {
      this.ignorePairedMode =
          Boolean
              .parseBoolean(properties
                  .getProperty(Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_MODE_KEY));

    } catch (Exception e) {
      // Default value
      this.ignorePairedMode = false;
    }

    int readsToCopy =
        Integer.parseInt(properties
            .getProperty(Settings.QC_CONF_FASTQSCREEN_FASTQ_READS_PF_USED_KEY));
    if (readsToCopy == -1) {
      // Use a fully fastq file, force uncompress fastq file independently
      // number reads
      this.countReadsPFtoCopy = Integer.MAX_VALUE;
    } else {
      this.countReadsPFtoCopy = readsToCopy;
    }

    final int countReads =
        Integer
            .parseInt(properties
                .getProperty(Settings.QC_CONF_FASTQSCREEN_FASTQ_MAX_READS_PARSED_KEY));
    if (countReads == -1) {
      // Parsing fully fastq file
      this.maxReadsPFtoParse = Integer.MAX_VALUE;
    } else {
      this.maxReadsPFtoParse = countReads;
    }
  }

  @Override
  public AbstractFastqProcessThread collectSample(final RunData data,
      final FastqSample fastqSample, final File reportDir, final boolean runPE)
      throws AozanException {

    if (fastqSample == null)
      return null;

    if (fastqSample.getFastqFiles() == null
        || fastqSample.getFastqFiles().isEmpty()) {
      throw new AozanException("Fail create partial FastQ for sample "
          + fastqSample.getSampleName() + " no FastQ file exist");
    }

    final boolean controlLane =
        data.getBoolean("design.lane"
            + fastqSample.getLane() + "." + fastqSample.getSampleName()
            + ".control");

    // Skip control lane
    if (controlLane && skipControlLane) {
      return null;
    }

    // Ignore fastq from reads R2 in run PE if the mapping mode is not paired
    final boolean isPairedMode = runPE && !ignorePairedMode;
    if (!isPairedMode && fastqSample.getRead() == 2)
      return null;

    // Check if the temporary partial fastq file exists
    if (getFastqStorage().tmpFileExists(fastqSample))
      return null;

    // Retrieve number of passing filter Illumina reads for this fastq
    // files
    final String prefix =
        "demux.lane"
            + fastqSample.getLane() + ".sample." + fastqSample.getSampleName()
            + ".read" + fastqSample.getRead();

    // Check value exist in rundata, if not then fastq is empty
    if (data.get(prefix + ".pf.cluster.count") == null
        || data.get(prefix + ".raw.cluster.count") == null)
      // No demultiplexing data exist
      throw new AozanException("Fail create partial FastQ for sample "
          + fastqSample.getSampleName() + " no demultiplexing data found.");

    int pfClusterCount = data.getInt(prefix + ".pf.cluster.count");
    int rawClusterCount = data.getInt(prefix + ".raw.cluster.count");

    // Create the thread object
    return new TemporaryPartialFastqThread(fastqSample, rawClusterCount,
        pfClusterCount, this.countReadsPFtoCopy, this.maxReadsPFtoParse);
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
