package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

/**
 * This class define a sequencer source.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class SequencerSource {

  private final String id;
  private final String manufacturer;
  private final String model;
  private final String serialNumber;
  private final String description;

  //
  // Getters
  //

  /**
   * Get the identifier of the sequencer.
   * @return the identifier of the sequencer
   */
  public String getId() {
    return this.id;
  }

  /**
   * Get the manufacturer of the sequencer.
   * @return the manufacturer of the sequencer
   */
  public String getManufacturer() {
    return this.manufacturer;
  }

  /**
   * Get the model of the sequencer.
   * @return the model of the sequencer
   */
  public String getModel() {
    return this.model;
  }

  /**
   * Get the serial number of the sequencer.
   * @return the serial number of the sequencer
   */
  public String getSerialNumber() {
    return this.serialNumber;
  }

  /**
   * Get the description of the sequencer.
   * @return the description of the sequencer
   */
  public String getDescription() {
    return this.description;
  }

  //
  // Static constructor
  //

  public static final SequencerSource unknownSequencerSource() {

    return new SequencerSource("unknown", null, null, null, null);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param id the id of the sequencer
   * @param manufacturer the manufacturer
   * @param model sequencer model
   * @param serialNumber sequencer serial number
   * @param description description of the sequencer
   */
  SequencerSource(final String id, final String manufacturer,
      final String model, final String serialNumber, final String description) {

    requireNonNull(id);

    this.id = id;

    this.manufacturer =
        manufacturer != null ? manufacturer : "unknown manufacturer";
    this.model = model != null ? model : "unknown model";
    this.serialNumber =
        serialNumber != null ? serialNumber : "unknown serial number";
    this.description =
        description != null ? description : "unknown description";
  }

}
