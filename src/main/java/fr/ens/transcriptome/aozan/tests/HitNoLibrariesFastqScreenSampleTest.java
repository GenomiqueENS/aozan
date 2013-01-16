/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.tests;

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;

public class HitNoLibrariesFastqScreenSampleTest extends
    AbstractSimpleSampleTest {

  @Override
  public String[] getCollectorsNamesRequiered() {
    return new String[] {FastqScreenCollector.COLLECTOR_NAME};
  }

  @Override
  protected String getKey(int read, int readSample, int lane, String sampleName) {

    return "fastqscreen.lane"
        + lane + ".sample." + sampleName + ".read" + read + "." + sampleName
        + ".hitnolibraries";
  }

  @Override
  protected boolean isValuePercent() {
    return true;
  }

  @Override
  protected Class<?> getValueType() {
    return Double.class;
  }

  protected Number transformValue(final Number value, final RunData data,
      final int read, final boolean indexedRead, final int lane) {

    return value.doubleValue() / 100.0;
  }

  public HitNoLibrariesFastqScreenSampleTest() {
    super("hitnolibraries", "", "fastqScreen hit no libraries", "%");
  }
}
