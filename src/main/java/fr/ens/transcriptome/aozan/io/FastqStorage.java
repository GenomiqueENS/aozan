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

  private static FastqStorage singleton = null;

  private static final String KEY_NUMBER_THREAD = "qc.conf.fastqc.threads";
  private static final String KEY_TMP_DIR = "tmp.dir";

  private static Map<String, File> setFastqFiles = new HashMap<String, File>();
  private static String tmpDir = null;

  /**
   * Create a sequenceFile
   * @param fastqFiles array of fastq files
   * @return SequenceFile
   * @throws AozanException if an error occurs while creating sequence file
   */
  public SequenceFile getFastqSequenceFile(final File[] fastqFiles)
      throws AozanException {

    if (fastqFiles.length == 0) {
      LOGGER.warning("List fastq file to uncompress and compile is empty");
      return null;
    }

    String key = keyFiles(fastqFiles);
    SequenceFile sequenceFile = null;

    try {
      // Verified that the fastq files have been treated
      if (setFastqFiles.containsKey(key))
        sequenceFile = SequenceFactory.getSequenceFile(fastqFiles);

      // Add in list of temporary fastq sequence files

    } catch (SequenceFormatException sfe) {
      sfe.printStackTrace();
      throw new AozanException(sfe.getMessage());

    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw new AozanException(ioe.getMessage());
    }

    return sequenceFile;
  }

  public Sequence nextSequence(final SequenceFile seqFile) {
    Sequence seq = null;

    return seq;
  }

  /**
   * Uncompresses and compiles files of array.
   * @param fastqFiles fastq files of array
   * @return file compile all files
   * @throws AozanException if an error occurs while creating file
   */
  public File getFastqFile(final File[] fastqFiles) throws AozanException {

    final long startTime = System.currentTimeMillis();
    LOGGER.fine("Start uncompressed for fastq File ");

    if (fastqFiles.length == 0) {
      LOGGER.warning("List fastq file to uncompress and compile is empty");
      return null;
    }

    String key = keyFiles(fastqFiles);

    // Return uncompress temporary file if it exist
    if (setFastqFiles.containsKey(key))
      return setFastqFiles.get(key);

    // Uncompresses and compiles files of array in new temporary files
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
      throw new AozanException(io.getMessage());
    }

    // Add in list of temporary fastq files
    setFastqFiles.put(key, tmpFastqFile);

    LOGGER.fine("End uncompressed for fastq File "
        + key + " in "
        + toTimeHumanReadable(System.currentTimeMillis() - startTime));

    return tmpFastqFile;
  }

  public void addTmpFile(final File[] files, final File tmpFile) {
    setFastqFiles.put(keyFiles(files), tmpFile);
  }

  public boolean tmpFileExist(File[] files) {
    return setFastqFiles.containsKey(keyFiles(files));
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

    if (file.exists())
      setFastqFiles.remove("");

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
      final String casavaOutputPath, final String compressionExtension)
      throws AozanException {

    // Create the list for threads
    final List<SeqFileThread> threads = Lists.newArrayList();
    final List<Future<SeqFileThread>> futureThreads = Lists.newArrayList();

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
          final SeqFileThread sft =
              processFile(data, casavaOutputPath, projectName, sampleName,
                  index, lane, readSample, compressionExtension);

          if (sft != null) {
            threads.add(sft);
            futureThreads.add(executor.submit(sft, sft));
          }
        }
      }
    }

    // Wait for threads
    waitThreads(futureThreads, executor);
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
  public SeqFileThread processFile(final RunData data,
      final String casavaOutputPath, final String projectName,
      final String sampleName, final String index, final int lane,
      final int read, final String compressionExtension) throws AozanException {

    // Set the directory to the file
    final File dir =
        new File(casavaOutputPath
            + "/Project_" + projectName + "/Sample_" + sampleName);

    // Set the prefix of the file
    final String prefix =
        String.format("%s_%s_L%03d_R%d_", sampleName, "".equals(index)
            ? "NoIndex" : index, lane, read);

    // Set the list of the files for the FASTQ data
    final File[] fastqFiles = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {

        return pathname.length() > 10
            && pathname.getName().startsWith(prefix)
            && pathname.getName().endsWith(compressionExtension);
      }
    });

    if (fastqFiles == null || fastqFiles.length == 0) {
      return null;
    }

    // Verified that the fastq files have been treated, if true return null
    String key = keyFiles(fastqFiles);
    if (setFastqFiles.containsKey(key))
      return null;

    System.out.println("file src : " + fastqFiles[0].getAbsolutePath());

    // Create the thread object
    return new SeqFileThread(projectName, sampleName, lane, read, fastqFiles,
        key, compressionExtension);
  }

  /**
   * Wait the end of the threads.
   * @param threads list with the threads
   * @param executor the executor
   * @throws AozanException if an error occurs while executing a thread
   */
  private void waitThreads(final List<Future<SeqFileThread>> threads,
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

      for (Future<SeqFileThread> fst : threads) {

        if (fst.isDone()) {

          try {

            final SeqFileThread st = fst.get();

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

  private static final class SeqFileThread implements Runnable {

    private final String projectName;
    private final String sampleName;
    private final int lane;
    private final int read;
    private final SequenceFile seqFile;
    private final String tmpFastqFileName;
    private final File tmpFastqFile;
    private final String compressionExtension;

    private final RunData results;
    private AozanException exception;
    private boolean success;

    /**
     * Get the results of the FastQC analysis.
     * @return a RunData object with only the result of the thread
     */
    public RunData getResults() {

      return this.results;
    }

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

      FileWriter outTmpFile;

      try {
        outTmpFile = new FileWriter(this.tmpFastqFile);

        processSequences(this.seqFile, outTmpFile);
        this.success = true;

        outTmpFile.close();

        // add new temparory uncompress fastq files in map
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

          // System.out.println("sample "
          // + sampleName + "\tseq " + seq.getSequence());

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
    public SeqFileThread(final String projectName, final String sampleName,
        final int lane, final int read, final File[] fastqFiles,
        final String keyFiles, final String compressionExtension)
        throws AozanException {

      if (fastqFiles == null || fastqFiles.length == 0)
        throw new AozanException("No fastq file defined");

      try {
        this.projectName = projectName;
        this.sampleName = sampleName;
        this.lane = lane;
        this.read = read;
        this.seqFile = SequenceFactory.getSequenceFile(fastqFiles);
        this.tmpFastqFileName = keyFiles;
        this.compressionExtension = compressionExtension;

        this.results = new RunData();

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
