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

package fr.ens.biologie.genomique.aozan.collectors;

import static fr.ens.biologie.genomique.aozan.io.FastqSample.UNDETERMINED_DIR_NAME;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.collectors.stats.EntityStat;
import fr.ens.biologie.genomique.aozan.fastqscreen.FastqScreenProjectReport;

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

  private Map<String, List<File>> fastqScreenReportFiles = new HashMap<>();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    // UndeterminedIndexesCollector and FastqScreenCollector is optional for
    // this collector.
    // Use their data only if a test use them.

    return Lists.newArrayList(RunInfoCollector.COLLECTOR_NAME,
        SamplesheetCollector.COLLECTOR_NAME,
        DemultiplexingCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final QC qc, final CollectorConfiguration conf) {

    // Set control quality directory
    this.reportDir = qc.getQcDir();

    // Set stylesheet file to build project report
    try {
      final File file =
          conf.getFile(Settings.QC_CONF_FASTQSCREEN_PROJECT_XSL_FILE_KEY);
      if (file != null && file.exists()) {
        this.fastqscreenXSLFile = file;
      }
    } catch (final Exception e) {
      // Call default xsl file
      this.fastqscreenXSLFile = null;
    }

    // Extract threshold from property
    this.contaminationThreshold = conf.getDouble(
        Settings.QC_CONF_FASTQSCREEN_PERCENT_PROJECT_CONTAMINATION_THRESHOLD_KEY,
        DEFAULT_CONTAMINATION_PERCENT_THRESHOLD);
  }

  @Override
  public void clear() {

  }

  @Override
  public void collect(RunData data) throws AozanException {

    // Parse FastqSample to build list Project
    final Map<Integer, EntityStat> stats = extractEntityStats(data);

    // Collect projects statistics in rundata
    for (Map.Entry<Integer, EntityStat> e : stats.entrySet()) {
      e.getValue().addRunDataProjectEntries(getCollectorPrefix() + e.getKey());
    }

    try {
      // Build FastqScreen project HTML report
      createReport(data, stats.values());
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
  private void createReport(final RunData data,
      final Collection<EntityStat> projects)
      throws AozanException, IOException {

    // Check FastqScreen collected
    if (!data.isCollectorEnabled(FastqScreenCollector.COLLECTOR_NAME)) {
      // No selected, no data to create project report
      return;
    }

    for (Map.Entry<String, List<File>> e : this.fastqScreenReportFiles
        .entrySet()) {

      final String[] fields = e.getKey().split("\t");
      final int projectId = Integer.parseInt(fields[0]);
      final int pooledSampleId = Integer.parseInt(fields[1]);
      final String description = "project "
          + data.getProjectName(projectId) + ", sample "
          + data.getSampleDemuxName(pooledSampleId);

      final FastqScreenProjectReport fpr = new FastqScreenProjectReport(
          e.getValue(), description, this.fastqscreenXSLFile);

      final File projectDir = (data.isUndeterminedSample(pooledSampleId)
          ? new File(this.reportDir, UNDETERMINED_DIR_NAME) : new File(
              this.reportDir + "/Project_" + data.getProjectName(projectId)));

      final File htmlReportFile;

      if (isSampleStatisticsCollector()) {
        htmlReportFile =
            new File(projectDir, data.getPooledSampleDemuxName(pooledSampleId)
                + "-fastqscreen.html");
      } else {
        htmlReportFile = new File(projectDir,
            data.getProjectName(projectId) + "-fastqscreen.html");
      }

      fpr.createReport(htmlReportFile);
    }
  }

  //
  // Protected methods
  //

  /**
   * Add a FastqScreen report.
   * @param projectId the projectId
   * @param sampleId the sampleId
   * @param reportFiles the list of report file
   */
  protected void addFastqScreenReport(final int projectId, final int sampleId,
      final List<File> reportFiles) {

    this.fastqScreenReportFiles.put(projectId + "\t" + sampleId, reportFiles);
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
  public abstract Map<Integer, EntityStat> extractEntityStats(
      final RunData data) throws AozanException;

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

  @Override
  public boolean isSummaryCollector() {
    return true;
  }

}
