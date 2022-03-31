package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.aozan3.DataType.Category.RAW;
import static fr.ens.biologie.genomique.aozan.aozan3.DataType.SequencingTechnology.ILLUMINA;
import static fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.DiscoverNewIlluminaRunDataProcessor.runInfoToString;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
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

/**
 * This class define an Illumina end run data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class EndIlluminaRunDataProcessor implements DataProcessor {

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
        new PartialDataTypeFilter(false));

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

      this.logger.info(inputRunData, "Ending run detection "
          + runId + " on " + sequencerNames.getIlluminaSequencerName(runId));

      String emailContent = String.format("You will find below the parameters "
          + "of the run %s.\n\n" + "Informations about this run:\n" + "%s\n",
          runId.getId(), runInfoToString(runInfo));

      // Create success message
      EmailMessage email = new EmailMessage(
          "Ending run " + runId + " on " + inputRunData.getSource(),
          emailContent);

      return new SimpleProcessResult(inputRunData, email);
    } catch (IOException | ParserConfigurationException | SAXException e) {
      throw new Aozan3Exception(inputRunData.getRunId(), e);
    }
  }

}
