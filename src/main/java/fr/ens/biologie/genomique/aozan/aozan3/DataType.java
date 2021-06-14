package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * This class define a data type
 * @author Laurent Jourdren
 * @since 3.0
 */
public class DataType {

  /**
   * Run data type.
   */
  public enum Category {
    RAW, PROCESSED, QC, ANALYZED, OTHER
  }

  /**
   * Sequencing technology
   */
  public enum SequencingTechnology {

    ILLUMINA, NANOPORE;
  }

  private Category category;
  private SequencingTechnology technology;
  private String type; // BCL, FASTQ, FAST5, Interop
  private boolean logOnly;
  private boolean partialData;

  //
  // Static methods
  //

  public static final DataType BCL =
      new DataType(Category.RAW, SequencingTechnology.ILLUMINA, "bcl");

  public static final DataType PARTIAL_BCL = new DataType(Category.RAW,
      SequencingTechnology.ILLUMINA, "bcl", false, true);

  public static final DataType INTEROP = new DataType(Category.RAW,
      SequencingTechnology.ILLUMINA, "interop", true, false);

  public static final DataType PARTIAL_INTEROP = new DataType(Category.RAW,
      SequencingTechnology.ILLUMINA, "interop", true, true);

  public static final DataType ILLUMINA_FASTQ = new DataType(Category.PROCESSED,
      SequencingTechnology.ILLUMINA, "illumina_fastq");

  public static final DataType PARTIAL_ILLUMINA_FASTQ =
      new DataType(Category.PROCESSED, SequencingTechnology.ILLUMINA,
          "illumina_fastq", false, true);

  //
  // Getters
  //

  /**
   * Get the category of Data.
   * @return the category
   */
  public Category getCategory() {

    return this.category;
  }

  /**
   * Get the sequencing technology used for the data.
   * @return the sequencing technology
   */
  public SequencingTechnology getSequencingTechnology() {

    return this.technology;
  }

  /**
   * Get the type of the data.
   * @return the type of the data
   */
  public String getType() {

    return this.type;
  }

  /**
   * Test if the run data is partial.
   * @return true if the run is partial
   */
  public boolean isPartialData() {

    return this.partialData;
  }

  /**
   * Test if the run data is partial.
   * @return true if the run is partial
   */
  public boolean isLogOnly() {

    return this.logOnly;
  }

  //
  // Update methods
  //

  /**
   * Create a new RunType object from the current object with a new type.
   * @param type the new type
   * @return a new RunType object
   */
  public DataType newCategory(final Category category) {

    requireNonNull(category);
    return new DataType(category, this.technology, this.type, this.logOnly,
        this.partialData);
  }

  /**
   * Create a new RunType object from the current object with a new type.
   * @param type the new type
   * @return a new RunType object
   */
  public DataType newTechnology(final SequencingTechnology technology) {

    requireNonNull(technology);
    return new DataType(this.category, technology, this.type, this.logOnly,
        this.partialData);
  }

  /**
   * Create a new RunType object from the current object with a new type.
   * @param type the new type
   * @return a new RunType object
   */
  public DataType newType(final String type) {

    requireNonNull(type);
    return new DataType(this.category, this.technology, checkType(type),
        this.logOnly, this.partialData);
  }

  /**
   * Create a new RunType object from the current object with a new partial data
   * setting.
   * @param partialData the new partial data value
   * @return a new RunType object
   */
  public DataType setPartialData(final boolean partialData) {

    return new DataType(this.category, this.technology, this.type, this.logOnly,
        partialData);
  }

  /**
   * Create a new RunType object from the current object with a new log only
   * setting.
   * @param logOnly the new log only value
   * @return a new RunType object
   */
  public DataType setLogOnly(final boolean logOnly) {

    return new DataType(this.category, this.technology, this.type, logOnly,
        this.partialData);
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return "DataType [category="
        + category + ", technology=" + technology + ", type=" + type
        + ", logOnly=" + logOnly + ", partialData=" + partialData + "]";
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.category, this.technology, this.type, this.logOnly,
        this.partialData);
  }

  @Override
  public boolean equals(Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof DataType)) {
      return false;
    }

    final DataType that = (DataType) o;

    return this.category == that.category
        && this.technology == that.technology
        && Objects.equals(this.type, that.type) && this.logOnly == that.logOnly
        && this.partialData == that.partialData;
  }

  //
  // Other methods
  //

  public static String checkType(String type) {

    requireNonNull(type);

    String result = type.trim().toLowerCase();

    if (result.isEmpty()) {
      throw new IllegalArgumentException(
          "Empty type are forbidden in DataType");
    }

    return result;
  }

  //
  // Constructor
  //

  public DataType(Category category, SequencingTechnology technology,
      String type) {

    this(category, technology, type, false, false);
  }

  public DataType(Category category, SequencingTechnology technology,
      String type, boolean logOnly, boolean partialData) {

    requireNonNull(category);
    requireNonNull(technology);
    requireNonNull(type);

    this.category = category;
    this.technology = technology;
    this.type = checkType(type);
    this.logOnly = logOnly;
    this.partialData = partialData;
  }

}
