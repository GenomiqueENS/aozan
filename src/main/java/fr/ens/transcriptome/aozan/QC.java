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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.CollectorRegistry;
import fr.ens.transcriptome.aozan.collectors.DesignCollector;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.AozanTestRegistry;
import fr.ens.transcriptome.aozan.tests.LaneTest;
import fr.ens.transcriptome.aozan.tests.SampleTest;

/**
 * This class is the main QC class.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class QC {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  /** RTA output directory property key. */
  public static final String RTA_OUTPUT_DIR = "rta.output.dir";

  /** Casava design path property key. */
  public static final String CASAVA_DESIGN_PATH = "casava.samplesheet.path";

  /** Casava output directory property key. */
  public static final String CASAVA_OUTPUT_DIR = "casava.output.dir";

  /** QC output directory property key. */
  public static final String QC_OUTPUT_DIR = "qc.output.dir";

  /** Temporary directory property key. */
  public static final String TMP_DIR = "tmp.dir";

  private static final String TEST_KEY_ENABLED_SUFFIX = ".enable";
  private static final String TEST_KEY_PREFIX = "qc.test.";

  private final String bclDir;
  private final String fastqDir;
  private final String qcDir;
  private final String runId;

  private final List<Collector> collectors = Lists.newArrayList();
  private final List<LaneTest> laneTests = Lists.newArrayList();
  private final List<SampleTest> sampleTests = Lists.newArrayList();
  private final Map<String, String> globalConf = Maps.newHashMap();

  private File tmpDir;

  /**
   * Process data.
   * @throws AozanException if an error occurs while computing report
   */
  public final QCReport computeReport() throws AozanException {

    final File RTAOutputDir = new File(this.bclDir);
    final File casavaOutputDir = new File(this.fastqDir);
    final File QCOutputDir = new File(this.qcDir);

    final File dataFile = new File(qcDir + "/data-" + runId + ".txt");

    RunData data = null;
    // Check if raw data file exists
    if (dataFile.exists()) {

      try {
        data = new RunData(dataFile);
        LOGGER.info("Data file for this run already exists.");

      } catch (IOException e) {

        if (!dataFile.delete())
          LOGGER.warning("IOException, fail delete partiel Data file "
              + dataFile.getName());
        data = null;

      }
    }

    // If no data file or it is empty, the collector must be launch
    if (data == null || data.size() == 0) {
      if (!RTAOutputDir.exists() || !RTAOutputDir.isDirectory())
        throw new AozanException(
            "The BCL directory does not exist or is not a directory: "
                + RTAOutputDir);

      if (!casavaOutputDir.exists() || !casavaOutputDir.isDirectory())
        throw new AozanException(
            "The Casava output directory does not exist or is not a directory: "
                + casavaOutputDir);

      if (!QCOutputDir.exists()) {
        if (!QCOutputDir.mkdirs())
          throw new AozanException("Cannot create QC directory : "
              + QCOutputDir);
      } else {
        LOGGER.info("Temporary QC directory already exists");
      }

      // Create RunDataGenerator object
      final RunDataGenerator rdg = new RunDataGenerator(this.collectors);

      // Set the parameters of the generator
      rdg.setGlobalConf(this.globalConf);

      // Create the run data object
      data = rdg.collect();
    }

    if (data.size() == 0)
      throw new AozanException("No data collected.");

    // Create the report
    QCReport qcReport = new QCReport(data, this.laneTests, this.sampleTests);

    // Create the completed raw data file
    writeRawData(qcReport, dataFile);

    return qcReport;
  }

  /**
   * Write the XML in a file.
   * @param report the QCReport object
   * @param outputFilename the path of the XML file
   * @throws AozanException if an error occurs while creating the report
   */
  public void writeXMLReport(final QCReport report, final String outputFilename)
      throws AozanException {

    writeXMLReport(report, new File(outputFilename));
  }

  /**
   * Write the XML in a file.
   * @param report the QCReport object
   * @param outputFile the XML file
   * @throws AozanException if an error occurs while creating the report
   */
  public void writeXMLReport(final QCReport report, final File outputFile)
      throws AozanException {

    try {
      final Writer writer =
          Files.newWriter(outputFile, Globals.DEFAULT_FILE_ENCODING);

      writer.write(report.toXML());

      writer.close();
    } catch (IOException e) {
      throw new AozanException(e);
    }

  }

  /**
   * Write the report usually in HTML) in a file.
   * @param report the QCReport object
   * @param stylesheetFilename XSL stylesheet file
   * @param outputFilename the path of the report file
   * @throws AozanException if an error occurs while creating the report
   */
  public void writeReport(final QCReport report,
      final String stylesheetFilename, final String outputFilename)
      throws AozanException {

    if (outputFilename == null)
      throw new AozanException("The filename for the qc report is null");

    InputStream is = null;
    try {

      if (stylesheetFilename == null)
        is = this.getClass().getResourceAsStream(Globals.EMBEDDED_QC_XSL);
      else
        is = new FileInputStream(stylesheetFilename);

      writeReport(report, is, new File(outputFilename));
    } catch (IOException e) {
      throw new AozanException(e);

    } finally {
      try {
        if (is != null)
          is.close();
      } catch (IOException ignored) {
      }
    }
  }

  /**
   * Write the report usually in HTML) in a file.
   * @param report the QCReport object
   * @param xslIs XSL stylesheet input stream
   * @param outputFile the report file
   * @throws AozanException if an error occurs while creating the report
   */
  public void writeReport(final QCReport report, final InputStream xslIs,
      final File outputFile) throws AozanException {

    try {
      final Writer writer =
          Files.newWriter(outputFile, Globals.DEFAULT_FILE_ENCODING);

      writer.write(report.export(xslIs));

      writer.close();
    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Write the raw data of the QC.
   * @param report the QCReport object
   * @param outputFilename the raw data file
   * @throws AozanException if an error occurs while writing the file
   */
  public void writeRawData(final QCReport report, final String outputFilename)
      throws AozanException {

    if (outputFilename == null)
      throw new AozanException("The filename for the qc report is null");

    writeRawData(report, new File(outputFilename));
  }

  /**
   * Write the raw data of the QC.
   * @param report the QCReport object
   * @param outputFile the raw data file
   * @throws AozanException if an error occurs while writing the file
   */
  public void writeRawData(final QCReport report, final File outputFile)
      throws AozanException {

    try {
      final Writer writer =
          Files.newWriter(outputFile, Globals.DEFAULT_FILE_ENCODING);

      writer.write(report.getData().toString());

      writer.close();
    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Init the QC.
   * @param properties Aozan configuration
   * @throws AozanException if an error occurs while initialize the QC
   */
  private final void init(final Map<String, String> properties)
      throws AozanException {

    final AozanTestRegistry registry = AozanTestRegistry.getInstance();
    final Map<String, AozanTest> mapTests = Maps.newHashMap();
    List<AozanTest> tests;

    for (final Map.Entry<String, String> e : properties.entrySet()) {

      final String key = e.getKey();
      final String value = e.getValue();

      if (key.startsWith(TEST_KEY_PREFIX)
          && key.endsWith(TEST_KEY_ENABLED_SUFFIX) && value != null
          && "true".equals(value.trim().toLowerCase())) {

        final String testName =
            key.substring(TEST_KEY_PREFIX.length(), key.length()
                - TEST_KEY_ENABLED_SUFFIX.length());

        final AozanTest test = registry.get(testName);

        if (test != null) {

          // Configure the test
          tests =
              configureTest(test, properties, TEST_KEY_PREFIX + testName + ".");

          // Add the test to laneTests or sampleTests
          if (test instanceof LaneTest) {
            for (AozanTest t : tests) {
              this.laneTests.add((LaneTest) t);
              mapTests.put(key, t);
            }

          } else if (test instanceof SampleTest) {
            for (AozanTest t : tests) {
              this.sampleTests.add((SampleTest) t);
              mapTests.put(key, t);
            }
          }

        } else
          throw new AozanException("No test found for property: " + key);
      }

    }

    initCollectors();

    // Initialize tests
    for (LaneTest test : this.laneTests)
      test.init();
    for (SampleTest test : this.sampleTests)
      test.init();

  }

  /**
   * Configure an Aozan Test.
   * @param test Aozan test to configure
   * @param properties Aozan configuration
   * @param prefix key that enable the test
   * @return list of Aozan tests
   * @throws AozanException if an error occurs while configuring the test
   */
  private final List<AozanTest> configureTest(final AozanTest test,
      final Map<String, String> properties, final String prefix)
      throws AozanException {

    final Map<String, String> conf = Maps.newHashMap();

    for (final Map.Entry<String, String> e : properties.entrySet()) {

      final String key = e.getKey();

      if (key.startsWith(prefix) && !key.endsWith(TEST_KEY_ENABLED_SUFFIX)) {

        final String confKey = key.substring(prefix.length());
        final String confValue = e.getValue();

        conf.put(confKey, confValue);

      }

      // add additional configuration in properties for collector
      conf.putAll(globalConf);
    }

    return test.configure(conf);
  }

  /**
   * Initialize the collectors.
   * @throws AozanException if an error occurs while initialize a collector
   */
  private final void initCollectors() throws AozanException {

    final Set<Collector> collectors = Sets.newHashSet();

    List<AozanTest> testsList = Lists.newArrayList();
    for (LaneTest lt : this.laneTests)
      testsList.add(lt);
    for (SampleTest lt : this.sampleTests)
      testsList.add(lt);

    // Test if number test enable in configuration empty
    if (testsList.isEmpty()) {
      throw new AozanException(
          "None test enabled, it must at least one test selected to launch collectors of the qc step.");
    }

    // Get necessary collector for the qc report for lane test
    if (!this.laneTests.isEmpty())
      addCollectors(Lists.newArrayList(RunInfoCollector.COLLECTOR_NAME),
          collectors);

    // Get necessary collector for the qc report for sample test
    if (!this.sampleTests.isEmpty())
      addCollectors(Lists.newArrayList(DesignCollector.COLLECTOR_NAME),
          collectors);

    // Get the necessary collectors
    for (final AozanTest test : testsList)
      addCollectors(test.getCollectorsNamesRequiered(), collectors);

    final Map<Collector, Set<Collector>> dependencies = Maps.newHashMap();
    final Set<Collector> added = Sets.newHashSet();

    // Create the dependencies map
    for (Collector c : collectors) {

      if (c == null)
        continue;

      final Set<Collector> deps =
          getCollectors(c.getCollectorsNamesRequiered());

      if (deps.size() == 0) {
        this.collectors.add(c);
        added.add(c);
      } else
        dependencies.put(c, deps);
    }

    // Resolve dependencies
    while (this.collectors.size() != collectors.size()) {

      final Set<Collector> toRemove = Sets.newHashSet();
      for (Map.Entry<Collector, Set<Collector>> e : dependencies.entrySet()) {
        e.getValue().removeAll(added);
        if (e.getValue().size() == 0)
          toRemove.add(e.getKey());
      }

      if (toRemove.size() == 0)
        throw new AozanException("Unable to resolve collectors dependencies");

      for (Collector c : toRemove) {
        dependencies.remove(c);
        this.collectors.add(c);
        added.add(c);
      }
    }

    if (this.collectors.size() == 0)
      throw new AozanException("No collector found.");

  }

  /**
   * Set global configuration used by collector and aozan tests. It retrieve all
   * paths present in aozan configuration file.
   * @param properties aozan configuration file
   * @throws AozanException if an error occurs while searching paths
   */
  private final void initGlobalConf(final Map<String, String> properties)
      throws AozanException {

    for (final Map.Entry<String, String> e : properties.entrySet()) {

      if (e.getKey().startsWith("qc.conf."))
        this.globalConf.put(e.getKey(), e.getValue());
    }

    File[] designFiles = new File(fastqDir).listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {

        return name.endsWith(".csv");
      }
    });

    if (designFiles == null || designFiles.length == 0)
      throw new AozanException("No Casava design file found in ");

    final File casavaDesignFile = designFiles[0];

    this.globalConf.put(RTA_OUTPUT_DIR, bclDir);
    this.globalConf.put(CASAVA_DESIGN_PATH, casavaDesignFile.getAbsolutePath());
    this.globalConf.put(CASAVA_OUTPUT_DIR, fastqDir);
    this.globalConf.put(QC_OUTPUT_DIR, qcDir);
    this.globalConf.put(TMP_DIR, tmpDir.getAbsolutePath());

  }

  /**
   * Transform a list of collector names in a set collectors objects.
   * @param collectorNames list of collectors names
   * @return a set of collectors objects
   * @throws AozanException if an error occurs while creating a collector
   */
  private Set<Collector> getCollectors(List<String> collectorNames)
      throws AozanException {

    if (collectorNames == null)
      return Collections.emptySet();

    final CollectorRegistry registry = CollectorRegistry.getInstance();
    final Set<Collector> result = Sets.newHashSet();

    for (String collectorName : collectorNames) {

      if (collectorName == null)
        continue;
      final Collector c = registry.get(collectorName);

      if (c == null)
        throw new AozanException("Unable to found collector: " + collectorName);

      result.add(c);
    }

    return result;
  }

  /**
   * Add collectors from a list of name in a set.
   * @param collectorNamesRequired list of the collectors required
   * @param collectors a set of collector
   * @throws AozanException if an error occurs while creating a collector
   */
  private void addCollectors(final List<String> collectorNamesRequired,
      final Set<Collector> collectors) throws AozanException {

    for (final Collector c : getCollectors(collectorNamesRequired)) {

      collectors.add(c);
      addCollectors(c.getCollectorsNamesRequiered(), collectors);
    }

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param properties QC properties
   * @param bclDir BCL directory
   * @param fastqDir fastq directory
   * @param runId run id
   * @param tmpDirname temporary directory path
   * @throws AozanException if an error occurs while initialize the QC object
   */
  public QC(final Map<String, String> properties, final String bclDir,
      final String fastqDir, final String qcDir, final String tmpDirname,
      final String runId) throws AozanException {

    this(properties, bclDir, fastqDir, qcDir, tmpDirname == null
        ? null : new File(tmpDirname), runId);
  }

  /**
   * Public constructor.
   * @param properties QC properties
   * @param bclDir BCL directory
   * @param fastqDir fastq directory
   * @param runId run id
   * @param tmpDir temporary directory
   * @throws AozanException if an error occurs while initialize the QC object
   */
  public QC(final Map<String, String> properties, final String bclDir,
      final String fastqDir, final String qcDir, final File tmpDir,
      final String runId) throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    this.bclDir = bclDir;
    this.fastqDir = fastqDir;
    this.qcDir = qcDir;
    this.runId = runId;

    this.tmpDir =
        tmpDir == null
            ? new File(System.getProperty("java.io.tmpdir")) : tmpDir;

    initGlobalConf(properties);
    init(properties);
  }
}
