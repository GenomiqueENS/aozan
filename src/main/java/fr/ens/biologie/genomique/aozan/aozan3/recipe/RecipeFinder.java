package fr.ens.biologie.genomique.aozan.aozan3.recipe;

import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.getTagValue;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.ParserUtils.nullToEmpty;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeXMLParser.RECIPE_NAME_TAG_NAME;
import static fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeXMLParser.RECIPE_DESCRIPTION_TAG_NAME;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;

/**
 * This class define a finder for recipe files.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RecipeFinder {

  private static final String RECIPE_FILE_EXTENSION = ".xml";

  /**
   * This class define a result of the finder.
   */
  public static class RecipePath implements Comparable<RecipePath> {

    public final String recipeName;
    public final String description;
    public final Path recipePath;

    private RecipePath(String recipeName, String description, Path recipePath) {

      this.recipeName = recipeName;
      this.description = description;
      this.recipePath = recipePath;
    }

    @Override
    public String toString() {
      return "RecipePath [recipeName="
          + recipeName + ", description=" + description + ", recipePath="
          + recipePath + "]";
    }

    @Override
    public int compareTo(RecipePath o) {

      if (o == null) {
        return -1;
      }

      int c = this.recipeName.compareTo(o.recipeName);

      if (c == 0) {
        c = this.description.compareTo(o.description);
      }

      if (c == 0) {
        c = this.recipePath.compareTo(o.recipePath);
      }

      return c;

    }
  }

  private Path directory;

  /**
   * Get the recipes existing in the directory.
   * @return a list of the recipe sorted by name
   * @throws Aozan3Exception if an error occurs while searching for recipe files
   */
  public List<RecipePath> getRecipes() throws Aozan3Exception {

    List<RecipePath> result = new ArrayList<>();

    try (DirectoryStream<Path> stream =
        Files.newDirectoryStream(this.directory)) {
      for (Path path : stream) {
        if (Files.isRegularFile(path)
            && path.getFileName().toString().endsWith(RECIPE_FILE_EXTENSION)) {

          String[] array = getRecipeNameAndDescription(path);
          if (array != null) {
            result.add(new RecipePath(array[0], array[1], path));
          }

        }
      }
    } catch (IOException e) {
      throw new Aozan3Exception(e);
    }

    Collections.sort(result);

    return result;
  }

  /**
   * Load a file and try to get the recipe name inside if it is a recipe file.
   * @param file the file to load
   * @return the name of the recipe in a String or null if the file is not a
   *         recipe file
   */
  public static String getRecipeName(Path file) {

    String[] result = getRecipeNameAndDescription(file);

    return result == null ? null : result[0];
  }

  /**
   * Load a file and try to get the recipe name and description inside if it is
   * a recipe file.
   * @param file the file to load
   * @return the name and the description of the recipe in a String array or
   *         null if the file is not a recipe file
   */
  private static String[] getRecipeNameAndDescription(Path file) {

    requireNonNull(file);

    if (!Files.isRegularFile(file)) {
      return null;
    }

    try {
      final DocumentBuilderFactory dbFactory =
          DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      final Document doc = dBuilder.parse(file.toFile());
      doc.getDocumentElement().normalize();

      NodeList nodeList =
          doc.getElementsByTagName(RecipeXMLParser.ROOT_TAG_NAME);

      for (int i = 0; i < nodeList.getLength(); i++) {

        Node nNode = nodeList.item(i);
        if (nNode.getNodeType() == Node.ELEMENT_NODE) {

          Element element = (Element) nNode;

          // Get the recipe name
          String recipeName =
              nullToEmpty(getTagValue(RECIPE_NAME_TAG_NAME, element));

          // Get the recipe description
          String description =
              nullToEmpty(getTagValue(RECIPE_DESCRIPTION_TAG_NAME, element));

          if (!recipeName.isEmpty()) {
            return new String[] {recipeName, description};
          }

        }

      }

      return null;
    } catch (IOException | ParserConfigurationException | SAXException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Test if a recipe exists in a list of recipes.
   * @param recipes recipe list
   * @param recipeName name of the recipe
   * @return true if the recipe exists
   */
  public static boolean isRecipe(List<RecipePath> recipes, String recipeName) {

    requireNonNull(recipes);
    requireNonNull(recipeName);

    for (RecipePath rp : recipes) {

      if (recipeName.equalsIgnoreCase(rp.recipeName)) {

        return true;
      }
    }

    return false;

  }

  /**
   * Test if a recipe is unique in a list of recipes.
   * @param recipes recipe list
   * @param recipeName name of the recipe
   * @return true if the recipe is unique
   */
  public static boolean isRecipeUnique(List<RecipePath> recipes,
      String recipeName) {

    requireNonNull(recipes);
    requireNonNull(recipeName);

    boolean found = false;

    for (RecipePath rp : recipes) {

      if (recipeName.equalsIgnoreCase(rp.recipeName)) {
        if (found) {
          return false;
        }
        found = true;
      }

    }

    return true;
  }

  /**
   * Get the path of a recipe in a list of recipes.
   * @param recipes recipe list
   * @param recipeName name of the recipe
   * @return true if the recipe exists
   */
  public static Path getRecipePath(List<RecipePath> recipes,
      String recipeName) {

    requireNonNull(recipes);
    requireNonNull(recipeName);

    for (RecipePath rp : recipes) {

      if (recipeName.equalsIgnoreCase(rp.recipeName)) {

        return rp.recipePath;
      }
    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param directory path of the recipe directory
   * @throws Aozan3Exception if the path is not an existing directory
   */
  public RecipeFinder(Path directory) throws Aozan3Exception {

    requireNonNull(directory);

    if (!Files.isDirectory(directory)) {
      throw new Aozan3Exception(
          "The path for recipe does not exists or is not a directory: "
              + directory);
    }

    this.directory = directory;

  }

}
