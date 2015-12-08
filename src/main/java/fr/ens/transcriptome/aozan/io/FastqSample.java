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
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.illumina.Bcl2FastqOutput;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

/**
 * The class correspond of one entity to treat by AbstractFastqCollector, so a
 * sample per lane and in mode-paired one FastSample for each read (R1 and R2).
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqSample {

  public static final String FASTQ_EXTENSION = ".fastq";

  private static final String SUBSET_FASTQ_FILENAME_PREFIX =
      Globals.APP_NAME_LOWER_CASE + "_subset_fastq_";
  private static final String NO_INDEX = "NoIndex";

  private final int read;
  private final int lane;
  private final String sampleName;
  private final String projectName;
  private final String description;
  private final String index;

  private final boolean undeterminedIndex;

  private final String keyFastqSample;
  private final String subsetFastqFilename;

  private final List<File> fastqFiles;
  private final CompressionType compressionType;

  private final Bcl2FastqOutput bcl2fastqOutput;

  private final File tmpDir;

  /**
   * Create a key unique for each fastq sample.
   * @return key
   */
  private String createKeyFastqSample() {

    // Case exists only during step test
    if (this.fastqFiles.isEmpty()) {
      return this.lane + " " + this.sampleName;
    }

    final String firstFastqFileName = this.fastqFiles.get(0).getName();

    return firstFastqFileName.substring(0,
        firstFastqFileName.length()
            - FASTQ_EXTENSION.length()
            - this.compressionType.getExtension().length());
  }

  /**
   * Create name for temporary fastq file uncompressed.
   * @param runId run id
   * @param key
   * @return name fastq file
   */
  private static String createSubsetFastqFilename(final String runId,
      final String key) {
    return SUBSET_FASTQ_FILENAME_PREFIX + key + FASTQ_EXTENSION;
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
  public String getPrefixRundata() {

    if (isUndeterminedIndex()) {
      return ".lane" + this.lane + ".undetermined.read" + this.read;
    }

    return ".lane"
        + this.lane + ".sample." + this.sampleName + ".read" + this.read + "."
        + this.sampleName;
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
  private List<File> createListFastqFiles(final int read) {

    final List<File> result =
        this.bcl2fastqOutput.createListFastqFiles(this, read);

    // Empty FASTQ files are not created by bcl2fastq 2
    if (result.isEmpty()) {

      final String filenamePrefix =
          this.bcl2fastqOutput.getFilenamePrefix(this, read);

      final File emptyFile =
          new File(this.tmpDir, filenamePrefix + "_empty.fastq");

      // Create empty file
      if (!emptyFile.exists()) {
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
   * Gets the prefix report filename.
   * @return the prefix report
   */
  public String getPrefixReport(final int read) {

    return this.bcl2fastqOutput.buildPrefixReport(this, read);

  }

  public String getPrefixReport() {

    return this.bcl2fastqOutput.buildPrefixReport(this);

  }

  //
  // Getters
  //

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
   * Get the prefix corresponding on read 2 for this sample, this value exists
   * only in mode paired.
   * @return prefix for read 2
   */
  public String getPrefixRead2() {
    return this.keyFastqSample.replaceFirst("R1", "R2");
  }

  /**
   * Get the name for temporary fastq files uncompressed.
   * @return temporary fastq file name
   */
  public String getSubsetFastqFilename() {
    return this.subsetFastqFilename;
  }

  /**
   * Get the unique key for sample.
   * @return unique key for sample
   */
  public String getKeyFastqSample() {
    return this.keyFastqSample;
  }

  /**
   * Get the compression type for fastq files.
   * @return compression type for fastq files
   */
  public CompressionType getCompressionType() {
    return this.compressionType;
  }

  //
  // Partial FASTQ file methods
  //

  /**
   * Return the temporary if exists which correspond to the key.
   * @return File temporary file or null if it not exists
   */
  public File getPartialFile() {

    return new File(this.tmpDir, getSubsetFastqFilename());
  }

  /**
   * Check if a temporary file corresponding with fastq files has already
   * created.
   * @return true if map of files contains a entry with the same key or false
   */
  public boolean isPartialFileExists() {

    if (this.getFastqFiles().isEmpty()) {
      return false;
    }

    return getPartialFile().exists();

  }

  //
  // SequenceFile methods
  //

  /**
   * Return a sequenceFile for all fastq files present to treat in the sample.
   * If the temporary file doesn't existed, it is created.
   * @param fastqSample sample to treat
   * @return SequenceFile an structure which allow to browse a fastq file
   *         sequence per sequence
   * @throws AozanException if an error occurs during writing file
   */
  public SequenceFile getSequenceFile() throws AozanException {

    final File[] fastq = getFastqFiles().toArray(new File[0]);
    final SequenceFile seqFile;

    try {

      if (getPartialFile().exists()) {
        seqFile = SequenceFactory.getSequenceFile(fastq);

      } else {
        // Create temporary fastq file
        seqFile = new AozanSequenceFile(fastq, getPartialFile(), this);
      }

    } catch (final IOException io) {
      throw new AozanException(io);

    } catch (final SequenceFormatException e) {
      throw new AozanException(e);
    }

    return seqFile;
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
        .add("keyFastqSample", keyFastqSample)
        .add("subsetFastqFilename", subsetFastqFilename)
        .add("fastqFiles", fastqFiles).add("compressionType", compressionType)
        .toString();
  }

  //
  // Constructor
  //

  /**
   * Public constructor corresponding of a technical replica sample.
   * @param samplesheet the samplesheet
   * @param casavaOutputPath path to fastq files
   * @param read read number
   * @param lane lane number
   * @param sampleName name of the sample
   * @param projectName name of the project
   * @param descriptionSample description of the sample
   * @param index value of index or if doesn't exists, NoIndex
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final QC qc, final int read, final int lane,
      final String sampleName, final String projectName,
      final String descriptionSample, final String index) throws IOException {

    checkNotNull(qc, "qc argument cannot be null");
    checkArgument(read > 0, "read value cannot be lower than 1");
    checkArgument(lane > 0, "read value cannot be lower than 1");

    this.read = read;
    this.lane = lane;
    this.sampleName = sampleName;
    this.projectName = projectName;
    this.description = descriptionSample;
    this.index = (index == null || index.isEmpty()) ? NO_INDEX : index;
    this.undeterminedIndex = false;

    this.bcl2fastqOutput =
        new Bcl2FastqOutput(qc.getSampleSheetFile(), qc.getFastqDir());
    this.tmpDir = qc.getTmpDir();

    this.fastqFiles = createListFastqFiles(this.read);

    this.compressionType = getCompressionExtension(this.fastqFiles);
    this.keyFastqSample = createKeyFastqSample();

    this.subsetFastqFilename =
        createSubsetFastqFilename(qc.getRunId(), this.keyFastqSample);
  }

  /**
   * Public constructor corresponding of a undetermined index sample.
   * @param samplesheet the samplesheet
   * @param casavaOutputPath path to fastq files
   * @param read read number
   * @param lane lane number
   * @throws IOException if an error occurs while reading bcl2fastq version
   */
  public FastqSample(final QC qc, final int read, final int lane)
      throws IOException {

    checkNotNull(qc, "qc argument cannot be null");
    checkArgument(read > 0, "read value cannot be lower than 1");
    checkArgument(lane > 0, "read value cannot be lower than 1");

    this.read = read;
    this.lane = lane;
    this.sampleName = "lane" + lane;
    this.projectName = "";
    this.description = "";
    this.index = NO_INDEX;
    this.undeterminedIndex = true;

    this.bcl2fastqOutput =
        new Bcl2FastqOutput(qc.getSampleSheetFile(), qc.getFastqDir());
    this.tmpDir = qc.getTmpDir();

    this.fastqFiles = createListFastqFiles(this.read);

    this.compressionType = getCompressionExtension(this.fastqFiles);
    this.keyFastqSample = createKeyFastqSample();

    this.subsetFastqFilename =
        createSubsetFastqFilename(qc.getRunId(), this.keyFastqSample);
  }

}
