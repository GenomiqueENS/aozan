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

package fr.ens.biologie.genomique.aozan.collectors.stats;

import static fr.ens.biologie.genomique.aozan.illumina.Bcl2FastqOutput.UNDETERMINED_DIR_NAME;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.StatisticsCollector;

/**
 * The class define a statistic collector on sample's data to build a project
 * summary table in qc report.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class SampleStatistics extends StatisticsCollector {

  /** Collector name. */
  public static final String COLLECTOR_NAME = "samplestats";

  /** Collector prefix for updating rundata */
  public static final String COLLECTOR_PREFIX = "samplestats.";

  public static final String UNDETERMINED_SAMPLE = "undetermined";

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  @Override
  public String getCollectorPrefix() {
    return COLLECTOR_PREFIX;
  }

  @Override
  public boolean isSampleStatisticsCollector() {
    return true;
  }

  @Override
  public boolean isProjectStatisticsCollector() {
    return false;
  }

  public List<EntityStat> extractEntityStats(final RunData data)
      throws AozanException {

    final int laneCount = data.getLaneCount();

    // Initialization ProjectStats with the project name
    final Map<String, EntityStat> samples = new HashMap<>();

    samples.put(UNDETERMINED_SAMPLE,
        new EntityStat(data, UNDETERMINED_SAMPLE, UNDETERMINED_SAMPLE, this,
            extractFastqscreenReportForUndeterminedSample()));

    // Add projects name
    for (int lane = 1; lane <= laneCount; lane++) {

      // Parse all samples in lane
      for (String sampleName : data.getSamplesNameListInLane(lane)) {

        if (!samples.containsKey(sampleName)) {

          // Extract project name related to sample name
          final String projectName = data.getProjectSample(lane, sampleName);

          samples.put(sampleName, new EntityStat(data, projectName, sampleName,
              this, extractFastqscreenReport(projectName, sampleName)));
        }

        // Update Statistics
        samples.get(sampleName).addEntity(lane, sampleName);
      }

      // Update Statistics on undetermined sample on each lane
      if (data.isUndeterminedInLane(lane)) {
        samples.get(UNDETERMINED_SAMPLE).addEntity(lane, "lane" + lane);
      }
    }

    // Sorted list project
    final List<EntityStat> samplesSorted =
        new ArrayList<>(samples.values());

    Collections.sort(samplesSorted);

    return Collections.unmodifiableList(samplesSorted);

  }

  /**
   * Extract fastqscreen xml report create for samples.
   * @return the list of xml file used to create project report.
   * @throws AozanException if the output project directory does not exist.
   */
  private List<File> extractFastqscreenReport(final String projectName,
      final String sampleName) throws AozanException {

    final List<File> reports = new ArrayList<>();

    if (projectName.equals(UNDETERMINED_SAMPLE)) {
      reports.addAll(extractFastqscreenReportForUndeterminedSample());
    } else {
      reports
          .addAll(extractFastqscreenReportOnProject(projectName, sampleName));
    }

    // Sort by filename
    Collections.sort(reports);

    return Collections.unmodifiableList(reports);
  }

  /**
   * Extract fastqscreen report on project.
   * @param projectName the project name
   * @param sampleName the sample name
   * @return the list
   */
  private List<File> extractFastqscreenReportOnProject(final String projectName,
      final String sampleName) {

    List<File> reports;
    final File projectDir =
        new File(this.getReportDirectory(), "/Project_" + projectName);

    // Extract in project directory all fastqscreen report xml
    reports = Arrays.asList(projectDir.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {
        return pathname.length() > 0
            && pathname.getName().startsWith(sampleName)
            && pathname.getName().endsWith("-fastqscreen.xml");
      }
    }));
    return reports;
  }

  /**
   * Extract fastqscreen report for undetermined sample.
   * @return the list
   * @throws AozanException the aozan exception
   */
  private List<File> extractFastqscreenReportForUndeterminedSample()
      throws AozanException {

    final File dir = new File(this.getReportDirectory(), UNDETERMINED_DIR_NAME);

    if (!dir.isDirectory()) {
      return Collections.emptyList();
    }

    // Extract in project directy all fastqscreen report xml
    final File[] files = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {

        return pathname.length() > 0
            && pathname.getName().endsWith("-fastqscreen.xml");
      }
    });

    if (files == null) {
      return Collections.emptyList();
    }

    return Collections.emptyList();
  }

  //
  // Public constructor
  //

  public SampleStatistics() {
  }

}
