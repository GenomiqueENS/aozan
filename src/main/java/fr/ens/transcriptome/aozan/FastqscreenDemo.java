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
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.io.FastqSample;

public class FastqscreenDemo {

  /** Logger */
  // private static final Logger LOGGER = Common.getLogger();
  private static final Logger LOGGER = Logger
      .getLogger(fr.ens.transcriptome.aozan.Globals.APP_NAME);

  /** Timer */
  private static final Stopwatch timer = Stopwatch.createStarted();

  public static final Properties properties = new Properties();

  public static final String RESOURCE_ROOT =
      "/home/sperrin/Documents/FastqScreenTest/resources";
  public static final String SRC_RUN =
      "/home/sperrin/Documents/FastqScreenTest/runtest";
  public static final String TMP_DIR =
      "/home/sperrin/Documents/FastqScreenTest/tmp";
  public static final String ALIAS_GENOME_PATH =
      "/home/sperrin/Documents/FastqScreenTest/resources/alias_name_genome_fastqscreen.txt";

  public static final String GENOMES_DESC_PATH = RESOURCE_ROOT
      + "/genomes_descs";
  public static final String MAPPERS_INDEXES_PATH = RESOURCE_ROOT
      + "/mappers_indexes";
  public static final String GENOMES_PATH = RESOURCE_ROOT + "/genomes";

  public static final String AOZAN_CONF =
      "/home/sperrin/Documents/FastqScreenTest/runtest/aozan_test.conf";
  // "/home/sperrin/home-net/aozan_validation-1.2.1.conf";
  // "/home/sperrin/Documents/FastqScreenTest/runtest/aozan_partiel_fastqc.conf";
  // "/home/sperrin/Documents/FastqScreenTest/runtest/aozan_without_fastqc.conf";

  public static Map<String, FastqSample> prefixList;
  private static final boolean paired = false;

  private static String runId;
  // private static String date;
  private static String qcDir;

  public static final void main(String[] args) {

    try {
      Locale.setDefault(Locale.US);
      inactiveCollectorClearMethod();

      if (paired) {
        // run test pair-end
        // runId = "120830_SNL110_0055_AD16D9ACXX";
        // runId = "130801_SNL110_0079_AD2CR3ACXX";

        // Mais - SR50
        // runId = "131004_SNL110_0087_AD297LACXX";

        // Validation (mm10) - PE150
        runId = "131015_SNL110_0088_AH13M0ADXX";
      } else {

        // runId = "130726_SNL110_0078_AC2AJTACXX";
        // runId = "130709_SNL110_0075_AD2C79ACXX";
        // runId = "130715_SNL110_0076_AD2C4UACXX";
        // RUN rapid
        // runId = "130722_SNL110_0077_AH0NT2ADXX";
        // runId = "130904_SNL110_0082_AC2BR0ACXX";
        runId = "130910_SNL110_0083_AC2AMKACXX";
        // runId = "130926_SNL110_0085_AH0EYHADXX";
      }

      // date = new SimpleDateFormat("yyMMdd").format(new Date());

      // Copy console output in a file
      // try {
      // System.setOut(new PrintStream(new FileOutputStream(new File(SRC_RUN
      // + "/qc_" + runId + "/console_" + date + ".txt"))));
      //
      // } catch (FileNotFoundException e1) {
      // e1.printStackTrace();
      // }

      Common.initLogger(TMP_DIR + "/" + runId + "_aozan_test.log");
      LOGGER.setLevel(Level.CONFIG);

      System.out.println("Create report qc for run "
          + runId + "  " + FastqSample.VALUE);

      reportQC2();

      LOGGER.info("Runtime for demo with a run "
          + runId + " "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    } catch (AozanException ae) {
      if (ae.getWrappedException() == null)
        ae.printStackTrace();
      else
        ae.getWrappedException().printStackTrace();

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      System.out.println(e.getMessage());
    }

    timer.stop();

  }

  /**
   * Create qc report with only on aozan test at true. FastQCCollector must be
   * modified, it is initialize by a register (singleton), the method to patch
   * FastQC must be call once.
   * @throws AozanException
   */
  public static void testBuildReportForEachAozanTest() throws AozanException {
    final List<String> list = Lists.newArrayList();

    list.add("qc.test.rawclusters.enable");
    list.add("qc.test.pfclusters.enable");
    list.add("qc.test.pfclusterspercent.enable");
    list.add("qc.test.clusterdensity.enable");
    list.add("qc.test.percentalign.enable");
    list.add("qc.test.errorrate.enable");
    list.add("qc.test.errorrate35cycle.enable");
    list.add("qc.test.errorrate75cycle.enable");
    list.add("qc.test.errorrate100cycle.enable");
    list.add("qc.test.firstcycleintensity.enable");
    list.add("qc.test.percentintensitycycle20.enable");
    list.add("qc.test.phasingprephasing.enable");
    list.add("qc.test.rawclusterssamples.enable");
    list.add("qc.test.pfclusterssamples.enable");
    list.add("qc.test.percentpfsample.enable");
    list.add("qc.test.percentinlanesample.enable");
    list.add("qc.test.percentq30.enable");
    list.add("qc.test.meanqualityscorepf.enable");
    list.add("qc.test.basicstats.enable");
    list.add("qc.test.perbasequalityscores.enable");
    list.add("qc.test.persequencequalityscores.enable");
    list.add("qc.test.perbasesequencecontent.enable");
    list.add("qc.test.perbasegccontent.enable");
    list.add("qc.test.persequencegccontent.enable");
    list.add("qc.test.ncontent.enable");
    list.add("qc.test.sequencelengthdistribution.enable");
    list.add("qc.test.duplicationlevel.enable");
    list.add("qc.test.overrepresentedseqs.enable");
    list.add("qc.test.kmercontent.enable");
    list.add("qc.test.badtiles.enable");
    list.add("qc.test.hitnolibraries.enable");
    list.add("qc.test.fsqmapped.enable");
    list.add("qc.test.linkreport.enable");

    Map<String, String> conf = getMapAozanConf();
    String previousTestName = "";
    int comp = 0;
    qcDir = SRC_RUN + "/qc_" + runId + "/" + runId;
    String fastqDir = "/home/sperrin/shares-net/sequencages/fastq/" + runId;
    // String bclDir, String fastqDir, String qcDir, File tmpDir
    File dataRun =
        new File(qcDir + "_qc_tmp/data-130722_SNL110_0077_AH0NT2ADXX.txt");

    for (String testName : list) {
      // Replace False the previous tests
      conf.put(previousTestName, "False");

      // Replace current test at True
      conf.put(testName, "true");

      LOGGER.warning("TEST aozan test "
          + testName + "----------------------------------");
      // Init qc
      QC qc = new QC(conf, qcDir, fastqDir, qcDir + "_qc_tmp", TMP_DIR, runId);
      comp++;

      // Compute report
      QCReport report = qc.computeReport();

      // Save report data
      qc.writeXMLReport(report, qcDir
          + "_qc_tmp/aozanTest/" + comp + "_" + testName + "_reportXmlFile.xml");

      // Save HTML report
      qc.writeReport(report, null, qcDir
          + "_qc_tmp/aozanTest/" + comp + "_" + testName
          + "_reportHtmlFile.html");

      // Rename run data, generate for each test
      if (!dataRun.renameTo(new File(qcDir
          + "_qc_tmp/aozanTest/" + comp + "_data_" + testName + ".txt")))
        throw new AozanException("Fail rename data file for test " + testName);

      previousTestName = testName;
    }

  }

  @SuppressWarnings("unused")
  public static void reportQC2() throws AozanException {

    qcDir = SRC_RUN + "/qc_" + runId + "/" + runId;
    String bclDir = "/home/sperrin/shares-net/sequencages/bcl/" + runId;
    String fastqDir = "/home/sperrin/shares-net/sequencages/fastq/" + runId;
    // String bclDir, String fastqDir, String qcDir, File tmpDir

    QC qc =
        new QC(getMapAozanConf(), qcDir, fastqDir, qcDir + "_qc_tmp", TMP_DIR,
            runId);

    // Compute report
    QCReport report = qc.computeReport();

    // Save report data
    qc.writeXMLReport(report, qcDir + "_qc_tmp/" + runId + "_reportXmlFile.xml");

    // Save HTML report
    qc.writeReport(report, null, qcDir
        + "_qc_tmp/" + runId + "_reportHtmlFile.html");
  }

  public static void inactiveCollectorClearMethod() {

    List<String> listClass = Lists.newArrayList();
    listClass
        .add("fr.ens.transcriptome.aozan.collectors.AbstractFastqCollector");
    listClass.add("fr.ens.transcriptome.aozan.io.FastqStorage");

    try {
      for (String className : listClass) {
        // Get the class to modify
        CtClass cc;
        cc = ClassPool.getDefault().get(className);

        // Retrieve the method to modify
        CtBehavior cb = cc.getDeclaredMethod("clear");
        cb.setBody(null);

        // Load the class by the ClassLoader
        cc.toClass();
      }
    } catch (NotFoundException e) {
      e.printStackTrace();
    } catch (CannotCompileException e) {
      e.printStackTrace();
    }
  }

  public static Properties getPropertiesDemo() {
    return properties;
  }

  public static Map<String, String> getMapAozanConf() {
    Map<String, String> conf = new LinkedHashMap<String, String>();
    String line;
    try {
      Reader aozanConf =
          Files.newReader(new File(AOZAN_CONF), Globals.DEFAULT_FILE_ENCODING);

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

    // conf.put("qc.conf.read.xml.collector.used", "false");
    conf.put(Settings.QC_CONF_CLUSTER_DENSITY_RATIO_KEY, "0.3472222");
    conf.put(
        Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY,
        "/home/sperrin/Documents/FastqScreenTest/resources/alias_name_genome_fastqscreen.txt");
    conf.put(Settings.QC_CONF_FASTQSCREEN_BLAST_ENABLE_KEY, "true");

    System.out
        .println("genomes : "
            + conf.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_KEY)
            + " mapping mode "
            + conf
                .get(Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_MODE_KEY));

    return conf;
  }

  public static Properties getPropertiesAozanConf() {
    Map<String, String> mapConf = getMapAozanConf();
    Properties props = new Properties();

    for (final Map.Entry<String, String> e : mapConf.entrySet()) {

      if (e.getKey().startsWith("qc.conf.")) {
        // System.out.println("props " + e.getKey() + " - " + e.getValue());
        props.put(e.getKey(), e.getValue());
      }
    }

    return props;
  }
}
