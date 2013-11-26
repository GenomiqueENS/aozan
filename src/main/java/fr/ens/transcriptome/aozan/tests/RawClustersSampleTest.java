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

package fr.ens.transcriptome.aozan.tests;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.collectors.FlowcellDemuxSummaryCollector;

/**
 * This class define a raw clusters count test for samples.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class RawClustersSampleTest extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(FlowcellDemuxSummaryCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final int read, final int readSample, final int lane,
      final String sampleName) {

    if (sampleName == null)
      return "demux.lane"
          + lane + ".sample.lane" + lane + ".read" + readSample
          + ".raw.cluster.count";

    return "demux.lane"
        + lane + ".sample." + sampleName + ".read" + readSample
        + ".raw.cluster.count";
  }

  @Override
  protected Class<?> getValueType() {

    return Integer.class;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public RawClustersSampleTest() {
    super("rawclusterssamples", "", "Raw clusters");
  }

}
