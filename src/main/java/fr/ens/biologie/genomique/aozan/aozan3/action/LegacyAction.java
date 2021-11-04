package fr.ens.biologie.genomique.aozan.aozan3.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Common;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.legacy.AozanLock;
import fr.ens.biologie.genomique.aozan.aozan3.legacy.LegacyRecipes;
import fr.ens.biologie.genomique.aozan.aozan3.legacy.RunIdStorage;
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

      // Lock Aozan
      AozanLock mainLock = new AozanLock(recipes.getMainLockPath());

      // Another instance of Aozan is running, exiting
      if (mainLock.isLocked()) {
        return;
      }

      // Create Aozan lock
      mainLock.createLock();

      // Perform synchronization
      execute(recipes, recipes.getSyncStepRecipe(), recipes.getVarPath());

      // Perform demultiplexing
      execute(recipes, recipes.getDemuxStepRecipe(), recipes.getVarPath());

      // Perform QC
      execute(recipes, recipes.getQCStepRecipe(), recipes.getVarPath());

      // Unlock Aozan
      mainLock.unlock();

    } catch (Aozan3Exception e) {
      Common.errorExit(e,
          "Error while parsing command line arguments: " + e.getMessage());
    }
  }

  //
  // Other methods
  //

  private static void execute(LegacyRecipes recipes, Recipe recipe,
      Path varPath) throws Aozan3Exception {
    execute(recipes, recipe, varPath, null);
  }

  private static void execute(LegacyRecipes recipes, Recipe recipe,
      Path varPath, Set<RunId> forbiddenRuns) throws Aozan3Exception {

    if (recipe == null) {
      return;
    }

    // Get output path of the recipe
    Path outputPath = recipes.getOutputPath(recipe);

    RunIdStorage processedRunIdStorage = new RunIdStorage(
        Paths.get(varPath.toString(), recipe.getName() + ".done"));

    // Run to process
    Set<RunId> todoRunIds = new HashSet<>(recipe.availableRuns());

    // Remove forbidden runs
    if (forbiddenRuns != null) {
      todoRunIds.removeAll(forbiddenRuns);
    }

    // Remove processed runs
    Set<RunId> processed = processedRunIdStorage.load();
    todoRunIds.removeAll(processed);

    // Remove locked runs
    Set<RunId> lockedRunIds = new HashSet<>();
    for (RunId runId : todoRunIds) {
      Path lockPath = Paths.get(outputPath.toString(), runId.getId() + ".lock");
      if (Files.isRegularFile(lockPath)) {
        lockedRunIds.add(runId);
      }
    }
    todoRunIds.removeAll(lockedRunIds);

    // Nothing to do
    if (todoRunIds.isEmpty()) {
      return;
    }

    // Initialize recipe
    if (forbiddenRuns == null) {
      recipe.init();
      forbiddenRuns = new HashSet<>();
    }

    // Get the first run
    RunId runId = todoRunIds.iterator().next();

    // Define lock path
    Path lockPath = Paths.get(outputPath.toString(), runId.getId() + ".lock");

    // Lock the step, if a lock already exists, there is nothing to do
    if (!lockStep(lockPath, recipe.getName())) {
      return;
    }

    // If run has been processed, add it to the list of processed runs
    if (recipe.execute(runId.getId())) {
      processedRunIdStorage.add(runId);
    } else {
      forbiddenRuns.add(runId);
    }

    // Unlock the step
    unlockStep(lockPath);

    // Search for new runs to process
    execute(recipes, recipe, varPath, forbiddenRuns);
  }

  /**
   * Lock a step.
   * @return true if a lock has been created
   * @throws Aozan3Exception if an error occurs while creating the lock file
   */
  private static boolean lockStep(Path lockPath, String stepName)
      throws Aozan3Exception {

    // Check if parent directory of the lock file exists
    if (!Files.isDirectory(lockPath.getParent())) {
      throw new Aozan3Exception(
          "Parent directory of lock file does not exist. The lock file for "
              + stepName + " step has not been created: " + lockPath);
    }

    // Return false if the run is currently processed
    if (Files.isRegularFile(lockPath)) {
      return false;
    }

    // Create the lock file
    try {
      Files.createFile(lockPath);
    } catch (IOException e) {
      throw new Aozan3Exception("The lock file cannot be created for "
          + stepName + ": " + "step has not been created: " + lockPath);
    }

    return true;
  }

  /**
   * Unlock a step.
   * @param lockPath lock file path
   */
  private static boolean unlockStep(Path lockPath) {

    // Remove lock file if exists
    if (Files.isRegularFile(lockPath)) {
      try {
        Files.delete(lockPath);
      } catch (IOException e) {
        return false;
      }
    }

    return true;
  }
}
