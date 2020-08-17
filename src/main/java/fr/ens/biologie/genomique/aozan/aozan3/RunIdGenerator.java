package fr.ens.biologie.genomique.aozan.aozan3;

import java.util.Map;

/**
 * This class define an interface f that define how to create processor run data
 * output names
 * @author Laurent Jourdren
 * @since 3.0
 */
public interface RunIdGenerator {

  /**
   * Create a new run id for the output of a processor.
   * @param runId the run id
   * @param contants constants used for new name creation
   * @return a String with the new name
   * @throws Aozan3Exception if an error occurs while creating the new run Id
   */
  RunId newRunId(RunId runId, Map<String, String> contants)
      throws Aozan3Exception;

}
