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

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * The class correspond of one entity to treat by AbstractFastqCollector, so a
 * sample per lane and in mode-paired one FastSample for each read (R1 and R2).
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqSample {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  public static final String FASTQ_EXTENSION = ".fastq";

  private static final String NO_INDEX = "NoIndex";

  private final int read;
  private final int lane;
  private final String sampleName;
  private final String projectName;
  private final String descriptionSample;
  private final String index;

  private final boolean undeterminedIndices;

  private final String runFastqPath;
  private final String keyFastqSample;
  private final String nameTemporaryFastqFiles;

  private final List<File> fastqFiles;
  private final CompressionType compressionType;

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

    return firstFastqFileName.substring(0, firstFastqFileName.length()
        - FASTQ_EXTENSION.length()
        - this.compressionType.getExtension().length());

  }

  /**
   * Create name for temporary fastq file uncompressed.
   * @return name fastq file
   */
  private String createNameTemporaryFastqFile() {
    return "aozan_fastq_" + this.keyFastqSample + FASTQ_EXTENSION;
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
      throw new AozanRuntimeException(
          "Fastq Sample, no fastq file found for sample ");
    }

    if (fastqFiles.get(0).getName().endsWith(FASTQ_EXTENSION)) {
      return CompressionType.NONE;
    }

    return CompressionType.getCompressionTypeByFilename(fastqFiles.get(0)
        .getName());

  }

  /**
   * Create the prefix used for add data in a RunData for each FastqSample.
   * @return prefix
   */
  public String getPrefixRundata() {

    if (isIndeterminedIndices()) {
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

    return ManagerQCPath.getInstance().createListFastqFiles(this, read);

  }

  /**
   * Gets the prefix report filename.
   * @return the prefix report
   */
  public String getPrefixReport(final int read) {

    return ManagerQCPath.getInstance().buildPrefixReport(this, read);

  }

  public String getPrefixReport() {

    return ManagerQCPath.getInstance().buildPrefixReport(this);

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
  public String getDescriptionSample() {
    return this.descriptionSample;
  }

  /**
   * Test if the sample is an undetermined indices sample.
   * @return true if the sample is an undetermined indices sample
   */
  public boolean isIndeterminedIndices() {
    return this.undeterminedIndices;
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
  public String getNameTemporaryFastqFiles() {
    return this.nameTemporaryFastqFiles;
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

  @Override
  public String toString() {
    return "FastqSample [read="
        + read + ", lane=" + lane + ", sampleName=" + sampleName
        + ", projectName=" + projectName + ", descriptionSample="
        + descriptionSample + ", index=" + index + ", undeterminedIndices="
        + undeterminedIndices + ", runFastqPath=" + runFastqPath
        + ", keyFastqSample=" + keyFastqSample + ", nameTemporaryFastqFiles="
        + nameTemporaryFastqFiles + ", fastqFiles=" + fastqFiles
        + ", compressionType=" + compressionType + "]";
  }

  //
  // Constructor
  //
  /**
   * Public constructor corresponding of a technical replica sample.
   * @param casavaOutputPath path to fastq files
   * @param read read number
   * @param lane lane number
   * @param sampleName name of the sample
   * @param projectName name of the project
   * @param descriptionSample description of the sample
   * @param index value of index or if doesn't exists, NoIndex
   */
  public FastqSample(final String casavaOutputPath, final int read,
      final int lane, final String sampleName, final String projectName,
      final String descriptionSample, final String index) {

    this.read = read;
    this.lane = lane;
    this.sampleName = sampleName;
    this.projectName = projectName;
    this.descriptionSample = descriptionSample;
    this.index = (index == null || index.isEmpty()) ? NO_INDEX : index;
    this.undeterminedIndices = false;

    this.runFastqPath = casavaOutputPath;
    
    this.fastqFiles = createListFastqFiles(this.read);

    this.compressionType = getCompressionExtension(this.fastqFiles);
    this.keyFastqSample = createKeyFastqSample();

    this.nameTemporaryFastqFiles = createNameTemporaryFastqFile();

  }

  /**
   * Public constructor corresponding of a undetermined index sample.
   * @param casavaOutputPath path to fastq files
   * @param read read number
   * @param lane lane number
   */
  public FastqSample(final String casavaOutputPath, final int read,
      final int lane) {

    this.read = read;
    this.lane = lane;
    this.sampleName = "lane" + lane;
    this.projectName = "";
    this.descriptionSample = "";
    this.index = NO_INDEX;
    this.undeterminedIndices = true;

    this.runFastqPath = casavaOutputPath;

    this.fastqFiles = createListFastqFiles(this.read);

    this.compressionType = getCompressionExtension(this.fastqFiles);
    this.keyFastqSample = createKeyFastqSample();

    this.nameTemporaryFastqFiles = createNameTemporaryFastqFile();

  }

}
