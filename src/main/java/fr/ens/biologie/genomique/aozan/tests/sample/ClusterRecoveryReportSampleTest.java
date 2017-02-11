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

package fr.ens.biologie.genomique.aozan.tests.sample;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.UndeterminedIndexesCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;

/**
 * The class adds in the qc report html a link to the report recoverable cluster
 * results for each sample.
 * @since 1.3
 * @author Sandrine Perrin
 */
public class ClusterRecoveryReportSampleTest extends AbstractSampleTest {

  @Override
  public TestResult test(final RunData data, final int read,
      final int readSample, final int sampleId) {

    final boolean undetermined = data.isUndeterminedSample(sampleId);
    final int lane = data.getSampleLane(sampleId);

    // Case undetermined indices sample
    if (undetermined) {

      final String key =
          "undeterminedindices.lane" + lane + ".recoverable.pf.cluster.count";

      if (data.get(key) == null) {
        return new TestResult("NA");
      }

      final String filename =
          data.get("undeterminedindices.lane" + lane + ".report.file.name");

      final String url = "Undetermined_indices/" + filename;

      return new TestResult(-1, url, "url");
    }

    // Case standard sample
    final String key = "undeterminedindices.sample"
        + sampleId + ".recoverable.pf.cluster.count";

    if (data.get(key) == null) {
      return new TestResult("NA");
    }

    // Get HTML report URL
    final String projectName = data.getProjectSample(sampleId);
    final String filename =
        data.get("undeterminedindices.sample" + sampleId + ".report.file.name");

    final String url = "Project_" + projectName + "/" + filename;

    return new TestResult(-1, url, "url");
  }

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
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
   * Public constructor.
   */
  public ClusterRecoveryReportSampleTest() {

    super("sample.cluster.recovery.report", "Cluster Recovery Report",
        "Cluster Recovery Report");
  }

}
