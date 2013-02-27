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

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenResult;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * This class manages the execution of Fastq Screen for a full run according to
 * the properties defined in the configuration file Aozan, which define the list
 * of references genomes.
 * @author Sandrine Perrin
 */
public class FastqScreenCollector extends AbstractFastqCollector {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final String COLLECTOR_NAME = "fastqscreen";
  public static final String KEY_GENOMES = "qc.conf.fastqscreen.genomes";

  // execute in mode mono-threaded
  private static final int numberThreads = 1;

  private FastqScreen fastqscreen;
  private List<String> listGenomes;

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collectors to execute before fastqscreen Collector
   * @return list of names collector
   */
  @Override
  public String[] getCollectorsNamesRequiered() {
    // return new String[] {RunInfoCollector.COLLECTOR_NAME,
    // DesignCollector.COLLECTOR_NAME, FastqSampleCollector.COLLECTOR_NAME};

    String[] defaultCollectorsNamesRequiered =
        super.getCollectorsNamesRequiered();

    String[] collectorsNamesRequiered =
        new String[defaultCollectorsNamesRequiered.length + 1];

    for (int i = 0; i < defaultCollectorsNamesRequiered.length; i++)
      collectorsNamesRequiered[i] = defaultCollectorsNamesRequiered[i];

    // add supplement collector
    collectorsNamesRequiered[collectorsNamesRequiered.length - 1] =
        UncompressFastqCollector.COLLECTOR_NAME;

    return collectorsNamesRequiered;
  }

  /**
   * Configure fastqScreen with properties from file aozan.conf
   * @param properties
   */
  @Override
  public void configure(/* final */Properties properties) {
    System.out.println("fsq configure");

    super.configure(properties);

    this.fastqscreen = new FastqScreen(properties);

    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();

    for (String genome : s.split(properties.getProperty(KEY_GENOMES))) {
      this.listGenomes.add(genome);
    }
  }

  @Override
  public void collectSample(RunData data, final FastqSample fastqSample)
      throws AozanException {
    // System.out.println("fsq collecte " + fastqSample.getProjectName());

    if (!(isExistRunDir && isExistBackupResults(data, fastqSample, true))) {

      FastqScreenResult resultsFastqscreen = null;

      final long startTime = System.currentTimeMillis();

      System.out.println("lane current "
          + fastqSample.getLane() + "\tsample current "
          + fastqSample.getSampleName() + "\tproject name "
          + fastqSample.getProjectName());

      LOGGER.fine("Start test in collector with project "
          + fastqSample.getProjectName() + " sample "
          + fastqSample.getSampleName());

      File read1 = fastqStorage.getTemporaryFile(fastqSample.getFastqFiles());
      if (read1 == null || !read1.exists())
        return;

      File read2 = null;
      if (paired) {
        // mode paired
        // concatenate fastq files of one sample
        read2 = fastqStorage.getTemporaryFile(fastqSample.getFastqFilesRead2());

        if (read2 == null || !read2.exists())
          return;
      }

      // add read2 in command line
      resultsFastqscreen =
          fastqscreen.execute(read1, read2, this.listGenomes,
              fastqSample.getProjectName(), fastqSample.getSampleName());

      if (resultsFastqscreen == null)
        throw new AozanException("Fastqscreen return no result for sample "
            + String.format("/Project_%s/Sample_%s",
                fastqSample.getProjectName(), fastqSample.getSampleName()));

      // update rundata
      processResults(data, resultsFastqscreen, fastqSample);

      fastqStorage.removeTemporaryFastq(read1, read2);

      LOGGER.fine("End test in collector with project "
          + fastqSample.getProjectName() + " sample "
          + fastqSample.getSampleName() + " : "
          + toTimeHumanReadable(System.currentTimeMillis() - startTime));

    }

  }// end method collect

  protected boolean isExistBackupResults(RunData data,
      final FastqSample fastqSample) throws AozanException {

    boolean isExist = false;

    // Verify if results are save in temporary directory
    File qcreportFile =
        new File(qcReportOutputPath
            + "/Project_" + fastqSample.getProjectName() + "/"
            + fastqSample.getKeyFastqSample() + "-fastqscreen.txt");

    // exist = dataFile.exists() && qcreportFile.exists();

    if (!qcreportFile.exists()) {
      isExist = false;

    } else {
      // verify for rundata
      File dataFile =
          new File(qcReportOutputPath
              + "/Project_" + fastqSample.getProjectName() + "/fastqscreen_"
              + fastqSample.getKeyFastqSample() + ".data");

      if (!dataFile.exists()) {

        isExist = false;
      } else {
        System.out.println("verify exists back-up for \n\t"
            + dataFile.getAbsolutePath() + "  " + dataFile.exists() + "\n\t"
            + qcreportFile.getAbsolutePath() + " " + qcreportFile.exists());

        try {
          // Restore results in data
          System.out.print("size rundata " + data.size());
          data.addDataFileInRundata(dataFile);
          System.out.println("\t\t after add data file " + data.size());

          isExist = true;
        } catch (IOException io) {
          isExist = false;

        }
      }
    }
    return isExist;

  }

  /**
   * Process results and update rundata.
   * @param data rundata
   * @param result object fastqScreenResult which contains all values from
   *          fastqscreen of one sample
   * @param fastqSample
   * @throws AozanException if an error occurs while generating FastqScreen
   *           results
   */
  private void processResults(final RunData data,
      final FastqScreenResult result, final FastqSample fastqSample)
      throws AozanException {

    // create a file to save result fastq screen
    final File reportDir =
        new File(qcReportOutputPath
            + "/Project_" + fastqSample.getProjectName());

    if (!reportDir.exists())
      if (!reportDir.mkdirs())
        throw new AozanException("Cannot create report directory: "
            + reportDir.getAbsolutePath());

    // Set the prefix for the run data entries
    String prefix = "fastqscreen" + fastqSample.getPrefixRundata();

    RunData results = result.updateRundata(prefix);

    // Save results in data file
    results.createRunDataFile(reportDir
        + "/fastqscreen_" + fastqSample.getKeyFastqSample() + ".data");

    // Add results in global rundata
    data.put(results);

    createReportFile(fastqSample, result, reportDir);
  }

  /**
   * @param fastqSample
   * @param result
   * @param reportDir
   */
  private void createReportFile(final FastqSample fastqSample,
      final FastqScreenResult result, final File reportDir) {

    System.out.println(result.statisticalTableToString());

    System.out.println("fqScreen save file "
        + reportDir.getAbsolutePath() + "/" + fastqSample.getKeyFastqSample()
        + "-fastqscreen.txt");

    File resultsFastqScreenFile =
        new File(reportDir.getAbsolutePath()
            + "/" + fastqSample.getKeyFastqSample() + "-fastqscreen.txt");

    try {
      FileWriter fr = new FileWriter(resultsFastqScreenFile);

      BufferedWriter br = new BufferedWriter(fr);

      br.append(result.statisticalTableToString());

      br.close();
      fr.close();

    } catch (IOException io) {

      if (resultsFastqScreenFile.exists())
        resultsFastqScreenFile.delete();

    }

    System.out.println("Save result fastqscreen in file : "
        + resultsFastqScreenFile.getAbsolutePath());

    LOGGER.fine("Save result fastqscreen in file : "
        + resultsFastqScreenFile.getAbsolutePath());

  }

  /**
   * Remove temporary files created in temporary directory which is defined in
   * properties of Aozan
   */
  public void clean() {

    LOGGER.fine("Delete fastq file uncompress");

    fastqStorage.clear();
  }

  //
  // Getters & Setters
  //

  public int getNumberThreads() {
    return numberThreads;
  }

  @Override
  public void setNumberThreads(final int numberThreads) {
  }

  //
  // Constructor
  //

  /**
   * Public constructor for FastqScreenCollector
   */
  public FastqScreenCollector() {
    this.listGenomes = new ArrayList<String>();
  }

}
