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
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * The class correspond of one entity to treat by AbstractFastqCollector, so a
 * sample per lane.
 * @author Sandrine Perrin
 */
public class FastqSample {

  private static final String VALUE = ".fq";

  private final int read1;
  // private final int read2;
  private final int lane;
  private final String sampleName;
  private final String projectName;
  private final String index;

  private final String runFastqPath;
  private final String keyFastqSample;
  private final int keyFastqFiles;
  private final String nameTemporaryFastqFiles;

  private final List<File> fastqFiles;
  private CompressionType compressionType;

  /**
   * Create a key unique for each fastq sample.
   * @return key
   */
  private String createKeyFastqSample() {

    // Case exists only during step test
    if (fastqFiles.isEmpty())
      return lane + " " + sampleName;

    String firstFastqFileName = this.fastqFiles.get(0).getName();

    return firstFastqFileName.substring(0,
        firstFastqFileName.length()
            - VALUE.length() - compressionType.getExtension().length());

  }

  /**
   * @return
   */
  private int createKeyFastqFiles() {

    StringBuilder key = new StringBuilder();
    String separator = "\t";

    for (File f : fastqFiles) {
      key.append(f.getAbsolutePath());
      key.append(separator);
    }
    // System.out.println("key files " + key + " hashcode " + key.hashCode());

    return key.hashCode();
  }

  /**
   * @return
   */
  private String createNameTemporaryFastqFile() {
    // return "aozan_fastq_" + Integer.toString(keyFastqFiles) + ".fastq";
    return "aozan_fastq_" + keyFastqSample + ".fastq";
  }

  /**
   * @return
   */
  private double ratioCommpression() {

    switch (compressionType) {

    case GZIP:
      return 7.;

    case BZIP2:
      return 5.;

    case NONE:
      return 1.;

      // TODO to define return value
    default:
      return 1.;

    }

  }

  /**
   * Receive the type of compression use for fastq files.
   * @throws AozanException
   */
  public CompressionType setCompressionExtension() { // throws AozanException {

    if (StringUtils.extension(fastqFiles.get(0).getName()).equals("fastq"))
      return CompressionType.NONE;

    CompressionType zType =
        CompressionType.getCompressionTypeByFilename(fastqFiles.get(0)
            .getName());

    // if (compressionType.equals(CompressionType.NONE))
    // throw new AozanException("Compression extension unknown.");

    return zType;
  }

  /**
   * Create the prefix used for add data in a RunData for each FastqSample
   * @return prefix
   */
  public String getPrefixRundata() {
    return ".lane"
        + this.lane + ".sample." + this.sampleName + ".read" + this.read1 + "."
        + this.sampleName;
  }

  /**
   * Return if it must uncompress fastq files else false.
   * @return true if it must uncompress fastq files else false.
   */
  public boolean isUncompressedNeeded() {
    return !compressionType.equals(CompressionType.NONE);
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

    for (File f : fastqFiles) {
      sizeFastqFiles += f.length();
    }
    // System.out.println("  ratio " + ratioCommpression());
    // System.out.println("type compression " + compressionType);

    return (long) (sizeFastqFiles * ratioCommpression());
  }

  /**
   * Set the directory to the file
   * @return
   */
  public File casavaOutputDir() {

    return new File(this.runFastqPath
        + "/Project_" + projectName + "/Sample_" + sampleName);
  }

  /**
   * Set the prefix of the file of read1
   * @return
   */
  private String prefixFileName(int read) {
    return String.format("%s_%s_L%03d_R%d_", sampleName, "".equals(index)
        ? "NoIndex" : index, lane, read);
  }

  /**
   * Keep files that satisfy the specified filter in this directory and
   * beginning with this prefix
   * @return an array of abstract pathnames
   */
  private List<File> createListFastqFiles(final int read) {

    return Arrays.asList(new File(casavaOutputDir() + "/")
        .listFiles(new FileFilter() {

          @Override
          public boolean accept(final File pathname) {
            return pathname.length() > 0
                && pathname.getName().startsWith(prefixFileName(read))
                && pathname.getName().contains(new StringBuffer(VALUE));
          }
        }));
  }

  //
  // Getter
  //

  /**
   * @return
   */
  public int getRead() {
    return this.read1;
  }

  /**
   * @return
   */
  public int getLane() {
    return this.lane;
  }

  /**
   * @return
   */
  public String getProjectName() {
    return this.projectName;
  }

  /**
   * @return
   */
  public String getSampleName() {
    return this.sampleName;
  }

  /**
   * @return
   */
  public List<File> getFastqFiles() {
    return this.fastqFiles;
  }

  public String getPrefixRead2() {
    return keyFastqSample.replaceFirst("R1", "R2");
  }

  /**
   * @return
   */
  public int getKeyFastqFiles() {
    return this.keyFastqFiles;
  }

  /**
   * @return
   */
  public String getNameTemporaryFastqFiles() {
    return this.nameTemporaryFastqFiles;
  }

  /**
   * @return
   */
  public String getKeyFastqSample() {
    return this.keyFastqSample;
  }

  /**
   * @return
   */
  public CompressionType getCompressionType() {
    return this.compressionType;
  }

  //
  // Constructor
  //

  /**
   * Public constructor corresponding of a technical replica sample
   * @param casavaOutputPath path to fastq files
   * @param read read number
   * @param lane lane number
   * @param sampleName name of the sample
   * @param projectName name of the project
   * @param index value of index or if doesn't exists, NoIndex
   */
  public FastqSample(final String casavaOutputPath, final int read,
      final int lane, final String sampleName, final String projectName,
      final String index) {

    // this.fastqStorage = FastqStorage.getInstance();
    this.read1 = (read == 3 ? 2 : read);
    // this.read2 = 0;

    this.lane = lane;
    this.sampleName = sampleName;
    this.projectName = projectName;
    this.index = index;

    this.runFastqPath = casavaOutputPath;

    // if (sampleName.equals("2012_0197"))
    this.fastqFiles = createListFastqFiles(read1);

    // else
    // this.fastqFiles = Collections.emptyList();

    // System.out.println("create fastqSample for "
    // + sampleName + "nb fastqFiles " + fastqFiles);

    if (fastqFiles.size() == 0) {
      this.keyFastqFiles = 0;
      this.keyFastqSample = "";
      this.nameTemporaryFastqFiles = null;
      this.compressionType = CompressionType.NONE;

    } else {

      this.compressionType = setCompressionExtension();
      // System.out.println("this.compressionType  " + this.compressionType);

      this.keyFastqSample = createKeyFastqSample();
      // System.out.println("this.keyFastqSample " + this.keyFastqSample);

      this.keyFastqFiles = createKeyFastqFiles();
      // System.out.println("this.keyFastqFiles  " + this.keyFastqFiles);

      this.nameTemporaryFastqFiles = createNameTemporaryFastqFile();
      // System.out.println("this.nameTemporaryFastqFiles  "
      // + this.nameTemporaryFastqFiles);

    }
  }
}
