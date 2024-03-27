package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger.info;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.sizeToHumanReadable;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataLocation;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define an Illumina synchronization data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public abstract class SyncDataProcessor implements DataProcessor {

  private DataStorage outputStorage;
  private String dataDescription;
  private boolean partialSync;
  private GenericLogger logger;
  private boolean initialized;

  @Override
  public void init(final Configuration conf, final GenericLogger logger)
      throws Aozan3Exception {

    requireNonNull(conf);

    // Check if object has not been already initialized
    if (this.initialized) {
      throw new IllegalStateException();
    }

    // Set logger
    if (logger != null) {
      this.logger = logger;
    }

    final DataStorage outputStorage =
        DataStorage.deSerializeFromJson(conf.get("output.storage"));

    // Check if directory is writable
    if (!outputStorage.isWritable()) {
      throw new Aozan3Exception(
          "The output synchronization directory is not writable: "
              + outputStorage);
    }

    this.outputStorage = outputStorage;
    this.partialSync = conf.getBoolean("partial.sync", false);
    this.dataDescription = conf.get("data.description", "no description");

    this.initialized = true;
  }

  @Override
  public ProcessResult process(final InputData input, RunConfiguration runConf)
      throws Aozan3Exception {

    requireNonNull(input);

    RunData inputRunData = input.getTheOnlyElement();

    // Check if object has been initialized
    if (!this.initialized) {
      throw new IllegalStateException();
    }

    RunId runId = inputRunData.getRunId();

    try {

      DataLocation inputLocation = inputRunData.getLocation();
      DataLocation finalLocation =
          this.outputStorage.newDataLocation(inputRunData.getRunId().getId());
      DataLocation outputLocation;

      // Check if the input and output storage are equals
      if (this.outputStorage.getPath()
          .equals(inputRunData.getLocation().getStorage().getPath())) {
        throw new IOException(
            "Input and output storage are the same: " + this.outputStorage);
      }

      // Check if input directory exists
      inputLocation.checkReadableDirectory("input synchronization");

      // Check if final output directory already exists
      finalLocation
          .checkIfNotExists("Output synchronization directory already exists");

      // Define output directory
      if (this.partialSync) {
        outputLocation = this.outputStorage
            .newDataLocation(inputRunData.getRunId().getId() + ".tmp");
      } else {
        outputLocation = finalLocation;
      }

      // Create output directory if not exists
      if (inputLocation.isDirectory()
          && !Files.exists(outputLocation.getPath())) {
        Files.createDirectories(outputLocation.getPath());
      }

      // Check output directory
      outputLocation.checkWritableDirectory(this.dataDescription + " output");

      // Check if enough disk space
      long inputSize = inputLocation.getDiskUsage();
      long outputSize =
          outputLocation.exist() ? outputLocation.getDiskUsage() : 0L;
      long requiredSize = inputSize - outputSize;
      this.outputStorage.checkIfEnoughSpace(requiredSize,
          "Not enough space on " + this.dataDescription + " output");

      // TODO Warning if soon not enough disk space

      if (this.partialSync) {
        partialSync(inputLocation.getPath(), outputLocation.getPath());
        return new SimpleProcessResult(inputRunData.newLocation(outputLocation),
            EmailMessage.noMessage());
      }

      long startTime = System.currentTimeMillis();
      sync(inputLocation.getPath(), outputLocation.getPath());
      long endTime = System.currentTimeMillis();

      // Log disk usage and disk free space
      outputSize = outputLocation.getDiskUsage();
      long outputFreeSize = outputLocation.getStorage().getUnallocatedSpace();
      info(this.logger, runId, "output disk free after demux: "
          + sizeToHumanReadable(outputFreeSize));
      info(this.logger, runId,
          "space used by demux: " + sizeToHumanReadable(outputSize));

      // Load email template
      var emailTemplate = new DataProcessorTemplateEmailMessage(runConf,
          "sync.end.email.template", "/emails/end-sync.email.template");

      // Create email content
      var subject = "Ending synchronization for run "
          + runId.getId() + " on " + inputRunData.getSource();
      var email = emailTemplate.endDataProcessorEmail(subject, runId,
          outputLocation.getPath(), startTime, endTime, outputSize,
          outputFreeSize, Collections.emptyMap());

      return new SimpleProcessResult(
          inputRunData.newLocation(outputLocation).setPartialData(false),
          email);

    } catch (IOException e) {
      throw new Aozan3Exception(inputRunData.getRunId(), e);
    }
  }

  protected abstract void sync(Path inputPath, Path outputPath)
      throws IOException;

  protected abstract void partialSync(Path inputPath, Path outputPath)
      throws IOException;

}
