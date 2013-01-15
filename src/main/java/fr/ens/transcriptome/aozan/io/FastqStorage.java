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

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFormatException;
import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

// singleton
public final class FastqStorage {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static FastqStorage singleton = null;
  private static Map<String, File> setFastqFiles = new HashMap<String, File>();
  private static Map<String, SequenceFile> setFastqSequenceFiles =
      new HashMap<String, SequenceFile>();
  private static String tmpDir = null;

  /**
   * @param fastqFiles
   * @return SequenceFile
   * @throws AozanException fail create sequence file
   */
  public SequenceFile getFastqSequenceFile(final File[] fastqFiles)
      throws AozanException {

    if (fastqFiles.length == 0) {
      LOGGER.warning("List fastq file to uncompress and compile is empty");
      return null;
    }

    SequenceFile sequenceFile = null;
    try {
      sequenceFile = SequenceFactory.getSequenceFile(fastqFiles);

    } catch (SequenceFormatException sfe) {
      sfe.printStackTrace();
      throw new AozanException(sfe.getMessage());

    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw new AozanException(ioe.getMessage());
    }

    // Add in list of temporary fastq sequence files
    String key = keyFiles(fastqFiles);
    setFastqSequenceFiles.put(key, sequenceFile);

    return sequenceFile;
  }

  /**
   * @param fastqFiles
   * @return file compile all files of the list, compressed or none
   * @throws AozanException
   */
  public File getFastqFile(final File[] fastqFiles) throws AozanException {

    final long startTime = System.currentTimeMillis();

    if (fastqFiles.length == 0) {
      LOGGER.warning("List fastq file to uncompress and compile is empty");
      return null;
    }

    File tmpFastqFile = null;
    try {
      tmpFastqFile =
          File.createTempFile("aozan_fastq_", ".fastq", new File(tmpDir));

      OutputStream out = new FileOutputStream(tmpFastqFile);

      for (File fastqFile : fastqFiles) {

        if (!fastqFile.exists()) {
          throw new IOException("Fastq file "
              + fastqFile.getName() + " doesn't exist");
        }

        // Get compression type
        CompressionType zType =
            CompressionType.getCompressionTypeByFilename(fastqFile.getName());

        // Append compressed fastq file to uncompressed file
        final InputStream in = new FileInputStream(fastqFile);
        FileUtils.append(zType.createInputStream(in), out);
      }

      out.close();

    } catch (IOException io) {
      io.printStackTrace();
      throw new AozanException(io.getMessage());
    }

    // Add in list of temporary fastq files
    String key = keyFiles(fastqFiles);
    setFastqFiles.put(key, tmpFastqFile);

    final long endTime = System.currentTimeMillis();
    LOGGER.info("Create uncompressed for fastq File "
        + key + " in " + toTimeHumanReadable(endTime - startTime));

    return tmpFastqFile;
  }

  private String keyFiles(final File[] tab) {

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
   * @param file
   */
  public void removeTemporaryFastq(final File file) {

    if (!setFastqFiles.containsKey(file))
      LOGGER.warning("Doesn't exist temporary fastq file: "
          + file.getAbsolutePath() + " in map.");

    if (!file.exists())
      LOGGER.warning("Doesn't exist temporary fastq file: "
          + file.getAbsolutePath());

    if (!file.delete())
      LOGGER.warning("Can't delete temporary fastq file: "
          + file.getAbsolutePath());
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
   * Delete all temporaries files if exist
   * @throws IOException
   */
  public void clear() {

    for (Map.Entry<String, File> e : setFastqFiles.entrySet())

      removeTemporaryFastq(e.getValue());
  }

  public static FastqStorage getFastqStorage(final String tmpDirectory) {

    if (singleton == null) {
      tmpDir = tmpDirectory;
      singleton = new FastqStorage();
    }

    return singleton;
  }

  //
  // Constructor
  //

  private FastqStorage() {
  }

}
