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
package fr.ens.transcriptome.aozan.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.LocalEoulsanRuntime.initEoulsanRuntimeForExternalApp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.base.Joiner;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

public class DockerUtils {

  private static final char SEPARATOR = File.separatorChar;

  private static final String DEPOT_DEFAULT = "genomicpariscentre";

  private static final String DEFAULT_VERSION = "latest";
  private static final String WORK_DIRECTORY_DOCKER_DEFAULT = "/root";

  private final String imageDockerVersion;
  private final String imageDockerName;
  private final String commandLine;
  private final List<String> mountArgument = new ArrayList<>();

  private String permission;
  private String depotPublicName;
  private String workDirectoryDocker;

  private int exitValue = -1;
  private Throwable exception = null;

  /**
   * Run image Docker with command line.
   * @throws AozanException
   */
  public void run() throws AozanException {

    // TOTO
    checkOSCompatibilityDocker();

    try {
      final String imageName = buildImageName();

      final DockerClient docker = init(imageName);

      final HostConfig hostConfig = configure();

      System.out.println(" * Create container");
      final String id = createContainer(docker, imageName);
      System.out.println("id: " + id);

      // Inspect container
      System.out.println(" * Inspect container");
      final ContainerInfo info = docker.inspectContainer(id);
      System.out.println("info: " + info);

      // Start container
      System.out.println(" * Start container");
      docker.startContainer(id, hostConfig);

      // Kill container
      System.out.println(" * Wait end of container container");
      final ContainerExit ce = docker.waitContainer(id);
      System.out.println(ce);
      this.exitValue = ce.statusCode();

      // TODO
      if (exitValue == 0)
        System.out.println("SUCCESS run docker " + docker.info());
      else
        System.out.println("FAIL run docker " + docker.info());

      // TODO check end docker image

      // Remove container
      System.out.println(" * Remove container");
      docker.removeContainer(id);

      // Close connection
      docker.close();

    } catch (DockerException | InterruptedException e) {
      this.exception = e;
      throw new AozanException(e);
    }
  }

  /**
   * Adds the mount directory.
   * @param localDirectory the local directory
   * @param dockerDirectory the docker directory
   * @return the string
   * @throws AozanException the Aozan exception
   */
  public void addMountDirectory(final String localDirectory,
      final String dockerDirectory) throws AozanException {

    if (localDirectory == null) {
      throw new AozanException("Error Docker: local directory is null. "
          + localDirectory);
    }

    // if (!new File(localDirectory).exists()) {
    // throw new AozanException("Error Docker: local directory does not exist. "
    // + localDirectory + " ? " + new File(localDirectory).exists());
    // }

    if (dockerDirectory == null) {
      throw new AozanException(
          "Error Docker: local directory is null or does not exist. "
              + dockerDirectory);
    }

    // Check it is absolute path
    if (localDirectory.trim().charAt(0) != SEPARATOR
        || dockerDirectory.trim().charAt(0) != SEPARATOR) {
      throw new AozanException(
          "Error Docker: directories path must be absolute local "
              + localDirectory + " docker directory " + dockerDirectory);
    }

    this.mountArgument.add(localDirectory + ":" + dockerDirectory);
  }

  //
  // Private methods
  //

  private void checkOSCompatibilityDocker() throws AozanException {

  }

  /**
   * Inits the Docker Client.
   * @param imageName the image name
   * @return the docker client
   * @throws DockerException the docker exception
   * @throws InterruptedException the interrupted exception
   */
  private DockerClient init(final String imageName) throws DockerException,
      InterruptedException, AozanException {

    // TODO
    System.out.println(" * Create connection");
    // Create connection

    final DockerClient docker =
        new DefaultDockerClient("unix:///var/run/docker.sock");

    // Pull image
    System.out.println(" * Pull image");
    docker.pull(imageName);

    return docker;

  }

  /**
   * Configure Docker.
   * @return the host config
   * @throws AozanException the Aozan exception
   */
  private HostConfig configure() throws AozanException {

    if (this.mountArgument.isEmpty())
      throw new AozanException(
          "Error Docker: no mount directory settings to configure hostDocker");

    final String args = Joiner.on(",").join(this.mountArgument);

    return HostConfig.builder().binds(args).build();
  }

  /**
   * Creates the container.
   * @param docker the docker
   * @param imageName the image name
   * @return the string
   * @throws DockerException the docker exception
   * @throws InterruptedException the interrupted exception
   */
  private String createContainer(final DockerClient docker,
      final String imageName) throws DockerException, InterruptedException {

    // Create container
    System.out.println(" * Create config");
    final String cmd = buildArgumentsCommandDocker();

    ContainerConfig config;

    if (this.permission == null) {
      config =
          ContainerConfig.builder().image(imageName).cmd("sh", "-c", cmd)
              .workingDir(this.workDirectoryDocker).build();
    } else {
      config =
          ContainerConfig.builder().image(imageName).cmd("sh", "-c", cmd)
              .user(this.permission).workingDir(this.workDirectoryDocker)
              .build();

    }
    return docker.createContainer(config).id();

  }

  /**
   * Builds the image name in depot Docker.
   * @return image name
   */
  private String buildImageName() {

    final String name = this.depotPublicName + "/" + this.imageDockerName;

    if (this.imageDockerVersion == null || this.imageDockerVersion.isEmpty()) {
      return name;
    }

    return name + ":" + this.imageDockerVersion;
  }

  private String buildArgumentsCommandDocker() {

    return this.commandLine;

  }

  //
  // Getters and setters
  //

  private String setDefaultPermission() throws AozanException {

    try {
      final String user = ProcessUtils.execToString("id -u");
      final String group = ProcessUtils.execToString("id -g");

      return user.trim() + ":" + group.trim();

    } catch (IOException e) {
      throw new AozanException(e);
    }

  }

  /**
   * Sets the permission.
   * @param permissions the new permission
   */
  public void setPermission(final String user, final String group) {
    // TODO check syntax
    this.permission = user + ":" + group;
  }

  /**
   * Sets the depot docker name.
   * @param depotName the new depot docker name
   */
  public void setDepotDockerName(final String depotName) {
    this.depotPublicName = depotName;
  }

  /**
   * Sets the work directory docker.
   * @param workDirectoryDocker the new work directory docker
   */
  public void setWorkDirectoryDocker(final String workDirectoryDocker) {
    this.workDirectoryDocker = workDirectoryDocker;
  }

  /**
   * Gets the exit value.
   * @return the exit value
   */
  public int getExitValue() {
    return this.exitValue;
  }

  /**
   * Gets the image docker name.
   * @return the image docker name
   */
  public String getImageDockerName() {
    return String.format("%s/%s:%s", this.depotPublicName,
        this.imageDockerName, this.imageDockerVersion);
  }

  /**
   * Gets the exception.
   * @return the exception
   */
  public Throwable getException() {
    return this.exception;
  }

  //
  // Constructor
  //

  public DockerUtils(final String commandLine, final String softwareName)
      throws AozanException {
    this(commandLine, softwareName, DEFAULT_VERSION);
  }

  /**
   * Public constructor to initialize parameters to a docker images.
   * @param commandLine the command line
   * @param softwareName the software name
   * @param softwareVersion the software version
   * @param workDirectory the work directory
   * @throws AozanException
   */
  public DockerUtils(final String commandLine, final String softwareName,
      final String softwareVersion) throws AozanException {

    checkNotNull(commandLine, "commande line");
    checkNotNull(softwareName, "software image Docker");

    this.commandLine = commandLine;
    this.imageDockerName = softwareName;

    this.imageDockerVersion = softwareVersion;

    this.depotPublicName = DEPOT_DEFAULT;
    this.permission = setDefaultPermission();

    System.out
        .println("----------------- setting permissions/user in container config "
            + this.permission);

    this.workDirectoryDocker = WORK_DIRECTORY_DOCKER_DEFAULT;

  }

  public static void main(String[] args) {

    // try {
    // test();
    //
    // } catch (DockerException | InterruptedException e1) {
    // // TODO Auto-generated catch block
    // e1.printStackTrace();
    // }
    //
    // System.exit(0);

    // final String cmd = "touch /root/lolotiti_" + new Random().nextInt();

    final String cmd =
        "/usr/local/bin/bcl2fastq -v 2> /root/bcl_version"
            + new Random().nextInt() + ".txt";

    final String name = "bcl2fastq2";
    final String version = "1.8.4";
    final String dir = "/tmp/aozan";

    try {
      // final DockerUtils dV2 = new DockerUtils(cmd, name);
      // dV2.addMountDirectory(dir, "/root");
      // dV2.run();
      //
      // final DockerUtils dV1 = new DockerUtils(cmd, name, version);
      // dV1.addMountDirectory(dir, "/root");
      // dV1.run();

      final DockerUtils script =
          new DockerUtils("/tmp/script_bcl2fastq.sh", name, version);
      // script.addMountDirectory("/import/mimir03/sequencages/nextseq_500/fastq",
      // "/root/");
      // script
      // .addMountDirectory(
      // "/import/mimir03/sequencages/nextseq_500/bcl/150331_TESTHISR_0151_AH9RLKADXX",
      // "/mnt/");
      script.addMountDirectory("/import/mimir03/sequencages/nextseq_500/tmp",
          "/tmp");

      script.setWorkDirectoryDocker("/root");
      script.run();

    } catch (AozanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public static void test() throws DockerException, InterruptedException {

    // Create connection
    System.out.println(" * Create connection");
    final DockerClient docker =
        new DefaultDockerClient("unix:///var/run/docker.sock");

    final String image = "genomicpariscentre/bcl2fastq2";
    // Pull image
    System.out.println(" * Pull image");
    docker.pull(image);

    // Create container
    System.out.println(" * Create config");
    final ContainerConfig config =
        ContainerConfig.builder().image(image)
            .cmd("sh", "-c", "touch /root/lolotiti").build();

    // Version OK
    // final ContainerConfig config =
    // ContainerConfig.builder().image("busybox")
    // .cmd("sh", "-c", "touch /root/lolotiti").user("2715:100").build();

    final HostConfig hostConfig =
        HostConfig.builder()
            .binds("/import/mimir03/sequencages/nextseq_500/tmp:/root").build();

    System.out.println(" * Create container");
    final ContainerCreation creation = docker.createContainer(config);
    final String id = creation.id();
    System.out.println("id: " + id);

    // Inspect container
    System.out.println(" * Inspect container");
    final ContainerInfo info = docker.inspectContainer(id);
    System.out.println("info: " + info);

    // Start container
    System.out.println(" * Start container");
    docker.startContainer(id, hostConfig);

    // Kill container
    System.out.println(" * Wait end of container container");
    System.out.println(docker.waitContainer(id));

    // Remove container
    System.out.println(" * Remove container");
    docker.removeContainer(id);

    // Close connection
    docker.close();

    /**
     * Message output * Create connection SLF4J: Failed to load class
     * "org.slf4j.impl.StaticLoggerBinder". SLF4J: Defaulting to no-operation
     * (NOP) logger implementation SLF4J: See
     * http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
     * Pull image Create config Create container id:
     * 7d270292eeeacaa49197e330fec49614ce6b77b95ec0b39e45231245e4e5b95f Inspect
     * container info: ContainerInfo{id=7d
     * 270292eeeacaa49197e330fec49614ce6b77b95ec0b39e45231245e4e5b95f,
     * created=Wed Apr 08 13:55:29 CEST 2015, path=sh, args=[-c, touch
     * /root/lolotiti], config=ContainerConfig{hostname=7d270292eeea,
     * domainname=, user=2715:100, memory=0, memorySwap=0, cpuShares=0, cpuset=,
     * attachStdin=false, attachStdout=false, attachStderr=false,
     * portSpecs=null, exposedPorts=null, tty=false, openStdin=false,
     * stdinOnce=false,
     * env=[PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin],
     * cmd=[sh, -c, touch /root/lolotiti], image=busybox, volumes=null,
     * workingDir=, entrypoint=null, networkDisabled=false, onBuild=null},
     * hostConfig=HostConfig{binds=null, containerIDFile=, lxcConf=null,
     * privileged=false, portBindings=null, links=null, publishAllPorts=false,
     * dns=null, dnsSearch=null, volumesFrom=null, networkMode=},
     * state=ContainerState{running=false, pid=0, exitCode=0, startedAt=Sat Jan
     * 01 01:00:00 CET 1, finishedAt=Sat Jan 01 01:00:00 CET 1},
     * image=4986bf8c15363d1c5d15512d5266f8777bfba4974ac56e3270e7760f6f0a8125,
     * networkSettings=NetworkSettings{ipAddress=, ipPrefixLen=0, gateway=,
     * bridge=, portMapping=null, ports=null}, resolvConfPath=, hostnamePath=,
     * hostsPath=, name=/silly_bohr, driver=aufs, execDriver=native-0.2,
     * processLabel=, mountLabel=, volumes=null, volumesRW=null} Start container
     * Wait end of container container ContainerExit{statusCode=0} Remove
     * container
     */
  }

}
