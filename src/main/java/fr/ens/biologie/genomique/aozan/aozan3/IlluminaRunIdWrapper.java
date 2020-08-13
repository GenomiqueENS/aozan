package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

/**
 * This class allow to get information from Illumina run id.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IlluminaRunIdWrapper {

  private final RunId runId;

  /**
   * Get RunId.
   * @return the RunId
   */
  public RunId getRunId() {
    return this.runId;
  }

  /**
   * Get the run number.
   * @return the run number as an integer
   */
  public int getRunNumber() {

    return Integer.parseInt(this.runId.getId().split("_")[2]);
  }

  /**
   * Get the flow cell identifier.
   * @return the flow cell identifier as a string
   */
  public String getFlowCellId() {
    return this.runId.getId().split("_")[3].substring(1);
  }

  /**
   * Get the instrument serial number.
   * @return the instrument serial number as a string
   */
  public String getInstrumentSerialNumber() {

    return this.runId.getId().split("_")[1];
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param runId run id
   */
  public IlluminaRunIdWrapper(RunId runId) {

    requireNonNull(runId);

    if (!IlluminaUtils.checkRunId(runId.getId())) {
      throw new IllegalArgumentException("Invalid Illumina run id: " + runId);
    }

    this.runId = runId;
  }

}
