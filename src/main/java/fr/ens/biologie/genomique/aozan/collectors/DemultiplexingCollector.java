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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.illumina.Bcl2FastqOutput;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;

public class DemultiplexingCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "demux";

  /** Prefix for run data */
  public static final String PREFIX = "demux";

  private QC qc;
  private File bcl2fastqOutputPath;
  private SampleSheet samplesheet;
  private CollectorConfiguration conf;

  //
  // Useful methods
  //

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(RunInfoCollector.COLLECTOR_NAME,
        SamplesheetCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final QC qc, final CollectorConfiguration conf) {

    if (conf == null) {
      return;
    }

    this.qc = qc;
    this.bcl2fastqOutputPath = qc.getFastqDir();
    this.samplesheet = qc.getSampleSheet();
    this.conf = new CollectorConfiguration(conf);
  }

  @Override
  public void collect(final RunData data) throws AozanException {


    final Collector subCollector;

    final Bcl2FastqOutput manager;

    try {
      manager =
          new Bcl2FastqOutput(this.samplesheet, this.bcl2fastqOutputPath);
    } catch (IOException e) {
      throw new AozanException(e);
    }

    switch (manager.getVersion()) {

    case BCL2FASTQ_1:

      // Call flowcell collector
      subCollector = new FlowcellDemuxSummaryCollector();
      break;

    case BCL2FASTQ_2:
    case BCL2FASTQ_2_15:

      // Conversion collector
      subCollector = new ConversionStatsCollector();
      break;

    case BCL_CONVERT:
      // Demultiplex Stats collector
      subCollector = new DemultiplexStatsCollector();
      break;

    default:

      throw new AozanRuntimeException(
          "Unable to detect the version of bcl2fastq used to generate data");

    }

    // Add Bcl2fastq version to result
    data.put(PREFIX + ".bcl2fastq.version", manager.getFullVersion());

    // Init collector
    subCollector.configure(qc, this.conf);

    // Create a temporary RunData object
    final RunData tmp = new RunData();

    // Put in the temporary RunData object the required keys
    tmp.put("run.info.read.count", data.getReadCount());
    for (int read = 1; read <= data.getReadCount(); read++) {
      tmp.put("run.info.read" + read + ".indexed", data.isReadIndexed(read));
      tmp.put("run.info.read" + read + ".cycles", data.getReadCyclesCount(read));
    }

    // Collect data
    subCollector.collect(tmp);

    // Convert temporary RunData
    convert(tmp, data);
  }

  /**
   * Check the right prefix.
   * @param data RunData object
   * @param index the index for the sample
   * @param oldPrefix1 the first old prefix to check
   * @param oldPrefix2 the second old prefix to check
   * @return the old prefix that exists in the RunData object
   */
  private String checkOldPrefix(final RunData data, final String index,
      final String oldPrefix1, final String oldPrefix2) {

    final String barcodeKey1 = oldPrefix1 + "barcode";
    final String barcodeKey2 = oldPrefix2 + "barcode";

    final String defaultResult = oldPrefix1;

    if (data.contains(barcodeKey1) && !data.contains(barcodeKey2)) {
      return oldPrefix1;
    }
    if (!data.contains(barcodeKey1) && data.contains(barcodeKey2)) {
      return oldPrefix2;
    }

    final String val1 = data.get(barcodeKey1);
    final String val2 = data.get(barcodeKey2);

    if (val1 != null && val2 != null) {

      final String barcode = "".equals(index) ? "all" : index;

      if (val1.equals(barcode)) {
        return oldPrefix1;
      }

      if (val2.equals(barcode)) {
        return oldPrefix2;
      }

      if (val1.startsWith(barcode)) {
        return oldPrefix1;
      }

      if (val2.startsWith(barcode)) {
        return oldPrefix2;
      }
    }

    for (String key : data.getMap().keySet()) {

      if (key.startsWith(oldPrefix1)) {
        return oldPrefix1;
      }

      if (key.startsWith(oldPrefix2)) {
        return oldPrefix2;
      }

    }

    return defaultResult;
  }

  /**
   * Convert the keys with old prefix to new keys with a new prefix.
   * @param inputData the input RunData object
   * @param outputData the output RunData object
   */
  private void convert(final RunData inputData, final RunData outputData) {

    final Map<String, String> prefixes = new HashMap<>();
    final int laneCount = outputData.getLaneCount();

    // Compute the prefix convertion table
    for (int sampleId : outputData.getAllSamples()) {

      final int lane = outputData.getSampleLane(sampleId);
      final boolean undetermined = outputData.isUndeterminedSample(sampleId);
      final String demuxName = outputData.getSampleDemuxName(sampleId);
      final String identifier = outputData.getSampleIdentifier(sampleId);
      final String barcode = outputData.getSampleIndex(sampleId);

      final String oldPrefix216 = "demux.lane"
          + lane + ".sample." + (undetermined ? "unknown" : identifier) + '.';

      final String oldPrefix217 = "demux.lane"
          + lane + ".sample." + (undetermined ? "undetermined" : demuxName)
          + '.';

      // Handle Bcl2fastq 2.16
      final String oldPrefix =
          checkOldPrefix(inputData, barcode, oldPrefix216, oldPrefix217);

      final String newPrefix = "demux.sample" + sampleId + '.';
      prefixes.put(oldPrefix.toLowerCase(), newPrefix);
    }

    // Add the new keys
    for (String key : new LinkedHashSet<>(inputData.getMap().keySet())) {

      for (Map.Entry<String, String> e : prefixes.entrySet()) {

        final String oldPrefixKey = e.getKey();
        if (key.startsWith(oldPrefixKey)) {

          final String newKey = key.replace(oldPrefixKey, e.getValue());
          outputData.put(newKey, inputData.get(key));
          break;
        }
      }

      // Keep lane old keys
      for (int lane = 1; lane <= laneCount; lane++) {
        if (key.startsWith("demux.lane" + lane + ".all.")) {
          outputData.put(key, inputData.get(key));
        }
      }
    }

  }

  @Override
  public void clear() {
  }

  @Override
  public boolean isSummaryCollector() {
    return false;
  }

}
