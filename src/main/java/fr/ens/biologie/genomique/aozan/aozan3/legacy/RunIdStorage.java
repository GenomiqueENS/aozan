package fr.ens.biologie.genomique.aozan.aozan3.legacy;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;

/**
 * This class allow to read and write processed run ids.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RunIdStorage {

  private Path filePath;

  /**
   * Load run ids.
   * @return a set with the run ids
   * @throws Aozan3Exception occurs while reading the file
   */
  public Set<RunId> load() throws Aozan3Exception {

    if (!Files.isRegularFile(this.filePath)) {
      return Collections.emptySet();
    }

    Set<RunId> result = new HashSet<>();

    try {
      for (String line : Files.readAllLines(this.filePath)) {
        result.add(new RunId(line.trim()));
      }
    } catch (IOException e) {
      throw new Aozan3Exception("Error while loading run id list", e);
    }

    return result;
  }

  /**
   * Add a run id to the storage
   * @param runId the run id to add
   * @throws Aozan3Exception if an error occurs while adding the run id to the
   *           file
   */
  public void add(RunId runId) throws Aozan3Exception {

    File f = this.filePath.toFile();

    // Open file
    try (RandomAccessFile raf = new RandomAccessFile(f, "rws");
        FileChannel channel = raf.getChannel()) {

      // Creating lock
      FileLock lock = channel.lock();

      // Set position at the end of the file
      raf.seek(f.length());

      // Add the run_id at the end of the file
      raf.writeBytes(runId.toString().trim() + '\n');

      // Release locks
      if (lock != null) {
        lock.release();
      }
    } catch (IOException e) {
      throw new Aozan3Exception("Error while adding a run id to list", e);
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param runIdStoragePath path of the run id storage
   */
  public RunIdStorage(Path runIdStoragePath) {

    requireNonNull(runIdStoragePath);

    this.filePath = runIdStoragePath;

  }

}
