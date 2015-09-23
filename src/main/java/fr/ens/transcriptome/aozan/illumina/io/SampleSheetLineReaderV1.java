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

import java.util.List;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntryVersion1;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleV1;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;

/**
 * The Class SampleSheetLineReaderV1.
 * @author Sandrine Perrin
 * @since 2.4
 */
class SampleSheetLineReaderV1 extends SampleSheetLineReader {

  private boolean firstLine = true;

  // Required in this order columns header for version1
  private static final String[] FIELDNAMES_VERSION1 = new String[] {"FCID",
      "Lane", "SampleID", "SampleRef", "Index", "Description", "Control",
      "Recipe", "Operator", "SampleProject"};

  @Override
  public void parseLine(final SampleSheet design, final List<String> fields)
      throws AozanException {

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

    final SampleV1 sample = new SampleEntryVersion1();

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

    design.addSample(sample);
  }

}