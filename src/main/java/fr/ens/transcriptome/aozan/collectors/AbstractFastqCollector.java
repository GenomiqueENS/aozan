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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.codec.net.QCodec;

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
  public static final String COLLECTOR_NAME = "";

  public static boolean isExistRunDir = false;

  protected static FastqStorage fastqStorage;
  protected static String casavaOutputPath;
  protected static String qcReportOutputPath;
  protected static String tmpPath;
  protected static boolean paired = false;

  // mode threaded
  private static final int CHECKING_DELAY_MS = 5000;
  private static final int WAIT_SHUTDOWN_MINUTES = 60;

  protected List<AbstractFastqProcessThread> threads;
  protected List<Future<? extends AbstractFastqProcessThread>> futureThreads;
  protected ExecutorService executor;
  private boolean modeMonoThreaded;

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

    qcReportOutputPath =
        properties.getProperty(RunDataGenerator.QC_OUTPUT_DIR) + "_tmp";

    tmpPath = properties.getProperty(RunDataGenerator.TMP_DIR);

    fastqStorage = FastqStorage.getInstance();
    fastqStorage.setTmpDir(casavaOutputPath, tmpPath);

    this.modeMonoThreaded = this.getNumberThreads() == 1;

    System.out.println("Abstract configure  :"
        + casavaOutputPath + "\tCompression Fastq File "
        + fastqStorage.getCompressionExtension());

    if (!modeMonoThreaded)
      configureModeMultiThread(properties);

  }

  /**
   * Configure a multitreaded mode.
   * @param properties for the collector
   */
  public void configureModeMultiThread(final Properties properties) {

    if (properties.containsKey("qc.conf.fastqc.threads")) {

      try {
        int confThreads =
            Integer.parseInt(properties.getProperty("qc.conf.fastqc.threads")
                .trim());
        if (confThreads > 0)
          this.setNumberThreads(confThreads);

      } catch (NumberFormatException e) {
      }

      // Create the list for threads
      this.threads = Lists.newArrayList();
      this.futureThreads = Lists.newArrayList();

      // Create executor service
      this.executor = Executors.newFixedThreadPool(this.getNumberThreads());
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

    fastqStorage.controlPreCollect(data, qcReportOutputPath);

    for (Map.Entry<String, FastqSample> e : fastqStorage.getFastqsSamples()
        .entrySet()) {

      // TODO to remove after text
      if (e.getValue().getFastqFiles() != null
          && e.getValue().getFastqFiles().length > 0) {

        collectSample(data, e.getValue());
      }
    }

    if (!this.modeMonoThreaded && futureThreads.size() > 0) {
      System.out.println("multithread " + futureThreads.size());
      // Wait for threads
      waitThreads(futureThreads, executor);

      // Add results of the threads to the data object
      for (AbstractFastqProcessThread sft : threads)
        data.put(sft.getResults());
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
  public static void clearFactqCollector() {

    // Delete temporary uncompress fastq file
    fastqStorage.clear();

    // Delete all data files fastqSample per fastqSample
    for (Map.Entry<String, FastqSample> e : fastqStorage.getFastqsSamples()
        .entrySet()) {

      // TODO to remove after tests
      if (e.getValue().getFastqFiles().length > 0) {

        File projectDir =
            new File(qcReportOutputPath
                + "/Project_" + e.getValue().getProjectName());

        File[] dataFiles = projectDir.listFiles(new FileFilter() {

          public boolean accept(final File pathname) {
            return pathname.getName().endsWith(".data");
          }
        });

        System.out.println("dir "
            + projectDir.getAbsolutePath() + " nb data files "
            + dataFiles.length);

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

    // Rename qc Report directory
    int n = qcReportOutputPath.indexOf("_tmp");
    if (!new File(qcReportOutputPath).renameTo(new File(qcReportOutputPath
        .substring(0, n))))
      LOGGER.warning("Can not rename qc report directory.");

  }

  /**
   * Check if the data file and the report exists for the fastqSample.
   * @param data
   * @param fastqSample
   * @param isRestoredData true if the data file must update data
   * @return true if all files are exists else false
   * @throws AozanException if an error occurs while execution
   */
  protected boolean isExistBackupResults(RunData data,
      final FastqSample fastqSample, final boolean isRestoredData)
      throws AozanException {

    boolean isExist = false;

    // Check if results are save in temporary directory
    File qcreportFile =
        new File(qcReportOutputPath
            + "/Project_" + fastqSample.getProjectName() + "/"
            + fastqSample.getKeyFastqSample() + "-" + getName());

    if (!qcreportFile.exists() || !qcreportFile.isDirectory()) {
      isExist = false;

      System.out.println("verify exists back-up for \n\t"
          + qcreportFile.getAbsolutePath() + " " + qcreportFile.exists());
    } else {

      // Check for data file
      File dataFile =
          new File(qcReportOutputPath
              + "/Project_" + fastqSample.getProjectName() + "/" + getName()
              + "_" + fastqSample.getKeyFastqSample() + ".data");

      System.out.println("verify exists back-up for \n\t"
          + dataFile.getAbsolutePath() + "  " + dataFile.exists());

      if (!dataFile.exists()) {

        isExist = false;
      } else {

        try {
          // Restore results in data
          System.out.print("size rundata " + data.size());
          data.addDataFileInRundata(dataFile);
          System.out.println("\t\t add data file " + data.size());

          isExist = true;
        } catch (IOException io) {
          isExist = false;

        }
      }
    }
    return isExist;
  }

  /**
   * Collect data for a fastqSample
   * @param data
   * @param fastqSample
   * @throws AozanException if an error occurs while execution
   */
  abstract public void collectSample(final RunData data,
      final FastqSample fastqSample) throws AozanException;

  abstract public int getNumberThreads();

  abstract public void setNumberThreads(final int numberThreads);

}
