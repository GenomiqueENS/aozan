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


/**
 * This class define a abstract Lane test.
 * @since 0.8
 * @author Laurent Jourdren
 */
public abstract class AbstractLaneTest implements LaneTest {

  private final String name;
  private final String description;
  private final String columnName;
  private final String unit;

  //
  // Getters
  //

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public String getDescription() {

    return this.description;
  }

  @Override
  public String getColumnName() {

    return this.columnName;
  }

  @Override
  public String getUnit() {

    return this.unit;
  }

  //
  // Other methods
  //

  @Override
  public void init() {
  }

  //
  // Constructor
  //

  /**
   * Constructor that set the field of this abstract test.
   * @param name name of the test
   * @param description description of the test
   * @param columnName column name of the test
   */
  protected AbstractLaneTest(final String name, final String description,
      final String columnName) {

    this(name, description, columnName, "");
  }

  /**
   * Constructor that set the field of this abstract test.
   * @param name name of the test
   * @param description description of the test
   * @param columnName column name of the test
   * @param unit unit of the test
   */
  protected AbstractLaneTest(final String name, final String description,
      final String columnName, final String unit) {

    this.name = name;
    this.description = description;
    this.columnName = columnName;
    this.unit = unit;
  }

}
