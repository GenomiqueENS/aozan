package fr.ens.biologie.genomique.aozan.aozan3.util;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class contains some system utility methods.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class DiskUtils {

  /**
   * This method returns the disk usage of a directory or a file.
   * @param path the path of the directory or the file
   * @return the disk usage in bytes
   * @throws IOException if an error occurs while getting the disk usage
   */
  public static long du(final Path path) throws IOException {

    Objects.requireNonNull(path);

    if (!Files.exists(path)) {
      throw new FileNotFoundException("Unknown path: " + path);
    }

    ProcessBuilder pb = new ProcessBuilder("du", "-b", "--max-depth=0",
        path.toAbsolutePath().toString());
    pb.environment().put("LANG", "C");

    final Process p = pb.start();

    final InputStream std = p.getInputStream();
    final StringBuilder sb = new StringBuilder();

    try (final BufferedReader stdr =
        new BufferedReader(new InputStreamReader(std))) {

      String l1 = null;

      while ((l1 = stdr.readLine()) != null) {
        sb.append(l1);
        sb.append('\n');
      }
    }

    int exitValue;
    try {
      exitValue = p.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }

    if (exitValue != 0) {
      throw new IOException(
          "Error, du commmand exit value is not 0: " + exitValue);
    }

    return Long.parseLong(sb.toString().split("\t")[0]);
  }

  /**
   * Recursively change the owner of a directory.
   * @param directory the directory
   * @param owner the new owner of the directory
   * @throws IOException if an error occurs while changing the owner of the
   *           directory
   */
  public static void changeDirectoryOwner(Path directory, String owner)
      throws IOException {

    changeDirectoryOwner(directory, owner, null);
  }

  /**
   * Recursively change the owner of a directory.
   * @param directory the directory
   * @param owner the new owner of the directory
   * @param group the new group of the directory
   * @throws IOException if an error occurs while changing the owner of the
   *           directory
   */
  public static void changeDirectoryOwner(Path directory, String user,
      String group) throws IOException {

    requireNonNull(directory);
    requireNonNull(user);

    if (!Files.isDirectory(directory)) {
      throw new IOException("Directory does not exists: " + directory);
    }

    if (user.trim().isEmpty()) {
      throw new IllegalArgumentException("user argument cannot be empty");
    }

    if (group != null && group.trim().isEmpty()) {
      throw new IllegalArgumentException("group argument cannot be empty");
    }

    List<String> commandLine = new ArrayList<>();
    commandLine.add("chown");
    commandLine.add("-R");
    commandLine.add(user.trim() + (group != null ? ':' + group.trim() : ""));
    commandLine.add(directory.toAbsolutePath().toString());

    ProcessBuilder pb = new ProcessBuilder(commandLine);

    try {
      int exitValue = pb.start().waitFor();

      if (exitValue != 0) {
        throw new IOException(
            "Error while performing chown, exit code: " + exitValue);
      }

    } catch (InterruptedException | IOException e) {
      throw new IOException("Error while performing chown", e);
    }
  }

  /**
   * Recursively change the mode of a directory.
   * @param directory the directory
   * @param mode the new mode of the directory and its files and subdirectories
   * @throws IOException if an error occurs while changing the owner of the
   *           directory
   */
  public static void changeDirectoryMode(Path directory, String mode)
      throws IOException {

    requireNonNull(directory);
    requireNonNull(mode);

    if (!Files.isDirectory(directory)) {
      throw new IOException("Directory does not exists: " + directory);
    }

    if (mode.trim().isEmpty()) {
      throw new IllegalArgumentException("mode argument cannot be empty");
    }

    List<String> commandLine = new ArrayList<>();
    commandLine.add("chmod");
    commandLine.add("-R");
    commandLine.add(mode.trim());
    commandLine.add(directory.toAbsolutePath().toString());

    ProcessBuilder pb = new ProcessBuilder(commandLine);

    try {
      int exitValue = pb.start().waitFor();

      if (exitValue != 0) {
        throw new IOException(
            "Error while performing chmod, exit code: " + exitValue);
      }

    } catch (InterruptedException | IOException e) {
      throw new IOException("Error while performing chmod", e);
    }
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private DiskUtils() {
  }

}
