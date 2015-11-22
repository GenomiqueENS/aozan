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

package fr.ens.transcriptome.aozan.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.logging.Logger;

import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;
import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;

/**
 * This class manages the files uncompressed fastq to create temporary files and
 * object sequenceFile for FastqC.
 * @since 1.0
 * @author Sandrine Perrin
 */
public final class FastqStorage {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private final File tmpDir;

  /**
   * Return a sequenceFile for all fastq files present to treat in the sample.
   * If the temporary file doesn't existed, it is created.
   * @param fastqSample sample to treat
   * @return SequenceFile an structure which allow to browse a fastq file
   *         sequence per sequence
   * @throws AozanException if an error occurs during writing file
   */
  public SequenceFile getSequenceFile(final FastqSample fastqSample)
      throws AozanException {

    final File[] fastq = fastqSample.getFastqFiles().toArray(new File[0]);
    final SequenceFile seqFile;

    try {

      if (tmpFileExists(fastqSample)) {
        seqFile = SequenceFactory.getSequenceFile(fastq);

      } else {
        // Create temporary fastq file
        final File tmpFile = new File(getTemporaryFile(fastqSample) + ".tmp");
        seqFile = new AozanSequenceFile(fastq, tmpFile, fastqSample, this);

      }

    } catch (final IOException io) {
      throw new AozanException(io);

    } catch (final SequenceFormatException e) {
      throw new AozanException(e);
    }

    return seqFile;
  }

  /**
   * Check if a temporary file corresponding with fastq files has already
   * created.
   * @param fastqSample fastq instance which describe an sample
   * @return true if map of files contains a entry with the same key or false
   */
  public boolean tmpFileExists(final FastqSample fastqSample) {

    if (fastqSample.getFastqFiles().isEmpty()) {
      return false;
    }

    return getTemporaryFile(fastqSample).exists();

  }

  /**
   * Delete all temporaries files (fastq tmp files and map files).
   * @throws IOException
   */
  public void clear() {
    LOGGER.info("Delete temporaries fastq and map files");

    final File[] files = this.tmpDir.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {
        return (pathname.getName().startsWith("aozan_fastq_")
            && (pathname.getName().endsWith(".fastq")
                || pathname.getName().endsWith(".fastq.tmp")))
            || (pathname.getName().startsWith("map-")
                && (pathname.getName().endsWith(".txt")));
      }
    });

    // Delete temporary files
    for (final File f : files) {
      if (f.exists()) {
        if (!f.delete()) {
          LOGGER.warning(
              "Can not delete the temporary file : " + f.getAbsolutePath());
        }
      }
    }
  }

  /**
   * Return the temporary if exists which correspond to the key.
   * @param fastqSample fastq instance which describe an sample
   * @return File temporary file or null if it not exists
   */
  public File getTemporaryFile(final FastqSample fastqSample) {

    return new File(this.tmpDir, fastqSample.getNameTemporaryFastqFiles());
  }

  /**
   * Get absolute path of the temporary directory.
   * @return path of the temporary directory
   */
  public File getTmpDir() {
    return this.tmpDir;
  }

  //
  // Constructor
  //

  /**
   * Constructor of FastqStorage.
   * @param tmpDir temporary directory
   * @throws AozanException if the temporary directory does not exists
   */
  public FastqStorage(final File tmpDir) throws AozanException {

    if (tmpDir == null) {
      throw new NullPointerException("tmpDir argument cannot be null");
    }

    if (!tmpDir.isDirectory()) {
      throw new AozanException(
          "The temprary directory does not exists or is not a directory: "
              + tmpDir);
    }

    if (tmpDir.canWrite()) {
      throw new AozanException(
          "The temprary directory is not writtable: " + tmpDir);
    }

    this.tmpDir = tmpDir;
  }

}
