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

import java.util.List;
import java.util.Properties;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils;

public class DemultiplexingCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "demux";

  /** Prefix for run data */
  public static final String PREFIX = "demux";

  private String casavaOutputPath;
  private Collector subCollector;

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
  public void configure(final Properties properties) {

    if (properties == null) {
      return;
    }

    final String bcl2fastqVersion =
        properties.getProperty(QC.BCL2FASTQ_VERSION);

    if (SampleSheetUtils.isBcl2fastqVersion1(bcl2fastqVersion)) {
      // Call flowcell collector
      this.subCollector = new FlowcellDemuxSummaryCollector();

    } else if (SampleSheetUtils.isBcl2fastqVersion2(bcl2fastqVersion)) {
      // Conversion collector
      this.subCollector = new ConversionStatsCollector();
      
    } else {
      throw new AozanRuntimeException("bcl2fastq version is invalid "
          + bcl2fastqVersion + " to identify required Demux collector.");
    }

    // Init collector
    subCollector.configure(properties);
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    subCollector.collect(data);
  }

  @Override
  public void clear() {
    // TODO Auto-generated method stub

  }

  //
  // Getter
  //

  /**
   * Gets the casava output path.
   * @return the casava output path
   */
  public String getCasavaOutputPath() {
    return this.casavaOutputPath;
  }
}
