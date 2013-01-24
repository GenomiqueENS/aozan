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

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.FastQCCollector;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;
import fr.ens.transcriptome.eoulsan.EoulsanException;

public class FastqScreenDemo {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final Properties properties = new Properties();

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

  public static RunData data = null;

  private static boolean paired = false;

  static String runId;
  static String date;
  static String fastqDir;

  public static final void main(String[] args) throws AozanException,
      IOException, EoulsanException {

    Locale.setDefault(Locale.US);
    final long startTime = System.currentTimeMillis();

    if (paired) {
      // run test pair-end
      runId = "120830_SNL110_0055_AD16D9ACXX";
    } else {
      // run test single-end
      runId = "121116_SNL110_0058_AC11HRACXX";
      // runId = "121219_SNL110_0059_AD1B1BACXX";
      // runId = "120615_SNL110_0051_AD102YACXX";
    }

    date = new SimpleDateFormat("yyMMdd").format(new Date());

    Main.initLogger(TMP_DIR + "/" + runId + "_aozan_test.log");
    ConsoleHandler ch = new ConsoleHandler();
    ch.setFormatter(Globals.LOG_FORMATTER);
    LOGGER.setLevel(Level.ALL);
    LOGGER.addHandler(ch);

    fastqDir = SRC_RUN + "/qc_" + runId + "/" + runId;

    String[] tabGenomes = {"phix", "adapters2", "lsuref_dna", "ssuref" /**/};
    String genomes = "";
    for (String g : tabGenomes) {
      genomes += g + ",";
    }
    // remove last separator character ","
    genomes = genomes.substring(0, genomes.length() - 1);

    Collector fsqCollector = new FastqScreenCollector();
    Collector fqcCollector = new FastQCCollector();
    List<Collector> collectorList = new ArrayList<Collector>();
    collectorList.add(fqcCollector);
    collectorList.add(fsqCollector);

    RunDataGenerator rdg = new RunDataGenerator(collectorList);

    // set paths utils
    rdg.setCasavaDesignFile(new File(fastqDir));
    rdg.setRTAOutputDir(new File(fastqDir));
    rdg.setCasavaOutputDir(new File(fastqDir));
    rdg.setQCOutputDir(new File(fastqDir));
    rdg.setTemporaryDir(new File(TMP_DIR));

    // add new property for execute fastqscreen
    properties.put("qc.conf.fastqscreen.genomes", genomes);

    // data include in aozan.conf
    properties.put("fastq.data.path", SRC_RUN);
    properties.put("reports.data.path", SRC_RUN);
    properties.put("tmp.dir", TMP_DIR);
    properties.put(RunDataGenerator.CASAVA_OUTPUT_DIR, fastqDir);
    properties.put(RunDataGenerator.QC_OUTPUT_DIR, fastqDir);

    // number threads used for fastqscreen is defined in aozan.conf
    properties.put("qc.conf.fastqc.threads", "4");

    // elements for configuration of eoulsanRuntime settings
    // use for create index
    properties.put("qc.conf.settings.genomes.desc.path", GENOMES_DESC_PATH);
    properties.put("qc.conf.settings.genomes", GENOMES_PATH);
    properties.put("qc.conf.settings.mappers.indexes.path",
        MAPPERS_INDEXES_PATH);

    File f = new File(fastqDir + "/data-" + runId + ".txt");

    try {
      data = new RunData(f);

      // Configure : create list of reference genome
      fqcCollector.configure(properties);
      fqcCollector.collect(data);

      fsqCollector.configure(properties);
      fsqCollector.collect(data);

      // completed rundata
      // data =
      // new RunData(
      // new File(
      // "/home/sperrin/Bureau/data-120301_SNL110_0038_AD0EJRABXX_construit.txt"));
      // rdg.collect();

      System.out.println("rundata Complet "
          + fastqDir + "/RunDataCompleted_" + runId + ".txt");

      FileWriter fw =
          new FileWriter(new File(fastqDir
              + "/RunDataCompleted_" + runId + ".txt"));
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(data.toString());
      bw.close();

      // reportQC();

    } catch (Exception io) {
      System.out.println(io.getMessage());
    }

    LOGGER.info("Runtime for demo with a run "
        + runId + " "
        + toTimeHumanReadable(System.currentTimeMillis() - startTime));
  }

  public static void reportQC() throws Exception {

    QC qc = new QC(getMapAozanConf(), TMP_DIR);

    QCReport report = new QCReport(data, qc.laneTests, qc.sampleTests);

    qc.writeRawData(report, TMP_DIR
        + "/" + runId + "_" + date + "_reportRawData.txt");

    // Save report data
    qc.writeXMLReport(report, TMP_DIR
        + "/" + runId + "_" + date + "_reportXmlFile.xml");

    // Save HTML report
    qc.writeReport(report, (String) null, TMP_DIR
        + "/" + runId + "_" + date + "_reportHtmlFile.html");

  }

  public static RunData getRunData() {
    return data;
  }

  public static Properties getPropertiesDemo() {
    return properties;
  }

  public static Map<String, String> getMapAozanConf() {
    Map<String, String> conf = new LinkedHashMap<String, String>();
    String line;
    try {
      FileReader aozanConf =
          new FileReader("/home/sperrin/Documents/FastqScreenTest/aozan.conf");
      BufferedReader br = new BufferedReader(aozanConf);

      while ((line = br.readLine()) != null) {

        final int pos = line.indexOf('=');
        if (pos == -1)
          continue;

        final String key = line.substring(0, pos);
        final String value = line.substring(pos + 1);

        conf.put(key, value);
      }
      br.close();
      aozanConf.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
    return conf;
  }
}
