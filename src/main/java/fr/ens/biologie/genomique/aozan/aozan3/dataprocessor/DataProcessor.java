package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
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
   * Get the name of the processor.
   * @return the name of the processor
   */
  public String getName();

  /**
   * Initialize the processor.
   * @param conf the configuration of the processor
   * @param logger the logger to use
   * @throws Aozan3Exception if an error occurs while initialize the processor
   */
  public void init(Configuration conf, AozanLogger logger)
      throws Aozan3Exception;

  /**
   * Test if the processor accept a type of RunData.
   * @param type type of run data
   * @param partialData partial run data
   * @return true if the process accept this type of run data
   */
  boolean accept(RunData.Type type, boolean partialData);

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
