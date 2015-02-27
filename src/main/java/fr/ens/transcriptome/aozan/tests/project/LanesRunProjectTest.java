package fr.ens.transcriptome.aozan.tests.project;

import static fr.ens.transcriptome.aozan.collectors.ProjectStatsCollector.COLLECTOR_PREFIX;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.collectors.ProjectStatsCollector;

public class LanesRunProjectTest extends AbstractSimpleProjectTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ProjectStatsCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final String projectName) {
    // TODO
    System.out.println("in test project " + projectName);

    return COLLECTOR_PREFIX + projectName + ".lanes";
  }

  @Override
  protected Class<?> getValueType() {

    return String.class;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public LanesRunProjectTest() {
    super("lanesrunproject", "", "Lane(s)");
  }

}
