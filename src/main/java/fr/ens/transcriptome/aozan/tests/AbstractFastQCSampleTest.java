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

package fr.ens.transcriptome.aozan.tests;

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastQCCollector;

/**
 * This class define an abstract FastQC test.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractFastQCSampleTest extends AbstractSampleTest {

  @Override
  public TestResult test(final RunData data, final int read,
      final int readSample, final int lane, final String sampleName) {

    if (sampleName == null)
      return new TestResult("NA");

    final String prefixKey =
        "fastqc.lane"
            + lane + ".sample." + sampleName + ".read" + read + "."
            + sampleName + "."
            + getQCModuleName().replace(' ', '.').toLowerCase();

    final boolean errorRaise = data.getBoolean(prefixKey + ".error");
    final boolean warningRaise = data.getBoolean(prefixKey + ".warning");

    final int score = errorRaise ? 0 : (warningRaise ? 4 : 9);

    // Get HTML report URL
    final String projectName =
        data.get("design.lane" + lane + "." + sampleName + ".sample.project");
    final String index =
        data.get("design.lane" + lane + "." + sampleName + ".index");
    final String dirname =
        String.format("%s_%s_L%03d_R%d_001-fastqc", sampleName,
            "".equals(index) ? "NoIndex" : index, lane, read);
    final String url =
        "Project_"
            + projectName + "/" + dirname + "/fastqc_report.html#M"
            + getHTMLAnchorIndex();

    return new TestResult(score, url, "url");
  }

  @Override
  public String[] getCollectorsNamesRequiered() {

    return new String[] {FastQCCollector.COLLECTOR_NAME};
  }

  /**
   * Get the name of the FastQC module.
   * @return the name of the FastQC module
   */
  protected abstract String getQCModuleName();

  /**
   * Get the anchor index of for the module in FastQC HTML report.
   * @return the position anchor
   */
  protected abstract int getHTMLAnchorIndex();

  //
  // Constructor
  //

  /**
   * Constructor that set the field of this abstract test.
   * @param name name of the test
   * @param description description of the test
   * @param columnName column name of the test
   */
  protected AbstractFastQCSampleTest(final String name,
      final String description, final String columnName) {

    super(name, description, columnName);
  }

}
