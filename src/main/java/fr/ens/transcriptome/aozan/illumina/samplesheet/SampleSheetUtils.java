package fr.ens.transcriptome.aozan.illumina.samplesheet;

import static fr.ens.transcriptome.aozan.illumina.samplesheet.Sample.DESCRIPTION_FIELD_NAME;
import static fr.ens.transcriptome.aozan.illumina.samplesheet.Sample.INDEX1_FIELD_NAME;
import static fr.ens.transcriptome.aozan.illumina.samplesheet.Sample.INDEX2_FIELD_NAME;
import static fr.ens.transcriptome.aozan.illumina.samplesheet.Sample.LANE_FIELD_NAME;
import static fr.ens.transcriptome.aozan.illumina.samplesheet.Sample.PROJECT_FIELD_NAME;
import static fr.ens.transcriptome.aozan.illumina.samplesheet.Sample.SAMPLE_ID_FIELD_NAME;
import static fr.ens.transcriptome.aozan.illumina.samplesheet.Sample.SAMPLE_NAME_FIELD_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NullArgumentException;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;

/**
 * This class define samplesheet useful methods.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SampleSheetUtils {

  private static final char SEPARATOR = ',';

  /**
   * Convert a SampleSheet object to CSV.
   * @param samplesheet SampleSheet design object to convert
   * @return a String with the converted design
   */
  public static final String toSampleSheetV1CSV(final SampleSheet samplesheet) {

    final StringBuilder sb = new StringBuilder();

    sb.append(
        "\"FCID\",\"Lane\",\"SampleID\",\"SampleRef\",\"Index\",\"Description\","
            + "\"Control\",\"Recipe\",\"Operator\",\"SampleProject\"\n");

    if (samplesheet == null) {
      return sb.toString();
    }

    for (Sample s : samplesheet) {

      sb.append(samplesheet.getFlowCellId().toUpperCase());
      sb.append(SEPARATOR);
      sb.append(s.getLane());
      sb.append(SEPARATOR);
      sb.append(quote(s.getSampleId().trim()));
      sb.append(SEPARATOR);
      sb.append(quote(s.getSampleRef().trim()));
      sb.append(SEPARATOR);

      String index = s.getIndex1();
      if (!s.getIndex2().isEmpty()) {
        index += '-' + s.getIndex2();
      }

      sb.append(quote(index));
      sb.append(SEPARATOR);
      sb.append(quote(s.getDescription().trim()));
      sb.append(SEPARATOR);
      sb.append(Boolean.parseBoolean(s.get("Control")) ? 'Y' : 'N');
      sb.append(SEPARATOR);
      sb.append(quote(s.get("Recipe").trim()));
      sb.append(SEPARATOR);
      sb.append(quote(s.get("Operator").trim()));
      sb.append(SEPARATOR);
      sb.append(quote(s.getProject()));

      sb.append('\n');

    }

    return sb.toString();
  }

  /**
   * Convert the name of a field of the design model to the bcl2fastq 2
   * samplesheet field name.
   * @param fieldName the field name to convert
   * @return the converted field name
   */
  private static final String convertFieldNameV2(final String fieldName) {

    if (fieldName == null) {
      return null;
    }

    switch (fieldName) {

    case LANE_FIELD_NAME:
      return "Lane";

    case SAMPLE_ID_FIELD_NAME:
      return "Sample_ID";

    case SAMPLE_NAME_FIELD_NAME:
      return "Sample_Name";

    case DESCRIPTION_FIELD_NAME:
      return "Description";

    case PROJECT_FIELD_NAME:
      return "Sample_Project";

    case INDEX1_FIELD_NAME:
      return "index";

    case INDEX2_FIELD_NAME:
      return "index2";

    case Sample.SAMPLE_REF_FIELD_NAME:
      return "Sample_Ref";

    default:
      return fieldName;
    }
  }

  /**
   * Convert a SampleSheet object to CSV.
   * @param samplesheet SampleSheet design object to convert
   * @return a String with the converted design
   */
  public static final String toSampleSheetV2CSV(final SampleSheet samplesheet) {

    if (samplesheet == null) {
      throw new NullArgumentException(
          "the samplesheet argument cannot be null");
    }

    final StringBuilder sb = new StringBuilder();

    // Write sections
    for (String section : samplesheet.getSections()) {

      sb.append('[');
      sb.append(section);
      sb.append("]\n");

      for (Map.Entry<String, List<String>> e : samplesheet
          .getSectionMetadata(section).entrySet()) {

        final String key = e.getKey();
        for (String value : e.getValue()) {

          sb.append(key);

          if (!value.isEmpty()) {
            sb.append(SEPARATOR);
            sb.append(value);
          }

          sb.append('\n');
        }
      }

      sb.append('\n');
    }

    // Write data
    sb.append("[Data]\n");

    final List<String> fieldNames = samplesheet.getSamplesFieldNames();

    // Write header
    boolean firstHeader = true;
    for (String fieldName : fieldNames) {

      if (firstHeader) {
        firstHeader = false;
      } else {
        sb.append(SEPARATOR);
      }

      sb.append(convertFieldNameV2(fieldName));
    }
    sb.append('\n');

    for (Sample s : samplesheet) {

      boolean first = true;

      for (String fieldName : fieldNames) {

        if (first) {
          first = false;
        } else {
          sb.append(SEPARATOR);
        }

        sb.append(s.get(fieldName));
      }
      sb.append('\n');
    }

    return sb.toString();
  }

  private static String quote(final String s) {

    if (s == null) {
      return "";
    }

    final String trimmed = s.trim();

    if (s.indexOf(' ') != -1 || s.indexOf(',') != -1 || s.indexOf('\'') != -1) {
      return '\"' + trimmed + '\"';
    }
    return trimmed;
  }

  /**
   * Duplicate sample for all lanes if lane field does not exists.
   * @param samplesheet the samplesheet
   * @param laneCount the number of lanes
   */
  public static void duplicateSamplesIfLaneFieldNotSet(
      final SampleSheet samplesheet, final int laneCount) {

    if (samplesheet == null) {
      throw new NullPointerException("The samplesheet argument cannot be null");
    }

    if (laneCount < 1) {
      throw new IllegalArgumentException(
          "The lane count cannot be lower than 1");
    }

    if (samplesheet.isLaneSampleField()) {
      return;
    }

    List<Sample> samples = new ArrayList<>();

    // Copy to avoid iterator modification
    for (Sample s : samplesheet) {
      samples.add(s);
    }

    final List<Map<String, String>> samplesToDuplicate = new ArrayList<>();

    for (Sample s : samples) {

      // Set the Lane field for the first line
      s.set(Sample.LANE_FIELD_NAME, "1");

      final Map<String, String> s2 = new LinkedHashMap<>();
      samplesToDuplicate.add(s2);
      for (String fieldName : s.getFieldNames()) {
        s2.put(fieldName, s.get(fieldName));
      }
    }

    // Duplicate the sample for all the other lanes
    for (int i = 2; i <= laneCount; i++) {
      for (Map<String, String> s2Entries : samplesToDuplicate) {
        final Sample s2 = samplesheet.addSample();
        for (Map.Entry<String, String> e : s2Entries.entrySet()) {
          s2.set(e.getKey(), e.getValue());
        }

        // Overwrite the Lane field
        s2.set(Sample.LANE_FIELD_NAME, "" + i);
      }
    }
  }

  /**
   * Check if the required field for creating the QC report exists in the
   * samplesheet.
   * @param samplesheet the samplesheet
   */
  public static void checkRequiredQCSampleFields(
      final SampleSheet samplesheet) {

    if (samplesheet == null) {
      throw new NullPointerException("The samplesheet argument cannot be null");
    }

    for (String fieldName : Arrays.asList("sampleref", INDEX1_FIELD_NAME,
        DESCRIPTION_FIELD_NAME, PROJECT_FIELD_NAME)) {

      if (samplesheet.isSampleFieldName(fieldName)) {
        throw new AozanRuntimeException(
            "A required field is Missing in the samplesheet to create thr quality control report: "
                + convertFieldNameV2(fieldName));
      }
    }
  }

  //
  // Other methods
  //

  /**
   * Replace index shortcuts in a design object by index sequences.
   * @param design Casava design object
   * @param sequences map for the sequences
   * @throws EoulsanException if the shortcut is unknown
   */
  public static void replaceIndexShortcutsBySequences(final SampleSheet design,
      final Map<String, String> sequences) throws AozanException {

    if (design == null || sequences == null) {
      return;
    }

    for (final Sample sample : design) {

      if (sample.isIndex1Field()) {

        String index1 = sample.getIndex1();

        if (index1 == null) {
          throw new NullPointerException(
              "Sample index1 is null for sample: " + sample);
        }

        index1 = index1.trim().toLowerCase();

        try {
          SampleSheetCheck.checkIndex(index1);
        } catch (AozanException e) {

          if (!sequences.containsKey(index1)) {
            throw new AozanException("Unknown index 1 sequence shortcut ("
                + index1 + ") for sample: " + sample);
          }
          sample.setIndex1(sequences.get(index1));
        }
      }

      if (sample.isIndex2Field()) {

        String index2 = sample.getIndex1();

        if (index2 == null) {
          throw new NullPointerException(
              "Sample index2 is null for sample: " + sample);
        }

        index2 = index2.trim().toLowerCase();

        try {
          SampleSheetCheck.checkIndex(index2);
        } catch (AozanException e) {

          if (!sequences.containsKey(index2)) {
            throw new AozanException("Unknown index 2 sequence shortcut ("
                + index2 + ") for sample: " + sample);
          }
          sample.setIndex2(sequences.get(index2));
        }
      }
    }
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private SampleSheetUtils() {
  }

}
