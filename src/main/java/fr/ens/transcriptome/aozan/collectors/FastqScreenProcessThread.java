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
 * @since 0.11
 * @author Sandrine Perrin
 */
class FastqScreenProcessThread extends AbstractFastqProcessThread {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final File reportDir;
  private final FastqScreen fastqscreen;
  private final List<String> genomes;
  private final String genomeSample;
  private final boolean paired;
  private FastqSample fastqSampleR2;

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
          + this.fastqSample.getKeyFastqSample() + " in mode "
          + (paired ? "paired" : "single") + " on genome(s) " + this.genomes
          + " in " + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    }

  }

  @Override
  protected void createReportFile() throws AozanException, IOException {

    String headerReport =
        "FastqScreen : for Projet "
            + fastqSample.getProjectName() + "\nresult for sample : "
            + fastqSample.getSampleName();

    // TODO to remove after test
    System.out.println("\n"
        + this.resultsFastqscreen.statisticalTableToString(headerReport));

    File fastqScreenFile =
        new File(this.reportDir.getAbsolutePath()
            + "/" + this.fastqSample.getKeyFastqSample() + "-fastqscreen.txt");

    BufferedWriter br = Files.newWriter(fastqScreenFile, Charsets.UTF_8);
    br.append(this.resultsFastqscreen.statisticalTableToString(headerReport));
    br.close();

    LOGGER.fine("FASTQSCREEN : for "
        + this.fastqSample.getKeyFastqSample() + " report fastqscreen");
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
    if (this.paired) {
      read2 = new File(fastqStorage.getTemporaryFile(fastqSampleR2));

      if (read2 == null || !read2.exists())
        return;
    }

    // Add read2 in command line
    resultsFastqscreen =
        fastqscreen.execute(read1, read2, fastqSample, genomes, genomeSample,
            paired);

    if (resultsFastqscreen == null)
      throw new AozanException("Fastqscreen return no result for sample "
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
   * @param paired true if a pair-end run else false
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */

  public FastqScreenProcessThread(final FastqSample fastqSampleR1,
      final FastqSample fastqSampleR2, final FastqScreen fastqscreen,
      final List<String> genomes, final String genomeSample,
      final File reportDir, final boolean paired) throws AozanException {

    this(fastqSampleR1, fastqscreen, genomes, genomeSample, reportDir, paired);
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
   * @param paired true if a pair-end run else false
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */
  public FastqScreenProcessThread(final FastqSample fastqSample,
      final FastqScreen fastqscreen, final List<String> genomes,
      final String genomeSample, final File reportDir, final boolean paired)
      throws AozanException {

    super(fastqSample);

    this.fastqSampleR2 = null;
    this.fastqscreen = fastqscreen;
    this.genomeSample = genomeSample;
    this.reportDir = reportDir;
    this.paired = paired;

    // Copy list genome for fastqscreen
    this.genomes = new ArrayList<String>();
    this.genomes.addAll(genomes);

    // Add genomeSample in list of genome to fastqscreen
    if (this.genomeSample.length() > 0
        && !this.genomes.contains(this.genomeSample))
      this.genomes.add(this.genomeSample);

  }
}
