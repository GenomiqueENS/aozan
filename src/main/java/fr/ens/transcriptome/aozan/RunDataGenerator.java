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

package fr.ens.transcriptome.aozan;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.collectors.Collector;

/**
 * This Class collect Data.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class RunDataGenerator {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  /** Collect done property key. */
  private static final String COLLECT_DONE = "collect.done";

  private final List<Collector> collectors = Lists.newArrayList();
  private final Properties properties = new Properties();

  /**
   * Set global configuration for collectors and tests.
   * @param conf global configuration object
   */
  public void setGlobalConf(final Map<String, String> conf) {

    if (conf == null)
      return;

    for (Map.Entry<String, String> e : conf.entrySet())
      properties.setProperty(e.getKey(), e.getValue());
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

    if (!this.properties.containsKey(QC.RTA_OUTPUT_DIR))
      throw new AozanException("RTA output directory is not set.");

    if (!this.properties.containsKey(QC.CASAVA_DESIGN_PATH))
      throw new AozanException("Casava design file path is not set.");

    if (!this.properties.containsKey(QC.CASAVA_OUTPUT_DIR))
      throw new AozanException("Casava output directory is not set.");

    if (!this.properties.containsKey(QC.QC_OUTPUT_DIR))
      throw new AozanException("QC output directory is not set.");

    if (!this.properties.containsKey(QC.TMP_DIR))
      throw new AozanException("Temporary directory is not set.");

    // Timer
    final Stopwatch timerGlobal = new Stopwatch().start();

    LOGGER.fine("Step collector start");

    // For all collectors
    for (final Collector collector : this.collectors) {

      Stopwatch timerCollector = new Stopwatch().start();
      LOGGER.fine(collector.getName().toUpperCase() + " start");

      // Configure
      collector.configure(new Properties(this.properties));

      // And collect data
      collector.collect(data);

      LOGGER.fine(collector.getName().toUpperCase()
          + " end in "
          + toTimeHumanReadable(timerCollector.elapsed(TimeUnit.MILLISECONDS)));

    }

    // for (final Collector collector : this.collectors) {
    // collector.clear();
    // }

    LOGGER.fine("Step collector end in "
        + toTimeHumanReadable(timerGlobal.elapsed(TimeUnit.MILLISECONDS)));
    timerGlobal.stop();

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
