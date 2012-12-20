/*                  Aozan development code 
 * 
 * 
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

import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFile;
import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

// singleton
public final class FastqStorage {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static FastqStorage singleton = null;
  private static Map<String, SequenceFile> setFastqSequenceFiles =
      new HashMap<String, SequenceFile>();
  private static Map<String, File> setFastqFiles = new HashMap<String, File>();
  private static String tmpDir = null;

  /**
   * @param fastqFiles
   * @return SequenceFile
   */
  public SequenceFile getFastqSequenceFile(File fastqFiles) {
    return null;// tmpFastqSequenceFile;
  }

  /**
   * @param fastqFiles
   * @return file compile all files of the list, compressed or none
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
      System.out.println(io.getMessage());
      throw new AozanException(io.getMessage());
    }

    // add in list or temporary fastq files
    String key = keyFiles(fastqFiles);
    setFastqFiles.put(key, tmpFastqFile);

    final long endTime = System.currentTimeMillis();
    LOGGER.info("Create uncompressed fastq File in "
        + toTimeHumanReadable(endTime - startTime));

    System.out.println("Create uncompressed for fastq File "
        + key + ", size " + (tmpFastqFile.length() / 1048576) + " Go in "
        + toTimeHumanReadable(endTime - startTime));

    return tmpFastqFile;
  }

  private String keyFiles(File[] tab) {

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
   * delete mapOutputFile if exist
   * @throws IOException
   */
  public void clear() {

    for (Map.Entry<String, File> e : setFastqFiles.entrySet()) {
      File f = e.getValue();
      if (!f.exists())
        LOGGER.warning("Doesn't exist temporary fastq file: "
            + f.getAbsolutePath());

      if (!f.delete())
        LOGGER.warning("Can't delete temporary fastq file: "
            + f.getAbsolutePath());
    }
  }

  public static FastqStorage getFastqStorage(String tmpDirectory) {
    if (singleton == null) {
      tmpDir = tmpDirectory;
      singleton = new FastqStorage();
    }

    return singleton;
  }

  private FastqStorage() {
  }

}
