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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.stats.ProjectStatistics;
import fr.ens.transcriptome.aozan.collectors.stats.SampleStatistics;

/**
 * This Class collect Data.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class RunDataGenerator {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  /** Collect done property key. */
  private static final String COLLECT_DONE = "collect.done";

  private final List<Collector> collectors;
  private final Properties properties = new Properties();

  private final String runId;

  /**
   * Set global configuration for collectors and tests.
   * @param conf global configuration object
   */
  public void setGlobalConf(final Map<String, String> conf) {

    if (conf == null) {
      return;
    }

    for (final Map.Entry<String, String> e : conf.entrySet()) {
      this.properties.setProperty(e.getKey(), e.getValue());
    }
  }

  //
  // Others methods
  //

  /**
   * Collect data and return a RunData object.
   * @return a RunData object with all data about the run
   * @throws AozanException if an error occurs while collecting data
   */
  public RunData collect() throws AozanException {

    final RunData data = new RunData();

    if (this.properties.containsKey(COLLECT_DONE)) {
      throw new AozanException("Collect has been already done.");
    }

    if (!this.properties.containsKey(QC.RTA_OUTPUT_DIR)) {
      throw new AozanException("RTA output directory is not set.");
    }

    if (!this.properties.containsKey(QC.CASAVA_DESIGN_PATH)) {
      throw new AozanException("Casava design file path is not set.");
    }

    if (!this.properties.containsKey(QC.CASAVA_OUTPUT_DIR)) {
      throw new AozanException("Casava output directory is not set.");
    }

    if (!this.properties.containsKey(QC.QC_OUTPUT_DIR)) {
      throw new AozanException("QC output directory is not set.");
    }

    if (!this.properties.containsKey(QC.TMP_DIR)) {
      throw new AozanException("Temporary directory is not set.");
    }

    // Timer
    final Stopwatch timerGlobal = Stopwatch.createStarted();

    LOGGER.info("Step collector start");

    // For all collectors
    for (final Collector collector : this.collectors) {

      final Stopwatch timerCollector = Stopwatch.createStarted();
      LOGGER.info(collector.getName().toUpperCase()
          + " start on run " + this.runId);

      // Configure
      collector.configure(new Properties(this.properties));

      // And collect data
      collector.collect(data);

      LOGGER.info(collector.getName().toUpperCase()
          + " end for run " + this.runId + " in "
          + toTimeHumanReadable(timerCollector.elapsed(TimeUnit.MILLISECONDS)));

    }

    for (final Collector collector : this.collectors) {
      collector.clear();
    }

    LOGGER.info("Step collector end in "
        + toTimeHumanReadable(timerGlobal.elapsed(TimeUnit.MILLISECONDS)));
    timerGlobal.stop();

    this.properties.setProperty(COLLECT_DONE, "true");

    return data;
  }

  /**
   * Adds the all collectors and change order per default to move
   * ProjectStatCollector at the end, if is selected.
   * @param collectorsInitOrder the collectors init order
   * @return the same list with new order
   */
  private List<Collector> reorderCollector(
      final List<Collector> collectorsInitOrder) {
    // Force ProjectCollector, must be the last

    final List<Collector> collectorsNewOrder = new ArrayList<>();

    final List<Collector> statisticsCollector = new ArrayList<>();

    for (final Collector collector : collectorsInitOrder) {

      // Collector selected
      // TODO to change
      if (collector.isStatisticCollector()) {
        statisticsCollector.add(collector);
      } else {
        collectorsNewOrder.add(collector);
      }
    }

    // Check ProjectCollector founded
    if (!statisticsCollector.isEmpty()) {
      collectorsNewOrder.addAll(statisticsCollector);
    }

    if (collectorsInitOrder.size() != collectorsNewOrder.size()) {
      throw new RuntimeException(
          "Reorder collector list, generate list with different size.\n\tinit list "
              + Joiner.on(",").join(collectorsInitOrder) + "\n\tnew order "
              + Joiner.on(",").join(collectorsNewOrder));
    }

    // Return list with new order
    return Collections.unmodifiableList(collectorsNewOrder);
  }

  private void addCollectorNameInProperties() {
    final List<String> collectorNames = new ArrayList<>();

    for (final Collector collector : this.collectors) {
      collectorNames.add(collector.getName());
    }

    // Compile collector names
    final String propertyValue = Joiner.on(",").join(collectorNames);

    LOGGER.config("Collectors requiered for QC " + propertyValue);

    // Update properties
    this.properties.setProperty(QC.QC_COLLECTOR_NAMES, propertyValue);

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param runId
   */
  public RunDataGenerator(final List<Collector> collectors, final String runId) {

    checkNotNull(collectors, "The list of collectors is null");

    this.collectors = reorderCollector(collectors);
    this.runId = runId;

    // Add collector name requiered in properties
    addCollectorNameInProperties();
  }

}
