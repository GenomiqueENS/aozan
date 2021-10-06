package fr.ens.biologie.genomique.aozan.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Strings;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerImageInstance;

/**
 * This class define a Docker command to execute.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class DockerCommand {

  private final DockerImageInstance instance;
  private final String imageName;
  private final List<String> commandLine;
  private final List<File> filesUsed = new ArrayList<>();
  private int exitValue;

  /**
   * Gets the image docker name.
   * @return the image docker name
   */
  public String getImageDockerName() {
    return this.imageName;
  }

  /**
   * Gets the exit value.
   * @return the exit value
   */
  public int getExitValue() {
    return this.exitValue;
  }

  /**
   * Adds the mount directory.
   * @param localDirectoryPath the local directory
   * @throws AozanException the Aozan exception
   */
  public void addMountDirectory(final String localDirectoryPath)
      throws AozanException {

    checkArgument(!Strings.isNullOrEmpty(localDirectoryPath),
        " local directory setting to mount partition on Docker -> "
            + localDirectoryPath);

    this.filesUsed.add(new File(localDirectoryPath));
  }

  /**
   * Launch Docker container.
   * @throws AozanException if an error occurs while running the container.
   */
  public void run() throws AozanException {

    try {
      this.instance.pullImageIfNotExists();

      this.exitValue =
          this.instance.execute(this.commandLine, new File("/root"),
              new File("java.io.tmpdir"), new File("/tmp", "STDOUT"),
              new File("/tmp", "STDERR"), this.filesUsed.toArray(new File[0]));

    } catch (IOException e) {
      throw new AozanException(e);
    }
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

    Objects.requireNonNull(dockerConnectionString);
    Objects.requireNonNull(commandLine);
    Objects.requireNonNull(imageName);

    this.imageName = imageName;
    this.commandLine = commandLine;

    try {
      this.instance =
          DockerManager
              .getInstance(DockerManager.ClientType.FALLBACK,
                  new URI(dockerConnectionString))
              .createImageInstance(imageName);
    } catch (URISyntaxException | IOException e) {
      throw new AozanException("Invalid Docker connection URI", e);
    }

  }

}
