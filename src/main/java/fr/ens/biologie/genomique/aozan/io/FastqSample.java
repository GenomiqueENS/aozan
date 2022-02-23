/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.io;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.illumina.Bcl2FastqOutput;
import fr.ens.biologie.genomique.aozan.illumina.Bcl2FastqOutput.Bcl2FastqVersion;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;

/**
 * The class correspond of one entity to treat by AbstractFastqCollector, so a
 * sample per lane and in mode-paired one FastSample for each read (R1 and R2).
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqSample {

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

  public static final String SUBSET_FASTQ_FILENAME_PREFIX =
      Globals.APP_NAME_LOWER_CASE + "_subset_fastq_";
  private static final String NO_INDEX = "NoIndex";

  private final int sampleId;
  private final int read;
  private final int lane;
  private final String sampleDirname;
  private final String sampleName;
  private final String projectName;
  private final String description;
  private final String index;

  private final boolean undeterminedIndex;

  private final String filenamePrefix;
  private final String subsetFastqFilename;

  private final List<File> fastqFiles;
  private final CompressionType compressionType;

  private final Bcl2FastqOutput bcl2fastqOutput;

  private final File tmpDir;

  //
  // Getters
  //

  /**
   * Get the sample Id.
   * @return the sample Id
   */
  public int getSampleId() {
    return this.sampleId;
  }

  /**
   * Get the number of read from the sample in run.
   * @return number read
   */
  public int getRead() {
    return this.read;
  }

  /**
   * Get the number of lane from the sample in run.
   * @return number lane
   */
  public int getLane() {
    return this.lane;
  }

  /**
   * Get the directory name of the sample. The value can be empty if there is
   * not dedicated directory for the sample.
   * @return the directory name of the sample
   */
  public String getSampleDirectoryName() {
    return this.sampleDirname;
  }

  /**
   * Get the project name from the sample in run.
   * @return project name
   */
  public String getProjectName() {
    return this.projectName;
  }

  /**
   * Get the sample name in run.
   * @return sample name
   */
  public String getSampleName() {
    return this.sampleName;
  }

  /**
   * Get the description of the sample in run.
   * @return description of the sample
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Test if the sample is an undetermined index sample.
   * @return true if the sample has an undetermined index
   */
  public boolean isUndeterminedIndex() {
    return this.undeterminedIndex;
  }

  /**
   * Gets the index.
   * @return the index
   */
  public String getIndex() {
    return this.index;
  }

  /**
   * Get list of fastq files for this sample.
   * @return list fastq files
   */
  public List<File> getFastqFiles() {
    return this.fastqFiles;
  }

  /**
   * Get list of fastq file names for this sample.
   * @return a String with the filename of the sample
   */
  public String getFastqFilenames() {

    List<String> result = new ArrayList<>();
    for (File f : this.fastqFiles) {
      result.add(f.getName());
    }

    return String.join(",", result);
  }

  /**
   * Get the name for temporary fastq files uncompressed.
   * @return temporary fastq file name
   */
  public String getSubsetFastqFilename() {
    return this.subsetFastqFilename;
  }

  /**
   * Get the compression type for fastq files.
   * @return compression type for fastq files
   */
  public CompressionType getCompressionType() {
    return this.compressionType;
  }

  //
  // Other methods
  //

  /**
   * Create name for temporary fastq file uncompressed.
   * @param runId run id
   * @param key sample key
   * @return name fastq file
   */
  private static String createSubsetFastqFilename(final String runId,
      final String key) {
    return SUBSET_FASTQ_FILENAME_PREFIX + runId + '_' + key + FASTQ_EXTENSION;
  }

  /**
   * Get ratio compression for fastq files according to type compression.
   * @return ratio compression or 1 if not compress
   */
  private double ratioCommpression() {

    switch (this.compressionType) {

    case GZIP:
      return 7.0;

    case BZIP2:
      return 5.0;

    case NONE:
      return 1.0;

    default:
      return 1.0;

    }
  }

  /**
   * Receive the type of compression use for fastq files, only one possible per
   * sample.
   */
  private static CompressionType getCompressionExtension(
      final List<File> fastqFiles) {

    if (fastqFiles.isEmpty()) {
      throw new IllegalArgumentException("fastqFiles argument cannot be empty");
    }

    if (fastqFiles.get(0).getName().endsWith(FASTQ_EXTENSION)) {
      return CompressionType.NONE;
    }

    return CompressionType
        .getCompressionTypeByFilename(fastqFiles.get(0).getName());
  }

  /**
   * Create the prefix used for add data in a RunData for each FastqSample.
   * @return prefix
   */
  public String getRundataPrefix() {

    return ".sample" + this.sampleId + ".read" + this.read;
  }

  /**
   * Return if it must uncompress fastq files else false.
   * @return true if it must uncompress fastq files else false.
   */
  public boolean isUncompressedNeeded() {

    return !this.compressionType.equals(CompressionType.NONE);
  }

  /**
   * Returns a estimation of the size of uncompressed fastq files according to
   * the type extension of files and the coefficient of uncompression
   * corresponding.
   * @return size if uncompressed fastq files
   */
  public long getUncompressedSize() {
    // according to type of compressionExtension
    long sizeFastqFiles = 0;

    for (final File f : this.fastqFiles) {
      sizeFastqFiles += f.length();
    }

    return (long) (sizeFastqFiles * ratioCommpression());
  }

  /**
   * Keep files that satisfy the specified filter in this directory and
   * beginning with this prefix.
   * @return an array of abstract pathnames
   */
  private List<File> createListFastqFiles(final boolean createEmptyFastq) {

    final List<File> result = createListFastqFiles();

    // Empty FASTQ files are not created by bcl2fastq 2
    if (result.isEmpty()) {

      final String filenamePrefix = getFilenamePrefix();

      final File emptyFile =
          new File(this.tmpDir, filenamePrefix + "_empty.fastq");

      // Create empty file
      if (!emptyFile.exists() && createEmptyFastq) {
        try {
          if (!emptyFile.createNewFile()) {
            throw new IOException(
                "Unable to create empty FASTQ file: " + emptyFile);
          }
        } catch (IOException e) {
          throw new AozanRuntimeException(e);
        }
      }

      return Collections.singletonList(emptyFile);
    }

    return result;
  }

  /**
   * Return the temporary if exists which correspond to the key.
   * @return File temporary file or null if it not exists
   */
  public File getSubsetFastqFile() {

    return new File(this.tmpDir, getSubsetFastqFilename());
  }

  /**
   * Find fastq parent directory.
   * @return the fastq output directory.
   */
  public File getFastqSampleParentDir() {

    final Bcl2FastqVersion version = this.bcl2fastqOutput.getVersion();
    final File fastqDirectory = this.bcl2fastqOutput.getFastqDirectory();

    switch (version) {

    case BCL2FASTQ_1:

      if (isUndeterminedIndex()) {

        final File undeterminedDir =
            new File(fastqDirectory, UNDETERMINED_DIR_NAME);

        return new File(undeterminedDir, UNDETERMINED_PREFIX + getLane());
      }

      final File projectDir = new File(this.bcl2fastqOutput.getFastqDirectory(),
          PROJECT_PREFIX + getProjectName());

      return new File(projectDir, SAMPLE_PREFIX + getSampleName());

    case BCL2FASTQ_2:
    case BCL2FASTQ_2_15:

      if (isUndeterminedIndex()) {
        return fastqDirectory;
      }

      final File projectV2Dir = new File(fastqDirectory, getProjectName());

      // Check if there is a sub directory for each sample
      if ("".equals(getSampleDirectoryName())) {

        // No sample sub directory
        return projectV2Dir;
      } else {

        // Sample sub directory
        return new File(projectV2Dir, getSampleDirectoryName());
      }

    case BCL_CONVERT:

      if (isUndeterminedIndex()) {
        return fastqDirectory;
      }

      final File projectV3Dir = new File(fastqDirectory, getProjectName());

      if (projectV3Dir.exists()) {
        return projectV3Dir;
      } else {
        return fastqDirectory;
      }

    default:
      throw new IllegalStateException(
          "Unhandled Bcl2FastqVersion enum value: " + version);
    }
  }

  /**
   * Get the prefix of the FASTQ file.
   * @return the prefix of the FASTQ file
   */
  public String getFilenamePrefix() {

    return this.filenamePrefix;
  }

  /**
   * Get the prefix of the FASTQ file.
   * @param read the read of the sample
   * @return the prefix of the FASTQ file
   */
  public String getFilenamePrefix(final int read) {

    final Bcl2FastqVersion version = this.bcl2fastqOutput.getVersion();
    final SampleSheet sampleSheet = this.bcl2fastqOutput.getSampleSheet();

    switch (version) {

    case BCL2FASTQ_1:

      if (isUndeterminedIndex()) {

        return String.format("lane%d_Undetermined%s", getLane(),
            getConstantFastqSuffix(getLane(), read));
      }

      return String.format("%s_%s%s", getSampleName(), getIndex(),
          getConstantFastqSuffix(getLane(), read));

    case BCL2FASTQ_2:
    case BCL2FASTQ_2_15:
    case BCL_CONVERT:

      if (isUndeterminedIndex()) {

        return String.format("Undetermined_S0%s",
            getConstantFastqSuffix(getLane(), read));
      }

      requireNonNull(sampleSheet,
          "sample sheet on version 2 instance not initialize.");

      // Build sample name on fastq file according to version used
      final String fastqSampleName = buildFastqSampleName();
      return String.format("%s_S%d%s", fastqSampleName,
          extractSamplePositionInSampleSheetLane(sampleSheet),
          getConstantFastqSuffix(getLane(), read));

    default:
      throw new IllegalStateException(
          "Unhandled Bcl2FastqVersion enum value: " + version);
    }
  }

  /**
   * Keep files that satisfy the specified filter in this directory and
   * beginning with this prefix.
   * @return an array of abstract pathnames
   */
  public List<File> createListFastqFiles() {

    final String filePrefix = getFilenamePrefix();
    final File fastqSampleDir = getFastqSampleParentDir();

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

  private String buildFastqSampleName() {

    final Bcl2FastqVersion version = this.bcl2fastqOutput.getVersion();

    switch (version) {

    case BCL2FASTQ_2:
      return getSampleName();

    case BCL2FASTQ_2_15:
    case BCL_CONVERT:
      return getSampleName().replace("_", "-");

    default:
      throw new IllegalStateException(
          "Unhandled Bcl2FastqVersion enum value: " + version);
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
   * @return the position of the sample in the samplesheet lane
   */
  private int extractSamplePositionInSampleSheetLane(
      final SampleSheet samplesheet) {

    // Undetermined cases always return 0
    if (isUndeterminedIndex()) {
      return 0;
    }

    return extractSamplePositionInSampleSheetLane(samplesheet, getSampleName());
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
    List<Integer> lanesSorted = new ArrayList<Integer>();
    Multimap<Integer, String> sampleEntries = ArrayListMultimap.create();

    for (Sample sample : samplesheet) {

      int lane = sample.getLane();
      // If sample id is not defined, use sample name
      sampleEntries.put(lane, sample.getDemultiplexingName());

      // Save the order of the lane in the samplesheet
      if (!lanesSorted.contains(lane)) {
        lanesSorted.add(lane);
      }
    }

    final Set<String> samplesFound = new HashSet<>();

    for (int lane : lanesSorted) {
      Collection<String> extractedSampleNames = sampleEntries.get(lane);

      for (String sample : extractedSampleNames) {

        if (!samplesFound.contains(sample)) {
          i++;
          samplesFound.add(sample);
        }

        if (sampleName.equals(sample)) {
          return i;
        }
      }
    }

    return -1;
  }

  /**
   * Get the sample sub directory.
   * @param sampleId sample identifier
   * @param sampleName sample name
   * @return the sample sub directory or an empty string
   */
  public static String defineSampleSubDirName(final String sampleId,
      final String sampleName) {

    if (sampleId != null
        && !"".equals(sampleId.trim()) && sampleName != null
        && !"".equals(sampleName.trim()) && !sampleId.equals(sampleName)) {
      return sampleId.trim();
    }

    return "";
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("read", read).add("lane", lane)
        .add("sampleName", sampleName).add("projectName", projectName)
        .add("descriptionSample", description).add("index", index)
        .add("undeterminedIndex", undeterminedIndex)
        .add("filenamePrefix", filenamePrefix)
        .add("subsetFastqFilename", subsetFastqFilename)
        .add("fastqFiles", fastqFiles).add("compressionType", compressionType)
        .toString();
  }

  //
  // Constructors
  //

  /**
   * Public constructor corresponding of a technical replica sample.
   * @param qc QC object
   * @param sampleId the sample Id
   * @param read read number
   * @param lane lane number
   * @param sampleDirname sample directory name
   * @param sampleName name of the sample
   * @param projectName name of the project
   * @param descriptionSample description of the sample
   * @param index value of index or if doesn't exists, NoIndex
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final QC qc, final int sampleId, final int read,
      final int lane, final String sampleDirname, final String sampleName,
      final String projectName, final String descriptionSample,
      final String index) throws IOException {

    this(qc.getSampleSheet(), qc.getFastqDir(), qc.getTmpDir(), qc.getRunId(),
        sampleId, read, lane, sampleDirname, sampleName, projectName,
        descriptionSample, index);
  }

  /**
   * Public constructor corresponding of a technical replica sample.
   * @param sampleSheet the sample sheet
   * @param fastqDir FASTQ directory
   * @param tmpDir temporary directory
   * @param runId the run id
   * @param sampleId the sample Id
   * @param read read number
   * @param lane lane number
   * @param sampleDirname sample directory name
   * @param sampleName name of the sample
   * @param projectName name of the project
   * @param descriptionSample description of the sample
   * @param index value of index or if doesn't exists, NoIndex
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final SampleSheet sampleSheet, final File fastqDir,
      final File tmpDir, final String runId, final int sampleId, final int read,
      final int lane, final String sampleDirname, final String sampleName,
      final String projectName, final String descriptionSample,
      final String index) throws IOException {

    this(new Bcl2FastqOutput(sampleSheet, fastqDir), tmpDir, runId, sampleId,
        read, lane, sampleDirname, sampleName, projectName, descriptionSample,
        index, false, true);

  }

  /**
   * Public constructor (used only for tests)
   * @param bcl2FastqOutput the output of bcl2fastq
   * @param tmpDir the temporary directory
   * @param runId the run id
   * @param sampleId the sampleId
   * @param read the read number
   * @param sample the sample
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(Bcl2FastqOutput bcl2FastqOutput, final File tmpDir,
      final String runId, final int sampleId, final int read,
      final Sample sample) throws IOException {

    this(bcl2FastqOutput, tmpDir, runId, sampleId, read,
        sample.getLane() != -1 ? sample.getLane() : 1,
        defineSampleSubDirName(sample.getSampleId(), sample.getSampleName()),
        sample.getDemultiplexingName(), sample.getSampleProject(),
        sample.getDescription(), "", false, true);
  }

  /**
   * Public constructor corresponding of a undetermined index sample.
   * @param qc QC object
   * @param sampleId the sample Id
   * @param read read number
   * @param lane lane number
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final QC qc, final int sampleId, final int read,
      final int lane) throws IOException {

    this(qc.getSampleSheet(), qc.getFastqDir(), qc.getTmpDir(), qc.getRunId(),
        sampleId, read, lane);
  }

  /**
   * Public constructor corresponding of a undetermined index sample.
   * @param sampleSheet the sample sheet
   * @param fastqDir the FASTQ directory
   * @param tmpDir the temporary directory
   * @param runId the runId
   * @param sampleId the sample Id
   * @param read read number
   * @param lane lane number
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final SampleSheet sampleSheet, final File fastqDir,
      final File tmpDir, final String runId, final int sampleId, final int read,
      final int lane) throws IOException {

    this(new Bcl2FastqOutput(sampleSheet, fastqDir), tmpDir, runId, sampleId,
        read, lane, null, "lane" + lane, "", "", NO_INDEX, true, true);
  }

  /**
   * Public constructor corresponding of a undetermined index sample.
   * @param bcl2FastqOutput Bcl2FastqOutput object
   * @param tmpDir temporary directory
   * @param runId the run id
   * @param sampleId the sample Id
   * @param read read number
   * @param lane lane number
   * @param sampleDirname sample dir name
   * @param sampleName name of the sample
   * @param projectName name of the project
   * @param descriptionSample description of the sample
   * @param index value of index or if doesn't exists, NoIndex
   * @param undeterminedIndex undetermined index
   * @param createEmptyFastq enable the creation of empty FASTQ files
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final Bcl2FastqOutput bcl2FastqOutput, final File tmpDir,
      final String runId, final int sampleId, final int read, final int lane,
      final String sampleDirname, final String sampleName,
      final String projectName, final String descriptionSample,
      final String index, final boolean undeterminedIndex,
      final boolean createEmptyFastq) throws IOException {

    checkArgument(read > 0, "read value cannot be lower than 1");
    checkArgument(lane > 0, "lane value cannot be lower than 1");

    this.sampleId = sampleId;
    this.read = read;
    this.lane = lane;
    this.sampleDirname = sampleDirname == null ? "" : sampleDirname.trim();
    this.sampleName = sampleName;
    this.projectName = projectName;
    this.description = descriptionSample;
    this.index = (index == null || index.isEmpty()) ? NO_INDEX : index;
    this.undeterminedIndex = undeterminedIndex;
    this.bcl2fastqOutput = bcl2FastqOutput;
    this.tmpDir = tmpDir;
    this.filenamePrefix = getFilenamePrefix(this.read);

    this.fastqFiles = createListFastqFiles(createEmptyFastq);

    this.compressionType = getCompressionExtension(this.fastqFiles);

    this.subsetFastqFilename =
        createSubsetFastqFilename(runId, getFilenamePrefix());
  }

}
