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

package fr.ens.biologie.genomique.aozan.tests.lane;

import static fr.ens.biologie.genomique.aozan.collectors.ReadCollector.READ_DATA_PREFIX;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.ReadCollector;

/**
 * This class define a test on error rate on 35 cycle.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class ErrorRate35CycleLaneTest extends AbstractSimpleLaneTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ReadCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(int read, boolean indexedRead, int lane) {

    return READ_DATA_PREFIX + ".read" + read + ".lane" + lane + ".err.rate.35";
  }

  @Override
  protected Class<?> getValueType() {

    return Double.class;
  }

  @Override
  protected boolean isValuePercent() {

    return true;
  }

  @Override
  protected Number transformValue(final Number value, final RunData data,
      final int read, final boolean indexedRead, final int lane) {

    return value.doubleValue() / 100.0;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public ErrorRate35CycleLaneTest() {

    super("errorrate35cycle", "", "Error Rate 35 cycles", "%");
  }

}
