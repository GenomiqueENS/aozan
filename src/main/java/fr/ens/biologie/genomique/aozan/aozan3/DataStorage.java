package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import fr.ens.biologie.genomique.aozan.aozan3.util.JSONUtils;

/**
 * This class define a data storage.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class DataStorage {

  private final String machine;
  private final Path path;
  private final long minimalSpace = 0;
  private final boolean writable = true;

  //
  // Getters
  //

  /**
   * Get the machine of the storage
   * @return the machine of the storage
   */
  public String getMachine() {

    return this.machine;
  }

  /**
   * Get the path of the storage.
   * @return the path of the storage
   */
  public Path getPath() {

    return this.path;
  }

  /**
   * Returns if the storage is writable.
   * @return true if the storage is writable
   */
  public boolean isWritable() {

    return this.writable;
  }

  //
  // Storage info
  //

  /**
   * Get the total space of the storage.
   * @return the total space in bytes of the storage
   * @throws IOException if an error occurs while getting the information
   */
  public long getTotalSpace() throws IOException {

    FileStore store = Files.getFileStore(getPath());
    return store.getTotalSpace();
  }

  /**
   * Get the usable space of the storage.
   * @return the usable space in bytes of the storage
   * @throws IOException if an error occurs while getting the information
   */
  public long getUsableSpace() throws IOException {

    FileStore store = Files.getFileStore(getPath());
    return store.getUsableSpace();
  }

  /**
   * Get the unallocated space of the storage.
   * @return the unallocated space in bytes of the storage
   * @throws IOException if an error occurs while getting the information
   */
  public long getUnallocatedSpace() throws IOException {

    FileStore store = Files.getFileStore(getPath());
    return store.getUnallocatedSpace();
  }

  /**
   * Test if there is enough space on the storage for a required amount of disk
   * space.
   * @param size the required amount of disk space
   * @return true if there is enough space on the storage for the required
   *         amount of disk space
   * @throws IOException if an error occurs while getting disk space information
   */
  public boolean isEnoughSpace(long size) throws IOException {

    if (size <= 0) {
      return true;
    }

    return getUnallocatedSpace() - size - this.minimalSpace > 0;
  }

  /**
   * Check if there is enough space on the storage for a required amount of disk
   * space.
   * @param size the required amount of disk space
   * @param errorMessage error message
   * @throws IOException if there is enough space on the storage for the
   *           required amount of disk space
   */
  public void checkIfEnoughSpace(long size, String errorMessage)
      throws IOException {

    Objects.requireNonNull(errorMessage);

    if (!isEnoughSpace(size)) {
      throw new IOException(errorMessage);
    }
  }

  /**
   * Check if there is enough space on the storage for a required amount of disk
   * space.
   * @param size the required amount of disk space
   * @param errorMessage error message
   * @throws IOException if there is enough space on the storage for the
   *           required amount of disk space
   */
  public void checkIfEnoughSpace(long size) throws IOException {

    checkIfEnoughSpace(size, "Not enough space on data storage: " + this);
  }

  /**
   * Create a data location.
   * @param name name of the data location
   * @return a data location
   */
  public DataLocation newDataLocation(String name) {

    return new DataLocation(this, Paths.get(this.path.toString(), name));
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return "DataStorage [machine="
        + machine + ", path=" + path + ", minimalSpace=" + minimalSpace
        + ", writable=" + writable + "]";
  }

  //
  // Static methods
  //

  /**
   * Deserialize a DataStorage from JSON.
   * @param json JSON string
   * @return a new DataStorage
   */
  public static DataStorage deSerializeFromJson(String json) {

    requireNonNull(json);

    return JSONUtils.newGson().fromJson(json, DataStorage.class);

  }

  /**
   * Serialize the object into a JSON string
   * @return
   */
  public String toJson() {

    return JSONUtils.newGson().toJson(this);
  }

  //
  // Constructors
  //

  /**
   * Private constructor for Gson.
   */
  @SuppressWarnings(value = {"unused"})
  private DataStorage() {
    this.machine = null;
    this.path = null;
  }

  /**
   * Constructor.
   * @param machine machine of the data storage
   * @param path path of the data storage
   */
  public DataStorage(final String machine, final String path) {

    this(machine, Paths.get(path));
  }

  /**
   * Constructor.
   * @param machine machine of the data storage
   * @param path path of the data storage
   */
  public DataStorage(final String machine, final Path path) {

    this.machine = machine;
    this.path = path;

    // TODO Check if path exists
    // TODO Check if directory and readable
    // TODO check if a writable directory -> this.writable
  }

}
