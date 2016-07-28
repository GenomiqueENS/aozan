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

package fr.ens.biologie.genomique.aozan.tests.sample;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.collectors.DemultiplexingCollector;

/**
 * This class define a passing filter clusters count test for samples.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class PFClustersSampleTest extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(DemultiplexingCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final int read, final int readSample,
      final int sampleId, final int lane, boolean undetermined) {

    return "demux.sample"
        + sampleId + ".read" + readSample + ".pf.cluster.count";
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
  public PFClustersSampleTest() {
    super("sample.pfclusterssamples", "", "PF_clusters");
  }

}
