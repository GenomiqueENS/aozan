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
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;

public abstract class SampleSheetLineReader extends
    AbstractCasavaDesignTextReader {

  @Override
  public SampleSheet read(String version) throws AozanException, IOException {
    return null;
  }

  public abstract void parseLine(final SampleSheet design,
      final List<String> fields) throws AozanException;

}
