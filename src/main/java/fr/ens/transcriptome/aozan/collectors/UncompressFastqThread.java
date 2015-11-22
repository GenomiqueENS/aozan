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

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.aozan.io.FastqStorage;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This private class create a thread for each array of file to uncompress and
 * compile in temporary file.
 * @since 1.0
 * @author Sandrine Perrin
 */
class UncompressFastqThread extends AbstractFastqProcessThread {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private long sizeFile = 0L;

  @Override
  protected void notifyStartLogger() {

    // Nothing to log
  }

  @Override
  protected void process() throws AozanException {

    processResults();
  }

  @Override
  protected void notifyEndLogger(final String duration) {

    final long uncompressSizeFile =
        getFastqSample().getUncompressedSize() / (1024 * 1024 * 1024);

    LOGGER.fine("UNCOMPRESS fastq : for "
        + getFastqSample().getKeyFastqSample() + " "
        + getFastqSample().getFastqFiles().size()
        + " fastq file(s) in value compression "
        + getFastqSample().getCompressionType() + " in " + duration
        + " : temporary fastq file size " + this.sizeFile
        + " Go (size estimated " + uncompressSizeFile + " Go)");
  }

  /**
   * Uncompresses and compiles files of array.
   * @throws AozanException if an error occurs while creating file
   */
  @Override
  protected void processResults() throws AozanException {

    final File tmpFastqFile = new File(
        getFastqStorage().getTemporaryFile(getFastqSample()).getAbsolutePath()
            + ".tmp");

    if (!getFastqStorage().getTemporaryFile(getFastqSample()).exists()) {
      // Uncompresses and compiles files of array in new temporary files

      try {

        final OutputStream out = new FileOutputStream(tmpFastqFile);

        for (final File fastqFile : getFastqSample().getFastqFiles()) {

          if (!fastqFile.exists()) {
            throw new IOException(
                "Fastq file " + fastqFile.getName() + " doesn't exist");
          }

          // Get compression value
          final CompressionType zType = getFastqSample().getCompressionType();

          // Append compressed fastq file to uncompressed file
          final InputStream in = new FileInputStream(fastqFile);
          FileUtils.append(zType.createInputStream(in), out);
        }

        out.close();

      } catch (final IOException io) {
        throw new AozanException(io);
      }
    }

    this.sizeFile = tmpFastqFile.length();
    this.sizeFile = this.sizeFile / (1024 * 1024 * 1024);

    // Rename file for remove '.tmp' final
    if (!tmpFastqFile
        .renameTo(getFastqStorage().getTemporaryFile(getFastqSample()))) {
      LOGGER.warning("Uncompress Fastq : fail to rename tmp fastq file "
          + getFastqSample());
    }

  }

  @Override
  protected void createReportFile() throws AozanException, IOException {

  }

  //
  // Constructor
  //

  /**
   * Thread constructor.
   * @param fastqSample sample object
   * @throws AozanException if an error occurs while creating sequence file for
   *           FastQC
   */
  public UncompressFastqThread(final FastqSample fastqSample,
      final FastqStorage fastqStorage) throws AozanException {

    super(fastqSample, fastqStorage);
  }

}
