package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.DataTypeFilter;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This interface defines a data processor
 * @author Laurent Jourdren
 * @since 3.0
 */
public interface DataProcessor {

  /**
   * This interface define a process result
   */
  interface ProcessResult {

    /**
     * Get the output data.
     * @return a RunData
     */
    Set<RunData> getRunData();

    /**
     * Get the output email
     * @return an email message
     */
    EmailMessage getEmail();
  }

  /**
   * Get the name of the processor.
   * @return the name of the processor
   */
  String getName();

  /**
   * Initialize the processor.
   * @param conf the configuration of the processor
   * @param logger the logger to use
   * @throws Aozan3Exception if an error occurs while initialize the processor
   */
  void init(Configuration conf, GenericLogger logger) throws Aozan3Exception;

  /**
   * Get the input requirements of the processor.
   * @return a set with input requirements
   */
  Set<DataTypeFilter> getInputRequirements();

  /**
   * Process data.
   * @param inputData input run data
   * @param conf run configuration
   * @return a ProcessResult object
   * @throws Aozan3Exception if an error occurs while processing the data
   */
  ProcessResult process(InputData inputData, RunConfiguration conf)
      throws Aozan3Exception;

}
