/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

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

    return value.doubleValue() * 100.0;
  }

  public String getNameGenome() {
    return this.genome;
  }

  public boolean isValuePercent() {
    return true;
  }

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    String genomesName = properties.get(KEY_GENOMES);

    if (genomesName == null || genomesName.length() == 0)
      throw new AozanException(
          "Step FastqScreen : default genome reference for tests");

    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();
    List<String> genomes = Lists.newArrayList(s.split(genomesName));

    List<AozanTest> list = new ArrayList<AozanTest>();

    for (String genome : genomes) {

      AbstractSimpleSampleTest testGenome =
          new FastqScreenSimpleSampleTest(genome);

      testGenome.interval.configureDoubleInterval(properties);

      list.add(testGenome);
    }

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
