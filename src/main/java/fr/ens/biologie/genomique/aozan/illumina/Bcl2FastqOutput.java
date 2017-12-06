package fr.ens.biologie.genomique.aozan.illumina;

import static fr.ens.biologie.genomique.eoulsan.util.FileUtils.checkExistingDirectoryFile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.eoulsan.core.Version;

/**
 * This class define the output of bcl2fastq.
 * @author Sandrine Perrin
 * @author Laurent Jourdren
 * @since 2.0
 */
public class Bcl2FastqOutput {

  /** The Bcl2fastq version. */
  private final Bcl2FastqVersion version;

  /** The Bcl2fastq full version. */
  private final String fullVersion;

  /** The fastq directory. */
  private final File fastqDirectory;

  /** The samplesheet instance. */
  private final SampleSheet samplesheet;

  //
  // Enum
  //

  /**
   * This enum defined the versions of bcl2fastq.
   * @author Laurent Jourdren
   */
  public enum Bcl2FastqVersion {
    BCL2FASTQ_1, BCL2FASTQ_2, BCL2FASTQ_2_15;

    public static Bcl2FastqVersion parseVersion(final String version) {

      if (version == null) {
        throw new NullPointerException("The version argument cannot be null");
      }

      final Version v = new Version(version);

      switch (v.getMajor()) {

      case 1:
        return BCL2FASTQ_1;

      case 2:
        return v.getMinor() == 15 || v.getMinor() == 16
            ? BCL2FASTQ_2_15 : BCL2FASTQ_2;

      default:
        throw new AozanRuntimeException(
            "Unknown bcl2fast major version: " + v.getMajor());

      }
    }
  }

  //
  // Getters
  //

  /**
   * Get the bcl2fastq version used to generate the FASTQ files.
   * @return the bcl2fastq version used to generate the FASTQ files
   */
  public Bcl2FastqVersion getVersion() {

    return this.version;
  }

  /**
   * Get the full bcl2fastq version used to generate the FASTQ files.
   * @return the full bcl2fastq version used to generate the FASTQ files
   */
  public String getFullVersion() {

    return this.fullVersion;
  }

  /**
   * Get the bcl2fastq output directory of the run.
   * @return the bcl2fastq output directory
   */
  public File getFastqDirectory() {

    return this.fastqDirectory;
  }

  /**
   * Get the sample sheet.
   * @return the sample sheet
   */
  public SampleSheet getSampleSheet() {

    return this.samplesheet;
  }

  //
  // Bcl2fastq version discovering methods
  //

  /**
   * Find the version of bcl2fastq used to create FASTQ files.
   * @param fastqDir the bcl2fastq output directory
   * @return the bcl2fastq version
   * @throws IOException
   */
  private static Bcl2FastqVersion findBcl2FastqVersion(final File fastqDir)
      throws IOException {

    // Check if bcl2fastq 1 files exists
    for (String filename : Arrays.asList("DemultiplexedBustardConfig.xml",
        "DemultiplexedBustardSummary.xml", "SampleSheet.mk", "support.txt",
        "DemultiplexConfig.xml", "Makefile", "make.err", "make.out", "Temp")) {

      if (new File(fastqDir, filename).exists()) {
        return Bcl2FastqVersion.BCL2FASTQ_1;
      }
    }

    // Check if bcl2fastq 2 files exists
    for (String filename : Arrays.asList("DemultiplexingStats.xml",
        "ConversionStats.xml")) {

      if (new File(new File(fastqDir, "Stats"), filename).exists()) {
        return Bcl2FastqVersion.BCL2FASTQ_2;
      }
    }

    // Check if the output directory is a bcl2fastq output
    if (!new File(fastqDir, "InterOp").isDirectory()) {
      throw new IOException("Unknown Bcl2fastq output directory tree");
    }

    // Find the bcl2fastq version in the log file
    final Bcl2FastqVersion versionInLogFile = Bcl2FastqVersion.parseVersion(
        extractBcl2FastqVersionFromLog(fastqDir));
    if (versionInLogFile != null) {
      return versionInLogFile;
    }

    // Find the bcl2fastq version from the number of the occurrences of the
    // underscore character in FASTQ filenames
    final int count = countUnderscoreInFastqFilename(fastqDir, 0);

    if (count >= 4) {
      return Bcl2FastqVersion.BCL2FASTQ_2;
    }

    throw new IOException("Unknown Bcl2fastq output directory tree");
  }

  /**
   * Read the bcl2Fastq version used to create FASTQ files in the bcl2fastq2 log
   * file.
   * @param fastqDir the bcl2fastq output directory
   * @return the bcl2fastq version or null if cannot be read in the log file
   * @throws IOException if more than one log file was found
   */
  private static String extractBcl2FastqVersionFromLog(
      final File fastqDir) throws IOException {

    // Find log file
    final File[] logFiles = fastqDir.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {

        final String filename = pathname.getName();

        return filename.startsWith("bcl2fastq_output_")
            && (filename.endsWith(".out") || filename.endsWith(".err"))
            && pathname.length() > 0;

      }
    });

    // If no log found,
    if (logFiles == null || logFiles.length == 0) {

      return null;
    }

    if (logFiles.length > 1) {
      throw new IOException(
          "Found more than one bcl1fastq2 log file in " + fastqDir);
    }

    // Parse log file
    for (String line : Files.readAllLines(logFiles[0].toPath(),
        StandardCharsets.UTF_8)) {

      line = line.toLowerCase().trim();

      if (line.startsWith("bcl2fastq v")) {

        final List<String> fields =
            Lists.newArrayList(Splitter.on('v').split(line));

        if (fields.size() != 2) {
          continue;
        }

        return fields.get(1);
      }
    }

    return null;
  }

  /**
   * Count the number of underscore character in the fastq filenames.
   * @param fastqDir the bcl2fastq output directory
   * @param max last maximum of occurrences of underscore
   * @return the number of underscore character in the fastq filenames
   */
  private static int countUnderscoreInFastqFilename(final File fastqDir,
      final int max) {

    int newMax = max;

    for (File f : fastqDir.listFiles()) {

      if (f.isFile()) {

        final String filename = f.getName();

        if (filename.contains(".fastq")) {

          int count = 0;
          for (int i = 0; i < filename.length(); i++) {
            if (filename.charAt(i) == '_') {
              count++;
            }
          }

          if (count > newMax) {
            newMax = count;
          }
        }
      }

      if (f.isDirectory()) {

        final int count = countUnderscoreInFastqFilename(f, newMax);

        if (count > newMax) {
          newMax = count;
        }
      }
    }

    return newMax;
  }

  /**
   * Read the samplesheet from a file.
   * @param samplesheetFile the samplesheet file
   * @return a SampleSheet object
   * @throws IOException if an error occurs while reading the samplesheet
   */
  private static SampleSheet readSampleSheet(final File samplesheetFile)
      throws IOException {

    return new SampleSheetCSVReader(samplesheetFile).read();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param samplesheetFile the samplesheet file
   * @param fastqDir the output directory of bcl2fastq
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public Bcl2FastqOutput(final File samplesheetFile, final File fastqDir)
      throws IOException {

    this(readSampleSheet(samplesheetFile), fastqDir);
  }

  /**
   * Constructor.
   * @param samplesheet the samplesheet
   * @param fastqDir the output directory of bcl2fastq
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public Bcl2FastqOutput(final SampleSheet samplesheet, final File fastqDir)
      throws IOException {

    this(samplesheet, fastqDir, findBcl2FastqVersion(fastqDir),
        extractBcl2FastqVersionFromLog(fastqDir), true);
  }

  /**
   * Constructor.
   * @param samplesheet the samplesheet
   * @param fastqDir the output directory of bcl2fastq
   * @param version of bcl2fastq
   * @param checkFastqDirectory check if the FASTQ directory exists
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public Bcl2FastqOutput(final SampleSheet samplesheet, final File fastqDir,
      Bcl2FastqVersion version, String fullVersion, boolean checkFastqDirectory) throws IOException {

    if (samplesheet == null) {
      throw new NullPointerException("samplesheet argument cannot be null");
    }

    if (fastqDir == null) {
      throw new NullPointerException("fastqDir argument cannot be null");
    }

    if (checkFastqDirectory) {
      checkExistingDirectoryFile(fastqDir, "fastq directory");
    }

    this.fastqDirectory = fastqDir;
    this.version = version;
    this.samplesheet = samplesheet;
    this.fullVersion = fullVersion;
  }

}
