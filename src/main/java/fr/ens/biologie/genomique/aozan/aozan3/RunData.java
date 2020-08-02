package fr.ens.biologie.genomique.aozan.aozan3;

import static fr.ens.biologie.genomique.aozan.aozan3.SequencerSource.unknownSequencerSource;
import static java.util.Objects.requireNonNull;

/**
 * This class define a run data.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RunData {

  /**
   * Run data type.
   */
  public enum Type {
    RAW, LOG, PROCESSED, PROCESSED_LOG, QC
  }

  private final RunId runId;
  private final SequencerSource source;
  private final Type type;
  private final boolean partialData;

  private final DataLocation location;

  //
  // Getters
  //

  /**
   * Get run Id.
   * @return the run id
   */
  public RunId getRunId() {

    return this.runId;
  }

  /**
   * Get the source of the data (e.g. the sequencer name).
   * @return the source of the data
   */
  public SequencerSource getSource() {

    return this.source;
  }

  /**
   * Get the sequencing technology used for the data.
   * @return the sequencing technology
   */
  public SequencingTechnology getSequencingTechnology() {

    return this.runId.getSequencingTechnology();
  }

  /**
   * Get the type of the data.
   * @return the type of the data
   */
  public Type getType() {

    return this.type;
  }

  /**
   * Get the location of the data.
   * @return the location of the data
   */
  public DataLocation getLocation() {

    return this.location;
  }

  /**
   * Test if the run data is partial.
   * @return true if the run is partial
   */
  public boolean isPartialData() {

    return this.partialData;
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return "RunData [runId="
        + runId + ", type=" + type + ", partialData=" + partialData
        + ", location=" + location + "]";
  }

  //
  // Update methods
  //

  /**
   * Create a new RunData object from the current object with a new location.
   * @param location the new location
   * @return a new RunData object
   */
  public RunData newLocation(final DataLocation location) {

    requireNonNull(location);
    return new RunData(this.runId, this.source, this.type, this.partialData,
        location);
  }

  /**
   * Create a new RunData object from the current object with a new type.
   * @param type the new type
   * @return a new RunData object
   */
  public RunData newType(final Type type) {

    requireNonNull(type);
    return new RunData(this.runId, this.source, type, this.partialData,
        this.location);
  }

  /**
   * Create a new RunData object from the current object with a new partial data
   * setting.
   * @param location the new location
   * @return a new RunData object
   */
  public RunData newPartialData(final boolean partialData) {

    return new RunData(this.runId, this.source, type, partialData,
        this.location);
  }

  //
  // New methods
  // TODO Create a dedicated factory for this methods
  //

  

  //
  // Constructor
  //

  /**
   * Constructor for Gson
   */
  protected RunData() {
    this.runId = null;
    this.source = null;
    this.type = null;
    this.partialData = false;
    this.location = null;
  }

  /**
   * Private conctructor.
   * @param runId the run id
   * @param source the source of the data
   * @param type the type of the data
   * @param partialData partial data
   * @param location location of the data
   */
  RunData(final RunId runId, final SequencerSource source, final Type type,
      final boolean partialData, final DataLocation location) {

    requireNonNull(runId);
    requireNonNull(type);
    requireNonNull(location);

    this.runId = runId;
    this.source = source != null ? source : unknownSequencerSource();
    this.type = type;
    this.partialData = partialData;
    this.location = location;
  }

}
