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
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.aozan.Aozan2Logger;
import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.kenetre.util.SystemUtils;
import fr.ens.biologie.genomique.kenetre.util.process.DockerImageInstance;
import fr.ens.biologie.genomique.kenetre.util.process.DockerManager;
import fr.ens.biologie.genomique.kenetre.util.process.DockerManager.ClientType;
import fr.ens.biologie.genomique.kenetre.util.process.SimpleProcess;
import fr.ens.biologie.genomique.kenetre.util.process.SystemSimpleProcess;

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
  private static final String MULTIQC_EXECUTABLE_DOCKER = "";
  private static final String MULTIQC_DOCKER_IMAGE = "ewels/multiqc:v1.12";

  private static final Logger LOGGER = Aozan2Logger.getLogger();

  private File fastqDir;
  private File qcDir;
  private File tmpDir;
  private String dockerImage;
  private String dockerExecutable = MULTIQC_EXECUTABLE_DOCKER;

  private boolean useDocker;
  private String multiQCPath;

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
    this.dockerImage =
        conf.get("qc.conf.multiqc.docker.image", MULTIQC_DOCKER_IMAGE);
    this.dockerExecutable = conf.get("qc.conf.multiqc.docker.executable",
        MULTIQC_EXECUTABLE_DOCKER);
    this.multiQCPath = conf.get("qc.conf.multiqc.path", null);

    // Test if Docker must be use to launch MultiQC
    this.useDocker =
        conf.getBoolean(Settings.QC_CONF_MULTIQC_USE_DOCKER_KEY, false);

    LOGGER.info("MultiQC, use Docker: " + this.useDocker);
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

      // Launch MultiQC
      try {
        runMultiQC(this.useDocker, inputDirectories, multiQCReportFile,
            projectName);
      } catch (IOException e) {
        throw new AozanException(e);
      }

      // Add result entry
      data.put(MULTIQC_DATA_PREFIX + ".project" + projectId + ".report",
          multiQCReportFile.getAbsolutePath());

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
    result.add("'Project " + projectName + " report'");
    result.add("--filename");
    result.add(multiQCReportFile.getAbsolutePath());

    // MultiQC input directories
    for (File f : inputDirectories) {
      result.add(f.getAbsolutePath());
    }

    return result;
  }

  private void runMultiQC(boolean docker, final List<File> inputDirectories,
      final File multiQCReportFile, final String projectName)
      throws IOException {

    SimpleProcess process;
    String executablePath;

    if (docker) {

      DockerImageInstance instance = DockerManager
          .getInstance(ClientType.FALLBACK,
              URI.create("unix:///var/run/docker.sock"),
              Aozan2Logger.getGenericLogger())
          .createImageInstance(this.dockerImage);

      instance.pullImageIfNotExists();

      executablePath = this.dockerExecutable;
      process = instance;

    } else {

      File multiQCExecutable = this.multiQCPath != null
          ? new File(this.multiQCPath)
          : SystemUtils.searchExecutableInPATH(MULTIQC_EXECUTABLE);

      if (multiQCExecutable == null
          || !Files.isExecutable(multiQCExecutable.toPath())) {
        throw new IOException(
            "Unable to find \"" + MULTIQC_EXECUTABLE + "\" executable");
      }

      executablePath = multiQCExecutable.getAbsolutePath();
      process = new SystemSimpleProcess();
    }

    final List<String> commandLine = new ArrayList<String>();
    if (!executablePath.isEmpty()) {
      commandLine.add(executablePath);
    }
    commandLine.addAll(
        createMultiQCOptions(inputDirectories, multiQCReportFile, projectName));

    List<File> filesUsed = new ArrayList<>(inputDirectories);
    filesUsed.add(multiQCReportFile.getParentFile());
    filesUsed.add(this.tmpDir);

    LOGGER.fine("FASTQC: MultiQC command line: " + commandLine);

    File stdout = new File(multiQCReportFile.getParentFile(),
        multiQCReportFile.getName() + ".out");
    File stderr = new File(multiQCReportFile.getParentFile(),
        multiQCReportFile.getName() + ".err");

    // Launch Docker container
    int exitValue =
        process.execute(commandLine, multiQCReportFile.getParentFile(),
            this.tmpDir, stdout, stderr, filesUsed.toArray(new File[0]));

    if (exitValue > 0) {
      Aozan2Logger.getLogger().warning(
          "FastQC: fail of blastn process, exit value is : " + exitValue);
    }

  }

  @Override
  public void clear() {
  }

  @Override
  public boolean isSummaryCollector() {
    return true;
  }

}
