package fr.ens.biologie.genomique.aozan.illumina.samplesheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.aozan.AozanRuntimeException;

public class SampleSheet implements Iterable<Sample> {

  private Map<String, List<String>> metadata =
      new LinkedHashMap<String, List<String>>();
  private List<Sample> samples = new ArrayList<Sample>();

  private int version = 2;
  private String flowCellId;

  //
  // Getters
  //

  /**
   * Get a medadata value.
   * @param section the section name
   * @param key the key name
   * @return the value of the metadata
   */
  public List<String> getMetadata(final String section, final String key) {

    if (section == null) {
      throw new NullPointerException("The section name cannot be null");
    }

    if (key == null) {
      throw new NullPointerException("The key name cannot be null");
    }

    return this.metadata.get(section.trim() + '.' + key.trim());
  }

  /**
   * Get the flow cell Id.
   * @return the flow cell Id
   */
  public String getFlowCellId() {

    return this.flowCellId;
  }

  /**
   * Get the version of the samplesheet model.
   * @return the version of the samplesheet model
   */
  public int getVersion() {

    return this.version;
  }

  /**
   * Get the names of sections of the metadata.
   * @return the names of sections of the metadata
   */
  public List<String> getSections() {

    final List<String> result = new ArrayList<String>();

    for (String key : this.metadata.keySet()) {

      final int pos = key.indexOf('.');

      if (pos == -1) {
        result.add(key);
      }
    }

    return result;
  }

  /**
   * Get a section metadata.
   * @param sectionName the name of the section name
   * @return the metadata of a section in a Map object
   */
  public Map<String, List<String>> getSectionMetadata(
      final String sectionName) {

    if (sectionName == null) {
      throw new NullPointerException("section argument cannot be null");
    }

    final Map<String, List<String>> result =
        new LinkedHashMap<String, List<String>>();

    for (Map.Entry<String, List<String>> e : this.metadata.entrySet()) {

      final int pos = e.getKey().indexOf('.');

      if (pos == -1) {
        continue;
      }

      if (sectionName.equals(e.getKey().substring(0, pos))) {

        result.put(e.getKey().substring(pos + 1), e.getValue());
      }
    }

    return result;
  }

  //
  // Setters
  //

  /**
   * Add a metadata of the samplesheet.
   * @param section the section of the metadata
   * @param key the key of the metadata
   * @param value the value of the metadata
   */
  public void addMetadata(final String section, final String key,
      final String value) {

    if (section == null) {
      throw new NullPointerException("The section name cannot be null");
    }

    if (key == null) {
      throw new NullPointerException("The key name cannot be null");
    }

    if (value == null) {
      throw new NullPointerException("The value cannot be null");
    }

    if (section.contains(".")) {
      throw new NullPointerException(
          "The section name cannot contains a '.' character");
    }

    if (!metadata.containsKey(section)) {
      this.metadata.put(section, null);
    }

    final String mapKey = section.trim() + '.' + key.trim();

    final List<String> values;
    if (this.metadata.containsKey(mapKey)) {
      values = this.metadata.get(mapKey);
    } else {
      values = new ArrayList<String>();
      this.metadata.put(mapKey, values);
    }

    values.add(value.trim());
  }

  public void addMetadataSection(final String section) {

    if (section == null) {
      throw new NullPointerException("The section name cannot be null");
    }

    if (section.contains(".")) {
      throw new NullPointerException(
          "The section name cannot contains a '.' character");
    }

    if (!metadata.containsKey(section)) {
      this.metadata.put(section, null);
    }

  }

  /**
   * Set the flow cell id.
   * @param flowcellId the flow cell id
   */
  public void setFlowCellId(final String flowcellId) {

    if (flowcellId == null) {
      throw new NullPointerException("The flowcellId argument cannot be null");
    }

    final String trimmedFlowCellId = flowcellId.trim();

    if (this.flowCellId != null && !this.flowCellId.equals(trimmedFlowCellId)) {
      throw new AozanRuntimeException(
          "The samplesheet cannot handle two or more flowcell ids: "
              + flowcellId);
    }

    this.flowCellId = flowcellId;
  }

  /**
   * Set the version of the samplesheet model.
   * @param version the version of the samplesheet model
   */
  public void setVersion(int version) {

    if (version < 1 || version > 2) {
      throw new AozanRuntimeException(
          "Unsupported samplesheet version: " + version);
    }

    this.version = version;
  }

  //
  // Sample Handling
  //

  /**
   * Add a sample.
   * @return the new sample
   */
  public Sample addSample() {

    final Sample sample = new Sample(this);
    this.samples.add(sample);

    return sample;
  }

  @Override
  public Iterator<Sample> iterator() {

    return samples.iterator();
  }

  /**
   * Get the samples fields names.
   * @return the samples fields names
   */
  public List<String> getSamplesFieldNames() {

    final List<String> result = new ArrayList<String>();

    for (Sample s : this) {

      for (String fieldName : s.getFieldNames()) {

        if (!result.contains(fieldName)) {
          result.add(fieldName);
        }
      }
    }

    return result;
  }

  /**
   * Test if the sample field exists.
   * @param fieldName the name of the field name to test
   * @return true if the sample field exists
   */
  public boolean isSampleFieldName(final String fieldName) {

    if (fieldName == null) {
      throw new NullPointerException("fieldName argument cannot be null");
    }

    return getSamplesFieldNames().contains(fieldName);
  }

  public boolean isLaneSampleField() {

    return isSampleFieldName(Sample.LANE_FIELD_NAME);
  }

  public boolean isSampleIdSampleField() {

    return isSampleFieldName(Sample.SAMPLE_ID_FIELD_NAME);
  }

  public boolean isSampleNameSampleField() {

    return isSampleFieldName(Sample.SAMPLE_NAME_FIELD_NAME);
  }

  public boolean isDescriptionSampleField() {

    return isSampleFieldName(Sample.DESCRIPTION_FIELD_NAME);
  }

  public boolean isProjectSampleField() {

    return isSampleFieldName(Sample.PROJECT_FIELD_NAME);
  }

  public boolean isIndex1SampleField() {

    return isSampleFieldName(Sample.INDEX1_FIELD_NAME);
  }

  public boolean isIndex2SampleField() {

    return isSampleFieldName(Sample.INDEX2_FIELD_NAME);
  }

  /**
   * Get all the samples of a lane.
   * @param lane the lane of the samples
   * @return a list of the samples in the lane in the same order as the
   *         samplesheet
   */
  public List<Sample> getSampleInLane(final int lane) {

    if (lane < 1 || !isLaneSampleField()) {
      return Collections.emptyList();
    }

    final List<Sample> result = new ArrayList<Sample>();

    for (Sample s : this) {
      if (s.getLane() == lane) {
        result.add(s);
      }
    }

    return result;
  }

  /**
   * Get the number of samples.
   * @return the number of samples
   */
  public int size() {
    return this.samples.size();
  }

}
