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

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.StatisticsCollector;

/**
 * The class define a statistic collector on project's data to build a project
 * summary table in qc report.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ProjectStatisticsCollector extends StatisticsCollector {

  // TODO Check the RunData keys names

  /** Collector name. */
  public static final String COLLECTOR_NAME = "projectstats";

  /** Collector prefix for updating rundata. */
  public static final String COLLECTOR_PREFIX = "projectstats";

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  @Override
  public String getCollectorPrefix() {
    return COLLECTOR_PREFIX + ".project";
  }

  @Override
  public boolean isSampleStatisticsCollector() {
    return false;
  }

  @Override
  public boolean isProjectStatisticsCollector() {
    return true;
  }

  @Override
  public Map<Integer, EntityStat> extractEntityStats(final RunData data)
      throws AozanException {

    // Initialization ProjectStats with the project name
    final Map<Integer, EntityStat> projects = initMap(data);

    for (int projectId : data.getProjects()) {

      for (int sampleId : data.getSamplesInProject(projectId)) {
        // Save new sample in related project
        projects.get(projectId).addEntity(sampleId);
      }
    }

    return projects;
  }

  /**
   * Initialization ProjectStat object.
   * @param data the data
   * @return the map
   * @throws AozanException the aozan exception
   */
  private Map<Integer, EntityStat> initMap(final RunData data)
      throws AozanException {

    final Map<Integer, EntityStat> projects =
        new TreeMap<>(new RunData.ProjectComparator(data));

    // Add projects
    for (int projectId : data.getProjects()) {

      projects.put(projectId, new EntityStat(data, projectId, this));

      addFastqScreenReport(projectId, -1,
          extractFastqscreenReport(data.getProjectName(projectId)));
    }

    return Collections.unmodifiableMap(projects);
  }

  /**
   * Extract fastqscreen xml report create for samples.
   * @param projectName the project name
   * @return the list of xml file used to create project report.
   * @throws AozanException if the output project directory does not exist.
   */
  private List<File> extractFastqscreenReport(final String projectName)
      throws AozanException {

    final File projectDir =
        new File(this.getReportDirectory(), "/Project_" + projectName);

    if (!projectDir.exists())
      throw new AozanException(
          "Project directory does not exist " + projectDir.getAbsolutePath());

    // Extract in project directory all fastqscreen report xml
    final List<File> reports =
        Arrays.asList(projectDir.listFiles(new FileFilter() {

          @Override
          public boolean accept(final File pathname) {
            return pathname.length() > 0
                && pathname.getName().endsWith("-fastqscreen.xml");
          }
        }));

    // Sort by filename
    Collections.sort(reports);

    return Collections.unmodifiableList(reports);
  }

  //
  // Public constructor
  //

  /**
   * Instantiates a new project statistics.
   */
  public ProjectStatisticsCollector() {
  }
}
