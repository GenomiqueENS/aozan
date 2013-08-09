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
import fr.ens.transcriptome.aozan.Globals;
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
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final String KEY_RUN_MODE = "run.info.run.mode";
  public static final String KEY_READ_COUNT = "run.info.read.count";
  public static final String KEY_LANE_COUNT = "run.info.flow.cell.lane.count";

  protected FastqStorage fastqStorage;
  protected String casavaOutputPath;
  protected String qcReportOutputPath;
  protected String tmpPath;

  // Set samples to treat
  protected final static Set<FastqSample> fastqSamples =
      new LinkedHashSet<FastqSample>();

  // mode threaded
  private static final int CHECKING_DELAY_MS = 5000;
  private static final int WAIT_SHUTDOWN_MINUTES = 60;

  protected List<AbstractFastqProcessThread> threads;
  protected List<Future<? extends AbstractFastqProcessThread>> futureThreads;
  protected ExecutorService executor;

  /**
   * Collect data for a fastqSample
   * @param data
   * @param fastqSample
   * @param runPE true if it is a run PE else false
   * @throws AozanException if an error occurs while execution
   */
  abstract protected AbstractFastqProcessThread collectSample(
      final RunData data, final FastqSample fastqSample, final File reportDir,
      final boolean runPE) throws AozanException;

  /**
   * Return the number of thread that the collector can be used for execution.
   * @return number of thread
   */
  abstract protected int getThreadsNumber();

  /**
   * Get the name of the collector
   * @return the name of the collector
   */
  @Override
  abstract public String getName();

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
    this.tmpPath = properties.getProperty(QC.TMP_DIR);

    this.fastqStorage = FastqStorage.getInstance();
    this.fastqStorage.setTmpDir(this.tmpPath);

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

    final boolean runPE = data.get(KEY_RUN_MODE).equals("PE") ? true : false;

    RunData resultPart = null;
    if (this.getThreadsNumber() > 1) {

      for (FastqSample fs : fastqSamples) {
        if (fs.getFastqFiles() != null && !fs.getFastqFiles().isEmpty()) {

          resultPart = loadResultPart(fs);

          if (resultPart != null) {
            data.put(resultPart);
          } else {

            // Create directory for the sample
            final File reportDir =
                new File(this.qcReportOutputPath
                    + "/Project_" + fs.getProjectName());

            if (!reportDir.exists())
              if (!reportDir.mkdirs())
                throw new AozanException("Cannot create report directory: "
                    + reportDir.getAbsolutePath());

            AbstractFastqProcessThread thread =
                collectSample(data, fs, reportDir, runPE);

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
        // TODO to remove after text
        if (fs.getFastqFiles() != null && !fs.getFastqFiles().isEmpty()) {

          resultPart = loadResultPart(fs);

          if (resultPart == null) {

            final File reportDir =
                new File(this.qcReportOutputPath
                    + "/Project_" + fs.getProjectName());

            if (!reportDir.exists())
              if (!reportDir.mkdirs())
                throw new AozanException("Cannot create report directory: "
                    + reportDir.getAbsolutePath());

            AbstractFastqProcessThread pseudoThread =
                collectSample(data, fs, reportDir, runPE);

            if (pseudoThread == null)
              continue;

            // This not really a thread as it will be never started
            pseudoThread.run();

            // Throw exception from fastqscreen collector thread if not success
            if (!pseudoThread.success)
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
   * @param data data used
   */
  private void createListFastqSamples(final RunData data) {

    if (!fastqSamples.isEmpty())
      return;

    final int laneCount = data.getInt(KEY_LANE_COUNT);
    final int readCount = data.getInt(KEY_READ_COUNT);

    for (int read = 1; read <= readCount; read++) {

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

          // TODO check possible to remove
          // update list of genomes samples
          // receive genome name for sample
          // String genomeSample =
          // data.get("design.lane" + lane + "." + sampleName + ".sample.ref");
          // if genomeSample is present in mapAliasGenome, add in list of
          // genomes reference for the mapping
          // genomeSample = genomeSample.trim().toLowerCase();
          // genomeSample = genomeSample.replace('"', '\0');

          FastqSample fastqSample =
              new FastqSample(this.casavaOutputPath, read, lane, sampleName,
                  projectName, index);
          fastqSamples.add(fastqSample);

        } // sample

      }// lane
    }// read
  }

  /**
   * Restore rundata from the save file if it exists.
   * @param fastqSample
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
   * @param fastqSample sample
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
              if (!st.isDataSave()) {
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
