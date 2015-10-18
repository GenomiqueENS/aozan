package fr.ens.transcriptome.aozan.illumina.samplesheet.io;

import static fr.ens.transcriptome.aozan.illumina.samplesheet.io.SampleSheetReaderUtils.parseLane;
import static fr.ens.transcriptome.aozan.illumina.samplesheet.io.SampleSheetReaderUtils.trimFields;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.aozan.illumina.samplesheet.Sample;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;

public class SampleSheetV2Parser implements SampleSheetParser {

  public static final String DATA_SECTION_NAME = "data";

  private final SampleSheet samplesheet;

  private String currentSection;
  private boolean dataSection;
  private List<String> header = new ArrayList<>();

  /**
   * Convert a field name to internal field name.
   * @param fieldName the field name to convert
   * @return the field name to use with SampleSheet internal model
   */
  private String convertFieldName(final String fieldName) {

    if (fieldName == null) {
      return null;
    }

    switch (fieldName) {

    case "Sample_Project":
      return Sample.PROJECT_FIELD_NAME;

    case "Lane":
      return Sample.LANE_FIELD_NAME;

    case "Sample_ID":
      return Sample.SAMPLE_ID_FIELD_NAME;

    case "Sample_Name":
      return Sample.SAMPLE_NAME_FIELD_NAME;

    case "index":
      return Sample.INDEX1_FIELD_NAME;

    case "index2":
      return Sample.INDEX2_FIELD_NAME;

    case "Description":
      return Sample.DESCRIPTION_FIELD_NAME;

    case "Sample_Ref":
      return Sample.SAMPLE_REF_FIELD_NAME;

    default:
      return fieldName;
    }

  }

  @Override
  public void parseLine(final List<String> fields) throws IOException {

    trimFields(fields);

    if (fields.isEmpty()) {
      return;
    }

    String firstField = fields.get(0);

    if (firstField.startsWith("[")) {

      if (!firstField.endsWith("]")) {

        throw new IOException(
            "Section header do not ends with ']': " + firstField);
      }

      final String sectionName =
          firstField.substring(1, firstField.length() - 1).trim();

      if (DATA_SECTION_NAME.equals(sectionName.toLowerCase())) {
        dataSection = true;
      } else {
        samplesheet.addMetadataSection(sectionName);
      }

      this.currentSection = sectionName;
      return;
    }

    if (!dataSection) {

      final String value = fields.size() > 1 ? fields.get(1).trim() : "";

      this.samplesheet.addMetadata(this.currentSection, fields.get(0), value);
      return;
    }

    // Set the header
    if (this.header.isEmpty()) {
      this.header.addAll(fields);
      return;
    }

    final Sample sample = this.samplesheet.addSample();

    final int headerLength = this.header.size();

    for (int i = 0; i < headerLength; i++) {

      final String key = convertFieldName(this.header.get(i));
      String value = i < fields.size() ? fields.get(i) : "";

      if (Sample.LANE_FIELD_NAME.equals(key)) {

        value = "" + parseLane(value);
      }

      sample.set(key, value);
    }

  }

  @Override
  public SampleSheet getSampleSheet() {

    return this.samplesheet;
  }

  //
  // Constructor
  //

  protected SampleSheetV2Parser() {

    this.samplesheet = new SampleSheet();
    this.samplesheet.setVersion(2);
  }

}
