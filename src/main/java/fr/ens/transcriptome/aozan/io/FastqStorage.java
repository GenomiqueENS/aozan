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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFormatException;
import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;

/**
 * This class manages the files unzipped fastq to create temporary files and
 * object sequenceFile for FastqC.
 * @author Sandrine Perrin
 */
public final class FastqStorage {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static FastqStorage singleton;

  private static String compression_extension = "fq.bz2";
  private static Map<String, File> setFastqFiles;
  private static String tmpDir;

  public SequenceFile getSequenceFile(final File[] fastqFiles)
      throws AozanException {

    final File f;
    final SequenceFile seqFile;

    String key = keyFiles(fastqFiles);

    try {

      if (setFastqFiles.containsKey(key)) {

        seqFile = SequenceFactory.getSequenceFile(fastqFiles);
        System.out.println("create sequencefile");

      } else {

        // Create temporary fastq file
        f = File.createTempFile("aozan_fastq_", ".fastq", new File(tmpDir));

        setFastqFiles.put(key, f);

        seqFile = new SequenceFileAozan(fastqFiles, f);
      }

    } catch (IOException io) {
      throw new AozanException(io.getMessage());

    } catch (SequenceFormatException e) {
      throw new AozanException(e.getMessage());
    }

    return seqFile;
  }

  public File getTemporaryFile(final File[] fastqFiles) {
    return getTemporaryFile(keyFiles(fastqFiles));
  }

  public File getTemporaryFile(final String keyFastqFiles) {
    return setFastqFiles.get(keyFastqFiles);
  }

  public void addTemporaryFile(final File[] fastqFiles, File tmpFile) {
    setFastqFiles.put(keyFiles(fastqFiles), tmpFile);
  }

  public String getTmpDir() {
    return tmpDir;
  }

  public String getCompressionExtension() {
    return this.compression_extension;
  }

  /**
   * Uncompresses and compiles files of array.
   * @param fastqFiles fastq files of array
   * @param read read number
   * @param lane lane number
   * @param projectName name of the project
   * @param sampleName name of the sample
   * @param index sequence index
   * @return file compile all files
   * @throws AozanException if an error occurs while creating file
   */
  public File getFastqFile(final String casavaOutputPath, final int read,
      final int lane, final String projectName, final String sampleName,
      final String index) throws AozanException {

    // Set the list of the files for the FASTQ data
    final File[] fastqFiles =
        createListFastqFiles(casavaOutputPath, read, lane, projectName,
            sampleName, index);

    if (fastqFiles == null || fastqFiles.length == 0)
      return null;

    // Return uncompress temporary file if it exist
    return setFastqFiles.get(keyFiles(fastqFiles));
  }

  /**
   * Test if a temporary file corresponding with projectName and sampleName has
   * already created
   * @param read read number
   * @param lane lane number
   * @param projectName name of the project
   * @param sampleName name of the sample
   * @param index sequence index
   * @return true if map of files contains a entry with the same key or false
   */
  public static boolean tmpFileExist(final File[] fastqFiles) {
    return tmpFileExist(keyFiles(fastqFiles));
  }

  public static boolean tmpFileExist(final String keyFastqFiles) {

    return setFastqFiles.containsKey(keyFastqFiles);
  }

  /**
   * Compile name of all files of array.
   * @param tab array of files
   * @return string key
   */
  public static String keyFiles(final File[] tab) {

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
   * Delete all temporaries files if exist
   * @throws IOException
   */
  public void clear() {
    System.out.println("delete temporaries files " + setFastqFiles);

    for (Map.Entry<String, File> e : setFastqFiles.entrySet())
      removeTemporaryFastq(e.getValue());
  }

  /**
   * Create a instance of fastqStorage or if it exists return instance
   * @param tmp
   * @return instance of fastqStorage
   */
  public static FastqStorage getInstance(final String tmp) {

    if (singleton == null) {
      tmpDir = tmp;
      singleton = new FastqStorage();
    }
    return singleton;
  }

  /**
   * Keep files that satisfy the specified filter in this directory and
   * beginning with this prefix
   * @param casavaOutputPath source directory
   * @return an array of abstract pathnames
   */
  public File[] createListFastqFiles(final String casavaOutputPath,
      final int read, final int lane, final String projectName,
      final String sampleName, final String index) {

    // System.out.println("storage list fastqFile "
    // + casavaOutputPath + " " + projectName + " " + sampleName + " " + index
    // + " " + lane);

    // Set the directory to the file
    final File dir =
        new File(casavaOutputPath
            + "/Project_" + projectName + "/Sample_" + sampleName);

    // Set the prefix of the file
    final String prefix =
        String.format("%s_%s_L%03d_R%d_", sampleName, "".equals(index)
            ? "NoIndex" : index, lane, read);

    // System.out.println("dir " + dir + " prefix " + prefix);

    return new File(dir + "/").listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {

        return pathname.length() > 0
            && pathname.getName().startsWith(prefix)
            && pathname.getName().endsWith(compression_extension);
      }
    });
  }

  //
  // Constructor
  //

  /**
   * Private constructor of FastqStorage
   */
  private FastqStorage() {
    setFastqFiles = new HashMap<String, File>();
  }

}
