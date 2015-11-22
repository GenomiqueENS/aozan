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

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.collectors.stats.EntityStat;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenProjectReport;

/**
 * The class define a abstract statistics collector.
 * @author Sandrine Perrin
 * @since 2.0
 */
public abstract class StatisticsCollector implements Collector {

  /** Default contamination percent threshold. */
  private static final double DEFAULT_CONTAMINATION_PERCENT_THRESHOLD = 0.10;

  /** Report directory. */
  private File reportDir;

  /** Stylesheet xsl file. */
  private File fastqscreenXSLFile;

  /** Contamination threshold. */
  private double contaminationThreshold;

  private boolean undeterminedIndexesCollectorSelected = false;
  private boolean fastqScreenCollectorSelected = false;

  @Override
  public boolean isStatisticCollector() {
    return true;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    // UndeterminedIndexesCollector and FastqScreenCollector is optional for
    // this collector.
    // Use their data only if a test use them.

    return Lists.newArrayList(RunInfoCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME, DemultiplexingCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final QC qc, final Properties properties) {

    // Set control quality directory
    this.reportDir = qc.getQcDir();

    // Set stylesheet file to build project report
    try {
      final String filename = properties
          .getProperty(Settings.QC_CONF_FASTQSCREEN_PROJECT_XSL_FILE_KEY);
      if (new File(filename).exists()) {
        this.fastqscreenXSLFile = new File(filename);
      }
    } catch (final Exception e) {
      // Call default xsl file
      this.fastqscreenXSLFile = null;
    }

    // Extract threshold from property
    final String threshod = properties.getProperty(
        Settings.QC_CONF_FASTQSCREEN_PERCENT_CONTAMINATION_THRESHOLD_KEY);

    // Set the contaminant threshold
    if (threshod == null || threshod.isEmpty()) {
      // Use default threshold
      this.contaminationThreshold = DEFAULT_CONTAMINATION_PERCENT_THRESHOLD;
    } else {
      try {
        this.contaminationThreshold = Double.parseDouble(threshod);
      } catch (Exception e) {
        this.contaminationThreshold = DEFAULT_CONTAMINATION_PERCENT_THRESHOLD;
      }
    }

    // Check optional collector selected
    final List<String> collectorNames =
        Splitter.on(',').trimResults().omitEmptyStrings()
            .splitToList(properties.getProperty(QC.QC_COLLECTOR_NAMES));

    undeterminedIndexesCollectorSelected =
        collectorNames.contains(UndeterminedIndexesCollector.COLLECTOR_NAME);

    fastqScreenCollectorSelected =
        collectorNames.contains(FastqScreenCollector.COLLECTOR_NAME);

  }

  @Override
  public void clear() {

  }

  @Override
  public void collect(RunData data) throws AozanException {

    // Parse FastqSample to build list Project
    final List<EntityStat> stats = extractEntityStats(data);

    // Collect projects statistics in rundata
    for (EntityStat stat : stats) {
      data.put(stat.createRunDataProject());
    }

    try {
      // Build FastqScreen project report html
      createReport(stats);
    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Creates the projects report.
   * @param projects the projects
   * @throws AozanException the Aozan exception
   * @throws IOException if a error occurs when create report HTML.
   */
  private void createReport(final List<EntityStat> projects)
      throws AozanException, IOException {

    // Check FastqScreen collected
    if (!isFastqScreenCollectorSelected()) {
      // No selected, no data to create project report
      return;
    }

    for (EntityStat p : projects) {
      final FastqScreenProjectReport fpr =
          new FastqScreenProjectReport(p, fastqscreenXSLFile);

      fpr.createReport(p.getReportHtmlFile());

    }

  }

  //
  // Abstract methods
  //
  /**
   * Extract entity stats.
   * @param data the data
   * @return the list
   * @throws AozanException the aozan exception
   */
  public abstract List<EntityStat> extractEntityStats(final RunData data)
      throws AozanException;

  /**
   * Checks if is sample statistics collector.
   * @return true, if is sample statistics collector
   */
  public abstract boolean isSampleStatisticsCollector();

  /**
   * Checks if is project statistics collector.
   * @return true, if is project statistics collector
   */
  public abstract boolean isProjectStatisticsCollector();

  /**
   * Gets the collector prefix.
   * @return the collector prefix
   */
  public abstract String getCollectorPrefix();

  //
  // Getters
  //

  /**
   * Checks if is undetermined indexes collector selected.
   * @return true, if is undetermined indexes collector selected
   */
  public boolean isUndeterminedIndexesCollectorSelected() {
    return this.undeterminedIndexesCollectorSelected;
  }

  /**
   * Checks if is fastqscreen collector selected.
   * @return true, if is fastqscreen collector selected
   */
  public boolean isFastqScreenCollectorSelected() {
    return this.fastqScreenCollectorSelected;
  }

  /**
   * Gets the report directory.
   * @return the report directory
   */
  public File getReportDirectory() {
    return this.reportDir;
  }

  /**
   * Gets the contamination threshold.
   * @return the contamination threshold
   */
  public double getContaminationThreshold() {
    return contaminationThreshold;
  }

}
