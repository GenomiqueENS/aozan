package fr.ens.transcriptome.aozan.tests.global;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.GlobalStatsCollector;
import fr.ens.transcriptome.aozan.collectors.ReadCollector;

/**
 * This class define a base count global test.
 * @since 1.x
 * @author Laurent Jourdren
 */
public class BaseCountGlobalTest extends AbstractSimpleGlobalTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(GlobalStatsCollector.COLLECTOR_NAME,
        ReadCollector.COLLECTOR_NAME);
  }

  @Override
  protected String getKey() {

    return "globalstats.bases";
  }

  @Override
  protected Class<?> getValueType() {

    return Long.class;
  }

  @Override
  protected Number transformValue(final Number value, final RunData data) {

    return value.longValue() / 1000000000;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public BaseCountGlobalTest() {

    super("globalbasecount", "", "Base count", "Gb");
  }

}
