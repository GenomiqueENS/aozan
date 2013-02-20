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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFormatException;
import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class manages the files unzipped fastq to create temporary files and
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
  private static String compression_extension = "fq.bz2";

  // Value per default 1 for uncompress file
  private static double coefficientUncompress = 1;

  private static Map<String, File> setFastqFiles;
  private static String tmpPath;
  private static String casavaOutputPath;

  private Map<String, FastqSample> fastqsSamples;

  private boolean controlPreCollectOK = false;

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
        f = File.createTempFile("aozan_fastq_", ".fastq", new File(tmpPath));
        System.out.println("create tmp fastq " + f.getName());

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

  public File getNewTemporaryFile() throws IOException {
    return File.createTempFile("aozan_fastq_", ".fastq", new File(tmpPath));
  }

  public File getTemporaryFile(final File[] fastqFiles) {

    if (fastqFiles == null || fastqFiles.length == 0)
      return null;

    return getTemporaryFile(keyFiles(fastqFiles));
  }

  // TODO create tmp file if not exists
  public File getTemporaryFile(final String keyFastqFiles) {
    return setFastqFiles.get(keyFastqFiles);
  }

  public void addTemporaryFile(final String keyFastqFiles, File tmpFile) {
    setFastqFiles.put(keyFastqFiles, tmpFile);
  }

  public String getTmpDir() {
    return tmpPath;
  }

  public double getCoefficientUncompress() {
    return coefficientUncompress;
  }

  public String getCompressionExtension() {
    return compression_extension;
  }

  public void setCompressionExtension(String casavaOutputPath)
      throws AozanException {

    File[] file = new File(casavaOutputPath + "/").listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {
        return pathname.length() > 0
            && pathname.getName().contains(new StringBuffer(".fastq."));
      }
    });

    // String nameFile = file.getName();
    // if (nameFile.indexOf(".fastq") < 0)
    // throw new AozanException("Compression extension unknown.");

    CompressionType zType =
        CompressionType.getCompressionTypeByFilename(file[0].getName());

    if (zType.equals(CompressionType.NONE))
      throw new AozanException("Compression extension unknown.");

    compression_extension = zType.getExtension();
    coefficientUncompress = zType.getCoefficientUncompress();

  }

  /**
   * @param data
   */
  public void controlPreCollect(final RunData data) throws AozanException {

    if (controlPreCollectOK)
      return;

    // Count size from all fastq files util
    long uncompressedSizeNeeded = fff(data);
    uncompressedSizeNeeded /= (1024L * 1024L * 1024L);

    // Control if free disk space is enough for uncompress fastq files
    // double sizeGo = (double) uncompressedSizeNeeded / 1024.0 / 1024.0 /
    // 1024.0;
    // System.out.println("size Compress " + sizeGo);
    // long sizeUncompress = (long) (sizeGo * 3.66);
    // System.out.println("size unCompress " + (double) sizeGo * 3.66);

    long freeSpace = new File(this.tmpPath).getFreeSpace();
    freeSpace /= (1024L * 1024L * 1024L);

    // long freeSpace2 = ProcessUtils.sh({"cd", TMP_DIR,"&","df","-h","."});
    // System.out.println("free space in /tmp " + freeSpace);
    // System.out.println("space ok " + (freeSpace > sizeUncompress));

    // if (uncompressedSizeNeeded < freeSpace)
    // throw new AozanException(
    // "Not enough disk space to store uncompressed fastq files for step fastqScreen. We are "
    // + freeSpace
    // + " in directory "
    // + new File(this.tmpPath).getAbsolutePath()
    // + ", and we need "
    // + uncompressedSizeNeeded + ". Echec Aozan");

    System.out
        .println("Enough disk space to store uncompressed fastq files for step fastqScreen. We are "
            + freeSpace
            + " in directory "
            + new File(this.tmpPath).getAbsolutePath()
            + ", and we need "
            + uncompressedSizeNeeded);

    controlPreCollectOK = true;
  }

  private long fff(final RunData data) {

    final int laneCount = data.getInt("run.info.flow.cell.lane.count");
    // mode paired or single-end present in Rundata
    final int readCount = data.getInt(KEY_READ_COUNT);
    final boolean lastReadIndexed =
        data.getBoolean(KEY_READ_X_INDEXED + readCount + ".indexed");

    long uncompressedSizeFiles = 0l;
    Map<String, FastqSample> samples = new LinkedHashMap<String, FastqSample>();

    // paired = readCount > 1 && !lastReadIndexed;

    for (int read = 1; read <= readCount - 1; read++) {

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;

      for (int lane = 1; lane <= laneCount; lane++) {

        final List<String> sampleNames =
            Lists.newArrayList(Splitter.on(',').split(
                data.get("design.lane" + lane + ".samples.names")));

        for (String sampleName : sampleNames) {

          // Get the sample index
          String index =
              data.get("design.lane" + lane + "." + sampleName + ".index");

          // Get project name
          String projectName =
              data.get("design.lane"
                  + lane + "." + sampleName + ".sample.project");

          FastqSample fs =
              new FastqSample(this.casavaOutputPath, read, lane, sampleName,
                  projectName, index);

          uncompressedSizeFiles += fs.getUncompressedSize();

          // key: identifiant uniq for a sample in lane
          samples.put(lane + "_" + sampleName, fs);

        } // sample
      }// lane
    }// read

    // Create a unmodifiable linked map
    fastqsSamples = Collections.unmodifiableMap(samples);

    System.out.println("Nb fastqSample create "
        + fastqsSamples.size() + " size " + uncompressedSizeFiles);

    return uncompressedSizeFiles;
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
  // public File getFastqFile(final String casavaOutputPath, final int read,
  // final int lane, final String projectName, final String sampleName,
  // final String index) throws AozanException {
  //
  // // Set the list of the files for the FASTQ data
  // final File[] fastqFiles =
  // createListFastqFiles(read, lane, projectName, sampleName, index);
  //
  // if (fastqFiles == null || fastqFiles.length == 0)
  // return null;
  //
  // // Return uncompress temporary file if it exist
  // return setFastqFiles.get(keyFiles(fastqFiles));
  // }

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
    if (fastqFiles == null || fastqFiles.length == 0)
      return false;

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
  public static FastqStorage getInstance() {

    if (singleton == null) {
      singleton = new FastqStorage();
    }
    return singleton;
  }

  public void setTmpDir(final String casavaOutput, final String tmp) {
    casavaOutputPath = casavaOutput;
    tmpPath = tmp;
  }

  public Map<String, FastqSample> getFastqsSamples() {
    return fastqsSamples;
  }

  /**
   * Keep files that satisfy the specified filter in this directory and
   * beginning with this prefix
   * @param casavaOutputPath source directory
   * @return an array of abstract pathnames
   */
  // public File[] createListFastqFiles_OLD(final FastqSample fastqSample) {
  //
  // return new File(dir + "/").listFiles(new FileFilter() {
  //
  // @Override
  // public boolean accept(final File pathname) {
  // return pathname.length() > 0
  //
  // && pathname.getName().startsWith(prefix)
  // && pathname.getName().endsWith(compression_extension);
  // }
  // });
  // }

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
