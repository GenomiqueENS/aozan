/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan.tests;

/**
 * This interface define an Aozan QC test.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface AozanTest {

  /** Integer result data format. */
  public String INTEGER_FORMAT = "%,d";

  /** Double result data format. */
  public String DOUBLE_FORMAT = "%,.2f";

  /**
   * Get the name of the test.
   * @return the name of the test
   */
  public String getName();

  /**
   * Get the description of the test.
   * @return the description of the test
   */
  public String getDescription();

  /**
   * Get the column name of the test in the QC result file
   * @return the coloumn name of the test
   */
  public String getColumnName();

  /**
   * Get the unit of the result of the test
   * @return the unit of the result of the test
   */
  public String getUnit();

  /**
   * Get the name of the collectors required for the test.
   * @return an array of String with the name of the required collectors
   */
  public String[] getCollectorsNamesRequiered();

}
