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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.NoSuchElementException;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

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
  protected static final int HEADER_SIZE = 2;

  protected int expectedRecordSize;
  protected int expectedVersion;

  protected final BinaryFileIterator bbIterator;

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

  private BinaryFileIterator getByteBufferIterator(
      final int expectedRecordSize, final File metricsOutFile)
      throws AozanException {

    final ByteBuffer buf;
    final byte[] header = new byte[HEADER_SIZE];

    try {
      FileUtils.checkExistingFile(metricsOutFile, "");

      final FileInputStream is = new FileInputStream(metricsOutFile);
      final FileChannel channel = is.getChannel();
      final long fileSize = channel.size();

      buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
      buf.order(ByteOrder.LITTLE_ENDIAN);
      channel.close();
      is.close();

    } catch (IOException e) {
      throw new AozanException(e.getMessage());
    }

    if (HEADER_SIZE > 0)
      buf.get(header);

    return new BinaryFileIterator(header, metricsOutFile, expectedRecordSize,
        buf);
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

    this.bbIterator = getByteBufferIterator(expectedRecordSize, metricsOutFile);

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
   * Copy class from Picard
   * @author Sandrine Perrin
   * @since 1.1
   */
  class BinaryFileIterator implements Iterator<ByteBuffer>,
      Iterable<ByteBuffer> {

    protected final File file;
    protected final long fileSize;
    protected final int elementSize;
    private final byte[] header;
    private final ByteBuffer buffer;
    private byte[] localBacking;
    private ByteBuffer localBuffer;

    public ByteBuffer getHeaderBytes() {
      final ByteBuffer bb = ByteBuffer.allocate(header.length);

      bb.order(ByteOrder.LITTLE_ENDIAN);
      bb.put(header);
      bb.position(0);

      return bb;
    }

    @Override
    public ByteBuffer next() {
      if (!hasNext())
        throw new NoSuchElementException();

      return getElement();
    }

    @Override
    public Iterator<ByteBuffer> iterator() {
      return this;
    }

    public boolean hasNext() {
      return buffer.limit() - buffer.position() >= elementSize;
    }

    public ByteBuffer getElement() {
      this.localBuffer.position(0);
      this.buffer.get(this.localBacking);
      this.localBuffer.position(0);

      return localBuffer;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    //
    // Getter
    //

    public int getElementSize() {
      return this.elementSize;
    }

    public File getFile() {
      return this.file;
    }

    //
    // Constuctor
    //

    /**
     * Public contructor.
     * @param header byte array for header
     * @param file binary file
     * @param elementSize size of element to read
     * @param buffer byteBuffer to read the file
     */
    public BinaryFileIterator(final byte[] header, final File file,
        final int elementSize, final ByteBuffer buffer) {
      this.header = header;
      this.file = file;
      this.elementSize = elementSize;
      this.fileSize = file.length();
      this.buffer = buffer;

      this.localBacking = new byte[elementSize];
      this.localBuffer = ByteBuffer.wrap(localBacking);
      this.localBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

  }

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

    /** Get the number lane */
    public int getLaneNumber() {
      return laneNumber;
    }

    /** Get the number tile */
    public int getTileNumber() {
      return tileNumber;
    }
  }
}
