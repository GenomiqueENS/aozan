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
import java.util.List;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
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

  private SampleSheet design;
  private SampleSheetLineReader reader;

  private boolean isCompatibleForQCReport = true;
  private int laneCount;
  private String version;

  /**
   * Parses the line.
   * @param fields the fields
   * @throws IOException Signals that an I/O exception has occurred.
   */

  protected void parseLine(final List<String> fields, final String version)
      throws AozanException {

    if (this.design == null) {
      this.version = version;

      if (this.version.equals(SampleSheetUtils.VERSION_2)) {
        this.design = new SampleSheetVersion2(version);
        this.reader =
            new SampleSheetLineReaderV2(getLaneCount(),
                isCompatibleForQCReport());
      } else {
        this.design = new SampleSheet(version);
        this.reader = new SampleSheetLineReaderV1();
      }
    }

    assert (this.version.equals(version));

    this.reader.parseLine(this.design, fields);

  }

  protected static final boolean parseControlField(final String value)
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

  protected static final void trimAndCheckFields(final List<String> fields)
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

  protected static final int parseLane(final String s) throws AozanException {

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

  //
  // Getters & setters
  //

  protected SampleSheet getDesign() {

    return (SampleSheet) this.design;
  }

  protected boolean isCompatibleForQCReport() {
    return this.isCompatibleForQCReport;
  }

  protected int getLaneCount() {

    return laneCount;
  }

  public void setCompatibleForQCReport(boolean isCompatibleForQCReport) {
    this.isCompatibleForQCReport = isCompatibleForQCReport;
  }

  public void setLaneCount(final int laneCount) {
    this.laneCount = laneCount;
  }

}
