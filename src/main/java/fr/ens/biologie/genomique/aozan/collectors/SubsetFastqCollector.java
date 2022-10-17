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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Aozan2Logger;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.io.FastqSample;

/**
 * This class manages the creation of subset fastq files for contamination
 * research.
 * @since 1.1
 * @author Sandrine Perrin
 */
public class SubsetFastqCollector extends AbstractFastqCollector {

  /** Logger. */
  private static final Logger LOGGER = Aozan2Logger.getLogger();

  public static final String COLLECTOR_NAME = "subsetfastq";

  /** Parameters configuration. */
  private boolean skipControlLane;
  private boolean ignorePairedMode;
  private boolean isProcessUndeterminedIndicesSamples = false;
  private int maxReadLength = -1;

  // count reads pf necessary for create a temporary partial fastq
  private int countReadsPFtoCopy;
  // Limit parsing of the initial fastq file
  // if it is -1 parse integral fastq file
  private int maxReadsPFtoParse;

  private int numberThreads = Runtime.getRuntime().availableProcessors();

  /**
   * Get collector name.
   * @return name
   */
  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collectors to execute before fastqscreen Collector.
   * @return list of names collector
   */
  @Override
  public List<String> getCollectorsNamesRequiered() {

    final List<String> result = super.getCollectorsNamesRequiered();
    result.add(DemultiplexingCollector.COLLECTOR_NAME);

    return Collections.unmodifiableList(result);

  }

  @Override
  public void configure(final QC qc, final CollectorConfiguration conf) {

    super.configure(qc, conf);

    // Set the number of threads
    if (conf.containsKey(Settings.QC_CONF_THREADS_KEY)) {

      try {
        final int confThreads = conf.getInt(Settings.QC_CONF_THREADS_KEY, -1);
        if (confThreads > 0) {
          this.numberThreads = confThreads;
        }

      } catch (final NumberFormatException ignored) {
      }
    }

    // Set the maximum read length
    if (conf.containsKey(Settings.QC_CONF_FASTQSCREEN_MAX_READ_LENGTH_KEY)) {

      try {
        final int maxReadLength =
            conf.getInt(Settings.QC_CONF_FASTQSCREEN_MAX_READ_LENGTH_KEY, -1);
        if (maxReadLength > 0) {
          this.maxReadLength = maxReadLength;
        }

      } catch (final NumberFormatException ignored) {
      }
    }

    try {
      this.skipControlLane = conf.getBoolean(
          Settings.QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY);
    } catch (final Exception e) {
      // Default value
      this.skipControlLane = true;
    }

    try {
      this.ignorePairedMode = conf.getBoolean(
          Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_END_MODE_KEY);

    } catch (final Exception e) {
      // Default value
      this.ignorePairedMode = false;
    }

    final int readsToCopy = conf
        .getInt(Settings.QC_CONF_FASTQSCREEN_FASTQ_READS_PF_USED_KEY, 20000);
    if (readsToCopy == -1) {
      // Use a fully fastq file, force uncompress fastq file independently
      // number reads
      this.countReadsPFtoCopy = Integer.MAX_VALUE;
    } else {
      this.countReadsPFtoCopy = readsToCopy;
    }

    final int countReads = conf.getInt(
        Settings.QC_CONF_FASTQSCREEN_FASTQ_MAX_READS_PARSED_KEY, 30000000);
    if (countReads == -1) {
      // Parsing fully fastq file
      this.maxReadsPFtoParse = Integer.MAX_VALUE;
    } else {
      this.maxReadsPFtoParse = countReads;
    }

    // Check if process undetermined indices samples specify in Aozan
    // configuration
    this.isProcessUndeterminedIndicesSamples = conf.getBoolean(
        Settings.QC_CONF_FASTQSCREEN_PROCESS_UNDETERMINED_SAMPLES_KEY);
  }

  @Override
  public AbstractFastqProcessThread collectSample(final RunData data,
      final FastqSample fastqSample, final File reportDir, final boolean runPE)
      throws AozanException {

    requireNonNull(data, "data argument cannot be null");
    requireNonNull(fastqSample, "fastqSample argument cannot be null");
    requireNonNull(reportDir, "reportDir argument cannot be null");

    if (fastqSample.getFastqFiles() == null
        || fastqSample.getFastqFiles().isEmpty()) {
      throw new AozanException("Fail create partial FastQ for sample "
          + fastqSample.getSampleName() + " no FastQ file exist");
    }

    final boolean controlLane = data.isLaneControl(fastqSample.getSampleId());

    // Skip control lane
    if (controlLane && this.skipControlLane) {
      LOGGER.fine(COLLECTOR_NAME.toUpperCase()
          + ": " + fastqSample.getSampleName()
          + " is a control lane. Skip it.");
      return null;
    }

    // Ignore fastq from reads R2 in run PE if the mapping mode is not paired
    final boolean isPairedMode = runPE && !this.ignorePairedMode;
    if (!isPairedMode && fastqSample.getRead() == 2) {
      LOGGER.fine(COLLECTOR_NAME.toUpperCase()
          + ": Do not process second end for" + fastqSample.getSampleName()
          + " sample");
      return null;
    }

    // Check if FASTQ file(s) exists for sample
    if (fastqSample.getFastqFiles().isEmpty()) {
      LOGGER.fine(COLLECTOR_NAME.toUpperCase()
          + ": No FASTQ file exists for " + fastqSample.getSampleName()
          + " sample");
      return null;
    }

    // Check if the subset FASTQ file exists
    if (fastqSample.getSubsetFastqFile().exists()) {
      LOGGER.fine(COLLECTOR_NAME.toUpperCase()
          + ": subset FASTQ file already exists for "
          + fastqSample.getSampleName() + " sample: "
          + fastqSample.getSubsetFastqFile().getAbsolutePath());
      return null;
    }

    // Retrieve number of passing filter Illumina reads for this fastq
    // files
    final String prefix = "demux.sample"
        + fastqSample.getSampleId() + ".read" + fastqSample.getRead();

    LOGGER.fine(COLLECTOR_NAME.toUpperCase()
        + " collector found: " + prefix + ".pf.cluster.count="
        + data.get(prefix + ".pf.cluster.count"));
    LOGGER.fine(COLLECTOR_NAME.toUpperCase()
        + " collector found: " + prefix + ".raw.cluster.count="
        + data.get(prefix + ".raw.cluster.count"));

    // Check value exist in rundata, if not then fastq is empty
    if (!data.contains(prefix + ".pf.cluster.count")
        || !data.contains(prefix + ".raw.cluster.count")) {

      // No demultiplexing data exist
      LOGGER.warning(COLLECTOR_NAME.toUpperCase()
          + ": Cannot create subset FASTQ file for sample "
          + fastqSample.getSampleName() + " no demultiplexing data found.");

      // Return no thread
      return null;
    }

    final long pfClusterCount = data.getInt(prefix + ".pf.cluster.count");
    final long rawClusterCount = data.getInt(prefix + ".raw.cluster.count");

    // Create the thread object
    return new SubsetFastqThread(fastqSample, rawClusterCount, pfClusterCount,
        this.countReadsPFtoCopy, this.maxReadsPFtoParse, this.maxReadLength);
  }

  /**
   * No data file to save in UncompressCollector.
   */
  @Override
  protected void saveResultPart(final FastqSample fastqSample,
      final RunData data) {
  }

  @Override
  protected int getThreadsNumber() {
    return this.numberThreads;
  }

  @Override
  protected boolean isProcessUndeterminedIndicesSamples() {

    return this.isProcessUndeterminedIndicesSamples;
  }

}
