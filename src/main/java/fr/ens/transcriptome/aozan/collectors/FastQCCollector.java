/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.RunDataGenerator;
import fr.ens.transcriptome.aozan.fastqc.BadTiles;
import fr.ens.transcriptome.aozan.io.FastqStorage;
import fr.ens.transcriptome.aozan.io.SequenceFileAozan;

/**
 * This class define a FastQC Collector
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FastQCCollector extends AbstractFastqCollector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "fastqc";

  private int numberThreads = Runtime.getRuntime().availableProcessors();

  private boolean ignoreFilteredSequences = false;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public String[] getCollectorsNamesRequiered() {
    return super.getCollectorsNamesRequiered();
  }

  @Override
  public void configure(final Properties properties) {

    super.configure(properties);

    System.out.println("fsqC configure");

    System.setProperty("java.awt.headless", "true");
    System.setProperty("fastqc.unzip", "true");

  }

  @Override
  public void collectSample(RunData data, final int read, final int lane,
      final String projectName, final String sampleName, final String index,
      final int readSample) throws AozanException {

    // Process sample FASTQ(s)
    final SeqFileThread sft =
        processFile(data, projectName, sampleName, index, lane, readSample);

    if (sft != null) {
      System.out.println("fsc collect sample "
          + projectName + "  nb thread " + getNumberThreads());
      threads.add(sft);
      futureThreads.add(executor.submit(sft, sft));
    }
  }

  /**
   * Process a FASTQ file.
   * @param data Run data
   * @param projectName name fo the project
   * @param sampleName name of the sample
   * @param index sequence index
   * @param lane lane number
   * @param read read number
   * @throws AozanException if an error occurs while processing a FASTQ file
   */
  public SeqFileThread processFile(final RunData data,
      final String projectName, final String sampleName, final String index,
      final int lane, final int read) throws AozanException {

    final File[] fastqFiles =
        fastqStorage.createListFastqFiles(casavaOutputPath, read, lane,
            projectName, sampleName, index);

    if (fastqFiles == null || fastqFiles.length == 0) {
      return null;
    }

    // Create the thread object
    return new SeqFileThread(projectName, sampleName, lane, read, fastqFiles,
        this.ignoreFilteredSequences, this.qcReportOutputPath, this.tmpPath);
  }

  //
  // Getters & Setters
  //

  public int getNumberThreads() {
    return this.numberThreads;
  }

  @Override
  public void setNumberThreads(final int numberThreads) {
    this.numberThreads = numberThreads;
  }
}
