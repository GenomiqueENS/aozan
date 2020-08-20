package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedAttributes;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.checkAllowedChildTags;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getAttribute;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getTagValue;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToEmpty;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToUnknownSource;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeParser.RECIPE_STORAGES_TAG_NAME;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;

/**
 * This class define a XML parser for storages.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class StoragesParser {

  private static final String STORAGE_TAG_NAME = "storage";
  private static final String STORAGE_NAME_TAG_NAME = "name";
  private static final String STORAGE_PATH_TAG_NAME = "path";
  private static final String STORAGE_SOURCE_TAG_NAME = "source";
  private static final String STORAGES_INCLUDE_ATTR_NAME = "include";

  private final Recipe recipe;

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public void parse(Path path) throws Aozan3Exception {

    try {
      parse(Files.newInputStream(path), path.toString());
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
  public void parse(InputStream is) throws Aozan3Exception {
    parse(is, null);
  }

  /**
   * Parse XML as an input stream.
   * @param is input stream
   * @param source XML source description
   * @throws Aozan3Exception if an error occurs while parsing the file
   */
  public void parse(InputStream is, String source) throws Aozan3Exception {

    Objects.requireNonNull(is);

    this.recipe.getLogger()
        .info("Start parsing the storage file: " + nullToUnknownSource(source));

    // Parse file into a Document object
    try {
      final DocumentBuilderFactory dbFactory =
          DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      // Create Recipe object
      parse(doc, source);

    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new Aozan3Exception(
          "Error while parsing recipe file: " + e.getMessage(), e);
    }
  }

  /**
   * Parse XML document.
   * @param doc XML document
   * @param source XML source description
   * @throws Aozan3Exception if an error occurs while parsing the XML document
   */
  private void parse(Document doc, String source) throws Aozan3Exception {

    parseStoragesTag(doc.getElementsByTagName(RECIPE_STORAGES_TAG_NAME),
        source);
  }

  /**
   * Parse a run step XML tag.
   * @param rootElement element to parse
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  void parseStoragesTag(final Element rootElement) throws Aozan3Exception {

    parseStoragesTag(rootElement, null);
  }

  /**
   * Parse a run step XML tag.
   * @param rootElement element to parse
   * @param source XML source description
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  void parseStoragesTag(final Element rootElement, final String source)
      throws Aozan3Exception {

    requireNonNull(rootElement);

    parseStoragesTag(rootElement.getElementsByTagName(RECIPE_STORAGES_TAG_NAME),
        source);
  }

  /**
   * Parse a storage tag.
   * @param recipe the recipe
   * @param rootElement element to parse
   * @throws Aozan3Exception if an error occurs while parsing the XML
   */
  void parseStoragesTag(NodeList nList, String source) throws Aozan3Exception {

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

        // Check allowed attributes for the "storages" tag
        checkAllowedAttributes(element, STORAGES_INCLUDE_ATTR_NAME);

        // Check allowed tags for the "storages" tag
        checkAllowedChildTags(element, STORAGE_TAG_NAME);

        // Get the include path
        String includePath =
            nullToEmpty(getAttribute(element, STORAGES_INCLUDE_ATTR_NAME))
                .trim();

        // Load storages in the include file
        if (!includePath.isEmpty()) {
          StoragesParser parser = new StoragesParser(this.recipe);
          parser.parse(Paths.get(includePath));
        }

        parseStorageTag(this.recipe, element);
      }
    }
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
  StoragesParser(Recipe recipe) {

    requireNonNull(recipe);

    this.recipe = recipe;
  }

}
