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

import fr.ens.transcriptome.aozan.AozanRuntimeException;

public class ManagerQCPath {

  // Singleton
  private static ManagerQCPath manager;

  /** The samplesheet file. */
  private final File samplesheet;

  /** The bcl2fasq version. */
  private final String bcl2fastqVersion;

  //
  // Get instance method
  //

  /**
   * Gets the single instance of ManagerQCPath.
   * @param samplesheet the samplesheet file
   * @param bcl2fastqVersion the bcl2fasq version
   * @return single instance of ManagerQCPath
   */
  public static ManagerQCPath getInstance(final File samplesheet,
      final String bcl2fastqVersion) {

    if (manager == null) {
      manager = new ManagerQCPath(samplesheet, bcl2fastqVersion);
    }

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

  //
  // Methods to get path file or directory
  //

  //
  // Private constructor
  //
  /**
   * Instantiates a new manager qc path.
   * @param samplesheet the samplesheet file
   * @param bcl2fastqVersion the bcl2fasq version
   */
  private ManagerQCPath(final File samplesheet, final String bcl2fastqVersion) {

    this.samplesheet = samplesheet;
    this.bcl2fastqVersion = bcl2fastqVersion;
  }
}
