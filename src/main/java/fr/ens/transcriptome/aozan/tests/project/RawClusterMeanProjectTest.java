package fr.ens.transcriptome.aozan.tests.project;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;
import fr.ens.transcriptome.aozan.collectors.ProjectStatsCollector;

public class RawClusterMeanProjectTest extends AbstractSimpleProjectTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ProjectStatsCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final String projectName) {

    return ProjectStatsCollector.COLLECTOR_PREFIX
        + projectName + ".raw.cluster.count";
  }

  @Override
  protected Class<?> getValueType() {

    return Integer.class;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public RawClusterMeanProjectTest() {
    super("rawclustersproject", "", "Raw clusters");
  }

}
