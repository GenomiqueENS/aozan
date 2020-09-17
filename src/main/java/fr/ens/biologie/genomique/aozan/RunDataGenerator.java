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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan;

import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.toTimeHumanReadable;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;

import fr.ens.biologie.genomique.aozan.collectors.Collector;
import fr.ens.biologie.genomique.aozan.collectors.CollectorConfiguration;

/**
 * This Class collect Data.
 * @since 0.8
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
public class RunDataGenerator {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  /** Collect done property key. */
  private static final String COLLECT_DONE = "collect.done";

  private final List<Collector> collectors;
  private final Map<String, String> generatorsProperties;

  private final String runId;

  //
  // Others methods
  //

  /**
   * Collect data and return a RunData object.
   * @return a RunData object with all data about the run
   * @throws AozanException if an error occurs while collecting data
   */
  public RunData collect(final QC qc) throws AozanException {

    if (qc == null) {
      throw new NullPointerException("qc argument cannot be null");
    }

    final RunData data = new RunData();

    if (this.generatorsProperties.containsKey(COLLECT_DONE)) {
      throw new AozanException("Collect has been already done.");
    }

    if (!this.generatorsProperties.containsKey(QC.RTA_OUTPUT_DIR)) {
      throw new AozanException("RTA output directory is not set.");
    }

    if (!this.generatorsProperties.containsKey(QC.BCL2FASTQ_SAMPLESHEET_PATH)) {
      throw new AozanException("Bcl2fastq samplesheet file path is not set.");
    }

    if (!this.generatorsProperties.containsKey(QC.BCL2FASTQ_OUTPUT_DIR)) {
      throw new AozanException("Bcl2fastq output directory is not set.");
    }

    if (!this.generatorsProperties.containsKey(QC.QC_OUTPUT_DIR)) {
      throw new AozanException("QC output directory is not set.");
    }

    if (!this.generatorsProperties.containsKey(QC.TMP_DIR)) {
      throw new AozanException("Temporary directory is not set.");
    }

    // Timer
    final Stopwatch timerGlobal = Stopwatch.createStarted();

    LOGGER.info("Starting step collector");

    // For all collectors
    for (final Collector collector : this.collectors) {

      final Stopwatch timerCollector = Stopwatch.createStarted();
      LOGGER.info("Starting "
          + collector.getName().toUpperCase() + " collector for run "
          + this.runId);

      // Configure
      collector.configure(qc,
          new CollectorConfiguration(this.generatorsProperties));

      // And collect data
      collector.collect(data);

      LOGGER.info("Ended "
          + collector.getName().toUpperCase() + " collector for run "
          + this.runId + " in "
          + toTimeHumanReadable(timerCollector.elapsed(TimeUnit.MILLISECONDS)));

      final File qcDir =
          new File(this.generatorsProperties.get(QC.QC_OUTPUT_DIR));
      final File dataFile = new File(qcDir, collector.getName()
          + '-' + System.currentTimeMillis() + ".snapshot.data");

      LOGGER.fine("Writing rundata to " + dataFile);

      try {
        data.createRunDataFile(dataFile);
      } catch (IOException e) {
        throw new AozanException(e);
      }

    }

    for (final Collector collector : this.collectors) {
      collector.clear();
    }

    LOGGER.info("Step collector ended in "
        + toTimeHumanReadable(timerGlobal.elapsed(TimeUnit.MILLISECONDS)));
    timerGlobal.stop();

    this.generatorsProperties.put(COLLECT_DONE, "true");

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
      if (collector.isSummaryCollector()) {
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
    final String propertyValue = Joiner.on(", ").join(collectorNames);

    LOGGER.config("Collectors requiered for QC: " + propertyValue);

    // Update properties
    this.generatorsProperties.put(QC.QC_COLLECTOR_NAMES, propertyValue);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param runId run id
   */
  public RunDataGenerator(final List<Collector> collectors, final String runId,
      final Map<String, String> generatorsConf) {

    requireNonNull(collectors, "The list of collectors is null");
    requireNonNull(generatorsConf, "the generatorsConf argument cannot be null");

    this.collectors = reorderCollector(collectors);
    this.runId = runId;
    this.generatorsProperties = new HashMap<>(generatorsConf);

    // Add collector name requiered in properties
    addCollectorNameInProperties();
  }

}
