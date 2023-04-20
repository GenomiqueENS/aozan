package fr.ens.biologie.genomique.aozan.aozan3.action;

import static java.util.Objects.requireNonNull;

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
import fr.ens.biologie.genomique.aozan.aozan3.SendMail;
import fr.ens.biologie.genomique.aozan.aozan3.legacy.AozanLock;
import fr.ens.biologie.genomique.aozan.aozan3.legacy.LegacyRecipes;
import fr.ens.biologie.genomique.aozan.aozan3.legacy.RunIdStorage;
import fr.ens.biologie.genomique.aozan.aozan3.recipe.Recipe;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

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
      GenericLogger logger) {

    if (arguments.isEmpty()) {
      Common.showErrorMessageAndExit(
          "Argument missing for " + ACTION_NAME + " action.");
    }

    SendMail sendMail = null;
    try {
      Path confFile = Paths.get(arguments.get(0));
      LegacyRecipes recipes = new LegacyRecipes(conf, logger, confFile);
      sendMail = recipes.getSendMail();

      // Test if Aozan is enabled
      if (!recipes.isAozanEnabled()) {
        // Nothing to do Aozan is disabled
        return;
      }

      // Lock Aozan
      AozanLock mainLock = new AozanLock(recipes.getMainLockPath());

      // Another instance of Aozan is running, exiting
      if (mainLock.isLocked()) {
        return;
      }

      // Create Aozan lock
      mainLock.createLock();

      // Perform new run discovering
      execute(recipes, recipes.getNewRunStepRecipe(), recipes.getVarPath());

      // Perform end of run discovering
      execute(recipes, recipes.getEndRunStepRecipe(), recipes.getVarPath());

      // Perform synchronization
      execute(recipes, recipes.getSyncStepRecipe(), recipes.getVarPath());

      // Perform demultiplexing
      execute(recipes, recipes.getDemuxStepRecipe(), recipes.getVarPath());

      // Perform QC
      execute(recipes, recipes.getQCStepRecipe(), recipes.getVarPath());

      // Unlock Aozan
      mainLock.unlock();

    } catch (Aozan3Exception e) {
      logger.error(e, true);
      if (sendMail != null) {
        sendMail.sendMail(e);
      }
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
      Path varPath, Set<RunId> excludedRunIds) throws Aozan3Exception {

    if (recipe == null) {
      return;
    }

    boolean initializedRecipe = true;
    if (excludedRunIds == null) {
      excludedRunIds = new HashSet<>();
      initializedRecipe = false;
    }

    // Get output path of the recipe
    Path lockDirectory = recipes.getLockDirectory(recipe);

    // Remove deny run ids
    excludedRunIds.addAll(runStorage(recipes, varPath, recipe, ".deny").load());

    // Remove done run ids
    RunIdStorage processedRunIdStorage =
        runStorage(recipes, varPath, recipe, ".done");
    excludedRunIds.addAll(processedRunIdStorage.load());

    // Run to process
    Set<RunId> todoRunIds = new HashSet<>(recipe.availableRuns(excludedRunIds));

    // Remove excluded run ids if run provider does not handle excluded run ids
    todoRunIds.removeAll(excludedRunIds);

    // Remove locked runs
    if (lockDirectory != null) {
      Set<RunId> lockedRunIds = new HashSet<>();
      for (RunId runId : todoRunIds) {
        Path lockPath =
            Paths.get(lockDirectory.toString(), runId.getId() + ".lock");
        if (Files.isRegularFile(lockPath)) {
          lockedRunIds.add(runId);
        }
      }
      todoRunIds.removeAll(lockedRunIds);
    }

    // Nothing to do
    if (todoRunIds.isEmpty()) {
      return;
    }

    // Initialize recipe
    if (!initializedRecipe) {
      recipe.init();
    }

    // Get the first run
    RunId runId = todoRunIds.iterator().next();

    // Define lock path
    Path lockPath = lockDirectory != null
        ? Paths.get(lockDirectory.toString(), runId.getId() + ".lock") : null;

    // Lock the step, if a lock already exists, there is nothing to do
    if (lockPath != null && !lockStep(lockPath, recipe.getName())) {
      return;
    }

    // If run has been processed, add it to the list of processed runs
    if (recipe.execute(runId.getId())) {
      processedRunIdStorage.add(runId);
    } else {
      excludedRunIds.add(runId);
    }

    // Unlock the step
    if (lockPath != null) {
      unlockStep(lockPath);
    }

    // Search for new runs to process
    execute(recipes, recipe, varPath, excludedRunIds);
  }

  /**
   * Create a RunIdStorage.
   * @param recipes recipes
   * @param directory directory for the storage
   * @param recipe recipe
   * @param suffix suffic of the storage file
   * @return a RunIdStorage object
   */
  private static RunIdStorage runStorage(LegacyRecipes recipes, Path directory,
      Recipe recipe, String suffix) {

    requireNonNull(directory);
    requireNonNull(recipe);
    requireNonNull(suffix);

    Path file = Paths.get(directory.toString(),
        recipes.getRecipeDoneDenyFilename(recipe) + suffix);

    if (Files.exists(file)) {
      return new RunIdStorage(file);
    }

    file = Paths.get(directory.toString(), recipe.getName() + suffix);

    return new RunIdStorage(file);
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
