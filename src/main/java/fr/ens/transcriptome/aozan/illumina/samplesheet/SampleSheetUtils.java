/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.illumina.samplesheet;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingStandardFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.illumina.io.AbstractCasavaDesignTextReader;
import fr.ens.transcriptome.aozan.illumina.io.CasavaDesignCSVReader;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntry;
import fr.ens.transcriptome.aozan.io.CasavaDesignXLSReader;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This abstract class contains common utility methods for sample sheet.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class SampleSheetUtils {

  public static final String SEP = ",";

  public static final String VERSION_1 = "1";
  public static final String VERSION_2 = "2";

  public static SampleSheet getSampleSheet(final File sampleSheetFile,
      String sampleSheetVersion, final int laneCount)
      throws FileNotFoundException, IOException, AozanException {

    checkArgument(!Strings.isNullOrEmpty(sampleSheetVersion),
        "sample sheet version");
    checkExistingStandardFile(sampleSheetFile, "sample sheet");

    final String extension = StringUtils.extension(sampleSheetFile.getName());

    switch (extension) {
    case ".csv":
      if (laneCount < 1)
        return new CasavaDesignCSVReader(sampleSheetFile)
            .read(sampleSheetVersion);
      return new CasavaDesignCSVReader(sampleSheetFile).readForQCReport(
          sampleSheetVersion, laneCount);

    case ".xls":
      if (laneCount < 1)
        return new CasavaDesignXLSReader(sampleSheetFile)
            .read(sampleSheetVersion);

      return new CasavaDesignXLSReader(sampleSheetFile).readForQCReport(
          sampleSheetVersion, laneCount);

    default:
      throw new AozanException(
          "Sample sheet: create instance from file fail, extension ("
              + extension + ") invalid " + sampleSheetFile);
    }

  }

  public static SampleSheet getSampleSheet(final String sampleSheetFilename,
      String sampleSheetVersion, final int laneCount)
      throws FileNotFoundException, IOException, AozanException {

    return getSampleSheet(new File(sampleSheetFilename), sampleSheetVersion,
        laneCount);

  }

  /**
   * Check a Casava design object.
   * @param design Casava design object to check
   * @return a list of warnings
   * @throws AozanException if the design is not valid
   */
  public static List<String> checkCasavaDesign(final SampleSheet design)
      throws AozanException {

    return checkCasavaDesign(design, null);
  }

  /**
   * Check a Casava design object.
   * @param design Casava design object to check
   * @param flowCellId flow cell id
   * @return a list of warnings
   * @throws AozanException if the design is not valid
   */
  public static List<String> checkCasavaDesign(final SampleSheet design,
      final String flowCellId) throws AozanException {

    if (design == null) {
      throw new NullPointerException("The design object is null");
    }

    if (design.size() == 0) {
      throw new AozanException("No samples found in the design.");
    }

    if (design.isVersion1()) {
      // Check version 1
      SampleSheetVersion1Utils.checkSampleSheet(design, flowCellId);
    } else {
      // Check version 2
      SampleSheetVersion2Utils.checkSampleSheet(design);
    }

    final List<String> warnings = new ArrayList<>();

    final Map<Integer, Set<String>> indexes = new HashMap<>();
    final Set<String> sampleIds = new HashSet<>();
    final Set<Integer> laneWithIndexes = new HashSet<>();
    final Set<Integer> laneWithoutIndexes = new HashSet<>();
    final Map<String, Set<Integer>> sampleInLanes = new HashMap<>();
    final Map<String, String> samplesProjects = new HashMap<>();
    final Map<String, String> samplesIndex = new HashMap<>();

    for (SampleEntry sample : design) {

      // Check if the sample is null or empty
      checkSampleId(sample.getSampleId(), sampleIds);

      // Check sample reference
      checkSampleRef(sample.getSampleId(), sample.getSampleRef());

      // Check index
      checkIndex(sample.getIndex());

      // Check sample Index
      checkSampleIndex(sample.getSampleId(), sample.getIndex(), samplesIndex);

      // Check the description
      checkSampleDescription(sample.getSampleId(), sample.getDescription());

      // Check sample project
      checkSampleProject(sample.getSampleProject());
      checkCharset(sample.getSampleProject());

      final String index = sample.getIndex();
      final int lane = sample.getLane();

      // Check if mixing lane with index and lanes without index
      if (index == null || "".equals(index.trim())) {

        if (laneWithoutIndexes.contains(lane)) {
          throw new AozanException(
              "Found two samples without index for the same lane: "
                  + lane + ".");
        }

        if (laneWithIndexes.contains(lane)) {
          throw new AozanException(
              "Found a lane with indexed and non indexed samples: "
                  + lane + ".");
        }

        laneWithoutIndexes.add(lane);
      } else {

        if (laneWithoutIndexes.contains(lane)) {
          throw new AozanException(
              "Found a lane with indexed and non indexed samples: "
                  + lane + ".");
        }
        laneWithIndexes.add(lane);
      }

      // check if a lane has not two or more same indexes
      if (indexes.containsKey(lane)) {

        if (indexes.get(lane).contains(index)) {
          throw new AozanException(
              "Found a lane with two time the same index: "
                  + lane + " (" + index + ").");
        }

      } else {
        indexes.put(lane, new HashSet<String>());
      }

      // Check sample and project
      checkSampleAndProject(sample.getSampleId(), sample.getSampleProject(),
          sample.getLane(), sampleInLanes, samplesProjects, warnings);

      indexes.get(lane).add(index);
    }

    // Add warnings for samples in several lanes
    checkSampleInLanes(sampleInLanes, warnings);

    // Return unique warnings
    final List<String> result = new ArrayList<>(new HashSet<>(warnings));
    Collections.sort(result);

    return result;

  }

  private static void checkCharset(final String s) throws AozanException {

    if (s == null) {
      return;
    }

    for (int i = 0; i < s.length(); i++) {

      final int c = s.codePointAt(i);

      if (c < ' ' || c >= 127) {
        throw new AozanException("Found invalid character '"
            + (char) c + "' in \"" + s + "\".");
      }
    }

  }

  private static void checkFCID(final String fcid) throws AozanException {

    if (isNullOrEmpty(fcid)) {
      throw new AozanException("Flow cell id is null or empty.");
    }

    // Check charset
    checkCharset(fcid);

    for (int i = 0; i < fcid.length(); i++) {

      final char c = fcid.charAt(i);
      if (!(Character.isLetterOrDigit(c))) {
        throw new AozanException(
            "Invalid flow cell id, only letters or digits are allowed : "
                + fcid + ".");
      }
    }

  }

  private static void checkSampleId(final String sampleId,
      final Set<String> sampleIds) throws AozanException {

    // Check if null of empty
    if (isNullOrEmpty(sampleId)) {
      throw new AozanException("Found a null or empty sample id.");
    }

    // Check charset
    checkCharset(sampleId);

    // Check for forbidden characters
    for (int i = 0; i < sampleId.length(); i++) {

      final char c = sampleId.charAt(i);
      if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-')) {
        throw new AozanException(
            "Invalid sample id, only letters, digits, '-' or '_' characters are allowed : "
                + sampleId + ".");
      }
    }

    sampleIds.add(sampleId);
  }

  private static void checkSampleRef(final String sampleId,
      final String sampleRef) throws AozanException {

    // Check if null of empty
    if (isNullOrEmpty(sampleRef)) {
      throw new AozanException(
          "Found a null or empty sample reference for sample: "
              + sampleId + ".");
    }

    // Check charset
    checkCharset(sampleRef);

    // Check for forbidden characters
    for (int i = 0; i < sampleRef.length(); i++) {
      final char c = sampleRef.charAt(i);
      if (!(Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_')) {
        throw new AozanException(
            "Invalid sample reference, only letters, digits, ' ', '-' or '_' characters are allowed: "
                + sampleRef + ".");
      }
    }
  }

  private static void checkIndex(final String index) throws AozanException {

    if (index == null) {
      return;
    }

    for (String subIndex : index.split("-")) {

      for (int i = 0; i < subIndex.length(); i++) {
        switch (subIndex.codePointAt(i)) {

        case 'A':
        case 'a':
        case 'T':
        case 't':
        case 'G':
        case 'g':
        case 'C':
        case 'c':
          break;

        default:
          throw new AozanException("Invalid index found: " + index + ".");
        }
      }
    }
  }

  private static void checkSampleDescription(final String sampleId,
      final String sampleDescription) throws AozanException {

    // Check if null of empty
    if (isNullOrEmpty(sampleDescription)) {
      throw new AozanException("Found a null or empty description for sample: "
          + sampleId);
    }

    // Check charset
    checkCharset(sampleDescription);

    // Check for forbidden characters
    for (int i = 0; i < sampleDescription.length(); i++) {
      final char c = sampleDescription.charAt(i);
      if (c == '\'' || c == '\"') {
        throw new AozanException("Invalid sample description, '"
            + c + "' character is not allowed: " + sampleDescription + ".");
      }
    }
  }

  private static void checkSampleProject(final String sampleProject)
      throws AozanException {

    // Check if null of empty
    if (isNullOrEmpty(sampleProject)) {
      throw new AozanException("Found a null or sample project.");
    }

    // Check for forbidden characters
    for (int i = 0; i < sampleProject.length(); i++) {
      final char c = sampleProject.charAt(i);
      if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) {
        throw new AozanException(
            "Invalid sample project, only letters, digits, '-' or '_' characters are allowed: "
                + sampleProject + ".");
      }
    }
  }

  private static void checkSampleAndProject(final String sampleId,
      final String projectName, final int lane,
      final Map<String, Set<Integer>> sampleInLanes,
      final Map<String, String> samplesProjects, final List<String> warnings)
      throws AozanException {

    // Check if two or more project use the same sample
    if (samplesProjects.containsKey(sampleId)
        && !samplesProjects.get(sampleId).equals(projectName)) {
      throw new AozanException("The sample \""
          + sampleId + "\" is used by two or more projects.");
    }

    samplesProjects.put(sampleId, projectName);

    final Set<Integer> lanes;
    if (!sampleInLanes.containsKey(sampleId)) {
      lanes = new HashSet<>();
      sampleInLanes.put(sampleId, lanes);
    } else {
      lanes = sampleInLanes.get(sampleId);
    }

    if (lanes.contains(lane)) {
      warnings.add("The sample \""
          + sampleId + "\" exists two or more times in the lane " + lane + ".");
    }

    lanes.add(lane);
  }

  private static void checkSampleInLanes(
      final Map<String, Set<Integer>> sampleInLanes, final List<String> warnings) {

    for (Map.Entry<String, Set<Integer>> e : sampleInLanes.entrySet()) {

      final Set<Integer> lanes = e.getValue();
      if (lanes.size() > 1) {

        final StringBuilder sb = new StringBuilder();
        sb.append("The sample \"");
        sb.append(e.getKey());
        sb.append("\" exists in lanes: ");

        final List<Integer> laneSorted = new ArrayList<>(lanes);
        Collections.sort(laneSorted);

        boolean first = true;
        for (int lane : laneSorted) {

          if (first) {
            first = false;
          } else {
            sb.append(", ");
          }
          sb.append(lane);
        }
        sb.append('.');

        warnings.add(sb.toString());
      }
    }
  }

  private static final void checkSampleIndex(final String sampleName,
      final String index, final Map<String, String> samplesIndex)
      throws AozanException {

    if (samplesIndex.containsKey(sampleName)
        && !samplesIndex.get(sampleName).equals(index)) {
      throw new AozanException("The sample \""
          + sampleName
          + "\" is defined in several lanes but without the same index.");
    }

    samplesIndex.put(sampleName, index);
  }

  //
  // Other methods
  //

  /**
   * Replace index shortcuts in a design object by index sequences.
   * @param design Casava design object
   * @param sequences map for the sequences
   * @throws AozanException if the shortcut is unknown
   */
  public static void replaceIndexShortcutsBySequences(final SampleSheet design,
      final Map<String, String> sequences) throws AozanException {

    if (design == null || sequences == null) {
      return;
    }

    for (final SampleEntry sample : design) {

      if (sample.getIndex() == null) {
        throw new NullPointerException("Sample index is null for sample: "
            + sample);
      }

      final String index = sample.getIndex().trim();

      try {
        checkIndex(index);
      } catch (AozanException e) {

        final StringBuilder sb = new StringBuilder();

        for (String subIndex : index.split("-")) {

          if (!sequences.containsKey(subIndex.toLowerCase())) {
            throw new AozanException("Unknown index sequence shortcut ("
                + index + ") for sample: " + sample);
          }

          if (sb.length() > 0) {
            sb.append('-');
          }
          sb.append(sequences.get(subIndex.toLowerCase()));
        }

        sample.setIndex(sb.toString());
      }
    }
  }

  //
  // Parsing methods
  //

  /**
   * Convert a Casava design to CSV.
   * @param design Casava design object to convert
   * @return a String with the converted design
   */
  public static final String toCSV(final SampleSheet design) {

    return design.isVersion1()
        ? SampleSheetVersion1Utils.toCSV(design) : SampleSheetVersion2Utils
            .toCSV(design);
  }

  /**
   * Parse a design in a tabulated format from a String
   * @param s string to parse
   * @return a Casava Design object
   * @throws IOException if an error occurs
   */
  public static SampleSheet parseTabulatedDesign(final String s,
      final String version) throws AozanException {

    if (s == null) {
      return null;
    }

    return new AbstractCasavaDesignTextReader() {

      @Override
      public SampleSheet read(final String version) throws AozanException {

        final String[] lines = s.split("\n");

        for (final String line : lines) {

          if ("".equals(line.trim())) {
            continue;
          }

          parseLine(parseTabulatedDesignLine(line), version);
        }

        return getDesign();
      }
    }.read(version);
  }

  /**
   * Parse a design in a tabulated format from a String.
   * @param s string to parse
   * @param version the version
   * @return a Casava Design object
   * @throws AozanException the aozan exception
   */
  public static SampleSheet parseCSVDesign(final String s, final String version)
      throws AozanException {

    if (s == null) {
      return null;
    }

    return new AbstractCasavaDesignTextReader() {

      @Override
      public SampleSheet read(final String version) throws AozanException {

        final String[] lines = s.split("\n");

        for (final String line : lines) {

          if ("".equals(line.trim())) {
            continue;
          }

          parseLine(parseCSVDesignLine(line), version);
        }

        return getDesign();
      }
    }.read(version);
  }

  /**
   * Custom splitter for Casava tabulated file.
   * @param line line to parse
   * @return a list of String with the contents of each cell without unnecessary
   *         quotes
   */
  public static List<String> parseTabulatedDesignLine(final String line) {

    if (line == null) {
      return null;
    }

    return Arrays.asList(line.split("\t"));
  }

  /**
   * Custom splitter for Casava CSV file.
   * @param line line to parse
   * @return a list of String with the contents of each cell without unnecessary
   *         quotes
   */
  public static final List<String> parseCSVDesignLine(final String line) {

    final List<String> result = new ArrayList<>();

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

  //
  // Static method
  //
  /**
   * Find bcl2fastq version.
   * @param version the version
   * @return the string
   */
  public static String findBcl2fastqMajorVersion(final String fullVersion) {

    checkArgument(!Strings.isNullOrEmpty(fullVersion),
        "bcl2fastq full version name: " + fullVersion);

    if (fullVersion.startsWith(VERSION_1))
      return VERSION_1;

    if (fullVersion.startsWith(VERSION_2) || fullVersion.startsWith("latest"))

      return VERSION_2;

    // Throw an exception version invalid for pipeline
    throw new AozanRuntimeException(
        "Demultiplexing collector, can be recognize bcl2fastq version (not start with 1 or 2) : "
            + fullVersion);
  }

  public static boolean isBcl2fastqVersion1(final String version) {

    // Check it is a full version name or major
    final String majorVersion =
        (version.indexOf(".") > 0
            ? findBcl2fastqMajorVersion(version) : version);

    return majorVersion.equals(VERSION_1);
  }

  public static boolean isBcl2fastqVersion2(final String version) {

    // Check it is a full version name or major
    final String majorVersion =
        (version.indexOf(".") > 0
            ? findBcl2fastqMajorVersion(version) : version);

    return majorVersion.equals(VERSION_2);

  }

  //
  // Private utility methods
  //

  /**
   * Test if a string is null or empty
   * @param s string to test
   * @return true if the input string is null or empty
   */
  private static boolean isNullOrEmpty(final String s) {

    return s == null || s.isEmpty();
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

  //
  // Constructor
  //

  SampleSheetUtils() {

  }

  //
  // Internal classes
  //

  /**
   * The internal static class manage useful methods only used on version2
   * sample sheet .
   * @author Sandrine Perrin
   * @since 2.4
   */
  static final class SampleSheetVersion1Utils {

    private final static List<String> COLUMNS_HEADER = Arrays.asList(
        "\"FCID\"", "\"Lane\"", "\"SampleID\"", "\"SampleRef\"", "\"Index\"",
        "\"Description\"", "\"Control\"", "\"Recipe\"", "\"Operator\"",
        "\"SampleProject\"");

    public static String toCSV(final SampleSheet design) {

      final StringBuilder sb = new StringBuilder();

      sb.append(Joiner.on(SEP).join(COLUMNS_HEADER) + "\n");

      if (design == null) {
        return sb.toString();
      }

      for (SampleEntry s : design) {

        sb.append(s.getFlowCellId().trim().toUpperCase());
        sb.append(SEP);
        sb.append(s.getLane());
        sb.append(SEP);
        sb.append(quote(s.getSampleId().trim()));
        sb.append(SEP);
        sb.append(quote(s.getSampleRef().trim()));
        sb.append(SEP);
        sb.append(quote(s.getIndex().toUpperCase()));
        sb.append(SEP);
        sb.append(quote(s.getDescription().trim()));
        sb.append(SEP);
        sb.append(s.isControl() ? 'Y' : 'N');
        sb.append(SEP);
        sb.append(quote(s.getRecipe().trim()));
        sb.append(SEP);
        sb.append(quote(s.getOperator().trim()));
        sb.append(SEP);
        sb.append(quote(s.getSampleProject()));

        sb.append('\n');

      }

      return sb.toString();
    }

    public static void checkSampleSheet(final SampleSheet design,
        final String flowCellId) throws AozanException {

      String fcid = null;
      boolean first = true;

      for (SampleEntry sample : design) {

        // Check if all the fields are not empty
        checkFCID(sample.getFlowCellId());

        if (flowCellId != null) {

          // Check if the flow cell id is the flow cell id expected
          if (!flowCellId.trim().toUpperCase()
              .equals(sample.getFlowCellId().toUpperCase())) {
            throw new AozanException("Bad flowcell name found: "
                + sample.getFlowCellId() + " (" + flowCellId + " expected).");
          }

          // Use the case of the flowCellId parameter as case for the flow cell
          // id
          // of sample
          sample.setFlowCellId(flowCellId);
        }

        // Check if all the samples had the same flow cell id
        if (first) {
          fcid = sample.getFlowCellId();
          first = false;
        } else {

          if (!fcid.equals(sample.getFlowCellId())) {
            throw new AozanException("Two differents flow cell id found: "
                + fcid + " and " + sample.getFlowCellId() + ".");
          }
        }

        // Check the lane number
        if (sample.getLane() < 1 || sample.getLane() > 8) {
          throw new AozanException("Invalid lane number found: "
              + sample.getLane() + ".");
        }

        // Check recipe
        if (isNullOrEmpty(sample.getRecipe())) {
          throw new AozanException("Found a null or empty recipe for sample: "
              + sample.getSampleId() + ".");
        }
        checkCharset(sample.getRecipe());

        // Check operator
        if (isNullOrEmpty(sample.getOperator())) {
          throw new AozanException(
              "Found a null or empty operator for sample: "
                  + sample.getSampleId() + ".");
        }

        checkCharset(sample.getOperator());

      }
    }

    //
    // Constructor
    //

    /**
     * Private constructor
     */
    private SampleSheetVersion1Utils() {
      super();
    }

  }

  /**
   * The internal static class manage useful methods only used on version2
   * sample sheet .
   * @author Sandrine Perrin
   * @since 2.4
   */
  final static class SampleSheetVersion2Utils {

    private final static String COLUMNS_HEADER =
        "\"lane\",\"SampleID\",\"sampleref\",\"index\",\"description\","
            + "\"Sample_Project\"\n";

    /**
     * Convert sample sheet instance in string in csv format.
     * @param design the design
     * @return the string
     */
    public static String toCSV(final SampleSheet design) {

      // Cast in sample sheet version 2
      final SampleSheetVersion2 sampleSheetV2 = (SampleSheetVersion2) design;

      final StringBuilder sb = new StringBuilder();

      // Add session Header
      if (sampleSheetV2.existHeaderSession()) {
        sb.append("[Header]\n");
        sb.append(addSession(sampleSheetV2.getHearderEntries()));
        sb.append("\n");
      }

      // Add session Reads
      if (sampleSheetV2.existReadsSession()) {
        sb.append("[Reads]\n");
        sb.append(addSession(sampleSheetV2.getReadsSession()));
        sb.append("\n");
      }

      // Add session Settings
      if (sampleSheetV2.existSettingsSession()) {
        sb.append("[Settings]\n");
        sb.append(addSession(sampleSheetV2.getSettingsSession()));
        sb.append("\n");
      }

      // Add session Data
      sb.append(addSessionData(sampleSheetV2));

      return sb.toString();
    }

    /**
     * Adds the session sample sheet in string csv format, excepted session
     * data.
     * @param entries the entries on session
     * @return string csv format
     */
    private static String addSession(final Map<String, String> entries) {

      final StringBuilder sb = new StringBuilder();

      for (Map.Entry<String, String> e : entries.entrySet()) {
        sb.append(e.getKey());
        sb.append(SEP);
        sb.append(e.getValue());
        sb.append("\n");

      }

      return sb.toString();
    }

    /**
     * Adds the session data.
     * @param design the design
     * @return the string csv format
     */
    private static String addSessionData(final SampleSheet design) {

      final StringBuilder sb = new StringBuilder();
      sb.append("[Data]\n");

      sb.append(COLUMNS_HEADER);

      if (design == null) {
        return sb.toString();
      }

      for (SampleEntry s : design) {

        sb.append(s.getLane());
        sb.append(SEP);
        sb.append(quote(s.getSampleId().trim()));
        sb.append(SEP);
        sb.append(quote(s.getSampleRef().trim()));
        sb.append(SEP);
        sb.append(quote(s.getIndex().toUpperCase()));
        sb.append(SEP);
        sb.append(quote(s.getDescription().trim()));
        sb.append(SEP);
        sb.append(quote(s.getSampleProject()));

        sb.append('\n');

      }

      return sb.toString();
    }

    public static void checkSampleSheet(final SampleSheet design) {
      // Nothing to do
    }

    //
    // Constructor
    //

    /**
     * Private constructor
     */
    private SampleSheetVersion2Utils() {
      super();
    }

  }

}
