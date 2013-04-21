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

/**
 * This class define a test result.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class TestResult {

  private final int score;
  private final String type;
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

    return this.type;
  }

  //
  // Other methods
  //

  /**
   * Get the class of a number object.
   * @param value number object
   * @return the Class of this object for TestResult
   */
  private static final String getNumberClass(final Number value) {

    if (value == null)
      return null;

    final Class<?> clazz = value.getClass();

    if (clazz == Byte.class
        || clazz == Short.class || clazz == Integer.class
        || clazz == Long.class)
      return "int";

    return "float";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param score score of a test
   * @param message result message of a test
   * @param percent true if value is a percent
   */
  public TestResult(final int score, final Number value, boolean percent) {

    if (score < -1 || score > 9)
      throw new IllegalArgumentException(
          "The score value must be between -1 and 9");

    this.score = score;
    this.message = value == null ? "" : value.toString();
    this.type = percent ? "percent" : getNumberClass(value);
  }

  /**
   * Public constructor.
   * @param score score of a test
   * @param message result message of a test
   * @param percent true if value is a percent
   */
  public TestResult(final int score, final Number value) {

    this(score, value, false);
  }

  /**
   * Public constructor.
   * @param message result message of a test
   */
  public TestResult(final Number value) {

    this(-1, value);
  }

  /**
   * Public constructor.
   * @param message result message of a test
   * @param percent true if value is a percent
   */
  public TestResult(final Number value, final boolean percent) {

    this(-1, value, percent);
  }

  /**
   * Public constructor.
   * @param score score of a test
   * @param message result message of a test
   */
  public TestResult(final int score, final String message) {

    this(score, message, "string");
  }

  /**
   * Public constructor.
   * @param message result message of a test
   */
  public TestResult(final String message) {

    this(-1, message, "string");
  }

  /**
   * Public constructor.
   * @param score score of a test
   * @param message result message of a test
   */
  public TestResult(final int score, final String message, final String type) {

    if (score < -1 || score > 9)
      throw new IllegalArgumentException(
          "The score value must be between -1 and 9");

    this.score = score;
    this.message = message == null ? "" : message;
    this.type = type;
  }

}
