package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedAttributes;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedChildTags;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getAttribute;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getTagValue;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToEmpty;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToUnknownSource;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeParser.RECIPE_STEPS_TAG_NAME;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeParser.RECIPE_STORAGES_TAG_NAME;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import fr.ens.biologie.genomique.aozan.aozan3.DefaultRunIdGenerator;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.RunConfigurationProvider;

/**
 * This class define a XML parser for steps.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class StepsParser {

  private static final String STEPS_INCLUDE_ATTR_NAME = "include";
  private static final String STEP_TAG_NAME = "step";
  private static final String STEP_NAME_TAG_NAME = "name";
  private static final String STEP_PROVIDER_TAG_NAME = "provider";
  private static final String STEP_SINKSTORAGE_TAG_NAME = "sinkstorage";
  private static final String STEP_CONF_TAG_NAME = "configuration";
  private static final String STEP_OUTPUTIDSCHEME_TAG_NAME = "outputidscheme";

  private final Recipe recipe;
  private final RunConfigurationProvider runConfProvider;

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @return a list of Step
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public List<Step> parse(Path path) throws Aozan3Exception {

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
   * @return a Recipe object
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public List<Step> parse(InputStream is) throws Aozan3Exception {

    return parse(is, null);
  }

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @param source XML source description
   * @return a Recipe object
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public List<Step> parse(InputStream is, String source)
      throws Aozan3Exception {

    Objects.requireNonNull(is);

    this.recipe.getLogger()
        .info("Start parsing the steps file: " + nullToUnknownSource(source));

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
          "Error while parsing recipe file: " + e.getMessage(), e);
    }
  }

  /**
   * Parse XML document.
   * @param doc XML document
   * @param source XML source description
   * @return a new Recipe Object
   * @throws Aozan3Exception if an error occurs while parsing the XML document
   */
  private List<Step> parse(Document doc, String source) throws Aozan3Exception {

    return parseStepsTag(doc.getElementsByTagName(RECIPE_STEPS_TAG_NAME),
        source);
  }

  /**
   * Parse a run step XML tag.
   * @param rootElement element to parse
   * @return a list with step objects
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  List<Step> parseStepsTag(final Element rootElement) throws Aozan3Exception {

    return parseStepsTag(rootElement, null);
  }

  /**
   * Parse a run step XML tag.
   * @param rootElement element to parse
   * @return a list with step objects
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  List<Step> parseStepsTag(final Element rootElement, String source)
      throws Aozan3Exception {

    requireNonNull(rootElement);

    return parseStepsTag(
        rootElement.getElementsByTagName(RECIPE_STEPS_TAG_NAME), source);
  }

  /**
   * Parse a run step XML tag.
   * @param nList node list
   * @param source XML source description
   * @return a list with step objects
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private List<Step> parseStepsTag(NodeList nList, String source)
      throws Aozan3Exception {

    if (nList.getLength() == 0) {
      throw new Aozan3Exception("No <"
          + RECIPE_STORAGES_TAG_NAME + "> tag found in "
          + nullToUnknownSource(source));
    }

    List<Step> result = new ArrayList<>();

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed attributes for the "storages" tag
        checkAllowedAttributes(element, STEPS_INCLUDE_ATTR_NAME);

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, STEP_TAG_NAME);

        // Get the include path
        String includePath =
            nullToEmpty(getAttribute(element, STEPS_INCLUDE_ATTR_NAME)).trim();

        // Load steps in the include file
        if (!includePath.isEmpty()) {
          StepsParser parser =
              new StepsParser(this.recipe, this.runConfProvider);
          result.addAll(parser.parse(Paths.get(includePath)));
        }

        result.addAll(parseStepTag(element));
      }
    }

    return result;
  }

  /**
   * Parse a run step XML tag.
   * @param recipe the recipe
   * @param runConfProvider run configuration provider
   * @param rootElement element to parse
   * @return a list with step objects
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  List<Step> parseStepTag(final Element rootElement) throws Aozan3Exception {

    List<Step> result = new ArrayList<>();

    final NodeList nStorageList =
        rootElement.getElementsByTagName(STEP_TAG_NAME);

    for (int j = 0; j < nStorageList.getLength(); j++) {

      final Node nStepNode = nStorageList.item(j);

      if (nStepNode.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) nStepNode;

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, STEP_NAME_TAG_NAME,
            STEP_PROVIDER_TAG_NAME, STEP_SINKSTORAGE_TAG_NAME,
            STEP_CONF_TAG_NAME, STEP_OUTPUTIDSCHEME_TAG_NAME);

        String stepName = nullToEmpty(getTagValue(STEP_NAME_TAG_NAME, element));
        String providerName =
            nullToEmpty(getTagValue(STEP_PROVIDER_TAG_NAME, element));
        String outputStorage =
            nullToEmpty(getTagValue(STEP_SINKSTORAGE_TAG_NAME, element));
        Configuration conf =
            ConfigurationParser.parseConfigurationTag(STEP_CONF_TAG_NAME,
                element, STEP_TAG_NAME, recipe.getConfiguration());
        String runIdScheme =
            nullToEmpty(getTagValue(STEP_OUTPUTIDSCHEME_TAG_NAME, element));

        // Create new step
        Step step = new Step(this.recipe, stepName, providerName, outputStorage,
            conf, runConfProvider, runIdScheme.isEmpty()
                ? null : new DefaultRunIdGenerator(runIdScheme));

        result.add(step);
      }
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param recipe the recipe of the step
   */
  StepsParser(Recipe recipe, RunConfigurationProvider runConfProvider) {

    requireNonNull(recipe);
    requireNonNull(runConfProvider);

    this.recipe = recipe;
    this.runConfProvider = runConfProvider;
  }

}
