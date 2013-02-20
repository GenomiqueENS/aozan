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
import java.util.Properties;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;

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

  }

  public void collectSample(RunData data, final FastqSample fastqSample)
      throws AozanException {

    // Process sample FASTQ(s)
    final SeqFileThread sft = processFile(data, fastqSample);

    if (sft != null) {
      System.out.println("fsc collect sample "
          + fastqSample.getProjectName() + "  nb thread " + getNumberThreads());

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
      final FastqSample fastqSample) throws AozanException {

    final File[] fastqFiles = fastqSample.getFastqFiles();

    if (fastqFiles == null || fastqFiles.length == 0) {
      return null;
    }

    // Create the thread object
    return new SeqFileThread(fastqSample, this.ignoreFilteredSequences,
        this.qcReportOutputPath);
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
