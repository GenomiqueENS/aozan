package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedAttributes;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedChildTags;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getAttribute;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getTagValue;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToEmpty;
import static java.lang.Boolean.parseBoolean;
import static java.util.Objects.requireNonNull;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.EmptyRunConfigurationProvider;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.RunConfigurationProvider;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.RunConfigurationProviderService;
import fr.ens.biologie.genomique.kenetre.log.DummyLogger;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define a recipe XML parser.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RecipeXMLParser extends AbstractXMLParser<Recipe> {

  private final Configuration conf;
  private final Configuration cliConf;
  private GenericLogger logger = new DummyLogger();

  /** Version of the format of the workflow file. */
  private static final String FORMAT_VERSION = "0.1";

  static final String ROOT_TAG_NAME = "recipe";
  private static final String RECIPE_FORMAT_VERSION_TAG_NAME = "formatversion";
  static final String RECIPE_NAME_TAG_NAME = "name";
  static final String RECIPE_DESCRIPTION_TAG_NAME = "description";
  private static final String RECIPE_CONF_TAG_NAME = "configuration";
  static final String RECIPE_STORAGES_TAG_NAME = "storages";
  private static final String RECIPE_RUN_CONFIGURATION_TAG_NAME =
      "runconfiguration";
  private static final String RECIPE_DATASOURCE_TAG_NAME = "datasource";
  private static final String RECIPE_DATASOURCES_TAG_NAME = "datasources";
  static final String RECIPE_STEPS_TAG_NAME = "steps";

  private static final String RUNCONF_PROVIDER_TAG_NAME = "provider";
  private static final String RUNCONF_CONF_TAG_NAME = "configuration";

  private static final String DATASOURCE_PROVIDER_TAG_NAME = "provider";
  private static final String DATASOURCE_STORAGE_TAG_NAME = "storage";
  private static final String DATASOURCE_CONF_TAG_NAME = "configuration";
  private static final String DATASOURCE_INPROGRESS_ATTR_NAME = "inprogress";

  @Override
  protected Recipe parse(NodeList nList, String source) throws Aozan3Exception {

    Recipe result = null;

    for (int i = 0; i < nList.getLength(); i++) {

      if (result != null) {
        throw new Aozan3Exception("Found several recipe in the recipe file");
      }

      Node nNode = nList.item(i);
      if (nNode.getNodeType() == Node.ELEMENT_NODE) {
        result = parseRecipeTag((Element) nNode, source);
      }

    }

    return result;
  }

  /**
   * Parse a recipe tag.
   * @param element element to parse
   * @param source file source
   * @return a new recipe object
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private Recipe parseRecipeTag(Element element, String source)
      throws Aozan3Exception {

    // Check allowed child tags of the root tag
    checkAllowedChildTags(element, RECIPE_FORMAT_VERSION_TAG_NAME,
        RECIPE_NAME_TAG_NAME, RECIPE_CONF_TAG_NAME, RECIPE_STORAGES_TAG_NAME,
        RECIPE_RUN_CONFIGURATION_TAG_NAME, RECIPE_DATASOURCES_TAG_NAME,
        RECIPE_STEPS_TAG_NAME);

    // Check version of the XML file
    final String formatVersion =
        nullToEmpty(getTagValue(RECIPE_FORMAT_VERSION_TAG_NAME, element));
    if (!FORMAT_VERSION.equals(formatVersion)) {
      throw new Aozan3Exception(
          "Invalid version of the format of the recipe file.");
    }

    // Get the recipe name
    String recipeName = nullToEmpty(getTagValue(RECIPE_NAME_TAG_NAME, element));

    // Get the recipe description
    String description =
        nullToEmpty(getTagValue(RECIPE_DESCRIPTION_TAG_NAME, element));

    // Get the recipe configuration
    Configuration recipeConf = new ConfigurationXMLParser(RECIPE_CONF_TAG_NAME,
        "recipe configuration", this.conf, this.logger).parse(element, source);

    // Overwrite recipe conf with command line configuration
    recipeConf.set(this.cliConf);

    // Create a new recipe
    Recipe recipe =
        new Recipe(recipeName, description, recipeConf, getLogger());

    // Parse storages
    new StoragesXMLParser(recipe).parse(element, source);

    // Parse run configuration provider
    RunConfigurationProvider runConfProvider =
        parseRunConfigurationTag(recipe, element, source);

    // Parse the datasources tag
    parseDataSourcesTag(recipe, element, source);

    // Parse steps
    recipe.addSteps(
        new StepsXMLParser(recipe, runConfProvider).parse(element, source));

    return recipe;
  }

  /**
   * Parse a run configuration XML tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @param source file source
   * @return a new RunConfigurationProvider object
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private static RunConfigurationProvider parseRunConfigurationTag(
      final Recipe recipe, final Element rootElement, final String source)
      throws Aozan3Exception {

    requireNonNull(recipe);
    requireNonNull(rootElement);

    RunConfigurationProvider result = new EmptyRunConfigurationProvider();

    final NodeList nList =
        rootElement.getElementsByTagName(RECIPE_RUN_CONFIGURATION_TAG_NAME);

    for (int i = 0; i < nList.getLength(); i++) {

      if (i > 1) {
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

        Configuration conf = new ConfigurationXMLParser(RUNCONF_CONF_TAG_NAME,
            "run configuration", recipe.getConfiguration(), recipe.getLogger())
                .parse(element, source);

        result = RunConfigurationProviderService.getInstance()
            .newService(providerName);
        result.init(conf, recipe.getLogger());
      }
    }

    return result;
  }

  /**
   * Parse a run datasources XML tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @param source file source
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private void parseDataSourcesTag(final Recipe recipe,
      final Element rootElement, final String source) throws Aozan3Exception {

    requireNonNull(recipe);
    requireNonNull(rootElement);

    final NodeList nList =
        rootElement.getElementsByTagName(RECIPE_DATASOURCES_TAG_NAME);

    for (int i = 0; i < nList.getLength(); i++) {

      if (i > 0) {
        throw new Aozan3Exception(
            "Found more than one \"" + RECIPE_DATASOURCES_TAG_NAME + "\" tag");
      }

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed tags for the "datasources" tag
        checkAllowedChildTags(element, RECIPE_DATASOURCE_TAG_NAME);

        // Parse each datasource element
        parseDataSourceTag(recipe, element, source);
      }
    }
  }

  /**
   * Parse a run data source XML tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @param source file source
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  private void parseDataSourceTag(final Recipe recipe,
      final Element rootElement, final String source) throws Aozan3Exception {

    requireNonNull(recipe);
    requireNonNull(rootElement);

    final NodeList nList =
        rootElement.getElementsByTagName(RECIPE_DATASOURCE_TAG_NAME);

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed attributes for the "storages" tag
        checkAllowedAttributes(element, DATASOURCE_INPROGRESS_ATTR_NAME);

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, DATASOURCE_PROVIDER_TAG_NAME,
            DATASOURCE_STORAGE_TAG_NAME, DATASOURCE_CONF_TAG_NAME);

        final boolean inProgress = parseBoolean(
            nullToEmpty(getAttribute(element, DATASOURCE_INPROGRESS_ATTR_NAME)
                .trim().toLowerCase()));

        String providerName =
            nullToEmpty(getTagValue(DATASOURCE_PROVIDER_TAG_NAME, element));
        String storageName =
            nullToEmpty(getTagValue(DATASOURCE_STORAGE_TAG_NAME, element));

        Configuration conf =
            new ConfigurationXMLParser(DATASOURCE_CONF_TAG_NAME,
                RECIPE_DATASOURCE_TAG_NAME, recipe.getConfiguration(),
                recipe.getLogger()).parse(element, source);

        recipe.addDataProvider(providerName, storageName, inProgress, conf);
      }
    }
  }

  //
  // Utility parsing methods
  //

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param conf initial configuration
   * @param logger default logger
   */
  public RecipeXMLParser(Configuration conf, GenericLogger logger) {

    this(conf, new Configuration(), logger);
  }

  /**
   * Public constructor.
   * @param conf initial configuration
   * @param cliConf configuration that will overwrite global configuration of
   *          the recipe
   * @param logger default logger
   */
  public RecipeXMLParser(Configuration conf, Configuration cliConf,
      GenericLogger logger) {

    super(ROOT_TAG_NAME, "recipe", logger);

    requireNonNull(conf);
    requireNonNull(cliConf);
    this.conf = conf;
    this.cliConf = cliConf;

    // Set logger
    if (logger != null) {
      this.logger = logger;
    }
  }

}
