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
import uk.ac.babraham.FastQC.Modules.AbstractQCModule;
import uk.ac.babraham.FastQC.Modules.AdapterContent;
import uk.ac.babraham.FastQC.Modules.BasicStats;
import uk.ac.babraham.FastQC.Modules.KmerContent;
import uk.ac.babraham.FastQC.Modules.NContent;
import uk.ac.babraham.FastQC.Modules.OverRepresentedSeqs;
import uk.ac.babraham.FastQC.Modules.PerBaseQualityScores;
import uk.ac.babraham.FastQC.Modules.PerBaseSequenceContent;
import uk.ac.babraham.FastQC.Modules.PerSequenceGCContent;
import uk.ac.babraham.FastQC.Modules.PerSequenceQualityScores;
import uk.ac.babraham.FastQC.Modules.PerTileQualityScores;
import uk.ac.babraham.FastQC.Modules.QCModule;
import uk.ac.babraham.FastQC.Modules.SequenceLengthDistribution;
import uk.ac.babraham.FastQC.Report.HTMLReportArchive;
import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.collectors.ReadCollector;
import fr.ens.transcriptome.aozan.fastqc.BadTiles;
import fr.ens.transcriptome.aozan.fastqc.OverrepresentedSequencesBlast;
import fr.ens.transcriptome.aozan.fastqc.RuntimePatchFastQC;
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
  private static final boolean paired = false;

  // private static String date;

  public static final void main(String[] args) {

    try {
      // testModules();

      runReadCollector();

    } catch (Throwable e) {
      e.printStackTrace();
    }

  }

  public static void runReadCollector() throws Exception {

    String dir =
        "/home/sperrin/Documents/FastqScreenTest/runtest/qc_140805_SNL110_0126_AC55P4ACXX/140805_SNL110_0126_AC55P4ACXX_qc_tmp";

    Properties props = getPropertiesAozanConf();
    props.setProperty(Settings.QC_CONF_READ_XML_COLLECTOR_USED_KEY, "false");
    props.setProperty(QC.RTA_OUTPUT_DIR, dir);

    File f = new File(dir, "startData.txt");
    RunData data = new RunData(f);

    ReadCollector rc = new ReadCollector();
    rc.configure(props);
    rc.collect(data);

    data.createRunDataFile(new File(dir, "newData.txt"));
  }

  public static void runAozanTest() {

    Locale.setDefault(Locale.US);
    inactiveCollectorClearMethod();

    List<String> runIds;
    if (paired) {
      // Validation (mm10) - PE150
      runIds = Lists.newArrayList("131015_SNL110_0088_AH13M0ADXX");
    } else {

      // runIds =
      // Lists.newArrayList("140805_SNL110_0126_AC55P4ACXX",
      // "140811_SNL110_0127_AH9AP9ADXX");

      runIds = Lists.newArrayList("140716_SNL110_0123_AC4VRHACXX");

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

    for (String runId : runIds) {
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
  }

  public static void testModules() throws Exception {

    RuntimePatchFastQC.runPatchFastQC(true);

    Properties propsAozan = getPropertiesAozanConf();
    propsAozan.setProperty("tmp.dir", "/tmp");

    OverrepresentedSequencesBlast.getInstance().configure(propsAozan);

    // Configuration
    System.setProperty("fastqc.unzip", "true");
    System.setProperty("fastqc.threads", "1");

    // final File fastq =
    // new File("/home/sperrin/Documents/FastqScreenTest/tmp",
    // "aozan_fastq_2014_355_GATCAG_L007_R1_001.fastq");

    final File fastq = new File("/tmp/essai.fastq");

    // Pach code

    final SequenceFile seqFile = SequenceFactory.getSequenceFile(fastq);

    final OverRepresentedSeqs os = new OverRepresentedSeqs();
    // Define modules list
    final List<AbstractQCModule> modules =
        Lists.newArrayList(new BasicStats(), new PerBaseQualityScores(),
            new PerTileQualityScores(), new PerSequenceQualityScores(),
            new PerBaseSequenceContent(), new PerSequenceGCContent(),
            new NContent(), new SequenceLengthDistribution(),
            os.duplicationLevelModule(), os, new AdapterContent(),
            new KmerContent(), new BadTiles());
    while (seqFile.hasNext()) {
      final Sequence seq = seqFile.next();
      for (final QCModule module : modules) {
        module.processSequence(seq);
      }
    }

    final String filename = "example-fastqc.html";
    final File reportFile =
        new File("/home/sperrin/Documents/FastqScreenTest/tmp", filename);

    // TODO Change with FastQC v0.11.2
    System.out.println("QC report file " + reportFile.getAbsolutePath());
    // new HTMLReportArchiveAozan(seqFile,
    // this.moduleList.toArray(new QCModule[] {}), reportFile);

    new HTMLReportArchive(seqFile, modules.toArray(new QCModule[] {}),
        reportFile);
  }

  /**
   * Create qc report with only on aozan test at true. FastQCCollector must be
   * modified, it is initialize by a register (singleton), the method to patch
   * FastQC must be call once.
   * @throws AozanException
   */
  public static void testBuildReportForEachAozanTest(final String runId)
      throws AozanException {
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

    final Map<String, String> conf = getMapAozanConf();
    int comp = 0;
    final String qcDir = SRC_RUN + "/qc_" + runId + "/" + runId;
    final String fastqDir =
        "/home/sperrin/shares-net/sequencages/fastq/" + runId;
    // String bclDir, String fastqDir, String qcDir, File tmpDir
    final File dataRun =
        new File(qcDir + "_qc_tmp/data-130722_SNL110_0077_AH0NT2ADXX.txt");

    String previousTestName = "";

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
  public static void reportQC2(final String runId) throws AozanException {

    final String qcDir = SRC_RUN + "/qc_" + runId + "/" + runId;
    final String bclDir = "/home/sperrin/shares-net/sequencages/bcl/" + runId;
    final String fastqDir =
        "/home/sperrin/shares-net/sequencages/fastq/" + runId;
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

    conf.put("qc.conf.read.xml.collector.used", "true");
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
