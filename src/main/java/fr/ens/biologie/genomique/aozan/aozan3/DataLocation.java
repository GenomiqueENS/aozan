package fr.ens.biologie.genomique.aozan.aozan3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import fr.ens.biologie.genomique.aozan.aozan3.util.DiskUtils;

/**
 * This class define a data location
 * @author Laurent Jourdren
 * @since 3.0
 */
public class DataLocation {

  /**
   * Format of the data location
   */
  public enum Format {
    DIRECTORY, FILE, TAR, TAR_GZ, TAR_BZ2, ZIP
  }

  private final DataStorage storage;
  private final Path path;
  private final boolean exists;
  private final Format format = Format.DIRECTORY;

  //
  // Getters
  //

  /**
   * Test if the data location exists.
   * @return true if the data location exists
   */
  public boolean exists() {

    return this.exists;
  }

  /**
   * Get the storage of the data location.
   * @return the data storage
   */
  public DataStorage getStorage() {

    return this.storage;
  }

  /**
   * Get the path of the data.
   * @return the path of the data
   */
  public Path getPath() {

    return this.path;
  }

  /**
   * Get the format of the data.
   * @return the format of the data
   */
  public Format getFormat() {

    return this.format;
  }

  //
  // Check methods
  //

  /**
   * Test if the data location is a directory.
   * @return true if the data location is a directory
   */
  public boolean isDirectory() {

    return Files.isDirectory(this.path);
  }

  /**
   * Check if the data location is a directory.
   * @param errorMessage error message to set in the exception message
   * @throws IOException if the data location is not a directory
   */
  public void checkIfDirectory(String errorMessage) throws IOException {

    Objects.requireNonNull(errorMessage);

    if (!isDirectory()) {
      throw new IOException(
          errorMessage + ": " + this.path + " on " + this.storage.getMachine());
    }
  }

  /**
   * Check if the data location is a directory.
   * @throws IOException if the data location is not a directory
   */

  public void checkIfDirectory() throws IOException {

    checkIfDirectory("The location is not a directory");
  }

  /**
   * Test if the data location is a regular file.
   * @return true if the data location is a file
   */
  public boolean isFile() {

    return Files.isRegularFile(this.path);
  }

  /**
   * Check if the data location is a directory.
   * @param errorMessage error message to set in the exception message
   * @throws IOException if the data location is not a directory
   */
  public void checkIfFile(String errorMessage) throws IOException {

    Objects.requireNonNull(errorMessage);

    if (!isFile()) {
      throw new IOException(
          errorMessage + ": " + this.path + " on " + this.storage.getMachine());
    }
  }

  /**
   * Check if the data location is a directory.
   * @throws IOException if the data location is not a directory
   */
  public void checkIfFile() throws IOException {

    checkIfDirectory("The location is not a file");
  }

  /**
   * Test if the data location exists.
   * @return true if the data location exists
   */
  public boolean exist() {

    return Files.exists(this.path);
  }

  /**
   * Check if the data location exists.
   * @param errorMessage error message to set in the exception message
   * @throws IOException if the data location exists
   */
  public void checkIfExists(String errorMessage) throws IOException {

    Objects.requireNonNull(errorMessage);

    if (!exist()) {
      throw new IOException(
          errorMessage + ": " + this.path + " on " + this.storage.getMachine());
    }
  }

  /**
   * Check if the data location exists.
   * @throws IOException if the data location exists
   */
  public void checkIfExists() throws IOException {

    checkIfExists("The location does not exists");
  }

  /**
   * Check if the data location not exists.
   * @param errorMessage error message to set in the exception message
   * @throws IOException if the data location not exists
   */
  public void checkIfNotExists(String errorMessage) throws IOException {

    Objects.requireNonNull(errorMessage);

    if (exist()) {
      throw new IOException(
          errorMessage + ": " + this.path + " on " + this.storage.getMachine());
    }
  }

  /**
   * Check if the data location not exists.
   * @throws IOException if the data location not exists
   */
  public void checkIfNotExists() throws IOException {

    checkIfExists("The location exists");
  }

  /**
   * Test if the data location is readable.
   * @return true if the data location is readable
   */
  public boolean isReadable() {

    return Files.isReadable(this.path);
  }

  /**
   * Check if the data location is readable.
   * @param errorMessage error message to set in the exception message
   * @throws IOException if the data location is readable
   */
  public void checkIfReadable(String errorMessage) throws IOException {

    Objects.requireNonNull(errorMessage);

    if (!isReadable()) {
      throw new IOException(
          errorMessage + ": " + this.path + " on " + this.storage.getMachine());
    }
  }

  /**
   * Check if the data location is readable.
   * @throws IOException if the data location is readable
   */
  public void checkIfReadable() throws IOException {

    checkIfExists("The location is not readable");
  }

  /**
   * Test if the data location is writable.
   * @return true if the data location is writable
   */
  public boolean isWriteable() {

    return Files.isWritable(this.path);
  }

  /**
   * Check if the data location is writable.
   * @param errorMessage error message to set in the exception message
   * @throws IOException if the data location is writable
   */
  public void checkIfWritable(String errorMessage) throws IOException {

    Objects.requireNonNull(errorMessage);

    if (!isReadable()) {
      throw new IOException(
          errorMessage + ": " + this.path + " on " + this.storage.getMachine());
    }
  }

  /**
   * Check if the data location is writable.
   * @throws IOException if the data location is writable
   */
  public void checkIfWritable() throws IOException {

    checkIfExists("The location is not writable");
  }

  /**
   * Check if the data location is a readable directory.
   * @param directoryType message in the exception
   * @throws IOException if the data location is not is a readable directory
   */
  public void checkReadableDirectory(String directoryType) throws IOException {

    checkIfExists("The " + directoryType + " directory does not exists");
    checkIfDirectory("The " + directoryType + " is not a directory");
    checkIfReadable("The " + directoryType + " is not readable");
  }

  /**
   * Check if the data location is a writable directory.
   * @param directoryType message in the exception
   * @throws IOException if the data location is not is a writable directory
   */
  public void checkWritableDirectory(String directoryType) throws IOException {

    checkIfExists("The " + directoryType + " directory does not exists");
    checkIfDirectory("The " + directoryType + " is not a directory");
    checkIfWritable("The " + directoryType + " is not writable");
  }

  /**
   * Check if the data location is a readable file.
   * @param directoryType message in the exception
   * @throws IOException if the data location is not is a readable file
   */
  public void checkReadableFile(String fileType) throws IOException {

    checkIfExists("The " + fileType + " file does not exists");
    checkIfFile("The " + fileType + " is not a regular file");
    checkIfReadable("The " + fileType + " is not readable");
  }

  /**
   * Check if the data location is a writable file.
   * @param directoryType message in the exception
   * @throws IOException if the data location is not is a writable file
   */
  public void checkWritableFile(String fileType) throws IOException {

    checkIfExists("The " + fileType + " file does not exists");
    checkIfFile("The " + fileType + " is not a regular file");
    checkIfWritable("The " + fileType + " is not writable");
  }

  //
  // Storage info
  //

  /**
   * Get the disk usage of the data location.
   * @return the disk usage in bytes
   * @throws IOException
   */
  public long getDiskUsage() throws IOException {

    checkIfExists();

    return DiskUtils.du(this.path);
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return "DataLocation [exists="
        + this.exists + ", storage=" + this.storage + ", path=" + this.path
        + ", format=" + this.format + "]";
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param storage storage of the data location
   * @param path path of the data location
   */
  public DataLocation(final DataStorage storage, final Path path) {

    Objects.requireNonNull(storage);
    Objects.requireNonNull(path);

    this.storage = storage;
    this.path = path;
    this.exists = true;
  }

}
