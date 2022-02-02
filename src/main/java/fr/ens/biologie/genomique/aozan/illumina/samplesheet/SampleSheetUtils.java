package fr.ens.biologie.genomique.aozan.illumina.samplesheet;

import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample.DESCRIPTION_FIELD_NAME;
import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample.INDEX1_FIELD_NAME;
import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample.INDEX2_FIELD_NAME;
import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample.LANE_FIELD_NAME;
import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample.PROJECT_FIELD_NAME;
import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample.SAMPLE_ID_FIELD_NAME;
import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample.SAMPLE_NAME_FIELD_NAME;
import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet.BCLCONVERT_DEMUX_TABLE_NAME;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetDiscoverFormatParser;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetReader;

/**
 * This class define samplesheet useful methods.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SampleSheetUtils {

  private static final char SEPARATOR = ',';

  /**
   * Convert a SampleSheet object to CSV.
   * @param samplesheet SampleSheet samplesheet object to convert
   * @return a String with the converted samplesheet
   */
  public static final String toSampleSheetV1CSV(final SampleSheet samplesheet) {

    final StringBuilder sb = new StringBuilder();

    sb.append(
        "\"FCID\",\"Lane\",\"SampleID\",\"SampleRef\",\"Index\",\"Description\","
            + "\"Control\",\"Recipe\",\"Operator\",\"SampleProject\"\n");

    if (samplesheet == null) {
      return sb.toString();
    }

    TableSection table;

    try {
      table = samplesheet.getDemuxSection();
    } catch (NoSuchElementException e) {
      return sb.toString();
    }

    for (Sample s : table) {

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
      sb.append(quote(s.getSampleProject()));

      sb.append('\n');

    }

    return sb.toString();
  }

  /**
   * Convert the name of a field of the samplesheet model to the bcl2fastq 2
   * samplesheet field name.
   * @param fieldName the field name to convert
   * @return the converted field name
   */
  private static String convertFieldNameV2(final String fieldName) {

    if (fieldName == null) {
      return null;
    }

    if (LANE_FIELD_NAME.equals(fieldName)) {
      return "Lane";
    }

    if (SAMPLE_ID_FIELD_NAME.equals(fieldName)) {
      return "Sample_ID";
    }

    if (SAMPLE_NAME_FIELD_NAME.equals(fieldName)) {
      return "Sample_Name";
    }

    if (DESCRIPTION_FIELD_NAME.equals(fieldName)) {
      return "Description";
    }

    if (PROJECT_FIELD_NAME.equals(fieldName)) {
      return "Sample_Project";
    }

    if (INDEX1_FIELD_NAME.equals(fieldName)) {
      return "index";
    }

    if (INDEX2_FIELD_NAME.equals(fieldName)) {
      return "index2";
    }

    if (Sample.SAMPLE_REF_FIELD_NAME.equals(fieldName)) {
      return "Sample_Ref";
    }

    return fieldName;
  }

  /**
   * Convert a SampleSheet object to CSV.
   * @param samplesheet SampleSheet samplesheet object to convert
   * @return a String with the converted samplesheet
   */
  public static final String toSampleSheetV2CSV(final SampleSheet samplesheet) {

    if (samplesheet == null) {
      throw new NullPointerException("the samplesheet argument cannot be null");
    }

    final StringBuilder sb = new StringBuilder();

    boolean first = true;

    // Write sections
    for (String section : samplesheet.getSections()) {

      if (first) {
        first = false;
      } else {
        sb.append('\n');
      }

      sb.append(sectionHeader(section));

      if (samplesheet.containsPropertySection(section)) {
        sb.append(
            propertySectionToCSV(samplesheet.getPropertySection(section)));
      } else {
        sb.append(tableSectionToCSV(samplesheet.getTableSection(section)));
      }
    }

    return sb.toString();
  }

  private static String sectionHeader(String sectionName) {

    return '[' + quoteStringWithComma(sectionName) + "]\n";
  }

  private static String propertySectionToCSV(PropertySection propertySection) {

    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, String> e : propertySection.entrySet()) {

      sb.append(e.getKey());

      if (!e.getValue().isEmpty()) {
        sb.append(SEPARATOR);
        sb.append(quoteStringWithComma(e.getValue()));
      }

      sb.append('\n');
    }

    return sb.toString();
  }

  private static String tableSectionToCSV(TableSection tableSection) {

    StringBuilder sb = new StringBuilder();

    final List<String> fieldNames = tableSection.getSamplesFieldNames();

    // Write header
    boolean firstHeader = true;
    for (String fieldName : fieldNames) {

      if (!fieldName.isEmpty()) {

        if (firstHeader) {
          firstHeader = false;
        } else {
          sb.append(SEPARATOR);
        }

        sb.append(quoteStringWithComma(convertFieldNameV2(fieldName)));
      }
    }
    sb.append('\n');

    for (Sample s : tableSection) {

      boolean first = true;

      for (String fieldName : fieldNames) {

        if (!fieldName.isEmpty()) {

          if (first) {
            first = false;
          } else {
            sb.append(SEPARATOR);
          }

          sb.append(quoteStringWithComma(s.get(fieldName)));
        }
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
   * Quote only string containing comma
   * @param String string to process
   */
  private static String quoteStringWithComma(final String s) {

    if (s == null) {
      return "";
    }

    if (s.contains(",")) {
      return "\"" + s + "\"";
    }

    return s;
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
          "The lane count cannot be lower than 1: " + laneCount);
    }

    TableSection table;

    try {
      table = samplesheet.getDemuxSection();
    } catch (NoSuchElementException e) {
      return;
    }

    if (table.isLaneSampleField()) {
      return;
    }

    List<Sample> samples = new ArrayList<Sample>();

    // Copy to avoid iterator modification
    for (Sample s : table) {
      samples.add(s);
    }

    final List<Map<String, String>> samplesToDuplicate =
        new ArrayList<Map<String, String>>();

    for (Sample s : samples) {

      // Set the Lane field for the first line
      s.set(Sample.LANE_FIELD_NAME, "1");

      final Map<String, String> s2 = new LinkedHashMap<String, String>();
      samplesToDuplicate.add(s2);
      for (String fieldName : s.getFieldNames()) {
        s2.put(fieldName, s.get(fieldName));
      }
    }

    // Duplicate the sample for all the other lanes
    for (int i = 2; i <= laneCount; i++) {
      for (Map<String, String> s2Entries : samplesToDuplicate) {
        final Sample s2 = table.addSample();
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

    TableSection table;

    try {
      table = samplesheet.getDemuxSection();
    } catch (NoSuchElementException e) {
      return;
    }

    for (String fieldName : Arrays.asList("sampleref", INDEX1_FIELD_NAME,
        DESCRIPTION_FIELD_NAME, PROJECT_FIELD_NAME)) {

      if (table.isSampleFieldName(fieldName)) {
        throw new AozanRuntimeException(
            "A required field is Missing in the samplesheet to create thr quality control report: "
                + convertFieldNameV2(fieldName));
      }
    }
  }

  //
  // Parse methods
  //

  /**
   * Parse a samplesheet in a tabulated format from a String
   * @param s string to parse
   * @return a Bcl2fastq samplesheet object
   * @throws IOException if an error occurs
   */
  public static SampleSheet parseCSVSamplesheet(final String s)
      throws IOException {

    if (s == null) {
      return null;
    }

    try (SampleSheetReader reader = new SampleSheetReader() {

      @Override
      public SampleSheet read() throws IOException {

        final String[] lines = s.split("\n");
        final SampleSheetDiscoverFormatParser parser =
            new SampleSheetDiscoverFormatParser();

        for (final String line : lines) {

          if ("".equals(line.trim())) {
            continue;
          }

          // Parse the line
          parser.parseLine(parseCSVSamplesheetLine(line));
        }

        return parser.getSampleSheet();
      }

      @Override
      public void close() throws IOException {
      }

    }) {
      return reader.read();
    }

  }

  /**
   * Parse a samplesheet in a tabulated format from a String
   * @param s string to parse
   * @return a Bcl2fastq samplesheet object
   * @throws IOException if an error occurs
   */
  public static SampleSheet parseTabulatedSamplesheet(final String s)
      throws IOException {

    if (s == null) {
      return null;
    }

    try (SampleSheetReader reader = new SampleSheetReader() {

      @Override
      public SampleSheet read() throws IOException {

        final String[] lines = s.split("\n");
        final SampleSheetDiscoverFormatParser parser =
            new SampleSheetDiscoverFormatParser();

        for (final String line : lines) {

          if ("".equals(line.trim())) {
            continue;
          }

          // Parse the line
          parser.parseLine(parseTabulatedSamplesheetLine(line));
        }

        return parser.getSampleSheet();
      }

      @Override
      public void close() throws IOException {
      }

    }) {
      return reader.read();
    }
  }

  /**
   * Custom splitter for Bcl2fastq CSV file.
   * @param line line to parse
   * @return a list of String with the contents of each cell without unnecessary
   *         quotes
   */
  public static final List<String> parseCSVSamplesheetLine(final String line) {

    final List<String> result = new ArrayList<String>();

    if (line == null) {
      return null;
    }

    final int len = line.length();
    boolean openQuote = false;
    final StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {

      final char c = line.charAt(i);

      if (!openQuote && c == ',') {
        result.add(sb.toString());
        sb.setLength(0);
      } else {
        if (c == '"') {
          openQuote = !openQuote;
        } else {
          sb.append(c);
        }
      }

    }
    result.add(sb.toString());

    return result;
  }

  /**
   * Custom splitter for Bcl2fastq tabulated file.
   * @param line line to parse
   * @return a list of String with the contents of each cell without unnecessary
   *         quotes
   */
  public static List<String> parseTabulatedSamplesheetLine(final String line) {

    if (line == null) {
      return null;
    }

    return Arrays.asList(line.split("\t"));
  }

  //
  // Other methods
  //

  /**
   * Replace index shortcuts in a samplesheet object by index sequences.
   * @param samplesheet Bcl2fastq samplesheet object
   * @param sequences map for the sequences
   * @throws AozanException if the shortcut is unknown
   */
  public static void replaceIndexShortcutsBySequences(
      final SampleSheet samplesheet, final Map<String, String> sequences)
      throws AozanException {

    if (samplesheet == null || sequences == null) {
      return;
    }

    TableSection table;

    try {
      table = samplesheet.getDemuxSection();
    } catch (NoSuchElementException e) {
      return;
    }

    for (final Sample sample : table) {

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

        String index2 = sample.getIndex2();

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

  /**
   * Get and check if demultiplexing section exists in a samplesheet.
   * @param samplesheet the samplesheet
   * @return a TableSection object
   * @throws AozanException if the demultiplexing sample table does not exist in
   *           the samplesheet
   */
  public static TableSection getCheckedDemuxTableSection(
      SampleSheet samplesheet) throws AozanException {

    requireNonNull(samplesheet);

    try {
      return samplesheet.getDemuxSection();
    } catch (NoSuchElementException e) {
      throw new AozanException(
          "No sample table for demultiplexing found in samplesheet");
    }

  }

  /**
   * Remove forbidden fields in BCLConvert_Data section.
   * @param samplesheet sample sheet to process
   * @throws AozanException if the section does not exists
   */
  public static void removeBclConvertDataForbiddenFields(
      SampleSheet samplesheet) throws AozanException {

    requireNonNull(samplesheet);

    if (!samplesheet.containsSection(BCLCONVERT_DEMUX_TABLE_NAME)) {
      throw new AozanException("No section "
          + BCLCONVERT_DEMUX_TABLE_NAME + " found in samplesheet");
    }

    TableSection demuxTable =
        samplesheet.getTableSection(BCLCONVERT_DEMUX_TABLE_NAME);

    for (String fieldName : demuxTable.getSamplesFieldNames()) {

      switch (fieldName.toLowerCase().trim()) {

      case Sample.LANE_FIELD_NAME:
      case Sample.SAMPLE_ID_FIELD_NAME:
      case Sample.INDEX1_FIELD_NAME:
      case Sample.INDEX2_FIELD_NAME:
      case Sample.PROJECT_FIELD_NAME:
        break;

      default:

        for (Sample s : demuxTable.getSamples()) {
          s.remove(fieldName);
        }
        break;
      }
    }
  }

  /**
   * Move the forbidden fields in BCLConvert_Data section.
   * @param samplesheet sample sheet to process
   * @throws AozanException if the section does not exists
   */
  public static void moveBclConvertDataForbiddenFieldsInNewSection(
      SampleSheet samplesheet, String otherSectionName) throws AozanException {

    requireNonNull(samplesheet);
    requireNonNull(otherSectionName);

    if (!samplesheet.containsSection(BCLCONVERT_DEMUX_TABLE_NAME)) {
      throw new AozanException("No section "
          + BCLCONVERT_DEMUX_TABLE_NAME + " found in samplesheet");
    }

    if (samplesheet.containsSection(otherSectionName)) {
      throw new AozanException(
          "No section " + otherSectionName + " already found in samplesheet");
    }

    TableSection demuxTable =
        samplesheet.getTableSection(BCLCONVERT_DEMUX_TABLE_NAME);
    TableSection otherTable = samplesheet.addTableSection(otherSectionName);

    for (Sample s : demuxTable.getSamples()) {

      Sample newSample = otherTable.addSample();

      for (String fieldName : s.getFieldNames()) {

        switch (fieldName.toLowerCase().trim()) {

        case Sample.LANE_FIELD_NAME:
        case Sample.SAMPLE_ID_FIELD_NAME:
        case Sample.PROJECT_FIELD_NAME:

          newSample.set(fieldName, s.get(fieldName));
          break;

        case Sample.INDEX1_FIELD_NAME:
        case Sample.INDEX2_FIELD_NAME:
          break;

        default:
          newSample.set(fieldName, s.get(fieldName));
          s.remove(fieldName);
          break;

        }
      }
    }
  }

  /**
   * Merge a section with forbidden fields for BCL Convert in the
   * BCLConvert_Data section of the sample sheet.
   * @param samplesheet the sample sheet
   * @param otherSectionName name of the section to merge
   * @throws AozanException if an error occurs while merging sections
   */
  public static void mergeBclConvertDataAndForbiddenData(
      SampleSheet samplesheet, String otherSectionName) throws AozanException {

    requireNonNull(samplesheet);
    requireNonNull(otherSectionName);

    if (!samplesheet.containsTableSection(otherSectionName)
        || !samplesheet.containsTableSection(BCLCONVERT_DEMUX_TABLE_NAME)) {
      return;
    }

    TableSection demuxTable =
        samplesheet.getTableSection(BCLCONVERT_DEMUX_TABLE_NAME);
    TableSection otherTable = samplesheet.addTableSection(otherSectionName);

    Map<String, String> values = new HashMap<>();
    Set<String> fieldsToAdd = new HashSet<>();

    // Fill value map
    for (Sample s : otherTable.getSamples()) {

      StringBuilder sb = new StringBuilder();

      if (s.isSampleIdField()) {
        sb.append(s.getSampleId());
      }
      sb.append('\t');

      if (s.isLaneField()) {
        sb.append(s.getLane());
      }
      sb.append('\t');

      if (s.isSampleProjectField()) {
        sb.append(s.getSampleName());
      }
      sb.append('\t');

      String key = sb.toString();

      for (String fieldName : s.getFieldNames()) {

        switch (fieldName.toLowerCase().trim()) {

        case Sample.LANE_FIELD_NAME:
        case Sample.SAMPLE_ID_FIELD_NAME:
        case Sample.PROJECT_FIELD_NAME:
          break;

        default:
          fieldsToAdd.add(fieldName);
          values.put(key + '\t' + fieldName, s.get(fieldName));
          break;
        }
      }
    }

    // Add values in BCLConvert Data
    for (Sample s : demuxTable.getSamples()) {

      StringBuilder sb = new StringBuilder();

      if (s.isSampleIdField()) {
        sb.append(s.getSampleId());
      }
      sb.append('\t');

      if (s.isLaneField()) {
        sb.append(s.getLane());
      }
      sb.append('\t');

      if (s.isSampleProjectField()) {
        sb.append(s.getSampleName());
      }
      sb.append('\t');

      String prefix = sb.toString();

      for (String fieldName : fieldsToAdd) {
        String key = prefix + '\t' + fieldName;
        if (values.containsKey(key)) {
          s.set(fieldName, values.get(key));
        }
      }
    }

    // Remove merged section
    samplesheet.removeSection(otherSectionName);
  }

  /**
   * Serialize a sample sheet.
   * @param sampleSheet the sample sheet to serialize
   * @return a String with a serialized sample sheet.
   */
  public static String serialize(SampleSheet sampleSheet) {

    return toSampleSheetV2CSV(sampleSheet);
  }

  /**
   * Deserialize a sample sheet.
   * @param s String to deserialize
   * @return a SampleSheet object.
   */
  public static SampleSheet deSerialize(String s) throws IOException {

    return parseCSVSamplesheet(s);
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
