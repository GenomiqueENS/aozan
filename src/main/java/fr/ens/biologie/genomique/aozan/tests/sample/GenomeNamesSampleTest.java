package fr.ens.biologie.genomique.aozan.tests.sample;

import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.aozan.collectors.SamplesheetCollector;

/**
 * This class define a test to get the genome(s) reference(s) of a sample.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class GenomeNamesSampleTest extends AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return Collections.singletonList(SamplesheetCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(int read, int readSample, int sampleId, int lane,
      boolean undetermined) {

    return SamplesheetCollector.SAMPLESHEET_DATA_PREFIX
        + ".sample" + sampleId + ".ref";
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
  public GenomeNamesSampleTest() {
    super("sample.genome.names", "Genome(s)", "Genome(s)");
  }

}
