package fr.ens.transcriptome.aozan.tests;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.collectors.UndeterminedIndexesCollector;

/**
 * This class define a recoverable passing filter clusters count test for
 * samples.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class RecoverablePFClusterSampleTest extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(UndeterminedIndexesCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final int read, final int readSample, final int lane,
      final String sampleName) {

    if (sampleName == null)
      return "undeterminedindices.lane"
          + lane + ".recoverable.pf.cluster.count";

    return "undeterminedindices.lane"
        + lane + ".sample." + sampleName + ".recoverable.pf.cluster.count";
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
  public RecoverablePFClusterSampleTest() {
    super("recoverablepfclusterssamples", "", "Recoverable PF clusters");
  }

}
