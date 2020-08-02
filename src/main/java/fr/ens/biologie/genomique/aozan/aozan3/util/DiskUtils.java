package fr.ens.biologie.genomique.aozan.aozan3.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
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

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private DiskUtils() {
  }

}
