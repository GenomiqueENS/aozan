package fr.ens.biologie.genomique.aozan.aozan3;

import static fr.ens.biologie.genomique.aozan.aozan3.SequencerSource.unknownSequencerSource;
import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.aozan.aozan3.DataType.Category;
import fr.ens.biologie.genomique.aozan.aozan3.DataType.SequencingTechnology;

/**
 * This class define a run data.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RunData {

  private final RunId runId;
  private final SequencerSource source;
  private final DataType type;

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
   * Get the type of the data.
   * @return the type of the data
   */
  public DataType getType() {

    return this.type;
  }

  /**
   * Get the location of the data.
   * @return the location of the data
   */
  public DataLocation getLocation() {

    return this.location;
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return "RunData [runId="
        + runId + ", type=" + type + ", location=" + location + "]";
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
    return new RunData(this.runId, this.source, this.type, location);
  }

  /**
   * Create a new RunData object from the current object with a new type.
   * @param type the new type
   * @return a new RunData object
   */
  public RunData newType(final DataType type) {

    requireNonNull(type);
    return new RunData(this.runId, this.source, type, this.location);
  }

  //
  // Convinient methods for updating DataType
  //

  /**
   * Create a new RunType object from the current object with a new type.
   * @param type the new type
   * @return a new RunType object
   */
  public RunData newCategory(final Category category) {

    return newType(this.type.newCategory(category));
  }

  /**
   * Create a new RunType object from the current object with a new type.
   * @param type the new type
   * @return a new RunType object
   */
  public RunData newTechnology(final SequencingTechnology technology) {

    return newType(this.type.newTechnology(technology));
  }

  /**
   * Create a new RunType object from the current object with a new type.
   * @param type the new type
   * @return a new RunType object
   */
  public RunData newType(final String type) {

    return newType(this.type.newType(type));
  }

  /**
   * Create a new RunType object from the current object with a new partial data
   * setting.
   * @param partialData the new partial data value
   * @return a new RunType object
   */
  public RunData setPartialData(final boolean partialData) {

    return newType(this.type.setPartialData(partialData));
  }

  /**
   * Create a new RunType object from the current object with a new log only
   * setting.
   * @param logOnly the new log only value
   * @return a new RunType object
   */
  public RunData setLogOnly(final boolean logOnly) {

    return newType(this.type.setLogOnly(logOnly));
  }

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
  RunData(final RunId runId, final SequencerSource source, final DataType type,
      final DataLocation location) {

    requireNonNull(runId);
    requireNonNull(type);
    requireNonNull(location);

    this.runId = runId;
    this.source = source != null ? source : unknownSequencerSource();
    this.type = type;
    this.location = location;
  }

}
