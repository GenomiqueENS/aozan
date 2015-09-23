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

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.aozan.illumina.sampleentry.Sample;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleV1;

/**
 * The Class SampleSheetVersion1.
 * @author Sandrine Perrin
 * @since 2.4
 */
public class SampleSheetVersion1 extends SampleSheet {

  /** The Constant COLUMNS_HEADER. */
  private final static List<String> COLUMNS_HEADER = Arrays.asList("\"FCID\"",
      "\"Lane\"", "\"SampleID\"", "\"SampleRef\"", "\"Index\"",
      "\"Description\"", "\"Control\"", "\"Recipe\"", "\"Operator\"",
      "\"SampleProject\"");

  @Override
  public String toCSV() {

    final StringBuilder sb = new StringBuilder();

    sb.append(Joiner.on(SEP).join(COLUMNS_HEADER) + "\n");

    for (Sample e : this) {

      final SampleV1 s = (SampleV1) e;

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

  //
  // Public constructor
  //
  /**
   * Instantiates a new sample sheet version1.
   * @param sampleSheetVersion the sample sheet version
   */
  public SampleSheetVersion1(final String sampleSheetVersion) {
    super(sampleSheetVersion);
  }
}
