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
import fr.ens.transcriptome.aozan.FastqScreenDemo;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.RunDataGenerator;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.aozan.io.FastqStorage;

/**
 * The abstract class define commons methods for the Collectors which treats
 * fastq files.
 * @author Sandrine Perrin
 */
abstract public class AbstractFastqCollector implements Collector {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final String KEY_READ_COUNT = "run.info.read.count";
  public static final String KEY_READ_X_INDEXED = "run.info.read";
  // public static final String COLLECTOR_NAME = "";

  public static boolean isExistRunDir = false;

  protected static FastqStorage fastqStorage;
  protected static String casavaOutputPath;
  protected static String qcReportOutputPath;
  protected static String tmpPath;
  protected static boolean paired = false;

  private static Set<FastqSample> fastqSamples =
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
   * @throws AozanException if an error occurs while execution
   */
  abstract protected AbstractFastqProcessThread collectSample(
      final RunData data, final FastqSample fastqSample, final File reportDir)
      throws AozanException;

  abstract public int getThreadsNumber();

  abstract public void setThreadsNumber(final int numberThreads);

  // TODO REVIEW: add this methods
  // abstract public AbstractFastqProcessThread processFile(final RunData data,
  // final FastqSample fastqSample) throws AozanException;

  @Override
  /**
   * Get the name of the collector
   * @return the name of the collector
   */
  abstract public String getName();

  @Override
  /**
   * Get the name of the collectors required to run this collector.
   * @return an array of String with the name of the required collectors
   */
  public String[] getCollectorsNamesRequiered() {
    return new String[] {RunInfoCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME};
  }

  @Override
  /**
   * Configure the collector with the path of the run data
   * @param properties object with the collector configuration
   */
  public void configure(Properties properties) {

    casavaOutputPath =
        properties.getProperty(RunDataGenerator.CASAVA_OUTPUT_DIR);

    // TODO REVIEW: "_tmp" is unnecessary as it has already set in the python
    // code
    qcReportOutputPath =
        properties.getProperty(RunDataGenerator.QC_OUTPUT_DIR) + "_tmp";

    tmpPath = properties.getProperty(RunDataGenerator.TMP_DIR);

    fastqStorage = FastqStorage.getInstance();
    fastqStorage.setTmpDir(tmpPath);

    System.out.println("Abstract configure  :" + casavaOutputPath);

    if (this.getThreadsNumber() > 1)
      configureModeMultiThread(properties);

  }

  /**
   * Configure a multi-threaded mode.
   * @param properties for the collector
   */
  public void configureModeMultiThread(final Properties properties) {

    if (properties.containsKey("qc.conf.fastqc.threads")) {

      try {
        int confThreads =
            Integer.parseInt(properties.getProperty("qc.conf.fastqc.threads")
                .trim());
        if (confThreads > 0)
          this.setThreadsNumber(confThreads);

      } catch (NumberFormatException e) {
      }

      // Create the list for threads
      this.threads = Lists.newArrayList();
      this.futureThreads = Lists.newArrayList();

      // Create executor service
      this.executor = Executors.newFixedThreadPool(this.getThreadsNumber());
    }
  }

  @Override
  /**
   * Collect data : browse data for call concrete collector,
   * @param data result data object
   * @throws AozanException if an error occurs while collecting data
   */
  public void collect(RunData data) throws AozanException {
    // TODO to remove
    data = FastqScreenDemo.getRunData();

    controlPreCollect(data, qcReportOutputPath);

    // System.out.println("nb fastq Sample " + fastqSamples.size());

    RunData resultPart = null;
    if (this.getThreadsNumber() > 1) {

      for (FastqSample fs : fastqSamples) {
        if (fs.getFastqFiles() != null && !fs.getFastqFiles().isEmpty()) {

          resultPart = loadResultPart(fs);

          if (resultPart != null) {
            data.put(resultPart);
          } else {

            final File reportDir =
                new File(qcReportOutputPath + "/Project_" + fs.getProjectName());

            if (!reportDir.exists())
              if (!reportDir.mkdirs())
                throw new AozanException("Cannot create report directory: "
                    + reportDir.getAbsolutePath());

            AbstractFastqProcessThread thread =
                collectSample(data, fs, reportDir);

            if (thread != null) {
              // Add thread to executor or futureThreads, I don't know
              threads.add(thread);
              futureThreads.add(executor.submit(thread, thread));
            }
          }
        }
      }

      System.out.println("multithread " + futureThreads.size());
      // Wait for threads
      waitThreads(futureThreads, executor);

      // Add results of the threads to the data object
      for (AbstractFastqProcessThread sft : threads)
        data.put(sft.getResults());

    } else {

      // Code without starting threads :
      for (FastqSample fs : fastqSamples) {
        // TODO to remove after text
        if (fs.getFastqFiles() != null && !fs.getFastqFiles().isEmpty()) {

          resultPart = loadResultPart(fs);

          if (resultPart == null) {

            final File reportDir =
                new File(qcReportOutputPath + "/Project_" + fs.getProjectName());

            if (!reportDir.exists())
              if (!reportDir.mkdirs())
                throw new AozanException("Cannot create report directory: "
                    + reportDir.getAbsolutePath());

            // This not really a thread as it will be never started
            AbstractFastqProcessThread pseudoThread =
                collectSample(data, fs, reportDir);
            pseudoThread.run();

            resultPart = pseudoThread.getResults();
            saveResultPart(fs, resultPart);
          }
          data.put(resultPart);
        }
      }
    }

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

    if (!fastqSamples.isEmpty())
      return;

    // Count size from all fastq files util
    long freeSpace = new File(tmpPath).getFreeSpace();

    // Estimate used space : needed space + 5%
    long uncompressedSizeNeeded =
        (long) ((double) countUncompressedSizeFilesNeeded(data) * 1.05);

    if (uncompressedSizeNeeded > freeSpace)
      throw new AozanException(
          "Not enough disk space to store uncompressed fastq files for step fastqScreen. We are "
              + freeSpace
              + " in directory "
              + new File(tmpPath).getAbsolutePath()
              + ", and we need "
              + uncompressedSizeNeeded + ". Echec Aozan");

    System.out
        .println("Enough disk space to store uncompressed fastq files for step fastqScreen. We are "
            + freeSpace
            + " in directory "
            + new File(tmpPath).getAbsolutePath()
            + ", and we need "
            + uncompressedSizeNeeded);

    // TODO to remove after test
    // Verify if directory for the run exists
    File runDir = new File(qcReportOutputPath);
    if (runDir.exists() && runDir.isDirectory()) {
      FastQCCollector.isExistRunDir = true;

    } else if (!(new File(qcReportOutputPath).mkdir())) {
      // throw new AozanException(
      System.out
          .println("Error during create save directory for results intermediate of AbstractFastqCollector.");
    }
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
    // Map<String, FastqSample> samples = new LinkedHashMap<String,
    // FastqSample>();

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

          // System.out.println("nx fs " + sampleName);

          FastqSample fs =
              new FastqSample(casavaOutputPath, read, lane, sampleName,
                  projectName, index);

          // System.out.println("size files  : " + uncompressedSizeFiles);

          uncompressedSizeFiles += fs.getUncompressedSize();

          // System.out.println("size files  : " + uncompressedSizeFiles);

          fastqSamples.add(fs);

        } // sample
      }// lane
    }// read

    // Create a unmodifiable linked map
    // fastqsSamples = Collections.unmodifiableMap(samples);

    return uncompressedSizeFiles;
  }

  /**
   * Restore rundata from the save file if it exists.
   * @param fastqSample
   * @return RunData corresponding to the file or null
   */
  private RunData loadResultPart(final FastqSample fastqSample) {
    // Check for data file
    File dataFile =
        new File(qcReportOutputPath
            + "/Project_" + fastqSample.getProjectName() + "/" + getName()
            + "_" + fastqSample.getKeyFastqSample() + ".data");

    // Data file doesn't exists
    if (!dataFile.exists())
      return null;

    // System.out.println("verify exists back-up for \n\t"
    // + dataFile.getAbsolutePath() + "  " + dataFile.exists());

    // Restore results in data
    RunData data = null;
    try {
      data = new RunData(dataFile);

    } catch (IOException io) {

      LOGGER.warning("Error during reading data file for the sample "
          + fastqSample.getKeyFastqSample());
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
          qcReportOutputPath
              + "/Project_" + fastqSample.getProjectName() + "/" + getName()
              + "_" + fastqSample.getKeyFastqSample() + ".data";

      boolean success = data.createRunDataFile(dataFilePath);

      if (success) {
        System.out
            .println("Save data file for the sample here "
                + new File(dataFilePath).getAbsolutePath() + " size "
                + data.size());

        LOGGER
            .fine("Save data file for the sample here "
                + new File(dataFilePath).getAbsolutePath() + " size "
                + data.size());
      }

    } catch (IOException ae) {

      System.out.println("Error during sava rundata in file "
          + qcReportOutputPath + "/" + getName()
          + fastqSample.getKeyFastqSample() + ".data");

      LOGGER.warning("Error during writing data file for the sample "
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
              saveResultPart(st.getFastqSample(), st.getResults());
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
  public void clear() {

    // Delete temporary uncompress fastq file
    fastqStorage.clear();

    // Delete all data files fastqSample per fastqSample
    for (FastqSample fs : fastqSamples) {

      if (fs.getFastqFiles().size() > 0) {

        File projectDir =
            new File(qcReportOutputPath + "/Project_" + fs.getProjectName());

        File[] dataFiles = projectDir.listFiles(new FileFilter() {

          public boolean accept(final File pathname) {
            return pathname.getName().endsWith(".data");
          }
        });

        if (dataFiles != null && dataFiles.length > 0) {

          for (File f : dataFiles) {
            System.out.println("Delete file " + f.getName());

            if (f.exists())
              if (!f.delete())
                LOGGER.warning("Can not delete data file : "
                    + f.getAbsolutePath());
          }
        }
      }
    }

    // TODO to remove
    // move action to the python code -> rename qc Report directory
    int n = qcReportOutputPath.indexOf("_tmp");
    if (!new File(qcReportOutputPath).renameTo(new File(qcReportOutputPath
        .substring(0, n))))
      LOGGER.warning("Can not rename qc report directory.");

  }

}
