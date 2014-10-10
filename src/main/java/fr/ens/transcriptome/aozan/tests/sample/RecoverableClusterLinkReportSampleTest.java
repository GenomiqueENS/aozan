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

package fr.ens.transcriptome.aozan.tests.sample;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.UndeterminedIndexesCollector;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.TestResult;

/**
 * The class adds in the qc report html a link to the report recoverable cluster
 * results for each sample.
 * @since 1.3
 * @author Sandrine Perrin
 */
public class RecoverableClusterLinkReportSampleTest extends AbstractSampleTest {

  @Override
  public TestResult test(final RunData data, final int read,
      final int readSample, final int lane, final String sampleName) {

    // Case undetermined indices sample
    if (sampleName == null) {

      final String key =
          "undeterminedindices.lane" + lane + ".recoverable.pf.cluster.count";

      if (data.get(key) == null)
        return new TestResult("NA");

      final String url =
          String
              .format(
                  "Undetermined_indices/lane%s_Undetermined_L%03d_R1_001-potentialindices.html",
                  lane, lane);

      return new TestResult(-1, url, "url");

    }

    // Case standard sample
    final String key =
        "undeterminedindices.lane"
            + lane + ".sample." + sampleName + ".recoverable.pf.cluster.count";

    if (data.get(key) == null)
      return new TestResult("NA");

    // Get HTML report URL
    final String projectName = data.getProjectSample(lane, sampleName);

    final String filename =
        String.format("%s_lane%s-potentialindices.html", sampleName, lane);

    final String url = "Project_" + projectName + "/" + filename;

    return new TestResult(-1, url, "url");
  }

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {
    return Collections.singletonList((AozanTest) this);
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(UndeterminedIndexesCollector.COLLECTOR_NAME);
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   */
  public RecoverableClusterLinkReportSampleTest() {

    super("linkreportrecoverycluster", "link report recovery cluster",
        "link report recovery cluster");
  }

}
