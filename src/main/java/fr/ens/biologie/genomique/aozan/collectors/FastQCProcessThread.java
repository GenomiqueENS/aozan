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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Aozan2Logger;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.io.FastqSample;
import uk.ac.babraham.FastQC.Modules.ModuleFactory;
import uk.ac.babraham.FastQC.Modules.QCModule;
import uk.ac.babraham.FastQC.Report.HTMLReportArchive;
import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

/**
 * This private class define a class for a thread that read fastq file for
 * FastQC modules.
 * @since 1.0
 * @author Laurent Jourdren
 */
class FastQCProcessThread extends AbstractFastqProcessThread {

  /** Logger. */
  private static final Logger LOGGER = Aozan2Logger.getLogger();

  private final SequenceFile seqFile;
  private final boolean ignoreFilteredSequences;
  private final List<QCModule> moduleList;
  private final File reportDir;
  private final boolean keepZipReportFile;

  private int processedReads;

  @Override
  protected void logThreadStart() {
    LOGGER.fine("FASTQC: start for " + getFastqSample().getFilenamePrefix());
  }

  @Override
  protected void process() throws AozanException {

    processSequences(this.seqFile);
  }

  @Override
  protected void logThreadEnd(final String duration) {

    LOGGER.fine("FASTQC: end for "
        + getFastqSample().getFilenamePrefix() + " in " + duration);
  }

  /**
   * Read FASTQ file and process the data by FastQC modules.
   * @param seqFile input file
   * @throws AozanException if an error occurs while processing file
   */
  protected void processSequences(final SequenceFile seqFile)
      throws AozanException {

    final boolean ignoreFiltered = this.ignoreFilteredSequences;
    final List<QCModule> modules = this.moduleList;
    this.processedReads = 0;

    // Reset modules
    for (final QCModule module : modules) {
      module.reset();
    }

    try {

      while (seqFile.hasNext()) {

        final Sequence seq = seqFile.next();

        boolean processed = false;

        for (final QCModule module : modules) {

          if (ignoreFiltered && module.ignoreFilteredSequences()) {
            continue;
          }
          processed = true;
          module.processSequence(seq);
        }
        if (processed) {
          this.processedReads++;
        }
      }

      // Process results
      processResults();

      // Keep module data is now unnecessary
      this.moduleList.clear();

    } catch (final SequenceFormatException e) {
      throw new AozanException(e);
    }

  }

  /**
   * Process results after the end of the thread.
   * @throws AozanException if an error occurs while generate FastQC reports
   */
  private void processResults() throws AozanException {

    // Set the prefix for the run data entries
    final String prefix = "fastqc" + getFastqSample().getRundataPrefix();

    if (this.processedReads > 0) {

      // Fill the run data object
      for (final QCModule module : this.moduleList) {

        if (module.ignoreInReport()) {
          continue;
        }

        final String keyPrefix = prefix + "." + module.name().replace(' ', '.');

        getResults().put(keyPrefix + ".error", module.raisesError());
        getResults().put(keyPrefix + ".warning", module.raisesWarning());
      }

      // Create report
      try {
        createReportFile(prefix);
      } catch (final IOException e) {
        throw new AozanException(e);
      }
    }

  }

  /**
   * Create the report file.
   * @throws AozanException if an error occurs while processing data
   * @throws IOException if an error occurs while processing data
   */
  private void createReportFile(final String keyPrefix) throws AozanException, IOException {

    // Set the name of the prefix of the report file
    final String filename = getFastqSample().getFilenamePrefix() + "-fastqc.html";

    final File reportFile = new File(this.reportDir, filename);

    // Save the filename of the report in RunData
    getResults().put(keyPrefix + ".report.file.name",
        getFastqSample().getFilenamePrefix() + "-fastqc");

    try {
      new HTMLReportArchive(this.seqFile,
          this.moduleList.toArray(new QCModule[this.moduleList.size()]),
          reportFile);

    } catch (final XMLStreamException e) {
      throw new AozanException(e);
    }

    LOGGER.fine("FASTQC: create the html QC report for "
        + getFastqSample().getFilenamePrefix());

    // Remove zip file
    final File reportZip =
        new File(this.reportDir, filename.replaceAll("\\.html$", ".zip"));
    if (reportZip.exists()) {

      if (!this.keepZipReportFile) {

        if (!reportZip.delete()) {
          LOGGER.warning(
              "FASTQC: fail to delete report " + reportZip.getAbsolutePath());
        }
      } else {

        // Create a symbolic link to the zip file for MultiQC
        File multiQCLink = new File(reportZip.getParentFile(),
            reportZip.getName().replace("-fastqc.zip", "_fastqc.zip"));

        Files.createSymbolicLink(multiQCLink.toPath(),
            Paths.get(reportZip.getName()));

      }
    }
  }

  //
  // Constructor
  //

  /**
   * Thread constructor.
   * @throws AozanException if an error occurs while creating sequence file for
   *           FastQC
   */
  public FastQCProcessThread(final FastqSample fastqSample,
      final boolean ignoreFilteredSequences, final File reportDir,
      final boolean keepZipReportFile) throws AozanException {

    super(fastqSample);

    this.ignoreFilteredSequences = ignoreFilteredSequences;
    this.reportDir = reportDir;
    this.keepZipReportFile = keepZipReportFile;

    try {
      this.seqFile = SequenceFactory.getSequenceFile(fastqSample.getFastqFiles()
          .toArray(new File[fastqSample.getFastqFiles().size()]));

    } catch (final IOException | SequenceFormatException e) {
      throw new AozanException(e);
    }

    // Define modules list
    this.moduleList = Lists.newArrayList(ModuleFactory.getStandardModuleList());
  }

}
