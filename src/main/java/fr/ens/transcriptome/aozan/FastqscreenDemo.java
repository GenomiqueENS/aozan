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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
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

import org.python.google.common.collect.Lists;

import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.aozan.io.FastqSample;

public class FastqscreenDemo {

  /** Logger */
  // private static final Logger LOGGER = Common.getLogger();
  private static final Logger LOGGER = Logger
      .getLogger(fr.ens.transcriptome.aozan.Globals.APP_NAME);

  /** Timer */
  private static Stopwatch timer = new Stopwatch();

  public static final Properties properties = new Properties();

  public static final String RESOURCE_ROOT =
      "/home/sperrin/Documents/FastqScreenTest/resources";
  public static final String SRC_RUN =
      "/home/sperrin/Documents/FastqScreenTest/runtest";
  public static final String TMP_DIR =
      "/home/sperrin/Documents/FastqScreenTest/tmp_bis";
  public static final String ALIAS_GENOME_PATH =
      "/home/sperrin/Documents/FastqScreenTest/resources/alias_name_genome_fastqscreen.txt";

  public static final String GENOMES_DESC_PATH = RESOURCE_ROOT
      + "/genomes_descs";
  public static final String MAPPERS_INDEXES_PATH = RESOURCE_ROOT
      + "/mappers_indexes";
  public static final String GENOMES_PATH = RESOURCE_ROOT + "/genomes";

  public static final String AOZAN_CONF =
      "/home/sperrin/Documents/FastqScreenTest/runtest/aozan_test.conf";
  // "/home/sperrin/Documents/FastqScreenTest/runtst/aozan_without_fastqc.conf";

  public static RunData data = null;
  public static Map<String, FastqSample> prefixList;
  private static boolean paired = true;

  static String runId;
  static String date;
  static String qcDir;

  public static final void main(String[] args) {

    timer.start();

    try {
      Locale.setDefault(Locale.US);
      inactiveClearMethod();

      if (paired) {
        // run test pair-end
        // runId = "120830_SNL110_0055_AD16D9ACXX";
        runId = "130801_SNL110_0079_AD2CR3ACXX";
        runId = "131004_SNL110_0087_AD297LACXX";
        runId = "131015_SNL110_0088_AH13M0ADXX";

      } else {

        // runId = "130726_SNL110_0078_AC2AJTACXX";
        // runId = "130709_SNL110_0075_AD2C79ACXX";
        // runId = "130715_SNL110_0076_AD2C4UACXX";
        // runId = "130722_SNL110_0077_AH0NT2ADXX";
        // runId = "130904_SNL110_0082_AC2BR0ACXX";
        runId = "130926_SNL110_0085_AH0EYHADXX";
      }

      date = new SimpleDateFormat("yyMMdd").format(new Date());

      // Copy console output in a file
      try {
        System.setOut(new PrintStream(new FileOutputStream(new File(SRC_RUN
            + "/qc_" + runId + "/console_" + date + ".txt"))));

      } catch (FileNotFoundException e1) {
        e1.printStackTrace();
      }

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

  public static void reportQC2() throws AozanException {

    qcDir = SRC_RUN + "/qc_" + runId + "/" + runId;
    String bclDir = "/home/sperrin/shares-net/sequencages/bcl/" + runId;
    String fastqDir =
        "/home/sperrin/shares-net/sequencages/fastq/" + runId + ".0mismatch";
    // String bclDir, String fastqDir, String qcDir, File tmpDir

    QC qc =
        new QC(getMapAozanConf(), bclDir, fastqDir, qcDir + "_qc_tmp", TMP_DIR,
            runId);

    // Compute report
    QCReport report = qc.computeReport();

    // Save report data
    qc.writeXMLReport(report, qcDir + "_qc_tmp/" + runId + "_reportXmlFile.xml");

    // Save HTML report
    qc.writeReport(report, (String) null, qcDir
        + "_qc_tmp/" + runId + "_reportHtmlFile.html");
  }

  public static void inactiveClearMethod() {

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
      FileReader aozanConf = new FileReader(AOZAN_CONF);
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

    System.out.println("genomes : "
        + conf.get("qc.conf.fastqscreen.genomes") + " mapping mode "
        + conf.get("qc.conf.ignore.paired.mode"));

    return conf;
  }
}
