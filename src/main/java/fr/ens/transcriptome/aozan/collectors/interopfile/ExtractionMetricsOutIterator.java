/*
 *                 Aozan development code
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

package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.aozan.AozanException;

/**
 * This class define a specified iterator for reading the binary file :
 * ExtractionMetricsOut.bin.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ExtractionMetricsOutIterator extends AbstractBinaryIteratorReader {

  public final String NAME = "ExtractionMetricsOut";

  public static final String extractMetricsOutFile = "ExtractionMetricsOut.bin";

  private static final int EXPECTED_RECORD_SIZE = 38;
  private static final int EXPECTED_VERSION = 2;

  /**
   * Get the file name treated
   * @return file name
   */
  @Override
  public String getName() {
    return this.NAME;
  }

  @Override
  public IlluminaMetrics next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return new IlluminaIntensitiesMetrics(bbIterator.next());
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @throws AozanException it occurs if size record or version aren't the same
   *           that expected.
   */
  public ExtractionMetricsOutIterator(final String dirInterOpPath)
      throws AozanException {

    super(new File(dirInterOpPath + extractMetricsOutFile),
        EXPECTED_RECORD_SIZE, EXPECTED_VERSION);
  }

  public ExtractionMetricsOutIterator() throws AozanException {

    super(new File(dirInterOp + extractMetricsOutFile), EXPECTED_RECORD_SIZE,
        EXPECTED_VERSION);
  }

  //
  // Internal Class
  //

  /**
   * This internal class save a record from ExtractionMetricsOut.bin file,
   * corresponding of the description of the EXPECTED_VERSION. An record
   * contains data per tile per cycle per lane. Each record create an object____
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
  public static class IlluminaIntensitiesMetrics extends IlluminaMetrics {

    static int maxCycle = 0;
    static int minCycle = Integer.MAX_VALUE;

    static final int BASE_A = 0;
    static final int BASE_C = 1;
    static final int BASE_G = 2;
    static final int BASE_T = 3;

    private static final Set<Integer> tilesNumberList = Sets.newHashSet();

    private final int cycleNumber; // uint16
    private final float[] fwhm = new float[4]; // A C G T - float
    private final int[] intensities = new int[4]; // A C G T - uint16

    /**
     * Public constructor. One record reads on the ByteBuffer.
     * @param bb ByteBuffer who read one record
     */
    public IlluminaIntensitiesMetrics(final ByteBuffer bb) {
      super(bb);

      tilesNumberList.add(tileNumber);

      this.cycleNumber = uShortToInt(bb.getShort());

      for (int i = 0; i < 4; i++) {
        this.fwhm[i] = bb.getFloat();
      }

      for (int i = 0; i < 4; i++) {
        this.intensities[i] = uShortToInt(bb.getShort());
      }

      // Read date/time for CIF creation, not used
      ByteBuffer dateCif = bb.get(new byte[8]);

      maxCycle = (cycleNumber > maxCycle) ? cycleNumber : maxCycle;
      minCycle = (cycleNumber < minCycle) ? cycleNumber : minCycle;

    }

    /**
     * Get the number cycle of this record
     * @return number cycle
     */
    public int getCycleNumber() {
      return cycleNumber;
    }

    /**
     * Get a float array with the fwhm (full width at half maximum) scores of
     * each base (A, C, G, T)
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
     * Get set of number tiles used in ErrorMetricsOut.bin file, for the
     * records.
     * @return set of number tiles used
     */
    public static Set<Integer> getTilesNumberList() {
      return tilesNumberList;
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
  }
}
