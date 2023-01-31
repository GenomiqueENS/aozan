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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors.interop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;

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
   * @param version version of the format
   * @return expected record size
   */
  protected abstract int getExpectedRecordSize(int version);

  /**
   * Gets the expected versions for the file.
   * @return a set with the expected versions of binary file
   */
  protected abstract Set<Integer> getExpectedVersions();

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
  public List<M> readMetrics() throws AozanException {

    final List<M> collection = new ArrayList<>();

    final ByteBuffer buf;
    final byte[] header = new byte[HEADER_SIZE];

    try {
      FileUtils.checkExistingFile(getMetricsFile(),
          "Error binary file " + getMetricsFile().getAbsolutePath());

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

    int version = -1;

    // check version file
    if (HEADER_SIZE > 0) {
      ByteBuffer b = ByteBuffer.allocate(HEADER_SIZE);
      b.order(ByteOrder.LITTLE_ENDIAN);
      b = buf.get(header);
      b.position(0);

      // Read byte 0: file version number
      version = uByteToInt(b.get());

      // Check version
      if (!getExpectedVersions().contains(version)) {
        throw new AozanException(getName()
            + " expects the version number to be " + getExpectedVersions()
            + ".  Actual Version in Header(" + version + ")");
      }

      // Read byte 1: length of each record
      final int recordSize = uByteToInt(b.get());

      readOptionalFlag(b, version);

      // Check the size record needed
      final int expectedRecordSize = getExpectedRecordSize(version);
      if (expectedRecordSize != recordSize) {
        throw new AozanException(getName()
            + " expects the record size to be " + expectedRecordSize
            + ". Actual Record Size in Header(" + recordSize + ")");
      }

      // checkRecordSize(recordSize);
    }

    final int recordSize = getExpectedRecordSize(version);
    final byte[] element = new byte[recordSize];
    final ByteBuffer recordBuf = ByteBuffer.wrap(element);
    recordBuf.order(ByteOrder.LITTLE_ENDIAN);

    // Build collection of illumina metrics
    while (buf.limit() - buf.position() >= recordSize) {
      recordBuf.position(0);
      buf.get(element);
      recordBuf.position(0);

      // collection.add(new IlluminaMetrics(recordBuf));
      readMetricRecord(collection, recordBuf, version);
    }

    return collection;
  }

  /**
   * Read optional flags.
   * @param bb byte buffer
   * @param version version of the format of the file
   */
  protected void readOptionalFlag(ByteBuffer bb, int version) {
  }

  /**
   * Build a set of a type of illumina metrics (M) according to the interop file
   * reading.
   * @param collection list of illumina metrics
   * @param bb ByteBuffer contains the value corresponding to one record
   * @param version version of the format
   */
  protected abstract void readMetricRecord(final List<M> collection,
      final ByteBuffer bb, int version);

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
      throw new AozanException(
          "No path to the InterOp directory has been provided");
    }

    if (!dirPath.exists()) {
      throw new AozanException(
          "Path to interOp directory doesn't exists " + dirPath);
    }

    this.dirInterOpPath = dirPath;
  }

  /** Convert an unsigned byte to a signed int. */
  public static final int uByteToInt(final ByteBuffer bb) {
    return uByteToInt(bb.get());
  }

  /** Convert an unsigned byte to a signed short. */
  public static final int uByteToShort(final ByteBuffer bb) {
    return uByteToShort(bb.get());
  }

  /** Convert an unsigned short to an int. */
  public static final int uShortToInt(final ByteBuffer bb) {
    return uShortToInt(bb.getShort());
  }

  /** Convert an unsigned int to a long. */
  public static final long uIntToLong(final ByteBuffer bb) {
    return uIntToLong(bb.getInt());
  }

  /** Convert an unsigned byte to a signed int. */
  private static final int uByteToInt(final byte unsignedByte) {
    return unsignedByte & 0xFF;
  }

  /** Convert an unsigned byte to a signed short. */
  private static final int uByteToShort(final byte unsignedByte) {
    return unsignedByte & 0xFF;
  }

  /** Convert an unsigned short to an int. */
  private static final int uShortToInt(final short unsignedShort) {
    return unsignedShort & 0xFFFF;
  }

  /** Convert an unsigned int to a long. */
  private static final long uIntToLong(final int unsignedInt) {
    return unsignedInt & 0xFFFFFFFFL;
  }

}
