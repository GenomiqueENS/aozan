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
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.tests.LaneTest;
import fr.ens.transcriptome.aozan.tests.SampleTest;
import fr.ens.transcriptome.aozan.tests.TestResult;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class generate the QC Report.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class QCReport {

  private final RunData data;

  private List<LaneTest> laneTests = Lists.newArrayList();
  private List<SampleTest> sampleTests = Lists.newArrayList();
  private Document doc;

  /**
   * Get the data.
   * @return the RunData object
   */
  public RunData getData() {

    return this.data;
  }

  /**
   * Generate the QC report for lane tests.
   * @param parentElement parent Element
   */
  private void doLanesTests(final Element parentElement) {

    final Document doc = this.doc;
    final int readCount = data.getInt("run.info.read.count");
    final int laneCount = data.getInt("run.info.flow.cell.lane.count");

    final Element root = doc.createElement("ReadsReport");
    parentElement.appendChild(root);

    final Element columns = doc.createElement("Columns");
    root.appendChild(columns);

    for (LaneTest test : laneTests) {
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

      final int cycles = data.getInt("run.info.read" + read + ".cycles");
      final boolean indexedRead =
          data.getBoolean("run.info.read" + read + ".indexed");

      final Element readElement = doc.createElement("Read");
      readElement.setAttribute("number", Integer.toString(read));
      readElement.setAttribute("cycles", Integer.toString(cycles));
      readElement.setAttribute("indexed", Boolean.toString(indexedRead));
      reads.appendChild(readElement);

      for (int lane = 1; lane <= laneCount; lane++) {

        final Element laneElement = doc.createElement("Lane");
        laneElement.setAttribute("number", Integer.toString(lane));
        readElement.appendChild(laneElement);

        for (LaneTest test : laneTests) {
          final TestResult result = test.test(data, read, indexedRead, lane);

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

  /**
   * Generate the QC report for samples tests.
   * @param parentElement parent Element
   */
  private void doSamplesTests(final Element parentElement) {

    final Document doc = this.doc;
    final int readCount = data.getInt("run.info.read.count");
    final int laneCount = data.getInt("run.info.flow.cell.lane.count");

    final Element root = doc.createElement("SamplesReport");
    parentElement.appendChild(root);

    final Element columns = doc.createElement("Columns");
    root.appendChild(columns);

    for (SampleTest test : this.sampleTests) {
      Element columnElement = doc.createElement("Column");
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

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;

      readSample++;

      final Element readElement = doc.createElement("Read");
      readElement.setAttribute("number", Integer.toString(readSample));

      reads.appendChild(readElement);

      for (int lane = 1; lane <= laneCount; lane++) {

        final List<String> sampleNames =
            Lists.newArrayList(Splitter.on(',').split(
                data.get("design.lane" + lane + ".samples.names")));

        final String firstIndex =
            data.get("design.lane" + lane + "." + sampleNames.get(0) + ".index");
        final boolean noIndex =
            sampleNames.size() == 1 && "".equals(firstIndex);

        for (String sampleName : sampleNames) {

          // Get the sample index
          final String index =
              data.get("design.lane" + lane + "." + sampleName + ".index");

          // Get the sample description
          final String desc =
              data.get("design.lane" + lane + "." + sampleName + ".description");

          // Get the sample project
          final String projectName =
              data.get("design.lane"
                  + lane + "." + sampleName + ".sample.project");

          addSample(readElement, read, readSample, lane, sampleName, desc,
              projectName, noIndex ? "NoIndex" : index);
        }

        // Undetermined indexes
        if (!noIndex)
          addSample(readElement, read, readSample, lane, null, null, null,
              "Undetermined");
      }
    }
  }

  private void addSample(final Element readElement, final int read,
      final int readSample, final int lane, final String sampleName,
      final String desc, final String projectName, final String index) {

    final Element sampleElement = doc.createElement("Sample");
    sampleElement.setAttribute("name", sampleName == null
        ? "Undetermined" : sampleName);
    sampleElement.setAttribute("desc", desc == null ? "No description" : desc);
    sampleElement.setAttribute("project", projectName == null
        ? "Undetermined" : projectName);
    sampleElement.setAttribute("lane", Integer.toString(lane));
    sampleElement.setAttribute("index", index);

    readElement.appendChild(sampleElement);

    for (SampleTest test : this.sampleTests) {

      final TestResult result =
          test.test(data, read, readSample, lane, sampleName);

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

    if (this.doc != null)
      return;

    try {
      DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
      final Document doc = this.doc = docBuilder.newDocument();

      // Create the root element and add it to the document
      Element root = doc.createElement("QCReport");
      root.setAttribute("formatversion", "1.0");
      doc.appendChild(root);

      XMLUtils.addTagValue(doc, root, "GeneratorName",
          Globals.APP_NAME_LOWER_CASE);
      XMLUtils.addTagValue(doc, root, "GeneratorVersion",
          Globals.APP_VERSION_STRING);
      XMLUtils
          .addTagValue(doc, root, "RunId", this.data.get("run.info.run.id"));
      XMLUtils
          .addTagValue(doc, root, "RunDate", this.data.get("run.info.date"));
      XMLUtils.addTagValue(doc, root, "FlowcellId",
          this.data.get("run.info.flow.cell.id"));
      XMLUtils.addTagValue(doc, root, "InstrumentSN",
          this.data.get("run.info.instrument"));
      XMLUtils.addTagValue(doc, root, "InstrumentRunNumber",
          this.data.get("run.info.run.number"));
      XMLUtils.addTagValue(doc, root, "ReportDate", new Date().toString());

      doLanesTests(root);
      doSamplesTests(root);

    } catch (ParserConfigurationException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Create the QC report as an XML String.
   * @return a String with the report in XML
   * @throws AozanException if an error occurs while creating the report
   */
  public String toXML() throws AozanException {

    doTests();

    try {
      // set up a transformer
      TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer trans = transfac.newTransformer();
      // trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      trans.setOutputProperty(OutputKeys.INDENT, "yes");
      trans.setOutputProperty(OutputKeys.METHOD, "xml");

      // create string from xml tree
      StringWriter sw = new StringWriter();
      StreamResult result = new StreamResult(sw);
      DOMSource source = new DOMSource(doc);
      trans.transform(source, result);

      return sw.toString();

    } catch (TransformerException e) {
      throw new AozanException(e);
    }
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
    } catch (FileNotFoundException e) {
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

    if (is == null)
      throw new NullPointerException(
          "The input stream for the XSL stylesheet is null.");

    doTests();

    try {

      // Create the transformer
      final Transformer transformer =
          TransformerFactory.newInstance().newTransformer(
              new javax.xml.transform.stream.StreamSource(is));

      // Create the String writer
      final StringWriter writer = new StringWriter();

      // Transform the document
      transformer.transform(new DOMSource(this.doc), new StreamResult(writer));

      // Close input stream
      is.close();

      // Return the result of the transformation
      return writer.toString();

    } catch (TransformerException e) {
      throw new AozanException(e);
    } catch (IOException e) {
      throw new AozanException(e);
    }

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param data Run data
   * @param laneTests list of the read tests
   * @param sampleTests list of the sample tests
   */
  public QCReport(RunData data, List<LaneTest> laneTests,
      List<SampleTest> sampleTests) {

    this.data = data;

    if (laneTests != null)
      this.laneTests.addAll(laneTests);

    if (sampleTests != null)
      this.sampleTests.addAll(sampleTests);
  }

}
