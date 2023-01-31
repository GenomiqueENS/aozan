package fr.ens.biologie.genomique.aozan.tests.global;

import static fr.ens.biologie.genomique.aozan.collectors.PrimaryAnalysisMetricsCollector.COLLECTOR_NAME;

import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.aozan.RunData;

/**
 * This class define a loading concentration percent global test.
 * @since 3.0
 * @author Laurent Jourdren
 */
public class LoadingConcentrationPercentGlobalTest
    extends AbstractSimpleGlobalTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return Collections.singletonList(COLLECTOR_NAME);
  }

  @Override
  protected String getKey() {
    return "primary.analysis.metrics.loading.concentration.percent";
  }

  @Override
  protected Class<?> getValueType() {
    return Float.class;
  }

  @Override
  protected Number transformValue(final Number value, final RunData data) {

    return value.floatValue() * 100.0;
  }

  //
  // Constructor
  //

  public LoadingConcentrationPercentGlobalTest() {

    super("global.loading.concentration.percent",
        "Loading concentration percent", "Loading concentration", "%");
  }

}
