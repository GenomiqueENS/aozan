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

  private final File RTAOutputDir;
  private final File casavaDesignFile;
  private final File casavaOutputDir;
  private final List<Collector> collectors = Lists.newArrayList();

  /**
   * Collect data and return a RunData object
   * @return a RunData object with all informations about the run
   * @throws AozanException if an error occurs while collecting data
   */
  public RunData collect() throws AozanException {

    final RunData data = new RunData();

    final Properties properties = new Properties();
    properties.setProperty(RTA_OUTPUT_DIR, this.RTAOutputDir.getAbsolutePath());
    properties.setProperty(CASAVA_DESIGN_PATH,
        this.casavaDesignFile.getAbsolutePath());
    properties.setProperty(CASAVA_OUTPUT_DIR,
        this.casavaOutputDir.getAbsolutePath());

    // For all collectors
    for (final Collector collector : this.collectors) {

      // Configure
      collector.configure(new Properties(properties));

      // And collect data
      collector.collect(data);
    }

    return data;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param RTAOutputDir
   * @param casavaDesignFile
   * @param casavaOutputDir
   */
  public RunDataGenerator(final File RTAOutputDir, final File casavaDesignFile,
      final File casavaOutputDir, final List<Collector> collectors) {

    checkNotNull(RTAOutputDir, "RTA output directory is null");
    checkNotNull(casavaDesignFile, "Casava design file is null");
    checkNotNull(casavaOutputDir, "Casava output directory is null");
    checkNotNull(collectors, "The list of collectors is null");

    this.RTAOutputDir = RTAOutputDir;
    this.casavaDesignFile = casavaDesignFile;
    this.casavaOutputDir = casavaOutputDir;
    this.collectors.addAll(collectors);
  }

}
