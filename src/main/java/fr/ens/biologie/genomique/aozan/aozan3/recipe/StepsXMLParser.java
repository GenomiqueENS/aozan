package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedChildTags;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getTagValue;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToEmpty;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToUnknownSource;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeXMLParser.RECIPE_STEPS_TAG_NAME;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeXMLParser.RECIPE_STORAGES_TAG_NAME;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DefaultRunIdGenerator;
import fr.ens.biologie.genomique.aozan.aozan3.runconfigurationprovider.RunConfigurationProvider;

/**
 * This class define a XML parser for steps.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class StepsXMLParser extends AbstractXMLParser<List<Step>> {

  private static final String STEP_TAG_NAME = "step";
  private static final String STEP_NAME_TAG_NAME = "name";
  private static final String STEP_PROVIDER_TAG_NAME = "provider";
  private static final String STEP_SINKSTORAGE_TAG_NAME = "sinkstorage";
  private static final String STEP_CONF_TAG_NAME = "configuration";
  private static final String STEP_OUTPUTIDSCHEME_TAG_NAME = "outputidscheme";

  private final Recipe recipe;
  private final RunConfigurationProvider runConfProvider;

  @Override
  protected List<Step> parse(NodeList nList, String source)
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

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, STEP_TAG_NAME);

        // Get the include path
        Path includePath = getIncludePath(element, source);

        // Load steps in the include file
        if (includePath != null) {
          StepsXMLParser parser =
              new StepsXMLParser(this.recipe, this.runConfProvider);
          result.addAll(parser.parse(includePath));
        }

        result.addAll(parseStepTag(element, source));
      }
    }

    return result;
  }

  /**
   * Parse a run step XML tag.
   * @param recipe the recipe
   * @param runConfProvider run configuration provider
   * @param rootElement element to parse
   * @param source file source
   * @return a list with step objects
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  List<Step> parseStepTag(final Element rootElement, final String source)
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

        Configuration conf = new ConfigurationXMLParser(STEP_CONF_TAG_NAME,
            STEP_TAG_NAME, recipe.getConfiguration(), recipe.getLogger())
                .parse(element, source);

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
  StepsXMLParser(Recipe recipe, RunConfigurationProvider runConfProvider) {

    super(RECIPE_STEPS_TAG_NAME, "step", recipe.getLogger());

    requireNonNull(recipe);
    requireNonNull(runConfProvider);

    this.recipe = recipe;
    this.runConfProvider = runConfProvider;
  }

}
