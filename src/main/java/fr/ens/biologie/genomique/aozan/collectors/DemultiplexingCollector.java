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

public class DemultiplexingCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "demux";

  /** Prefix for run data */
  public static final String PREFIX = "demux";

  private QC qc;
  private File bcl2fastqOutputPath;
  private File samplesheetFile;
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
    this.samplesheetFile = qc.getSampleSheetFile();
    this.conf = new CollectorConfiguration(conf);
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    final Collector subCollector;

    final Bcl2FastqOutput manager;

    try {
      manager =
          new Bcl2FastqOutput(this.samplesheetFile, this.bcl2fastqOutputPath);
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

    default:

      throw new AozanRuntimeException(
          "Unable to detect the version of bcl2fastq used to generate data");

    }

    // Init collector
    subCollector.configure(qc, this.conf);

    // Collect data
    subCollector.collect(data);

    // Add the new keys here
    addNewKeys(data);
  }

  private void addNewKeys(final RunData data) {

    final Map<String, String> prefixes = new HashMap<>();

    // Compute the prefix convertion table
    for (int sampleId : data.getAllSamples()) {

      final int lane = data.getSampleLane(sampleId);
      final boolean undetermined = data.isUndeterminedSample(sampleId);
      final String sampleName = data.getSampleDemuxName(sampleId);

      final String oldPrefix = "demux.lane"
          + lane + ".sample." + (undetermined ? "undetermined" : sampleName)
          + '.';
      final String newPrefix = "demux.sample" + sampleId + '.';

      prefixes.put(oldPrefix, newPrefix);
    }

    // Add the new keys
    for (String key : new LinkedHashSet<>(data.getMap().keySet())) {

      for (Map.Entry<String, String> e : prefixes.entrySet()) {

        final String oldPrefixKey = e.getKey();
        if (key.startsWith(oldPrefixKey)) {

          final String newKey = key.replace(oldPrefixKey, e.getValue());

          data.put(newKey, data.get(key));
        }
      }
    }
  }

  @Override
  public void clear() {
  }

}
