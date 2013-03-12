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
import java.util.List;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenResult;
import fr.ens.transcriptome.aozan.io.FastqSample;

public class FastqScreenProcessThread extends AbstractFastqProcessThread {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final File reportDir;
  private final FastqScreen fastqscreen;
  private final List<String> listGenomes;
  private final String genomeSample;
  private final boolean paired;
  private FastqSample fastqSampleR2;

  private FastqScreenResult resultsFastqscreen = null;

  @Override
  public void run() {
    try {
      processResults();
      success = true;
    } catch (AozanException e) {
      exception = e;
    }
  }

  @Override
  protected void createReportFile() throws AozanException, IOException {

    System.out.println(resultsFastqscreen.statisticalTableToString());

    File fastqScreenFile =
        new File(reportDir.getAbsolutePath()
            + "/" + fastqSample.getKeyFastqSample() + "-fastqscreen.txt");

    try {
      BufferedWriter br = new BufferedWriter(new FileWriter(fastqScreenFile));
      br.append(resultsFastqscreen.statisticalTableToString());
      br.close();

    } catch (IOException io) {

      if (fastqScreenFile.exists())
        fastqScreenFile.delete();

    }

    LOGGER.fine("Save result fastqscreen in file : "
        + fastqScreenFile.getAbsolutePath());

  }

  @Override
  protected void processResults() throws AozanException {

    final long startTime = System.currentTimeMillis();

    System.out.println("lane current "
        + fastqSample.getLane() + "\tsample current "
        + fastqSample.getSampleName() + "\tproject name "
        + fastqSample.getProjectName());

    LOGGER.fine("Start test in collector with project "
        + fastqSample.getProjectName() + " sample "
        + fastqSample.getSampleName());

    File read1 = new File(fastqStorage.getTemporaryFile(fastqSample));

    if (read1 == null || !read1.exists())
      return;

    File read2 = null;
    // mode paired
    if (paired) {

      read2 = new File(fastqStorage.getTemporaryFile(fastqSampleR2));

      if (read2 == null || !read2.exists())
        return;
    }

    // add read2 in command line
    resultsFastqscreen =
        fastqscreen.execute(read1, read2, listGenomes,
            fastqSample.getProjectName(), fastqSample.getSampleName(),
            genomeSample);

    if (resultsFastqscreen == null)
      throw new AozanException("Fastqscreen return no result for sample "
          + String.format("/Project_%s/Sample_%s",
              fastqSample.getProjectName(), fastqSample.getSampleName()));

    // create rundata for the sample
    this.results.put(resultsFastqscreen.createRundata("fastqscreen"
        + fastqSample.getPrefixRundata()));

    LOGGER.fine("End test in collector with project "
        + fastqSample.getProjectName() + " sample "
        + fastqSample.getSampleName() + " : "
        + toTimeHumanReadable(System.currentTimeMillis() - startTime));

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
   * @param reportDir path for the directory who save the FastqScreen report
   * @param paired true if a pair-end run else false
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */

  public FastqScreenProcessThread(final FastqSample fastqSampleR1,
      final FastqSample fastqSampleR2, final FastqScreen fastqscreen,
      final List<String> listGenomes, final String genomeSample,
      final File reportDir, final boolean paired) throws AozanException {

    this(fastqSampleR1, fastqscreen, listGenomes, genomeSample, reportDir,
        paired);

    this.fastqSampleR2 = fastqSampleR2;

  }

  /**
   * Public constructor for a thread object collector for FastqScreen in
   * single-end mode
   * @param fastqSample fastqSample corresponding to the read 1
   * @param fastqscreen
   * @param listGenomes list of references genomes for FastqScreen
   * @param reportDir path for the directory who save the FastqScreen report
   * @param paired true if a pair-end run else false
   * @throws AozanException if an error occurs during create thread, if no fastq
   *           file was found
   */
  public FastqScreenProcessThread(final FastqSample fastqSample,
      final FastqScreen fastqscreen, final List<String> listGenomes,
      final String genomeSample, final File reportDir, final boolean paired)
      throws AozanException {

    super(fastqSample);

    this.fastqSampleR2 = null;
    this.fastqscreen = fastqscreen;
    this.listGenomes = listGenomes;
    this.genomeSample = genomeSample;
    this.reportDir = reportDir;
    this.paired = paired;

  }

}
