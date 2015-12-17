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

package fr.ens.biologie.genomique.aozan.collectors.interop;

import static fr.ens.biologie.genomique.aozan.collectors.interop.AbstractBinaryFileReader.uShortToInt;

import java.nio.ByteBuffer;

/**
 * This internal class save a record from ErrorMetricsOut.bin file,
 * corresponding of the description of the EXPECTED_VERSION. An record contains
 * data per tile per cycle per lane. Each record create an object
 * IlluminaErrorMetrics.______________________________________________________
 * byte 0: file version number (3)____________________________________________
 * byte 1: length of each record______________________________________________
 * bytes (N * 30 + 2) - (N *30 + 11): record:_________________________________
 * __2 bytes: lane number (uint16)____________________________________________
 * __2 bytes: tile number (uint16)____________________________________________
 * __2 bytes: cycle number (uint16)___________________________________________
 * __4 bytes: error rate (float)______________________________________________
 * __4 bytes: number of perfect countReads (uint32)___________________________
 * __4 bytes: number of countReads with 1 error (uint32)______________________
 * __4 bytes: number of countReads with 2 errors (uint32)_____________________
 * __4 bytes: number of countReads with 3 errors (uint32)_____________________
 * __4 bytes: number of countReads with 4 errors (uint32)_____________________
 * Where N is the record index________________________________________________
 * @author Sandrine Perrin
 * @since 1.1
 */
public class IlluminaErrorMetrics {

  /** The lane number. */
  private final int laneNumber; // uint16

  /** The tile number. */
  private final int tileNumber; // uint16

  /** The cycle number. */
  private final int cycleNumber; // uint16

  /** The error rate. */
  private final float errorRate; // float

  /** The number perfect reads. */
  private final int numberPerfectReads; // uint32

  /** The number reads one error. */
  private final int numberReadsOneError; // uint32

  /** The number reads two errors. */
  private final int numberReadsTwoErrors; // uint32

  /** The number reads three errors. */
  private final int numberReadsThreeErrors; // uint32

  /** The number reads four errors. */
  private final int numberReadsFourErrors; // uint32

  /**
   * Get the number lane.
   * @return the lane number
   */
  public int getLaneNumber() {
    return this.laneNumber;
  }

  /**
   * Get the number tile.
   * @return the tile number
   */
  public int getTileNumber() {
    return this.tileNumber;
  }

  /**
   * Get the number cycle of this record.
   * @return number cycle
   */
  public int getCycleNumber() {
    return this.cycleNumber;
  }

  /**
   * Get the rate error of this record.
   * @return rate error
   */
  public double getErrorRate() {
    return this.errorRate;
  }

  /**
   * Get the number perfect countReads for this record.
   * @return number perfect countReads
   */
  public int getNumberPerfectReads() {
    return this.numberPerfectReads;
  }

  /**
   * Gets the number reads one error.
   * @return the number reads one error
   */
  public int getNumberReadsOneError() {
    return this.numberReadsOneError;
  }

  /**
   * Gets the number reads two errors.
   * @return the number reads two errors
   */
  public int getNumberReadsTwoErrors() {
    return this.numberReadsTwoErrors;
  }

  /**
   * Gets the number reads three errors.
   * @return the number reads three errors
   */
  public int getNumberReadsThreeErrors() {
    return this.numberReadsThreeErrors;
  }

  /**
   * Gets the number reads four errors.
   * @return the number reads four errors
   */
  public int getNumberReadsFourErrors() {
    return this.numberReadsFourErrors;
  }

  @Override
  public String toString() {
    return String.format("%s\t%s\t%s\t%.2f\t%s\t%s\t%s\t%s\t%s",
        this.laneNumber, this.tileNumber, this.cycleNumber, this.errorRate,
        this.numberPerfectReads, this.numberReadsOneError,
        this.numberReadsTwoErrors, this.numberReadsThreeErrors,
        this.numberReadsFourErrors);
  }

  //
  // Constructor
  //

  /**
   * Constructor. One record countReads on the ByteBuffer.
   * @param bb ByteBuffer who read one record
   */
  IlluminaErrorMetrics(final ByteBuffer bb) {

    this.laneNumber = uShortToInt(bb.getShort());
    this.tileNumber = uShortToInt(bb.getShort());
    this.cycleNumber = uShortToInt(bb.getShort());

    this.errorRate = bb.getFloat();
    this.numberPerfectReads = bb.getInt();

    this.numberReadsOneError = bb.getInt();
    this.numberReadsTwoErrors = bb.getInt();
    this.numberReadsThreeErrors = bb.getInt();
    this.numberReadsFourErrors = bb.getInt();

  }
}
