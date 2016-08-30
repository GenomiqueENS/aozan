package fr.ens.biologie.genomique.aozan.util;

import java.net.URI;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

/**
 * This class define a docker connection.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerConnection {

  private static DockerConnection singleton;

  private final String connectionString;
  private DockerClient client;

  /**
   * Get Docker client.
   * @return a Docker client object
   */
  public DockerClient getClient() {

    if (this.client != null) {
      return this.client;
    }

    final URI dockerConnection = URI.create(connectionString != null
        ? connectionString.trim() : "unix:///var/run/docker.sock");

    this.client = new DefaultDockerClient(dockerConnection);

    return this.client;
  }

  /**
   * Close Docker connections.
   */
  public void closeConnections() {

    if (this.client != null) {
      client.close();
      this.client = null;
    }
  }

  //
  // Singleton method
  //

  public static DockerConnection getInstance(final String connectionString) {

    if (singleton == null) {
      singleton = new DockerConnection(connectionString);
    }

    return singleton;
  }

  //
  // Constructor
  //

  private DockerConnection(final String connectionString) {
    this.connectionString = connectionString;
  }

}
