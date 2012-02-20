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
 * This class define a test result.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class TestResult {

  private final int score;
  private final Class<?> type;
  private final String message;

  //
  // Getters
  //

  /**
   * Get the score of the test
   * @return the score of the test
   */
  public int getScore() {

    return this.score;
  }

  /**
   * Get the message of the result.
   * @return Returns the message
   */
  public String getMessage() {

    return this.message;
  }

  /**
   * Get the type of the result message.
   * @return the type of the result message as a string
   */
  public String getType() {

    if (this.type == Long.class)
      return "int";

    if (this.type == Double.class)
      return "float";

    return "string";
  }

  //
  // Other methods
  //

  /**
   * Get the class of a number object.
   * @param value number object
   * @return the Class of this object for TestResult
   */
  private static final Class<?> getNumberClass(final Number value) {

    if (value == null)
      return null;

    final Class<?> clazz = value.getClass();

    if (clazz == Byte.class
        || clazz == Short.class || clazz == Integer.class
        || clazz == Long.class)
      return Long.class;

    return Double.class;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param score score of a test
   * @param message result message of a test
   */
  public TestResult(final int score, final Number value) {

    if (score < -1 || score > 9)
      throw new IllegalArgumentException(
          "The score value must be between -1 and 9");

    this.score = score;
    this.message = value == null ? "" : value.toString();
    this.type = getNumberClass(value);
  }

  /**
   * Public constructor.
   * @param message result message of a test
   */
  public TestResult(final Number value) {

    this.score = -1;
    this.message = value.toString();
    this.type = getNumberClass(value);
  }

  /**
   * Public constructor.
   * @param score score of a test
   * @param message result message of a test
   */
  public TestResult(final int score, final String message) {

    if (score < -1 || score > 9)
      throw new IllegalArgumentException(
          "The score value must be between -1 and 9");

    this.score = score;
    this.message = message == null ? "" : message;
    this.type = String.class;
  }

  /**
   * Public constructor.
   * @param message result message of a test
   */
  public TestResult(final String message) {

    this.score = -1;
    this.message = message;
    this.type = String.class;
  }

}
