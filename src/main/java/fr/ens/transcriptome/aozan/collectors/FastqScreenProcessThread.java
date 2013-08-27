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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenResult;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * The private class define a class for a thread that execute fastqScreen for a
 * sample. It receive results in rundata and create a report file.
 * @since 1.0
 * @author Sandrine Perrin
 */
class FastqScreenProcessThread extends AbstractFastqProcessThread {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final File reportDir;
  private final FastqScreen fastqscreen;
  private final List<String> genomes;
  private final String genomeSample;
  private final boolean pairedMode;
  private FastqSample fastqSampleR2;
  private final String runId;

  private FastqScreenResult resultsFastqscreen = null;

  @Override
  public void run() {

    // Timer
    final Stopwatch timer = new Stopwatch().start();

    LOGGER.fine("FASTQSCREEN : start for "
        + this.fastqSample.getKeyFastqSample());

    try {
      processResults();
      this.success = true;
    } catch (AozanException e) {
      this.exception = e;
    } finally {

      timer.stop();

      LOGGER.fine("FASTQSCREEN : end for "
          + this.fastqSample.getKeyFastqSample()
          + " in mode "
          + (pairedMode ? "paired" : "single")
          + (this.success
              ? " on genome(s) "
                  + this.genomes + " in "
                  + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS))
              : " with fail."));

    }

  }

  @Override
  protected void createReportFile() throws AozanException, IOException {

    // TODO to remove after test
    System.out.println("\n"
        + this.resultsFastqscreen.reportToCSV(this.fastqSample,
            this.genomeSample));

    String fileName =
        this.reportDir.getAbsolutePath()
            + "/" + this.fastqSample.getKeyFastqSample() + "-fastqscreen";

    writeCSV(fileName);

    // Report with a link in qc html page
    writeHtml(fileName);

    LOGGER.fine("FASTQSCREEN : for "
        + this.fastqSample.getKeyFastqSample() + " report fastqscreen");
  }

  /**
   * Create a report fastqScreen for a sample in csv format.
   * @param fileName name of the report file in csv format
   * @throws IOException if an error occurs during writing file
   */
  private void writeCSV(final String fileName) throws AozanException,
      IOException {

    File file = new File(fileName + ".csv");

    BufferedWriter br = Files.newWriter(file, Charsets.UTF_8);
    br.append(this.resultsFastqscreen.reportToCSV(this.fastqSample,
        this.genomeSample));

    br.close();

  }

  /**
   * Create a report fastqScreen for a sample in html format.
   * @param fileName name of the report file in html format
   * @throws IOException if an error occurs during writing file
   */
  private void writeHtml(final String fileName) throws AozanException,
      IOException {

    File file = new File(fileName + ".html");

    BufferedWriter br = Files.newWriter(file, Charsets.UTF_8);
    br.append(this.resultsFastqscreen.reportToHtml(this.fastqSample,
        this.genomeSample, this.runId));

    br.close();

  }

  @Override
  protected void processResults() throws AozanException {

    // TODO to remove after test
    System.out.println("lane current "
        + this.fastqSample.getLane() + "\tsample current "
        + this.fastqSample.getSampleName() + "\tproject name "
        + this.fastqSample.getProjectName());

    File read1 = new File(fastqStorage.getTemporaryFile(fastqSample));

    if (read1 == null || !read1.exists())
      return;

    File read2 = null;
    // mode paired
    if (this.pairedMode) {
      read2 = new File(fastqStorage.getTemporaryFile(fastqSampleR2));

      if (read2 == null || !read2.exists())
        return;
    }

    // Add read2 in command line
    resultsFastqscreen =
        fastqscreen.execute(read1, read2, fastqSample, genomes, genomeSample,
            pairedMode);

    if (resultsFastqscreen == null)
      throw new AozanException("Fastqscreen returns no result for sample "
          + String.format("/Project_%s/Sample_%s",
              fastqSample.getProjectName(), fastqSample.getSampleName()));

    // Create rundata for the sample
    this.results.put(resultsFastqscreen.createRundata("fastqscreen"
        + fastqSample.getPrefixRundata()));

    try {
      createReportFile();
    } catch (IOException e) {
      throw new AozanException(e);
    }

  }// end method collect

  //
  // Constructor
  //

  /**
   * Public constructor for a thread object collector for FastqScreen in
   * pair-end mode
   * @param fastqSampleR1 fastqSample corresponding to the read 1
   * @param fastqSampleR2 fastqSample corresponding to the read 2
   * @param fastqscreen
   * @param listGenomes list of references genomes for FastqScreen
   * @param genomeSample genome reference corresponding to sample
   * @param reportDir path for the directory who save the FastqScreen report
   * @param paired true if a pair-end run and option paired mode equals true
   *          else false
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */

  public FastqScreenProcessThread(final FastqSample fastqSampleR1,
      final FastqSample fastqSampleR2, final FastqScreen fastqscreen,
      final List<String> genomes, final String genomeSample,
      final File reportDir, final boolean pairedMode, final String runId)
      throws AozanException {

    this(fastqSampleR1, fastqscreen, genomes, genomeSample, reportDir,
        pairedMode, runId);
    this.fastqSampleR2 = fastqSampleR2;
  }

  /**
   * Public constructor for a thread object collector for FastqScreen in
   * single-end mode
   * @param fastqSample fastqSample corresponding to the read 1
   * @param fastqscreen instance of fastqscreen
   * @param listGenomes list of references genomes for FastqScreen
   * @param genomeSample genome reference corresponding to sample
   * @param reportDir path for the directory who save the FastqScreen report
   * @param paired true if a pair-end run and option paired mode equals true
   *          else false
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */
  public FastqScreenProcessThread(final FastqSample fastqSample,
      final FastqScreen fastqscreen, final List<String> genomes,
      final String genomeSample, final File reportDir,
      final boolean pairedMode, final String runId) throws AozanException {

    super(fastqSample);

    this.fastqSampleR2 = null;
    this.fastqscreen = fastqscreen;
    this.genomeSample = genomeSample;
    this.reportDir = reportDir;
    this.pairedMode = pairedMode;
    this.runId = runId;

    // Copy list genome for fastqscreen
    this.genomes = new ArrayList<String>();
    this.genomes.addAll(genomes);

    // Add genomeSample in list of genome to fastqscreen
    if (this.genomeSample.length() > 0
        && !this.genomes.contains(this.genomeSample))
      this.genomes.add(this.genomeSample);

  }
}
