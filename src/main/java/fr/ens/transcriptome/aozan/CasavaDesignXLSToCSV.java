/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.aozan;

import java.io.File;
import java.io.IOException;

import fr.ens.transcriptome.aozan.io.CasavaDesignXLSReader;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.CasavaDesignUtil;
import fr.ens.transcriptome.eoulsan.illumina.io.CasavaDesignCSVWriter;
import fr.ens.transcriptome.eoulsan.illumina.io.CasavaDesignReader;
import fr.ens.transcriptome.eoulsan.illumina.io.CasavaDesignWriter;

public class CasavaDesignXLSToCSV {

  public static final void convertCasavaDesignXLSToCSV(final String inputPath,
      final String outputPath) throws EoulsanException {

    final File fileIn = new File(inputPath);
    final File fileOut = new File(outputPath);

    final CasavaDesign design;
    try {
      final CasavaDesignReader reader = new CasavaDesignXLSReader(fileIn);
      design = reader.read();
    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    }

    CasavaDesignUtil.checkCasavaDesign(design);

    try {
      final CasavaDesignWriter writer = new CasavaDesignCSVWriter(fileOut);
      writer.writer(design);

    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    }

  }

}
