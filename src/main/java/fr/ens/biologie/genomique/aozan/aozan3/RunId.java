package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

/**
 * This class define a run run identifier.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RunId {

  private final String id;
  private final SequencingTechnology sequencingTechnology;

  //
  // Getter
  //

  /**
   * Get the id of the sequencer
   * @return
   */
  public String getId() {

    return this.id;
  }

  /**
   * Get the sequencing technology
   * @return the sequencing technology
   */
  public SequencingTechnology getSequencingTechnology() {

    return this.sequencingTechnology;
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return "RunId [id="
        + id + ", sequencingTechnology=" + sequencingTechnology + "]";
  }

  //
  // Constructor
  //

  /**
   * Constructor for Gson
   */
  protected RunId() {
    this.id = null;
    this.sequencingTechnology = null;
  }

  /**
   * Public constructor.
   * @param id the identifier of the run
   * @param sequencingTechnology the sequencing technology
   */
  public RunId(String id, SequencingTechnology sequencingTechnology) {

    requireNonNull(id);
    requireNonNull(sequencingTechnology);

    if (id.trim().isEmpty()) {
      throw new IllegalArgumentException("Run id cannot be empty");
    }

    this.id = id;
    this.sequencingTechnology = sequencingTechnology;
  }

}
