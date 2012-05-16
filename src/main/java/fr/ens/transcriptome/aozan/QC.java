/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.CollectorRegistry;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.AozanTestRegistry;
import fr.ens.transcriptome.aozan.tests.LaneTest;
import fr.ens.transcriptome.aozan.tests.SampleTest;

public class QC {

  private static final String TEST_KEY_ENABLED_SUFFIX = ".enable";
  private static final String TEST_KEY_PREFIX = "qc.test.";

  private List<Collector> collectors = Lists.newArrayList();
  private List<LaneTest> laneTests = Lists.newArrayList();
  private List<SampleTest> sampleTests = Lists.newArrayList();
  private Map<String, String> additionalConf = Maps.newHashMap();
  private File tmpDir;

  /**
   * Process data.
   * @param bclDir BCL directory
   * @param fastqDir fastq directory
   * @param runId run id
   * @throws AozanException if an error occurs while computing report
   */
  public final QCReport computeReport(final String bclDir,
      final String fastqDir, final String qcDir, final String runId)
      throws AozanException {

    final File RTAOutputDir = new File(bclDir);
    final File casavaOutputDir = new File(fastqDir);
    final File QCOutputDir = new File(qcDir);

    if (RTAOutputDir == null
        || !RTAOutputDir.exists() || !RTAOutputDir.isDirectory())
      throw new AozanException(
          "The BCL directory does not exist or is not a directory: "
              + RTAOutputDir);

    if (casavaOutputDir == null
        || !casavaOutputDir.exists() || !casavaOutputDir.isDirectory())
      throw new AozanException(
          "The Casava output directory does not exist or is not a directory: "
              + casavaOutputDir);

    if (QCOutputDir == null
        || (QCOutputDir.exists() && !QCOutputDir.isDirectory()))
      throw new AozanException(
          "The QC directory does not exist or is not a directory: "
              + QCOutputDir);

    if (!QCOutputDir.exists())
      if (!QCOutputDir.mkdirs())
        throw new AozanException("Cannot create QC directory : " + QCOutputDir);

    File[] designFiles = casavaOutputDir.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {

        return name.endsWith(".csv");
      }
    });

    if (designFiles == null || designFiles.length == 0)
      throw new AozanException("No Casava design file found in "
          + casavaOutputDir);

    final File casavaDesignFile = designFiles[0];

    // Create RunDataGenerator object
    final RunDataGenerator rdg = new RunDataGenerator(this.collectors);

    // Set the parameters of the generator
    rdg.setRTAOutputDir(RTAOutputDir);
    rdg.setCasavaDesignFile(casavaDesignFile);
    rdg.setCasavaOutputDir(casavaOutputDir);
    rdg.setQCOutputDir(QCOutputDir);
    rdg.setTemporaryDir(this.tmpDir);
    rdg.setAdditionnalConf(this.additionalConf);

    // Create the run data object
    final RunData data = rdg.collect();

    if (data.size() == 0)
      throw new AozanException("No data collected.");

    // Print the content of the run data object
    // data.print();

    // Create the report
    return new QCReport(data, this.laneTests, this.sampleTests);
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
      final Writer writer = new FileWriter(outputFile);

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

    try {
      final InputStream is;

      if (stylesheetFilename == null)
        is = this.getClass().getResourceAsStream(Globals.EMBEDDED_QC_XSL);
      else
        is = new FileInputStream(stylesheetFilename);

      writeReport(report, is, new File(outputFilename));
    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Write the report usually in HTML) in a file.
   * @param report the QCReport object
   * @param xslIs XSL stylesheet input strea,
   * @param outputFile the report file
   * @throws AozanException if an error occurs while creating the report
   */
  public void writeReport(final QCReport report, final InputStream xslIs,
      final File outputFile) throws AozanException {

    try {
      final Writer writer = new FileWriter(outputFile);

      writer.write(report.export(xslIs));

      writer.close();
    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Write the raw data of the QC.
   * @param report the QCReport object
   * @param outputFile the raw data file
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
      final Writer writer = new FileWriter(outputFile);

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
          mapTests.put(key, test);

          // Add the test to laneTests or sampleTests
          if (test instanceof LaneTest)
            this.laneTests.add((LaneTest) test);
          else if (test instanceof SampleTest)
            this.sampleTests.add((SampleTest) test);

          // Configure the test
          configureTest(test, properties, TEST_KEY_PREFIX + testName + ".");

        } else
          throw new AozanException("No test found for property: " + key);
      }

      if (key.startsWith("qc.conf."))
        this.additionalConf.put(key, value);

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
   * @param enableKey key that enable the test
   * @throws AozanException if an error occurs while configuring the test
   */
  private final void configureTest(final AozanTest test,
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
    }

    test.configure(conf);
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
   * Transform a list of collector names in a set collectors objects.
   * @param collectorNames list of collectors names
   * @return a set of collectors objects
   * @throws AozanException if an error occurs while creating a collector
   */
  private Set<Collector> getCollectors(String[] collectorNames)
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
  private void addCollectors(final String[] collectorNamesRequired,
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
   * @param tmpDirname temporary directory path
   * @throws AozanException if an error occurs while initialize the QC object
   */
  public QC(final Map<String, String> properties, final String tmpDirname)
      throws AozanException {

    this(properties, tmpDirname == null ? null : new File(tmpDirname));
  }

  /**
   * Public constructor.
   * @param properties QC properties
   * @param tmpDir temporary directory
   * @throws AozanException if an error occurs while initialize the QC object
   */
  public QC(final Map<String, String> properties, final File tmpDir)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    this.tmpDir =
        tmpDir == null
            ? new File(System.getProperty("java.io.tmpdir")) : tmpDir;
    init(properties);
  }

}
