/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */
package fr.ens.transcriptome.aozan.illumina.samplesheet;

import static fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils.SEP;
import static fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils.quote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.illumina.sampleentry.Sample;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleV2;
import fr.ens.transcriptome.aozan.io.FastqSample;

public class SampleSheetVersion2 extends SampleSheet {

  // Save order sample entry from sample sheet file
  private Map<String, Integer> ordonnancementColumns;

  private final Map<String, String> headerSession;
  private final Map<String, String> readsSession;
  private final Map<String, String> settingsSession;
  private ArrayList<String> dataSessionHeaderColumns;

  private int lastIndice = 0;

  private boolean columnLaneExist;

  private boolean dualIndexesSample;

  public void addSample(final Sample sample, boolean isColumnLaneExist) {

    super.addSample(sample);
    setColumnLaneExist(isColumnLaneExist);

    // Save order

    final String key = buildKey(sample.getSampleId(), sample.getLane());
    final int indice = findPositionInSamplesheetFile(sample);

    ordonnancementColumns.put(key, indice);
  }

  private int findPositionInSamplesheetFile(final Sample sample) {

    if (sample.getLane() == 1 || sample.getLane() == 0) {
      return ++lastIndice;
    }

    // Return order found for sample in lane 1
    return this.ordonnancementColumns.get(buildKey(sample.getSampleId(), 1));

  }

  public void addSessionEntry(final List<String> fields,
      final String sessionName) throws AozanException {

    switch (sessionName) {

    case "[Header]":
      if (fields.size() == 1) {
        this.headerSession.put(fields.get(0).trim(), "");
      } else {
        this.headerSession.put(fields.get(0).trim(), fields.get(1).trim());
      }
      break;

    case "[Reads]":
      this.readsSession.put(fields.get(0).trim(), "");
      break;

    case "[Settings]":
      if (fields.size() == 1) {
        this.settingsSession.put(fields.get(0).trim(), "");
      } else {
        this.settingsSession.put(fields.get(0).trim(), fields.get(1).trim());
      }
      break;

    default:
      throw new AozanException(
          "Parsing sample sheet file, invalid session name " + sessionName);
    }

  }

  public Map<String, String> getHearderEntries() {
    return headerSession;
  }

  public int extractOrderNumberSample(final FastqSample fastqSample) {

    if (this.ordonnancementColumns == null
        || this.ordonnancementColumns.isEmpty())
      throw new AozanRuntimeException(
          "list sample order in sample sheet file no setting");

    // Case undetermined always return 0
    if (fastqSample.isIndeterminedIndices()) {
      return 0;
    }

    String key;

    // Case no lane column in sample sheet file
    if (useLaneNumberInKey()) {
      // Lane number is zero
      key = buildKey(fastqSample.getSampleName(), 0);
    } else {

      key = buildKey(fastqSample.getSampleName(), fastqSample.getLane());
    }

    return this.ordonnancementColumns.get(key);
  }

  private boolean useLaneNumberInKey() {

    final String first = this.ordonnancementColumns.keySet().iterator().next();

    return first.endsWith("0");
  }

  private String buildKey(String sampleName, int laneNumber) {

    return String.format("%s_%s", sampleName, laneNumber);
  }

  public String createHeaderColumnsInStringForCSV() {
    final String start = "\"";
    final String end = "\"\n";

    final StringBuilder sb = new StringBuilder();

    sb.append(start);
    sb.append(Joiner.on("\",\"").join(this.dataSessionHeaderColumns));
    sb.append(end);

    return sb.toString();
  }

  // private final static String COLUMNS_HEADER =
  // "\"SampleID\",\"sampleref\",\"index\",\"index2\",\"description\","
  // + "\"Sample_Project\"\n";
  //
  // private final static String COLUMNS_HEADER_WITH_LANE =
  // "\"lane\",\"SampleID\",\"sampleref\",\"index\",\"index2\",\"description\","
  // + "\"Sample_Project\"\n";

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

    sb.append(addSessionData(sampleSheetV2));

    return sb.toString();
  }

  private static String addSessionData(SampleSheetVersion2 design) {

    final StringBuilder sb = new StringBuilder();
    sb.append("[Data]\n");

    if (design == null) {
      return sb.toString();
    }

    // Create header columns
    sb.append(design.createHeaderColumnsInStringForCSV());

    for (Sample e : design) {

      final SampleV2 s = (SampleV2) e;

      sb.append(s.getLane());
      sb.append(SEP);
      sb.append(quote(s.getSampleId().trim()));
      sb.append(SEP);
      sb.append(quote(s.getSampleRef().trim()));
      sb.append(SEP);
      sb.append(quote(s.getIndex().toUpperCase()));
      sb.append(SEP);
      sb.append(quote(s.getIndex2().toUpperCase()));
      sb.append(SEP);
      sb.append(quote(s.getDescription().trim()));
      sb.append(SEP);
      sb.append(quote(s.getSampleProject()));

      sb.append('\n');

    }

    return sb.toString();
  }

  /**
   * Adds the session sample sheet in string csv format, excepted session data.
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

  public static void checkSampleSheet(final SampleSheet design) {
    // Nothing to do
  }

  //
  // Getters & setters
  //

  /**
   * @return the readsSession
   */
  public Map<String, String> getReadsSession() {
    return readsSession;
  }

  /**
   * @return the settingsSession
   */
  public Map<String, String> getSettingsSession() {
    return settingsSession;
  }

  /**
   * Exist header session.
   * @return true, if successful
   */
  public boolean existHeaderSession() {
    return !headerSession.isEmpty();
  }

  /**
   * Exist reads session.
   * @return true, if successful
   */
  public boolean existReadsSession() {
    return !readsSession.isEmpty();
  }

  /**
   * Exist settings session.
   * @return true, if successful
   */
  public boolean existSettingsSession() {
    return !settingsSession.isEmpty();
  }

  /**
   * Checks if is column header lane exist.
   * @return true, if is column header lane exist
   */
  public boolean isColumnHeaderLaneExist() {
    return this.columnLaneExist;
  }

  /**
   * Sets the column lane exist.
   * @param isColumnLaneExist the new column lane exist
   */
  private void setColumnLaneExist(boolean isColumnLaneExist) {
    this.columnLaneExist = isColumnLaneExist;
  }

  /**
   * Sets the header columns.
   * @param fields the new header columns
   */
  public void setHeaderColumns(final List<String> fields) {
    this.dataSessionHeaderColumns.addAll(fields);
  }

  /**
   * Gets the header columns.
   * @return the header columns
   */
  public List<String> getHeaderColumns() {

    Preconditions.checkArgument(!this.dataSessionHeaderColumns.isEmpty(),
        "No header columns found in sample sheet.");

    return Collections.unmodifiableList(this.dataSessionHeaderColumns);
  }

  public void setDualIndexes() {
    this.dualIndexesSample = true;
  }

  public boolean isDualIndexes() {
    return this.dualIndexesSample;
  }

  //
  // Constructor
  //

  @Override
  public String toString() {
    return "SampleSheetVersion2 [headerSession="
        + headerSession
        + ", readsSession="
        + readsSession
        + ", settingsSession="
        + settingsSession
        + ", lastIndice="
        + lastIndice
        + ", columnLaneexist="
        + isColumnHeaderLaneExist()
        + ", list=\n\t"
        + Joiner.on("\n\t").withKeyValueSeparator("\t")
            .join(ordonnancementColumns) + "]";
  }

  /**
   * Public constructor a new sample sheet version2.
   * @param sampleSheetVersion the sample sheet version
   */
  public SampleSheetVersion2(String sampleSheetVersion) {
    super(sampleSheetVersion);

    this.headerSession = new HashMap<>();
    this.settingsSession = new HashMap<>();

    this.readsSession = new HashMap<>();
    this.dataSessionHeaderColumns = new ArrayList<>();

    this.ordonnancementColumns = new HashMap<>();
    this.dualIndexesSample = false;
  }

}
