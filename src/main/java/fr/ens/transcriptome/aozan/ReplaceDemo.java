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

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.QCReport;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.io.FastqSample;

public class ReplaceDemo {

  /** Logger */
  // private static final Logger LOGGER = Common.getLogger();
  private static final Logger LOGGER = Logger
      .getLogger(fr.ens.transcriptome.aozan.Globals.APP_NAME);

  /** Timer */
  private static final Stopwatch timer = Stopwatch.createUnstarted();

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
      "/home/sperrin/Documents/FastqScreenTest/runtest/aozan_v1.3.conf";

  public static Map<String, FastqSample> prefixList;

  // private static String date;

  public static final void main(String[] args) {

    Locale.setDefault(Locale.US);
    inactiveCollectorClearMethod();

    String runId = "140707_SNL110_0122_AH9BN2ADXX";

    try {
      timer.start();
      final String runLog = TMP_DIR + "/" + runId + "_aozan_test.log";
      Common.initLogger(runLog);
      LOGGER.setLevel(Level.CONFIG);

      System.out.println("Create report qc for run "
          + runId + "  " + FastqSample.VALUE);

      reportQC2(runId);

      LOGGER.info("Runtime for demo with a run "
          + runId + " "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

      // Delete lock on run log file
      if (!new File(runLog + ".lck").delete()) {
        throw new Exception("Fail remove lock file " + runLog);
      }

    } catch (AozanException ae) {
      if (ae.getWrappedException() == null)
        ae.printStackTrace();
      else
        ae.getWrappedException().printStackTrace();

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      System.out.println(e.getMessage());

    } finally {
      timer.stop();
    }
  }

  @SuppressWarnings("unused")
  public static void reportQC2(final String runId) throws AozanException {

    final String qcDir = SRC_RUN + "/qc_" + runId + "/" + runId;
    final String bclDir = "/home/sperrin/shares-net/sequencages/bcl/" + runId;
    final String fastqDir =
        "/home/sperrin/shares-net/sequencages/fastq/" + runId;
    // String bclDir, String fastqDir, String qcDir, File tmpDir

    Map<String, String> conf = getMapAozanConf();
    QC qc = new QC(conf, qcDir, fastqDir, qcDir + "_qc_tmp", TMP_DIR, runId);

    // Compute report
    QCReport report = qc.computeReport();

    // Save report data
    qc.writeXMLReport(report, qcDir + "_qc_tmp/" + runId + "_reportXmlFile.xml");

    // Save HTML report
    qc.writeReport(report, null, qcDir
        + "_qc_tmp/" + runId + "_reportHtmlFile.html");
  }

  public static Map<String, String> getMapAozanConf() {
    Map<String, String> conf = new LinkedHashMap<String, String>();
    String line;
    try {
      Reader aozanConf =
          Files.newReader(new File(AOZAN_CONF), Globals.DEFAULT_FILE_ENCODING);

      BufferedReader br = new BufferedReader(aozanConf);

      while ((line = br.readLine()) != null) {
        if (line.startsWith("#"))
          continue;

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

    conf.put("qc.conf.read.xml.collector.used", "false");
    conf.put(Settings.QC_CONF_CLUSTER_DENSITY_RATIO_KEY, "0.3472222");
    conf.put(
        Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY,
        "/home/sperrin/Documents/FastqScreenTest/resources/alias_name_genome_fastqscreen.txt");

    // USE BLAST
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
}
