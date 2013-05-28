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

import net.sf.picard.util.UnsignedTypeUtil;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.aozan.AozanException;

/**
 * This class define a specified iterator for reading the binary file :
 * ErrorMetricsOut.bin.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ErrorMetricsOutIterator extends AbstractBinaryIteratorReader {

  public final String NAME = "ErrorMetricsOut";

  public static String errorMetricsOutFile = "ErrorMetricsOut.bin";
  private static final int EXPECTED_RECORD_SIZE = 30;
  private static final int EXPECTED_VERSION = 3;

  /**
   * Get the file name treated
   * @return file name
   */
  @Override
  public String getName() {
    return this.NAME;
  }

  /**
   * Check if the file ErrorMetricsOut.bin exists
   * @return boolean true if the file exists
   */
  public static boolean errorMetricsOutFileExists(final String dirInterOpPath) {

    return new File(dirInterOpPath + errorMetricsOutFile).exists();
  }

  public static boolean errorMetricsOutFileExists() {

    return new File(dirInterOp + errorMetricsOutFile).exists();
  }

  @Override
  public IlluminaErrorMetrics next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return new IlluminaErrorMetrics(bbIterator.next());
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @throws AozanException it occurs if size record or version aren't the same
   *           that expected.
   */

  public ErrorMetricsOutIterator() throws AozanException {

    super(new File(dirInterOp + errorMetricsOutFile), EXPECTED_RECORD_SIZE,
        EXPECTED_VERSION);

  }

  //
  // Internal class
  //

  /**
   * This internal class save a record from ErrorMetricsOut.bin file,
   * corresponding of the description of the EXPECTED_VERSION. An record
   * contains data per tile per cycle per lane. Each record create an object
   * IlluminaErrorMetrics.____________________________________________________
   * byte 0: file version number (3)____________________________________________
   * byte 1: length of each record______________________________________________
   * bytes (N * 30 + 2) - (N *30 + 11): record:_________________________________
   * __2 bytes: lane number (uint16)____________________________________________
   * __2 bytes: tile number (uint16)____________________________________________
   * __2 bytes: cycle number (uint16)___________________________________________
   * __4 bytes: error rate (float)______________________________________________
   * __4 bytes: number of perfect reads (uint32)________________________________
   * __4 bytes: number of reads with 1 error (uint32)___________________________
   * __4 bytes: number of reads with 2 errors (uint32)__________________________
   * __4 bytes: number of reads with 3 errors (uint32)__________________________
   * __4 bytes: number of reads with 4 errors (uint32)__________________________
   * Where N is the record index________________________________________________
   * @author Sandrine Perrin
   * @since 1.1
   */
  public static class IlluminaErrorMetrics extends IlluminaMetrics {

    static int maxCycle = 0;
    static int minCycle = Integer.MAX_VALUE;

    private static final Set<Integer> tilesNumberList = Sets.newHashSet();

    private final int cycleNumber; // uint16
    private final float errorRate; // float
    private final int numberPerfectReads; // uint32
    private final int numberReadsOneError; // uint32
    private final int numberReadsTwoErrors; // uint32
    private final int numberReadsThreeErrors; // uint32
    private final int numberReadsFourErrors; // uint32

    /**
     * Public constructor. One record reads on the ByteBuffer.
     * @param bb ByteBuffer who read one record
     */
    public IlluminaErrorMetrics(final ByteBuffer bb) {
      super(bb);

      tilesNumberList.add(tileNumber);

      this.cycleNumber = UnsignedTypeUtil.uShortToInt(bb.getShort());

      this.errorRate = bb.getFloat();
      this.numberPerfectReads = bb.getInt();
      this.numberReadsOneError = bb.getInt();
      this.numberReadsTwoErrors = bb.getInt();
      this.numberReadsThreeErrors = bb.getInt();
      this.numberReadsFourErrors = bb.getInt();

      maxCycle = (cycleNumber > maxCycle) ? cycleNumber : maxCycle;
      minCycle = (cycleNumber < minCycle) ? cycleNumber : minCycle;
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
    public float getErrorRate() {
      return errorRate;
    }

    /**
     * Get the number perfect reads for this record.
     * @return number perfect reads
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
  }

}
