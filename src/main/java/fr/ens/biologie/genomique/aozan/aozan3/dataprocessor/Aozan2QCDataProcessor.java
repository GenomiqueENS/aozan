package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.Globals.QC_DATA_EXTENSION;
import static fr.ens.biologie.genomique.aozan.aozan3.DataType.BCL;
import static fr.ens.biologie.genomique.aozan.aozan3.DataType.ILLUMINA_FASTQ;
import static fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.BclConvertIlluminaDemuxDataProcessor.BCL_CONVERT_FORBIDDEN_DATA_SECTION;
import static fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.EndIlluminaRunDataProcessor.createTar;
import static fr.ens.biologie.genomique.aozan.aozan3.legacy.IndexGenerator.createIndexRun;
import static fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger.newAozanLogger;
import static fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger.newDummyLogger;
import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet.BCLCONVERT_DEMUX_TABLE_NAME;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.sizeToHumanReadable;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toTimeHumanReadable;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.QCReport;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataLocation;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.DataType;
import fr.ens.biologie.genomique.aozan.aozan3.DataType.Category;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.SimpleDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.log.Aozan3Logger;
import fr.ens.biologie.genomique.aozan.aozan3.util.DiskUtils;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define an Aozan 2 QC data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class Aozan2QCDataProcessor implements DataProcessor {

  public static final String PROCESSOR_NAME = "aozan2qc";

  private static final long DEFAULT_MIN_OUTPUT_FREE_SPACE = 10_000_000;
  private static final String DEMUX_REPORT_PREFIX = "basecall_stats_";

  private Aozan3Logger logger = newDummyLogger();

  private DataStorage outputStorage;
  private String dataDescription;
  private final RunConfiguration conf = new RunConfiguration();
  private boolean initialized;

  @Override
  public String getName() {

    return PROCESSOR_NAME;
  }

  @Override
  public void init(Configuration conf, GenericLogger logger)
      throws Aozan3Exception {

    requireNonNull(conf);

    // Check if object has not been already initialized
    if (this.initialized) {
      throw new IllegalStateException();
    }

    // Set logger
    if (logger != null) {
      this.logger = newAozanLogger(logger);
    }

    final DataStorage outputStorage =
        DataStorage.deSerializeFromJson(conf.get("output.storage"));

    // Check if directory is writable
    if (!outputStorage.isWritable()) {
      throw new Aozan3Exception(
          "The output QC directory is not writable: " + outputStorage);
    }

    this.outputStorage = outputStorage;
    this.dataDescription = conf.get("data.description", "no description");

    // Default configuration
    this.conf.set(conf);

    this.initialized = true;
  }

  @Override
  public Set<DataTypeFilter> getInputRequirements() {

    return new HashSet<DataTypeFilter>(
        Arrays.asList(new SimpleDataTypeFilter(BCL),
            new SimpleDataTypeFilter(ILLUMINA_FASTQ)));
  }

  @Override
  public ProcessResult process(InputData inputData, RunConfiguration runConf)
      throws Aozan3Exception {

    requireNonNull(inputData);
    requireNonNull(runConf);

    // Check if object has been initialized
    if (!this.initialized) {
      throw new IllegalStateException();

    }

    // TODO use InterOP directory instead
    RunData bclRunData = inputData.get(DataType.BCL);
    RunData fastqRunData = inputData.get(DataType.ILLUMINA_FASTQ);
    RunId runId = fastqRunData.getRunId();

    RunConfiguration conf = new RunConfiguration(this.conf);
    conf.set(runConf);

    DataLocation bclLocation = bclRunData.getLocation();
    DataLocation fastqLocation = fastqRunData.getLocation();
    DataLocation outputLocation =
        this.outputStorage.newDataLocation(runId.getId());

    try {

      // Check if a samplesheet exists
      if (!conf.containsKey("illumina.samplesheet")) {
        throw new IOException("No samplesheet found");
      }

      SampleSheet samplesheet =
          SampleSheetUtils.deSerialize(conf.get("illumina.samplesheet"));

      // Merge forbidden section and BCLconvert section
      if (samplesheet.containsSection(BCLCONVERT_DEMUX_TABLE_NAME)) {

        SampleSheetUtils.mergeBclConvertDataAndForbiddenData(samplesheet,
            BCL_CONVERT_FORBIDDEN_DATA_SECTION);
      }

      // Check if the input and output storage are equals
      if (this.outputStorage.getPath()
          .equals(fastqRunData.getLocation().getStorage().getPath())) {
        throw new IOException(
            "Input and output storage are the same: " + this.outputStorage);
      }

      // Check if input directory exists
      fastqLocation.checkReadableDirectory("fastq input is not readable");

      // Legacy mode for output
      if (conf.getBoolean("legacy.output")) {
        outputLocation = new DataLocation(outputLocation.getStorage(), Paths
            .get(outputLocation.getPath().toString(), "qc_" + runId.getId()));
      }

      // Check if final output directory already exists
      outputLocation
          .checkIfNotExists(this.dataDescription + " output already exists");

      // Create output directory
      Files.createDirectories(outputLocation.getPath());

      // Create demux log in legacy mode
      if (conf.getBoolean("legacy.output")) {

        // Generate tar archive with demultiplexing reports and logs
        Path logDir = outputLocation.getPath().getParent();

        Path reportArchiveFile = Paths.get(logDir.toString(),
            DEMUX_REPORT_PREFIX + runId.getId() + ".tar.bz2");

        createTar(reportArchiveFile, fastqLocation.getPath(),
            Arrays.asList("Reports", "Stats", "InterOp", "Logs", "*.csv"));

        createIndexRun(logDir, runId.getId(),
            Arrays.asList("hiseq.step", "demux.step"));
      }

      // Check if enough disk space
      long requiredSize =
          conf.getLong("min.output.free.space", DEFAULT_MIN_OUTPUT_FREE_SPACE);
      this.outputStorage.checkIfEnoughSpace(requiredSize,
          "Not enough space on " + this.dataDescription + " output");

      boolean writeDataFile = conf.getBoolean("qc.write.data.file", true);
      boolean writeXMLFile = conf.getBoolean("qc.write.xml.file", true);
      boolean writeHTMLFile = conf.getBoolean("qc.write.html.file", true);

      long startTime = System.currentTimeMillis();

      // Perform QC
      qc(conf, bclLocation, fastqLocation, outputLocation, runId, samplesheet,
          writeDataFile, writeXMLFile, writeHTMLFile);

      // Create index.html at run of run directory in legacy mode
      if (conf.getBoolean("legacy.output")) {

        createIndexRun(outputLocation.getPath().getParent(), runId.getId(),
            Arrays.asList("hiseq.step", "demux.step", "qc.step"));
      }

      // Chmod on output directory
      if (conf.getBoolean("read.only.output.files", false)) {
        DiskUtils.changeDirectoryMode(outputLocation.getPath(), "u-w,g-w,o-w");
      }

      long endTime = System.currentTimeMillis();

      // Log disk usage and disk free space
      long outputSize = outputLocation.getDiskUsage();
      long outputFreeSize = outputLocation.getStorage().getUsableSpace();
      this.logger.info(runId,
          "output disk free after QC: " + sizeToHumanReadable(outputFreeSize));
      this.logger.info(runId,
          "space used by qc: " + sizeToHumanReadable(outputSize));

      // TODO send email with attached report
      // TODO add http link to the report
      // TODO Use absolute path

      // Report URL in email message
      String reportLocationMessage = conf.containsKey("reports.url")
          ? "\nRun reports can be found at following location:\n  "
              + conf.get("reports.url") + '/' + runId.getId() + "\n"
          : "";

      String emailContent = String.format("Ending quality control for run %s.\n"
          + "Job finished at %s without error in %s.\n"
          + "You will find attached to this message the quality control report.\n\n"
          + "QC files for this run can be found in the following directory:\n  %s\n%s"
          + "\nFor this task %s has been used and %s GB still free.",
          runId.getId(), new Date(endTime).toString(),
          toTimeHumanReadable(endTime - startTime), outputLocation.getPath(),
          reportLocationMessage, sizeToHumanReadable(outputSize),
          sizeToHumanReadable(outputFreeSize));

      // Create success message
      EmailMessage email = new EmailMessage(
          "Ending QC for run "
              + runId.getId() + " on " + fastqRunData.getSource(),
          emailContent);

      return new SimpleProcessResult(
          fastqRunData.newLocation(outputLocation).newCategory(Category.QC),
          email);

    } catch (IOException | AozanException | KenetreException e) {
      throw new Aozan3Exception(runId, e);
    }
  }

  private static void qc(RunConfiguration conf, DataLocation bclLocation,
      DataLocation fastqLocation, DataLocation outputLocation, RunId runId,
      SampleSheet sampleSheet, boolean writeDataFile, boolean writeXMLFile,
      boolean writeHTMLFile) throws AozanException {

    requireNonNull(conf);
    requireNonNull(bclLocation);
    requireNonNull(fastqLocation);
    requireNonNull(outputLocation);
    requireNonNull(runId);

    Settings settings = Aozan2Compatibility.runConfigurationToSettings(conf);
    String bclDir = bclLocation.getPath().toString();
    String fastqDir = fastqLocation.getPath().toString();
    String qcDir = outputLocation.getPath().toString();
    String illuminaRunId = runId.getOriginalRunId();
    String styleSheetPath = conf.get("qc.report.stylesheet", "");

    // Get temporary directory
    File temporaryDirectory = new File(conf.get("tmp.path"));

    // TODO Create a constructor with File or Path Object types instead of
    // String for path
    QC qc = new QC(settings, bclDir, fastqDir, qcDir, temporaryDirectory,
        illuminaRunId, sampleSheet);

    // Compute report
    QCReport qcReport = qc.computeReport();

    // Write data file
    if (writeDataFile) {
      File dataFile = new File(outputLocation.getPath().toFile(),
          illuminaRunId + QC_DATA_EXTENSION);
      qc.writeRawData(qcReport, dataFile);
    }

    // Write XML report
    if (writeXMLFile) {
      File xmlFile =
          new File(outputLocation.getPath().toFile(), illuminaRunId + ".xml");
      qc.writeXMLReport(qcReport, xmlFile);
    }

    // Write HTML report
    if (writeHTMLFile) {
      File htmlFile =
          new File(outputLocation.getPath().toFile(), illuminaRunId + ".html");

      // TODO update argument type of the method
      qc.writeReport(qcReport, styleSheetPath.isEmpty() ? null : styleSheetPath,
          htmlFile.toString());
    }
  }

}
