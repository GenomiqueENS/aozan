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

package fr.ens.biologie.genomique.aozan.util;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import org.xml.sax.SAXException;

import com.google.common.base.Strings;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.kenetre.util.XMLUtils;

/**
 * The class implements common action to create control quality report.
 * @author Sandrine Perrin
 * @since 1.3
 */
public final class XMLUtilsWriter {

  /**
   * Add common tag in document header xml to describe Aozan and if data exists
   * on run.
   * @param doc document xml
   * @param parent parent element on tag
   * @param data instance of run data
   * @throws AozanException if document or parent element doesn't exist
   */
  public static void buildXMLCommonTagHeader(final Document doc,
      final Element parent, final RunData data) throws AozanException {

    /** Default locale date format in the application. */
    final DateFormat dateFormatter =
        new SimpleDateFormat("EEE dd MMM yyyy", Globals.DEFAULT_LOCALE);

    if (doc == null) {
      throw new AozanException(
          "Fail add common tag header in document XML, document doesn't exist.");
    }

    if (parent == null) {
      throw new AozanException(
          "Fail add common tag header in document XML, parent element of new tag doesn't exist.");
    }

    XMLUtils.addTagValue(doc, parent, "GeneratorName", Globals.APP_NAME);
    XMLUtils.addTagValue(doc, parent, "GeneratorVersion",
        Globals.APP_VERSION_STRING);
    XMLUtils.addTagValue(doc, parent, "GeneratorWebsite", Globals.WEBSITE_URL);
    XMLUtils.addTagValue(doc, parent, "GeneratorRevision",
        Globals.APP_BUILD_COMMIT);

    if (data != null) {
      XMLUtils.addTagValue(doc, parent, "RunId", data.get("run.info.run.id"));

      // Convert string to date
      try {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        final Date runDate = sdf.parse(data.get("run.info.date"));
        XMLUtils.addTagValue(doc, parent, "RunDate",
            dateFormatter.format(runDate));
      } catch (final ParseException e1) {
        XMLUtils.addTagValue(doc, parent, "RunDate", data.get("run.info.date"));
      }

      XMLUtils.addTagValue(doc, parent, "SequencerName",
          data.getSequencerName());
      XMLUtils.addTagValue(doc, parent, "SequencerFamily",
          data.getSequencerFamily());
      XMLUtils.addTagValue(doc, parent, "InstrumentSN",
          data.get("run.info.instrument"));

      XMLUtils.addTagValue(doc, parent, "FlowcellId", data.getFlowcellId());
      XMLUtils.addTagValue(doc, parent, "InstrumentRunNumber",
          data.get("run.info.run.number"));

      XMLUtils.addTagValue(doc, parent, "SequencerApplicationName",
          data.getSequencerApplicationName());
      XMLUtils.addTagValue(doc, parent, "SequencerApplicationVersion",
          data.getSequencerApplicationVersion());
      XMLUtils.addTagValue(doc, parent, "SequencerRTAVersion",
          data.getSequencerRTAVersion());

      XMLUtils.addTagValue(doc, parent, "Bcl2FastqVersion",
          Strings.nullToEmpty(data.getBcl2FastqVersion()).isEmpty()
              ? "Unknown version" : data.getBcl2FastqVersion());

      XMLUtils.addTagValue(doc, parent, "ReportDate",
          dateFormatter.format(new Date()));
    }
  }

  /**
   * Create a xml file from document xml.
   * @param doc document xml
   * @param output the output
   * @throws IOException if an error occurs while writing the file
   * @throws AozanException if document or output file doesn't exist or if an
   *           error occurs during transforming document.
   */
  public static void createXMLFile(final Document doc, final File output)
      throws IOException, AozanException {

    if (doc == null) {
      throw new AozanException(
          "Error create XML file, document doesn't exist.");
    }

    if (output == null) {
      throw new AozanException(
          "Error create XML file, output file is not define.");
    }

    // Transform document XML
    final String text = createXMLFileContent(doc);

    // Create XML file
    if (output.getAbsolutePath().endsWith(".html")) {
      Files.write(
          new File(output.getAbsolutePath().replace(".html", ".xml")).toPath(),
          text.getBytes());
      // (text, , Charset.defaultCharset());
    } else {
      Files.write(output.toPath(), text.getBytes());
    }
  }

  /**
   * Create a XML file content from XML document.
   * @param doc the XML document
   * @return a String with the XML document
   * @throws IOException if an error occurs while writing the file
   * @throws AozanException if document or output file doesn't exist or if an
   *           error occurs during transforming document.
   */
  public static String createXMLFileContent(final Document doc)
      throws IOException, AozanException {

    if (doc == null) {
      throw new AozanException(
          "Error create XML file, document doesn't exist.");
    }

    try {

      // Set up a transformer
      final TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer trans = transfac.newTransformer();
      trans.setOutputProperty(OutputKeys.INDENT, "yes");
      trans.setOutputProperty(OutputKeys.METHOD, "xml");
      trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      // Create the String writer
      final StringWriter swxml = new StringWriter();

      // Write the DOM to the String writer
      trans.transform(new DOMSource(doc), new StreamResult(swxml));

      return swxml.toString();

    } catch (final TransformerException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Transform a XML file using XSL style sheet.
   * @param XMLPath XML file
   * @param XSLPath XSL file
   * @return the QC report as a String
   * @throws AozanException if an error occurs while creating the report
   */
  public static String createHTMLFileFromXSL(final String XMLPath,
      final String XSLPath) throws AozanException {

    requireNonNull(XMLPath);
    requireNonNull(XSLPath);

    return createHTMLFileFromXSL(new File(XMLPath), new File(XSLPath));
  }

  /**
   * Transform a XML file using XSL style sheet.
   * @param XMLFile XML file
   * @param XSLFile XSL file
   * @return the QC report as a String
   * @throws AozanException if an error occurs while creating the report
   */
  public static String createHTMLFileFromXSL(final File XMLFile,
      final File XSLFile) throws AozanException {

    requireNonNull(XMLFile);
    requireNonNull(XSLFile);

    try {
      DocumentBuilder db =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = db.parse(XMLFile);

      try (InputStream is = new FileInputStream(XSLFile)) {
        return createHTMLFileFromXSL(doc, is);
      }

    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Create a html file from document xml and xsl files.
   * @param doc document xml
   * @param isXslFile input stream on the xsl file
   * @param reportHtml output html file
   * @throws IOException if an error occurs while writing the file
   * @throws AozanException if document or output file or xsl file doesn't exist
   *           or if an error occurs during transforming document.
   */
  public static void createHTMLFileFromXSL(final Document doc,
      final InputStream isXslFile, final File reportHtml)
      throws IOException, AozanException {

    if (doc == null) {
      throw new AozanException(
          "Error create HTML file, document doesn't exist.");
    }

    if (reportHtml == null) {
      throw new AozanException(
          "Error create HTML file, output HTML file is not define.");
    }

    if (isXslFile == null) {
      throw new AozanException(
          "Error create HTML file, XSL file is not define.");
    }

    // Transform document xml in text html
    final String text = createHTMLFileFromXSL(doc, isXslFile);

    // Create html file
    Files.write(reportHtml.toPath(), text.getBytes());
  }

  /**
   * Create a html file from document xml and xsl files.
   * @param doc document xml
   * @param isXslFile input stream on the xsl file
   * @return document xml in html format
   * @throws AozanException if document or output file or xsl file doesn't exist
   *           or if an error occurs during transforming document.
   */
  public static String createHTMLFileFromXSL(final Document doc,
      final InputStream isXslFile) throws AozanException {

    if (doc == null) {
      throw new AozanException(
          "Error create HTML file, document doesn't exist.");
    }

    if (isXslFile == null) {
      throw new AozanException(
          "Error create HTML file, XSL file is not define.");
    }

    try {
      // Set up a transformer
      final TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer trans = transfac.newTransformer();
      // trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      trans.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
      trans.setOutputProperty(OutputKeys.INDENT, "yes");
      trans.setOutputProperty(OutputKeys.METHOD, "html");
      trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      // Create string from XML tree
      final StringWriter sw = new StringWriter();
      final StreamResult result = new StreamResult(sw);

      final DOMSource source = new DOMSource(doc);
      trans.transform(source, result);

      // Create the transformer
      final Transformer transformer =
          TransformerFactory.newInstance().newTransformer(
              new javax.xml.transform.stream.StreamSource(isXslFile));

      // Create the String writer
      final StringWriter writer = new StringWriter();

      // Transform the document
      transformer.transform(new DOMSource(doc), new StreamResult(writer));

      // Close input stream
      isXslFile.close();

      return writer.toString();

    } catch (final TransformerException | IOException e) {
      throw new AozanException(e);
    }
  }
}
