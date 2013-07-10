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

import fr.ens.transcriptome.aozan.AozanException;

/**
 * This class define a specified iterator for reading the binary file :
 * TileMetricsOut.bin.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class TileMetricsOutIterator extends AbstractBinaryIteratorReader {

  public final String NAME = "TileMetricsOut";

  public static final String tileMetricsOutFile = "TileMetricsOut.bin";

  private static final int EXPECTED_RECORD_SIZE = 10;
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
    return new IlluminaTileMetrics(bbIterator.next());
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @throws AozanException it occurs if size record or version aren't the same
   *           that expected.
   */

  public TileMetricsOutIterator() throws AozanException {
    super(new File(dirInterOp + tileMetricsOutFile), EXPECTED_RECORD_SIZE,
        EXPECTED_VERSION);
  }

  //
  // Internal class
  //

  /**
   * This internal class save a record from TileMetricsOut.bin file,
   * corresponding of the description of the EXPECTED_VERSION. An record
   * contains data per tile per lane per metrics.Each record create an object
   * IlluminaTileMetrics._______________________________________________________
   * byte 0: file version number (2)____________________________________________
   * byte 1: length of each record______________________________________________
   * bytes (N * 10 + 2) - (N *10 + 11): record:_________________________________
   * __2 bytes: lane number (uint16)____________________________________________
   * __2 bytes: tile number (uint16)____________________________________________
   * __2 bytes: metric code (uint16)____________________________________________
   * __4 bytes: metric value (float)____________________________________________
   * Where N is the record index and possible metric codes are:_________________
   * _____code 100: cluster density (k/mm2)_____________________________________
   * _____code 101: cluster density passing filters (k/mm2)_____________________
   * _____code 102: number of clusters__________________________________________
   * _____code 103: number of clusters passing filters__________________________
   * _____code (200 + (N – 1) * 2): phasing for read N__________________________
   * _____code (201 + (N – 1) * 2): prephasing for read N_______________________
   * _____code (300 + N – 1): percent aligned for read N________________________
   * _____code 400: control lane________________________________________________
   * @author Sandrine Perrin
   * @since 1.1
   */
  class IlluminaTileMetrics extends IlluminaMetrics {

    private final int metricCode;
    private final float metricValue;

    /**
     * Public constructor. One record reads on the ByteBuffer.
     * @param bb ByteBuffer who read one record
     */
    public IlluminaTileMetrics(final ByteBuffer bb) {
      super(bb);
      metricCode = IlluminaMetrics.uShortToInt(bb.getShort());
      metricValue = bb.getFloat();

    }

    /**
     * Get metric code for this record.
     * @return metric code
     */
    public int getMetricCode() {
      return metricCode;
    }

    /**
     * Get value for the metric code for this record.
     * @return value for the metric code
     */
    public float getMetricValue() {
      return metricValue;
    }

    @Override
    public String toString() {
      // return String.format("lane %s tile %s code %s value %.04f", laneNumber,
      // tileNumber, metricCode, metricValue);

      return laneNumber
          + "\t" + tileNumber + "\t" + metricCode + "\t" + metricValue + "\n";
    }
  }
}
