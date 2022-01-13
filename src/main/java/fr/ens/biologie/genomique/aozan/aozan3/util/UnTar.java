package fr.ens.biologie.genomique.aozan.aozan3.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;

/**
 * This class allow to create untar archives.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class UnTar {

  public enum Compression {
    NONE(""), AUTO("--auto-compress"), GZIP("--gzip"), BZIP2("--bzip2"),
    XZ("--xz");

    private String argument;

    String getArgument() {
      return this.argument;
    }

    Compression(String argument) {
      this.argument = argument;
    }

  };

  private Path inputArchive;
  private Path outputPath;
  private Compression compression = Compression.AUTO;

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
   * Execute untar.
   * @throws Aozan3Exception if an error occurs while untarring data
   */
  public void execute() throws Aozan3Exception {

    List<String> commandLine = new ArrayList<>();
    commandLine.add("tar");
    commandLine.add("-xf");
    commandLine.add(this.inputArchive.toString());

    if (!this.compression.getArgument().isEmpty()) {
      commandLine.add(this.compression.getArgument());
    }

    commandLine.add("--directory=" + this.outputPath.toString());

    ProcessBuilder pb = new ProcessBuilder(commandLine);
    pb.directory(this.outputPath.toFile());
    pb.environment().put("LANG", "C");

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
   * @param inputArchive input tar file
   * @param outputDirectoryPath output directory for tar file content
   * @throws Aozan3Exception if input archive or output directory path are not
   *           found or not readable/writable
   */
  public UnTar(Path inputArchive, Path outputDirectoryPath)
      throws Aozan3Exception {

    requireNonNull(inputArchive);
    requireNonNull(outputDirectoryPath);

    if (!Files.isRegularFile(inputArchive)) {
      throw new Aozan3Exception("Input tar file not found: " + inputArchive);
    }

    if (!Files.isReadable(inputArchive)) {
      throw new Aozan3Exception(
          "Input tar file is not readable: " + inputArchive);
    }

    if (!Files.isDirectory(outputDirectoryPath)) {
      throw new Aozan3Exception(
          "Output directory for tar content not found: " + outputDirectoryPath);
    }

    if (!Files.isWritable(outputDirectoryPath)) {
      throw new Aozan3Exception(
          "Output directory for tar content is not writable: "
              + outputDirectoryPath);
    }

    this.inputArchive = inputArchive;
    this.outputPath = outputDirectoryPath;
  }

}
