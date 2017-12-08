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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.util.DockerCommand;
import fr.ens.biologie.genomique.eoulsan.util.SystemUtils;

/**
 * This class define a MultiQC collector.
 * @since 2.2
 * @author Laurent Jourdren
 */
public class MultiQCCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "multiqc";

  public static final String MULTIQC_DATA_PREFIX = "multiqc";

  private static final String MULTIQC_EXECUTABLE = "multiqc";
  private static final String MULTIQC_EXECUTABLE_DOCKER = "multiqc";
  private static final String MULTIQC_DOCKER_DEPOT = "ewels";
  private static final String MULTIQC_DOCKER_IMAGE = "multiqc";
  private static final String MULTIQC_VERSION_DOCKER = "v1.3";

  private File fastqDir;
  private File qcDir;
  private File tmpDir;

  private boolean useDocker;
  private String dockerConnectionString;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return Lists.newArrayList(RunInfoCollector.COLLECTOR_NAME,
        SamplesheetCollector.COLLECTOR_NAME,
        DemultiplexingCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final QC qc, final CollectorConfiguration conf) {

    this.fastqDir = qc.getFastqDir();
    this.qcDir = qc.getQcDir();
    this.tmpDir = qc.getTmpDir();

    // Test if Docker must be use to launch MultiQC
    this.useDocker = conf.getBoolean(Settings.QC_CONF_MULTIQC_USE_DOCKER_KEY);

    if (this.useDocker) {
      this.dockerConnectionString = conf.get(Settings.DOCKER_URI_KEY);
    }
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    List<File> inputRunDirectories = new ArrayList<>();

    // Add Bcl2fastq report
    File bcl2fastqReportDir = new File(this.fastqDir, "Stats");
    if (bcl2fastqReportDir.isDirectory()) {
      inputRunDirectories.add(bcl2fastqReportDir);
    }

    // Add FastQC and FastQC Screen reports
    for (int projectId : data.getProjects()) {

      List<File> inputDirectories = new ArrayList<>(inputRunDirectories);

      String projectName = data.getProjectName(projectId);
      File projectReportDir = new File(this.qcDir, "/Project_" + projectName);
      File multiQCReportFile =
          new File(projectReportDir, projectName + "-multiqc.html");

      // Create output directory if not exists
      if (!projectReportDir.isDirectory()) {
        if (!projectReportDir.mkdirs()) {
          throw new AozanException(
              "Unable to create project directory: " + projectReportDir);
        }
      }

      inputDirectories.add(projectReportDir);

      try {
        // Launch MultiQC
        if (this.useDocker) {
          createMultiQCReport(inputDirectories, multiQCReportFile, projectName);
        } else {
          createMultiQCReportWithDocker(inputDirectories, multiQCReportFile,
              projectName);
        }
      } catch (IOException e) {
        throw new AozanException(e);
      }

      // Add result entry
      data.put(MULTIQC_DATA_PREFIX + ".project" + projectId + ".report",
          multiQCReportFile.getAbsolutePath());

    }
  }

  /**
   * Create the MultiQC report.
   * @param inputDirectories input directories
   * @param multiQCReportFile output report
   * @param projectName project name
   * @throws IOException if an error occurs while creating the report
   */
  private void createMultiQCReport(final List<File> inputDirectories,
      final File multiQCReportFile, final String projectName)
      throws IOException {

    File multiQCExecutable =
        SystemUtils.searchExecutableInPATH(MULTIQC_EXECUTABLE);

    if (multiQCExecutable == null) {
      throw new IOException(
          "Unable to find \"" + MULTIQC_EXECUTABLE + "\" executable");
    }

    // Create command line
    final ProcessBuilder builder = new ProcessBuilder();
    builder.command().add(multiQCExecutable.getAbsolutePath());
    builder.command().addAll(
        createMultiQCOptions(inputDirectories, multiQCReportFile, projectName));
    builder.directory(multiQCReportFile.getParentFile());
    builder.environment().put("TMPDIR", this.tmpDir.getAbsolutePath());

    Common.getLogger()
        .fine("MultiQC: MultiQC command line: " + builder.command());

    // Execute command line
    try {
      final int exitValue = builder.start().waitFor();
      if (exitValue > 0) {
        throw new IOException("MultiQC: bad exit value: " + exitValue);
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  /**
   * Create the MultiQC report using docker.
   * @param inputDirectories input directories
   * @param multiQCReportFile output report
   * @param projectName project name
   * @throws IOException if an error occurs while creating the report
   */
  private void createMultiQCReportWithDocker(final List<File> inputDirectories,
      final File multiQCReportFile, final String projectName)
      throws IOException, AozanException {

    final List<String> cmd = new ArrayList<String>();
    cmd.add(MULTIQC_EXECUTABLE_DOCKER);
    cmd.addAll(
        createMultiQCOptions(inputDirectories, multiQCReportFile, projectName));

    DockerCommand dc = new DockerCommand(this.dockerConnectionString, cmd,
        MULTIQC_DOCKER_IMAGE, MULTIQC_VERSION_DOCKER);

    dc.setDepotDockerName(MULTIQC_DOCKER_DEPOT);

    // MultiQC input directories
    for (File d : inputDirectories) {
      dc.addMountDirectory(d.getAbsolutePath());
    }

    dc.addMountDirectory(multiQCReportFile.getParentFile().getAbsolutePath());

    Common.getLogger().fine("FASTQC: Blast command line: " + cmd);

    // Launch Docker container
    dc.run();
    final int exitValue = dc.getExitValue();
    if (exitValue > 0) {
      Common.getLogger().warning(
          "FastQC: fail of blastn process, exit value is : " + exitValue);
    }

  }

  /**
   * Creating MultiQC command line.
   * @param inputDirectories input directories
   * @param multiQCReportFile output report
   * @param projectName project name
   * @return a list with the MultiQC arguments
   */
  private List<String> createMultiQCOptions(final List<File> inputDirectories,
      final File multiQCReportFile, final String projectName) {

    List<String> result = new ArrayList<>();

    // MultiQC options
    result.add("--title");
    result.add("Project " + projectName + " report");
    result.add("--filename");
    result.add(multiQCReportFile.getAbsolutePath());

    // MultiQC input directories
    for (File f : inputDirectories) {
      result.add(f.getAbsolutePath());
    }

    return result;
  }

  @Override
  public void clear() {
  }

}
