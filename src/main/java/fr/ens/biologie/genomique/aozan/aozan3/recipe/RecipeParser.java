package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.DefaultRunIdGenerator;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.log.DummyAzoanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.RunConfigurationProvider;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.RunConfigurationProviderService;

/**
 * This class define a recipe XML parser.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RecipeParser {

  private final Configuration conf;
  private AozanLogger logger = new DummyAzoanLogger();

  /** Version of the format of the workflow file. */
  private static final String FORMAT_VERSION = "0.1";

  private static final String ROOT_TAG_NAME = "recipe";
  private static final String RECIPE_FORMAT_VERSION_TAG_NAME = "formatversion";
  private static final String RECIPE_NAME_TAG_NAME = "name";
  private static final String RECIPE_CONF_TAG_NAME = "configuration";
  private static final String RECIPE_STORAGES_TAG_NAME = "storages";
  private static final String RECIPE_RUN_CONFIGURATION_TAG_NAME =
      "runconfiguration";
  private static final String RECIPE_DATASOURCE_TAG_NAME = "datasource";
  private static final String RECIPE_STEPS_TAG_NAME = "steps";

  private static final String PARAMETER_TAG_NAME = "parameter";
  private static final String PARAMETERNAME_TAG_NAME = "name";
  private static final String PARAMETERVALUE_TAG_NAME = "value";

  private static final String STORAGE_TAG_NAME = "storage";
  private static final String STORAGE_INPROGRESS_ATTR_NAME = "inprogress";
  private static final String STORAGE_NAME_TAG_NAME = "name";
  private static final String STORAGE_PATH_TAG_NAME = "path";
  private static final String STORAGE_SOURCE_TAG_NAME = "source";

  private static final String RUNCONF_PROVIDER_TAG_NAME = "provider";
  private static final String RUNCONF_CONF_TAG_NAME = "configuration";

  private static final String DATASOURCE_PROVIDER_TAG_NAME = "provider";
  private static final String DATASOURCE_STORAGE_TAG_NAME = "storage";
  private static final String DATASOURCE_CONF_TAG_NAME = "configuration";

  private static final String STEP_TAG_NAME = "step";
  private static final String STEP_NAME_TAG_NAME = "name";
  private static final String STEP_PROVIDER_TAG_NAME = "provider";
  private static final String STEP_SINKSTORAGE_TAG_NAME = "sinkstorage";
  private static final String STEP_CONF_TAG_NAME = "configuration";
  private static final String STEP_OUTPUTIDSCHEME_TAG_NAME = "outputidscheme";

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @return a Recipe object
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public Recipe parse(Path path) throws Aozan3Exception {

    try {
      return parse(Files.newInputStream(path));
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
  public Recipe parse(InputStream is) throws Aozan3Exception {

    Objects.requireNonNull(is);

    this.logger.info("Start parsing the workflow workflow file");

    // Parse file into a Document object
    try {
      final DocumentBuilderFactory dbFactory =
          DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      // Create Recipe object
      return parse(doc);

    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new Aozan3Exception(
          "Error while parsing recipe file: " + e.getMessage(), e);
    }
  }

  /**
   * Parse XML document.
   * @param doc XML document
   * @return a new Recipe Object
   * @throws Aozan3Exception if an error occurs while parsing the XML document
   */
  public Recipe parse(Document doc) throws Aozan3Exception {

    final NodeList nAnalysisList = doc.getElementsByTagName(ROOT_TAG_NAME);

    Recipe result = null;

    for (int i = 0; i < nAnalysisList.getLength(); i++) {

      if (result != null) {
        throw new Aozan3Exception("Found several recipe in the recipe file");
      }

      Node nNode = nAnalysisList.item(i);
      if (nNode.getNodeType() == Node.ELEMENT_NODE) {
        result = parseRecipeTag((Element) nNode);
      }

    }

    return result;
  }

  /**
   * Parse a recipe tag.
   * @param element element to parse
   * @return a new recipe object
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private Recipe parseRecipeTag(Element element) throws Aozan3Exception {

    // Check allowed child tags of the root tag
    checkAllowedChildTags(element, RECIPE_FORMAT_VERSION_TAG_NAME,
        RECIPE_NAME_TAG_NAME, RECIPE_CONF_TAG_NAME, RECIPE_STORAGES_TAG_NAME,
        RECIPE_RUN_CONFIGURATION_TAG_NAME, RECIPE_DATASOURCE_TAG_NAME,
        RECIPE_STEPS_TAG_NAME);

    // Check version of the XML file
    final String formatVersion =
        nullToEmpty(getTagValue(RECIPE_FORMAT_VERSION_TAG_NAME, element));
    if (!FORMAT_VERSION.equals(formatVersion)) {
      throw new Aozan3Exception(
          "Invalid version of the format of the workflow file.");
    }

    // Get the recipe name
    String recipeName = nullToEmpty(getTagValue(RECIPE_NAME_TAG_NAME, element));

    // Get the recipe configuration
    Configuration recipeConf = parseConfigurationTag(RECIPE_CONF_TAG_NAME,
        element, "recipe configuration", this.conf);

    // Create a new recipe
    Recipe recipe = new Recipe(recipeName, recipeConf);

    // Parse storages
    parseStoragesTag(recipe, element);

    // Parse run configuration provider
    RunConfigurationProvider runConfProvider =
        parseRunConfigurationTag(recipe, element);

    // Parse the datasource tag
    parseDataSourceTag(recipe, element);

    // Parse steps
    recipe.addSteps(parseStepsTag(recipe, runConfProvider, element));

    return recipe;
  }

  /**
   * Parse a configuration tag.
   * @param tagName name of the tag to parse
   * @param rootElement root element of the tag
   * @param sectionName name of the section in the recipe file
   * @param parentConf parent configuration
   * @return a new Configuration object with the content of the parent
   *         configuration and the parameters defined inside the XML tag
   * @throws Aozan3Exception if an error occurs while parsing the tag
   */
  Configuration parseConfigurationTag(final String tagName,
      final Element rootElement, String sectionName, Configuration parentConf)
      throws Aozan3Exception {

    requireNonNull(tagName);
    requireNonNull(rootElement);
    requireNonNull(sectionName);

    Configuration result = new Configuration(parentConf);

    final Set<String> parameterNames = new HashSet<>();

    final NodeList nList = rootElement.getElementsByTagName(tagName);

    // TODO configuration can be define in a another key/value or XML file

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed tags for the "parameter" tag
        checkAllowedChildTags(element, PARAMETER_TAG_NAME);

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
                      + sectionName + " in recipe file.");
            }
            if (paramValue == null) {
              throw new Aozan3Exception(
                  "<value> Tag not found in parameter section of "
                      + sectionName + " in recipe file.");
            }

            if (parameterNames.contains(paramName)) {
              throw new Aozan3Exception("The parameter \""
                  + paramName + "\" has been already defined for " + sectionName
                  + " in workflow file.");
            }
            parameterNames.add(paramName);

            result.set(paramName, evaluateExpressions(paramValue, result));
          }
        }

      }
    }

    return result;
  }

  /**
   * Parse a storage tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private void parseStoragesTag(final Recipe recipe, final Element rootElement)
      throws Aozan3Exception {

    requireNonNull(recipe);
    requireNonNull(rootElement);

    final NodeList nList =
        rootElement.getElementsByTagName(RECIPE_STORAGES_TAG_NAME);

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // TODO storages can be define in another XML file

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, STORAGE_TAG_NAME);

        parseStorageTag(recipe, element);
      }
    }
  }

  /**
   * Parse a storage tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private void parseStorageTag(final Recipe recipe, final Element rootElement)
      throws Aozan3Exception {

    final NodeList nStorageList =
        rootElement.getElementsByTagName(STORAGE_TAG_NAME);

    for (int j = 0; j < nStorageList.getLength(); j++) {

      final Node nStorageNode = nStorageList.item(j);

      if (nStorageNode.getNodeType() == Node.ELEMENT_NODE) {

        Element eStorageElement = (Element) nStorageNode;

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(eStorageElement, STORAGE_NAME_TAG_NAME,
            STORAGE_PATH_TAG_NAME, STORAGE_SOURCE_TAG_NAME);

        String storageName =
            nullToEmpty(getTagValue(STORAGE_NAME_TAG_NAME, eStorageElement));
        String storagePath =
            nullToEmpty(getTagValue(STORAGE_PATH_TAG_NAME, eStorageElement));
        String storageSource =
            nullToEmpty(getTagValue(STORAGE_SOURCE_TAG_NAME, eStorageElement));

        // Add the storage to the recipe
        // TODO Handle sequencer sources
        DataStorage storage = new DataStorage("local", storagePath, null);
        recipe.addStorage(storageName, storage);
      }

    }
  }

  /**
   * Parse a run configuration XML tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @return a new RunConfigurationProvider object
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private RunConfigurationProvider parseRunConfigurationTag(final Recipe recipe,
      final Element rootElement) throws Aozan3Exception {

    requireNonNull(recipe);
    requireNonNull(rootElement);

    RunConfigurationProvider result = null;

    final NodeList nList =
        rootElement.getElementsByTagName(RECIPE_RUN_CONFIGURATION_TAG_NAME);

    for (int i = 0; i < nList.getLength(); i++) {

      if (result != null) {
        throw new Aozan3Exception("Found more than one \""
            + RECIPE_RUN_CONFIGURATION_TAG_NAME + "\" tag");
      }

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, RUNCONF_PROVIDER_TAG_NAME,
            RUNCONF_CONF_TAG_NAME);

        String providerName =
            nullToEmpty(getTagValue(RUNCONF_PROVIDER_TAG_NAME, element));
        Configuration conf =
            parseConfigurationTag(RUNCONF_CONF_TAG_NAME, element,
                RECIPE_RUN_CONFIGURATION_TAG_NAME, recipe.getConfiguration());

        result = RunConfigurationProviderService.getInstance()
            .newService(providerName);
        result.init(conf, recipe.getLogger());
      }
    }

    return result;
  }

  /**
   * Parse a run data source XML tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private void parseDataSourceTag(final Recipe recipe,
      final Element rootElement) throws Aozan3Exception {

    requireNonNull(recipe);
    requireNonNull(rootElement);

    final NodeList nList =
        rootElement.getElementsByTagName(RECIPE_DATASOURCE_TAG_NAME);

    for (int i = 0; i < nList.getLength(); i++) {

      if (i > 0) {
        throw new Aozan3Exception(
            "Found more than one \"" + RECIPE_DATASOURCE_TAG_NAME + "\" tag");
      }

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed attributes for the "storages" tag
        checkAllowedAttributes(element, STORAGE_INPROGRESS_ATTR_NAME);

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, DATASOURCE_PROVIDER_TAG_NAME,
            DATASOURCE_STORAGE_TAG_NAME, DATASOURCE_CONF_TAG_NAME);

        final boolean inProgress = Boolean.parseBoolean(
            nullToEmpty(getAttribute(element, STORAGE_INPROGRESS_ATTR_NAME)
                .trim().toLowerCase()));

        String providerName =
            nullToEmpty(getTagValue(DATASOURCE_PROVIDER_TAG_NAME, element));
        String storageName =
            nullToEmpty(getTagValue(DATASOURCE_STORAGE_TAG_NAME, element));
        Configuration conf = parseConfigurationTag(DATASOURCE_CONF_TAG_NAME,
            element, RECIPE_DATASOURCE_TAG_NAME, recipe.getConfiguration());

        recipe.setDataProvider(providerName, storageName, conf);
        recipe.setUseInProgressData(inProgress);
      }
    }
  }

  /**
   * Parse a run step XML tag.
   * @param recipe the recipe
   * @param runConfProvider run configuration provider
   * @param rootElement element to parse
   * @return a list with step objects
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private List<Step> parseStepsTag(final Recipe recipe,
      RunConfigurationProvider runConfProvider, final Element rootElement)
      throws Aozan3Exception {

    requireNonNull(recipe);
    requireNonNull(rootElement);

    List<Step> result = new ArrayList<>();

    final NodeList nList =
        rootElement.getElementsByTagName(RECIPE_STEPS_TAG_NAME);

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // TODO steps can be define in another XML files

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, STEP_TAG_NAME);

        result.addAll(parseStepTag(recipe, runConfProvider, element));
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
  private List<Step> parseStepTag(final Recipe recipe,
      RunConfigurationProvider runConfProvider, final Element rootElement)
      throws Aozan3Exception {

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
        Configuration conf = parseConfigurationTag(STEP_CONF_TAG_NAME, element,
            RECIPE_DATASOURCE_TAG_NAME, recipe.getConfiguration());
        String runIdScheme =
            nullToEmpty(getTagValue(STEP_OUTPUTIDSCHEME_TAG_NAME, element));

        // Create new step
        Step step = new Step(recipe, stepName, providerName, outputStorage,
            conf, runConfProvider, runIdScheme.isEmpty()
                ? null : new DefaultRunIdGenerator(runIdScheme));

        result.add(step);
      }
    }

    return result;
  }

  //
  // Utility parsing methods
  //

  /**
   * Check if the child tags of a tag is in a list of allowed tag names.
   * @param element the tag element
   * @param tagNames the allowed tag names
   * @throws Aozan3Exception if a child tag of the tag in not in the allowed tag
   *           list
   */
  private static void checkAllowedChildTags(final Element element,
      String... tagNames) throws Aozan3Exception {

    final List<String> tagList = Arrays.asList(tagNames);

    final NodeList nl = element.getChildNodes();

    for (int i = 0; i < nl.getLength(); i++) {

      final Node n = nl.item(i);

      if (n.getNodeType() == Node.ELEMENT_NODE) {

        final String childTagName = n.getNodeName();

        if (!tagList.contains(childTagName)) {
          throw new Aozan3Exception("the \""
              + element.getNodeName() + "\" tag contains an unknown tag: "
              + childTagName + ".");
        }
      }
    }
  }

  /**
   * Check if the attribute of a tag is in a list of allowed attribute names.
   * @param element the tag element
   * @param attributeNames the allowed attribute names
   * @throws Aozan3Exception if an attribute of the tag in not in the allowed
   *           attribute list
   */
  private static void checkAllowedAttributes(final Element element,
      String... attributeNames) throws Aozan3Exception {

    final List<String> attributeList = Arrays.asList(attributeNames);

    final NamedNodeMap nnm = element.getAttributes();

    for (int i = 0; i < nnm.getLength(); i++) {

      final Node n = nnm.item(i);

      if (n.getNodeType() == Node.ATTRIBUTE_NODE) {

        final String attributeName = n.getNodeName();

        if (!attributeList.contains(attributeName)) {
          throw new Aozan3Exception("the \""
              + element.getNodeName() + "\" tag contains an unknown attribute: "
              + attributeName + ".");
        }
      }
    }
  }

  /**
   * Get the value of a tag
   * @param tag name of the tag
   * @param element root element
   * @return the value of the tag
   */
  private static String getTagValue(final String tag, final Element element) {

    final NodeList nl = element.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {

      final Node n = nl.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && tag.equals(n.getNodeName())) {
        return n.getTextContent();
      }
    }

    return null;
  }

  /**
   * Get the value of an XML tag attribute and evaluate it if necessary.
   * @param element the XML tag element
   * @param name the name of the attribute
   * @return the value of the attribute
   */
  private String getAttribute(Element element, final String name) {

    return element.getAttribute(name);
  }

  private static String nullToEmpty(String s) {

    return Objects.toString(s, "");
  }

  /**
   * Evaluate expression in a string.
   * @param s string in witch expression must be replaced
   * @param allowExec allow execution of code
   * @return a string with expression evaluated
   * @throws Aozan3Exception if an error occurs while parsing the string or
   *           executing an expression
   */
  private String evaluateExpressions(final String s, Configuration conf)
      throws Aozan3Exception {

    if (s == null) {
      return null;
    }

    final StringBuilder result = new StringBuilder();

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      final int c0 = s.codePointAt(i);

      // Variable substitution
      if (c0 == '$' && i + 1 < len) {

        final int c1 = s.codePointAt(i + 1);
        if (c1 == '{') {

          final String expr = subStr(s, i + 2, '}');

          final String trimmedExpr = expr.trim();
          if (this.conf.containsKey(trimmedExpr)) {
            result.append(this.conf.get(trimmedExpr));
          }

          i += expr.length() + 2;
          continue;
        }
      }

      result.appendCodePoint(c0);
    }

    return result.toString();
  }

  private String subStr(final String s, final int beginIndex,
      final int charPoint) throws Aozan3Exception {

    final int endIndex = s.indexOf(charPoint, beginIndex);

    if (endIndex == -1) {
      throw new Aozan3Exception(
          "Unexpected end of expression in \"" + s + "\"");
    }

    return s.substring(beginIndex, endIndex);
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param conf initial configuration
   * @param logger default logger
   */
  public RecipeParser(Configuration conf, AozanLogger logger) {

    requireNonNull(conf);
    this.conf = conf;

    // Set logger
    if (logger != null) {
      this.logger = logger;
    }
  }

}
