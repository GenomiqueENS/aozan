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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.illumina.samplesheet.io;

import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet.BCL2FASTQ_DEMUX_TABLE_NAME;
import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetReaderUtils.checkFields;
import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetReaderUtils.parseLane;
import static fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetReaderUtils.trimFields;

import java.io.IOException;
import java.util.List;

import fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.TableSection;

/**
 * This class allow to easily write reader for SampleSheet objects in text
 * format.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class SampleSheetV1Parser implements SampleSheetParser {

  static final String[] FIELDNAMES =
      new String[] {"FCID", "Lane", "SampleID", "SampleRef", "Index",
          "Description", "Control", "Recipe", "Operator", "SampleProject"};

  private final SampleSheet samplesheet;
  private final TableSection tableSection;
  private boolean firstLine = true;

  /**
   * Convert a field name to internal field name.
   * @param fieldName the field name to convert
   * @return the field name to use with SampleSheet internal model
   */
  private String convertFieldName(final String fieldName) {

    if (fieldName == null) {
      return null;
    }

    if ("Lane".equals(fieldName)) {
      return Sample.LANE_FIELD_NAME;
    }

    if ("SampleID".equals(fieldName)) {
      return Sample.SAMPLE_ID_FIELD_NAME;
    }

    if ("Description".equals(fieldName)) {
      return Sample.DESCRIPTION_FIELD_NAME;
    }

    if ("SampleProject".equals(fieldName)) {
      return Sample.PROJECT_FIELD_NAME;
    }

    if ("SampleRef".equals(fieldName)) {
      return Sample.SAMPLE_REF_FIELD_NAME;
    }

    return fieldName;
  }

  @Override
  public void parseLine(final List<String> fields) throws IOException {

    trimFields(fields);
    checkFields(fields);

    if (this.firstLine) {
      this.firstLine = false;

      for (int i = 0; i < fields.size(); i++) {
        if (!FIELDNAMES[i].toLowerCase().equals(fields.get(i).toLowerCase())) {
          throw new IOException("Invalid field name: " + fields.get(i));
        }
      }

      return;
    }

    final Sample sample = this.tableSection.addSample();

    // FCID
    this.samplesheet.setFlowCellId(fields.get(0));

    // Lane
    sample.set(convertFieldName(FIELDNAMES[1]), "" + parseLane(fields.get(1)));

    // SampleId
    sample.set(convertFieldName(FIELDNAMES[2]), fields.get(2));

    // SampleRef
    sample.set(convertFieldName(FIELDNAMES[3]), fields.get(3));

    // Description
    sample.set(convertFieldName(FIELDNAMES[5]), fields.get(5));

    // Index
    final String indexes = fields.get(4);
    if (indexes.isEmpty()) {
      sample.setIndex1("");
      sample.setIndex2("");
    } else {

      final String[] indexArray = indexes.split("-");

      if (indexArray.length > 2) {
        throw new IOException("More than two indexes found: " + indexes);
      }

      sample.setIndex1(indexArray[0]);
      sample.setIndex2(indexArray.length == 1 ? "" : indexArray[1]);
    }

    // Description
    sample.set(convertFieldName(FIELDNAMES[5]), fields.get(5));

    // Control
    sample.set(convertFieldName(FIELDNAMES[6]),
        "" + parseControlField(fields.get(6)));

    // Recipe
    sample.set(convertFieldName(FIELDNAMES[7]), fields.get(7));

    // Operator
    sample.set(convertFieldName(FIELDNAMES[8]), fields.get(8));

    // SampleProject
    sample.set(convertFieldName(FIELDNAMES[9]), fields.get(9));
  }

  private static boolean parseControlField(final String value)
      throws IOException {

    if ("".equals(value)) {
      throw new IOException("Empty value in the control field");
    }

    if ("Y".equals(value) || "y".equals(value)) {
      return true;
    }

    if ("N".equals(value) || "n".equals(value)) {
      return false;
    }

    throw new IOException("Invalid value for the control field: " + value);
  }

  @Override
  public SampleSheet getSampleSheet() {

    return this.samplesheet;
  }

  //
  // Constructor
  //

  public SampleSheetV1Parser() {

    this.samplesheet = new SampleSheet();
    this.samplesheet.setVersion(1);
    this.tableSection =
        this.samplesheet.addTableSection(BCL2FASTQ_DEMUX_TABLE_NAME);
  }

}
