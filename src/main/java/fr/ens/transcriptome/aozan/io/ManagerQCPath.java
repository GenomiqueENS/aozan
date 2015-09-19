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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingStandardFile;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetVersion2;

public class ManagerQCPath {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  // Singleton
  private static ManagerQCPath manager;

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

  /** The samplesheet instance. */
  private final SampleSheet samplesheet;

  /** The bcl2fastq version. */
  private final String bcl2fastqVersion;
  private final String bcl2fastqMajorVersion;

  /** The fastq directory. */
  private final File fastqDirectory;

  private final int laneCount;

  //
  // Get instance method
  //

  /**
   * Gets the single instance of ManagerQCPath.
   * @param globalConf the global configuration
   * @return single instance of ManagerQCPath
   * @throws AozanException if an error occurs when create instance during
   *           parsing sample sheet file
   */
  public static ManagerQCPath getInstance(final Map<String, String> globalConf)
      throws AozanException {

    if (manager == null) {

      final int laneCount = Integer.parseInt(globalConf.get(QC.LANE_COUNT));
      // Extract sample sheet file
      final File samplesheetFile =
          new File(globalConf.get(QC.CASAVA_DESIGN_PATH));

      // Extract sample sheet version
      final String samplesheetVersion = globalConf.get(QC.BCL2FASTQ_VERSION);

      // Extract fastq output directory
      final File fastqDir = new File(globalConf.get(QC.CASAVA_OUTPUT_DIR));

      // Return instance
      return getInstance(samplesheetFile, samplesheetVersion, fastqDir,
          laneCount);
    }

    return manager;
  }

  /**
   * Gets the single instance of ManagerQCPath.
   * @param samplesheetFilename the samplesheet file
   * @param bcl2fastqVersion the bcl2fasq version
   * @param fastqDir the fastq directory
   * @param laneCount the lane count
   * @return single instance of ManagerQCPath
   * @throws AozanException if an error occurs when create instance during
   *           parsing sample sheet file
   */
  public static ManagerQCPath getInstance(final File samplesheetFilename,
      final String bcl2fastqVersion, final File fastqDir, final int laneCount)
      throws AozanException {

    if (manager == null) {

      try {

        final String bcl2fastqMajorVersion =
            SampleSheetUtils.findBcl2fastqMajorVersion(bcl2fastqVersion);

        // Instance ManagerQCPath for sample sheet version 1
        if (SampleSheetUtils.isBcl2fastqVersion1(bcl2fastqMajorVersion)) {

          manager =
              new ManagerQCPath().new ManagerQCPathVersion1(
                  samplesheetFilename, bcl2fastqVersion, bcl2fastqMajorVersion,
                  fastqDir, laneCount);

        } else if (SampleSheetUtils.isBcl2fastqVersion2(bcl2fastqMajorVersion)) {

          // Instance ManagerQCPath for sample sheet version 2
          manager =
              new ManagerQCPath().new ManagerQCPathVersion2(
                  samplesheetFilename, bcl2fastqVersion, bcl2fastqMajorVersion,
                  fastqDir, laneCount);

        } else {
          throw new AozanException(
              "Can not possible to initialize ManagerQCPath, sample sheet version invalid "
                  + bcl2fastqVersion);
        }

      } catch (IOException e) {
        throw new AozanException(e.getMessage(), e);
      }
    }

    // Return instance of manager
    return manager;

  }

  /**
   * Gets the single instance of ManagerQCPath.
   * @param samplesheet the sample sheet
   * @param fastq the fastq directory
   * @return single instance of ManagerQCPath
   */
  public static ManagerQCPath getInstance(final SampleSheet samplesheet,
      final File fastq, final int laneCount) {

    if (manager == null) {

      // Extract sample sheet version
      final String version = samplesheet.getSampleSheetVersion();

      if (SampleSheetUtils.isBcl2fastqVersion1(version)) {
        // Instance ManagerQCPath for sample sheet version 1
        manager =
            new ManagerQCPath().new ManagerQCPathVersion1(samplesheet, fastq,
                laneCount);

      } else if (SampleSheetUtils.isBcl2fastqVersion1(version)) {
        // Instance ManagerQCPath for sample sheet version 2
        manager =
            new ManagerQCPath().new ManagerQCPathVersion2(samplesheet, fastq,
                laneCount);
      }
    }

    // Return instance manager
    return manager;

  }

  /**
   * Gets the single instance of ManagerQCPath.
   * @return single instance of ManagerQCPath
   */
  public static ManagerQCPath getInstance() {

    if (manager == null) {
      throw new AozanRuntimeException("Manager QC path not initialized yet.");
    }

    return manager;
  }

  // TODO method for test
  public static void destroyInstance() {
    manager = null;
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

  //
  // Getters
  //

  /**
   * @return the bcl2fastqVersion
   */
  public String getBcl2fastqVersion() {
    return bcl2fastqVersion;
  }

  /**
   * @return the bcl2fastqMajorVersion
   */
  public String getBcl2fastqMajorVersion() {
    return bcl2fastqMajorVersion;
  }

  /**
   * @return the lane count
   */
  public int getLaneCount() {
    return this.laneCount;
  }

  /**
   * @return the fastqDirectory
   */
  public File getFastqDirectory() {
    return fastqDirectory;
  }

  //
  // Methods to get path file or directory
  //

  /**
   * Find fastq output directory.
   * @param fastSample the fast sample
   * @return the fastq output directory.
   */
  public File casavaOutputDir(final FastqSample fastSample) {

    return manager.casavaOutputDir(fastSample);
  }

  /**
   * Set the prefix of the fastq file of read for a fastq on a sample.
   * @return prefix fastq files for this fastq on asample
   */
  public String prefixFileName(final FastqSample fastqSample, final int read) {

    return manager.prefixFileName(fastqSample, read);
  }

  /**
   * Build the prefix report filename.
   * @param fastqSample the fastq sample
   * @param read the read number
   * @return the prefix report filename
   */
  public String buildPrefixReport(final FastqSample fastqSample, final int read) {

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
   * @return an array of abstract pathnames
   */
  public List<File> createListFastqFiles(final FastqSample fastqSample,
      final int read) {

    return Arrays.asList(new File(casavaOutputDir(fastqSample) + "/")
        .listFiles(new FileFilter() {

          @Override
          public boolean accept(final File pathname) {

            return pathname.length() > 0
                && pathname.getName().startsWith(
                    manager.prefixFileName(fastqSample, read))
                && pathname.getName().contains(FASTQ_EXTENSION);
          }
        }));
  }

  //
  // Private constructor
  //

  /**
   * Instantiates a new manager qc path.
   * @param samplesheet the sample sheet file
   * @param bcl2fastqVersion the bcl2fastq full version name
   * @param bcl2fastqMajorVersion the bcl2fastq major version name
   * @throws AozanException if an error occurs during reading sample sheet file
   * @throws IOException if an error occurs during reading sample sheet file
   * @throws FileNotFoundException if sample sheet file not exist
   */
  private ManagerQCPath(final File samplesheet, final String bcl2fastqVersion,
      final String bcl2fastqMajorVersion, final int laneCount)
      throws FileNotFoundException, IOException, AozanException {

    this(samplesheet, bcl2fastqVersion, bcl2fastqMajorVersion, samplesheet
        .getParentFile(), laneCount);
  }

  /**
   * Instantiates a new manager qc path.
   * @param samplesheet the samplesheet
   * @param bcl2fastqVersion the bcl2fastq full version name
   * @param bcl2fastqMajorVersion the bcl2fastq major version name
   * @param fastq the fastq
   * @throws AozanException if an error occurs during reading sample sheet file
   * @throws IOException if an error occurs during reading sample sheet file
   * @throws FileNotFoundException if sample sheet file not exist
   */
  private ManagerQCPath(final File samplesheet, final String bcl2fastqVersion,
      final String bcl2fastqMajorVersion, final File fastq, final int laneCount)
      throws FileNotFoundException, IOException, AozanException {

    checkExistingStandardFile(samplesheet, "sample sheet");
    checkExistingDirectoryFile(fastq, "fastq directory");

    this.samplesheet =
        SampleSheetUtils.getSampleSheet(samplesheet, bcl2fastqMajorVersion,
            laneCount);

    this.bcl2fastqVersion = bcl2fastqVersion;
    this.bcl2fastqMajorVersion = bcl2fastqMajorVersion;
    this.fastqDirectory = fastq;
    this.laneCount = laneCount;
  }

  /**
   * Instantiates a new manager qc path.
   * @param samplesheet the samplesheet
   * @param fastq the fastq
   */
  private ManagerQCPath(final SampleSheet samplesheet, final File fastq,
      final int laneCount) {

    this.samplesheet = samplesheet;
    this.bcl2fastqVersion = this.samplesheet.getSampleSheetVersion();
    this.bcl2fastqMajorVersion =
        SampleSheetUtils.findBcl2fastqMajorVersion(this.bcl2fastqVersion);

    this.fastqDirectory = fastq;
    this.laneCount = laneCount;
  }

  /**
   * Default constructor
   */
  private ManagerQCPath() {

    this.bcl2fastqVersion = null;
    this.bcl2fastqMajorVersion = null;
    this.samplesheet = null;
    this.fastqDirectory = null;
    this.laneCount = -1;
  }

  //
  // Internal class
  //

  /**
   * The class instance a instance on ManagerQCPath which manage sample sheet
   * and fastq directory from version 1.
   * @author Sandrine Perrin
   * @since 2.0
   */
  final class ManagerQCPathVersion1 extends ManagerQCPath {

    /**
     * Set the directory to the fastq files for this fastqSample.
     * @return directory of fastq files for a fastqSample
     */
    public File casavaOutputDir(final FastqSample fastSample) {

      if (fastSample.isIndeterminedIndices()) {
        return new File(getFastqDirectory()
            + "/" + UNDETERMINED_DIR_NAME + "/" + UNDETERMINED_PREFIX
            + fastSample.getLane());
      }

      return new File(getFastqDirectory()
          + "/" + PROJECT_PREFIX + fastSample.getProjectName() + "/"
          + SAMPLE_PREFIX + fastSample.getSampleName());
    }

    /**
     * Set the prefix of the fastq file of read1 for this fastqSample.
     * @return prefix fastq files for this fastqSample
     */
    public String prefixFileName(final FastqSample fastqSample, final int read) {

      if (fastqSample.isIndeterminedIndices()) {

        return String.format("lane%d_Undetermined%s", fastqSample.getLane(),
            getConstantFastqSuffix(fastqSample.getLane(), read));
      }

      return String.format("%s_%s%s", fastqSample.getSampleName(),
          fastqSample.getIndex(),
          getConstantFastqSuffix(fastqSample.getLane(), read));
    }

    //
    // Private constructor
    //

    /**
     * Private constructor a new manager qc path.
     * @param samplesheet the sample sheet file
     * @param bcl2fastqVersion the bcl2fastq full version name
     * @param bcl2fastqMajorVersion the bcl2fastq major version name
     * @param fastqDir the fastq directory
     * @param laneCount the lane count
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws AozanException the aozan exception
     */
    private ManagerQCPathVersion1(final File samplesheet,
        final String bcl2fastqVersion, final String bcl2fastqMajorVersion,
        final File fastqDir, final int laneCount) throws FileNotFoundException,
        IOException, AozanException {

      super(samplesheet, bcl2fastqVersion, bcl2fastqMajorVersion, fastqDir,
          laneCount);

    }

    /**
     * Private constructor a new manager qc path version1.
     * @param samplesheet the sample sheet file
     * @param fastq the fastq directory
     */
    private ManagerQCPathVersion1(final SampleSheet samplesheet,
        final File fastq, final int laneCount) {
      super(samplesheet, fastq, laneCount);
    }
  }

  /**
   * The class instance a instance on ManagerQCPath which manage sample sheet
   * and fastq directory from version 2.
   * @author Sandrine Perrin
   * @since 2.0
   */
  final class ManagerQCPathVersion2 extends ManagerQCPath {

    /** The sample sheet version2. */
    private final SampleSheetVersion2 sampleSheetV2;

    /**
     * Set the directory to the fastq files for this fastqSample.
     * @return directory of fastq files for a fastqSample
     */
    public File casavaOutputDir(final FastqSample fastSample) {

      if (fastSample.isIndeterminedIndices()) {
        return getFastqDirectory();
      }

      return new File(getFastqDirectory() + "/" + fastSample.getProjectName());
    }

    /**
     * Set the prefix of the fastq file of read1 for this fastqSample.
     * @return prefix fastq files for this fastqSample
     */
    public String prefixFileName(final FastqSample fastqSample, final int read) {

      if (fastqSample.isIndeterminedIndices()) {

        return String.format("Undetermined_S0%s",
            getConstantFastqSuffix(fastqSample.getLane(), read));
      }

      checkNotNull(this.sampleSheetV2,
          "sample sheet on version 2 instance not initialize.");

      // Build sample name on fastq file according to version used
      final String fastqSampleName = buildFastqSampleName(fastqSample);

      return String.format("%s_S%d%s", fastqSampleName,
          this.sampleSheetV2.extractOrderNumberSample(fastqSample),
          getConstantFastqSuffix(fastqSample.getLane(), read));

    }

    private String buildFastqSampleName(final FastqSample fastqSample) {

      // With bcl2fastq version 2.15 and 2.16, in sample name
      if (sampleSheetV2.getSampleSheetVersion().startsWith("2.15")
          || sampleSheetV2.getSampleSheetVersion().startsWith("2.16")) {

        return fastqSample.getSampleName().replace("_", "-");
      }

      return fastqSample.getSampleName();
    }

    //
    // Private constructor
    //

    /**
     * Private constructor a new manager qc path.
     * @param samplesheet the sample sheet file
     * @param bcl2fastqVersion the bcl2fastq full version name
     * @param bcl2fastqMajorVersion the bcl2fastq major version name
     * @param fastqDir the fastq directory
     * @param laneCount the lane count
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws AozanException the Aozan exception
     */
    private ManagerQCPathVersion2(final File samplesheet,
        final String bcl2fastqVersion, final String bcl2fastqMajorVersion,
        final File fastqDir, final int laneCount) throws FileNotFoundException,
        IOException, AozanException {

      super(samplesheet, bcl2fastqVersion, bcl2fastqMajorVersion, fastqDir,
          laneCount);

      this.sampleSheetV2 =
          (SampleSheetVersion2) SampleSheetUtils.getSampleSheet(samplesheet,
              bcl2fastqVersion, laneCount);

    }

    /**
     * Private constructor a new manager qc path version2.
     * @param samplesheet the sample sheet file
     * @param fastqDir the fastq directory
     * @param laneCount the lane count
     */
    private ManagerQCPathVersion2(final SampleSheet samplesheet,
        final File fastqDir, final int laneCount) {

      super(samplesheet, fastqDir, laneCount);

      this.sampleSheetV2 = (SampleSheetVersion2) samplesheet;
    }
  }

}
