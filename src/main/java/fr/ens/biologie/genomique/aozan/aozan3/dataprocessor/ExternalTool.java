package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger.info;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;

import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.kenetre.log.DummyLogger;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import fr.ens.biologie.genomique.kenetre.util.process.DockerImageInstance;
import fr.ens.biologie.genomique.kenetre.util.process.FallBackDockerClient;
import fr.ens.biologie.genomique.kenetre.util.process.SimpleProcess;
import fr.ens.biologie.genomique.kenetre.util.process.SystemSimpleProcess;

/**
 * This class define a class for calling external tool in DataProcessors.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class ExternalTool {

  private String toolName;
  private boolean dockerMode;
  private String dockerImage;
  private boolean dockerGpuMode;
  private GenericLogger logger;

  private String toolNameLower() {

    return this.toolName.replaceAll(" ", "").toLowerCase();
  }

  /**
   * Create a new process.
   * @param runId run id for logging
   * @param runConf run configuration
   * @param enableLogging enable logging
   * @return a new SimpleProcess
   * @throws IOException if an error occurs while creating the process
   */
  public SimpleProcess newSimpleProcess(final RunId runId,
      boolean enableLogging) throws IOException {

    if (enableLogging) {
      info(this.logger, runId,
          dockerMode
              ? "Use Docker for executing " + this.toolName
              : "Use installed version of " + this.toolName);
    }

    if (!this.dockerMode) {
      return new SystemSimpleProcess();
    }

    if (this.dockerImage.isEmpty()) {
      throw new IOException("No docker image defined for " + this.toolName);
    }

    if (enableLogging) {
      info(this.logger, runId,
          "Docker image to use for " + this.toolName + ": " + dockerImage);
    }

    // Use fallback Docker client
    FallBackDockerClient client = new FallBackDockerClient();
    client.enableGpus(this.dockerGpuMode);
    DockerImageInstance result = client.createConnection(this.dockerImage);

    // Pull Docker image if not exists
    result.pullImageIfNotExists();

    return result;
  }

  /**
   * Get the tool executable version.
   * @param runId the run Id
   * @param runConf run configuration
   * @param parseStdErr parse stderr instead of stdout
   * @return a string with the tool version
   * @throws IOException if an error occurs while getting tool version
   */
  public String getToolVersion(final RunId runId, String tmpDirPath,
      List<String> commandLine, boolean parseStdErr,
      Function<List<String>, String> parser) throws IOException {

    requireNonNull(runId);
    requireNonNull(tmpDirPath);
    requireNonNull(commandLine);
    requireNonNull(parser);

    File tmpDir = new File(tmpDirPath);

    File stdoutFile = Files
        .createTempFile(tmpDir.toPath(), toolNameLower() + "-version", ".out")
        .toFile();
    File stderrFile = Files
        .createTempFile(tmpDir.toPath(), toolNameLower() + "-version", ".err")
        .toFile();

    final int exitValue = newSimpleProcess(runId, false).execute(commandLine,
        tmpDir, tmpDir, stdoutFile, stderrFile, tmpDir);

    // Launch demultiplexing tool
    if (exitValue != 0) {
      Files.delete(stdoutFile.toPath());
      Files.delete(stderrFile.toPath());
      throw new IOException(
          "Unable to launch " + this.toolName + " to get software version");
    }

    // Parse stderr file
    String result = parser.apply(
        Files.readAllLines((parseStdErr ? stderrFile : stdoutFile).toPath()));

    // Delete output files
    Files.delete(stdoutFile.toPath());
    Files.delete(stderrFile.toPath());

    if (result == null) {
      throw new IOException("Unable to get " + this.toolName + " version");
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Constructor. Docker mode disabled.
   * @param toolName name of the tool
   */
  ExternalTool(String toolName) {
    this(toolName, false, null, false, null);
  }

  /**
   * Constructor.
   * @param toolName name of the tool
   * @param docker enable docker mode
   * @param dockerImage docker image
   * @param dockerGpuMode true to enable GPU mode
   * @param logger logger to use
   */
  ExternalTool(String toolName, boolean dockerMode, String dockerImage,
      boolean dockerGpuMode, GenericLogger logger) {

    requireNonNull(toolName);

    if (toolName.trim().isEmpty()) {
      throw new IllegalArgumentException("toolName cannot be empty");
    }

    this.toolName = toolName;
    this.dockerMode = dockerMode;
    this.dockerImage = dockerImage == null ? "" : dockerImage.trim();
    this.logger = logger == null ? new DummyLogger() : logger;
  }

}
