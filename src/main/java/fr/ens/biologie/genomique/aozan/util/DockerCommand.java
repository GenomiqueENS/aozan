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

package fr.ens.biologie.genomique.aozan.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.LogsParameter;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.LogMessage;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;

/**
 * This class define a Docker command to execute.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class DockerCommand {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private static final char SEPARATOR = File.separatorChar;

  private static final String WORK_DIRECTORY_DOCKER_DEFAULT = "/root";

  private final File stdoutFile;
  private final File stderrFile;

  //private String depotPublicName;
  //private final String imageDockerVersion;
  private final String imageName;
  private final List<String> commandLine;
  private final List<String> mountArgument = new ArrayList<>();

  private String permission;
  private String workDirectoryDocker;
  private Map<String, String> mountPoints = new HashMap<>();

  private int exitValue = -1;
  private Throwable exception = null;
  private final DockerClient dockerClient;

  /**
   * Launch Docker container.
   * @throws AozanException if an error occurs while running the container.
   */
  public void run() throws AozanException {

    LOGGER.warning("Pull docker image: " + this.imageName);

    try {
      // Pull image
      dockerClient.pull(this.imageName);
    } catch (DockerException | InterruptedException e) {

      // Do not thrown an exception as the network can be down
      LOGGER.severe("Unable to pull docker image: " + e.getMessage());
    }

    try {

      // Create container
      final HostConfig hostConfig =
          HostConfig.builder().binds(this.mountArgument).build();

      List<String> cmd = this.commandLine;

      final String permission = this.permission;
      final String workDir = this.workDirectoryDocker;

      // // Create container
      LOGGER.warning("Docker create config "
          + "\n\tdocker " + dockerClient + "\n\t imagename " + this.imageName
          + "\n\t host Configure is  " + hostConfig + "\n\tcommend line "
          + Joiner.on(" ").join(cmd) + "\n\twork directory " + workDir
          + "\n\tpermission " + permission);

      final ContainerConfig config = ContainerConfig.builder()
          .image(this.imageName).cmd(cmd).hostConfig(hostConfig)
          .user(permission).workingDir(workDir).build();

      ContainerCreation creation;
      creation = dockerClient.createContainer(config);
      final String id = creation.id();

      // Inspect container
      final ContainerInfo info = dockerClient.inspectContainer(id);
      LOGGER.info("Docker container id: " + id);
      LOGGER.info("Docker container info: " + info);

      // Start container
      dockerClient.startContainer(id);

      // Redirect stdout and stderr
      final LogStream logStream = dockerClient.logs(id, LogsParameter.FOLLOW,
          LogsParameter.STDERR, LogsParameter.STDOUT);
      redirect(logStream, this.stdoutFile, this.stderrFile);

      // Wait the end of container
      this.exitValue = dockerClient.waitContainer(id).statusCode();
      LOGGER.info("Docker exit value " + this.exitValue);

      // Remove container
      dockerClient.removeContainer(id);
      LOGGER.info("Docker container successufully removed: " + id);

    } catch (DockerException | InterruptedException e) {
      LOGGER.severe("Error while running Docker container: " + e.getMessage());
      throw new AozanException(e);
    }
  }

  /**
   * Adds the mount directory.
   * @param localDirectoryPath the local directory
   * @throws AozanException the Aozan exception
   */
  public void addMountDirectory(final String localDirectoryPath)
      throws AozanException {
    addMountDirectory(localDirectoryPath, localDirectoryPath);
  }

  /**
   * Adds the mount directory.
   * @param localDirectoryPath the local directory
   * @param dockerDirectoryPath the docker directory
   * @throws AozanException the Aozan exception
   */
  public void addMountDirectory(final String localDirectoryPath,
      final String dockerDirectoryPath) throws AozanException {

    checkArgument(!Strings.isNullOrEmpty(localDirectoryPath),
        " local directory setting to mount partition on Docker -> "
            + localDirectoryPath);
    checkArgument(!Strings.isNullOrEmpty(dockerDirectoryPath),
        " docker directory setting to mount partition on Docker -> "
            + dockerDirectoryPath);

    final File localDir = new File(localDirectoryPath);

    checkArgument(localDir.exists(),
        "local directory for partition Docker does not exist: "
            + localDirectoryPath);
    checkArgument(localDir.isDirectory(),
        "local directory for partition Docker is not a directory: "
            + localDirectoryPath);

    // Check it is absolute path
    checkArgument(
        localDirectoryPath.trim().charAt(0) == SEPARATOR
            || dockerDirectoryPath.trim().charAt(0) == SEPARATOR,
        "Error Docker: directories path must be absolute local "
            + localDirectoryPath + " docker directory " + dockerDirectoryPath);

    // Do not mount the data twice
    if (this.mountPoints.containsValue(dockerDirectoryPath)) {
      if (!dockerDirectoryPath
          .equals(this.mountPoints.get(localDirectoryPath))) {
        throw new AozanException(
            "The Docker mount point is already used: " + dockerDirectoryPath);
      }
      return;
    }

    this.mountPoints.put(localDirectoryPath, dockerDirectoryPath);

    this.mountArgument.add(localDirectoryPath + ":" + dockerDirectoryPath);

    LOGGER.info("Docker for image "
        + imageName + " mount partition "
        + this.mountArgument.get(this.mountArgument.size() - 1));
  }

  //
  // Static method
  //

  /**
   * Split shell command line.
   * @param commandline the command line
   * @return the list
   */
  public static List<String> splitShellCommandLine(final String commandline) {

    if (commandline == null) {
      return null;
    }

    final String s = commandline.trim();

    final List<String> result = new ArrayList<>();

    final StringBuilder sb = new StringBuilder();
    boolean escape = false;
    boolean inArgument = false;
    char quote = ' ';

    for (int i = 0; i < s.length(); i++) {

      final char c = s.charAt(i);

      if (escape) {

        if (c == '\"') {
          sb.append(c);
        }

        escape = false;
        continue;
      }

      if (c == '\\') {
        escape = true;
        continue;
      }

      if ((c == '"' || c == '\'') && !inArgument) {
        quote = c;
        inArgument = true;
        continue;
      }

      if ((c == ' ' && !inArgument) || (c == quote && inArgument)) {

        if (inArgument) {
          result.add(sb.toString());
        } else {

          String s2 = sb.toString().trim();
          if (!s2.isEmpty()) {
            result.add(s2);
          }
        }

        sb.setLength(0);
        inArgument = false;
        continue;
      }

      sb.append(c);
    }

    if (inArgument) {
      result.add(sb.toString());
    } else {

      String s2 = sb.toString().trim();
      if (!s2.isEmpty()) {
        result.add(s2);
      }
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Redirect the outputs of the container to files.
   * @param logStream the log stream
   * @param stdout stdout output file
   * @param stderr stderr output file
   */
  private static void redirect(final LogStream logStream, final File stdout,
      final File stderr) {

    final Runnable r = new Runnable() {

      @Override
      public void run() {

        try (
            WritableByteChannel stdoutChannel =
                Channels.newChannel(new FileOutputStream(stderr));
            WritableByteChannel stderrChannel =
                Channels.newChannel(new FileOutputStream(stdout))) {

          for (LogMessage message; logStream.hasNext();) {

            message = logStream.next();
            switch (message.stream()) {

            case STDOUT:
              stdoutChannel.write(message.content());
              break;

            case STDERR:
              stderrChannel.write(message.content());
              break;

            case STDIN:
            default:
              break;
            }
          }
        } catch (IOException e) {
          EoulsanLogger.getLogger().severe(e.getMessage());
        }
      }
    };

    new Thread(r).start();
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
   * @param user the user name
   * @param group the user group
   */
  public void setPermission(final String user, final String group) {
    // TODO check syntax
    this.permission = user.trim() + ":" + group.trim();
  }

  /**
   * Sets the work directory docker.
   * @param workDirectoryDocker the new work directory docker
   */
  public void setWorkDirectoryDocker(final String workDirectoryDocker) {

    checkArgument(!Strings.isNullOrEmpty(workDirectoryDocker),
        "work directory setting for Docker");

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
    return this.imageName;
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

  /**
   * Public constructor to initialize parameters to a docker images.
   * @param dockerConnectionString Docker connection string
   * @param commandLine the command line
   * @param imageName the image name
   * @throws AozanException
   */
  public DockerCommand(final String dockerConnectionString,
      final List<String> commandLine, final String imageName)
      throws AozanException {

    checkNotNull(commandLine, "commande line");
    checkNotNull(imageName, "software image Docker");

    this.dockerClient =
        DockerConnection.getInstance(dockerConnectionString).getClient();

    this.commandLine = commandLine;

    this.imageName = imageName.trim();
    this.permission = setDefaultPermission();

    this.stderrFile = new File("/tmp", "STDERR");
    this.stdoutFile = new File("/tmp", "STDOUT");

    this.workDirectoryDocker = WORK_DIRECTORY_DOCKER_DEFAULT;

  }

}
