package fr.ens.biologie.genomique.aozan.illumina.samplesheet;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import fr.ens.biologie.genomique.aozan.AozanRuntimeException;

/**
 * This class define an Illumina samplesheet.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SampleSheet implements Iterable<Sample> {

  public static final String DEFAULT_TABLE_NAME = "Data";

  private List<String> sectionOrder = new ArrayList<>();
  private Map<String, PropertySection> propertySections = new HashMap<>();
  private Map<String, TableSection> tableSections = new HashMap<>();
  private TableSection defaultTable = null;

  private int version = 2;
  private String flowCellId;

  //
  // Getters
  //

  private TableSection getDefaultTableSection() {

    if (this.defaultTable == null) {
      return addTableSection(DEFAULT_TABLE_NAME);
    }

    return this.defaultTable;
  }

  /**
   * Get a property section.
   * @param sectionName section name
   * @return a PropertySection object
   */
  public PropertySection getPropertySection(String sectionName) {

    if (sectionName == null) {
      throw new NullPointerException("The section name cannot be null");
    }

    PropertySection result = this.propertySections.get(sectionName.trim());

    if (result == null) {
      throw new NoSuchElementException();
    }

    return result;
  }

  /**
   * Get a table section.
   * @param sectionName section name
   * @return a PropertySection object
   */
  public TableSection getTableSection(String sectionName) {

    if (sectionName == null) {
      throw new NullPointerException("The section name cannot be null");
    }

    TableSection result = this.tableSections.get(sectionName.trim());

    if (result == null) {
      throw new NoSuchElementException();
    }

    return result;
  }

  /**
   * Test if a section is a property section.
   * @param sectionName section name
   * @return true if the section is a property section
   */
  public boolean isPropertySection(String sectionName) {

    if (sectionName == null) {
      throw new NullPointerException("The section name cannot be null");
    }

    return this.propertySections.containsKey(sectionName.trim());
  }

  /**
   * Test if a table is a property section.
   * @param sectionName section name
   * @return true if the section is a property section
   */
  public boolean isTableSection(String sectionName) {

    if (sectionName == null) {
      throw new NullPointerException("The section name cannot be null");
    }

    return this.tableSections.containsKey(sectionName.trim());
  }

  /**
   * Get a medadata value.
   * @param sectionName the section name
   * @param key the key name
   * @return the value of the metadata
   */
  @Deprecated
  public List<String> getMetadata(final String sectionName, final String key) {

    if (sectionName == null) {
      throw new NullPointerException("The section name cannot be null");
    }

    if (key == null) {
      throw new NullPointerException("The key name cannot be null");
    }

    String result = getPropertySection(sectionName).get(key);

    if (result == null) {
      return Collections.emptyList();
    }

    return Arrays.asList(key, result);
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

    return Collections.unmodifiableList(this.sectionOrder);
  }

  /**
   * Get a section metadata.
   * @param sectionName the name of the section name
   * @return the metadata of a section in a Map object
   */
  @Deprecated
  public Map<String, List<String>> getSectionMetadata(
      final String sectionName) {

    if (sectionName == null) {
      throw new NullPointerException("section argument cannot be null");
    }

    final Map<String, List<String>> result =
        new LinkedHashMap<String, List<String>>();

    PropertySection section = this.getPropertySection(sectionName);

    if (section == null) {
      return result;
    }

    for (String key : section.keySet()) {
      result.put(key, getMetadata(sectionName, key));
    }

    return result;
  }

  //
  // Setters
  //

  /**
   * Add a property section.
   * @param sectionName name of the section to add
   * @return a PropertySection object
   */
  public PropertySection addPropertySection(String sectionName) {

    requireNonNull(sectionName);

    String trimmedSectionName = sectionName.trim();

    if (trimmedSectionName.isEmpty()) {
      throw new IllegalArgumentException("sectionName cannot be empty");
    }

    if (this.sectionOrder.contains(trimmedSectionName)) {
      throw new IllegalArgumentException(
          "section already exists: " + sectionName);
    }

    PropertySection result = new PropertySection();
    this.propertySections.put(trimmedSectionName, result);
    this.sectionOrder.add(trimmedSectionName);

    return result;
  }

  /**
   * Add a table section.
   * @param sectionName name of the section to add
   * @return a TableSection object
   */
  public TableSection addTableSection(String sectionName) {

    requireNonNull(sectionName);

    String trimmedSectionName = sectionName.trim();

    if (trimmedSectionName.isEmpty()) {
      throw new IllegalArgumentException("sectionName cannot be empty");
    }

    if (this.sectionOrder.contains(trimmedSectionName)) {
      throw new IllegalArgumentException(
          "section already exists: " + sectionName);
    }

    TableSection result = new TableSection(this);
    this.tableSections.put(trimmedSectionName, result);
    this.sectionOrder.add(trimmedSectionName);

    if (this.defaultTable == null) {
      this.defaultTable = result;
    }

    return result;
  }

  /**
   * Remove a section
   * @param sectionName name of the section to remove
   */
  public void removeSection(String sectionName) {

    requireNonNull(sectionName);

    String trimmedSectionName = sectionName.trim();

    if (trimmedSectionName.isEmpty()) {
      throw new IllegalArgumentException("sectionName cannot be empty");
    }

    if (!this.sectionOrder.contains(trimmedSectionName)) {
      throw new IllegalArgumentException(
          "section does not exists: " + sectionName);
    }

    if (this.propertySections.remove(trimmedSectionName) != null) {
      this.tableSections.remove(trimmedSectionName);
    }

    this.sectionOrder.remove(trimmedSectionName);
  }

  /**
   * Add a metadata of the samplesheet.
   * @param section the section of the metadata
   * @param key the key of the metadata
   * @param value the value of the metadata
   */
  @Deprecated
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

    String trimmedSection = section.trim();

    final PropertySection propertySection;

    if (!this.propertySections.containsKey(trimmedSection)) {

      if (this.sectionOrder.contains(trimmedSection)) {
        throw new IllegalArgumentException(
            "Section argument is a table section: " + section);
      }

      propertySection = addPropertySection(trimmedSection);
    } else {
      propertySection = getPropertySection(trimmedSection);
    }

    propertySection.set(key, value);
  }

  @Deprecated
  public void addMetadataSection(final String section) {

    addPropertySection(section);
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

  @Deprecated
  public Sample addSample() {

    return getDefaultTableSection().addSample();
  }

  @Override
  @Deprecated
  public Iterator<Sample> iterator() {

    return getDefaultTableSection().iterator();
  }

  /**
   * Get the samples fields names.
   * @return the samples fields names
   */
  public List<String> getSamplesFieldNames() {

    return getDefaultTableSection().getSamplesFieldNames();
  }

  @Deprecated
  public boolean isSampleFieldName(final String fieldName) {

    return getDefaultTableSection().isSampleFieldName(fieldName);
  }

  @Deprecated
  public boolean isLaneSampleField() {

    return getDefaultTableSection().isLaneSampleField();
  }

  @Deprecated
  public boolean isSampleIdSampleField() {

    return getDefaultTableSection().isSampleIdSampleField();
  }

  @Deprecated
  public boolean isSampleNameSampleField() {

    return getDefaultTableSection().isSampleNameSampleField();
  }

  @Deprecated
  public boolean isDescriptionSampleField() {

    return getDefaultTableSection().isDescriptionSampleField();
  }

  @Deprecated
  public boolean isProjectSampleField() {

    return getDefaultTableSection().isProjectSampleField();
  }

  @Deprecated
  public boolean isIndex1SampleField() {

    return getDefaultTableSection().isIndex1SampleField();
  }

  @Deprecated
  public boolean isIndex2SampleField() {

    return getDefaultTableSection().isIndex2SampleField();
  }

  @Deprecated
  public List<Sample> getSampleInLane(final int lane) {

    return getDefaultTableSection().getSampleInLane(lane);
  }

  @Deprecated
  public List<Sample> getSamples() {

    return getDefaultTableSection().getSamples();
  }

  @Deprecated
  public int size() {

    return getDefaultTableSection().size();
  }

}
