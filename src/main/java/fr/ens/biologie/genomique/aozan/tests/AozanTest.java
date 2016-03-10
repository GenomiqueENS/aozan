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

package fr.ens.biologie.genomique.aozan.tests;

import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.aozan.AozanException;

/**
 * This interface define an Aozan QC test.
 * @since 0.8
 * @author Laurent Jourdren
 */
public interface AozanTest {

  /**
   * Get the name of the test.
   * @return the name of the test
   */
  String getName();

  /**
   * Get the description of the test.
   * @return the description of the test
   */
  String getDescription();

  /**
   * Get the column name of the test in the QC result file.
   * @return the coloumn name of the test
   */
  String getColumnName();

  /**
   * Get the unit of the result of the test.
   * @return the unit of the result of the test
   */
  String getUnit();

  /**
   * Get the name of the collectors required for the test.
   * @return a list of String with the name of the required collectors
   */
  List<String> getCollectorsNamesRequiered();

  /**
   * Configure the test.
   * @param properties a map with the configuration of the test
   * @return list of Aozan tests
   */
  List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException;

  /**
   * Initialize the test.
   * @throws AozanException if an error occurs while initialize the test.
   */
  void init() throws AozanException;
}
