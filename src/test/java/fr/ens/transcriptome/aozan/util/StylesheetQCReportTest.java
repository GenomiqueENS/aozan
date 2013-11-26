package fr.ens.transcriptome.aozan.util;

import java.io.StringWriter;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
// JAXP used for XSLT transformations
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

// JUnit classes
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import fr.ens.transcriptome.aozan.Globals;

public class StylesheetQCReportTest extends TestCase {

  private TransformerFactory transFact;

  /**
   * All JUnit tests have a constructor that takes the test name.
   */
  public StylesheetQCReportTest(String name) {
    super(name);
  }

  /**
   * Initialization before each test[...] method is called.
   */
  public void setUp() {
    this.transFact = TransformerFactory.newInstance();
  }

  /**
   * An individual unit test.
   */
  public void testTransformReportQC() throws Exception {
    assertTrue("Invalide syntax XSL "
        + Globals.EMBEDDED_QC_XSL + " for report QC ",
        isStylesheetValide(Globals.EMBEDDED_QC_XSL, "reportQCXML.xml"));
  }

  public void testTransformReportFQS() throws Exception {
    assertTrue(
        "Invalide syntax XSL "
            + Globals.EMBEDDED_FASTQSCREEN_XSL + " for report fastqscreen",
        isStylesheetValide(Globals.EMBEDDED_FASTQSCREEN_XSL,
            "reportFastqscreenXML.xml"));
  }

  private boolean isStylesheetValide(final String stylesheetFile,
      final String reportFile) {
    try {

      Templates templates =
          this.transFact.newTemplates(new StreamSource(
              fr.ens.transcriptome.aozan.QC.class
                  .getResourceAsStream(stylesheetFile)));

      Transformer trans = templates.newTransformer();

      // Create the String writer
      final StringWriter writer = new StringWriter();

      // Transform the document
      trans.transform(
          new StreamSource(this.getClass().getResourceAsStream(reportFile)),
          new StreamResult(writer));

      assertTrue("HTML page generate for " + reportFile + " fail ", writer
          .toString().length() > 0);

      return true;
    } catch (TransformerException ae) {
      return false;
    }

  }

  /**
   * @return a TestSuite, which is a composite of Test objects.
   */
  public static Test suite() {
    // uses reflection to locate each method named test[...]
    return new TestSuite(StylesheetQCReportTest.class);
  }

  /**
   * Allow the unit tests to be invoked from the command line in text-only mode.
   */
  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
