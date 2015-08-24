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
package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.collectors.stats.EntityStat;
import fr.ens.transcriptome.aozan.util.XMLUtilsWriter;

/**
 * The class set a project HTML report file on detection contamination by
 * compilation report on samples.
 * @author Sandrine Perrin
 * @since 1.4
 */
public class FastqScreenProjectReport {

  /** Project data instance. */
  private final EntityStat entitiesStat;

  /** Stylesheet xsl file. */
  private final File fastqscreenXSLFile;

  /**
   * Creates the HTML report.
   * @param reportHtml the report HTML.
   * @throws AozanException the Aozan exception
   * @throws IOException if a error occurs when create report HTML.
   */
  public void createReport(final File reportHtml) throws AozanException,
      IOException {

    // Call stylesheet file for report
    InputStream is = null;
    if (fastqscreenXSLFile == null) {

      // Use default stylesheet file
      is =
          this.getClass().getResourceAsStream(
              Globals.EMBEDDED_FASTQSCREEN_PROJECT_XSL);
    } else {
      // Use specific stylesheet from properties
      is = new FileInputStream(fastqscreenXSLFile);
    }

    // Build document instance
    final Document doc = buildDoc();

    XMLUtilsWriter.createXMLFile(doc);

    XMLUtilsWriter.createHTMLFileFromXSL(doc, is, reportHtml);
  }

  /**
   * Builds the document xml on project by extraction data from sample xml file.
   * @return the document
   * @throws AozanException if an error occurs during extract data from sample
   *           xml
   */
  private Document buildDoc() throws AozanException {

    boolean runDataAdded = false;

    DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();

    DocumentBuilder docBuilder = null;
    Document doc = null;

    try {
      docBuilder = dbfac.newDocumentBuilder();
      doc = docBuilder.newDocument();

    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }

    // Create the root element and add it to the document
    Element root = doc.createElement("ReportFastqScreen");
    root.setAttribute("formatversion", "1.0");
    doc.appendChild(root);

    // Create a project element
    final Element project = doc.createElement("project");
    root.appendChild(project);

    // Parsing all sample on project
    for (File fqsxml : entitiesStat.getFastqScreenReport()) {

      // Buid document on sample xml file
      final Document srcDoc = buildDom(fqsxml);

      if (!runDataAdded) {
        // Extract run data from first xml file
        extractRunData(doc, root, srcDoc);
        runDataAdded = true;
      }

      // Set project name
      final String name = fqsxml.getName().substring(0, 10);

      // Create a sample element
      Element sample = doc.createElement("sample");
      sample.setAttribute("name", name);
      project.appendChild(sample);

      // Add sample data in project document
      extractSampleData(doc, sample, srcDoc);

    }

    // Create project element with all genomes names
    final String textContent = compileGenomesInProject(doc);

    final Element e = doc.createElement("genomesProject");
    e.setTextContent(textContent);
    root.appendChild(e);

    // Return document on project
    return doc;
  }

  /**
   * Builds the DOM from a XML file.
   * @param xmlFile the xml file.
   * @return the document from xml file.
   * @throws AozanException if an error occurs when build DOM file.
   */
  private Document buildDom(final File xmlFile) throws AozanException {

    Document doc = null;

    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder;
      dBuilder = dbFactory.newDocumentBuilder();

      doc = dBuilder.parse(xmlFile);

    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new AozanException(e);
    }

    if (doc == null) {
      throw new AozanException("Fail to create document from file "
          + xmlFile.getAbsolutePath() + " for "
          + entitiesStat.getName());
    }

    doc.getDocumentElement().normalize();

    return doc;
  }

  /**
   * Extract run data on sample DOM to add in project DOM.
   * @param destDocument the destination document.
   * @param root the parent element to add in destination document.
   * @param srcDocument the source document.
   */
  private void extractRunData(final Document destDocument, final Element root,
      final Document srcDocument) {

    // Tag name to extract
    final List<String> runDataTag = new ArrayList<>();
    runDataTag.add("GeneratorName");
    runDataTag.add("GeneratorVersion");
    runDataTag.add("GeneratorWebsite");
    runDataTag.add("GeneratorRevision");
    runDataTag.add("RunId");
    runDataTag.add("RunDate");
    runDataTag.add("FlowcellId");
    runDataTag.add("InstrumentSN");
    runDataTag.add("InstrumentRunNumber");
    runDataTag.add("ReportDate");
    runDataTag.add("projectName");

    // Extract
    extractDataFromDOMtoDOM(destDocument, root, srcDocument, runDataTag);

  }

  /**
   * Extract sample data from DOM on sample to add in DOM on project.
   * @param destDocument the destination document, project
   * @param sample the sample element
   * @param srcDocument the source document, sample
   */
  private void extractSampleData(final Document destDocument,
      final Element sample, final Document srcDocument) {

    // Tags name to extract
    final List<String> sampleDataTag = new ArrayList<>();
    sampleDataTag.add("sampleName");
    sampleDataTag.add("genomeSample");
    sampleDataTag.add("descriptionSample");
    sampleDataTag.add("lane");
    sampleDataTag.add("Report");

    // Extract
    extractDataFromDOMtoDOM(destDocument, sample, srcDocument, sampleDataTag);

  }

  /**
   * Extract sample data from DOM on sample to add in DOM on project.
   * @param destDocument the destination document, project
   * @param destElement the dest element
   * @param srcDocument the source document, sample
   * @param tagNames the tag names
   */
  private void extractDataFromDOMtoDOM(final Document destDocument,
      final Element destElement, final Document srcDocument,
      final List<String> tagNames) {

    // Source root element
    final Element srcRoot = srcDocument.getDocumentElement();

    // Parsing tag names selected
    for (final String tagName : tagNames) {
      // Extract node from tag name
      final NodeList childs = srcRoot.getElementsByTagName(tagName);

      if (childs != null && childs.getLength() > 0) {
        // Create new element in destination document
        final Element elem = (Element) childs.item(0);

        Node newElem = destDocument.importNode(elem, true);
        destElement.appendChild(newElem);

      }
    }
  }

  /**
   * Compile genomes in project.
   * @param the document from xml file.
   * @return all genomes or if none found no genome
   */
  private String compileGenomesInProject(final Document doc) {
    // Save all genomes in project
    final List<String> genomes = new ArrayList<>();
    final String tagName = "genomeSample";

    String textTag = "No genome";

    final NodeList childs = doc.getElementsByTagName(tagName);

    if (childs != null && childs.getLength() > 0) {

      for (int i = 0; i < childs.getLength(); i++) {
        final String genome = childs.item(i).getTextContent();

        if (!genome.isEmpty() && !genomes.contains(genome)) {
          // Add new founded genome name
          genomes.add(genome);
        }
      }
    }

    // Compile genomes name
    if (!genomes.isEmpty()) {
      // Sort list
      Collections.sort(genomes);

      // Set new tag text content
      textTag = Joiner.on(", ").join(genomes);
    }

    return textTag;
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new fastq screen project report.
   * @param project the project
   * @param xslFile the xsl file
   */
  public FastqScreenProjectReport(final EntityStat project, final File xslFile) {

    this.entitiesStat = project;
    this.fastqscreenXSLFile = xslFile;

  }

}