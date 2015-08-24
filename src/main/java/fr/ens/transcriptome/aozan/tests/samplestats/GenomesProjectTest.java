package fr.ens.transcriptome.aozan.tests.samplestats;

import static fr.ens.transcriptome.aozan.collectors.stats.SampleStatistics.COLLECTOR_PREFIX;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.collectors.stats.SampleStatistics;

public class GenomesProjectTest  extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(SampleStatistics.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final String sampleName) {

    return COLLECTOR_PREFIX + sampleName + ".genomes.ref";
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
  public GenomesProjectTest() {
    super("genomessample", "", "Genome(s)");
  }


}
