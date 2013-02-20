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

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.TableModel;

import uk.ac.bbsrc.babraham.FastQC.Modules.BasicStats;
import uk.ac.bbsrc.babraham.FastQC.Modules.KmerContent;
import uk.ac.bbsrc.babraham.FastQC.Modules.NContent;
import uk.ac.bbsrc.babraham.FastQC.Modules.OverRepresentedSeqs;
import uk.ac.bbsrc.babraham.FastQC.Modules.PerBaseGCContent;
import uk.ac.bbsrc.babraham.FastQC.Modules.PerBaseQualityScores;
import uk.ac.bbsrc.babraham.FastQC.Modules.PerBaseSequenceContent;
import uk.ac.bbsrc.babraham.FastQC.Modules.PerSequenceGCContent;
import uk.ac.bbsrc.babraham.FastQC.Modules.PerSequenceQualityScores;
import uk.ac.bbsrc.babraham.FastQC.Modules.QCModule;
import uk.ac.bbsrc.babraham.FastQC.Modules.SequenceLengthDistribution;
import uk.ac.bbsrc.babraham.FastQC.Report.HTMLReportArchive;
import uk.ac.bbsrc.babraham.FastQC.Sequence.Sequence;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.bbsrc.babraham.FastQC.Sequence.SequenceFormatException;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqc.BadTiles;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.aozan.io.FastqStorage;

/**
 * This private class define a class for a thread that read fastq file for
 * FastQC modules.
 * @author Laurent Jourdren
 */
public class SeqFileThread extends AbstractFastqProcessThread {

  private final SequenceFile seqFile;
  private final String firstFastqFileName;
  private final boolean ignoreFilteredSequences;
  private final List<QCModule> moduleList;
  private final String qcReportOutputPath;
  private final String compressionExtension;

  private final FastqStorage fastqStorage;

  @Override
  public void run() {

    System.out.println("seqFileThread run ");

    try {
      processSequences(this.seqFile);
      success = true;
    } catch (AozanException e) {
      exception = e;
    }
  }

  /**
   * Read FASTQ file and process the data by FastQC modules
   * @param seqFile input file
   * @throws AozanException if an error occurs while processing file
   */
  private void processSequences(final SequenceFile seqFile)
      throws AozanException {

    final boolean ignoreFiltered = this.ignoreFilteredSequences;
    final List<QCModule> modules = this.moduleList;

    try {

      while (seqFile.hasNext()) {

        final Sequence seq = seqFile.next();

        for (final QCModule module : modules) {

          if (ignoreFiltered && module.ignoreFilteredSequences())
            continue;

          module.processSequence(seq);
        }
      }

      // Process results
      processResults();

      // Keep module data is now unnecessary
      this.moduleList.clear();

    } catch (SequenceFormatException e) {
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
  private static final int getClusterNumberFromBasicStatsModule(
      final BasicStats bs) throws AozanException {

    try {

      final JScrollPane jsp =
          (JScrollPane) bs.getResultsPanel().getComponent(1);
      final JViewport jvp = (JViewport) jsp.getComponent(0);
      final TableModel tm = ((JTable) jvp.getComponent(0)).getModel();

      final String value = (String) tm.getValueAt(3, 1);

      if (value == null)
        throw new AozanException(
            "The results panel of Basic Stats FastQC module has changed."
                + " Update Aozan code to handle this.");

      return Integer.parseInt(value);

    } catch (ClassCastException e) {

      e.printStackTrace();

      throw new AozanException(
          "The results panel of Basic Stats FastQC module has changed."
              + " Update Aozan code to handle this.");
    } catch (NumberFormatException e) {
      throw new AozanException(
          "The results panel of Basic Stats FastQC module has changed."
              + " Update Aozan code to handle this.");
    }

  }

  /**
   * Process results after the end of the thread.
   * @throws AozanException if an error occurs while generate FastQC reports
   */
  private void processResults() throws AozanException {

    // Set the prefix for the run data entries
    final String prefix = "fastqc.lane" + this.fastqSample.getPrefixRundata();

    int nClusters = -1;

    // Fill the run data object
    for (final QCModule module : this.moduleList) {

      final String keyPrefix = prefix + "." + module.name().replace(' ', '.');

      if (module instanceof BasicStats) {

        nClusters = getClusterNumberFromBasicStatsModule((BasicStats) module);

        // If no read read don't go further
        if (nClusters == 0)
          break;
      }

      this.results.put(keyPrefix + ".error", module.raisesError());
      this.results.put(keyPrefix + ".warning", module.raisesWarning());
    }

    // Create report
    if (nClusters > 0)
      try {
        createReportFile();
      } catch (IOException e) {
        throw new AozanException(e);
      }

  }

  /**
   * Create the report file.
   * @throws AozanException if an error occurs while processing data
   * @throws IOException if an error occurs while processing data
   */
  private void createReportFile() throws AozanException, IOException {

    final File reportDir =
        new File(qcReportOutputPath
            + "/Project_" + this.fastqSample.getProjectName());

    if (!reportDir.exists())
      if (!reportDir.mkdirs())
        throw new AozanException("Cannot create report directory: "
            + reportDir.getAbsolutePath());

    // Set the name of the prefix of the report file
    final String filename =
        this.firstFastqFileName.substring(0, this.firstFastqFileName.length()
            - compressionExtension.length() - 1);

    final File reportFile = new File(reportDir, filename + "-fastqc.zip");

    // Force unzip of the report
    System.setProperty("fastqc.unzip", "true");

    new HTMLReportArchive(seqFile, this.moduleList.toArray(new QCModule[] {}),
        reportFile);

    // Keep only the uncompressed data
    if (reportFile.exists())
      reportFile.delete();
  }

  //
  // Constructor
  //

  /**
   * Thread constructor.
   * @throws AozanException if an error occurs while creating sequence file for
   *           FastQC
   */
  public SeqFileThread(final FastqSample fastqSample,
      final boolean ignoreFilteredSequences, final String qcReportOutputPath)
      throws AozanException {

    super(fastqSample);

    this.firstFastqFileName = this.fastqSample.getFastqFiles()[0].getName();
    this.ignoreFilteredSequences = ignoreFilteredSequences;
    this.qcReportOutputPath = qcReportOutputPath;

    this.fastqStorage = FastqStorage.getInstance();

    this.seqFile =
        this.fastqStorage.getSequenceFile(this.fastqSample.getFastqFiles());

    this.compressionExtension = this.fastqStorage.getCompressionExtension();

    // Define modules list
    final OverRepresentedSeqs os = new OverRepresentedSeqs();

    this.moduleList =
        Lists.newArrayList(new BasicStats(), new PerBaseQualityScores(),
            new PerSequenceQualityScores(), new PerBaseSequenceContent(),
            new PerBaseGCContent(), new PerSequenceGCContent(), new NContent(),
            new SequenceLengthDistribution(), os.duplicationLevelModule(), os,
            new KmerContent(), new BadTiles());

  }

}
