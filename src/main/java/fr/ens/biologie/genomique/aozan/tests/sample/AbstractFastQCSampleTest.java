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
 *      http://tools.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.tests.sample;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.FastQCCollector;
import fr.ens.biologie.genomique.aozan.tests.TestResult;

/**
 * This class define an abstract FastQC test.
 * @since 0.8
 * @author Laurent Jourdren
 */
public abstract class AbstractFastQCSampleTest extends AbstractSampleTest {

  @Override
  public TestResult test(final RunData data, final int read,
      final int readSample, final int lane, final String sampleName) {

    // Check indetermined indexed sample
    if (sampleName == null) {
      // return new TestResult("NA");
      final String projectName = "Undetermined_indices";

      final String dirname =
          String.format("lane%s_Undetermined_L%03d_R%d_001-fastqc", lane, lane,
              readSample);

      final String url =
          projectName
              + "/" + dirname + "/fastqc_report.html#M" + getHTMLAnchorIndex();

      // Set score test at -1
      return new TestResult(-1, url, "url");
    }

    final String prefixKey =
        "fastqc.lane"
            + lane + ".sample." + sampleName + ".read" + readSample + "."
            + sampleName + "."
            + getQCModuleName().replace(' ', '.').toLowerCase();

    if (data.get(prefixKey + ".error") == null)
      return new TestResult("NA");

    // Get HTML report URL, score at -1
    final boolean errorRaise = data.getBoolean(prefixKey + ".error");
    final boolean warningRaise = data.getBoolean(prefixKey + ".warning");

    final int score = errorRaise ? 0 : (warningRaise ? 4 : 9);

    final String projectName = data.getProjectSample(lane, sampleName);

    final String index = data.getIndexSample(lane, sampleName);

    final String dirname =
        String.format("%s_%s_L%03d_R%d_001-fastqc", sampleName,
            "".equals(index) ? "NoIndex" : index, lane, readSample);

    final String url =
        "Project_"
            + projectName + "/" + dirname + "/fastqc_report.html#M"
            + getHTMLAnchorIndex();

    return new TestResult(score, url, "url");
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(FastQCCollector.COLLECTOR_NAME);
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
