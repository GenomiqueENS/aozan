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
 *      http://tools.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

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
  private File casavaOutputPath;
  private File samplesheetFile;
  private Properties conf;

  //
  // Useful methods
  //

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(RunInfoCollector.COLLECTOR_NAME);
  }

  @Override
  public boolean isStatisticCollector() {
    return false;
  }

  @Override
  public void configure(final QC qc, final Properties properties) {

    if (properties == null) {
      return;
    }

    this.qc = qc;
    this.casavaOutputPath = qc.getFastqDir();
    this.samplesheetFile = qc.getSampleSheetFile();
    this.conf = new Properties(properties);
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    final Collector subCollector;

    final Bcl2FastqOutput manager;

    try {
      manager =
          new Bcl2FastqOutput(this.samplesheetFile, this.casavaOutputPath);
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
  }

  @Override
  public void clear() {
  }

  //
  // Getter
  //

  /**
   * Gets the casava output path.
   * @return the casava output path
   */
  public File getCasavaOutputPath() {
    return this.casavaOutputPath;
  }
}
