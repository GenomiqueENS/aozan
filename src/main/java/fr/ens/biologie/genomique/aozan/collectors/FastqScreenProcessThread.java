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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors;


import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.fastqscreen.FastqScreen;
import fr.ens.biologie.genomique.aozan.fastqscreen.FastqScreenResult;
import fr.ens.biologie.genomique.aozan.io.FastqSample;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * The private class define a class for a thread that execute fastqScreen for a
 * sample. It receive results in rundata and create a report file.
 * @since 1.0
 * @author Sandrine Perrin
 */
class FastqScreenProcessThread extends AbstractFastqProcessThread {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private final File reportDir;
  private final FastqScreen fastqscreen;
  private final List<String> genomes;
  private final String sampleGenome;
  private final boolean isPairedEndMode;
  private final boolean isRunPE;
  private FastqSample fastqSampleR2;
  private final RunData data;

  private FastqScreenResult resultsFastqscreen = null;
  private File fastqscreenXSLFile = null;

  //
  // AbstractFastqProcessThread methods
  //

  @Override
  protected void logThreadStart() {
    LOGGER
        .fine("FASTQSCREEN: start for " + getFastqSample().getFilenamePrefix());
  }

  @Override
  protected void process() throws AozanException {

    processResults();
  }

  @Override
  protected void logThreadEnd(final String duration) {

    LOGGER.fine("FASTQSCREEN: end for "
        + getFastqSample().getFilenamePrefix() + " in mode "
        + (this.isPairedEndMode ? "paired" : "single")
        + (isSuccess()
            ? " on genome(s) " + this.genomes + " in " + duration
            : " with fail."));

  }

  /**
   * Create the report file.
   * @throws AozanException if an error occurs while processing data
   * @throws IOException if an error occurs while processing data
   */
  private void createReportFile() throws AozanException, IOException {

    final String reportFilename =
        getFastqSample().getFilenamePrefix() + "-fastqscreen";

    final File csvFile = new File(this.reportDir, reportFilename + ".csv");
    final File htmlFile = new File(this.reportDir, reportFilename + ".html");
    final File multiQCLink = new File(this.reportDir,
        getFastqSample().getFilenamePrefix() + "_screen.txt");

    // Create CVS report
    writeCSV(csvFile);

    // Create symbolic link for MultiQC filename handling
    Files.createSymbolicLink(multiQCLink.toPath(), Paths.get(csvFile.getName()));

    // Report with a link in qc html page
    writeHtml(htmlFile);

    // Save the filename of the report in RunData
    String key = "fastqscreen"
        + getFastqSample().getRundataPrefix() + ".report.file.name";
    this.data.put(key, htmlFile.getName());

    // Save the filename of the report in RunData for read 2
    if (this.isRunPE) {
      this.data.put(key.replace(".read1.", ".read2."), htmlFile.getName());
    }

    LOGGER.fine("FASTQSCREEN: save "
        + getFastqSample().getFilenamePrefix() + " report fastqscreen");
  }

  /**
   * Process results after the end of the thread.
   * @throws AozanException if an error occurs while generate FastQ Screen
   *           reports
   */
  private void processResults() throws AozanException {

    final File read1 = getFastqSample().getSubsetFastqFile();
    final String sampleDescription = getFastqSample().getFilenamePrefix();

    if (!read1.exists()) {
      LOGGER.warning("No partial file for " + getFastqSample() + ": " + read1);
      return;
    }

    File read2 = null;
    // mode paired
    if (this.isPairedEndMode) {
      read2 = this.fastqSampleR2.getSubsetFastqFile();

      if (!read2.exists()) {
        return;
      }
    }

    // Add read2 in command line
    this.resultsFastqscreen =
        this.fastqscreen.execute(read1, read2, sampleDescription, this.genomes,
            this.sampleGenome, this.isPairedEndMode);

    if (this.resultsFastqscreen == null) {
      throw new AozanException("Fastqscreen returns no result for sample "
          + String.format("/Project_%s/Sample_%s",
              getFastqSample().getProjectName(),
              getFastqSample().getSampleName()));
    }

    // Create rundata for the sample
    final String prefixR1 = "fastqscreen" + getFastqSample().getRundataPrefix();
    getResults().put(this.resultsFastqscreen.createRundata(prefixR1));

    // Paired-end run : same values for fastqSample R2
    if (this.isRunPE) {

      final String prefixR2 =
          prefixR1.substring(0, prefixR1.length() - 1) + '2';

      getResults().put(this.resultsFastqscreen.createRundata(prefixR2));
    }

    try {
      createReportFile();
    } catch (final IOException e) {
      throw new AozanException(e);
    }

  }

  //
  // Other methods
  //

  /**
   * Create a report fastqScreen for a sample in CSV format.
   * @param file report file in CSV format
   * @throws IOException if an error occurs during writing file
   */
  private void writeCSV(final File file)
      throws AozanException, IOException {

    final Writer br = new FileWriter(file);
    br.write(this.resultsFastqscreen.reportToCSV(getFastqSample(),
        this.sampleGenome));

    br.close();

    // Run paired-end: copy file for read R2
    if (this.isRunPE) {
      final File fileR2 = new File(this.reportDir.getAbsolutePath()
          + "/" + getFastqSample().getFilenamePrefix(2) + "-fastqscreen.csv");

      if (fileR2.exists()) {
        if (!fileR2.delete()) {
          LOGGER.warning(
              "FASTQSCREEN: Fail delete report " + fileR2.getAbsolutePath());
        }
      }

      FileUtils.copyFile(file, fileR2);
    }

  }

  /**
   * Create a report fastqScreen for a sample in html format.
   * @param file report file in html format
   * @throws IOException if an error occurs during writing file
   */
  private void writeHtml(final File file)
      throws AozanException, IOException {

    this.resultsFastqscreen.reportToHtml(getFastqSample(), this.data,
        this.sampleGenome, file, this.fastqscreenXSLFile);

    // Run paired-end: copy file for read R2
    if (this.isRunPE) {
      final File outputReportR2 = new File(this.reportDir.getAbsolutePath()
          + "/" + getFastqSample().getFilenamePrefix(2) + "-fastqscreen.html");

      if (outputReportR2.exists()) {
        if (!outputReportR2.delete()) {
          LOGGER.warning("FASTQSCREEN: Fail delete report "
              + outputReportR2.getAbsolutePath());
        }
      }

      FileUtils.copyFile(file, outputReportR2);
    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor for a thread object collector for FastqScreen in
   * pair-end mode.
   * @param fastqSampleR1 fastqSample corresponding to the read 1
   * @param fastqSampleR2 fastqSample corresponding to the read 2
   * @param fastqscreen instance of fastqscreen
   * @param data object rundata on the run
   * @param genomesToMap list of references genomes for FastqScreen
   * @param genomeSample genome reference corresponding to sample
   * @param reportDir path for the directory who save the FastqScreen report
   * @param isPairedEndMode true if a pair-end run and option paired mode equals
   *          true else false
   * @param isRunPE true if the run is PE else false
   * @param fastqscreenXSLFile xsl file needed to create report html
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */
  public FastqScreenProcessThread(final FastqSample fastqSampleR1,
      final FastqSample fastqSampleR2, final FastqScreen fastqscreen,
      final RunData data, final Set<String> genomesToMap,
      final String genomeSample, final File reportDir,
      final boolean isPairedEndMode, final boolean isRunPE,
      final File fastqscreenXSLFile) throws AozanException {

    this(fastqSampleR1, fastqscreen, data, genomesToMap, genomeSample,
        reportDir, isPairedEndMode, isRunPE, fastqscreenXSLFile);

    if (isPairedEndMode) {
      requireNonNull(fastqSampleR2, "fastqSampleR2 argument cannot be null");
    }

    this.fastqSampleR2 = fastqSampleR2;
  }

  /**
   * Public constructor for a thread object collector for FastqScreen in
   * single-end mode.
   * @param fastqSample fastqSample corresponding to the read 1
   * @param fastqscreen instance of fastqscreen
   * @param data object rundata on the run
   * @param genomes list of references genomes for FastqScreen
   * @param sampleGenome genome reference corresponding to sample
   * @param reportDir path for the directory who save the FastqScreen report
   * @param isPairedEndMode true if a paired-end run and option paired mode
   *          equals true else false
   * @param isRunPE true if the run is PE else false
   * @param fastqscreenXSLFile xsl file needed to create report html
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */
  public FastqScreenProcessThread(final FastqSample fastqSample,
      final FastqScreen fastqscreen, final RunData data,
      final Set<String> genomes, final String sampleGenome,
      final File reportDir, final boolean isPairedEndMode,
      final boolean isRunPE, final File fastqscreenXSLFile)
      throws AozanException {

    super(fastqSample);

    requireNonNull(fastqscreen, "fastqscreen argument cannot be null");
    requireNonNull(data, "data argument cannot be null");
    requireNonNull(genomes, "genomes argument cannot be null");
    requireNonNull(reportDir, "reportDir argument cannot be null");

    this.fastqSampleR2 = null;
    this.fastqscreen = fastqscreen;
    this.sampleGenome = sampleGenome;
    this.reportDir = reportDir;
    this.isPairedEndMode = isPairedEndMode;
    this.isRunPE = isRunPE;
    this.data = data;

    if (fastqscreenXSLFile == null || !fastqscreenXSLFile.exists()) {
      this.fastqscreenXSLFile = null;
    } else {
      this.fastqscreenXSLFile = fastqscreenXSLFile;
    }

    // Copy list genomes names for fastqscreen
    this.genomes = new ArrayList<>(genomes);
  }

}
