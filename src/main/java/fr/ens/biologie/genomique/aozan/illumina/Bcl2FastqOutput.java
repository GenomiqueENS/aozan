package fr.ens.biologie.genomique.aozan.illumina;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.util.FileUtils.checkExistingDirectoryFile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.TreeMultimap;

import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.aozan.io.FastqSample;
import fr.ens.biologie.genomique.eoulsan.core.Version;

/**
 * This class define the output of bcl2fastq.
 * @author Sandrine Perrin
 * @author Laurent Jourdren
 * @since 2.0
 */
public class Bcl2FastqOutput {

  /** The Constant FASTQ_EXTENSION. */
  public static final String FASTQ_EXTENSION = ".fastq";

  /** The Constant UNDETERMINED_DIR_NAME. */
  public static final String UNDETERMINED_DIR_NAME = "Undetermined_indices";

  /** The Constant PROJECT_PREFIX. */
  public static final String PROJECT_PREFIX = "Project_";

  /** The Constant SAMPLE_PREFIX. */
  public static final String SAMPLE_PREFIX = "Sample_";

  /** The Constant UNDETERMINED_PREFIX. */
  public static final String UNDETERMINED_PREFIX = SAMPLE_PREFIX + "lane";

  private final Bcl2FastqVersion version;

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
  // Other methods
  //

  /**
   * Get the bcl2fastq version used to generate the FASTQ files.
   * @return the bcl2fastq version used to generate the FASTQ files
   */
  public Bcl2FastqVersion getVersion() {

    return this.version;
  }

  /**
   * Get the bcl2fastq output directory of the run.
   * @return the bcl2fastq output directory
   */
  public File getFastqDirectory() {

    return fastqDirectory;
  }

  /**
   * Find fastq parent directory.
   * @param fastqSample the fast sample
   * @return the fastq output directory.
   */
  public File getFastqSampleParentDir(final FastqSample fastqSample) {

    if (fastqSample == null) {
      throw new NullPointerException("fastqSample argument cannot be null");
    }

    switch (this.version) {

    case BCL2FASTQ_1:

      if (fastqSample.isUndeterminedIndex()) {

        final File undeterminedDir =
            new File(getFastqDirectory(), UNDETERMINED_DIR_NAME);

        return new File(undeterminedDir,
            UNDETERMINED_PREFIX + fastqSample.getLane());
      }

      final File projectDir = new File(getFastqDirectory(),
          PROJECT_PREFIX + fastqSample.getProjectName());

      return new File(projectDir, SAMPLE_PREFIX + fastqSample.getSampleName());

    case BCL2FASTQ_2:
    case BCL2FASTQ_2_15:

      if (fastqSample.isUndeterminedIndex()) {
        return getFastqDirectory();
      }

      final File projectV2Dir =
          new File(getFastqDirectory(), fastqSample.getProjectName());

      // Check if there is a sub directory for each sample
      if ("".equals(fastqSample.getSampleDirectoryName())) {

        // No sample sub directory
        return projectV2Dir;
      } else {

        // Sample sub directory
        return new File(projectV2Dir, fastqSample.getSampleDirectoryName());
      }

    default:
      throw new IllegalStateException(
          "Unhandled Bcl2FastqVersion enum value: " + this.version);
    }

  }

  /**
   * Set the prefix of the fastq file of read for a fastq on a sample.
   * @return prefix fastq files for this fastq on asample
   */
  public String getFilenamePrefix(final FastqSample fastqSample,
      final int read) {

    switch (this.version) {

    case BCL2FASTQ_1:

      if (fastqSample.isUndeterminedIndex()) {

        return String.format("lane%d_Undetermined%s", fastqSample.getLane(),
            getConstantFastqSuffix(fastqSample.getLane(), read));
      }

      return String.format("%s_%s%s", fastqSample.getSampleName(),
          fastqSample.getIndex(),
          getConstantFastqSuffix(fastqSample.getLane(), read));

    case BCL2FASTQ_2:
    case BCL2FASTQ_2_15:

      if (fastqSample.isUndeterminedIndex()) {

        return String.format("Undetermined_S0%s",
            getConstantFastqSuffix(fastqSample.getLane(), read));
      }

      checkNotNull(this.samplesheet,
          "sample sheet on version 2 instance not initialize.");

      // Build sample name on fastq file according to version used
      final String fastqSampleName = buildFastqSampleName(fastqSample);
      return String.format("%s_S%d%s", fastqSampleName,
          extractSamplePositionInSampleSheetLane(this.samplesheet, fastqSample),
          getConstantFastqSuffix(fastqSample.getLane(), read));

    default:
      throw new IllegalStateException(
          "Unhandled Bcl2FastqVersion enum value: " + this.version);
    }
  }

  /**
   * Build the prefix report filename.
   * @param fastqSample the fastq sample
   * @param read the read number
   * @return the prefix report filename
   */
  public String buildPrefixReport(final FastqSample fastqSample,
      final int read) {

    if (fastqSample.isUndeterminedIndex()) {
      return String.format("lane%s_Undetermined%s", fastqSample.getLane(),
          getConstantFastqSuffix(fastqSample.getLane(), read));
    }

    return String.format("%s_%s%s", fastqSample.getSampleName(),
        fastqSample.getIndex(),
        getConstantFastqSuffix(fastqSample.getLane(), read));

  }

  /**
   * Build the prefix report filename.
   * @param fastqSample the fastq sample
   * @return the prefix report filename
   */
  public String buildPrefixReport(final FastqSample fastqSample) {

    return buildPrefixReport(fastqSample, fastqSample.getRead());

  }

  /**
   * Keep files that satisfy the specified filter in this directory and
   * beginning with this prefix.
   * @param fastqSample the fastq sample
   * @param read the read number
   * @return an array of abstract pathnames
   */
  public List<File> createListFastqFiles(final FastqSample fastqSample,
      final int read) {

    final String filePrefix = getFilenamePrefix(fastqSample, read);
    final File fastqSampleDir = getFastqSampleParentDir(fastqSample);

    final File[] result = fastqSampleDir.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File file) {

        final String filename = file.getName();

        return file.length() > 0
            && filename.startsWith(filePrefix)
            && filename.contains(FASTQ_EXTENSION);
      }
    });

    if (result == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(result);

  }

  //
  // Other methods
  //

  private String buildFastqSampleName(final FastqSample fastqSample) {

    switch (this.version) {

    case BCL2FASTQ_2:
      return fastqSample.getSampleName();

    case BCL2FASTQ_2_15:
      return fastqSample.getSampleName().replace("_", "-");

    default:
      throw new IllegalStateException(
          "Unhandled Bcl2FastqVersion enum value: " + this.version);
    }
  }

  /**
   * Gets the constant fastq suffix.
   * @param lane the lane
   * @param read the read
   * @return the constant fastq suffix
   */
  private static String getConstantFastqSuffix(final int lane, final int read) {

    return String.format("_L%03d_R%d_001", lane, read);
  }

  /**
   * Extract sample position in a lane of the samplesheet.
   * @param fastqSample the fastq sample
   * @return the position of the sample in the samplesheet lane
   */
  private static int extractSamplePositionInSampleSheetLane(
      final SampleSheet samplesheet, final FastqSample fastqSample) {

    if (fastqSample == null) {
      throw new NullPointerException("fastqSample argument cannot be null");
    }

    // Undetermined cases always return 0
    if (fastqSample.isUndeterminedIndex()) {
      return 0;
    }

    return extractSamplePositionInSampleSheetLane(samplesheet,
        fastqSample.getSampleName());
  }

  /**
   * Extract sample position in a lane of the samplesheet.
   * @param sampleName the sample sample
   * @return the position of the sample in the samplesheet lane
   */
  private static int extractSamplePositionInSampleSheetLane(
      final SampleSheet samplesheet, final String sampleName) {

    if (samplesheet == null) {
      throw new NullPointerException("samplesheet argument cannot be null");
    }

    if (sampleName == null) {
      throw new NullPointerException("sampleName argument cannot be null");
    }

    int i = 0;
    TreeMultimap<Integer, String> sampleEntries = TreeMultimap.create();

    for (Sample sample : samplesheet) {
      // If sample id is not defined, use sample name
      sampleEntries.put(sample.getLane(), sample.getDemultiplexingName());
    }

    List<Integer> lanesSorted = new ArrayList<Integer>(sampleEntries.keySet());
    Collections.sort(lanesSorted);

    for (int lane : lanesSorted) {
      Collection<String> extractedSampleNames = sampleEntries.get(lane);
      for (String sample : extractedSampleNames) {
        i++;
        if (sampleName.equals(sample)) {
          return i;
        }
      }
    }

    return -1;
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
    final Bcl2FastqVersion versionInLogFile =
        extractBcl2FastqVersionFromLog(fastqDir);
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
  private static Bcl2FastqVersion extractBcl2FastqVersionFromLog(
      final File fastqDir) throws IOException {

    // Find log file
    final File[] logFiles = fastqDir.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {

        final String filename = pathname.getName();

        return filename.startsWith("bcl2fastq_output_")
            && (filename.endsWith(".out")
                || filename.endsWith(".err") && pathname.length() > 0);

      }
    });

    // If no log found,
    if (logFiles == null || logFiles.length == 0) {

      return Bcl2FastqVersion.BCL2FASTQ_1;
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

        return Bcl2FastqVersion.parseVersion(fields.get(1));
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

    this(samplesheet, fastqDir, findBcl2FastqVersion(fastqDir));
  }

  /**
   * Constructor.
   * @param samplesheet the samplesheet
   * @param fastqDir the output directory of bcl2fastq
   * @param version of bcl2fastq
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public Bcl2FastqOutput(final SampleSheet samplesheet, final File fastqDir,
      Bcl2FastqVersion version) throws IOException {

    if (samplesheet == null) {
      throw new NullPointerException("samplesheet argument cannot be null");
    }

    if (fastqDir == null) {
      throw new NullPointerException("fastqDir argument cannot be null");
    }

    checkExistingDirectoryFile(fastqDir, "fastq directory");

    this.fastqDirectory = fastqDir;
    this.version = version;
    this.samplesheet = samplesheet;
  }

}
