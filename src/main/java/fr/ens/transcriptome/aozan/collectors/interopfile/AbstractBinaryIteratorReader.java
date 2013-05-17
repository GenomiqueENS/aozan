package fr.ens.transcriptome.aozan.collectors.interopfile;

import static net.sf.picard.illumina.parser.readers.MMapBackedIteratorFactory.getByteBufferIterator;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import fr.ens.transcriptome.aozan.AozanException;

import net.sf.picard.PicardException;
import net.sf.picard.illumina.parser.readers.BinaryFileIterator;
import net.sf.picard.util.UnsignedTypeUtil;

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

  public AbstractBinaryIteratorReader(final File metricsOutFile,
      final int expectedRecordSize, final int expectedVersion) {

    this.expectedRecordSize = expectedRecordSize;
    this.expectedVersion = expectedVersion;

    bbIterator =
        getByteBufferIterator(HEADER_SIZE, expectedRecordSize, metricsOutFile);

    final ByteBuffer header = bbIterator.getHeaderBytes();

    // Get the version, should be EXPECTED_VERSION, which is 2
    final int actualVersion = UnsignedTypeUtil.uByteToInt(header.get());
    if (actualVersion != expectedVersion) {
      throw new PicardException(getName()
          + " expects the version number to be " + expectedVersion
          + ".  Actual Version in Header( " + actualVersion + ")");
    }

    final int actualRecordSize = UnsignedTypeUtil.uByteToInt(header.get());
    if (expectedRecordSize != actualRecordSize) {
      throw new PicardException(getName()
          + " expects the record size to be " + expectedRecordSize
          + ".  Actual Record Size in Header( " + actualRecordSize + ")");
    }
  }

  public boolean hasNext() {
    return bbIterator.hasNext();
  }

  public IlluminaMetrics next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return new IlluminaMetrics(bbIterator.next());
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  //
  // Internal class
  //

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
