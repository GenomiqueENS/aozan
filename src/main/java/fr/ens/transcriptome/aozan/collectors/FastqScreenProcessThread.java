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
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenResult;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * The private class define a class for a thread that execute fastqScreen for a
 * sample. It receive results in rundata and create a report file.
 * @since 1.0
 * @author Sandrine Perrin
 */
class FastqScreenProcessThread extends AbstractFastqProcessThread {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  private final File reportDir;
  private final FastqScreen fastqscreen;
  private final List<String> genomes;
  private final String genomeSample;
  private final boolean isPairedMode;
  private final boolean isRunPE;
  private FastqSample fastqSampleR2;
  private final RunData data;

  private FastqScreenResult resultsFastqscreen = null;
  private File fastqscreenXSLFile = null;

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
          + (isPairedMode ? "paired" : "single")
          + (this.success
              ? " on genome(s) "
                  + this.genomes + " in "
                  + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS))
              : " with fail."));

    }

  }

  @Override
  protected void createReportFile() throws AozanException, IOException {

    final String report =
        this.reportDir.getAbsolutePath()
            + "/" + this.fastqSample.getKeyFastqSample() + "-fastqscreen";

    writeCSV(report);
    // Report with a link in qc html page
    writeHtml(report);

    LOGGER.fine("FASTQSCREEN : save "
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

    // Run paired-end : copy file for read R2
    if (this.isRunPE) {
      File fileR2 =
          new File(this.reportDir.getAbsolutePath()
              + "/" + this.fastqSample.getPrefixRead2() + "-fastqscreen.csv");

      if (fileR2.exists())
        fileR2.delete();

      FileUtils.copyFile(file, fileR2);
    }

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
    br.append(this.resultsFastqscreen.reportToHtml(this.fastqSample, this.data,
        this.genomeSample, this.fastqscreenXSLFile));

    br.close();

    // Run paired-end : copy file for read R2
    if (this.isRunPE) {
      File fileR2 =
          new File(this.reportDir.getAbsolutePath()
              + "/" + this.fastqSample.getPrefixRead2() + "-fastqscreen.html");

      if (fileR2.exists())
        fileR2.delete();

      FileUtils.copyFile(file, fileR2);

    }
  }

  @Override
  protected void processResults() throws AozanException {

    File read1 = new File(fastqStorage.getTemporaryFile(fastqSample));

    if (read1 == null || !read1.exists())
      return;

    File read2 = null;
    // mode paired
    if (this.isPairedMode) {
      read2 = new File(fastqStorage.getTemporaryFile(fastqSampleR2));

      if (read2 == null || !read2.exists())
        return;
    }

    // Add read2 in command line
    resultsFastqscreen =
        fastqscreen.execute(read1, read2, fastqSample, genomes, genomeSample,
            isPairedMode);

    if (resultsFastqscreen == null)
      throw new AozanException("Fastqscreen returns no result for sample "
          + String.format("/Project_%s/Sample_%s",
              fastqSample.getProjectName(), fastqSample.getSampleName()));

    // Create rundata for the sample
    this.results.put(resultsFastqscreen.createRundata("fastqscreen"
        + fastqSample.getPrefixRundata()));

    // run paired : same values for fastqSample R2
    if (this.isRunPE) {

      final String prefixR2 =
          fastqSample.getPrefixRundata().replace(".read1.", ".read2.");

      this.results.put(resultsFastqscreen.createRundata("fastqscreen"
          + prefixR2));
    }

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
   * @param fastqscreen instance of fastqscreen
   * @param data object rundata on the run
   * @param listGenomes list of references genomes for FastqScreen
   * @param genomeSample genome reference corresponding to sample
   * @param reportDir path for the directory who save the FastqScreen report
   * @param paired true if a pair-end run and option paired mode equals true
   *          else false
   * @param isRunPE true if the run is PE else false
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */

  public FastqScreenProcessThread(final FastqSample fastqSampleR1,
      final FastqSample fastqSampleR2, final FastqScreen fastqscreen,
      final RunData data, final List<String> genomes,
      final String genomeSample, final File reportDir,
      final boolean isPairedMode, final boolean isRunPE,
      final File fastqscreenXSLFile) throws AozanException {

    this(fastqSampleR1, fastqscreen, data, genomes, genomeSample, reportDir,
        isPairedMode, isRunPE, fastqscreenXSLFile);
    this.fastqSampleR2 = fastqSampleR2;
  }

  /**
   * Public constructor for a thread object collector for FastqScreen in
   * single-end mode
   * @param fastqSample fastqSample corresponding to the read 1
   * @param fastqscreen instance of fastqscreen
   * @param data object rundata on the run
   * @param listGenomes list of references genomes for FastqScreen
   * @param genomeSample genome reference corresponding to sample
   * @param reportDir path for the directory who save the FastqScreen report
   * @param paired true if a pair-end run and option paired mode equals true
   *          else false
   * @param isRunPE true if the run is PE else false
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */
  public FastqScreenProcessThread(final FastqSample fastqSample,
      final FastqScreen fastqscreen, final RunData data,
      final List<String> genomes, final String genomeSample,
      final File reportDir, final boolean isPairedMode, final boolean isRunPE,
      final File fastqscreenXSLFile) throws AozanException {

    super(fastqSample);

    this.fastqSampleR2 = null;
    this.fastqscreen = fastqscreen;
    this.genomeSample = genomeSample;
    this.reportDir = reportDir;
    this.isPairedMode = isPairedMode;
    this.isRunPE = isRunPE;
    this.data = data;

    if (fastqscreenXSLFile == null || !fastqscreenXSLFile.exists()) {
      this.fastqscreenXSLFile = null;
    } else {
      this.fastqscreenXSLFile = fastqscreenXSLFile;
    }

    // Copy list genome for fastqscreen
    this.genomes = new ArrayList<String>();
    this.genomes.addAll(genomes);

    // Add genomeSample in list of genome to fastqscreen
    if (this.genomeSample != null && !this.genomes.contains(this.genomeSample))
      this.genomes.add(this.genomeSample);

  }
}
