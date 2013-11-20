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

package fr.ens.transcriptome.aozan.collectors.interop;

import static fr.ens.transcriptome.aozan.collectors.interop.AbstractBinaryFileReader.uShortToInt;

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

  private final int laneNumber; // uint16
  private final int tileNumber; // uint16

  private final int cycleNumber; // uint16
  private final float errorRate; // float

  private final int numberPerfectReads; // uint32
  private final int numberReadsOneError; // uint32
  private final int numberReadsTwoErrors; // uint32
  private final int numberReadsThreeErrors; // uint32
  private final int numberReadsFourErrors; // uint32

  /** Get the number lane */
  public int getLaneNumber() {
    return laneNumber;
  }

  /** Get the number tile */
  public int getTileNumber() {
    return tileNumber;
  }

  /**
   * Get the number cycle of this record
   * @return number cycle
   */
  public int getCycleNumber() {
    return cycleNumber;
  }

  /**
   * Get the rate error of this record.
   * @return rate error
   */
  public double getErrorRate() {
    return (double) errorRate;
  }

  /**
   * Get the number perfect countReads for this record.
   * @return number perfect countReads
   */
  public int getNumberPerfectReads() {
    return this.numberPerfectReads;
  }

  public int getNumberReadsOneError() {
    return this.numberReadsOneError;
  }

  public int getNumberReadsTwoErrors() {
    return this.numberReadsTwoErrors;
  }

  public int getNumberReadsThreeErrors() {
    return this.numberReadsThreeErrors;
  }

  public int getNumberReadsFourErrors() {
    return this.numberReadsFourErrors;
  }

  @Override
  public String toString() {
    return String.format("%s\t%s\t%s\t%.2f\t%s\t%s\t%s\t%s\t%s", laneNumber,
        tileNumber, cycleNumber, errorRate, numberPerfectReads,
        numberReadsOneError, numberReadsTwoErrors, numberReadsThreeErrors,
        numberReadsFourErrors);
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
