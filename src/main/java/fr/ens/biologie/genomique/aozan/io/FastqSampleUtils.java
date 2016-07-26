package fr.ens.biologie.genomique.aozan.io;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;

public class FastqSampleUtils {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  /**
   * Create a set of FastqSample to process.
   * @param qc the QC object
   * @param data the RunData object
   * @param processStandardSamples true if standard samples must be processed
   * @param processUndeterminedSamples true if undetermined samples must be
   *          processed
   * @param processOnlyFirstRead true if only the first read must be processed
   * @return a set of of FastqSample to process
   * @throws IOException if an error occurs while creating a FastqSample
   */
  public static Set<FastqSample> createListFastqSamples2(final QC qc,
      final RunData data, final boolean processStandardSamples,
      final boolean processUndeterminedSamples,
      final boolean processOnlyFirstRead) throws IOException {

    final Set<FastqSample> result = new LinkedHashSet<>();

    final int readCount = data.getReadCount();

    int readIndexedCount = 0;

    for (int read = 1; read <= readCount; read++) {

      if (data.isReadIndexed(read)) {
        continue;
      }

      readIndexedCount++;

      for (int sampleId : data.getAllSamples()) {

        final int lane = data.getSampleLane(sampleId);
        final boolean undetermined = data.isUndeterminedSample(sampleId);

        if (!undetermined && processStandardSamples) {

          // Skip invalid sample for quality control, like FASTQ file empty
          if (!isValidFastQSampleForQC(data, sampleId, readIndexedCount)) {
            continue;
          }

          // Get the sample index
          final String index = data.getIndexSample(sampleId);

          // Get sample name
          final String demuxName = data.getSampleDemuxName(sampleId);

          // Get project name
          final String projectName = data.getProjectSample(sampleId);

          // Get description on sample
          final String descriptionSample = data.getSampleDescription(sampleId);

          result.add(new FastqSample(qc, sampleId, readIndexedCount, lane,
              demuxName, projectName, descriptionSample, index));
        }

        if (undetermined && processUndeterminedSamples) {

          // Add undetermined sample
          result.add(new FastqSample(qc, sampleId, readIndexedCount, lane));
        }

      } // Sample

      // Process only one read if needed
      if (processOnlyFirstRead) {
        break;
      }

    } // Read

    return result;
  }

  /**
   * Checks if is valid fast q sample for qc.
   * @param data result data object
   * @param sampleId the sample Id
   * @param read the read number
   * @return true, if sample is valid otherwise false, like FASTQ file empty
   */
  private static boolean isValidFastQSampleForQC(final RunData data,
      final int sampleId, final int read) {

    final String prefix = "demux.sample" + sampleId + ".read" + read;

    // Check value exist in rundata, if not then fastq is empty
    final boolean valid = !(data.get(prefix + ".pf.cluster.count") == null
        || data.get(prefix + ".raw.cluster.count") == null);

    if (!valid)
      LOGGER.warning("Sample "
          + data.getSampleDemuxName(sampleId) + " lane "
          + data.getSampleLane(sampleId)
          + ": no demultiplexing data found, no quality control data. Use prefix in rundata "
          + prefix);

    // Return true if sample valid
    return valid;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private FastqSampleUtils() {
  }

}
