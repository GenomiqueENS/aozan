package fr.ens.transcriptome.aozan.tests.project;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.ProjectStatsCollector;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.TestResult;

public class LinkReportFastqScreenProjectTest extends AbstractProjectTest {

  @Override
  public List<AozanTest> configure(Map<String, String> properties)
      throws AozanException {
    return Collections.singletonList((AozanTest) this);
  }

  @Override
  public TestResult test(RunData data, String projectName) {

    // if (sampleName == null) {
    // // return new TestResult("NA");
    // final String projectName = "Undetermined_indices";
    //
    // final String filename =
    // String.format("lane%s_Undetermined_L%03d_R%d_001-fastqscreen.html",
    // lane, lane, readSample);
    //
    // final String url = projectName + "/" + filename;
    //
    // // Set score test at -1
    // return new TestResult(-1, url, "url");
    // }
    //
    // // Check fastqscreen launch for sample
    // final String key =
    // "fastqscreen.lane"
    // + lane + ".sample." + sampleName + ".read" + readSample + "."
    // + sampleName + ".mappedexceptgenomesample";
    //
    // if (data.get(key) == null) {
    // return new TestResult("NA");
    // }

    // Get HTML report URL
    final String filename = String.format("%s-fastqscreen.html", projectName);

    final String url = "Project_" + projectName + "/" + filename;

    return new TestResult(9, url, "url");
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ProjectStatsCollector.COLLECTOR_NAME);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public LinkReportFastqScreenProjectTest() {

    super("linkprojectreport", "link report fastqScreen", "FastqScreen report");
  }

}
