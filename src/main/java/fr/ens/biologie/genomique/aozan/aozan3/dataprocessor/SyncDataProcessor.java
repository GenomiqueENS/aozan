package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.sizeToHumanReadable;
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.toTimeHumanReadable;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataLocation;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.DummyAzoanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;

/**
 * This class define an Illumina synchronization data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public abstract class SyncDataProcessor implements DataProcessor {

  private final DataStorage outputStorage;
  private final String dataDescription;
  private final boolean partialSync;
  private final AozanLogger logger;

  @Override
  public ProcessResult process(final RunData inputRunData,
      RunConfiguration runConf) throws Aozan3Exception {

    requireNonNull(inputRunData);
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
      inputLocation.checkReadableDirectory(this.dataDescription + " input");

      // Check if final output directory already exists
      finalLocation.checkIfNotExists(this.dataDescription + " output");

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
      this.logger.info(runId, "output disk free after demux: "
          + sizeToHumanReadable(outputFreeSize));
      this.logger.info(runId,
          "space used by demux: " + sizeToHumanReadable(outputSize));

      // Report URL in email message
      String reportLocationMessage = runConf.containsKey("reports.url")
          ? String.format(
              "\n\nRun reports can be found at following location:\n  %s",
              runConf.get("reports.url") + '/' + runId.getId())
          : "";

      String emailContent = String.format("Ending synchronization "
          + "for run %s.\n" + "Job finished at %s \" without error in %s.\n"
          + "Run output files for this run can be found in the following directory: %s\n%s"
          + "\n\nFor this task %.2f GB has been used and %.2f GB still free.",
          runId.getId(), new Date(endTime).toString(),
          toTimeHumanReadable(endTime - startTime), outputLocation,
          reportLocationMessage, sizeToHumanReadable(outputSize),
          sizeToHumanReadable(outputFreeSize));

      // Create success message
      EmailMessage email = new EmailMessage(
          "Ending demultiplexing for run "
              + runId.getId() + " on " + inputRunData.getSource(),
          emailContent);

      return new SimpleProcessResult(
          inputRunData.newLocation(outputLocation).newPartialData(false),
          email);

    } catch (IOException e) {
      throw new Aozan3Exception(inputRunData.getRunId(), e);
    }
  }

  protected abstract void sync(Path inputPath, Path outputPath)
      throws IOException;

  protected abstract void partialSync(Path inputPath, Path outputPath)
      throws IOException;

  //
  // Constructor
  //

  public SyncDataProcessor(final Configuration conf, final AozanLogger logger)
      throws IOException {

    requireNonNull(conf);

    // Set logger
    this.logger = logger == null ? new DummyAzoanLogger() : logger;

    final DataStorage outputStorage =
        DataStorage.deSerializeFromJson(conf.get("output.storage"));

    // Check if directory is writable
    if (!outputStorage.isWritable()) {
      throw new IOException(
          "The output synchronization directory is not writable: "
              + outputStorage);
    }

    this.outputStorage = outputStorage;
    this.partialSync = conf.getBoolean("partial.sync", false);
    this.dataDescription = conf.get("data.description", "no description");
  }

}
