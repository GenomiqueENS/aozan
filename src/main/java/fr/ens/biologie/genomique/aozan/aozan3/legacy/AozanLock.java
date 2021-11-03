package fr.ens.biologie.genomique.aozan.aozan3.legacy;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;

/**
 * This class define a global lock for Aozan.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class AozanLock {

  private Path lockFile;

  /**
   * Create the lock file.
   * @throws Aozan3Exception if error occurs while creating the lock file
   */
  public void createLock() throws Aozan3Exception {

    try {
      Files.write(this.lockFile, ("" + Common.getCurrentPid()).getBytes());
    } catch (IOException e) {
      throw new Aozan3Exception("Unable to create the Aozan main lock", e);
    }
  }

  /**
   * Remove the lock file.
   * @throws Aozan3Exception if error occurs while removing the lock file
   */
  public void unlock() throws Aozan3Exception {

    try {
      Files.delete(this.lockFile);
    } catch (IOException e) {
      throw new Aozan3Exception("Unable to remove the Aozan main lock", e);
    }
  }

  /**
   * Check if lock is locked.
   * @return true if the lock is locked
   */
  public boolean isLocked() {

    if (!Files.isRegularFile(this.lockFile)) {
      return false;
    }

    if (Files.isRegularFile(Paths.get("/proc/" + loadPid(this.lockFile)))) {
      return true;
    }

    // PID from a dead processus, lock to delete
    deleteLockFile(this.lockFile);

    return false;
  }

  //
  // Other methods
  //

  /**
   * Load the PID in the lock file.
   * @param lockFile lock file to read
   * @return the PID in the lock file
   */
  private static int loadPid(Path lockFile) {

    List<String> s;
    try {
      s = Files.readAllLines(lockFile);

      if (s.isEmpty()) {
        return -1;
      }
      return Integer.parseInt(s.get(0).trim());

    } catch (IOException | NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Delete a lock file.
   * @param lockFile the lock file to delete
   */
  private static void deleteLockFile(Path lockFile) {

    if (Files.isRegularFile(lockFile)) {
      lockFile.toFile().delete();
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param lockFile the lock file
   */
  public AozanLock(Path lockFile) {

    requireNonNull(lockFile);

    this.lockFile = lockFile;
  }

}
