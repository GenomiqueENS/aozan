package fr.ens.transcriptome.aozan.illumina;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;

import fr.ens.transcriptome.aozan.AozanRuntimeException;

public class Bcl2FastqUtils {

  /** The Constant LATEST_VERSION_NAME. */
  public static final String LATEST_VERSION_NAME = "latest";

  /** The Constant VERSION_1. */
  public static final int VERSION_1 = 1;

  /** The Constant VERSION_2. */
  public static final int VERSION_2 = 2;

  /**
   * Find bcl2fastq version.
   * @param fullVersion the full version
   * @return the string
   */
  public static int findBcl2fastqMajorVersion(final String fullVersion) {

    checkArgument(!Strings.isNullOrEmpty(fullVersion),
        "bcl2fastq full version name: " + fullVersion);

    if (fullVersion.startsWith("" + VERSION_1))
      return VERSION_1;

    if (fullVersion.startsWith("" + VERSION_2)
        || fullVersion.startsWith(LATEST_VERSION_NAME))

      return VERSION_2;

    // Throw an exception version invalid for pipeline
    throw new AozanRuntimeException(
        "Demultiplexing collector, can be recognize bcl2fastq version (not start with 1 or 2 or latest) : "
            + fullVersion);
  }

  /**
   * Checks if is bcl2fastq version1.
   * @param version the version
   * @return true, if is bcl2fastq version1
   */
  public static boolean isBcl2fastqVersion1(final String version) {

    // Check it is a full version name or major
    final int majorVersion = (version.indexOf(".") > 0
        ? findBcl2fastqMajorVersion(version) : Integer.parseInt(version));

    return majorVersion == VERSION_1;
  }

  /**
   * Checks if is bcl2fastq version2.
   * @param version the version
   * @return true, if is bcl2fastq version2
   */
  public static boolean isBcl2fastqVersion2(final String version) {

    if (LATEST_VERSION_NAME.equals(version)) {
      return true;
    }

    // Check it is a full version name or major
    final int majorVersion = (version.indexOf(".") > 0
        ? findBcl2fastqMajorVersion(version) : Integer.parseInt(version));

    return majorVersion == VERSION_2;
  }

  //
  // Constructor
  //

  private Bcl2FastqUtils() {
  }

}
