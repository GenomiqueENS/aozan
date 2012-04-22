/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.collectors.Collector;

/**
 * This Class collect Data.
 * @author Laurent Jourdren
 */
public class RunDataGenerator {

  /** RTA output directory property key. */
  public static final String RTA_OUTPUT_DIR = "rta.output.dir";

  /** Casava design path property key. */
  public static final String CASAVA_DESIGN_PATH = "casava.design.path";

  /** Casava output directory property key. */
  public static final String CASAVA_OUTPUT_DIR = "casava.output.dir";

  /** QC output directory property key. */
  public static final String QC_OUTPUT_DIR = "qc.output.dir";

  /** Temporary directory property key. */
  public static final String TMP_DIR = "tmp.dir";

  /** Collect done property key. */
  private static final String COLLECT_DONE = "collect.done";

  private final List<Collector> collectors = Lists.newArrayList();
  private final Properties properties = new Properties();

  //
  // Setters
  //

  /**
   * Set the RTA output directory.
   * @param RTAOutputDir the RTA output directory
   */
  public void setRTAOutputDir(final File RTAOutputDir) {

    checkNotNull(RTAOutputDir, "RTA output directory is null");

    properties.setProperty(RTA_OUTPUT_DIR, RTAOutputDir.getAbsolutePath());
  }

  /**
   * Set the Casava design file path.
   * @param casavaDesignFile the Casava design path
   */
  public void setCasavaDesignFile(final File casavaDesignFile) {

    checkNotNull(casavaDesignFile, "Casava design file is null");
    properties.setProperty(CASAVA_DESIGN_PATH,
        casavaDesignFile.getAbsolutePath());
  }

  /**
   * Set the Casava output directory.
   * @param casavaOutputDir the Casava output directory
   */
  public void setCasavaOutputDir(final File casavaOutputDir) {

    checkNotNull(casavaOutputDir, "Casava output directory is null");
    properties
        .setProperty(CASAVA_OUTPUT_DIR, casavaOutputDir.getAbsolutePath());
  }

  /**
   * Set the QC output directory.
   * @param QCOutputDir the QC output directory
   */
  public void setQCOutputDir(final File QCOutputDir) {

    checkNotNull(QCOutputDir, "QC output directory is null");
    properties.setProperty(QC_OUTPUT_DIR, QCOutputDir.getAbsolutePath());
  }

  /**
   * Set the temporary directory.
   * @param tmpDir the temporary output directory
   */
  public void setTemporaryDir(final File tmpDir) {

    checkNotNull(tmpDir, "Temporary directory is null");
    properties.setProperty(TMP_DIR, tmpDir.getAbsolutePath());
  }

  //
  // Others methods
  //

  /**
   * Collect data and return a RunData object
   * @return a RunData object with all informations about the run
   * @throws AozanException if an error occurs while collecting data
   */
  public RunData collect() throws AozanException {

    final RunData data = new RunData();

    if (this.properties.containsKey(COLLECT_DONE))
      throw new AozanException("Collect has been already done.");

    if (!this.properties.containsKey(RTA_OUTPUT_DIR))
      throw new AozanException("RTA output directory is not set.");

    if (!this.properties.containsKey(CASAVA_DESIGN_PATH))
      throw new AozanException("Casava design file path is not set.");

    if (!this.properties.containsKey(CASAVA_OUTPUT_DIR))
      throw new AozanException("Casava output directory is not set.");

    if (!this.properties.containsKey(QC_OUTPUT_DIR))
      throw new AozanException("QC output directory is not set.");

    if (!this.properties.containsKey(TMP_DIR))
      throw new AozanException("Temporary directory is not set.");

    // For all collectors
    for (final Collector collector : this.collectors) {

      // Configure
      collector.configure(new Properties(this.properties));

      // And collect data
      collector.collect(data);
    }

    this.properties.setProperty(COLLECT_DONE, "true");

    return data;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public RunDataGenerator(final List<Collector> collectors) {

    checkNotNull(collectors, "The list of collectors is null");

    this.collectors.addAll(collectors);
  }

}
