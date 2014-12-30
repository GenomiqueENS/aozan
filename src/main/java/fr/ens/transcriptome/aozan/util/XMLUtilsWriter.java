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

package fr.ens.transcriptome.aozan.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

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

      XMLUtils.addTagValue(doc, parent, "FlowcellId",
          data.get("run.info.flow.cell.id"));
      XMLUtils.addTagValue(doc, parent, "InstrumentSN",
          data.get("run.info.instrument"));
      XMLUtils.addTagValue(doc, parent, "InstrumentRunNumber",
          data.get("run.info.run.number"));
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
      throw new AozanException("Error create XML file, document doesn't exist.");
    }

    if (output == null) {
      throw new AozanException(
          "Error create XML file, output file is not define.");
    }

    // Transform document XML
    final String text = createXMLFile(doc);

    // Create XML file
    if (output.getAbsolutePath().endsWith(".html")) {
      Files.write(text,
          new File(output.getAbsolutePath().replace(".html", ".xml")),
          StandardCharsets.UTF_8);
    } else {
      Files.write(text, output, StandardCharsets.UTF_8);
    }
  }

  /**
   * Create a xml file from document xml.
   * @param doc document xml
   * @return document xml in text format
   * @throws IOException if an error occurs while writing the file
   * @throws AozanException if document or output file doesn't exist or if an
   *           error occurs during transforming document.
   */
  public static String createXMLFile(final Document doc) throws IOException,
      AozanException {

    if (doc == null) {
      throw new AozanException("Error create XML file, document doesn't exist.");
    }

    try {
      // Print document XML
      final TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer trans;
      trans = transfac.newTransformer();
      final StringWriter swxml = new StringWriter();
      final StreamResult resultxml = new StreamResult(swxml);
      final DOMSource sourcexml = new DOMSource(doc);
      trans.transform(sourcexml, resultxml);

      return swxml.toString();

    } catch (final TransformerException e) {
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
      final InputStream isXslFile, final File reportHtml) throws IOException,
      AozanException {

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
    Files.write(text, reportHtml, StandardCharsets.UTF_8);
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
      Transformer trans;
      trans = transfac.newTransformer();
      // trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      trans.setOutputProperty(OutputKeys.INDENT, "yes");
      trans.setOutputProperty(OutputKeys.METHOD, "html");

      // Create string from xml tree
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

    } catch (final TransformerException e) {
      throw new AozanException(e);
    } catch (final IOException e) {
      throw new AozanException(e);
    }
  }
}
