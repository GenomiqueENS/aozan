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

package fr.ens.biologie.genomique.aozan.tests.lane;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.collectors.ReadCollector;

/**
 * This class define a test on first cycle intensity.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class FirstCycleIntensityPFLaneTest extends AbstractSimpleLaneTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ReadCollector.COLLECTOR_NAME);
  }

  @Override
  protected Class<?> getValueType() {

    return Integer.class;
  }

  @Override
  protected String getKey(final int read, final boolean indexedRead,
      final int lane) {

    return "read" + read + ".lane" + lane + ".first.cycle.int.pf";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public FirstCycleIntensityPFLaneTest() {

    super("firstcycleintensity", "", "First cycle intensity");
  }

}
