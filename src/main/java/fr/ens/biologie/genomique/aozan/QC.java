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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import fr.ens.biologie.genomique.aozan.collectors.Collector;
import fr.ens.biologie.genomique.aozan.collectors.CollectorRegistry;
import fr.ens.biologie.genomique.aozan.collectors.FastqScreenCollector;
import fr.ens.biologie.genomique.aozan.collectors.RunInfoCollector;
import fr.ens.biologie.genomique.aozan.collectors.SamplesheetCollector;
import fr.ens.biologie.genomique.aozan.fastqc.RuntimePatchFastQC;
import fr.ens.biologie.genomique.aozan.fastqscreen.GenomeAliases;
import fr.ens.biologie.genomique.aozan.fastqscreen.GenomeDescriptionCreator;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.AozanTestRegistry;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.global.GlobalTest;
import fr.ens.biologie.genomique.aozan.tests.lane.LaneTest;
import fr.ens.biologie.genomique.aozan.tests.pooledsample.PooledSampleTest;
import fr.ens.biologie.genomique.aozan.tests.project.ProjectTest;
import fr.ens.biologie.genomique.aozan.tests.sample.SampleTest;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.data.protocols.DataProtocolService;
import uk.ac.babraham.FastQC.FastQCConfig;

/**
 * This class is the main QC class.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class QC {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  /** RTA output directory property key. */
  public static final String RTA_OUTPUT_DIR = "rta.output.dir";

  /** Bcl2fastq samplesheet path property key. */
  public static final String BCL2FASTQ_SAMPLESHEET_PATH =
      "casava.samplesheet.path";

  /** Bcl2fastq output directory property key. */
  public static final String BCL2FASTQ_OUTPUT_DIR = "casava.output.dir";

  /** QC output directory property key. */
  public static final String QC_OUTPUT_DIR = "qc.output.dir";

  /** QC output directory property key. */
  public static final String QC_COLLECTOR_NAMES = "qc.collector.names";

  /** Temporary directory property key. */
  public static final String TMP_DIR = "tmp.dir";

  public static final String LANE_COUNT = "lane.count";

  public static final String TEST_KEY_ENABLED_SUFFIX = ".enable";
  public static final String TEST_KEY_PREFIX = "qc.test.";

  private final Settings settings;

  private final File bclDir;
  private final File fastqDir;
  private final File qcDir;
  private final String runId;

  private final List<Collector> collectors = new ArrayList<>();
  private final List<GlobalTest> globalTests = new ArrayList<>();
  private final List<LaneTest> laneTests = new ArrayList<>();
  private final List<ProjectTest> projectStatsTests = new ArrayList<>();
  private final List<PooledSampleTest> samplesStatsTests = new ArrayList<>();
  private final List<SampleTest> sampleTests = new ArrayList<>();
  private final Map<String, String> globalConf = new LinkedHashMap<>();

  private final File tmpDir;
  private final File sampleSheetFile;

  private final CollectorRegistry collectorRegistry = new CollectorRegistry();

  //
  // Getters
  //

  /**
   * Get the run id.
   * @return the run id as a String
   */
  public String getRunId() {
    return this.runId;
  }

  /**
   * Get the BCL directory of the run.
   * @return the BCL directory of the run
   */
  public File getBclDir() {
    return this.bclDir;
  }

  /**
   * Get the FASTQ directory of the run.
   * @return the FASTQ directory of the run
   */
  public File getFastqDir() {
    return this.fastqDir;
  }

  /**
   * Get the QC directory of the run.
   * @return the QC directory of the run
   */
  public File getQcDir() {
    return this.qcDir;
  }

  /**
   * Get the samplesheet file.
   * @return the samplesheet file
   */
  public File getSampleSheetFile() {
    return this.sampleSheetFile;
  }

  /**
   * Get the temporary directory.
   * @return the temporary directory
   */
  public File getTmpDir() {
    return this.tmpDir;
  }

  /**
   * Get Aozan settings.
   * @return the settings
   */
  public Settings getSettings() {
    return this.settings;
  }

  //
  // Report methods
  //

  /**
   * Process data.
   * @throws AozanException if an error occurs while computing report
   */
  public final QCReport computeReport() throws AozanException {

    final File RTAOutputDir = this.bclDir;
    final File bcl2fastqOutputDir = this.fastqDir;
    final File QCOutputDir = this.qcDir;

    final File dataFile =
        new File(this.qcDir, this.runId + Globals.QC_DATA_EXTENSION);

    RunData data = null;
    // Check if raw data file exists
    if (dataFile.exists()) {

      try {
        data = new RunData(dataFile);
        LOGGER.info("Data file for this run already exists.");

      } catch (final IOException e) {

        if (!dataFile.delete()) {
          LOGGER.warning("IOException, fail delete partiel Data file "
              + dataFile.getName());
        }
        data = null;

      }
    }

    // If no data file or it is empty, the collector must be launch
    if (data == null || data.size() == 0) {
      if (!RTAOutputDir.exists() || !RTAOutputDir.isDirectory()) {
        throw new AozanException(
            "The BCL directory does not exist or is not a directory: "
                + RTAOutputDir);
      }

      if (!bcl2fastqOutputDir.exists() || !bcl2fastqOutputDir.isDirectory()) {
        throw new AozanException(
            "The Bcl2fastq output directory does not exist or is not a directory: "
                + bcl2fastqOutputDir);
      }

      if (!QCOutputDir.exists()) {
        if (!QCOutputDir.mkdirs()) {
          throw new AozanException(
              "Cannot create QC directory : " + QCOutputDir);
        }
      } else {
        LOGGER.info("Temporary QC directory already exists");
      }

      // Create RunDataGenerator object
      final RunDataGenerator rdg =
          new RunDataGenerator(this.collectors, this.runId, this.globalConf);

      // Create the run data object
      data = rdg.collect(this);
    }

    if (data.size() == 0) {
      throw new AozanException("No data collected.");
    }

    // Create the report
    final QCReport qcReport =
        new QCReport(data, this.globalTests, this.laneTests,
            this.projectStatsTests, this.samplesStatsTests, this.sampleTests);

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
    } catch (final IOException e) {
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

    if (outputFilename == null) {
      throw new AozanException("The filename for the qc report is null");
    }

    InputStream is = null;
    try {

      if (stylesheetFilename == null) {
        is = this.getClass().getResourceAsStream(Globals.EMBEDDED_QC_XSL);
      } else {
        is = new FileInputStream(stylesheetFilename);
      }

      writeReport(report, is, new File(outputFilename));
    } catch (final IOException e) {
      throw new AozanException(e);

    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (final IOException ignored) {
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
    } catch (final IOException e) {
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

    if (outputFilename == null) {
      throw new AozanException("The filename for the qc report is null");
    }

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
    } catch (final IOException e) {
      throw new AozanException(e);
    }
  }

  //
  // Initialization methods
  //

  /**
   * Init the QC.
   * @param settings Aozan settings
   * @throws AozanException if an error occurs while initialize the QC
   */
  private void init(final Settings settings) throws AozanException {

    // Initialize Genome aliases
    GenomeAliases.initialize(settings);

    final AozanTestRegistry registry = new AozanTestRegistry();
    List<AozanTest> tests;

    boolean fastqScreenCollectorRequirementInitialized = false;

    for (final Map.Entry<String, String> e : settings.entrySet()) {

      final String key = e.getKey();
      final String value = e.getValue();

      if (key.startsWith(TEST_KEY_PREFIX)
          && key.endsWith(TEST_KEY_ENABLED_SUFFIX) && value != null
          && "true".equals(value.trim().toLowerCase())) {

        final String testFullName = key.substring(TEST_KEY_PREFIX.length(),
            key.length() - TEST_KEY_ENABLED_SUFFIX.length());

        final AozanTest test = registry.get(testFullName);

        if (test != null) {

          // Initialize FastqScreen requirements
          if (!fastqScreenCollectorRequirementInitialized
              && test.getCollectorsNamesRequiered()
                  .contains(FastqScreenCollector.COLLECTOR_NAME)) {

            initFastqScreenCollectorRequirements(settings);
            fastqScreenCollectorRequirementInitialized = true;
          }

          // Get the type of the test
          final String[] fields = testFullName.split("\\.");
          if (fields.length < 2) {
            throw new AozanException("Invalid test name: " + testFullName);
          }
          final String testType = fields[0];

          // Configure the test
          tests = test.configure(new TestConfiguration(settings,
              TEST_KEY_PREFIX + testFullName + ".", globalConf));

          // Add the test to runTests, laneTests or sampleTests

          switch (testType) {

          case "global":
            for (final AozanTest t : tests) {
              this.globalTests.add((GlobalTest) t);
            }
            break;

          case "lane":
            for (final AozanTest t : tests) {
              this.laneTests.add((LaneTest) t);
            }
            break;

          case "sample":
            for (final AozanTest t : tests) {
              this.sampleTests.add((SampleTest) t);
            }
            break;

          case "project":
            for (final AozanTest t : tests) {
              this.projectStatsTests.add((ProjectTest) t);
            }
            break;

          case "pooledsample":
            for (final AozanTest t : tests) {
              this.samplesStatsTests.add((PooledSampleTest) t);
            }
            break;

          default:
            throw new AozanException("Unknown test type: " + testType);

          }

        } else {
          throw new AozanException("No test found for property: " + key);
        }
      }

    }

    // Create the list of collectors
    createCollectorList();

    // Initialize tests
    for (final GlobalTest test : this.globalTests) {
      test.init();
    }
    for (final LaneTest test : this.laneTests) {
      test.init();
    }
    for (final SampleTest test : this.sampleTests) {
      test.init();
    }
    for (final ProjectTest test : this.projectStatsTests) {
      test.init();
    }
    for (final PooledSampleTest test : this.samplesStatsTests) {
      test.init();
    }
  }

  /**
   * Initialize the collectors.
   * @throws AozanException if an error occurs while initialize a collector
   */
  private void createCollectorList() throws AozanException {

    final Set<Collector> collectors = new HashSet<>();

    final List<AozanTest> testsList = new ArrayList<>();
    for (final GlobalTest gt : this.globalTests) {
      testsList.add(gt);
    }
    for (final LaneTest lt : this.laneTests) {
      testsList.add(lt);
    }
    for (final SampleTest st : this.sampleTests) {
      testsList.add(st);
    }
    for (final ProjectTest st : this.projectStatsTests) {
      testsList.add(st);
    }
    for (final PooledSampleTest st : this.samplesStatsTests) {
      testsList.add(st);
    }

    // Test if number test enable in configuration empty
    if (testsList.isEmpty()) {
      throw new AozanException("None test enabled, "
          + "it must at least one test selected to launch collectors of the qc step.");
    }

    // Get necessary collector for the qc report for lane test
    if (!this.laneTests.isEmpty() || !this.globalTests.isEmpty()) {
      addCollectors(Lists.newArrayList(RunInfoCollector.COLLECTOR_NAME),
          collectors);
    }

    // Get necessary collector for the qc report for sample test
    if (!this.sampleTests.isEmpty()) {
      addCollectors(Lists.newArrayList(SamplesheetCollector.COLLECTOR_NAME),
          collectors);
    }

    // Get the necessary collectors
    for (final AozanTest test : testsList) {
      addCollectors(test.getCollectorsNamesRequiered(), collectors);
    }

    final Map<Collector, Set<Collector>> dependencies = new HashMap<>();
    final Set<Collector> added = new HashSet<>();

    // Create the dependencies map
    for (final Collector c : collectors) {

      if (c == null) {
        continue;
      }

      final Set<Collector> deps =
          getCollectors(c.getCollectorsNamesRequiered());

      if (deps.size() == 0) {
        this.collectors.add(c);
        added.add(c);
      } else {
        dependencies.put(c, deps);
      }
    }

    // Resolve dependencies
    while (this.collectors.size() != collectors.size()) {

      final Set<Collector> toRemove = new HashSet<>();
      for (final Map.Entry<Collector, Set<Collector>> e : dependencies
          .entrySet()) {
        e.getValue().removeAll(added);
        if (e.getValue().size() == 0) {
          toRemove.add(e.getKey());
        }
      }

      if (toRemove.size() == 0) {
        throw new AozanException("Unable to resolve collectors dependencies");
      }

      for (final Collector c : toRemove) {
        dependencies.remove(c);
        this.collectors.add(c);
        added.add(c);
      }
    }

    if (this.collectors.size() == 0) {
      throw new AozanException("No collector found.");
    }

  }

  /**
   * Set global configuration used by collector and aozan tests. It retrieve all
   * paths present in aozan configuration file.
   * @param settings Aozan settings
   * @throws AozanException if an error occurs while searching paths
   */
  private void initGlobalConf(final Settings settings) throws AozanException {

    for (final Map.Entry<String, String> e : settings.entrySet()) {

      if (e.getKey().startsWith("qc.conf.")) {
        this.globalConf.put(e.getKey(), e.getValue());
      }
    }

    this.globalConf.put(RTA_OUTPUT_DIR, this.bclDir.getPath());
    this.globalConf.put(BCL2FASTQ_SAMPLESHEET_PATH,
        this.sampleSheetFile.getAbsolutePath());
    this.globalConf.put(BCL2FASTQ_OUTPUT_DIR, this.fastqDir.getPath());
    this.globalConf.put(QC_OUTPUT_DIR, this.qcDir.getPath());
    this.globalConf.put(TMP_DIR, this.tmpDir.getAbsolutePath());
  }

  /**
   * Initialize FastQC v0.11.X from configuration Aozan.
   * @param settings Aozan settings
   * @throws AozanException if an error occurs when patching FastQC classes.
   */
  private void initFastQCCollectorRequirements(final Settings settings)
      throws AozanException {

    // Define parameters of FastQC
    System.setProperty("java.awt.headless", "true");

    // Set the number of threads of FastQC at one
    System.setProperty("fastqc.threads", "1");

    // Contaminant file
    addSystemProperty(settings, Settings.QC_CONF_FASTQC_CONTAMINANT_FILE_KEY,
        "fastqc.contaminant_file");

    // Adapter file
    addSystemProperty(settings, Settings.QC_CONF_FASTQC_ADAPTER_FILE_KEY,
        "fastqc.adapter_file");

    // Limits file
    addSystemProperty(settings, Settings.QC_CONF_FASTQC_LIMITS_FILE_KEY,
        "fastqc.limits_file");

    // Kmer Size
    addSystemProperty(settings, Settings.QC_CONF_FASTQC_KMER_SIZE_KEY,
        "fastqc.kmer_size");

    // Set fastQC nogroup
    addSystemProperty(settings, Settings.QC_CONF_FASTQC_NOGROUP_KEY,
        "fastqc.nogroup");

    // Set fastQC expgroup
    addSystemProperty(settings, Settings.QC_CONF_FASTQC_EXPGROUP_KEY,
        "fastqc.expgroup");

    // Set fastQC format fastq
    addSystemProperty(settings, Settings.QC_CONF_FASTQC_CASAVA_KEY,
        "fastqc.casava");

    // Set fastQC nofilter system property
    addSystemProperty(settings, Settings.QC_CONF_FASTQC_NOFILTER_KEY,
        "fastqc.nofilter");

    addSystemProperty(settings, Settings.QC_CONF_FASTQC_NANO_KEY,
        "fastqc.nano");

    // Unzip FastQC report
    System.setProperty("fastqc.unzip", "false");
    addSystemProperty(settings, Settings.QC_CONF_FASTQC_UNZIP_REPORT_FILE_KEY,
        "fastqc.unzip");

    // Patch FastQC classes
    RuntimePatchFastQC.runPatchFastQC(Boolean
        .valueOf(settings.get(Settings.QC_CONF_FASTQC_BLAST_ENABLE_KEY)));

    // Initialize FastQCConfig
    FastQCConfig.getInstance();

    // Fix FastQCConfig
    if (FastQCConfig.getInstance().do_unzip == null) {
      FastQCConfig.getInstance().do_unzip = Boolean.FALSE;
    }

  }

  /**
   * Initialize initGenomeDescriptionCreator.
   * @param settings Aozan settings
   * @throws AozanException if an error occurs while initializing the genome
   *           aliases
   */
  private void initFastqScreenCollectorRequirements(final Settings settings)
      throws AozanException {

    final fr.ens.biologie.genomique.eoulsan.Settings eoulsanSettings =
        EoulsanRuntime.getSettings();

    final String genomeDescStoragePath =
        settings.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_DESC_PATH_KEY);

    final String genomeMapperIndexStoragePath =
        settings.get(Settings.QC_CONF_FASTQSCREEN_MAPPERS_INDEXES_PATH_KEY);

    final String genomeStoragePath =
        settings.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_PATH_KEY);

    if (genomeDescStoragePath == null
        || genomeDescStoragePath.trim().isEmpty()) {
      throw new AozanException("No "
          + Settings.QC_CONF_FASTQSCREEN_GENOMES_DESC_PATH_KEY
          + " property set in Aozan settings to define the path to "
          + "the genome description files");
    }

    if (genomeMapperIndexStoragePath == null
        || genomeMapperIndexStoragePath.trim().isEmpty()) {
      throw new AozanException("No "
          + Settings.QC_CONF_FASTQSCREEN_MAPPERS_INDEXES_PATH_KEY
          + " property set in Aozan settings to define the path to "
          + "the genome mapper index files");
    }

    if (genomeStoragePath == null || genomeStoragePath.trim().isEmpty()) {
      throw new AozanException("No "
          + Settings.QC_CONF_FASTQSCREEN_GENOMES_PATH_KEY
          + " property set in Aozan settings to define the path to "
          + "the genomes files");
    }

    // Set the values
    eoulsanSettings.setGenomeDescStoragePath(genomeDescStoragePath);
    eoulsanSettings
        .setGenomeMapperIndexStoragePath(genomeMapperIndexStoragePath);
    eoulsanSettings.setGenomeStoragePath(genomeStoragePath);

    // Set data protocol from Eoulsan not load for Aozan because it needs to add
    // dependencies
    DataProtocolService.getInstance().addClassesToNotLoad(Lists.newArrayList(
        "fr.ens.biologie.genomique.eoulsan.data.protocols.S3DataProtocol",
        "fr.ens.biologie.genomique.eoulsan.data.protocols.S3NDataProtocol"));

    // Initialize GenomeDescriptionCreator
    GenomeDescriptionCreator.initialize(
        settings.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_DESC_PATH_KEY));
  }

  /**
   * Add a system properties from Aozan properties.
   * @param settings Aozan settings
   * @param keyAozan key in Aozan properties
   * @param keySystem key in System properties
   */
  private static void addSystemProperty(final Settings settings,
      final String keyAozan, final String keySystem) {

    final String value = settings.get(keyAozan);

    if (value != null && !value.isEmpty()) {
      System.setProperty(keySystem, value);
    }

  }

  /**
   * Transform a list of collector names in a set collectors objects.
   * @param collectorNames list of collectors names
   * @return a set of collectors objects
   * @throws AozanException if an error occurs while creating a collector
   */
  private Set<Collector> getCollectors(final List<String> collectorNames)
      throws AozanException {

    if (collectorNames == null) {
      return Collections.emptySet();
    }

    final Set<Collector> result = new HashSet<>();

    for (final String collectorName : collectorNames) {

      if (collectorName == null) {
        continue;
      }
      final Collector c = this.collectorRegistry.get(collectorName);

      if (c == null) {
        throw new AozanException("Unable to found collector: " + collectorName);
      }

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
  // Other methods
  //

  /**
   * Create a File object for a directory path and check if the directory
   * exists.
   * @param dirPath the path of the directory
   * @return a File object with the directory path
   * @throws AozanException if the directory does not exists
   */
  private static File checkDir(final String dirPath) throws AozanException {

    if (dirPath == null) {
      throw new NullPointerException("dirPath argument cannot be null");
    }

    final File dir = new File(dirPath);

    if (!dir.isDirectory()) {
      throw new AozanException(
          "The directory does not exits or is not a directory: " + dir);
    }

    return dir;
  }

  private static File getSampleSheetFile(final File fastqDir)
      throws AozanException {

    final File[] samplesheetFiles = fastqDir.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(final File dir, final String name) {

        return name.endsWith(".csv");
      }
    });

    if (samplesheetFiles == null || samplesheetFiles.length == 0) {
      throw new AozanException(
          "No Bcl2fastq samplesheet file found in " + fastqDir);
    }

    return samplesheetFiles[0];
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param settings Aozan settings
   * @param bclDir BCL directory
   * @param fastqDir fastq directory
   * @param qcDir the qc dir
   * @param tmpDirname temporary directory path
   * @param runId run id
   * @throws AozanException if an error occurs while initialize the QC object
   */
  public QC(final Settings settings, final String bclDir, final String fastqDir,
      final String qcDir, final String tmpDirname, final String runId)
      throws AozanException {

    this(settings, bclDir, fastqDir, qcDir,
        tmpDirname == null ? null : new File(tmpDirname), runId);
  }

  /**
   * Public constructor.
   * @param settings Aozan settings
   * @param bclDir BCL directory
   * @param fastqDir fastq directory
   * @param qcDir the qc dir
   * @param tmpDir temporary directory
   * @param runId run id
   * @throws AozanException if an error occurs while initialize the QC object
   */
  public QC(final Settings settings, final String bclDir, final String fastqDir,
      final String qcDir, final File tmpDir, final String runId)
      throws AozanException {

    this.settings = settings;
    this.bclDir = checkDir(bclDir);
    this.fastqDir = checkDir(fastqDir);
    this.qcDir = checkDir(qcDir);
    this.runId = runId;

    this.tmpDir = tmpDir == null
        ? new File(System.getProperty("java.io.tmpdir")) : tmpDir;

    this.sampleSheetFile = getSampleSheetFile(this.fastqDir);

    // Create the global settings for collectors and tests
    initGlobalConf(settings);

    // Initialize FastQC requirements
    initFastQCCollectorRequirements(settings);

    // Create tests and collectors
    init(settings);
  }
}
