package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;

/**
 * This interface defines a data processor
 * @author Laurent Jourdren
 * @since 3.0
 */
public interface DataProcessor {

  /**
   * This interface define a process result
   */
  public interface ProcessResult {

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
   * Process data.
   * @param inputRunData input run data
   * @param conf run configuration
   * @return a ProcessResult object
   * @throws Aozan3Exception if an error occurs while processing the data
   */
  ProcessResult process(RunData inputRunData, RunConfiguration conf)
      throws Aozan3Exception;

}
