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

package fr.ens.transcriptome.aozan.fastqc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import uk.ac.babraham.FastQC.Sequence.Contaminant.Contaminant;
import uk.ac.babraham.FastQC.Sequence.Contaminant.ContaminantHit;

/**
 * Source FastQC version 0.10.0, not modify. The class version 0.10.1 doesn't
 * provide access to files in fastqc jar. Use old version. Call the method
 * instead of the true after modification of bytecode. Copyright Copyright
 * 2010-11 Simon Andrews
 * @since 1.1
 */
public class ContaminantFinder {

  /** LOGGER */
  private static final Logger LOGGER = Common.getLogger();

  private static Contaminant[] contaminants;

  public static ContaminantHit findContaminantHit_Aozan(String sequence) {

    // Modify call Aozan method
    if (contaminants == null) {
      contaminants = makeContaminantList();
    }

    ContaminantHit bestHit = null;
    OverrepresentedSequencesBlast blastInstance =
        new OverrepresentedSequencesBlast();

    for (int c = 0; c < contaminants.length; c++) {
      ContaminantHit thisHit = contaminants[c].findMatch(sequence);

      // System.out.println("Best hit from "+c+" is "+thisHit);

      if (thisHit == null)
        continue; // No hit

      if (bestHit == null || thisHit.length() > bestHit.length())
        bestHit = thisHit;

    }

    if (bestHit == null) {

      ContaminantHit contaminantBlast =
          blastInstance.searchSequenceInBlast(sequence);

      // Catch exception
      if (OverrepresentedSequencesBlast.throwException() != null)

        LOGGER.warning("Error during find contaminant with blast : "
            + StringUtils.join(OverrepresentedSequencesBlast.throwException()
                .getStackTrace(), "\n\t"));

      if (contaminantBlast != null)
        bestHit = contaminantBlast;

    }

    return bestHit;

  }

  public static Contaminant[] makeContaminantList() {
    Vector<Contaminant> c = new Vector<Contaminant>();

    try {

      final InputStream is;

      if (System.getProperty("fastqc.contaminant_file") != null
          && System.getProperty("fastqc.contaminant_file").length() > 0) {
        is = new FileInputStream(System.getProperty("fastqc.contaminant_file"));
      } else {
        is =
            ClassLoader
                .getSystemResourceAsStream("Contaminants/contaminant_list.txt");
      }

      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      String line;
      while ((line = br.readLine()) != null) {

        if (line.startsWith("#"))
          continue; // Skip comments
        if (line.trim().length() == 0)
          continue; // Skip blank lines

        String[] sections = line.split("\\t+");
        if (sections.length != 2) {
          System.err
              .println("Expected 2 sections for contaminant line but got "
                  + sections.length + " from " + line);
          continue;
        }
        Contaminant con = new Contaminant(sections[0], sections[1]);
        c.add(con);
      }

      br.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return c.toArray(new Contaminant[0]);
  }
}
