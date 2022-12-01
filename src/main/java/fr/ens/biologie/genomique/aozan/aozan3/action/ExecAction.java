package fr.ens.biologie.genomique.aozan.aozan3.action;

import static fr.ens.biologie.genomique.aozan.aozan3.ConfigurationDefaults.RECIPES_DIRECTORY_KEY;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Common;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.Globals;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.Recipe;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeFinder;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeXMLParser;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define an exec action.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class ExecAction implements Action {

  /** Name of this action. */
  public static final String ACTION_NAME = "exec";

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "exec a recipe";
  }

  @Override
  public boolean isHidden() {
    return false;
  }

  @Override
  public void action(Configuration conf, List<String> arguments,
      GenericLogger logger) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;
    boolean allRuns = false;

    try {

      // parse the command line arguments
      final CommandLine line =
          parser.parse(options, arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      // All option
      if (line.hasOption("all")) {
        allRuns = true;
        argsOptions++;
      }

      if (line.hasOption("d")) {

        String recipesDirectory = line.getOptionValue("d");
        conf.set(RECIPES_DIRECTORY_KEY, recipesDirectory);
        argsOptions += 2;
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing command line arguments: " + e.getMessage());
    }

    if (arguments.size() < argsOptions + 1) {
      help(options);
    }

    final String recipeName = arguments.get(argsOptions);

    final List<String> newArgs =
        arguments.subList(argsOptions + 1, arguments.size());

    final Configuration cliConf = getCommandLineConfiguration(newArgs);
    final List<String> runIds = getCommandLineRunIds(newArgs);

    try {

      // Add CLI conf in the global conf
      conf.set(cliConf);

      // Load recipe
      Recipe recipe = loadRecipe(conf, cliConf, recipeName, logger);

      // Initialize recipe
      recipe.init();

      // Print available runs if there is no run argument
      if (runIds.isEmpty() && !allRuns) {
        printAvailableRuns(recipe);
        Common.exit(0);
      }

      // Execute recipe
      if (allRuns) {
        recipe.execute();
      } else {
        recipe.execute(runIds);
      }

    } catch (Aozan3Exception e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }
  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  private static Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Help option
    options.addOption("h", "help", false, "display this help");

    // All option
    options.addOption("a", "all", false, "process all available runs");

    // Description option
    options.addOption(OptionBuilder.withArgName("recipes-directory").hasArg()
        .withDescription("recipes directory").withLongOpt("directory")
        .create('d'));

    return options;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + ACTION_NAME
        + " [options] recipeName/recipeFile key1=value1 key2=value2 run1_id run2_id...runN_id",
        options);

    Common.exit(0);
  }

  /**
   * Parse the command line argument to find additional configuration.
   * @param args command line arguments
   * @return a new configuration object
   */
  private Configuration getCommandLineConfiguration(List<String> args) {

    Configuration result = new Configuration();

    for (String arg : args) {

      if (arg.indexOf('=') != -1) {
        result.parseAndSet(arg);
      }
    }

    return result;
  }

  /**
   * Parse the command line to get run ids.
   * @param args command line arguments
   * @return a list of run ids
   */
  private List<String> getCommandLineRunIds(List<String> args) {

    List<String> result = new ArrayList<>();

    for (String arg : args) {

      if (arg.indexOf('=') == -1) {
        result.add(arg);
      }
    }

    return result;
  }

  //
  // Execution
  //

  private Recipe loadRecipe(Configuration conf, Configuration cliConf,
      String recipeName, GenericLogger logger) throws Aozan3Exception {

    Path recipePath = null;
    String recipesDirectory = conf.get(RECIPES_DIRECTORY_KEY, "").trim();

    if (!recipesDirectory.isEmpty()) {

      // Get the list of available recipes
      RecipeFinder finder = new RecipeFinder(Paths.get(recipesDirectory));
      List<RecipeFinder.RecipePath> recipes = finder.getRecipes();

      // The recipe has been found
      if (RecipeFinder.isRecipe(recipes, recipeName)) {

        // Test if there
        if (!RecipeFinder.isRecipeUnique(recipes, recipeName)) {
          throw new Aozan3Exception(
              "Found two or more recipes with the same name: " + recipeName);
        }

        // Get the recipe path
        recipePath = RecipeFinder.getRecipePath(recipes, recipeName);
      }
    }

    // In no recipe has been found, the recipe name can be the recipe path?
    if (recipePath == null) {

      Path p = Paths.get(recipeName);
      recipePath = RecipeFinder.getRecipeName(p) != null ? p : null;
    }

    // No recipe found
    if (recipePath == null) {
      throw new Aozan3Exception("Unknown recipe: " + recipeName);
    }

    return loadRecipe(conf, cliConf, recipePath, logger);
  }

  private Recipe loadRecipe(Configuration conf, Configuration cliConf,
      Path recipePath, GenericLogger logger) throws Aozan3Exception {

    RecipeXMLParser parser = new RecipeXMLParser(conf, cliConf, logger);
    return parser.parse(recipePath);
  }

  private void printAvailableRuns(Recipe recipe) throws Aozan3Exception {

    System.out.println("Available runs:");
    for (String runId : recipe.getAvalaibleRuns()) {
      System.out.println(runId);
    }
  }

}
