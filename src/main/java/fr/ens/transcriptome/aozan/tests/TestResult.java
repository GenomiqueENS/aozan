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

  private int score;
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

  //
  // Constructor
  //

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
  }

  /**
   * Public constructor.
   * @param message result message of a test
   */
  public TestResult(final String message) {

    this.score = -1;
    this.message = message;
  }

}
