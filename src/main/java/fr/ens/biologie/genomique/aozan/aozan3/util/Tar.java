package fr.ens.biologie.genomique.aozan.aozan3.util;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.util.UnTar.Compression;

/**
 * This class allow to create tar files.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class Tar {

  private Path inputDirectoryPath;
  private Path outputArchive;
  private Compression compression = Compression.AUTO;
  private List<String> exclude = new ArrayList<>();
  private List<String> include = new ArrayList<>();

  /**
   * Get tar compression.
   * @return the tar compression
   */
  public Compression getCompression() {
    return compression;
  }

  /**
   * Set the tar compression
   * @param compression the tar compression
   */
  public void setCompression(Compression compression) {

    requireNonNull(compression);

    this.compression = compression;
  }

  /**
   * Add a pattern to exclude.
   * @param pattern pattern to exclude
   */
  public void addExcludePattern(String pattern) {

    requireNonNull(pattern);

    if (pattern.trim().isEmpty()) {
      throw new IllegalArgumentException("pattern argument is empty");
    }

    this.exclude.add(pattern);
  }

  /**
   * Get the list of patterns to exclude.
   * @return the list of patterns to exclude
   */
  public List<String> getExcludePatterns() {

    return Collections.unmodifiableList(this.exclude);
  }

  /**
   * Add a pattern to exclude.
   * @param pattern pattern to exclude
   */
  public void addIncludePattern(String pattern) {

    requireNonNull(pattern);

    if (pattern.trim().isEmpty()) {
      throw new IllegalArgumentException("pattern argument is empty");
    }

    this.include.add(pattern);
  }

  /**
   * Get the list of patterns to include.
   * @return the list of patterns to include
   */
  public List<String> getIncludePatterns() {

    return Collections.unmodifiableList(this.include);
  }

  /**
   * Execute tar.
   * @throws Aozan3Exception if an error occurs while untarring data
   */
  public void execute() throws Aozan3Exception {

    List<String> commandLine = new ArrayList<>();
    commandLine.add("tar");

    for (String pattern : this.exclude) {
      commandLine.add("--exclude=" + pattern);
    }

    if (!this.compression.getArgument().isEmpty()) {
      commandLine.add(this.compression.getArgument());
    }

    commandLine.add("-cf");
    commandLine.add(this.outputArchive.toString());

    if (this.include.isEmpty()) {
      commandLine.add(this.inputDirectoryPath.getFileName().toString());
    } else {
      for (String p : this.include) {
        commandLine.add(this.inputDirectoryPath.getFileName().toString()
            + File.separator + p);
      }
    }

    ProcessBuilder pb = new ProcessBuilder(commandLine);
    pb.directory(this.inputDirectoryPath.getParent().toFile());
    pb.environment().put("LANG", "C");
    pb.inheritIO();

    try {
      int exitValue = pb.start().waitFor();

      if (exitValue != 0) {
        throw new Aozan3Exception(
            "Error while performing untar, exit code: " + exitValue);
      }

    } catch (InterruptedException | IOException e) {
      throw new Aozan3Exception("Error while performing untar", e);
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param inputDirectoryPath input tar file
   * @param outputArchive output directory for tar file content
   * @throws Aozan3Exception if input archive or output directory path are not
   *           found or not readable/writable
   */
  public Tar(Path inputDirectoryPath, Path outputArchive)
      throws Aozan3Exception {

    requireNonNull(inputDirectoryPath);
    requireNonNull(outputArchive);

    if (!Files.isDirectory(inputDirectoryPath)) {
      throw new Aozan3Exception(
          "Input directory not found: " + inputDirectoryPath);
    }

    if (!Files.isReadable(inputDirectoryPath)) {
      throw new Aozan3Exception(
          "Input directory is not readable: " + inputDirectoryPath);
    }

    if (Files.exists(outputArchive)) {
      throw new Aozan3Exception(
          "Output tar file already exists: " + outputArchive);
    }

    if (!Files.isWritable(outputArchive.getParent())) {
      throw new Aozan3Exception(
          "Output directory for tar content is not writable: " + outputArchive);
    }

    this.inputDirectoryPath = inputDirectoryPath;
    this.outputArchive = outputArchive;
  }

}
