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

package fr.ens.transcriptome.aozan.tests.samplestats;

import static fr.ens.transcriptome.aozan.collectors.stats.SampleStatistics.UNDETERMINED_SAMPLE;
import static fr.ens.transcriptome.aozan.illumina.Bcl2FastqOutput.UNDETERMINED_DIR_NAME;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.stats.SampleStatistics;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.TestResult;

public class LinkReportFastqScreenSampleTest extends AbstractSampleTest {

  @Override
  public List<AozanTest> configure(Map<String, String> properties)
      throws AozanException {
    return Collections.singletonList((AozanTest) this);
  }

  @Override
  public TestResult test(RunData data, String sampleName) {

    // Get HTML report URL
    final String filename = String.format("%s-fastqscreen.html", sampleName);

    // Get project name to build url
    final String projectName =
        (sampleName.equals(UNDETERMINED_SAMPLE)
            ? UNDETERMINED_DIR_NAME : "Project_"
                + data.getProjectSample(1, sampleName));

    final String url = projectName + "/" + filename;

    return new TestResult(9, url, "url");
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(SampleStatistics.COLLECTOR_NAME);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public LinkReportFastqScreenSampleTest() {

    super("linksamplereport", "link report fastqScreen", "FastqScreen report");
  }

}
