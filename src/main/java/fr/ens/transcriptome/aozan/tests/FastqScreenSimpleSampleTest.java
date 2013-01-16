package fr.ens.transcriptome.aozan.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;

public class FastqScreenSimpleSampleTest extends AbstractSimpleSampleTest {

  private static final String KEY_GENOMES = "qc.conf.fastqscreen.genomes";
  private String genome;

  @Override
  public String[] getCollectorsNamesRequiered() {
    return new String[] {FastqScreenCollector.COLLECTOR_NAME};
  }

  @Override
  public String getKey(int read, int readSample, int lane, String sampleName) {

    return "fastqscreen.lane"
        + lane + ".sample." + sampleName + ".read" + readSample + "."
        + sampleName + "." + genome + ".unmapped.percent";

  }

  public Class<?> getValueType() {
    return Double.class;
  }

  protected Number transformValue(final Number value, final RunData data,
      final int read, final boolean indexedRead, final int lane) {

    return value.doubleValue() / 100.0;
  }

  public String getNameGenome() {
    return this.genome;
  }

  public boolean isPercent() {
    return true;
  }

  @Override
  public List<AozanTest> configure(Map<String, String> properties)
      throws AozanException {

    String txt = properties.get(KEY_GENOMES);

    if (txt == null || txt.length() == 0)
      throw new AozanException(
          "Step FastqScreen : default genome reference for tests");

    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();
    List<String> genomes = Lists.newArrayList(s.split(txt));

    List<AozanTest> list = new ArrayList<AozanTest>();
    for (String g : genomes)
      list.add(new FastqScreenSimpleSampleTest(g));

    return list;
  }

  //
  // Constructor
  //

  public FastqScreenSimpleSampleTest() {
    this(null);
  }

  public FastqScreenSimpleSampleTest(String genome) {
    super("fsqunmapped", "", "fastqscreen unmapped on " + genome, "%");
    this.genome = genome;
  }

}
