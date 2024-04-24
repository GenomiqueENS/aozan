package fr.ens.biologie.genomique.aozan;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import org.w3c.dom.Document;

import com.google.common.io.Files;

import fr.ens.biologie.genomique.aozan.util.XMLUtilsWriter;

/**
 * This class export the QC Report in several formats.
 * @since 3.1
 * @author Laurent Jourdren
 */
public class QCReportExporter {

  private final RunData data;
  private final Document doc;

  //
  // Raw data
  //

  /**
   * Write the raw data of the QC.
   * @param outputFilename the raw data file
   * @throws AozanException if an error occurs while writing the file
   */
  public void writeRawData(String outputFilename) throws AozanException {

    if (outputFilename == null) {
      throw new AozanException("The filename for the qc report is null");
    }

    writeRawData(new File(outputFilename));
  }

  /**
   * Write the raw data of the QC.
   * @param outputFile the raw data file
   * @throws AozanException if an error occurs while writing the file
   */
  public void writeRawData(File outputFile) throws AozanException {

    this.data.removeIfExists("aozan.info.conf.samplesheet");

    try {
      final Writer writer =
          Files.newWriter(outputFile, Globals.DEFAULT_FILE_ENCODING);

      writer.write(this.data.toString());

      writer.close();
    } catch (final IOException e) {
      throw new AozanException(e);
    }
  }

  //
  // XML
  //

  /**
   * Create the QC report as an XML String.
   * @return a String with the report in XML
   * @throws AozanException if an error occurs while creating the report
   */
  public String toXML() throws AozanException, IOException {

    return XMLUtilsWriter.createXMLFileContent(this.doc);
  }

  /**
   * Write the XML in a file.
   * @param outputFilename the path of the XML file
   * @throws AozanException if an error occurs while creating the report
   */
  public void writeXMLReport(String outputFilename) throws AozanException {

    writeXMLReport(new File(outputFilename));
  }

  /**
   * Write the XML in a file.
   * @param outputFile the output file
   * @throws AozanException if an error occurs while creating the report
   */
  public void writeXMLReport(File outputFile) throws AozanException {

    try {
      final Writer writer =
          Files.newWriter(outputFile, Globals.DEFAULT_FILE_ENCODING);

      writer.write(toXML());

      writer.close();
    } catch (final IOException e) {
      throw new AozanException(e);
    }

  }

  //
  // HTML
  //

  /**
   * Export the QC report. The XML report is transformed using an XSL style
   * sheet.
   * @param XSLFile XSL file
   * @return the QC report as a String
   * @throws AozanException if an error occurs while creating the report
   */
  public String export(File XSLFile) throws AozanException {

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
  public String export(InputStream is) throws AozanException {

    if (is == null) {
      throw new NullPointerException(
          "The input stream for the XSL stylesheet is null.");
    }

    return XMLUtilsWriter.createHTMLFileFromXSL(this.doc, is);
  }

  /**
   * Write the report usually in HTML) in a file.
   * @param stylesheetFilename XSL stylesheet file
   * @param outputFilename the path of the report file
   * @throws AozanException if an error occurs while creating the report
   */
  public void writeReport(String stylesheetFilename,
      final String outputFilename) throws AozanException {

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

      writeReport(is, new File(outputFilename));
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
   * @param xslIs XSL stylesheet input stream
   * @param outputFile the report file
   * @throws AozanException if an error occurs while creating the report
   */
  public void writeReport(InputStream xslIs, File outputFile)
      throws AozanException {

    try {
      final Writer writer =
          Files.newWriter(outputFile, Globals.DEFAULT_FILE_ENCODING);

      writer.write(export(xslIs));

      writer.close();
    } catch (final IOException e) {
      throw new AozanException(e);
    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param qcReport QC report object
   * @throws AozanException if an error occurs while getting the XML Document
   *           form the QCReport object
   */
  public QCReportExporter(QCReport qcReport) throws AozanException {

    requireNonNull(qcReport);
    this.data = new RunData(qcReport.getData());
    this.doc = qcReport.toDocument();
  }
}
