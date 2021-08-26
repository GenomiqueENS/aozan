package fr.ens.biologie.genomique.aozan.illumina.samplesheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * This class define a table section of a samplesheet.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class TableSection implements Iterable<Sample> {

  private final List<Sample> samples = new ArrayList<Sample>();
  private final SampleSheet samplesheet;

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

  /**
   * Test if the table section contains a lane field.
   * @return true if the table section contains a lane field.
   */
  public boolean isLaneSampleField() {

    return isSampleFieldName(Sample.LANE_FIELD_NAME);
  }

  /**
   * Test if the table section contains a sample Id.
   * @return true if the table section contains a sample Id.
   */
  public boolean isSampleIdSampleField() {

    return isSampleFieldName(Sample.SAMPLE_ID_FIELD_NAME);
  }

  /**
   * Test if the table section contains a sample name field.
   * @return true if the table section contains a sample name field.
   */
  public boolean isSampleNameSampleField() {

    return isSampleFieldName(Sample.SAMPLE_NAME_FIELD_NAME);
  }

  /**
   * Test if the table section contains a description field.
   * @return true if the table section contains a description field.
   */
  public boolean isDescriptionSampleField() {

    return isSampleFieldName(Sample.DESCRIPTION_FIELD_NAME);
  }

  /**
   * Test if the table section contains a project field.
   * @return true if the table section contains a project field.
   */
  public boolean isProjectSampleField() {

    return isSampleFieldName(Sample.PROJECT_FIELD_NAME);
  }

  /**
   * Test if the table section contains an index1 field.
   * @return true if the table section contains an index1 field.
   */
  public boolean isIndex1SampleField() {

    return isSampleFieldName(Sample.INDEX1_FIELD_NAME);
  }

  /**
   * Test if the table section contains an index2 field.
   * @return true if the table section contains an index2 field.
   */
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
   * Get the samples of the samplesheet.
   * @return a list of samples
   */
  public List<Sample> getSamples() {

    return Collections.unmodifiableList(this.samples);
  }

  /**
   * Get the number of samples.
   * @return the number of samples
   */
  public int size() {
    return this.samples.size();
  }

  /**
   * Test if the table is empty.
   * @return true if the table is empty
   */
  public boolean isEmpty() {

    return this.samples.isEmpty();
  }

  /**
   * Get the samplesheet of the sample.
   * @return a SampleSheet object
   */
  public SampleSheet getSampleSheet() {

    return this.samplesheet;
  }

  //
  // Constructor
  //

  TableSection(SampleSheet samplesheet) {

    Objects.requireNonNull(samplesheet);
    this.samplesheet = samplesheet;
  }

}
