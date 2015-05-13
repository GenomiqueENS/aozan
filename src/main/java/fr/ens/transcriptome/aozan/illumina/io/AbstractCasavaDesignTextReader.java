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

package fr.ens.transcriptome.aozan.illumina.io;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntry;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntryVersion1;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntryVersion2;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetVersion2;

/**
 * This class allow to easily write reader for CasavaDesign in text format.
 * @since 1.1
 * @author Laurent Jourdren
 */
public abstract class AbstractCasavaDesignTextReader implements
    CasavaDesignReader {

  // Required in this order columns header for version1
  private static final String[] FIELDNAMES_VERSION1 = new String[] {"FCID",
      "Lane", "SampleID", "SampleRef", "Index", "Description", "Control",
      "Recipe", "Operator", "SampleProject"};

  // Required in this order columns header for version2
  private static final List<String> FIELDNAMES_VERSION2 = Arrays.asList("Lane",
      "Sample_ID", "Sample_Ref", "index", "Description", "Sample_Project");

  private final static List<String> SESSIONS_HEADER = Arrays.asList("[Header]",
      "[Reads]", "[Settings]", "[Data]");

  private SampleSheet design;
  private String version;
  private boolean firstLine = true;
  private int fieldsCountExpected;

  private String currentSessionName;

  private boolean firstData = true;

  private Map<String, Integer> posFields;

  /**
   * Parses the line.
   * @param fields the fields
   * @throws IOException Signals that an I/O exception has occurred.
   */

  protected void parseLine(final List<String> fields, final String version)
      throws AozanException {

    if (this.design == null) {
      this.version = version;

      if (this.version.equals(SampleSheetUtils.VERSION_2))
        this.design = new SampleSheetVersion2(version);
      else
        this.design = new SampleSheet(version);
    }

    assert (this.version.equals(version));

    if (this.design.isVersion1()) {
      parseLineVersion1(fields);

    } else {
      parseLineVersion2(fields);
    }

  }

  private void parseLineVersion1(List<String> fields) throws AozanException {

    trimAndCheckFields(fields);

    if (this.firstLine) {
      this.firstLine = false;

      for (int i = 0; i < fields.size(); i++) {
        if (!FIELDNAMES_VERSION1[i].toLowerCase().equals(
            fields.get(i).toLowerCase())) {

          throw new AozanException("Invalid field name: " + fields.get(i));
        }
      }

      return;
    }

    final SampleEntry sample = new SampleEntryVersion1();

    sample.setFlowCellId(fields.get(0));
    sample.setLane(parseLane(fields.get(1)));
    sample.setSampleId(fields.get(2));
    sample.setSampleRef(fields.get(3));
    sample.setIndex(fields.get(4));
    sample.setDescription(fields.get(5));
    sample.setControl(parseControlField(fields.get(6)));
    sample.setRecipe(fields.get(7));
    sample.setOperator(fields.get(8));
    sample.setSampleProject(fields.get(9));

    this.design.addSample(sample);
  }

  private static final boolean parseControlField(final String value)
      throws AozanException {

    if ("".equals(value)) {
      throw new AozanException("Empty value in the control field");
    }

    if ("Y".equals(value) || "y".equals(value)) {
      return true;
    }

    if ("N".equals(value) || "n".equals(value)) {
      return false;
    }

    throw new AozanException("Invalid value for the control field: " + value);
  }

  private static final void trimAndCheckFields(final List<String> fields)
      throws AozanException {

    if (fields == null) {
      throw new AozanException("The fields are null");
    }

    // Trim fields
    for (int i = 0; i < fields.size(); i++) {
      final String val = fields.get(i);
      if (val == null) {
        throw new AozanException("Found null field.");
      }
      fields.set(i, val.trim());
    }

    if (fields.size() == 10) {
      return;
    }

    if (fields.size() < 10) {
      throw new AozanException("Invalid number of field ("
          + fields.size() + "), 10 excepted.");
    }

    for (int i = 10; i < fields.size(); i++) {
      if (!"".equals(fields.get(i).trim())) {
        throw new AozanException("Invalid number of field ("
            + fields.size() + "), 10 excepted.");
      }
    }

  }

  private void parseLineVersion2(List<String> fields) throws AozanException {

    final String firstField = fields.get(0).trim();
    final SampleSheetVersion2 design2 = (SampleSheetVersion2) this.design;

    // First field empty
    if (firstField.isEmpty())
      return;

    // If first field start with '[' is an new session
    if (firstField.startsWith("[") && firstField.endsWith("]")) {

      // Check exist
      if (!SESSIONS_HEADER.contains(firstField))
        throw new AozanException(
            "Parsing sample sheet file, invalid session name " + firstField);

      // Set currentSessionName
      this.currentSessionName = firstField;

    } else {

      if (this.currentSessionName.equals("[Data]")) {

        if (firstData) {

          this.firstData = false;
          this.fieldsCountExpected = fields.size();
          this.posFields = extractFieldNames(fields);

        } else {
          assert (fields.size() == this.fieldsCountExpected);

          final SampleEntry sample = new SampleEntryVersion2();
          int i = 0;

          String key = FIELDNAMES_VERSION2.get(i++);
          String value = fields.get(this.posFields.get(key));
          sample.setLane(parseLane(value));

          key = FIELDNAMES_VERSION2.get(i++);
          value = fields.get(this.posFields.get(key));
          sample.setSampleId(value);

          key = FIELDNAMES_VERSION2.get(i++);
          value = fields.get(this.posFields.get(key));
          sample.setSampleRef(value);

          key = FIELDNAMES_VERSION2.get(i++);
          value = fields.get(this.posFields.get(key));
          sample.setIndex(value);

          key = FIELDNAMES_VERSION2.get(i++);
          value = fields.get(this.posFields.get(key));
          sample.setDescription(value);

          key = FIELDNAMES_VERSION2.get(i++);
          value = fields.get(this.posFields.get(key));
          sample.setSampleProject(value);

          // Add new sample
          this.design.addSample(sample);
        }

      } else {
        design2.addSessionEntry(fields, this.currentSessionName);
      }
    }

  }

  private Map<String, Integer> extractFieldNames(List<String> fields)
      throws AozanException {

    final Map<String, Integer> pos = new HashMap<>(fields.size());

    // Check all required field exists
    for (String name : FIELDNAMES_VERSION2) {
      if (!fields.contains(name)) {
        throw new AozanException(
            "Parsing Sample sheet file: missing required field for version2 "
                + name);
      }
    }

    // Locate fields
    for (int i = 0; i < fields.size(); i++) {
      pos.put(fields.get(i), i);
    }

    return pos;
  }

  private static final int parseLane(final String s) throws AozanException {

    if (s == null) {
      return 0;
    }

    final double d;
    try {
      d = Double.parseDouble(s);

    } catch (NumberFormatException e) {
      throw new AozanException("Invalid lane number: " + s);
    }

    final int result = (int) d;

    if (d - result > 0) {
      throw new AozanException("Invalid lane number: " + s);
    }

    return result;
  }

  protected SampleSheet getDesign() {

    return (SampleSheet) this.design;
  }

}
