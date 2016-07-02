package fr.ens.biologie.genomique.aozan.illumina;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.util.XMLUtils.getTagValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.XMLUtils;

/**
 * This class allow to collect data from the runParameters.xml file.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class RunParameters {

  private final String applicationName;
  private final String applicationVersion;
  private final String rtaVersion;
  private final int rtaMajorVersion;
  private final String sequencerFamily;

  //
  // Getters
  //

  /**
   * Get application name.
   * @return the application name
   */
  public String getApplicationName() {
    return this.applicationName;
  }

  /**
   * Get the application version.
   * @return the application version
   */
  public String getApplicationVersion() {
    return this.applicationVersion;
  }

  /**
   * Get the RTA version.
   * @return RTA version
   */
  public String getRTAVersion() {
    return this.rtaVersion;
  }

  /**
   * Get the RTA major version.
   * @return the RTA major version
   */
  public int getRTAMajorVersion() {
    return this.rtaMajorVersion;
  }

  /**
   * Get the sequencer family.
   * @return the sequencer family
   */
  public String getSequencerFamily() {
    return this.sequencerFamily;
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return "RunParameters{applicationName="
        + this.applicationName + ", applicationVersion=" + applicationVersion
        + ", rtaVersion=" + rtaVersion + ", sequencerFamily=" + sequencerFamily
        + "}";
  }

  //
  // Parsers
  //

  /**
   * Parses the run parameter file.
   * @param filepath the path to the run info file
   * @return a RunParameters object with the information of the parsed file
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static RunParameters parse(final String filepath)
      throws ParserConfigurationException, SAXException, IOException {

    checkNotNull(filepath, "RunInfo.xml path cannot be null");

    return parse(new File(filepath));
  }

  /**
   * Parses the run parameter file.
   * @param file the run info file
   * @return a RunParameters object with the information of the parsed file
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static RunParameters parse(final File file)
      throws ParserConfigurationException, SAXException, IOException {

    checkNotNull(file, "file cannot be null");

    checkArgument(file.isFile(),
        "RunParameters.xml does not exists or is not a file");

    return parse(FileUtils.createInputStream(file));
  }

  /**
   * Parses the run parameter file.
   * @param is the input stream on run info file
   * @return a RunParameters object with the information of the parsed file
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static RunParameters parse(final InputStream is)
      throws ParserConfigurationException, SAXException, IOException {

    checkNotNull(is, "RunParameters.xml input stream cannot be null");

    try (InputStream in = is) {

      final Document doc;

      final DocumentBuilderFactory dbFactory =
          DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(in);
      doc.getDocumentElement().normalize();

      return parse(doc);
    }
  }

  /**
   * Parses the run info file.
   * @param document the document from run info XML file
   */
  private static RunParameters parse(final Document document) {

    for (Element e : XMLUtils.getElementsByTagName(document, "RunParameters")) {

      String rtaVersion = getTagValue(e, "RTAVersion");

      final List<Element> elements = XMLUtils.getElementsByTagName(e, "Setup");
      if (!elements.isEmpty()) {

        final Element setupElement = elements.get(0);

        final String applicationName =
            getTagValue(setupElement, "ApplicationName");
        final String applicationVersion =
            getTagValue(setupElement, "ApplicationVersion");

        if (rtaVersion == null) {
          rtaVersion = getTagValue(setupElement, "RTAVersion");
        }
        return new RunParameters(applicationName, applicationVersion,
            rtaVersion);
      }
    }

    throw new AozanRuntimeException("No \"Run\" tag found in RunInfo.xml");
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private RunParameters(final String applicationName,
      final String applicationVersion, final String rtaVersion) {

    this.applicationName = applicationName;
    this.applicationVersion = applicationVersion;
    this.rtaVersion = rtaVersion;

    if (applicationName != null) {
      this.sequencerFamily =
          applicationName.substring(0, applicationName.indexOf(' '));
    } else {
      this.sequencerFamily = "Unknown";
    }

    int rtaMajorVersion = -1;

    if (this.rtaVersion != null) {

      try {
        rtaMajorVersion =
            Integer.parseInt(rtaVersion.substring(0, rtaVersion.indexOf('.')));
      } catch (NumberFormatException e) {
        // Do nothing
      }
    }

    this.rtaMajorVersion = rtaMajorVersion;
  }

}
