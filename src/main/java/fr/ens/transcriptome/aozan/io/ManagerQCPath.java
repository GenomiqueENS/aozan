package fr.ens.transcriptome.aozan.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingStandardFile;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.illumina.samplesheet.Sample;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.transcriptome.eoulsan.util.Version;
import jersey.repackaged.com.google.common.collect.Lists;

public class ManagerQCPath {

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

  private static ManagerQCPath singleton;

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
  };

  //
  // Singleton
  //

  /**
   * Gets the single instance of ManagerQCPath.
   * @param globalConf the global configuration
   * @return single instance of ManagerQCPath
   * @throws AozanException if an error occurs when create instance during
   *           parsing sample sheet file
   */
  public static void initizalize(final Map<String, String> globalConf)
      throws AozanException {

    if (singleton != null) {
      throw new IllegalStateException("Singleton has been already initialized");
    }

    // Extract sample sheet file
    final File samplesheetFile =
        new File(globalConf.get(QC.CASAVA_DESIGN_PATH));

    // Extract fastq output directory
    final File fastqDir = new File(globalConf.get(QC.CASAVA_OUTPUT_DIR));

    try {
      singleton = new ManagerQCPath(samplesheetFile, fastqDir);
    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  public static ManagerQCPath getInstance() {

    if (singleton == null) {
      throw new IllegalStateException("Singleton has not been initialized");
    }

    return singleton;
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
   * Find fastq output directory.
   * @param fastqSample the fast sample
   * @return the fastq output directory.
   */
  public File casavaOutputDir(final FastqSample fastqSample) {

    if (fastqSample == null) {
      throw new NullPointerException("fastqSample argument cannot be null");
    }

    switch (this.version) {

    case BCL2FASTQ_1:

      if (fastqSample.isIndeterminedIndices()) {
        return new File(getFastqDirectory()
            + "/" + UNDETERMINED_DIR_NAME + "/" + UNDETERMINED_PREFIX
            + fastqSample.getLane());
      }

      return new File(getFastqDirectory()
          + "/" + PROJECT_PREFIX + fastqSample.getProjectName() + "/"
          + SAMPLE_PREFIX + fastqSample.getSampleName());

    case BCL2FASTQ_2:
    case BCL2FASTQ_2_15:

      if (fastqSample.isIndeterminedIndices()) {
        return getFastqDirectory();
      }

      return new File(getFastqDirectory() + "/" + fastqSample.getProjectName());

    default:
      throw new IllegalStateException(
          "Unhandled Bcl2FastqVersion enum value: " + this.version);
    }

  }

  /**
   * Set the prefix of the fastq file of read for a fastq on a sample.
   * @return prefix fastq files for this fastq on asample
   */
  public String prefixFileName(final FastqSample fastqSample, final int read) {

    switch (this.version) {

    case BCL2FASTQ_1:

      if (fastqSample.isIndeterminedIndices()) {

        return String.format("lane%d_Undetermined%s", fastqSample.getLane(),
            getConstantFastqSuffix(fastqSample.getLane(), read));
      }

      return String.format("%s_%s%s", fastqSample.getSampleName(),
          fastqSample.getIndex(),
          getConstantFastqSuffix(fastqSample.getLane(), read));

    case BCL2FASTQ_2:
    case BCL2FASTQ_2_15:

      if (fastqSample.isIndeterminedIndices()) {

        return String.format("Undetermined_S0%s",
            getConstantFastqSuffix(fastqSample.getLane(), read));
      }

      checkNotNull(this.samplesheet,
          "sample sheet on version 2 instance not initialize.");

      // Build sample name on fastq file according to version used
      final String fastqSampleName = buildFastqSampleName(fastqSample);

      return String.format("%s_S%d%s", fastqSampleName,
          extractOrderNumberSample(this.samplesheet, fastqSample),
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

    if (fastqSample.isIndeterminedIndices()) {
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
   * @param read the read number
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

    return Arrays.asList(new File(casavaOutputDir(fastqSample) + "/")
        .listFiles(new FileFilter() {

          @Override
          public boolean accept(final File pathname) {

            return pathname.length() > 0
                && pathname.getName()
                    .startsWith(singleton.prefixFileName(fastqSample, read))
                && pathname.getName().contains(FASTQ_EXTENSION);
          }
        }));
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
   * Extract order number sample.
   * @param fastqSample the fastq sample
   * @return the int
   */
  private int extractOrderNumberSample(final SampleSheet samplesheet,
      final FastqSample fastqSample) {

    if (samplesheet == null) {
      throw new NullPointerException("samplesheet argument cannot be null");
    }

    if (fastqSample == null) {
      throw new NullPointerException("fastqSample argument cannot be null");
    }

    final int lane = fastqSample.getLane();
    final String sampleName = fastqSample.getSampleName();

    // Undetermined cases always return 0
    if (fastqSample.isIndeterminedIndices()) {
      return 0;
    }

    int i = 0;

    for (Sample sample : samplesheet) {

      i++;

      if (lane > 0 && sample.getLane() != lane) {
        continue;
      }

      if (sampleName.equals(sample.getSampleName())) {
        return i;
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

    for (String filename : Arrays.asList("DemultiplexedBustardConfig.xml",
        "DemultiplexedBustardSummary.xml", "SampleSheet.mk", "support.txt",
        "DemultiplexConfig.xml", "Makefile", "make.err", "make.out", "Temp")) {

      if (new File(fastqDir, filename).exists()) {
        return Bcl2FastqVersion.BCL2FASTQ_1;
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

    if (count == 4) {
      return Bcl2FastqVersion.BCL2FASTQ_2_15;
    } else if (count > 4) {
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
            && filename.endsWith(".out");

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
        Charsets.UTF_8)) {

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

  //
  // Constructor
  //

  private ManagerQCPath(final File samplesheetFile, final File fastqDir)
      throws FileNotFoundException, IOException {

    if (samplesheetFile == null) {
      throw new NullPointerException("samplesheetFile argument cannot be null");
    }

    if (fastqDir == null) {
      throw new NullPointerException("fastqDir argument cannot be null");
    }

    checkExistingStandardFile(samplesheetFile, "sample sheet");
    checkExistingDirectoryFile(fastqDir, "fastq directory");

    this.version = findBcl2FastqVersion(fastqDir);

    // TODO implements this
    this.fastqDirectory = fastqDir;
    this.samplesheet = new SampleSheetCSVReader(samplesheetFile).read();
  }

}
