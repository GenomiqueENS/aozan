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
import java.util.List;
import java.util.logging.Logger;

import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFormatException;
import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;

/**
 * This class manages the files uncompressed fastq to create temporary files and
 * object sequenceFile for FastqC.
 * @author Sandrine Perrin
 */
public final class FastqStorage {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static FastqStorage singleton;

  public static final String KEY_READ_COUNT = "run.info.read.count";
  public static final String KEY_READ_X_INDEXED = "run.info.read";

  // Default value, redefine with setCompressionExtension
  // private static String compression_extension = "fq.bz2";

  // private static Map<String, File> setFastqFiles;
  private static String tmpPath;

  /**
   * Return a sequenceFile. If temporary file exists, it uses SequenceFile from
   * FastqC library, else it uses a sequenceFile implemented in Aozan which
   * creates a temporary file.
   * @param fastqFiles file to treat
   * @return SequenceFile
   * @throws AozanException if an error occurs during creating sequenceFile
   */
  public SequenceFile getSequenceFile(final FastqSample fastqSample)
      throws AozanException {

    final File[] fastq = (File[]) fastqSample.getFastqFiles().toArray();

    final SequenceFile seqFile;

    try {

      if (tmpFileExists(fastqSample)) {
        seqFile = SequenceFactory.getSequenceFile(fastq);

      } else {
        // Create temporary fastq file
        final File f = new File(getTemporaryFile(fastqSample) + ".tmp");
        seqFile = new SequenceFileAozan(fastq, f, fastqSample);

      }

    } catch (IOException io) {
      throw new AozanException(io.getMessage());

    } catch (SequenceFormatException e) {
      throw new AozanException(e.getMessage());
    }

    return seqFile;
  }

  /**
   * Check if a temporary file corresponding with fastq files has already
   * created
   * @param fastqFiles fastq files
   * @return true if map of files contains a entry with the same key or false
   */
  public boolean tmpFileExists(final FastqSample fastqSample) {

    if (fastqSample.getFastqFiles().isEmpty())
      return false;

    return new File(getTemporaryFile(fastqSample)).exists();

  }

  /**
   * Compile name of all files of array.
   * @param tab array of files
   * @return key
   */
  public static String keyFiles_old(final List<File> tab) {

    StringBuilder key = new StringBuilder();
    String separator = "\t";

    for (File f : tab) {
      key.append(f.getName());
      key.append(separator);
    }
    int end = key.length() - separator.length();

    return key.toString().substring(0, end);

  }

  /**
   * Remove specific temporary file
   * @param file to remove
   */
  public void removeTemporaryFastq(final File file) {

    if (file.exists()) {

      if (!file.delete())
        LOGGER.warning("Can't delete temporary fastq file: "
            + file.getAbsolutePath());
    }
  }

  /**
   * Remove specific of pair of temporaries files
   * @param file1
   * @param file2
   */
  public void removeTemporaryFastq(final File file1, final File file2) {
    removeTemporaryFastq(file1);

    if (file2 != null)
      removeTemporaryFastq(file2);
  }

  /**
   * Delete all temporaries files
   * @throws IOException
   */
  public void clear() {
    System.out.println("delete temporaries files ");
    File[] files = new File(tmpPath).listFiles(new FileFilter() {

      public boolean accept(final File pathname) {
        return pathname.getName().startsWith("aozan_fastq_")
            && (pathname.getName().endsWith(".fastq") || pathname.getName()
                .endsWith(".fastq.tmp"));
      }
    });

    // Delete temporary files
    for (File f : files) {
      if (f.exists())
        if (!f.delete())
          LOGGER.warning("Can not delete the temporary file : "
              + f.getAbsolutePath());
    }
  }

  /**
   * Return the temporary if exists which correspond to the key.
   * @param keyFastqFiles key of a array of fastq files.
   * @return File temporary file or null if it not exists
   */
  public String getTemporaryFile(final FastqSample fastqSample) {

    return tmpPath + "/" + fastqSample.getNameTemporaryFastqFiles();
  }

  //
  // Setter
  //

  /**
   * Define the path used for FastqStorage.
   * @param casavaOutput path of the directory contains fastq file
   * @param tmp path of the tmp directory
   */
  public void setTmpDir(final String tmp) {
    if (singleton != null) {
      tmpPath = tmp;
    }
  }

  //
  // Getter
  //

  /**
   * Create a instance of fastqStorage or if it exists return instance
   * @return instance of fastqStorage
   */
  public static FastqStorage getInstance() {

    if (singleton == null) {
      singleton = new FastqStorage();
    }
    return singleton;
  }

  public String getTmpDir() {
    return tmpPath;
  }

  //
  // Constructor
  //

  /**
   * Private constructor of FastqStorage
   */
  private FastqStorage() {
  }

}
