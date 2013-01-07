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

package fr.ens.transcriptome.aozan;

import static com.google.common.collect.Lists.newArrayList;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;
import fr.ens.transcriptome.aozan.tests.HitNoLibrariesFastqScreenSampleTest;
import fr.ens.transcriptome.aozan.tests.SampleTest;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;

public class FastqScreenDemo {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final Properties properties = new Properties();
  public static final Map<String, String> propertiesTest = Maps
      .newLinkedHashMap();

  public static final String RESOURCE_ROOT =
      "/home/sperrin/Documents/FastqScreenTest/resources";
  public static final String SRC_RUN =
      "/home/sperrin/Documents/FastqScreenTest/runtest";
  public static final String TMP_DIR =
      "/home/sperrin/Documents/FastqScreenTest/tmp";

  public static final String GENOMES_DESC_PATH = RESOURCE_ROOT
      + "/genomes_descs";
  public static final String MAPPERS_INDEXES_PATH = RESOURCE_ROOT
      + "/mappers_indexes";
  public static final String GENOMES_PATH = RESOURCE_ROOT + "/genomes";

  private static boolean paired = false;

  public static final void main(String[] args) throws AozanException,
      IOException, EoulsanException {

    Locale.setDefault(Locale.US);
    final long startTime = System.currentTimeMillis();

    String runId;
    if (paired) {
      // run test pair-end
      runId = "120830_SNL110_0055_AD16D9ACXX";
    } else {
      // run test single-end
      // runId = "121116_SNL110_0058_AC11HRACXX";
      // runId = "121219_SNL110_0059_AD1B1BACXX";
      runId = "120615_SNL110_0051_AD102YACXX";
    }

    // include in RunDataGenerator
    final String fastqDir = SRC_RUN + "/qc_" + runId + "/" + runId;

    String[] tabGenomes = {"phix", "lsuref_dna", "ssuref", "adapters2"};
    String genomes = "";
    for (String g : tabGenomes) {
      genomes += g + ",";
    }
    // delete last separator character ","
    genomes = genomes.substring(0, genomes.length() - 1);

    // Sample tests
    properties.put("qc.fastqscreen.genomes", genomes);
    properties.put("qc.fastqscreen.fastqDir", fastqDir);
    properties.put("tmp.dir", TMP_DIR);

    // number threads used for fastqscreen is defined in aozan.conf
    properties.put("qc.conf.fastqscreen.threads", "4");

    // elements for configuration of eoulsanRuntime settings
    // use for create index
    properties.put("conf.settings.genomes.desc.path", GENOMES_DESC_PATH);
    properties.put("conf.settings.genomes", GENOMES_PATH);
    properties.put("conf.settings.mappers.indexes.path", MAPPERS_INDEXES_PATH);

    // process for one run
    FastqScreenCollector fsqCollector = new FastqScreenCollector();

    File f = new File(fastqDir + "/data-" + runId + ".txt");
    RunData data = null;
    try {
      data = new RunData(f);

      // Configure : create list of reference genome
      fsqCollector.configure(properties);

      // And collect data
      fsqCollector.collect(data);

      // TODO test method
      // print completed rundata with results of fastqscreen
      FileWriter fw =
          new FileWriter(new File(fastqDir
              + "/RunDataCompleted_" + runId + ".txt"));
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(data.toString());
      bw.close();
    } catch (IOException io) {
      System.out.println(io.getMessage());
    }

    /** TEST AozanTest and compute QC report */

    /*
     * propertiesTest.put("qc.test.hitnolibraries.enable", "true"); final QC qc
     * = new QC(propertiesTest, TMP_DIR); List<SampleTest> fsqTest =
     * Lists.newArrayList(); // FastqScreenSampleTest fsqTest.add((SampleTest)
     * new HitNoLibrariesFastqScreenSampleTest()); // create QC report with a
     * rundata existed QCReport report = new QCReport(data, null, fsqTest);
     * qc.writeXMLReport(report, new File(fastqDir + "/XMLReport_" + runId +
     * ".xml"));
     */

    final long endTime = System.currentTimeMillis();
    LOGGER.info("Runtime for demo with a run "
        + runId + " " + toTimeHumanReadable(endTime - startTime));

    System.out.println("Runtime for demo with a run "
        + runId + " " + toTimeHumanReadable(endTime - startTime));

  }
}
