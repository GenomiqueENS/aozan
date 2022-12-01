package fr.ens.biologie.genomique.aozan.aozan3.action;

import static fr.ens.biologie.genomique.aozan.aozan3.ConfigurationDefaults.RECIPES_DIRECTORY_KEY;

import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Strings;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Common;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.Globals;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeFinder;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.RecipeFinder.RecipePath;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define an action that list existing recipes.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class ListAction implements Action {

  /** Name of this action. */
  public static final String ACTION_NAME = "list";

  private static final int TEXT_PADDING = 30;

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "list existing recipes";
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

    try {

      // parse the command line arguments
      final CommandLine line =
          parser.parse(options, arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
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

    // Check the number of arguments
    if (arguments.size() != argsOptions) {
      help(options);
    }

    String recipesDirectory = conf.get(RECIPES_DIRECTORY_KEY, "").trim();

    try {

      // Get the list of available recipes
      RecipeFinder finder = new RecipeFinder(Paths.get(recipesDirectory));
      List<RecipeFinder.RecipePath> recipes = finder.getRecipes();

      if (!recipes.isEmpty()) {

        System.out.println(
            Strings.padEnd("Recipe name", TEXT_PADDING, ' ') + "Description");

        for (RecipePath rp : recipes) {
          System.out.println(Strings.padEnd(rp.recipeName, TEXT_PADDING, ' ')
              + (rp.description.isEmpty()
                  ? "(no description)" : rp.description));
        }

      } else {
        // No recipe found
        System.err.println("No recipe found in: " + recipesDirectory);
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
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE + ".sh " + ACTION_NAME,
        options);

    Common.exit(0);
  }

}
