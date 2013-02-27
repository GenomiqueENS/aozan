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
import fr.ens.transcriptome.aozan.collectors.FastQCCollector;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

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
  private static String compression_extension = "fq.bz2";

  // Value per default 1 for compression coefficient
  private static double coefficientUncompress = 1;

  private static Map<String, File> setFastqFiles;
  private static String tmpPath;
  private static String casavaOutputPath;

  private Map<String, FastqSample> fastqsSamples;

  private boolean controlPreCollectOK = false;

  /**
   * Return a sequenceFile. If temporary file exists, it uses SequenceFile from
   * FastqC library, else it uses a sequenceFile implemented in Aozan which
   * creates a temporary file.
   * @param fastqFiles file to treat
   * @return SequenceFile
   * @throws AozanException if an error occurs during creating sequenceFile
   */
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

  /**
   * Add a temporary fastq file in map.
   * @param keyFastqFiles key
   * @param tmpFile value
   */
  public void addTemporaryFile(final String keyFastqFiles, File tmpFile) {
    setFastqFiles.put(keyFastqFiles, tmpFile);
  }

  /**
   * Receive the type of compression use for fastq files.
   * @param casavaOutputPath
   * @throws AozanException
   */
  public void setCompressionExtension(String casavaOutputPath)
      throws AozanException {

    File[] file = new File(casavaOutputPath + "/").listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {
        return pathname.length() > 0
            && pathname.getName().contains(new StringBuffer(".fastq."));
      }
    });

    CompressionType zType =
        CompressionType.getCompressionTypeByFilename(file[0].getName());

    if (zType.equals(CompressionType.NONE))
      throw new AozanException("Compression extension unknown.");

    compression_extension = zType.getExtension();
    coefficientUncompress = zType.getCoefficientUncompress();

  }

  /**
   * Realize all preliminary control before execute AbstractFastqCollector, it
   * check if the free space in tmp directory is enough for save all
   * uncompressed fastq files.
   * @param data data used
   * @param qcReportOutputPath path to save qc report
   * @throws AozanException
   */
  public void controlPreCollect(final RunData data,
      final String qcReportOutputPath) throws AozanException {

    if (controlPreCollectOK)
      return;

    // Count size from all fastq files util
    long uncompressedSizeNeeded = countUncompressedSizeFilesNeeded(data);
    uncompressedSizeNeeded /= (1024L * 1024L * 1024L);

    // Control if free disk space is enough for uncompress fastq files
    // double sizeGo = (double) uncompressedSizeNeeded / 1024.0 / 1024.0 /
    // 1024.0;
    // System.out.println("size Compress " + sizeGo);
    // long sizeUncompress = (long) (sizeGo * 3.66);
    // System.out.println("size unCompress " + (double) sizeGo * 3.66);

    long freeSpace = new File(tmpPath).getFreeSpace();
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
    //
    // System.out
    // .println("Enough disk space to store uncompressed fastq files for step fastqScreen. We are "
    // + freeSpace
    // + " in directory "
    // + new File(tmpPath).getAbsolutePath()
    // + ", and we need "
    // + uncompressedSizeNeeded);

    // Create temporary directory who save intermediary results create by
    // AbstractFastqCollector
    System.out.println(qcReportOutputPath);

    // Verify if directory for the run exists
    File runDir = new File(qcReportOutputPath);
    if (runDir.exists() && runDir.isDirectory()) {
      FastQCCollector.isExistRunDir = true;

    } else if (!(new File(qcReportOutputPath).mkdir())) {
      // throw new AozanException(
      System.out
          .println("Error during create save directory for results intermediate of AbstractFastqCollector.");
    }
    controlPreCollectOK = true;
  }

  /**
   * Estimate the size needed for all uncompresses fastq files and construct the
   * map with all samples to treat.
   * @param data data used
   * @return
   */
  private long countUncompressedSizeFilesNeeded(final RunData data) {

    final int laneCount = data.getInt("run.info.flow.cell.lane.count");
    // mode paired or single-end present in Rundata
    final int readCount = data.getInt(KEY_READ_COUNT);

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
              new FastqSample(casavaOutputPath, read, lane, sampleName,
                  projectName, index);

          uncompressedSizeFiles += fs.getUncompressedSize();

          samples.put(fs.getKeyFastqSample(), fs);

        } // sample
      }// lane
    }// read

    // Create a unmodifiable linked map
    fastqsSamples = Collections.unmodifiableMap(samples);

    return uncompressedSizeFiles;
  }

  /**
   * Check if a temporary file corresponding with fastq files has already
   * created
   * @param fastqFiles fastq files
   * @return true if map of files contains a entry with the same key or false
   */
  public boolean tmpFileExist(final File[] fastqFiles) {
    if (fastqFiles == null || fastqFiles.length == 0)
      return false;

    return tmpFileExist(keyFiles(fastqFiles));
  }

  /**
   * Check if a temporary file corresponding with a key has already created.
   * @param keyFastqFiles key
   * @return true if temporary file exists else false
   */
  public boolean tmpFileExist(final String keyFastqFiles) {

    return setFastqFiles.containsKey(keyFastqFiles);
  }

  /**
   * Compile name of all files of array.
   * @param tab array of files
   * @return key
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

  //
  // Setter
  //

  /**
   * Define the path used for FastqStorage.
   * @param casavaOutput path of the directory contains fastq file
   * @param tmp path of the tmp directory
   */
  public void setTmpDir(final String casavaOutput, final String tmp) {
    if (singleton != null) {
      casavaOutputPath = casavaOutput;
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

  /**
   * Create a new empty temporary file.
   * @return file a new empty temporary file.
   * @throws IOException if an error occurs which creating file
   */
  public File getNewTemporaryFile() throws IOException {
    return File.createTempFile("aozan_fastq_", ".fastq", new File(tmpPath));
  }

  /**
   * Return the temporary if exists which contains all fastq files in the array.
   * @param fastqFiles array of fastq files, else null.
   * @return File temporary file or null if it not exists
   */
  public File getTemporaryFile(final File[] fastqFiles) {

    if (fastqFiles == null || fastqFiles.length == 0)
      return null;

    return getTemporaryFile(keyFiles(fastqFiles));
  }

  /**
   * Return the temporary if exists which correspond to the key.
   * @param keyFastqFiles key of a array of fastq files.
   * @return File temporary file or null if it not exists
   */
  public File getTemporaryFile(final String keyFastqFiles) {
    return setFastqFiles.get(keyFastqFiles);
  }

  //
  // Getters
  //

  public Map<String, FastqSample> getFastqsSamples() {
    return fastqsSamples;
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
