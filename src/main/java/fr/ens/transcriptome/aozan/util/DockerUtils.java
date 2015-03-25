package fr.ens.transcriptome.eoulsan;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;

public class DockerMain {

  public static void main(String[] args) throws DockerException,
      InterruptedException {

    // Create connection
    System.out.println(" * Create connection");
    final DockerClient docker =
        new DefaultDockerClient("unix:///var/run/docker.sock");

    // Pull image
    System.out.println(" * Pull image");
    docker.pull("busybox");

    // Create container
    System.out.println(" * Create config");
    final ContainerConfig config =
        ContainerConfig.builder().image("busybox")
            .cmd("sh", "-c", "touch /root/lolotiti").user("2710:100").build();

    final HostConfig hostConfig =
        HostConfig.builder().binds("/home/jourdren/workspace/eoulsan:/root")
            .build();

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
  }

}

