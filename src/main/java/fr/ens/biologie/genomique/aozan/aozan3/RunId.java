package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

/**
 * This class define a run run identifier.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RunId {

  private final String id;
  private final String originalId;

  //
  // Getter
  //

  /**
   * Get the id of the run.
   * @return the id of the run
   */
  public String getId() {

    return this.id;
  }

  /**
   * Get the id of the run.
   * @return the id of the run
   */
  public String getOriginalRunId() {

    return this.originalId;
  }

  /**
   * Test if the run Id is the original run Id.
   * @return true if the run Id is the original run Id
   */
  public boolean isOriginalId() {

    return this.id.equals(this.originalId);
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return "RunId [id=" + this.id + ", originalId=" + this.originalId + "]";
  }

  //
  // Constructor
  //

  /**
   * Constructor for Gson
   */
  protected RunId() {
    this.id = null;
    this.originalId = null;
  }

  /**
   * Public constructor.
   * @param id the identifier of the run
   */
  public RunId(String id) {

    this(id, id);
  }

  /**
   * Public constructor.
   * @param id the identifier of the run
   * @param originalId original run identifier
   */
  public RunId(String id, String originalId) {

    requireNonNull(id);
    requireNonNull(originalId);

    if (id.trim().isEmpty()) {
      throw new IllegalArgumentException("Run id cannot be empty");
    }

    if (originalId.trim().isEmpty()) {
      throw new IllegalArgumentException("Original run id cannot be empty");
    }

    this.id = id;
    this.originalId = originalId;
  }

}
