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

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
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

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.aozan.io.FastqStorage;

/**
 * The abstract class define commons methods for the Collectors which treats
 * fastq files.
 * @since 1.0
 * @author Sandrine Perrin
 */
abstract public class AbstractFastqCollector implements Collector {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  public static final String RUN_MODE_KEY = "run.info.run.mode";
  public static final String READ_COUNT_KEY = "run.info.read.count";
  public static final String LANE_COUNT_KEY = "run.info.flow.cell.lane.count";

  private FastqStorage fastqStorage;
  private String casavaOutputPath;
  private String qcReportOutputPath;
  private File tmpDir;

  // Set samples to treat
  protected final static Set<FastqSample> fastqSamples =
      new LinkedHashSet<FastqSample>();

  // mode threaded
  private static final int CHECKING_DELAY_MS = 5000;
  private static final int WAIT_SHUTDOWN_MINUTES = 60;

  private List<AbstractFastqProcessThread> threads;
  private List<Future<? extends AbstractFastqProcessThread>> futureThreads;
  private ExecutorService executor;

  //
  // Abstract methods
  //

  /**
   * Collect data for a fastqSample
   * @param data result data object
   * @param fastqSample sample object
   * @param runPE true if it is a run PE else false
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
   * Test if standard samples must be processed.
   * @return true if standard samples must be processed
   */
  protected boolean isProcessStandardSamples() {

    return true;
  }

  /**
   * Test if undetermined indices samples must be processed.
   * @return true if undetermined indices samples must be processed
   */
  protected boolean isProcessUndeterminedIndicesSamples() {

    return false;
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
   * Get the fastq storage.
   * @return the instance of the FastqStorage of the collector
   */
  protected FastqStorage getFastqStorage() {

    return this.fastqStorage;
  }

  //
  // Collector methods
  //

  /**
   * Get the name of the collectors required to run this collector.
   * @return a list of String with the name of the required collectors
   */
  @Override
  public List<String> getCollectorsNamesRequiered() {
    return Lists.newArrayList(RunInfoCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME);
  }

  /**
   * Configure the collector with the path of the run data
   * @param properties object with the collector configuration
   */
  @Override
  public void configure(Properties properties) {

    this.casavaOutputPath = properties.getProperty(QC.CASAVA_OUTPUT_DIR);
    this.qcReportOutputPath = properties.getProperty(QC.QC_OUTPUT_DIR);
    this.tmpDir = new File(properties.getProperty(QC.TMP_DIR));

    this.fastqStorage = FastqStorage.getInstance();
    this.fastqStorage.setTmpDir(this.tmpDir);

    if (this.getThreadsNumber() > 1) {

      // Create the list for threads
      this.threads = Lists.newArrayList();
      this.futureThreads = Lists.newArrayList();

      // Create executor service
      this.executor = Executors.newFixedThreadPool(this.getThreadsNumber());
    }
  }

  /**
   * Collect data : browse data for call concrete collector,
   * @param data result data object
   * @throws AozanException if an error occurs while collecting data
   */
  @Override
  public void collect(RunData data) throws AozanException {

    createListFastqSamples(data);

    final boolean isRunPE = data.get(RUN_MODE_KEY).toUpperCase().equals("PE");

    RunData resultPart = null;
    if (this.getThreadsNumber() > 1) {

      for (FastqSample fs : fastqSamples) {
        if (fs.getFastqFiles() != null && !fs.getFastqFiles().isEmpty()) {

          resultPart = loadResultPart(fs);

          if (resultPart != null) {
            data.put(resultPart);
          } else {

            // Create directory for the sample
            final File reportDir;

            if (fs.isIndeterminedIndices())
              reportDir =
                  new File(this.qcReportOutputPath + "/Undetermined_indices");
            else
              reportDir =
                  new File(this.qcReportOutputPath
                      + "/Project_" + fs.getProjectName());

            if (!reportDir.exists())
              if (!reportDir.mkdirs())
                throw new AozanException("Cannot create report directory: "
                    + reportDir.getAbsolutePath());

            AbstractFastqProcessThread thread =
                collectSample(data, fs, reportDir, isRunPE);

            if (thread != null) {
              // Add thread to executor or futureThreads, I don't know
              this.threads.add(thread);
              this.futureThreads.add(this.executor.submit(thread, thread));
            }
          }
        }
      }

      if (this.futureThreads.size() > 0) {

        // Wait for threads
        waitThreads(this.futureThreads, this.executor);

        // Add results of the threads to the data object
        for (AbstractFastqProcessThread sft : this.threads)
          data.put(sft.getResults());
      }

    } else {

      // Code without starting threads :
      for (FastqSample fs : fastqSamples) {

        if (fs.getFastqFiles() != null && !fs.getFastqFiles().isEmpty()) {

          resultPart = loadResultPart(fs);

          if (resultPart == null) {

            // Create directory for the sample
            final File reportDir;

            if (fs.isIndeterminedIndices())
              reportDir =
                  new File(this.qcReportOutputPath + "/Undetermined_indices");
            else
              reportDir =
                  new File(this.qcReportOutputPath
                      + "/Project_" + fs.getProjectName());

            if (!reportDir.exists())
              if (!reportDir.mkdirs())
                throw new AozanException("Cannot create report directory: "
                    + reportDir.getAbsolutePath());

            AbstractFastqProcessThread pseudoThread =
                collectSample(data, fs, reportDir, isRunPE);

            if (pseudoThread == null)
              continue;

            // This not really a thread as it will be never started
            pseudoThread.run();

            // Throw exception from fastqscreen collector thread if not success
            if (!pseudoThread.isSuccess())
              throw new AozanException(pseudoThread.getException());
            else {
              // Save result
              resultPart = pseudoThread.getResults();
              saveResultPart(fs, resultPart);
            }
          }
          data.put(resultPart);
        }
      }
    }

  }

  /**
   * Estimate the size needed for all uncompresses fastq files and construct the
   * map with all samples to treat.
   * @param data result data object
   */
  private void createListFastqSamples(final RunData data) {

    if (!fastqSamples.isEmpty())
      return;

    final int laneCount = data.getInt(LANE_COUNT_KEY);
    final int readCount = data.getInt(READ_COUNT_KEY);

    int readIndexedCount = 0;

    for (int read = 1; read <= readCount; read++) {

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;

      readIndexedCount++;

      for (int lane = 1; lane <= laneCount; lane++) {

        final List<String> sampleNames =
            Lists.newArrayList(Splitter.on(',').split(
                data.get("design.lane" + lane + ".samples.names")));

        if (isProcessStandardSamples())
          for (String sampleName : sampleNames) {

            // Get the sample index
            String index =
                data.get("design.lane" + lane + "." + sampleName + ".index");

            // Get project name
            String projectName =
                data.get("design.lane"
                    + lane + "." + sampleName + ".sample.project");
            // Get description on sample
            String descriptionSample =
                data.get("design.lane"
                    + lane + "." + sampleName + ".description");

            fastqSamples.add(new FastqSample(this.casavaOutputPath,
                readIndexedCount, lane, sampleName, projectName,
                descriptionSample, index));

          } // Sample

        // Add undetermined indices samples
        if (isProcessUndeterminedIndicesSamples())
          fastqSamples.add(new FastqSample(this.casavaOutputPath,
              readIndexedCount, lane));

      } // Lane

      // Process only one read if needed
      if (!isProcessAllReads())
        break;

    } // Read
  }

  /**
   * Restore rundata from the save file if it exists.
   * @param fastqSample sample object
   * @return RunData corresponding to the file or null
   */
  private RunData loadResultPart(final FastqSample fastqSample) {
    // Check for data file
    File dataFile =
        new File(this.qcReportOutputPath
            + "/Project_" + fastqSample.getProjectName() + "/" + getName()
            + "_" + fastqSample.getKeyFastqSample() + ".data");

    // Data file doesn't exists
    if (!dataFile.exists())
      return null;

    // Restore results in data
    RunData data = null;
    try {
      data = new RunData(dataFile);

      LOGGER.fine("For the "
          + this.getName().toUpperCase() + " : Restore data file for "
          + fastqSample.getKeyFastqSample());

    } catch (IOException io) {

      LOGGER.warning("In "
          + this.getName().toUpperCase()
          + " : Error during reading data file for the sample "
          + fastqSample.getKeyFastqSample());

      return null;
    }
    return data;
  }

  /**
   * Save rundata for a sample in a file in a qc report directory
   * @param fastqSample sample object
   * @param data RunData corresponding to one sample
   */
  protected void saveResultPart(final FastqSample fastqSample,
      final RunData data) {

    try {
      String dataFilePath =
          this.qcReportOutputPath
              + "/Project_" + fastqSample.getProjectName() + "/" + getName()
              + "_" + fastqSample.getKeyFastqSample() + ".data";

      data.createRunDataFile(dataFilePath);

      LOGGER.fine(this.getName().toUpperCase()
          + " : " + fastqSample.getKeyFastqSample() + " save data file");

    } catch (IOException ae) {

      LOGGER.warning("For the "
          + this.getName()
          + " : Error during writing data file for the sample "
          + fastqSample.getKeyFastqSample());
    }
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

    int samplesNotProcessed = 0;

    // Wait until all samples are processed
    do {

      try {
        Thread.sleep(CHECKING_DELAY_MS);
      } catch (InterruptedException e) {
        // LOGGER.warning("InterruptedException: " + e.getMessage());
      }

      samplesNotProcessed = 0;

      for (Future<? extends AbstractFastqProcessThread> fst : threads) {

        if (fst.isDone()) {

          try {

            final AbstractFastqProcessThread st = fst.get();

            if (!st.isSuccess()) {

              // Close the thread pool
              executor.shutdownNow();

              // Wait the termination of current running task
              executor
                  .awaitTermination(WAIT_SHUTDOWN_MINUTES, TimeUnit.MINUTES);

              // Return error Step Result
              throw new AozanException(st.getException());

            } else {
              // if success, save results
              if (!st.isDataSaved()) {
                saveResultPart(st.getFastqSample(), st.getResults());
                st.setDataSave();
              }

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
   * Clear qc directory after successfully all FastqCollector
   */
  @Override
  public void clear() {

    // Delete temporary uncompress fastq file
    this.fastqStorage.clear();

    // Delete all data files fastqSample per fastqSample
    for (FastqSample fs : fastqSamples) {

      if (!fs.getFastqFiles().isEmpty()) {
        File projectDir =
            new File(this.qcReportOutputPath
                + "/Project_" + fs.getProjectName());

        File[] dataFiles = projectDir.listFiles(new FileFilter() {

          public boolean accept(final File pathname) {
            return pathname.getName().endsWith(".data");
          }
        });

        // delete datafile
        for (File f : dataFiles) {
          if (f.exists())
            if (!f.delete())
              LOGGER.warning("Can not delete data file : "
                  + f.getAbsolutePath());
        }
      }
    }
  }
}
