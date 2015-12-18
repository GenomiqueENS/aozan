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
 *      http://tools.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.aozan.util.StringUtils.stackTraceToString;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.io.FastqSample;

/**
 * The abstract class define commons methods for the Collectors which treats
 * fastq files.
 * @author Sandrine Perrin
 * @since 1.0
 */
public abstract class AbstractFastqCollector implements Collector {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private QC qc;

  /** The qc report output path. */
  private File qcReportOutputPath;

  /** The tmp dir. */
  private File tmpDir;

  // Set samples to treat
  /** The fastq samples. */
  private final Set<FastqSample> fastqSamples =
      new LinkedHashSet<>();

  // mode threaded
  /** The Constant CHECKING_DELAY_MS. */
  private static final int CHECKING_DELAY_MS = 5000;

  /** The Constant WAIT_SHUTDOWN_MINUTES. */
  private static final int WAIT_SHUTDOWN_MINUTES = 60;

  /** The threads. */
  private List<AbstractFastqProcessThread> threads;

  /** The future threads. */
  private List<Future<? extends AbstractFastqProcessThread>> futureThreads;

  /** The executor. */
  private ExecutorService executor;

  //
  // Abstract methods
  //

  /**
   * Collect data for a fastqSample.
   * @param data result data object
   * @param fastqSample sample object
   * @param reportDir the report dir
   * @param runPE if is a PE run
   * @return process thread instance
   * @throws AozanException if an error occurs while execution
   */
  protected abstract AbstractFastqProcessThread collectSample(
      final RunData data, final FastqSample fastqSample, final File reportDir,
      final boolean runPE) throws AozanException;

  /**
   * Return the number of thread that the collector can be used for execution.
   * @return number of thread
   */
  protected abstract int getThreadsNumber();

  /**
   * Test if undetermined indices samples must be processed.
   * @return true if undetermined indices samples must be processed
   */
  protected abstract boolean isProcessUndeterminedIndicesSamples();

  /**
   * Test if standard samples must be processed.
   * @return true if standard samples must be processed
   */
  protected boolean isProcessStandardSamples() {

    return true;
  }

  /**
   * Test if all reads (e.g. first end and second ends) must be processed or
   * only one.
   * @return true if all reads must be processed
   */
  protected boolean isProcessAllReads() {

    return true;
  }

  /**
   * Get the temporary path.
   * @return a File object with the temporary path
   */
  protected File getTemporaryDir() {

    return this.tmpDir;
  }

  //
  // Getters
  //

  /**
   * Get the FASTQ samples.
   * @return a set with the FASTQ samples
   */
  protected Set<FastqSample> getFastqSamples() {

    return Collections.unmodifiableSet(this.fastqSamples);
  }

  //
  // Collector methods
  //

  @Override
  public boolean isStatisticCollector() {
    return false;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return Lists.newArrayList(RunInfoCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME,
        FlowcellDemuxSummaryCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final QC qc, final Properties properties) {

    checkNotNull(qc, "qc argument cannot be null");
    checkNotNull(properties, "properties argument cannot be null");

    this.qcReportOutputPath = qc.getQcDir();
    this.tmpDir = qc.getTmpDir();
    this.qc = qc;

    if (this.getThreadsNumber() > 1) {

      // Create the list for threads
      this.threads = new ArrayList<>();
      this.futureThreads = new ArrayList<>();

      // Create executor service
      this.executor = Executors.newFixedThreadPool(this.getThreadsNumber());
    }
  }

  /**
   * Collect data : browse data for call concrete collector.
   * @param data result data object
   * @throws AozanException if an error occurs while collecting data
   */
  @Override
  public void collect(final RunData data) throws AozanException {

    checkNotNull(data, "data argument cannot be null");

    try {
      createListFastqSamples(data);
    } catch (IOException e) {
      throw new AozanException(e);
    }

    final boolean isRunPE = data.getRunMode().toUpperCase().equals("PE");

    RunData resultPart;
    if (this.getThreadsNumber() > 1) {

      for (final FastqSample fs : this.fastqSamples) {
        if (fs.getFastqFiles() != null && !fs.getFastqFiles().isEmpty()) {

          resultPart = this.loadResultPart(fs);

          if (resultPart != null) {
            data.put(resultPart);
          } else {

            // Create directory for the sample
            final File reportDir;

            if (fs.isUndeterminedIndex()) {
              reportDir =
                  new File(this.qcReportOutputPath, "Undetermined_indices");
            } else {
              reportDir = new File(this.qcReportOutputPath,
                  "Project_" + fs.getProjectName());
            }

            if (!reportDir.exists()) {
              if (!reportDir.mkdirs()) {
                throw new AozanException("Cannot create report directory: "
                    + reportDir.getAbsolutePath());
              }
            }

            final AbstractFastqProcessThread thread =
                this.collectSample(data, fs, reportDir, isRunPE);

            if (thread != null) {
              // Add thread to executor or futureThreads, I don't know
              this.threads.add(thread);
              this.futureThreads.add(this.executor.submit(thread, thread));
            }
          }
        } else {
          // TODO

          LOGGER.severe("FASTQ Collect: fastq is null or empty, key fq is "
              + fs.getKeyFastqSample() + " tmp fq "
              + fs.getSubsetFastqFilename() + " sample name "
              + fs.getSampleName() + " prefix rundata "
              + fs.getRundataPrefix());
        }
      }

      if (this.futureThreads.size() > 0) {

        // Wait for threads
        this.waitThreads(this.futureThreads, this.executor);

        // Add results of the threads to the data object
        for (final AbstractFastqProcessThread sft : this.threads) {
          data.put(sft.getResults());
        }
      }

    } else {

      // Code without starting threads :
      for (final FastqSample fs : this.fastqSamples) {

        if (fs.getFastqFiles() != null && !fs.getFastqFiles().isEmpty()) {

          resultPart = this.loadResultPart(fs);

          if (resultPart == null) {

            // Create directory for the sample
            final File reportDir;

            if (fs.isUndeterminedIndex()) {
              reportDir =
                  new File(this.qcReportOutputPath, "Undetermined_indices");
            } else {
              reportDir = new File(this.qcReportOutputPath,
                  "Project_" + fs.getProjectName());
            }

            if (!reportDir.exists()) {
              if (!reportDir.mkdirs()) {
                throw new AozanException("Cannot create report directory: "
                    + reportDir.getAbsolutePath());
              }
            }

            final AbstractFastqProcessThread pseudoThread =
                this.collectSample(data, fs, reportDir, isRunPE);

            if (pseudoThread == null) {
              continue;
            }

            // This not really a thread as it will be never started
            pseudoThread.run();

            // Throw exception from fastqscreen collector thread if not success
            if (!pseudoThread.isSuccess()) {
              throw new AozanException(pseudoThread.getException());
            } else {
              // Save result
              resultPart = pseudoThread.getResults();
              this.saveResultPart(fs, resultPart);
            }
          }
          data.put(resultPart);
        }
      }
    }

  }

  /**
   * Delete all temporaries files (fastq tmp files and map files).
   */
  public void clearTemporaryFiles() {

    LOGGER.info("Delete temporaries fastq and map files");

    final File[] files = this.tmpDir.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {
        return (pathname.getName()
            .startsWith(FastqSample.SUBSET_FASTQ_FILENAME_PREFIX)
            && (pathname.getName().endsWith(FastqSample.FASTQ_EXTENSION)
                || pathname.getName()
                    .endsWith(FastqSample.FASTQ_EXTENSION + ".tmp")))
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
   * Clear qc directory after successfully all FastqCollector.
   */
  @Override
  public void clear() {

    // Delete temporary uncompress fastq file
    clearTemporaryFiles();

    // Delete all data files fastqSample per fastqSample
    for (final FastqSample fs : this.fastqSamples) {

      if (fs.getFastqFiles().isEmpty()) {
        // Nothing to do
        continue;
      }

      deleteTemporaryDatafile(fs);

      deleteFastqScreenSampleReportXML(fs);
    }
  }

  //
  // Private methods
  //

  /**
   * Construct the map with all samples to treat.
   * @param data result data object
   * @throws IOException
   */
  private void createListFastqSamples(final RunData data) throws IOException {

    if (!this.fastqSamples.isEmpty()) {
      return;
    }

    final int laneCount = data.getLaneCount();
    final int readCount = data.getReadCount();

    int readIndexedCount = 0;

    for (int read = 1; read <= readCount; read++) {

      if (data.isReadIndexed(read)) {
        continue;
      }

      readIndexedCount++;

      for (int lane = 1; lane <= laneCount; lane++) {

        final List<String> sampleNames = data.getSamplesNameListInLane(lane);

        if (this.isProcessStandardSamples()) {
          for (final String sampleName : sampleNames) {

            // Skip invalid sample for quality control, like FASTQ file empty
            if (!isValidFastQSampleForQC(data, lane, sampleName,
                readIndexedCount)) {
              continue;
            }

            // Get the sample index
            final String index = data.getIndexSample(lane, sampleName);

            // Get project name
            final String projectName = data.getProjectSample(lane, sampleName);

            // Get description on sample
            final String descriptionSample =
                data.getSampleDescription(lane, sampleName);

            this.fastqSamples.add(new FastqSample(this.qc, readIndexedCount,
                lane, sampleName, projectName, descriptionSample, index));

          } // Sample
        }

        // Add undetermined indices samples
        if (this.isProcessUndeterminedIndicesSamples()) {

          // Check Undetermined fastq exist for this lane
          final String laneBarcode = data
              .get("demux.lane" + lane + ".sample.lane" + lane + ".barcode");

          if (laneBarcode != null && data.isUndeterminedInLane(lane)) {

            // Add undetermined sample
            this.fastqSamples
                .add(new FastqSample(this.qc, readIndexedCount, lane));
          }
        }
      } // Lane

      // Process only one read if needed
      if (!this.isProcessAllReads()) {
        break;
      }

    } // Read
  }

  /**
   * Checks if is valid fast q sample for qc.
   * @param data result data object.
   * @param lane the lane number.
   * @param sampleName the sample name.
   * @param read the read number.
   * @return true, if sample is valid otherwise false, like FASTQ file empty.
   */
  private boolean isValidFastQSampleForQC(final RunData data, final int lane,
      final String sampleName, final int read) {

    final String prefix =
        "demux.lane" + lane + ".sample." + sampleName + ".read" + read;

    // Check value exist in rundata, if not then fastq is empty
    final boolean valid = !(data.get(prefix + ".pf.cluster.count") == null
        || data.get(prefix + ".raw.cluster.count") == null);

    if (!valid)
      LOGGER.warning("Sample "
          + sampleName + " lane " + lane
          + ": no demultiplexing data found, no quality control data. Use prefix in rundata "
          + prefix);

    // Return true if sample valid
    return valid;

  }

  /**
   * Restore rundata from the save file if it exists.
   * @param fastqSample sample object
   * @return RunData corresponding to the file or null
   */
  private RunData loadResultPart(final FastqSample fastqSample) {
    // Check for data file
    final File dataFile = this.createTemporaryDataFile(fastqSample);

    // Data file doesn't exists
    if (!dataFile.exists()) {
      return null;
    }

    // Restore results in data
    RunData data;
    try {
      data = new RunData(dataFile);

      LOGGER.fine("For the "
          + this.getName().toUpperCase() + " : Restore data file for "
          + fastqSample.getKeyFastqSample());

    } catch (final IOException io) {

      LOGGER.warning("In "
          + this.getName().toUpperCase()
          + " : Error during reading data file for the sample "
          + fastqSample.getKeyFastqSample());

      return null;
    }
    return data;
  }

  /**
   * Save rundata for a sample in a file in a qc report directory.
   * @param fastqSample sample object
   * @param data RunData corresponding to one sample
   */
  protected void saveResultPart(final FastqSample fastqSample,
      final RunData data) {

    try {
      // Define the part result directory
      final File dataFile = this.createTemporaryDataFile(fastqSample);

      // Create the result part file
      data.createRunDataFile(dataFile);

      LOGGER.fine(this.getName().toUpperCase()
          + ": " + fastqSample.getKeyFastqSample() + " save data file");

    } catch (final IOException ae) {

      LOGGER.warning("For the "
          + this.getName()
          + " collector: Error during writing data file for the sample "
          + fastqSample.getKeyFastqSample() + "(" + ae.getMessage() + ")");
    }
  }

  /**
   * Return run data file corresponding of a sample or a undetermined fastq.
   * @param fastqSample the fastq sample
   * @return run data file
   */
  private File createTemporaryDataFile(final FastqSample fastqSample) {

    // Define the part result directory
    final File dataFileDir;
    if (fastqSample.isUndeterminedIndex()) {
      dataFileDir = new File(this.qcReportOutputPath, "Undetermined_indices");
    } else {
      dataFileDir = new File(this.qcReportOutputPath,
          "Project_" + fastqSample.getProjectName());
    }

    // Define the part result file
    return new File(dataFileDir,
        this.getName() + "_" + fastqSample.getKeyFastqSample() + ".data");

  }

  /**
   * Wait the end of the threads.
   * @param threads list with the threads
   * @param executor the executor
   * @throws AozanException if an error occurs while executing a thread
   */
  private void waitThreads(
      final List<Future<? extends AbstractFastqProcessThread>> threads,
      final ExecutorService executor) throws AozanException {

    final boolean interruptThreadIfRunning = true;

    int samplesNotProcessed;

    // Wait until all samples are processed
    do {

      try {
        Thread.sleep(CHECKING_DELAY_MS);
      } catch (final InterruptedException e) {
        // LOGGER.warning("InterruptedException: " + e.getMessage());
      }

      final Set<AbstractFastqProcessThread> threadDataSaved = new HashSet<>();
      samplesNotProcessed = 0;

      for (final Future<? extends AbstractFastqProcessThread> fst : threads) {

        if (fst.isDone()) {

          try {

            final AbstractFastqProcessThread st = fst.get();

            if (!st.isSuccess()) {

              // Close the thread pool
              executor.shutdownNow();

              // Wait the termination of current running task
              executor.awaitTermination(WAIT_SHUTDOWN_MINUTES,
                  TimeUnit.MINUTES);

              // Return error Step Result
              throw new AozanException(st.getException());

            } else {
              // if success, save results
              if (!threadDataSaved.contains(st)) {
                this.saveResultPart(st.getFastqSample(), st.getResults());
                threadDataSaved.add(st);
              }

            }
          } catch (final InterruptedException | ExecutionException e) {

            if (fst.cancel(interruptThreadIfRunning)) {
              LOGGER.severe(
                  "Throw exception by thread execution, task could not be cancelled. "
                      + e.getMessage() + '\n' + stackTraceToString(e));
            } else {
              LOGGER.severe(
                  "Throw exception by thread execution, task is cancelled."
                      + e.getMessage() + '\n' + stackTraceToString(e));
            }

            // Throw exception
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
   * Delete the fastq screen sample report xml.
   * @param fs the fastq sample
   */
  private void deleteFastqScreenSampleReportXML(final FastqSample fs) {
    // Collect all data file
    final List<File> files = collectFileFromSuffix(fs, "-fastqscreen.xml");

    // Delete temporary data file
    for (final File f : files) {
      if (!(f.exists() && f.delete())) {
        LOGGER.warning("Can not delete fastqscreen sample report XML file : "
            + f.getAbsolutePath());
      }
    }

  }

  /**
   * Delete the temporary data file.
   * @param fs the fastq sample
   */
  private void deleteTemporaryDatafile(final FastqSample fs) {

    // Collect all data file
    final List<File> files = collectFileFromSuffix(fs, ".data");

    // Delete temporary data file
    for (final File f : files) {
      if (!(f.exists() && f.delete())) {
        LOGGER.warning("Can not delete data file : " + f.getAbsolutePath());
      }
    }

  }

  /**
   * Collect specific file in quality control directory from suffix.
   * @param fs the FastqSample instance.
   * @param suffix the suffix of temporary file.
   * @return the list of data file, if not found return an empty list.
   */
  private List<File> collectFileFromSuffix(final FastqSample fs,
      final String suffix) {

    // Build subdirectory name
    final String subdir = (fs.isUndeterminedIndex()
        ? "Undetermined_indices" : "Project_" + fs.getProjectName());

    // Build subdirectory path
    final File projectDir = new File(this.qcReportOutputPath, subdir);

    // Collect all data file
    final File[] dataFiles = projectDir.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {
        return pathname.getName().endsWith(suffix);
      }
    });

    if (dataFiles == null || dataFiles.length == 0) {
      // Not found
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(Arrays.asList(dataFiles));
  }

}
