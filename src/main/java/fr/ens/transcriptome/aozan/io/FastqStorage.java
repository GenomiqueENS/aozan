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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import uk.ac.bbsrc.babraham.FastQC.Sequence.Sequence;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFormatException;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class manages the files unzipped fastq to create temporary files and
 * object sequenceFile for FastqC.
 * @author Sandrine Perrin
 */
public final class FastqStorage {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final int CHECKING_DELAY_MS = 5000;
  private static final int WAIT_SHUTDOWN_MINUTES = 60;
  private static int threads = Runtime.getRuntime().availableProcessors();
  private static final String COMPRESSION_EXTENSION = "fq.bz2";

  private static FastqStorage singleton = null;

  private static final String KEY_NUMBER_THREAD = "qc.conf.fastqc.threads";
  private static final String KEY_TMP_DIR = "tmp.dir";

  private static Map<String, File> setFastqFiles = new HashMap<String, File>();
  private static String tmpDir = null;

  /**
   * Return file uncompressed corresponding on projectName and sampleName if it
   * exist, else it is created
   * @param read read number
   * @param lane lane number
   * @param projectName name of the project
   * @param sampleName name of the sample
   * @param index sequence index
   * @return file
   * @throws AozanException if an error occurs while creating file
   */
  public File getFastqFile(final String casavaOutputPath, final int read,
      final int lane, final String projectName, final String sampleName,
      final String index) throws AozanException {

    String key = keyFiles(read, lane, projectName, sampleName);

    // Return uncompress temporary file if it exist
    if (setFastqFiles.containsKey(key))
      return setFastqFiles.get(key);

    // Set the list of the files for the FASTQ data
    final File[] fastqFiles =
        createListFastqFiles(casavaOutputPath, read, lane, projectName,
            sampleName, index);

    // No file passing the filter file
    if (fastqFiles == null || fastqFiles.length == 0) {
      return null;
    }

    return getFastqFile(fastqFiles, read, lane, projectName, sampleName);
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
  public static File getFastqFile(final File[] fastqFiles, final int read,
      final int lane, final String projectName, final String sampleName)
      throws AozanException {

    if (fastqFiles == null || fastqFiles.length == 0) {
      LOGGER.warning("List fastq file to uncompress and compile is empty");
      return null;
    }

    String key = keyFiles(read, lane, projectName, sampleName);

    // Return uncompress temporary file if it exist
    if (setFastqFiles.containsKey(key))
      return setFastqFiles.get(key);

    final long startTime = System.currentTimeMillis();
    LOGGER.fine("Start uncompressed fastq Files.");

    // Uncompresses and compiles files of array in new temporary files
    File tmpFastqFile = null;

    try {

      tmpFastqFile = createTmpFile(read, lane, projectName, sampleName);

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
      throw new AozanException(io.getMessage());
    }

    // Add in list of temporary fastq files
    setFastqFiles.put(key, tmpFastqFile);

    double sizeFile = (double) (((tmpFastqFile.length() / 1024) / 1024) / 1024);
    sizeFile = ((int) (sizeFile * 10.0)) / 10.0;

    LOGGER.fine("End uncompressed for fastq File "
        + key + "(size : " + sizeFile + " Gio) in "
        + toTimeHumanReadable(System.currentTimeMillis() - startTime));

    return tmpFastqFile;
  }

  public SequenceFile getSequenceFile(final File[] fastqFiles, final int read,
      final int lane, final String projectName, final String sampleName)
      throws AozanException {

    FileWriter outTmpFile;
    File f;
    SequenceFile seqFile ;

    try {

      // Create temporary fastq file
      f = createTmpFile(read, lane, projectName, sampleName);
      outTmpFile = new FileWriter(f);

      seqFile = SequenceFactory.getSequenceFile(fastqFiles);

    } catch (IOException io) {
      throw new AozanException(io.getMessage());
      
    } catch (SequenceFormatException sfe) {
      throw new AozanException(sfe.getMessage());
    }

    return seqFile;
  }

  public Sequence nextSequenceFile(final SequenceFile seqFile) {
    
    return null;
  }

  /**
   * Create a empty temporary file correspond to projectName and sampleName if
   * it doesn't exist
   * @param read read number
   * @param lane lane number
   * @param projectName name of the project
   * @param sampleName name of the sample
   * @param index sequence index
   * @return temporary file
   * @throws AozanException if an error occurs while creating file or if
   *           temporary file exist
   */
  public static File createTmpFile(final int read, final int lane,
      final String projectName, final String sampleName) throws AozanException {

    if (tmpFileExist(read, lane, projectName, sampleName))
      throw new AozanException("Create empty temporary file for "
          + projectName + " " + sampleName
          + " is impossible, it already exists");

    File f;
    try {
      f = File.createTempFile("aozan_fastq_", ".fastq", new File(tmpDir));
    } catch (IOException io) {
      throw new AozanException(io.getMessage());
    }
    setFastqFiles.put(keyFiles(read, lane, projectName, sampleName), f);

    return f;
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
  public static boolean tmpFileExist(final int read, final int lane,
      final String projectName, final String sampleName) {

    return setFastqFiles.containsKey(keyFiles(read, lane, projectName,
        sampleName));
  }

  /**
   * Create a key with read, lane, projectName and sampleName for adding a new
   * temporary file in map
   * @param read read number
   * @param lane lane number
   * @param projectName name of the project
   * @param sampleName name of the sample
   * @return string
   */
  private static String keyFiles(final int read, final int lane,
      final String projectName, final String sampleName) {

    return read + "\t" + lane + "\t" + projectName + "\t" + sampleName;
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
    System.out.println(setFastqFiles);

    for (Map.Entry<String, File> e : setFastqFiles.entrySet())
      removeTemporaryFastq(e.getValue());
  }

  /**
   * Create a instance of fastqStorage or if it exists return instance
   * @param tmp
   * @return instance of fastqStorage
   */
  public static FastqStorage getFastqStorage(final String tmp) {

    if (singleton == null) {
      tmpDir = tmp;
      singleton = new FastqStorage();
    }
    return singleton;
  }

  /**
   * Create a instance of fastqStorage or if it exists return instance
   * @param properties
   * @return instance of fastqStorage
   */
  public static FastqStorage getFastqStorage(final Properties properties) {

    if (singleton == null) {
      tmpDir = properties.getProperty(KEY_TMP_DIR);

      if (properties.containsKey(KEY_TMP_DIR)) {
        try {
          int confThreads =
              Integer.parseInt(properties.getProperty(KEY_NUMBER_THREAD));
          if (confThreads > 0)
            threads = confThreads;
        } catch (Exception e) {
        }
      }

      singleton = new FastqStorage();
    }

    return singleton;
  }

  /**
   * Uncompress fastq files from each sample in temporaries files
   * @param data
   * @param casavaOutputPath fastq file path
   * @param compressionExtension extension of file
   * @throws AozanException if an error occurs while creating thread
   */
  public void uncompressFastqFiles(final RunData data,
      final String casavaOutputPath) throws AozanException {

    LOGGER
        .fine("Start uncompressed all fastq Files before execute fastqscreen.");

    final long startTime = System.currentTimeMillis();

    System.out
        .println("Start uncompressed all fastq Files before execute fastqscreen.");

    // Create the list for threads
    final List<UncompressFileThread> threads = Lists.newArrayList();
    final List<Future<UncompressFileThread>> futureThreads =
        Lists.newArrayList();

    // Create executor service
    final ExecutorService executor = Executors.newFixedThreadPool(this.threads);

    final int readCount = data.getInt("run.info.read.count");
    final int laneCount = data.getInt("run.info.flow.cell.lane.count");
    int readSample = 0;

    // Create the thread and add them to the list
    for (int read = 1; read <= readCount; read++) {

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;

      readSample++;

      for (int lane = 1; lane <= laneCount; lane++) {

        final List<String> sampleNames =
            Lists.newArrayList(Splitter.on(',').split(
                data.get("design.lane" + lane + ".samples.names")));

        for (String sampleName : sampleNames) {

          // Get project name
          final String projectName =
              data.get("design.lane"
                  + lane + "." + sampleName + ".sample.project");

          // Get the sample index
          final String index =
              data.get("design.lane" + lane + "." + sampleName + ".index");

          // Process sample FASTQ(s)
          final UncompressFileThread sft =
              processFile(data, casavaOutputPath, projectName, sampleName,
                  index, lane, readSample, COMPRESSION_EXTENSION);

          if (sft != null) {
            threads.add(sft);
            futureThreads.add(executor.submit(sft, sft));
          }
        }
      }
    }

    // Wait for threads
    waitThreads(futureThreads, executor);

    System.out.println("End uncompressed "
        + setFastqFiles.size() + " fastq files before execute fastqscreen in "
        + toTimeHumanReadable(System.currentTimeMillis() - startTime));

    LOGGER.fine("End uncompressed "
        + setFastqFiles.size() + " fastq Files before execute fastqscreen in "
        + toTimeHumanReadable(System.currentTimeMillis() - startTime));

  }

  /**
   * Process a FASTQ file.
   * @param data Run data
   * @param projectName name of the project
   * @param sampleName name of the sample
   * @param index sequence index
   * @param lane lane number
   * @param read read number
   * @throws AozanException if an error occurs while processing a FASTQ file
   */
  public UncompressFileThread processFile(final RunData data,
      final String casavaOutputPath, final String projectName,
      final String sampleName, final String index, final int lane,
      final int read, final String compressionExtension) throws AozanException {

    // Control the fastq files have been treated, if true return null
    String key = keyFiles(read, lane, projectName, sampleName);
    if (setFastqFiles.containsKey(key))
      return null;

    // Set the list of the files for the FASTQ data
    final File[] fastqFiles =
        createListFastqFiles(casavaOutputPath, read, lane, projectName,
            sampleName, index);

    if (fastqFiles == null || fastqFiles.length == 0) {
      return null;
    }

    // Create the thread object
    return new UncompressFileThread(fastqFiles, read, lane, projectName,
        sampleName, key);
  }

  /**
   * Wait the end of the threads.
   * @param threads list with the threads
   * @param executor the executor
   * @throws AozanException if an error occurs while executing a thread
   */
  private void waitThreads(final List<Future<UncompressFileThread>> threads,
      final ExecutorService executor) throws AozanException {

    int samplesNotProcessed = 0;

    // Wait until all samples are processed
    do {

      try {
        Thread.sleep(CHECKING_DELAY_MS);
      } catch (InterruptedException e) {
        // LOGGER.warning("InterruptedException: " + e.getMessage());
      }

      samplesNotProcessed = 0;

      for (Future<UncompressFileThread> fst : threads) {

        if (fst.isDone()) {

          try {

            final UncompressFileThread st = fst.get();

            if (!st.isSuccess()) {

              // Close the thread pool
              executor.shutdownNow();

              // Wait the termination of current running task
              executor
                  .awaitTermination(WAIT_SHUTDOWN_MINUTES, TimeUnit.MINUTES);

              // Return error Step Result
              throw new AozanException(st.getException());
            }

          } catch (InterruptedException e) {
            // LOGGER.warning("InterruptedException: " + e.getMessage());
          } catch (ExecutionException e) {
            throw new AozanException(e);
          }

        } else {
          samplesNotProcessed++;
        }

      }

    } while (samplesNotProcessed > 0);

    // Close the thread pool
    executor.shutdown();
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

    // Set the directory to the file
    final File dir =
        new File(casavaOutputPath
            + "/Project_" + projectName + "/Sample_" + sampleName);

    // Set the prefix of the file
    final String prefix =
        String.format("%s_%s_L%03d_R%d_", sampleName, "".equals(index)
            ? "NoIndex" : index, lane, read);

    return new File(dir + "/").listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {

        return pathname.length() > 0
            && pathname.getName().startsWith(prefix)
            && pathname.getName().endsWith(COMPRESSION_EXTENSION);
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
  }

  //
  // Internal class
  //

  /**
   * This internal class create a thread for each array of file to uncompress
   * and compile in temporary file.
   * @author Sandrine Perrin
   */
  private static final class UncompressFileThread implements Runnable {

    private final File[] fastqFiles;
    private final String projectName;
    private final String sampleName;
    private final int lane;
    private final int read;
    private final SequenceFile seqFile;
    private final String tmpFastqFileName;
    private final File tmpFastqFile;

    private AozanException exception;
    private boolean success;

    /**
     * Get the exception generated by the call to processSequences in the run()
     * method.
     * @return a exception object or null if no Exception has been thrown
     */
    public Exception getException() {

      return this.exception;
    }

    /**
     * Test if the call to run method was a success
     * @return true if the call to run method was a success
     */
    public boolean isSuccess() {

      return this.success;
    }

    @Override
    public void run() {

      try {
        File f = getFastqFile(fastqFiles, read, lane, projectName, sampleName);

        this.success = (f != null && f.length() > 0);

      } catch (AozanException e) {
        this.exception = e;
      }

    }

    // TODO to remove
    public void run_old() {

      FileWriter outTmpFile;

      try {
        outTmpFile = new FileWriter(this.tmpFastqFile);

        processSequences(this.seqFile, outTmpFile);
        this.success = true;

        outTmpFile.close();

        // add new temparory uncompress fastq files in map
        System.out.println("key " + tmpFastqFileName + " name " + tmpFastqFile);

        setFastqFiles.put(this.tmpFastqFileName, this.tmpFastqFile);

      } catch (AozanException e) {
        this.exception = e;

      } catch (IOException e) {
        this.exception = new AozanException(e);
      }
    }

    /**
     * Read FASTQ file and process the data by FastQC modules
     * @param seqFile input file
     * @throws AozanException if an error occurs while processing file
     */
    private void processSequences(final SequenceFile seqFile,
        final FileWriter out) throws AozanException {

      try {

        while (seqFile.hasNext()) {

          final Sequence seq = seqFile.next();

          out.write(seq.getID() + "\n");
          out.write(seq.getSequence() + "\n");

          if (seq.getColorspace() == null)
            out.write("+\n");
          else
            out.write(seq.getColorspace() + "\n");

          out.write(seq.getQualityString() + "\n");
        }

      } catch (SequenceFormatException e) {
        throw new AozanException(e);

      } catch (IOException e) {
        throw new AozanException(e);
      }

    }

    //
    // Constructor
    //

    /**
     * Thread constructor.
     * @param projectName name of the project
     * @param sampleName name of the sample
     * @param lane lane of the sample
     * @param read read of the sample
     * @param fastqFiles fastq files for the sample
     * @throws AozanException if an error occurs while creating sequence file
     *           for FastQC
     */
    public UncompressFileThread(final File[] fastqFiles, final int read,
        final int lane, String projectName, final String sampleName,
        final String keyFiles) throws AozanException {

      if (fastqFiles == null || fastqFiles.length == 0)
        throw new AozanException("No fastq file defined");

      try {
        this.fastqFiles = fastqFiles;
        this.projectName = projectName;
        this.sampleName = sampleName;
        this.lane = lane;
        this.read = read;
        this.seqFile = SequenceFactory.getSequenceFile(fastqFiles);
        this.tmpFastqFileName = keyFiles;

        // Create temporary file
        this.tmpFastqFile =
            File.createTempFile("aozan_fastq_", ".fastq", new File(tmpDir));

      } catch (IOException e) {
        throw new AozanException(e);

      } catch (SequenceFormatException e) {
        throw new AozanException(e);
      }

    }

  }
}
