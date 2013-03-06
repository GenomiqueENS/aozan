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
  private final boolean paired;

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

    // final File reportDir =
    // new File(qcReportOutputPath
    // + "/Project_" + fastqSample.getProjectName());
    //
    // if (!reportDir.exists())
    // if (!reportDir.mkdirs())
    // throw new AozanException("Cannot create report directory: "
    // + reportDir.getAbsolutePath());

    System.out.println(resultsFastqscreen.statisticalTableToString());

    System.out.println("fqScreen save file "
        + reportDir.getAbsolutePath() + "/" + fastqSample.getKeyFastqSample()
        + "-fastqscreen.txt");

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

    System.out.println("Save result fastqscreen in file : "
        + fastqScreenFile.getAbsolutePath());

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
    System.out.println("read1 " + read1.getName());

    if (read1 == null || !read1.exists())
      return;

    File read2 = null;
    if (paired) {
      // mode paired
      // concatenate fastq files of one sample
      read2 = new File(fastqStorage.getTemporaryFile(fastqSample));

      if (read2 == null || !read2.exists())
        return;
    }

    // add read2 in command line
    resultsFastqscreen =
        fastqscreen.execute(read1, read2, listGenomes,
            fastqSample.getProjectName(), fastqSample.getSampleName());

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

  public FastqScreenProcessThread(final FastqSample fastqSample,
      final FastqScreen fastqscreen, final List<String> listGenomes,
      final File reportDir, final boolean paired) throws AozanException {

    super(fastqSample);

    this.fastqscreen = fastqscreen;
    this.listGenomes = listGenomes;
    this.reportDir = reportDir;
    this.paired = paired;

  }

}
