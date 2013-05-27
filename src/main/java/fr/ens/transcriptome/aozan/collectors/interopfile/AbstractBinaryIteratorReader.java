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

package fr.ens.transcriptome.aozan.collectors.interopfile;

import static net.sf.picard.illumina.parser.readers.MMapBackedIteratorFactory.getByteBufferIterator;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import net.sf.picard.illumina.parser.readers.BinaryFileIterator;
import net.sf.picard.util.UnsignedTypeUtil;
import fr.ens.transcriptome.aozan.AozanException;

/**
 * This class define an iterator on Illumina Metrics for reading binary files
 * from the InterOp directory. It allow to parse all records.
 * @author Sandrine Perrin
 * @since 1.1
 */
public abstract class AbstractBinaryIteratorReader implements
    BinaryIteratorReader {

  public static String dirInterOp;

  // 2 bytes: 1 for file version number and 1 for length for each record
  protected final int HEADER_SIZE = 2;

  protected int expectedRecordSize;
  protected int expectedVersion;

  protected final BinaryFileIterator<ByteBuffer> bbIterator;

  abstract public String getName();

  public static void setDirectory(String directoryPath) throws AozanException {

    if (directoryPath == null)
      throw new AozanException("None path to InterOp directory provided");

    if (!new File(directoryPath).exists())
      throw new AozanException("Path to interOp directory doesn't exists "
          + directoryPath);

    dirInterOp = directoryPath;
  }

  @Override
  public boolean hasNext() {
    return bbIterator.hasNext();
  }

  @Override
  public IlluminaMetrics next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return new IlluminaMetrics(bbIterator.next());
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  //
  // Constructor
  //

  /**
   * This constructor create an iterable object on the binary file and check the
   * version of the file if it corresponds with the implementation class
   * OutReader.
   * @param metricsOutFile binary path of file from InterOp directory
   * @param expectedRecordSize size records to read
   * @param expectedVersion version of file
   * @throws AozanException it occurs if size record or version aren't good.
   */
  public AbstractBinaryIteratorReader(final File metricsOutFile,
      final int expectedRecordSize, final int expectedVersion)
      throws AozanException {

    this.expectedRecordSize = expectedRecordSize;
    this.expectedVersion = expectedVersion;

    this.bbIterator =
        getByteBufferIterator(HEADER_SIZE, expectedRecordSize, metricsOutFile);

    final ByteBuffer header = bbIterator.getHeaderBytes();

    // Get the version, should be EXPECTED_VERSION
    final int actualVersion = UnsignedTypeUtil.uByteToInt(header.get());
    if (actualVersion != expectedVersion) {
      throw new AozanException(getName()
          + " expects the version number to be " + expectedVersion
          + ".  Actual Version in Header( " + actualVersion + ")");
    }

    // Check the size record needed
    final int actualRecordSize = UnsignedTypeUtil.uByteToInt(header.get());
    if (expectedRecordSize != actualRecordSize) {
      throw new AozanException(getName()
          + " expects the record size to be " + expectedRecordSize
          + ".  Actual Record Size in Header( " + actualRecordSize + ")");
    }
  }

  //
  // Internal class
  //

  /**
   * This internal class define an object of illumina metrics with data always
   * present in records from binary file in InterOp directory.
   * @author Sandrine Perrin
   * @since 1.1
   */
  static class IlluminaMetrics {

    protected final int laneNumber; // uint16
    protected final int tileNumber; // uint16

    IlluminaMetrics(final ByteBuffer bb) {
      laneNumber = UnsignedTypeUtil.uShortToInt(bb.getShort());
      tileNumber = UnsignedTypeUtil.uShortToInt(bb.getShort());
    }

    public int getLaneNumber() {
      return laneNumber;
    }

    public int getTileNumber() {
      return tileNumber;
    }
  }
}
