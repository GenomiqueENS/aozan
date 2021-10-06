package fr.ens.biologie.genomique.aozan.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerClient;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerImageInstance;
import fr.ens.biologie.genomique.eoulsan.util.process.FallBackDockerClient;
import fr.ens.biologie.genomique.eoulsan.util.process.SingularityDockerClient;
import fr.ens.biologie.genomique.eoulsan.util.process.SpotifyDockerClient;

/**
 * This class define a class that manage Eoulsan Docker connections.
 * TODO Remove this once Eoulsan 2.6 will be used.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerManager {

  /** Available Docker clients. */
  public enum ClientType {
    FALLBACK, SPOTIFY, SINGULARITY
  };

  private static DockerManager singleton;
  private final DockerClient client;

  /**
   * Create a Docker image instance.
   * @param dockerImage docker image
   * @return a Docker connection object
   * @throws IOException if an error occurs while creating the image instance
   */
  public DockerImageInstance createImageInstance(final String dockerImage)
      throws IOException {

    return this.client.createConnection(dockerImage);
  }

  /**
   * List the tags of installed images.
   * @return a set with the tags of installed images
   * @throws IOException if an error occurs while listing the tag
   */
  public Set<String> listImageTags() throws IOException {

    return client.listImageTags();
  }

  /**
   * Close Docker connections.
   * @throws IOException if an error occurs while closing the connections
   */
  public static void closeConnections() throws IOException {

    if (singleton != null) {
      singleton.client.close();
    }
  }

  //
  // Singleton method
  //

  /**
   * Get the instance of the DockerManager.
   * @return the instance of the DockerManager
   * @throws IOException if an error occurs while creating the DockerManager
   *           instance
   */
  public static DockerManager getInstance() throws IOException {

    return getInstance(findClientForEoulsan(),
        EoulsanRuntime.getSettings().getDockerConnectionURI());
  }

  /**
   * Get the instance of the DockerManager.
   * @param clientType Docker client type
   * @param dockerConnection URI of the docker connection
   * @return the instance of the DockerManager
   * @throws IOException if an error occurs while creating the DockerManager
   *           instance
   */
  public static synchronized DockerManager getInstance(ClientType clientType,
      String dockerConnection) throws IOException {

    return getInstance(clientType, URI.create(dockerConnection));
  }

  /**
   * Get the instance of the DockerManager.
   * @param clientType Docker client type
   * @param dockerConnection URI of the docker connection
   * @return the instance of the DockerManager
   * @throws IOException if an error occurs while creating the DockerManager
   *           instance
   */
  public static synchronized DockerManager getInstance(ClientType clientType,
      URI dockerConnection) throws IOException {

    if (singleton == null) {
      singleton = new DockerManager(clientType, dockerConnection);
    }

    return singleton;
  }

  //
  // Other methods
  //

  private static ClientType findClientForEoulsan() {

    if (EoulsanRuntime.isRuntime()
        && EoulsanRuntime.getSettings().isDockerBySingularityEnabled()) {

      return ClientType.SINGULARITY;
    }

    if (EoulsanRuntime.isRuntime()
        && EoulsanRuntime.getRuntime().getMode().isHadoopMode()) {

      return ClientType.FALLBACK;
    }

    return ClientType.SPOTIFY;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   * @throws IOException if an error occurs while creating the instance
   */
  private DockerManager(ClientType clientType, URI dockerConnection)
      throws IOException {

    requireNonNull(clientType);
    requireNonNull(dockerConnection);

    switch (clientType) {
    case FALLBACK:
      this.client = new FallBackDockerClient();
      break;

    case SPOTIFY:
      this.client = new SpotifyDockerClient();
      break;

    case SINGULARITY:
      this.client = new SingularityDockerClient();
      break;

    default:
      throw new IllegalStateException(
          "Unsupported Docker client implementation: " + clientType);
    }

    this.client.initialize(dockerConnection);
  }

}
