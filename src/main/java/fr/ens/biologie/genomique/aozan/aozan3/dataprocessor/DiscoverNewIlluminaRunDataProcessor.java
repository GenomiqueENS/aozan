package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.aozan3.DataType.Category.RAW;
import static fr.ens.biologie.genomique.aozan.aozan3.DataType.SequencingTechnology.ILLUMINA;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataLocation;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.SequencerNames;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.CategoryDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.MultiDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.PartialDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.TechnologyDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.log.DummyAzoanLogger;
import fr.ens.biologie.genomique.kenetre.illumina.RunInfo;
import fr.ens.biologie.genomique.kenetre.illumina.RunInfo.Read;

/**
 * This class define an Illumina run discovering data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class DiscoverNewIlluminaRunDataProcessor implements DataProcessor {

  public static final String PROCESSOR_NAME = "illumina_discover";

  private AozanLogger logger = new DummyAzoanLogger();
  private boolean initialized;

  @Override
  public String getName() {
    return PROCESSOR_NAME;
  }

  @Override
  public void init(Configuration conf, AozanLogger logger)
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

    this.initialized = true;
  }

  @Override
  public Set<DataTypeFilter> getInputRequirements() {

    DataTypeFilter filter = new MultiDataTypeFilter(
        new CategoryDataTypeFilter(RAW), new TechnologyDataTypeFilter(ILLUMINA),
        new PartialDataTypeFilter(true));

    return Collections.singleton(filter);
  }

  @Override
  public ProcessResult process(InputData inputData, RunConfiguration conf)
      throws Aozan3Exception {

    requireNonNull(inputData);

    RunData inputRunData = inputData.getTheOnlyElement();

    // Check if object has been initialized
    if (!this.initialized) {
      throw new IllegalStateException();
    }

    RunId runId = inputRunData.getRunId();
    DataLocation inputLocation = inputRunData.getLocation();
    SequencerNames sequencerNames = new SequencerNames(conf);

    try {
      RunInfo runInfo = RunInfo
          .parse(new File(inputLocation.getPath().toFile(), "RunInfo.xml"));

      // Check if input directory exists
      inputLocation.checkReadableDirectory("input synchronization");

      this.logger.info(inputRunData,
          "New run discovered "
              + runId + " on sequencer "
              + sequencerNames.getIlluminaSequencerName(runId));

      String emailContent = String.format("You will find below the parameters "
          + "of the run %s.\n\n" + "Informations about this run:\n" + "%s\n",
          runId.getId(), runInfoToString(runInfo));

      // Create success message
      EmailMessage email = new EmailMessage("New run "
          + estimatedRunType(runInfo) + " " + runId + " on "
          + inputRunData.getSource(), emailContent);

      return new SimpleProcessResult(inputRunData, email);
    } catch (IOException | ParserConfigurationException | SAXException e) {
      throw new Aozan3Exception(inputRunData.getRunId(), e);
    }
  }

  private static String estimatedRunType(RunInfo runInfo) {

    List<Read> reads = runInfo.getReads();
    int readsIndexedCount = 0;
    int readsNotIndexedCount = 0;
    int cyclesPerReadNotIndexed = 0;
    String typeRunEstimated;

    for (Read read : reads) {

      if (read.isIndexedRead()) {
        readsIndexedCount += 1;
      } else {
        readsNotIndexedCount += 1;
        if (cyclesPerReadNotIndexed == 0) {
          cyclesPerReadNotIndexed = read.getNumberCycles();
        }
      }
    }

    // Identification type run according to data in RunInfos.xml : SR or PE
    switch (readsNotIndexedCount) {

    case 1:
      typeRunEstimated = "SR-"
          + cyclesPerReadNotIndexed + " with " + readsIndexedCount + " index"
          + (readsIndexedCount > 1 ? "es" : "");
      break;

    case 2:
      typeRunEstimated = "PE-"
          + cyclesPerReadNotIndexed + " with " + readsIndexedCount + " index"
          + (readsIndexedCount > 1 ? "es" : "");
      break;

    default:
      typeRunEstimated = "Undetermined run type ("
          + readsNotIndexedCount + " reads with " + readsIndexedCount + " index"
          + (readsIndexedCount > 1 ? "es" : "") + ")";
      break;
    }

    return typeRunEstimated;
  }

  /**
   * @param runInfo
   * @return
   */
  static String runInfoToString(RunInfo runInfo) {

    StringBuilder sb = new StringBuilder();

    List<Read> reads = runInfo.getReads();
    boolean errorCyclesPerReadNotIndexesCount = false;
    int readsIndexedCount = 0;
    int readsNotIndexedCount = 0;
    int cyclesCount = 0;
    int cyclesPerReadNotIndexed = 0;

    for (Read read : reads) {

      cyclesCount += read.getNumberCycles();

      if (read.isIndexedRead()) {
        readsIndexedCount += 1;
      } else {
        readsNotIndexedCount += 1;
        if (cyclesPerReadNotIndexed == 0) {
          cyclesPerReadNotIndexed = read.getNumberCycles();
        }

        // Check same cycles count for each reads not indexed
        errorCyclesPerReadNotIndexesCount =
            (cyclesPerReadNotIndexed != read.getNumberCycles());
      }
    }

    sb.append("Informations about this run:\n");
    // description_run += "\t- Sequencer: " + common.get_instrument_name(run_id,
    // conf) + ".\n"
    sb.append("\t- "
        + runInfo.getFlowCellLaneCount() + " lanes with "
        + runInfo.getAlignToPhix().size() + " aligned to Phix.\n");
    sb.append("\t- "
        + readsNotIndexedCount + " read"
        + (readsNotIndexedCount > 1 ? "s" : ""));
    sb.append(" and "
        + readsIndexedCount + " index" + (readsIndexedCount > 1 ? "es" : "")
        + ".\n");

    if (errorCyclesPerReadNotIndexesCount || cyclesPerReadNotIndexed == 0) {
      sb.append("\t- ERROR : cycles count per read different between reads ("
          + cyclesCount + " total cycles).\n");
    } else {
      sb.append("\t- "
          + cyclesPerReadNotIndexed + " cycles per read (" + cyclesCount
          + " total cycles).\n");
    }
    sb.append("\t- Estimated run type: " + estimatedRunType(runInfo) + ".\n");

    return sb.toString();
  }

}
