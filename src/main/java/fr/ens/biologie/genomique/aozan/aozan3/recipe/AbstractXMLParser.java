package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedAttributes;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getAttribute;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToEmpty;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToUnknownSource;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.log.DummyAzoanLogger;

/**
 * This class define an abstract class to parse XML recipe files.
 * @author Laurent Jourdren
 * @since 3.0
 * @param <E> the output object of the parsing
 */
abstract class AbstractXMLParser<E> {

  private static final String INCLUDE_ATTR_NAME = "include";

  private final AozanLogger logger;
  private final String rootTagName;
  private final String fileType;

  //
  // Getters
  //

  /**
   * Get the logger.
   * @return the logger
   */
  protected AozanLogger getLogger() {
    return this.logger;
  }

  /**
   * Get the root tag name
   * @return the root tag name
   */
  protected String getRootTagName() {
    return this.rootTagName;
  }

  //
  // Parsing methods
  //

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @return a list of Step
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public E parse(Path path) throws Aozan3Exception {

    try {
      return parse(Files.newInputStream(path), path.toString());
    } catch (IOException e) {
      throw new Aozan3Exception(
          "Error while reading " + this.fileType + " file: " + e.getMessage(),
          e);
    }
  }

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @return a Recipe object
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public E parse(InputStream is) throws Aozan3Exception {

    return parse(is, null);
  }

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @param source XML source description
   * @return a Recipe object
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public E parse(InputStream is, String source) throws Aozan3Exception {

    Objects.requireNonNull(is);

    this.logger.info("Start parsing the "
        + this.fileType + " file: " + nullToUnknownSource(source));

    // Parse file into a Document object
    try {
      final DocumentBuilderFactory dbFactory =
          DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      // Create Recipe object
      return parse(doc, source);

    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new Aozan3Exception(
          "Error while parsing " + this.fileType + " file: " + e.getMessage(),
          e);
    }
  }

  /**
   * Parse XML document.
   * @param doc XML document
   * @param source XML source description
   * @return a new Recipe Object
   * @throws Aozan3Exception if an error occurs while parsing the XML document
   */
  private E parse(Document doc, String source) throws Aozan3Exception {

    return parse(doc.getElementsByTagName(this.rootTagName), source);
  }

  /**
   * Parse a run step XML tag.
   * @param rootElement element to parse
   * @return a list with step objects
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  E parse(final Element rootElement) throws Aozan3Exception {

    return parse(rootElement, null);
  }

  /**
   * Parse a run step XML tag.
   * @param rootElement element to parse
   * @return a list with step objects
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  E parse(final Element rootElement, String source) throws Aozan3Exception {

    requireNonNull(rootElement);

    return parse(rootElement.getElementsByTagName(this.rootTagName), source);
  }

  /**
   * Parse a run step XML tag.
   * @param nList node list
   * @param source XML source description
   * @return a list with step objects
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  protected abstract E parse(NodeList nList, String source)
      throws Aozan3Exception;

  //
  // Other methods
  //

  /**
   * Get the path of a file to include.
   * @param element element to parse
   * @return a Path object if the element to include exists or null
   * @throws Aozan3Exception if other attribute than include exists for the tag
   */
  protected Path getIncludePath(Element element, String source)
      throws Aozan3Exception {

    // Check allowed attributes for the "storages" tag
    checkAllowedAttributes(element, INCLUDE_ATTR_NAME);

    // Get the include path
    String includePath =
        nullToEmpty(getAttribute(element, INCLUDE_ATTR_NAME)).trim();

    if (includePath.isEmpty()) {
      return null;
    }

    // If the file is absolute return the Path
    Path result = Paths.get(includePath);
    if (result.isAbsolute() || source == null) {
      return result;
    }

    // If relative try to use the source to get the Path
    Path sourcePath = Paths.get(source);
    if (Files.isRegularFile(sourcePath)) {
      return Paths.get(sourcePath.getParent().toString(), includePath);
    }

    return Paths.get(includePath);
  }

  //
  // Constructor
  //

  protected AbstractXMLParser(String rootTagName, String fileType,
      AozanLogger logger) {

    requireNonNull(rootTagName);
    requireNonNull(fileType);

    this.rootTagName = rootTagName;
    this.fileType = fileType;
    this.logger = logger != null ? logger : new DummyAzoanLogger();
  }

}
