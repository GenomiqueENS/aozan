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
 * This internal class save a record from ExtractionMetricsOut.bin file,
 * corresponding of the description of the EXPECTED_VERSION. An record contains
 * data per tile per cycle per lane. Each record create an object_____________
 * byte 0: file version number (2)____________________________________________
 * byte 1: length of each record______________________________________________
 * bytes (N * 38 + 2) - (N *38 + 39): record:_________________________________
 * __2 bytes: lane number (uint16)____________________________________________
 * __2 bytes: tile number (uint16)____________________________________________
 * __2 bytes: cycle number (uint16)___________________________________________
 * __4 x 4 bytes: fwhm scores (float) for channel [A, C, G, T] respectively___
 * __2 x 4 bytes: intensities (uint16) for channel [A, C, G, T] respectively__
 * __8 bytes: date/time of CIF creation_______________________________________
 * Where N is the record index________________________________________________
 * @author Sandrine Perrin
 * @since 1.1
 */
public class IlluminaIntensityMetrics {

  static final int BASE_A = 0;
  static final int BASE_C = 1;
  static final int BASE_G = 2;
  static final int BASE_T = 3;

  private final int laneNumber; // uint16
  private final int tileNumber; // uint16

  private final int cycleNumber; // uint16
  private final float[] fwhm = new float[4]; // A C G T - float
  private final int[] intensities = new int[4]; // A C G T - uint16

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
   * Get a float array with the fwhm (full width at half maximum) scores of each
   * base (A, C, G, T)
   * @return float array with the fwhm scores of each channel
   */
  public float[] getFwhm() {
    return fwhm;
  }

  /**
   * Get a integer array with the raw intensities of each base (A, C, G, T)
   * @return float array with the raw intensities of each channel
   */
  public int[] getIntensities() {
    return intensities;
  }

  /**
   * Get the average of the four intensities (one per channel) for this record
   * @return average of the four intensities (one per channel)
   */
  public int getAverageIntensities() {
    int sum = 0;
    for (int n = 0; n < intensities.length; n++) {
      sum += intensities[n];
    }
    return sum / intensities.length;
  }

  //
  // Constructor
  //

  /**
   * Constructor. One record countReads on the ByteBuffer.
   * @param bb ByteBuffer who read one record
   */
  IlluminaIntensityMetrics(final ByteBuffer bb) {

    this.laneNumber = uShortToInt(bb.getShort());
    this.tileNumber = uShortToInt(bb.getShort());
    this.cycleNumber = uShortToInt(bb.getShort());

    for (int i = 0; i < 4; i++) {
      this.fwhm[i] = bb.getFloat();
    }

    for (int i = 0; i < 4; i++) {
      this.intensities[i] = uShortToInt(bb.getShort());
    }

    // Read date/time for CIF creation, not used
    // TODO get date create cif file to finalize
    ByteBuffer buf = bb.get(new byte[8]);

  }
}
