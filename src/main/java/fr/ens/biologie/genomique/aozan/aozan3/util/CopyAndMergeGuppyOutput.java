package fr.ens.biologie.genomique.aozan.aozan3.util;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Merge FASTQ files from a directory in another directory.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class CopyAndMergeGuppyOutput {

  // TODO This class must works if input and output path are the same (in place
  // mode)

  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  private Path inputPath;
  private Path outputPath;
  private boolean mergeFastq = false;
  private boolean mergeLog = false;
  private boolean compressLogs = false;
  private boolean compressTelemetry = false;
  private boolean compressSequencingSummary = false;
  private boolean initialized = false;

  private static class PathTimeComparator implements Comparator<Path> {

    @Override
    public int compare(Path o1, Path o2) {

      return Long.compare(o1.toFile().lastModified(),
          o2.toFile().lastModified());
    }
  }

  //
  // Getters
  //

  /**
   * Test if FASTQ merging is enabled.
   * @return true if FASTQ merging is enabled
   */
  public boolean isFastqMergin() {

    if (this.initialized) {
      throw new IllegalStateException();
    }

    return mergeFastq;
  }

  /**
   * Test if logs merging is enabled.
   * @return true if logs merging is enabled
   */
  public boolean isLogMerging() {
    return mergeLog;
  }

  /**
   * Test if log compression is enabled.
   * @return true if log compression is enabled
   */
  public boolean isCompressLogs() {
    return compressLogs;
  }

  /**
   * Test if log telemetry file compression is enabled.
   * @return true if telemetry file compression is enabled
   */
  public boolean isCompressSequencingTelemetry() {
    return compressTelemetry;
  }

  /**
   * Test if log sequencing summary file compression is enabled.
   * @return true if sequencing summary file compression is enabled
   */
  public boolean isCompressSequencingSummary() {
    return compressSequencingSummary;
  }

  //
  // Setters
  //

  /**
   * Enable the FASTQ files merging.
   * @param mergeFastq true to merge FASTQ file
   */
  public void setFastqMerging(boolean mergeFastq) {

    checkInitialization();
    this.mergeFastq = mergeFastq;
  }

  /**
   * Enable the log files merging.
   * @param mergeLog true to merge log files
   */
  public void setLogMerging(boolean mergeLog) {

    checkInitialization();
    this.mergeLog = mergeLog;
  }

  /**
   * Enable log files compression.
   * @param compressLogs true to compress log files
   */
  public void setCompressLogs(boolean compressLogs) {

    checkInitialization();
    this.compressLogs = compressLogs;
  }

  /**
   * Enable telemetry file compression.
   * @param compressTelemetry true to compress telemetry file
   */
  public void setCompressTelemetry(boolean compressTelemetry) {

    checkInitialization();
    this.compressTelemetry = compressTelemetry;
  }

  /**
   * Enable sequencing summary file compression.
   * @param compressSequencingSummary true to compress sequencing summary file
   */
  public void setCompressSequencingSummary(boolean compressSequencingSummary) {

    checkInitialization();
    this.compressSequencingSummary = compressSequencingSummary;
  }

  //
  // Execution methods
  //

  private void checkInitialization() {

    if (this.initialized) {
      throw new IllegalStateException();
    }

  }

  /**
   * Execute the copy and merge FASTQ files.
   * @throws IOException if an error occurs while copying and merging files
   */
  public void execute() throws IOException {

    checkInitialization();

    this.initialized = true;

    // Create output directory
    Files.createDirectory(this.outputPath);

    Multimap<Path, Path> fastqFiles =
        this.mergeFastq ? ArrayListMultimap.create() : null;
    List<Path> logFiles = this.mergeLog ? new ArrayList<>() : null;
    Set<String> filesToGzip = new HashSet<>();

    if (this.compressTelemetry) {
      filesToGzip.add("sequencing_telemetry.js");
    }

    if (this.compressSequencingSummary) {
      filesToGzip.add("sequencing_summary.txt");
    }

    // Copy files
    try (Stream<Path> stream = Files.walk(this.inputPath)) {
      stream.forEach(source -> copy(source,
          this.outputPath.resolve(this.inputPath.relativize(source)),
          fastqFiles, logFiles, filesToGzip));
    }

    // For each FASTQ file
    for (Path subDir : fastqFiles.keySet()) {

      boolean gzip = false;
      List<Path> inputFiles = new ArrayList<>(fastqFiles.get(subDir));
      inputFiles.sort(new PathTimeComparator());

      // Test if files are gzipped
      if (inputFiles.get(0).toFile().getName().endsWith(".gz")) {
        gzip = true;
      }

      Path outputDir =
          this.outputPath.resolve(this.inputPath.relativize(subDir));
      Path outputFile =
          Paths.get(outputDir.toString() + (gzip ? ".fastq.gz" : ".fastq"));

      concatenateFASTQ(outputFile, inputFiles, gzip);

      // Remove empty directory
      Files.delete(outputDir);
    }

    concatenateLogs(
        Paths.get(this.outputPath.toString(),
            "guppy_basecaller.log" + (this.compressLogs ? ".gz" : "")),
        logFiles, this.compressLogs);
  }

  /**
   * Copy non FASTQ files and populate map with FASTQ files.
   * @param source file to copy
   * @param dest destination of the copy
   * @param fastqFiles map with FASTQ files
   */
  private static void copy(Path source, Path dest,
      Multimap<Path, Path> fastqFiles, List<Path> logFiles,
      Set<String> filesToGzip) {

    String filename = source.toFile().getName();

    if (fastqFiles != null
        && (filename.endsWith(".fastq") || filename.endsWith(".fastq.gz"))) {
      fastqFiles.put(source.getParent(), source);
      return;
    }

    if (logFiles != null
        && filename.startsWith("guppy_basecaller_log-")
        && filename.endsWith(".log")) {
      logFiles.add(source);
      return;
    }

    try {

      if (filesToGzip.contains(filename)) {
        compress(source, dest);
      } else {
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES);
      }

    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Concatenate FASTQ files
   * @param outputFile output file
   * @param inputFiles input
   * @param gzip true if files are gzip files
   * @throws IOException if an error occurs while concatenate FASTQ files
   */
  private static void concatenateFASTQ(Path outputFile, List<Path> inputFiles,
      boolean gzip) throws IOException {

    // Concatenation disabled, nothing to do
    if (inputFiles == null) {
      return;
    }

    try (OutputStream os = new FileOutputStream(outputFile.toFile())) {

      OutputStream out = gzip ? new GZIPOutputStream(os) : os;

      for (Path file : inputFiles) {

        try (InputStream in = new FileInputStream(file.toFile())) {

          if (gzip) {
            copy(new GZIPInputStream(in), out);
            in.close();
          } else {
            copy(in, out);
          }
        }
      }

      if (gzip) {
        out.close();
      }
    }
  }

  private static void concatenateLogs(Path outputFile, List<Path> inputFiles,
      boolean gzip) throws IOException {

    // Concatenation disabled, nothing to do
    if (inputFiles == null) {
      return;
    }

    try (OutputStream os = new FileOutputStream(outputFile.toFile())) {

      OutputStream out = gzip ? new GZIPOutputStream(os) : os;

      for (Path file : inputFiles) {

        try (InputStream in = new FileInputStream(file.toFile())) {
          copy(in, out);
        }
      }

      if (gzip) {
        out.close();
      }
    }
  }

  /**
   * Copy a stream in another stream.
   * @param input input stream
   * @param output output stream
   * @throws IOException if an error occurs while copying stream
   */
  private static void copy(final InputStream input, final OutputStream output)
      throws IOException {

    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
    }
  }

  private static void compress(Path source, Path dest) throws IOException {

    File inputFile = source.toFile();
    File outputFile = new File(dest.toString() + ".gz");

    try (InputStream in = new FileInputStream(inputFile);
        OutputStream out =
            new GZIPOutputStream(new FileOutputStream(outputFile))) {
      copy(in, out);
    }

    outputFile.setLastModified(inputFile.lastModified());
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param inputPath input path to copy
   * @param outputPath output path to copy
   * @throws IOException if path are not valid
   */
  public CopyAndMergeGuppyOutput(Path inputPath, Path outputPath)
      throws IOException {

    requireNonNull(inputPath);
    requireNonNull(outputPath);

    if (!Files.isDirectory(inputPath)) {
      throw new IOException(
          "Path to copy does not exists or is not a directory: " + inputPath);
    }

    if (Files.exists(outputPath)) {
      throw new IOException(
          "Destination path of copy already exists: " + outputPath);
    }

    this.inputPath = inputPath;
    this.outputPath = outputPath;
  }

}
