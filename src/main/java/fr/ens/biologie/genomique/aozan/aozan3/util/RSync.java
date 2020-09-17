package fr.ens.biologie.genomique.aozan.aozan3.util;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * This class define a synchronization tool based on the rsync command.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RSync {

  private Path inputPath;
  private Path outputPath;

  private int minimumAgeOfFiles;
  private Collection<String> excludePatterns = new HashSet<String>();
  private List<String> arguments =
      new ArrayList<>(Arrays.asList("-a", "--no-owner", "--no-group"));

  /**
   * Synchronize.
   * @throws IOException if an error occurs while executing rsync
   */
  public void sync() throws IOException {

    // Check if input path exists
    if (!Files.exists(this.inputPath)) {
      throw new IOException(
          "Input path for RSync does not exists: " + this.inputPath);
    }

    // Create ouput directory if not exists
    if (Files.isDirectory(this.inputPath) && Files.exists(this.outputPath)) {
      Files.createDirectories(outputPath);
    }

    Path manifestPath = null;

    // Test if a manifest file is required
    if (minimumAgeOfFiles > 0 || !excludePatterns.isEmpty()) {

      manifestPath = Files.createTempFile("rsync-", ".list");
      find(this.inputPath, this.excludePatterns, this.minimumAgeOfFiles,
          manifestPath);
    }

    // Define arguments
    List<String> args = new ArrayList<>();
    args.add("/usr/bin/rsync");
    args.addAll(this.arguments);

    // Use manifest file ?
    if (manifestPath != null) {
      args.add("--files-from=" + manifestPath);
    }

    // Define input and output path in command line
    args.add(this.inputPath.toString()
        + (Files.isDirectory(this.inputPath) ? "/" : ""));
    args.add(this.outputPath.toString());

    // Create process
    ProcessBuilder pb = new ProcessBuilder(args);
    pb.environment().put("LANG", "C");

    int exitValue;
    try {
      exitValue = pb.start().waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }

    if (exitValue != 0) {
      throw new IOException(
          "Error, rsync commmand exit value is not 0: " + exitValue);
    }

    // Remove manifest file if needed
    if (manifestPath != null) {
      Files.delete(manifestPath);
    }

  }

  private static void find(final Path path, Collection<String> excludePatterns,
      final int minimumAgeOfFiles, final Path rsyncManifestPath)
      throws IOException {

    Objects.requireNonNull(path);
    Objects.requireNonNull(excludePatterns);

    if (!Files.exists(path)) {
      throw new FileNotFoundException("Unknown path: " + path);
    }

    List<String> arguments =
        new ArrayList<>(Arrays.asList("find", ".", "-type", "f"));

    if (minimumAgeOfFiles > 0) {
      arguments.add("-mmin");
      arguments.add("" + minimumAgeOfFiles);
    }

    for (String p : excludePatterns) {
      arguments.add("-not");
      arguments.add("-name");
      arguments.add(p);
    }

    ProcessBuilder pb = new ProcessBuilder(arguments);
    pb.directory(path.toFile());
    pb.environment().put("LANG", "C");

    final Process p = pb.start();

    final InputStream std = p.getInputStream();

    try (
        final BufferedReader stdr =
            new BufferedReader(new InputStreamReader(std));
        final Writer writer = Files.newBufferedWriter(rsyncManifestPath)) {

      String l1 = null;

      while ((l1 = stdr.readLine()) != null) {
        writer.write(l1 + '\n');
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
          "Error, find commmand exit value is not 0: " + exitValue);
    }

  }

  //
  // Constructor
  //

  public RSync(final Path inputPath, final Path outputPath) {

    this(inputPath, outputPath, 0, Collections.<String> emptySet());
  }

  public RSync(final Path inputPath, final Path outputPath,
      int minimumAgeOfFiles) {

    this(inputPath, outputPath, minimumAgeOfFiles,
        Collections.<String> emptySet());
  }

  public RSync(final Path inputPath, final Path outputPath,
      Collection<String> excludePatterns) {

    this(inputPath, outputPath, 0, excludePatterns);
  }

  /**
   * Constructor.
   * @param inputPath input path
   * @param outputPath output path
   * @param minimumAgeOfFiles minimum age of the file in minutes
   * @param excludePatterns file patterns to exclude
   */
  public RSync(final Path inputPath, final Path outputPath,
      int minimumAgeOfFiles, Collection<String> excludePatterns) {

    requireNonNull(inputPath);
    requireNonNull(outputPath);
    requireNonNull(excludePatterns);

    this.inputPath = inputPath;
    this.outputPath = outputPath;
    this.minimumAgeOfFiles = minimumAgeOfFiles;
    this.excludePatterns = new HashSet<String>(excludePatterns);
  }

}
