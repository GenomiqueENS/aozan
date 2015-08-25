package fr.ens.transcriptome.aozan.illumina.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntry;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntryVersion2;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetVersion2;

class SampleSheetLineReaderV2 extends SampleSheetLineReader {

  // Required in this order columns header for version2
  private static final List<String> FIELDNAMES_VERSION2_REQUIERED = Arrays
      .asList("Sample_ID");

  private static final List<String> FIELDNAMES_VERSION2_REQUIERED_FOR_QC =
      Arrays.asList("Sample_ID", "Sample_Ref", "index", "Description",
          "Sample_Project");

  private static final List<String> FIELDNAMES_VERSION2_FORBIDDEN = Arrays
      .asList("Sample_Name", "SampleName");

  private static final List<String> FIELDNAMES_VERSION2 = buildList();

  private static final List<String> SESSIONS_HEADER = Arrays.asList("[Header]",
      "[Reads]", "[Settings]", "[Data]");

  private final boolean isCompatibleForQCReport;

  private final int laneCount;

  private int fieldsCountExpected;

  private String currentSessionName;

  private boolean firstLineData = true;

  private Map<String, Integer> positionFields;

  private boolean columnLaneFound = false;

  @Override
  public void parseLine(final SampleSheet design, final List<String> fields)
      throws AozanException {

    final String firstField = fields.get(0).trim();
    final SampleSheetVersion2 design2 = (SampleSheetVersion2) design;

    // First field empty
    if (firstField.isEmpty())
      return;

    // If first field start with '[' is an new session
    if (firstField.startsWith("[") && firstField.contains("]")) {

      // Check exist
      if (!SESSIONS_HEADER.contains(firstField))
        throw new AozanException(
            "Parsing sample sheet file, invalid session name " + firstField);

      // Set currentSessionName
      this.currentSessionName = firstField;

    } else {

      if (this.currentSessionName.contains("[Data]")) {

        addSessionDataEntry(fields, design2);

      } else {
        design2.addSessionEntry(fields, this.currentSessionName);
      }
    }

  }

  private void addSessionDataEntry(List<String> fields,
      SampleSheetVersion2 design2) throws AozanException {

    if (firstLineData) {

      this.firstLineData = false;
      this.positionFields = checkHeaderColumnSessionData(fields);
      this.fieldsCountExpected = positionFields.size();

    } else {
      // assert (fields.size() == this.fieldsCountExpected);

      // Check field on sample description in run
      // trimAndCheckFields(fields, fieldsCountExpected);

      if (isColumnLaneExist()) {
        // Set lane value on sample
        int laneNumber = parseLane(fields.get(this.positionFields.get("Lane")));

        // Set sample as the same line contains in sample sheet file
        createSample(design2, fields, laneNumber);
      } else if (!isCompatibleForQCReport) {

        // Set sample as the same line contains in sample sheet file
        createSample(design2, fields, 0);

      } else {

        // Set same sample for each lane and add field lane
        createAndDuplicateSample(design2, fields);
      }
    }
  }

  private void createAndDuplicateSample(final SampleSheetVersion2 design2,
      final List<String> fields) throws AozanException {

    for (int lane = 1; lane <= this.laneCount; lane++) {

      // Create same sample for each lane number
      createSample(design2, fields, lane);
    }
  }

  private void createSample(final SampleSheetVersion2 design2,
      final List<String> fields, int laneNumber) throws AozanException {

    Preconditions.checkNotNull(design2, "sample sheet instance");
    Preconditions.checkNotNull(fields,
        "fields for one line on sample sheet file");

    final SampleEntry sample = new SampleEntryVersion2();
    sample.setLane(laneNumber);

    for (int indice = 0; indice <= this.fieldsCountExpected; indice++) {

      final String key = FIELDNAMES_VERSION2.get(indice);

      if (key == null) {
        throw new AozanException("Parsing design file, key is invalid: "
            + key + " no position found in list "
            + Joiner.on("\t").withKeyValueSeparator("=").join(positionFields));
      }

      if (this.positionFields.containsKey(key)) {
        final String value = fields.get(this.positionFields.get(key));

        switch (key) {

        case "Sample_ID":
          sample.setSampleId(value);
          break;

        case "Sample_Ref":
          sample.setSampleRef(value);
          break;

        case "index":
          sample.setIndex(value);
          break;

        case "Description":
          sample.setDescription(value);
          break;

        case "Sample_Project":
          sample.setSampleProject(value);
          break;

        default:
        }
      }
    }

    // Add sample in design instance
    design2.addSample(sample, isColumnLaneExist());
  }

  private static List<String> buildList() {
    final List<String> l = new ArrayList<>();

    l.addAll(FIELDNAMES_VERSION2_REQUIERED);
    l.addAll(FIELDNAMES_VERSION2_REQUIERED_FOR_QC);

    // Optional
    l.add("Lane");

    return Collections.unmodifiableList(l);
  }

  private Map<String, Integer> checkHeaderColumnSessionData(
      final List<String> fields) throws AozanException {

    final Map<String, Integer> pos = new HashMap<>(fields.size());

    // Check all required field exists
    for (String name : FIELDNAMES_VERSION2_REQUIERED) {
      if (!fields.contains(name)) {
        throw new AozanException(
            "Parsing Sample sheet file: missing required field for version2 "
                + name);
      }
    }

    for (String name : FIELDNAMES_VERSION2_REQUIERED_FOR_QC) {
      if (!fields.contains(name)) {
        throw new AozanException(
            "Parsing Sample sheet file: missing required field to create quality control report "
                + name);
      }
    }

    for (String name : FIELDNAMES_VERSION2_FORBIDDEN) {
      if (fields.contains(name)) {

        throw new AozanException(
            "Parsing Sample sheet file: the column name can not use " + name);
      }
    }

    // Locate fields
    for (int i = 0; i < fields.size(); i++) {
      if (FIELDNAMES_VERSION2.contains(fields.get(i))) {
        // Save position only for field useful for sample entry instance
        pos.put(fields.get(i), i);
      }
    }

    // TODO
    Common.getLogger().warning(
        "SAMPLE-SHEET: columns "
            + pos.size() + " found position "
            + Joiner.on("\t").withKeyValueSeparator(":").join(pos));

    return pos;
  }

  private boolean isColumnLaneExist() {

    return this.columnLaneFound;
  }

  //
  // Constructor
  //
  public SampleSheetLineReaderV2(final int laneCount,
      final boolean compatibleForQCReport) {

    this.isCompatibleForQCReport = compatibleForQCReport;

    this.laneCount = (laneCount == -1 ? 2 : laneCount);

  }
}