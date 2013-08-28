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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;

/**
 * The class adds in the qc report html a link to the report fastqScreen for
 * each sample.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenSimpleSampleLienReportTest extends AbstractSampleTest {

  @Override
  public TestResult test(final RunData data, final int read,
      final int readSample, final int lane, final String sampleName) {

    if (sampleName == null)
      return new TestResult("NA");

    // Check fastqscreen launch for sample
    String key =
        "fastqscreen.lane"
            + lane + ".sample." + sampleName + ".read" + readSample + "."
            + sampleName + ".mappedexceptgenomesample";

    if (data.get(key) == null)
      return new TestResult("NA");

    // Get HTML report URL
    final String projectName =
        data.get("design.lane" + lane + "." + sampleName + ".sample.project");
    final String index =
        data.get("design.lane" + lane + "." + sampleName + ".index");
    final String filename =
        String.format("%s_%s_L%03d_R1_001-fastqscreen.html", sampleName,
            "".equals(index) ? "NoIndex" : index, lane);

    final String url = "Project_" + projectName + "/" + filename;

    return new TestResult(9, url, "url");
  }

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {
    return Collections.singletonList((AozanTest) this);
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(FastqScreenCollector.COLLECTOR_NAME);
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   */
  public FastqScreenSimpleSampleLienReportTest() {

    super("linkreport", "link report fastqScreen", "link report fastqScreen");
  }

}
