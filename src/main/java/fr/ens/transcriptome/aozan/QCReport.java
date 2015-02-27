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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.aozan.tests.TestResult;
import fr.ens.transcriptome.aozan.tests.global.GlobalTest;
import fr.ens.transcriptome.aozan.tests.lane.LaneTest;
import fr.ens.transcriptome.aozan.tests.project.ProjectTest;
import fr.ens.transcriptome.aozan.tests.sample.SampleTest;
import fr.ens.transcriptome.aozan.util.XMLUtilsWriter;

/**
 * This class generate the QC Report.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class QCReport {
  /** Splitter */
  private static final Splitter COMMA_SPLITTER = Splitter.on(",").trimResults()
      .omitEmptyStrings();

  private final RunData data;

  private final List<GlobalTest> globalTests = new ArrayList<>();
  private final List<LaneTest> laneTests = new ArrayList<>();
  private final List<ProjectTest> projectTests = new ArrayList<>();
  private final List<SampleTest> sampleTests = new ArrayList<>();
  private Document doc;

  /**
   * Get the data.
   * @return the RunData object
   */
  public RunData getData() {

    return this.data;
  }

  /**
   * Generate the QC report for global tests.
   * @param parentElement parent Element
   */
  private void doGlobalTests(final Element parentElement) {

    final Document doc = this.doc;

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
  private void doLanesTests(final Element parentElement) {

    final Document doc = this.doc;
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
          testElement
              .setAttribute("score", Integer.toString(result.getScore()));
          testElement.setAttribute("type", result.getType());
          testElement.setTextContent(result.getMessage());
          laneElement.appendChild(testElement);

        }
      }
    }
  }

  private void doProjectsTests(final Element parentElement) {

    final Document doc = this.doc;
    final List<String> projects = this.data.getProjectsNameList();

    final Element root = doc.createElement("ProjectsReport");
    parentElement.appendChild(root);

    final Element columns = doc.createElement("Columns");
    root.appendChild(columns);

    for (final ProjectTest test : this.projectTests) {
      final Element columnElement = doc.createElement("Column");
      columnElement.setAttribute("testname", test.getName());
      columnElement.setAttribute("description", test.getDescription());
      columnElement.setAttribute("unit", test.getUnit());
      columnElement.setTextContent(test.getColumnName());
      columns.appendChild(columnElement);
    }

    final Element projectsElement = doc.createElement("Projects");
    root.appendChild(projectsElement);

    for (String project : projects) {
      final Element projectElement = doc.createElement("Project");
      projectElement.setAttribute("name", project);
      projectsElement.appendChild(projectElement);

      for (final ProjectTest test : this.projectTests) {
        final TestResult result = test.test(this.data, project);

        final Element testElement = doc.createElement("Test");
        testElement.setAttribute("name", test.getName());
        testElement.setAttribute("score", Integer.toString(result.getScore()));
        testElement.setAttribute("type", result.getType());
        testElement.setTextContent(result.getMessage());
        projectElement.appendChild(testElement);

      }
    }
  }

  /**
   * Generate the QC report for projects data.
   * @param parentElement parent Element
   */
  private void doProjectsNavigation(final Element parentElement) {

    final Document doc = this.doc;

    // Add undetermined sample
    final String list = this.data.getProjectsName();
    // Add projects name
    final List<String> projectsName = COMMA_SPLITTER.splitToList(list);

    // Build map associate lanes number with project
    final ListMultimap<String, Integer> lanesNumberRelatedProjectName =
        extractLaneNumberRelatedProjectName();

    final Element projects = doc.createElement("ProjectsReport");
    parentElement.appendChild(projects);

    for (final String projectName : projectsName) {

      final Element project = doc.createElement("ProjectName");
      project.setAttribute("classValue", "projectName");

      // Extract lanes number related project name
      final Set<Integer> lanesRelatedProject =
          Sets.newTreeSet(lanesNumberRelatedProjectName.get(projectName));

      // Build command javascript for filter line samples report by project
      project.setAttribute("cmdJS",
          "'" + Joiner.on(",").join(lanesRelatedProject) + "'");

      project.setTextContent(projectName);
      projects.appendChild(project);
    }

    // Add Element for undetermined lane
    final Element undeterminedLanes = doc.createElement("ProjectName");
    undeterminedLanes.setAttribute("classValue", "projectName");

    // Build list lane number
    final List<Integer> s = new ArrayList<>();
    for (int lane = 1; lane <= this.data.getLaneCount(); lane++) {

      // Check lane is indexed
      if (this.data.isLaneIndexed(lane)) {
        s.add(lane);
      }
    }

    undeterminedLanes.setAttribute("cmdJS", "'" + Joiner.on(",").join(s) + "'");

    undeterminedLanes.setTextContent("Undetermined");
    projects.appendChild(undeterminedLanes);
  }

  /**
   * Collect lanes number for each project name to run.
   * @return map associate project name related lane number
   */
  private ListMultimap<String, Integer> extractLaneNumberRelatedProjectName() {

    final ListMultimap<String, Integer> projectsNameWithLaneNumber =
        ArrayListMultimap.create();

    final int laneCount = this.data.getLaneCount();

    // Parse lane run
    for (int lane = 1; lane <= laneCount; lane++) {

      // Extract samples name in lane
      final List<String> samplesNameInLane =
          COMMA_SPLITTER.splitToList(this.data.getSamplesNameInLane(lane));

      for (final String sampleName : samplesNameInLane) {
        // Extract project name corresponding to sample name
        final String key =
            "design.lane" + lane + "." + sampleName + ".sample.project";

        final String projectName = this.data.get(key);
        if (projectName != null) {
          // Add project name and lane number in map
          projectsNameWithLaneNumber.put(projectName, lane);
        }
      }
    }

    return projectsNameWithLaneNumber;
  }

  /**
   * Generate the QC report for samples tests.
   * @param parentElement parent Element
   */
  private void doSamplesTests(final Element parentElement) {

    final Document doc = this.doc;
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

        final List<String> sampleNames =
            this.data.getSamplesNameListInLane(lane);

        final String firstIndex =
            this.data.getIndexSample(lane, sampleNames.get(0));
        final boolean noIndex =
            sampleNames.size() == 1 && "".equals(firstIndex);

        for (final String sampleName : sampleNames) {

          // Get the sample index
          final String index = this.data.getIndexSample(lane, sampleName);

          // Get the sample description
          final String desc = this.data.getSampleDescription(lane, sampleName);

          // Get the sample project
          final String projectName =
              this.data.getProjectSample(lane, sampleName);

          addSample(readElement, read, readSample, lane, sampleName, desc,
              projectName, noIndex ? "NoIndex" : index);
        }

        // Undetermined indexes
        if (!noIndex) {
          addSample(readElement, read, readSample, lane, null, null, null,
              "Undetermined");
        }
      }
    }
  }

  private void addSample(final Element readElement, final int read,
      final int readSample, final int lane, final String sampleName,
      final String desc, final String projectName, final String index) {

    final Element sampleElement = this.doc.createElement("Sample");
    sampleElement.setAttribute("name", sampleName == null
        ? "Undetermined" : sampleName);
    sampleElement.setAttribute("desc", desc == null ? "No description" : desc);
    sampleElement.setAttribute("project", projectName == null
        ? "Undetermined" : projectName);
    sampleElement.setAttribute("lane", Integer.toString(lane));
    sampleElement.setAttribute("index", index);

    readElement.appendChild(sampleElement);

    for (final SampleTest test : this.sampleTests) {

      final TestResult result =
          test.test(this.data, read, readSample, lane, sampleName);

      final Element testElement = this.doc.createElement("Test");
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

    if (this.doc != null) {
      return;
    }

    try {

      final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      final DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
      final Document doc = this.doc = docBuilder.newDocument();

      // Create the root element and add it to the document
      final Element root = doc.createElement("QCReport");
      root.setAttribute("formatversion", "1.0");
      doc.appendChild(root);

      // Common tag header in document xml
      XMLUtilsWriter.buildXMLCommonTagHeader(doc, root, this.data);

      if (!this.globalTests.isEmpty()) {
        doGlobalTests(root);
      }

      if (!this.laneTests.isEmpty()) {
        doLanesTests(root);
      }

      if (!this.projectTests.isEmpty()) {
        doProjectsTests(root);
      }

      if (!this.sampleTests.isEmpty()) {
        doProjectsNavigation(root);
        doSamplesTests(root);
      }
    } catch (final ParserConfigurationException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Create the QC report as an XML String.
   * @return a String with the report in XML
   * @throws AozanException if an error occurs while creating the report
   */
  public String toXML() throws AozanException, IOException {

    doTests();

    return XMLUtilsWriter.createXMLFile(this.doc);

  }

  /**
   * Export the QC report. The XML report is transformed using an XSL style
   * sheet.
   * @param XSLFile XSL file
   * @return the QC report as a String
   * @throws AozanException if an error occurs while creating the report
   */
  public String export(final File XSLFile) throws AozanException {

    try {
      return export(new FileInputStream(XSLFile));
    } catch (final FileNotFoundException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Export the QC report. The XML report is transformed using an XSL style
   * sheet.
   * @param is XSL file as input stream
   * @return the QC report as a String
   * @throws AozanException if an error occurs while creating the report
   */
  public String export(final InputStream is) throws AozanException {

    if (is == null) {
      throw new NullPointerException(
          "The input stream for the XSL stylesheet is null.");
    }

    doTests();

    return XMLUtilsWriter.createHTMLFileFromXSL(this.doc, is);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param data Run data
   * @param globalTests list of the global tests
   * @param laneTests list of the read tests
   * @param projectTests
   * @param sampleTests list of the sample tests
   */
  public QCReport(final RunData data, final List<GlobalTest> globalTests,
      final List<LaneTest> laneTests, List<ProjectTest> projectTests,
      final List<SampleTest> sampleTests) {

    this.data = data;

    if (globalTests != null) {
      this.globalTests.addAll(globalTests);
    }

    if (laneTests != null) {
      this.laneTests.addAll(laneTests);
    }

    if (projectTests != null) {
      this.projectTests.addAll(projectTests);
    }

    if (sampleTests != null) {
      this.sampleTests.addAll(sampleTests);
    }
  }

}
