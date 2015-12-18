package fr.ens.biologie.genomique.aozan.illumina.samplesheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Sample {

  public static final String LANE_FIELD_NAME = "_lane";
  public static final String SAMPLE_ID_FIELD_NAME = "_sampleid";
  public static final String SAMPLE_NAME_FIELD_NAME = "_samplename";
  public static final String DESCRIPTION_FIELD_NAME = "_description";
  public static final String PROJECT_FIELD_NAME = "_project";
  public static final String INDEX1_FIELD_NAME = "_index1";
  public static final String INDEX2_FIELD_NAME = "_index2";
  public static final String SAMPLE_REF_FIELD_NAME = "_sampleref";

  private final SampleSheet samplesheet;

  private final Map<String, String> map = new LinkedHashMap<String, String>();

  //
  // Getters
  //

  /**
   * Get the value for a field of the sample.
   * @param fieldName field name
   * @return the value for a field of the sample
   */
  public String get(final String fieldName) {

    return this.map.get(fieldName);
  }

  /**
   * Get the lane of the sample.
   * @return the lane of the sample
   */
  public int getLane() {

    if (!this.map.containsKey(LANE_FIELD_NAME)) {
      return -1;
    }

    final String value = get(LANE_FIELD_NAME);

    if (value == null) {
      return -1;
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Get the sample Id.
   * @return the sample Id
   */
  public String getSampleId() {

    return this.map.get(SAMPLE_ID_FIELD_NAME);
  }

  /**
   * Get the sample name.
   * @return the sample name
   */
  public String getSampleName() {

    return this.map.get(SAMPLE_NAME_FIELD_NAME);
  }

  /**
   * Get the description of the sample.
   * @return the description of the sample
   */
  public String getDescription() {

    return this.map.get(DESCRIPTION_FIELD_NAME);
  }

  /**
   * Get the project related to the sample.
   * @return the project related to the sample
   */
  public String getSampleProject() {

    return this.map.get(PROJECT_FIELD_NAME);
  }

  /**
   * Get the first index of the sample.
   * @return the first index of the sample
   */
  public String getIndex1() {

    return this.map.get(INDEX1_FIELD_NAME);
  }

  /**
   * Get the second index of the sample.
   * @return the first second of the sample
   */
  public String getIndex2() {

    return this.map.get(INDEX2_FIELD_NAME);
  }

  /**
   * Get the sample reference.
   * @return the sample reference
   */
  public String getSampleRef() {

    return this.map.get(SAMPLE_REF_FIELD_NAME);
  }

  /**
   * Get the sample field names.
   * @return a list with the sample field names
   */
  public List<String> getFieldNames() {

    final List<String> result = new ArrayList<String>();

    for (String key : this.map.keySet()) {
      result.add(key);
    }

    return result;
  }

  /**
   * Test if the sample is indexed.
   * @return true if the sample is indexed
   */
  public boolean isIndexed() {

    if (this.map.containsKey(INDEX1_FIELD_NAME)) {

      final String value = getIndex1();

      if (value != null && !value.trim().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Test if the sample is indexed.
   * @return true if the sample is indexed
   */
  public boolean isDualIndexed() {

    if (isIndexed() && this.map.containsKey(INDEX2_FIELD_NAME)) {

      final String value = getIndex2();

      if (value != null && !value.trim().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the samplesheet of the sample.
   * @return a SampleSheet object
   */
  public SampleSheet getSampleSheet() {

    return this.samplesheet;
  }

  //
  // Setters
  //

  /**
   * Set a field value.
   * @param fieldName the name of the field
   * @param value the value of the field for the sample
   */
  public void set(final String fieldName, final String value) {

    if (fieldName == null) {
      throw new NullPointerException("The field name cannot be null");
    }

    if (value == null) {
      throw new NullPointerException("The value cannot be null");
    }

    this.map.put(fieldName, value);
  }

  /**
   * Set the first index.
   * @param index the first index
   */
  public void setIndex1(final String index) {

    if (index == null) {
      throw new NullPointerException("The index value cannot be null");
    }

    this.map.put(INDEX1_FIELD_NAME, index.trim());
  }

  /**
   * Set the second index.
   * @param index the second index
   */
  public void setIndex2(final String index) {

    if (index == null) {
      throw new NullPointerException("The index value cannot be null");
    }

    this.map.put(INDEX2_FIELD_NAME, index.trim());
  }

  //
  // Field test methods
  //

  /**
   * Test if a field exists for the sample.
   * @param fieldName field name
   * @return true if the field exists
   */
  public boolean isField(final String fieldName) {

    return this.map.containsKey(fieldName);
  }

  /**
   * Test if the lane field exists for the sample.
   * @return true if the field exists
   */
  public boolean isLaneField() {

    return isField(LANE_FIELD_NAME);
  }

  /**
   * Test if the sample Id field exists for the sample.
   * @return true if the field exists
   */
  public boolean isSampleIdField() {

    return isField(SAMPLE_ID_FIELD_NAME);
  }

  /**
   * Test if the sample name field exists for the sample.
   * @return true if the field exists
   */
  public boolean isSampleNameField() {

    return isField(SAMPLE_NAME_FIELD_NAME);
  }

  /**
   * Test if the description field exists for the sample.
   * @return true if the field exists
   */
  public boolean isDescriptionField() {

    return isField(DESCRIPTION_FIELD_NAME);
  }

  /**
   * Test if the project field exists for the sample.
   * @return true if the field exists
   */
  public boolean isSampleProjectField() {

    return isField(PROJECT_FIELD_NAME);
  }

  /**
   * Test if the index1 field exists for the sample.
   * @return true if the field exists
   */
  public boolean isIndex1Field() {

    return isField(INDEX1_FIELD_NAME);
  }

  /**
   * Test if the index2 field exists for the sample.
   * @return true if the field exists
   */
  public boolean isIndex2Field() {

    return isField(INDEX2_FIELD_NAME);
  }

  /**
   * Test if the sample reference field exists for the sample.
   * @return true if the field exists
   */
  public boolean isSampleRefField() {

    return isField(SAMPLE_REF_FIELD_NAME);
  }

  //
  // Other methods
  //

  public static boolean isInternalField(final String fieldName) {

    if (fieldName == null) {
      throw new NullPointerException("fieldName argument cannot be null");
    }

    // Do no use switch for string for GWT dependency
    return LANE_FIELD_NAME.equals(fieldName)
            || SAMPLE_ID_FIELD_NAME.equals(fieldName)
            || SAMPLE_NAME_FIELD_NAME.equals(fieldName)
            || DESCRIPTION_FIELD_NAME.equals(fieldName)
            || PROJECT_FIELD_NAME.equals(fieldName)
            || INDEX1_FIELD_NAME.equals(fieldName)
            || INDEX2_FIELD_NAME.equals(fieldName)
            || SAMPLE_REF_FIELD_NAME.equals(fieldName);

  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return this.getClass().getName() + "{map=" + this.map + "}";
  }

  //
  // Constructor
  //

  public Sample(final SampleSheet samplesheet) {

    if (samplesheet == null) {
      throw new NullPointerException("The samplesheet cannot be null");
    }

    this.samplesheet = samplesheet;
  }

}
