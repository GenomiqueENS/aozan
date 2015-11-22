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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define an iterator on Illumina Metrics for reading binary files
 * from the InterOp directory. It allow to parse all records.
 * @author Sandrine Perrin
 * @since 1.1
 */
abstract class AbstractBinaryFileReader<M> {

  private final File dirInterOpPath;

  // 2 bytes: 1 for file version number and 1 for length for each record
  private static final int HEADER_SIZE = 2;

  /**
   * Gets the name.
   * @return collector name
   */
  public abstract String getName();

  /**
   * Gets the metrics file.
   * @return metrics filename
   */
  protected abstract File getMetricsFile();

  /**
   * Gets the expected record size.
   * @return expected record size
   */
  protected abstract int getExpectedRecordSize();

  /**
   * Gets the expected version.
   * @return expected version of binary file
   */
  protected abstract int getExpectedVersion();

  /**
   * Gets the dir path inter op.
   * @return the dir path inter op
   */
  public File getDirPathInterOP() {
    return this.dirInterOpPath;
  }

  /**
   * Gets the sets the illumina metrics.
   * @return set Illumina metrics corresponding to one binary InterOp file
   * @throws AozanException the aozan exception
   */
  public List<M> getSetIlluminaMetrics() throws AozanException {

    final List<M> collection = new ArrayList<>();

    final ByteBuffer buf;
    final byte[] header = new byte[HEADER_SIZE];

    final byte[] element = new byte[getExpectedRecordSize()];
    final ByteBuffer recordBuf = ByteBuffer.wrap(element);
    recordBuf.order(ByteOrder.LITTLE_ENDIAN);

    try {
      FileUtils.checkExistingFile(getMetricsFile(), "Error binary file "
          + getMetricsFile().getAbsolutePath());

      final FileInputStream is = new FileInputStream(getMetricsFile());
      final FileChannel channel = is.getChannel();
      final long fileSize = channel.size();

      // Copy binary file in buffer
      buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
      buf.order(ByteOrder.LITTLE_ENDIAN);
      channel.close();
      is.close();

    } catch (final IOException e) {
      throw new AozanException(e);
    }

    // check version file
    if (HEADER_SIZE > 0) {
      ByteBuffer b = ByteBuffer.allocate(HEADER_SIZE);
      b.order(ByteOrder.LITTLE_ENDIAN);
      b = buf.get(header);
      b.position(0);

      checkVersionFile(b);
    }

    // Build collection of illumina metrics
    while (buf.limit() - buf.position() >= getExpectedRecordSize()) {
      recordBuf.position(0);
      buf.get(element);
      recordBuf.position(0);

      // collection.add(new IlluminaMetrics(recordBuf));
      addIlluminaMetricsInCollection(collection, recordBuf);
    }

    return collection;
  }

  /**
   * Build a set of a type of illumina metrics (M) according to the interop file
   * reading.
   * @param collection list of illumina metrics
   * @param bb ByteBuffer contains the value corresponding to one record
   */
  protected abstract void addIlluminaMetricsInCollection(
      final List<M> collection, final ByteBuffer bb);

  /**
   * Check version file corresponding to the implemented code
   * @param header header file.
   * @throws AozanException occurs if the checking fails
   */
  private void checkVersionFile(final ByteBuffer header) throws AozanException {

    // Get the version, should be EXPECTED_VERSION
    final int actualVersion = uByteToInt(header.get());
    if (actualVersion != getExpectedVersion()) {
      throw new AozanException(getName()
          + " expects the version number to be " + getExpectedVersion()
          + ".  Actual Version in Header( " + actualVersion + ")");
    }

    // Check the size record needed
    final int actualRecordSize = uByteToInt(header.get());
    if (getExpectedRecordSize() != actualRecordSize) {
      throw new AozanException(getName()
          + " expects the record size to be " + getExpectedRecordSize()
          + ".  Actual Record Size in Header( " + actualRecordSize + ")");
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param dirPath path to the interop directory for a run
   * @throws AozanException
   */
  AbstractBinaryFileReader(final File dirPath) throws AozanException {

    if (dirPath == null) {
      throw new AozanException("No path to the InterOp directory has been provided");
    }

    if (!dirPath.exists()) {
      throw new AozanException("Path to interOp directory doesn't exists "
          + dirPath);
    }

    this.dirInterOpPath = dirPath;
  }

  /** Convert an unsigned byte to a signed int. */
  public static final int uByteToInt(final byte unsignedByte) {
    return unsignedByte & 0xFF;
  }

  /** Convert an unsigned byte to a signed short. */
  public static final int uByteToShort(final byte unsignedByte) {
    return unsignedByte & 0xFF;
  }

  /** Convert an unsigned short to an int. */
  public static final int uShortToInt(final short unsignedShort) {
    return unsignedShort & 0xFFFF;
  }

  /** Convert an unsigned int to a long. */
  public static final long uIntToLong(final int unsignedInt) {
    return unsignedInt & 0xFFFFFFFFL;
  }

}
