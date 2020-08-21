package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedChildTags;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getTagValue;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToEmpty;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToUnknownSource;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeXMLParser.RECIPE_STORAGES_TAG_NAME;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;

/**
 * This class define a XML parser for storages.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class StoragesXMLParser extends AbstractXMLParser<Void> {

  private static final String STORAGE_TAG_NAME = "storage";
  private static final String STORAGE_NAME_TAG_NAME = "name";
  private static final String STORAGE_PATH_TAG_NAME = "path";
  private static final String STORAGE_SOURCE_TAG_NAME = "source";

  private final Recipe recipe;

  /**
   * Parse a storage tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  protected Void parse(NodeList nList, String source) throws Aozan3Exception {

    requireNonNull(nList);

    if (nList.getLength() == 0) {
      throw new Aozan3Exception("No <"
          + RECIPE_STORAGES_TAG_NAME + "> tag found in "
          + nullToUnknownSource(source));
    }

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, STORAGE_TAG_NAME);

        // Get the include path
        Path includePath = getIncludePath(element);

        // Load storages in the include file
        if (includePath != null) {
          StoragesXMLParser parser = new StoragesXMLParser(this.recipe);
          parser.parse(includePath);
        }

        parseStorageTag(this.recipe, element);
      }
    }

    return null;
  }

  /**
   * Parse a storage tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  void parseStorageTag(final Recipe recipe, final Element rootElement)
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

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param recipe the recipe of the step
   */
  StoragesXMLParser(Recipe recipe) {

    super(RECIPE_STORAGES_TAG_NAME, recipe.getLogger());

    requireNonNull(recipe);

    this.recipe = recipe;
  }

}
