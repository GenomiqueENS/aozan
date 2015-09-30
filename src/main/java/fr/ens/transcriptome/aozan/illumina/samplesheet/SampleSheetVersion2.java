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

import static fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils.SEP;
import static fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils.quote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.illumina.sampleentry.Sample;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleV2;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * The Class SampleSheetVersion2.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class SampleSheetVersion2 extends SampleSheet {

  /** Header columns required for Aozan QC report */
  private final static String COLUMNS_HEADER_FOR_AOZAN =
      "\"SampleID\",\"sampleref\",\"index\",\"description\","
          + "\"Sample_Project\"";

  // Save order sample entry from sample sheet file
  private Map<String, Integer> ordonnancementColumns;

  private final Map<String, String> headerSession;
  private final Multimap<String, String> readsSession;
  private final Map<String, String> settingsSession;
  private ArrayList<String> dataSessionHeaderColumns;

  private int lastIndice = 0;

  private boolean columnLaneExist;

  private boolean dualIndexesSample;

  /**
   * Adds the sample.
   * @param sample the sample
   * @param isColumnLaneExist the is column lane exist
   */
  public void addSample(final Sample sample, boolean isColumnLaneExist) {

    super.addSample(sample);
    setColumnLaneExist(isColumnLaneExist);

    // Save order

    final String key = buildKey(sample.getSampleId(), sample.getLane());
    final int indice = findPositionInSamplesheetFile(sample);

    ordonnancementColumns.put(key, indice);
  }

  /**
   * Adds the session entry.
   * @param fields the fields
   * @param sessionName the session name
   * @throws AozanException the aozan exception
   */
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

  /**
   * Extract order number sample.
   * @param fastqSample the fastq sample
   * @return the int
   */
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

  /**
   * Creates the header columns in string for csv.
   * @param s the s
   * @return the string
   */
  public String createHeaderColumnsInStringForCSV(final SampleV2 s) {

    final StringBuilder sb = new StringBuilder();

    sb.append(COLUMNS_HEADER_FOR_AOZAN);

    if (s.isLaneSetting()) {
      sb.append(SEP);
      sb.append(quote("lane"));
    }

    if (isDualIndexes()) {
      sb.append(SEP);
      sb.append(quote("index2"));
    }

    // Add additional columns
    for (String h : s.getAdditionalHeaderColumns()) {
      sb.append(SEP);
      sb.append(quote(h));
    }
    sb.append("\n");

    return sb.toString();
  }

  /**
   * Convert sample sheet instance in string in csv format.
   * @return the string
   */
  public String toCSV() {

    // Cast in sample sheet version 2

    final StringBuilder sb = new StringBuilder();

    // Add session Header
    if (existHeaderSession()) {
      sb.append("[Header]\n");
      sb.append(addSession(getHearderEntries()));
      sb.append("\n");
    }

    // Add session Reads
    if (existReadsSession()) {
      sb.append("[Reads]\n");
      sb.append(readsSessionToString());
      sb.append("\n");
    }

    // Add session Settings
    if (existSettingsSession()) {
      sb.append("[Settings]\n");
      sb.append(addSession(getSettingsSession()));
      sb.append("\n");
    }

    sb.append(addSessionData());

    return sb.toString();
  }

  //
  // Private methods
  //

  /**
   * Use lane number in key.
   * @return true, if successful
   */
  private boolean useLaneNumberInKey() {

    final String first = this.ordonnancementColumns.keySet().iterator().next();

    return first.endsWith("0");
  }

  private String buildKey(String sampleName, int laneNumber) {

    return String.format("%s_%s", sampleName, laneNumber);
  }

  private int findPositionInSamplesheetFile(final Sample sample) {

    if (sample.getLane() == 1 || sample.getLane() == 0) {
      return ++lastIndice;
    }

    // Return order found for sample in lane 1
    return this.ordonnancementColumns.get(buildKey(sample.getSampleId(), 1));

  }

  /**
   * Adds the session data.
   * @return the string
   */
  private String addSessionData() {

    final StringBuilder sb = new StringBuilder();
    sb.append("[Data]\n");

    boolean first = true;

    for (Sample e : this) {

      final SampleV2 s = (SampleV2) e;

      if (first) {
        // Create header columns
        sb.append(createHeaderColumnsInStringForCSV(s));
        first = false;
      }

      sb.append(quote(s.getSampleId().trim()));
      sb.append(SEP);
      sb.append(quote(s.getSampleRef().trim()));
      sb.append(SEP);
      sb.append(quote(s.getIndex().toUpperCase()));
      sb.append(SEP);
      sb.append(quote(s.getDescription().trim()));
      sb.append(SEP);
      sb.append(quote(s.getSampleProject()));

      if (s.isLaneSetting()) {
        sb.append(SEP);
        sb.append(s.getLane());
      }

      if (isDualIndexes()) {
        sb.append(SEP);
        sb.append(quote(s.getIndex2().toUpperCase()));
      }

      // Add additional columns
      for (Map.Entry<String, String> col : s.getAdditionalColumns().entrySet()) {
        sb.append(SEP);
        sb.append(quote(col.getValue()));
      }

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

  /**
   * Check sample sheet.
   * @param design the design
   */
  public static void checkSampleSheet(final SampleSheet design) {
    // TODO
    // Nothing to do
  }

  /**
   * Reads session to string.
   * @return the string
   */
  public String readsSessionToString() {
    final StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, Collection<String>> e : this.readsSession.asMap()
        .entrySet()) {

      for (String s : e.getValue()) {

        sb.append(quote(e.getKey()));
        sb.append(SEP);
        sb.append(quote(s));
      }
      sb.append("\n");
    }

    return sb.toString();

  }

  //
  // Getters & setters
  //

  /**
   * Gets the reads session.
   * @return the readsSession
   */
  public Multimap<String, String> getReadsSession() {
    return readsSession;
  }

  /**
   * Gets the settings session.
   * @return the settingsSession
   */
  public Map<String, String> getSettingsSession() {
    return settingsSession;
  }

  /**
   * Gets the hearder entries.
   * @return the hearder entries
   */
  public Map<String, String> getHearderEntries() {
    return headerSession;
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

    this.readsSession = ArrayListMultimap.create();
    this.dataSessionHeaderColumns = new ArrayList<>();

    this.ordonnancementColumns = new HashMap<>();
    this.dualIndexesSample = false;
  }
}
