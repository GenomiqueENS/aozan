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
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.TableModel;
import javax.xml.stream.XMLStreamException;

import uk.ac.babraham.FastQC.Modules.AbstractQCModule;
import uk.ac.babraham.FastQC.Modules.AdapterContent;
import uk.ac.babraham.FastQC.Modules.BasicStats;
import uk.ac.babraham.FastQC.Modules.KmerContent;
import uk.ac.babraham.FastQC.Modules.NContent;
import uk.ac.babraham.FastQC.Modules.OverRepresentedSeqs;
import uk.ac.babraham.FastQC.Modules.PerBaseQualityScores;
import uk.ac.babraham.FastQC.Modules.PerBaseSequenceContent;
import uk.ac.babraham.FastQC.Modules.PerSequenceGCContent;
import uk.ac.babraham.FastQC.Modules.PerSequenceQualityScores;
import uk.ac.babraham.FastQC.Modules.PerTileQualityScores;
import uk.ac.babraham.FastQC.Modules.QCModule;
import uk.ac.babraham.FastQC.Modules.SequenceLengthDistribution;
import uk.ac.babraham.FastQC.Report.HTMLReportArchive;
import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.fastqc.BadTiles;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.aozan.io.FastqStorage;

/**
 * This private class define a class for a thread that read fastq file for
 * FastQC modules.
 * @since 1.0
 * @author Laurent Jourdren
 */
class FastQCProcessThread extends AbstractFastqProcessThread {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private final SequenceFile seqFile;
  private final boolean ignoreFilteredSequences;
  private final List<AbstractQCModule> moduleList;
  private final File reportDir;

  @Override
  protected void notifyStartLogger() {
    LOGGER.fine("FASTQC: start for " + getFastqSample().getKeyFastqSample());
  }

  @Override
  protected void process() throws AozanException {

    processSequences(this.seqFile);
  }

  @Override
  protected void notifyEndLogger(final String duration) {

    LOGGER.fine("FASTQC: end for "
        + getFastqSample().getKeyFastqSample() + " in " + duration);
  }

  /**
   * Read FASTQ file and process the data by FastQC modules.
   * @param seqFile input file
   * @throws AozanException if an error occurs while processing file
   */
  protected void processSequences(final SequenceFile seqFile)
      throws AozanException {

    final boolean ignoreFiltered = this.ignoreFilteredSequences;
    final List<AbstractQCModule> modules = this.moduleList;

    try {

      while (seqFile.hasNext()) {

        final Sequence seq = seqFile.next();

        for (final QCModule module : modules) {

          if (ignoreFiltered && module.ignoreFilteredSequences()) {
            continue;
          }

          module.processSequence(seq);
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
   * Get the number of reads in the fastq file(s).
   * @param bs BasicStats module
   * @return the number of reads in the fastq file(s)
   * @throws AozanException if the implementation of BasicStat has changed and
   *           is incompatible this method
   */
  private static int getClusterNumberFromBasicStatsModule(final BasicStats bs)
      throws AozanException {

    try {

      final JScrollPane jsp =
          (JScrollPane) bs.getResultsPanel().getComponent(1);
      final JViewport jvp = (JViewport) jsp.getComponent(0);
      final TableModel tm = ((JTable) jvp.getComponent(0)).getModel();

      final String value = (String) tm.getValueAt(3, 1);

      if (value == null) {
        throw new AozanException(
            "The results panel of Basic Stats FastQC module has changed."
                + " Update Aozan code to handle this.");
      }

      return Integer.parseInt(value);

    } catch (final ClassCastException e) {

      throw new AozanException(
          "The results panel of Basic Stats FastQC module has changed."
              + " Update Aozan code to handle this.");
    } catch (final NumberFormatException e) {
      throw new AozanException(
          "The results panel of Basic Stats FastQC module has changed."
              + " Update Aozan code to handle this.");
    }

  }

  /**
   * Process results after the end of the thread.
   * @throws AozanException if an error occurs while generate FastQC reports
   */
  @Override
  protected void processResults() throws AozanException {

    // Set the prefix for the run data entries
    final String prefix = "fastqc" + getFastqSample().getPrefixRundata();

    int nClusters = -1;

    // Fill the run data object
    for (final QCModule module : this.moduleList) {

      final String keyPrefix = prefix + "." + module.name().replace(' ', '.');

      if (module instanceof BasicStats) {

        nClusters = getClusterNumberFromBasicStatsModule((BasicStats) module);

        // If no read read don't go further
        if (nClusters == 0) {
          break;
        }
      }

      getResults().put(keyPrefix + ".error", module.raisesError());
      getResults().put(keyPrefix + ".warning", module.raisesWarning());

    }

    // Create report
    if (nClusters > 0) {
      try {
        createReportFile();
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
  @Override
  protected void createReportFile() throws AozanException, IOException {

    // Set the name of the prefix of the report file
    final String filename = getFastqSample().getPrefixReport() + "-fastqc.html";

    final File reportFile = new File(this.reportDir, filename);

    try {
      new HTMLReportArchive(this.seqFile,
          this.moduleList.toArray(new QCModule[] {}), reportFile);

    } catch (final XMLStreamException e) {
      throw new AozanException(e);
    }

    LOGGER.fine("FASTQC: create the html QC report for "
        + getFastqSample().getPrefixReport());

    // Keep only the uncompressed data
    if (reportFile.exists()) {

      if (!reportFile.delete()) {
        LOGGER.warning(
            "FastQC: fail to delete report " + reportFile.getAbsolutePath());
      }
    }

    // Remove zip file
    final File reportZip =
        new File(this.reportDir, filename.replaceAll("\\.html$", ".zip"));
    if (reportZip.exists()) {

      if (!reportZip.delete()) {
        LOGGER.warning(
            "FastQC: fail to delete report " + reportZip.getAbsolutePath());
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
      final FastqStorage fastqStorage, final boolean ignoreFilteredSequences,
      final File reportDir) throws AozanException {

    super(fastqSample, fastqStorage);

    this.ignoreFilteredSequences = ignoreFilteredSequences;
    this.reportDir = reportDir;

    try {
      this.seqFile = SequenceFactory.getSequenceFile(fastqSample.getFastqFiles()
          .toArray(new File[fastqSample.getFastqFiles().size()]));

    } catch (final IOException io) {
      throw new AozanException(io);

    } catch (final SequenceFormatException e) {
      throw new AozanException(e);
    }

    // Define modules list
    final OverRepresentedSeqs os = new OverRepresentedSeqs();

    this.moduleList = Lists.newArrayList(new BasicStats(),
        new PerBaseQualityScores(), new PerTileQualityScores(),
        new PerSequenceQualityScores(), new PerBaseSequenceContent(),
        new PerSequenceGCContent(), new NContent(),
        new SequenceLengthDistribution(), os.duplicationLevelModule(), os,
        new AdapterContent(), new KmerContent(), new BadTiles());
  }

}
