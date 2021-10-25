package fr.ens.biologie.genomique.aozan.aozan3.action;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Common;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.legacy.LegacyRecipes;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.Recipe;

/**
 * This class define an legacy action. The legacy action allow to use Aozan 3
 * like Aozan 2.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class LegacyAction implements Action {

  /** Name of this action. */
  public static final String ACTION_NAME = "legacy";

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "exec Aozan in legacy mode";
  }

  @Override
  public boolean isHidden() {
    return false;
  }

  @Override
  public void action(Configuration conf, List<String> arguments,
      AozanLogger logger) {

    if (arguments.isEmpty()) {
      Common.showErrorMessageAndExit(
          "Argument missing for " + ACTION_NAME + " action.");
    }

    try {
      Path confFile = Paths.get(arguments.get(0));
      LegacyRecipes recipes = new LegacyRecipes(conf, logger, confFile);

      // Perform synchronization
      execute(recipes.getSyncStepRecipe());

      // Perform demultiplexing
      execute(recipes.getDemuxStepRecipe());

      // Perform QC
      execute(recipes.getQCStepRecipe());

    } catch (Aozan3Exception e) {
      Common.errorExit(e,
          "Error while parsing command line arguments: " + e.getMessage());
    }
  }

  //
  // Other methods
  //

  private static void execute(Recipe recipe) throws Aozan3Exception {

    if (recipe == null) {
      return;
    }

    // Initialize recipe
    recipe.init();

    // Execute the recipe for all available runs
    recipe.execute();
  }

}
