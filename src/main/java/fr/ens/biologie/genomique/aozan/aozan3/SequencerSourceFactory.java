package fr.ens.biologie.genomique.aozan.aozan3;

import static fr.ens.biologie.genomique.aozan.aozan3.SequencerSourceFactory.Manufacturer.ILLUMINA;
import static java.util.Objects.requireNonNull;

/**
 * This class define a sequencer source factory.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class SequencerSourceFactory {

  /**
   * This enum defines sequencer manufacturers.
   */
  public enum Manufacturer {
    ILLUMINA("Illumina");

    private String name;

    public String toString() {
      return this.name;
    }

    Manufacturer(String name) {
      this.name = name;
    }
  }

  /**
   * This enum defines Illumina sequencer models.
   */
  public enum IlluminaModel {
    HISEQ_1000("HiSeq 1000"), HISEQ_1500("HiSeq 1500"),
    HISEQ_2000("HiSeq 2000"), HISEQ_2500("HiSeq 2500"),
    NEXTSEQ_500("NextSeq 500");

    private String name;

    public String toString() {
      return this.name;
    }

    IlluminaModel(String name) {
      this.name = name;
    }

  }

  /**
   * Create a new sequencer source from an Illumina runId.
   * @param illuminaRunId illumina runId
   * @return a new SequencerSource object
   */
  public static SequencerSource newIlluminaSequencerSource(
      RunId illuminaRunId) {

    requireNonNull(illuminaRunId);

    IlluminaRunIdWrapper wrapper = new IlluminaRunIdWrapper(illuminaRunId);

    return new SequencerSource(wrapper.getInstrumentSerialNumber(),
        Manufacturer.ILLUMINA.toString(), null,
        wrapper.getInstrumentSerialNumber(), null);
  }

  /**
   * Create a new Illumina sequencer source.
   * @param id sequencer identifier
   * @param model sequencer model
   * @param serialModel sequencer serial model
   * @param description sequencer description
   * @return a new SequencerSource object
   */
  public static SequencerSource newIlluminaSequencerSource(
      IlluminaModel model) {

    requireNonNull(model);

    return new SequencerSource(model.toString(), ILLUMINA.toString(),
        model.toString(), null, null);
  }

  /**
   * Create a new Illumina sequencer source.
   * @param id sequencer identifier
   * @param model sequencer model
   * @param serialModel sequencer serial model
   * @param description sequencer description
   * @return a new SequencerSource object
   */
  public static SequencerSource newIlluminaSequencerSource(String id,
      IlluminaModel model, String serialModel, String description) {

    requireNonNull(id);
    requireNonNull(model);

    return new SequencerSource(id, ILLUMINA.toString(), model.toString(),
        serialModel, description);
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private SequencerSourceFactory() {
  }

}
