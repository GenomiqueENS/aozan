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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.aozan.collectors.stats.SampleStatisticsCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.AozanTestRegistry;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.tests.global.GlobalTest;
import fr.ens.biologie.genomique.aozan.tests.lane.LaneTest;
import fr.ens.biologie.genomique.aozan.tests.pooledsample.PooledSampleTest;
import fr.ens.biologie.genomique.aozan.tests.project.ProjectTest;
import fr.ens.biologie.genomique.aozan.tests.sample.SampleTest;
import fr.ens.biologie.genomique.aozan.util.XMLUtilsWriter;

/**
 * This class generate the QC Report.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class QCReport {

  private final RunData data;

  private final List<GlobalTest> globalTests = new ArrayList<>();
  private final List<LaneTest> laneTests = new ArrayList<>();
  private final List<ProjectTest> projectStatsTests = new ArrayList<>();
  private final List<PooledSampleTest> samplesStatsTests = new ArrayList<>();
  private final List<SampleTest> sampleTests = new ArrayList<>();
  private Document doc;

  //
  // Getters
  //

  /**
   * Get the data.
   * @return the RunData object
   */
  public RunData getData() {

    return this.data;
  }

  /**
   * Get Report as a XML Document.
   * @return a XML document
   * @throws AozanException if an error occurs while creating the report.
   */
  public Document toDocument() throws AozanException {

    if (this.doc == null) {
      doTests();
    }

    return this.doc;
  }

  //
  // Report computation methods
  //

  /**
   * Generate the QC report for global tests.
   * @param parentElement parent Element
   */
  private void doGlobalTests(final Document doc, final Element parentElement) {

    final Element root = doc.createElement("GlobalReport");
    parentElement.appendChild(root);

    final Element columns = doc.createElement("Columns");
    root.appendChild(columns);

    for (final GlobalTest test : this.globalTests) {
      final Element columnElement = doc.createElement("Column");
      columnElement.setAttribute("testname", test.getName());
      columnElement.setAttribute("description", test.getDescription());
      columnElement.setAttribute("unit", test.getUnit());
      columnElement.setTextContent(test.getColumnName());
      columns.appendChild(columnElement);
    }

    final Element runElement = doc.createElement("Run");
    root.appendChild(runElement);

    for (final GlobalTest test : this.globalTests) {
      final TestResult result = test.test(this.data);

      final Element testElement = doc.createElement("Test");
      testElement.setAttribute("name", test.getName());
      testElement.setAttribute("score", Integer.toString(result.getScore()));
      testElement.setAttribute("type", result.getType());
      testElement.setTextContent(result.getMessage());
      runElement.appendChild(testElement);
    }
  }

  /**
   * Generate the QC report for lane tests.
   * @param parentElement parent Element
   */
  private void doLanesTests(final Document doc, final Element parentElement) {

    final int readCount = this.data.getReadCount();
    final int laneCount = this.data.getLaneCount();

    final Element root = doc.createElement("ReadsReport");
    parentElement.appendChild(root);

    final Element columns = doc.createElement("Columns");
    root.appendChild(columns);

    for (final LaneTest test : this.laneTests) {
      final Element columnElement = doc.createElement("Column");
      columnElement.setAttribute("testname", test.getName());
      columnElement.setAttribute("description", test.getDescription());
      columnElement.setAttribute("unit", test.getUnit());
      columnElement.setTextContent(test.getColumnName());
      columns.appendChild(columnElement);
    }

    final Element reads = doc.createElement("Reads");
    root.appendChild(reads);

    for (int read = 1; read <= readCount; read++) {

      final int cycles = this.data.getReadCyclesCount(read);
      final boolean indexedRead = this.data.isReadIndexed(read);

      final Element readElement = doc.createElement("Read");
      readElement.setAttribute("number", Integer.toString(read));
      readElement.setAttribute("cycles", Integer.toString(cycles));
      readElement.setAttribute("indexed", Boolean.toString(indexedRead));
      reads.appendChild(readElement);

      for (int lane = 1; lane <= laneCount; lane++) {

        final Element laneElement = doc.createElement("Lane");
        laneElement.setAttribute("number", Integer.toString(lane));
        readElement.appendChild(laneElement);

        for (final LaneTest test : this.laneTests) {
          final TestResult result =
              test.test(this.data, read, indexedRead, lane);

          final Element testElement = doc.createElement("Test");
          testElement.setAttribute("name", test.getName());
          testElement.setAttribute("score",
              Integer.toString(result.getScore()));
          testElement.setAttribute("type", result.getType());
          testElement.setTextContent(result.getMessage());
          laneElement.appendChild(testElement);

        }
      }
    }
  }

  private void doProjectsStatsTests(final Document doc,
      final Element parentElement) {

    // Sort pooled samples
    final List<Integer> projectIds = this.data.getProjects();
    Collections.sort(projectIds, new RunData.ProjectComparator(data));

    final Element root = doc.createElement("ProjectsReport");
    parentElement.appendChild(root);

    final Element columns = doc.createElement("Columns");
    root.appendChild(columns);

    for (final ProjectTest test : this.projectStatsTests) {
      final Element columnElement = doc.createElement("Column");
      columnElement.setAttribute("testname", test.getName());
      columnElement.setAttribute("description", test.getDescription());
      columnElement.setAttribute("unit", test.getUnit());
      columnElement.setTextContent(test.getColumnName());
      columns.appendChild(columnElement);
    }

    final Element projectsElement = doc.createElement("Projects");
    root.appendChild(projectsElement);

    for (int projectId : projectIds) {
      final Element projectElement = doc.createElement("Project");
      projectElement.setAttribute("id", "" + projectId);
      projectElement.setAttribute("name", data.getProjectName(projectId));
      projectsElement.appendChild(projectElement);

      for (final ProjectTest test : this.projectStatsTests) {
        final TestResult result = test.test(this.data, projectId);

        final Element testElement = doc.createElement("Test");
        testElement.setAttribute("name", test.getName());
        testElement.setAttribute("score", Integer.toString(result.getScore()));
        testElement.setAttribute("type", result.getType());
        testElement.setTextContent(result.getMessage());
        projectElement.appendChild(testElement);

      }
    }
  }

  private void doSamplesStatsTests(final Document doc,
      final Element parentElement) {

    // Check needed to add this tests
    if (this.data.getProjectCount() > 1) {
      return;
    }

    final Element root = doc.createElement("SamplesStatsReport");
    parentElement.appendChild(root);

    final Element columns = doc.createElement("Columns");
    root.appendChild(columns);

    for (final PooledSampleTest test : this.samplesStatsTests) {
      final Element columnElement = doc.createElement("Column");
      columnElement.setAttribute("testname", test.getName());
      columnElement.setAttribute("description", test.getDescription());
      columnElement.setAttribute("unit", test.getUnit());
      columnElement.setTextContent(test.getColumnName());
      columns.appendChild(columnElement);
    }

    final Element samplesStatsElement = doc.createElement("SamplesStats");
    root.appendChild(samplesStatsElement);

    // Sort pooled samples
    final List<Integer> pooledSamples = data.getAllPooledSamples();
    Collections.sort(pooledSamples, new RunData.PooledSampleComparator(data));

    for (int pooledSampleId : pooledSamples) {
      final Element sampleStatsElement = doc.createElement("SampleStats");
      sampleStatsElement.setAttribute("id", "" + pooledSampleId);
      sampleStatsElement.setAttribute("name",
          this.data.getPooledSampleDemuxName(pooledSampleId));
      sampleStatsElement.setAttribute("description",
          this.data.getPooledSampleDescription(pooledSampleId));
      sampleStatsElement.setAttribute("index",
          this.data.getPooledSampleIndex(pooledSampleId));
      sampleStatsElement.setAttribute("project",
          this.data.getPooledSampleProjectName(pooledSampleId));
      samplesStatsElement.appendChild(sampleStatsElement);

      for (final PooledSampleTest test : this.samplesStatsTests) {
        final TestResult result = test.test(this.data, pooledSampleId);

        final Element testElement = doc.createElement("Test");
        testElement.setAttribute("name", test.getName());
        testElement.setAttribute("score", Integer.toString(result.getScore()));
        testElement.setAttribute("type", result.getType());
        testElement.setTextContent(result.getMessage());
        sampleStatsElement.appendChild(testElement);

      }
    }
  }

  /**
   * Generate the QC report for projects data.
   * @param parentElement parent Element
   */
  private void addElementForFilter(final Document doc,
      final Element parentElement) {

    final List<String> elements = new ArrayList<>();
    String filterType;

    if (this.data.getProjectCount() > 1) {
      filterType = "project";

      for (int projectId : this.data.getProjects()) {
        elements.add(this.data.getProjectName(projectId));
      }

    } else {

      filterType = "sample";
      elements.addAll(extractSamplesNamesInRun());

      // Remove Undetermined if exists
      elements.remove(SampleStatisticsCollector.UNDETERMINED_SAMPLE);
    }

    // Build map associate lanes number with project
    final ListMultimap<String, Integer> lanesNumberRelatedElement =
        extractLaneNumberRelatedProjectName(filterType);

    final Element filter = doc.createElement("TableFilter");
    parentElement.appendChild(filter);

    for (final String element : elements) {

      final Element elementFilter = doc.createElement("ElementFilter");
      elementFilter.setAttribute("classValue", "elementFilter");

      // Extract lanes number related project name
      final Set<Integer> lanesRelatedProject =
          Sets.newTreeSet(lanesNumberRelatedElement.get(element));

      // Build command javascript for filter line samples report by project
      elementFilter.setAttribute("cmdJS",
          "'" + Joiner.on(",").join(lanesRelatedProject) + "'");
      elementFilter.setAttribute("typeFilter", filterType);

      elementFilter.setTextContent(element);
      filter.appendChild(elementFilter);
    }

    // Add Element for undetermined lane
    final Element undeterminedLanes = doc.createElement("ElementFilter");
    undeterminedLanes.setAttribute("classValue", "elementFilter");

    // Build list lane number
    final List<Integer> s = new ArrayList<>();
    for (int lane = 1; lane <= this.data.getLaneCount(); lane++) {

      // Check lane is indexed
      if (this.data.isLaneIndexed(lane)) {
        s.add(lane);
      }
    }

    undeterminedLanes.setAttribute("cmdJS", "'" + Joiner.on(",").join(s) + "'");
    undeterminedLanes.setAttribute("typeFilter", filterType);

    undeterminedLanes.setTextContent("undetermined");
    filter.appendChild(undeterminedLanes);
  }

  /**
   * Generate the QC report for samples tests.
   * @param parentElement parent Element
   */
  private void doSamplesTests(final Document doc, final Element parentElement) {

    final int readCount = this.data.getReadCount();
    final int laneCount = this.data.getLaneCount();

    final Element root = doc.createElement("SamplesReport");
    parentElement.appendChild(root);

    final Element columns = doc.createElement("Columns");
    root.appendChild(columns);

    for (final SampleTest test : this.sampleTests) {
      final Element columnElement = doc.createElement("Column");
      columnElement.setAttribute("testname", test.getName());
      columnElement.setAttribute("description", test.getDescription());
      columnElement.setAttribute("unit", test.getUnit());
      columnElement.setTextContent(test.getColumnName());
      columns.appendChild(columnElement);
    }

    final Element reads = doc.createElement("Reads");
    root.appendChild(reads);
    int readSample = 0;

    for (int read = 1; read <= readCount; read++) {

      if (this.data.isReadIndexed(read)) {
        continue;
      }

      readSample++;

      final Element readElement = doc.createElement("Read");
      readElement.setAttribute("number", Integer.toString(readSample));

      reads.appendChild(readElement);

      for (int lane = 1; lane <= laneCount; lane++) {

        final List<Integer> sampleIds = this.data.getAllSamplesInLane(lane);

        for (final int sampleId : sampleIds) {

          // Get the sample name
          final String sampleName = this.data.getSampleDemuxName(sampleId);

          // Get the sample index
          final String index = this.data.getIndexSample(sampleId);

          // Get the sample description
          String desc = this.data.getSampleDescription(sampleId);
          desc = "".equals(desc) ? "No description" : desc;

          // Get the sample project
          String projectName = this.data.getProjectSample(sampleId);
          projectName =
              "".equals(projectName) ? "Undefined project" : projectName;

          final String indexString;

          if (this.data.isUndeterminedSample(sampleId)) {
            indexString = "undetermined";
            projectName = "undetermined";
          } else {
            if (this.data.isIndexedSample(sampleId)) {
              indexString = index;
            } else {
              indexString = "NoIndex";
            }
          }
          addSample(doc, readElement, read, readSample, sampleId, lane,
              sampleName, desc, projectName, indexString);
        }
      }
    }
  }

  private void addSample(final Document doc, final Element readElement,
      final int read, final int readSample, final int sampleId, final int lane,
      final String sampleName, final String desc, final String projectName,
      final String index) {

    final Element sampleElement = doc.createElement("Sample");
    sampleElement.setAttribute("id", "" + sampleId);
    sampleElement.setAttribute("name",
        sampleName == null ? "undetermined" : sampleName);
    sampleElement.setAttribute("desc", desc == null ? "No description" : desc);
    sampleElement.setAttribute("project",
        projectName == null ? "undetermined" : projectName);
    sampleElement.setAttribute("lane", Integer.toString(lane));
    sampleElement.setAttribute("index", index);

    readElement.appendChild(sampleElement);

    for (final SampleTest test : this.sampleTests) {

      final TestResult result =
          test.test(this.data, read, readSample, sampleId);

      final Element testElement = doc.createElement("Test");
      testElement.setAttribute("name", test.getName());
      testElement.setAttribute("score", Integer.toString(result.getScore()));
      testElement.setAttribute("type", result.getType());
      testElement.setTextContent(result.getMessage());
      sampleElement.appendChild(testElement);
    }
  }

  /**
   * Create the QC report.
   * @throws AozanException if an error occurs while creating the report
   */
  private void doTests() throws AozanException {

    try {

      final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      final DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
      final Document doc = docBuilder.newDocument();

      // Create the root element and add it to the document
      final Element root = doc.createElement("QCReport");
      root.setAttribute("formatversion", "1.0");
      doc.appendChild(root);

      // Common tag header in document xml
      XMLUtilsWriter.buildXMLCommonTagHeader(doc, root, this.data);

      if (!this.globalTests.isEmpty()) {
        doGlobalTests(doc, root);
      }

      if (!this.laneTests.isEmpty()) {
        doLanesTests(doc, root);
      }

      if (!this.projectStatsTests.isEmpty()) {
        doProjectsStatsTests(doc, root);
      }

      if (!this.samplesStatsTests.isEmpty()) {
        doSamplesStatsTests(doc, root);
      }

      if (!this.sampleTests.isEmpty()) {
        addElementForFilter(doc, root);
        doSamplesTests(doc, root);
      }

      this.doc = doc;

    } catch (final ParserConfigurationException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Collect lanes number for each project name to run.
   * @param typeFilter filter type
   * @return map associate project name related lane number
   */
  private ListMultimap<String, Integer> extractLaneNumberRelatedProjectName(
      final String typeFilter) {

    final ListMultimap<String, Integer> nameWithLaneNumber =
        ArrayListMultimap.create();

    final int laneCount = this.data.getLaneCount();

    // Parse lane run
    for (int lane = 1; lane <= laneCount; lane++) {

      // Extract samples name in lane
      final List<Integer> samplesInLane = this.data.getSamplesInLane(lane);

      switch (typeFilter) {

      case "sample":

        for (final int sampleId : samplesInLane) {
          nameWithLaneNumber.put(this.data.getSampleDemuxName(sampleId), lane);
        }

        break;

      default:

        for (final int sampleId : samplesInLane) {

          // Extract project name corresponding to sample name
          final String projectName = this.data.getProjectSample(sampleId);

          if (projectName != null) {
            // Add project name and lane number in map
            nameWithLaneNumber.put(projectName, lane);
          }
        }
        break;

      }

    }

    return nameWithLaneNumber;
  }

  /**
   * Extract samples names in run from Rundata instance.
   * @return the list of samples names
   */
  private List<String> extractSamplesNamesInRun() {

    final Set<String> names = new TreeSet<>();

    final int laneCount = this.data.getLaneCount();

    for (int lane = 1; lane <= laneCount; lane++) {
      // Add all samples names by lane and ignore replica
      for (int sampleId : this.data.getSamplesInLane(lane)) {
        names.add(this.data.getSampleDemuxName(sampleId));
      }

      // Add Undetermined sample
      if (this.data.isUndeterminedInLane(lane)) {
        names.add(SampleStatisticsCollector.UNDETERMINED_SAMPLE);
      }
    }

    return Collections.unmodifiableList(new ArrayList<>(names));
  }

  //
  // Filtering methods
  //

  /**
   * Filter test entry in the report
   * @param testNames name of the tests to remove
   */
  public void filterTests(Collection<String> testNamesToRemove) {

    requireNonNull(testNamesToRemove);
    this.doc = null;

    final AozanTestRegistry registry = new AozanTestRegistry();

    for (String tn : testNamesToRemove) {

      final String testFullName = tn.substring(QC.TEST_KEY_PREFIX.length());
      final AozanTest test = registry.get(testFullName);

      if (test != null) {

        // Filter global tests
        if (test instanceof GlobalTest) {
          for (GlobalTest t : new ArrayList<>(this.globalTests)) {
            if (test.getName().equals(t.getName())) {
              this.globalTests.remove(t);
            }
          }
        }

        // Filter lane tests
        if (test instanceof LaneTest) {
          for (LaneTest t : new ArrayList<>(this.laneTests)) {
            if (test.getName().equals(t.getName())) {
              this.laneTests.remove(t);
            }
          }
        }

        // Filter project tests
        if (test instanceof ProjectTest) {
          for (ProjectTest t : new ArrayList<>(this.projectStatsTests)) {
            if (test.getName().equals(t.getName())) {
              this.projectStatsTests.remove(t);
            }
          }
        }

        // Filter pooled sample tests
        if (test instanceof PooledSampleTest) {
          for (PooledSampleTest t : new ArrayList<>(this.samplesStatsTests)) {
            if (test.getName().equals(t.getName())) {
              this.samplesStatsTests.remove(t);
            }
          }
        }

        // Filter sample tests
        if (test instanceof SampleTest) {
          for (SampleTest t : new ArrayList<>(this.sampleTests)) {
            if (test.getName().equals(t.getName())) {
              this.sampleTests.remove(t);
            }
          }
        }
      }
    }

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param data Run data
   * @param globalTests list of the global tests
   * @param laneTests list of the read tests
   * @param projectStatsTests the project stats tests
   * @param samplesStatsTests the samples stats tests
   * @param sampleTests list of the sample tests
   */
  public QCReport(final RunData data, final List<GlobalTest> globalTests,
      final List<LaneTest> laneTests, final List<ProjectTest> projectStatsTests,
      final List<PooledSampleTest> samplesStatsTests,
      final List<SampleTest> sampleTests) {

    this.data = data;

    if (globalTests != null) {
      this.globalTests.addAll(globalTests);
    }

    if (laneTests != null) {
      this.laneTests.addAll(laneTests);
    }

    if (projectStatsTests != null) {
      this.projectStatsTests.addAll(projectStatsTests);
    }

    if (samplesStatsTests != null) {
      this.samplesStatsTests.addAll(samplesStatsTests);
    }

    if (sampleTests != null) {
      this.sampleTests.addAll(sampleTests);
    }
  }

  /**
   * Copy constructor.
   * @param report report to copy
   */
  public QCReport(QCReport report) {

    requireNonNull(report);

    this.data = new RunData(report.data);
    this.globalTests.addAll(report.globalTests);
    this.laneTests.addAll(report.laneTests);
    this.projectStatsTests.addAll(report.projectStatsTests);
    this.samplesStatsTests.addAll(report.samplesStatsTests);
    this.sampleTests.addAll(report.sampleTests);
    this.doc = null;
  }
}
