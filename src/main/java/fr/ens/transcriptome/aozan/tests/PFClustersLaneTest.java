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

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.ReadCollector;

/**
 * This class define a Passing Filter cluster lane test.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class PFClustersLaneTest extends AbstractSimpleLaneTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ReadCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(int read, boolean indexedRead, int lane) {

    return "read" + read + ".lane" + lane + ".clusters.pf";
  }

  @Override
  protected Class<?> getValueType() {

    return Long.class;
  }

  @Override
  protected Number transformValue(final Number value, final RunData data,
      final int read, final boolean indexedRead, final int lane) {

    final int tiles =
        data.getInt("read" + read + ".lane" + lane + ".tile.count");

    return value.longValue() * tiles;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PFClustersLaneTest() {

    super("pfclusters", "", "PF Clusters Est.");
  }

}
