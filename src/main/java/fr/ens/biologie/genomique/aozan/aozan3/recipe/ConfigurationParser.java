package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedAttributes;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedChildTags;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.evaluateExpressions;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getAttribute;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getTagValue;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToEmpty;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToUnknownSource;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.log.DummyAzoanLogger;

/**
 * This class define a XML parser for configuration.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class ConfigurationParser {

  private final AozanLogger logger;
  private final String rootTagName;
  private final String sectionName;
  private final Configuration parentConf;

  static final String PARAMETER_TAG_NAME = "parameter";
  static final String PARAMETERNAME_TAG_NAME = "name";
  static final String PARAMETERVALUE_TAG_NAME = "value";
  private static final String INCLUDE_ATTR_NAME = "include";

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  Configuration parse(Path path) throws Aozan3Exception {

    try {
      return parse(Files.newInputStream(path), path.toString());
    } catch (IOException e) {
      throw new Aozan3Exception(
          "Error while reading recipe file: " + e.getMessage(), e);
    }
  }

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  Configuration parse(InputStream is) throws Aozan3Exception {
    return parse(is, null);
  }

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @param source XML source description
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  Configuration parse(InputStream is, String source) throws Aozan3Exception {

    Objects.requireNonNull(is);

    this.logger.info(
        "Start parsing the configuration file: " + nullToUnknownSource(source));

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
          "Error while parsing configuration file: " + e.getMessage(), e);
    }
  }

  /**
   * Parse XML document.
   * @param doc XML document
   * @throws Aozan3Exception if an error occurs while parsing the XML document
   */
  Configuration parse(Document doc) throws Aozan3Exception {

    return parse(doc, null);
  }

  /**
   * Parse XML document.
   * @param doc XML document
   * @param source XML source description
   * @throws Aozan3Exception if an error occurs while parsing the XML document
   */
  Configuration parse(Document doc, String source) throws Aozan3Exception {

    return parseConfigurationTag(doc.getElementsByTagName(this.rootTagName),
        source);
  }

  /**
   * Parse a run step XML tag.
   * @param rootElement element to parse
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  Configuration parseConfigurationTag(final Element rootElement)
      throws Aozan3Exception {

    return parseConfigurationTag(rootElement, null);
  }

  /**
   * Parse a run step XML tag.
   * @param rootElement element to parse
   * @param source XML source description
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  Configuration parseConfigurationTag(final Element rootElement,
      final String source) throws Aozan3Exception {

    requireNonNull(rootElement);

    return parseConfigurationTag(
        rootElement.getElementsByTagName(this.rootTagName), source);
  }

  /**
   * Parse a configuration tag.
   * @param tagName name of the tag to parse
   * @param rootElement root element of the tag
   * @return a new Configuration object with the content of the parent
   *         configuration and the parameters defined inside the XML tag
   * @throws Aozan3Exception if an error occurs while parsing the tag
   */
  Configuration parseConfigurationTag(NodeList nList, String source)
      throws Aozan3Exception {

    Configuration result = new Configuration(this.parentConf);

    final Set<String> parameterNames = new HashSet<>();

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed attributes for the "storages" tag
        checkAllowedAttributes(element, INCLUDE_ATTR_NAME);

        // Check allowed tags for the "parameter" tag
        checkAllowedChildTags(element, PARAMETER_TAG_NAME);

        // Get the include path
        String includePath =
            nullToEmpty(getAttribute(element, INCLUDE_ATTR_NAME)).trim();

        // TODO configuration can be defined in a another key/value file
        // Load steps in the include file
        if (!includePath.isEmpty()) {
          ConfigurationParser parser = new ConfigurationParser(this.rootTagName,
              this.sectionName, this.parentConf, this.logger);

          result.set(parser.parse(Paths.get(includePath)));
        }

        final NodeList nParameterList =
            element.getElementsByTagName(PARAMETER_TAG_NAME);

        for (int j = 0; j < nParameterList.getLength(); j++) {

          final Node nParameterNode = nParameterList.item(j);

          if (nParameterNode.getNodeType() == Node.ELEMENT_NODE) {

            Element eParameterElement = (Element) nParameterNode;

            checkAllowedChildTags(eParameterElement, PARAMETERNAME_TAG_NAME,
                PARAMETERVALUE_TAG_NAME);

            final String paramName =
                getTagValue(PARAMETERNAME_TAG_NAME, eParameterElement);
            final String paramValue =
                getTagValue(PARAMETERVALUE_TAG_NAME, eParameterElement);

            if (paramName == null) {
              throw new Aozan3Exception(
                  "<name> Tag not found in parameter section of "
                      + this.sectionName + " in recipe file.");
            }
            if (paramValue == null) {
              throw new Aozan3Exception(
                  "<value> Tag not found in parameter section of "
                      + this.sectionName + " in recipe file.");
            }

            if (parameterNames.contains(paramName)) {
              throw new Aozan3Exception("The parameter \""
                  + paramName + "\" has been already defined for "
                  + this.sectionName + " in workflow file.");
            }
            parameterNames.add(paramName);

            result.set(paramName, evaluateExpressions(paramValue, result));
          }
        }

      }
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param sectionName name of the section in the recipe file
   * @param parentConf parent configuration
   * @param logger logger to use
   */
  public ConfigurationParser(String rootTagName, String sectionName,
      Configuration parentConfiguration, AozanLogger logger) {

    requireNonNull(rootTagName);
    requireNonNull(sectionName);
    requireNonNull(parentConfiguration);

    this.rootTagName = rootTagName;
    this.sectionName = sectionName;
    this.parentConf = parentConfiguration;
    this.logger = logger != null ? logger : new DummyAzoanLogger();
  }

}
