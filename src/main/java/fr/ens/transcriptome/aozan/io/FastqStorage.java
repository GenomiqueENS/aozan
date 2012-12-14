/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFile;
import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

// singleton
public final class FastqStorage {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static FastqStorage singleton = null;
  private static SequenceFile tmpFastqSequenceFile = null;
  private static File tmpFastqFile = null;
  private static String tmpDir = null;
  
  final static long startTime = System.currentTimeMillis();
  
  /**
   * @param fastqFiles
   * @return SequenceFile
   */
  public static SequenceFile getFastqSequenceFile(File fastqFiles) {
    return tmpFastqSequenceFile;
  }

  /**
   * @param fastqFiles
   * @return file compile all files of the list
   */
  public static File getFastqFile(File[] fastqFiles) throws AozanException {
       
    if (fastqFiles.length == 0){
      LOGGER.warning("list fastq file to uncompress and compile is empty");
      return null;
    }
    try {
      tmpFastqFile =
          File.createTempFile("aozan_fastq_", ".fastq", new File("tmpDir"));

      OutputStream out = new FileOutputStream(tmpFastqFile);

      for (File fastqFile : fastqFiles) {

        if (!fastqFile.exists()) {
          throw new IOException("file "
              + fastqFile.getName() + " doesn't exist");
        }

        String nameFastqFile = fastqFile.getName();
        CompressionType zType =
            CompressionType.getCompressionTypeByFilename(nameFastqFile);

        if (zType.equals(CompressionType.BZIP2)) {

          ArArchiveInputStream dumpIs =
              new ArArchiveInputStream(new FileInputStream(fastqFile));
          DumpArchiveEntry entry =
              new DumpArchiveEntry(fastqFile.getCanonicalPath(), nameFastqFile);

          if (!dumpIs.canReadEntryData(entry)) {
            dumpIs.close();
            System.out.println("File " + fastqFile + "unreadable");
            throw new IOException("File " + fastqFile + "unreadable");
          }
          BufferedInputStream bis =
              new BufferedInputStream(new FileInputStream(fastqFile));

          BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(bis);
          System.out.println("file " + nameFastqFile);
          IOUtils.copy(bzIn, out);

          bis.close();
          bzIn.close();
          dumpIs.close();

        } else if (nameFastqFile.endsWith(".fastq")
            || nameFastqFile.endsWith(".fq")) {
          InputStream is = new FileInputStream(fastqFile);

          IOUtils.copy(is, out);
          is.close();
        }
      }//
      out.close();

    } catch (IOException io) {
      io.printStackTrace();
      System.out.println(io.getMessage());
      throw new AozanException(io.getMessage());
    }

    final long endTime = System.currentTimeMillis();
    System.out.println("time for create fastq File "+(endTime - startTime));

    return tmpFastqFile;
  }

  /**
   * delete mapOutputFile if exist
   * @throws IOException
   */
  public static void clear() {

      if (tmpFastqFile.exists())
        if (!tmpFastqFile.delete())
        LOGGER.warning("Can't delete temporary fastq file: "
            + tmpFastqFile.getAbsolutePath());
  }

  public static FastqStorage getFastqStorage(String tmpDir) {
    if (singleton == null) {
      singleton = new FastqStorage(tmpDir);
    }

    return singleton;
  }

  private FastqStorage(String tmpDir) {
    this.tmpDir = tmpDir; 
  }

}
