package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;

/**
 * This interface defines a data processor
 * @author Laurent Jourdren
 * @since 3.0
 */
public interface DataProcessor extends DataTypeFilter {

  /**
   * This interface define a process result
   */
  interface ProcessResult {

    /**
     * Get the output data.
     * @return a RunData
     */
    RunData getRunData();

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
  void init(Configuration conf, AozanLogger logger) throws Aozan3Exception;

  /**
   * Process data.
   * @param inputRunData input run data
   * @param conf run configuration
   * @return a ProcessResult object
   * @throws Aozan3Exception if an error occurs while processing the data
   */
  ProcessResult process(RunData inputRunData, RunConfiguration conf)
      throws Aozan3Exception;

}
