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

package fr.ens.biologie.genomique.aozan.fastqc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.biologie.genomique.aozan.Aozan2Logger;
import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.Settings;
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

  /** LOGGER. */
  private static final Logger LOGGER = Aozan2Logger.getLogger();

  private static Contaminant[] contaminants;

  public static ContaminantHit findContaminantHit(final String sequence) {

    // Modify call Aozan method
    if (contaminants == null) {
      contaminants = makeContaminantList();
    }

    ContaminantHit bestHit = null;

    for (final Contaminant contaminant : contaminants) {
      final ContaminantHit thisHit = contaminant.findMatch(sequence);

      if (thisHit == null) {
        continue; // No hit
      }

      if (bestHit == null || thisHit.length() > bestHit.length()) {
        bestHit = thisHit;
      }

    }

    if (bestHit == null) {
      return new BlastContaminantHit(sequence);
    }

    return bestHit;
  }

  public static Contaminant[] makeContaminantList() {
    final List<Contaminant> c = new ArrayList<>();

    try {

      final InputStream is;

      if (System
          .getProperty(Settings.QC_CONF_FASTQC_CONTAMINANT_FILE_KEY) != null
          && System.getProperty(Settings.QC_CONF_FASTQC_CONTAMINANT_FILE_KEY)
              .length() > 0) {
        is = new FileInputStream(
            System.getProperty(Settings.QC_CONF_FASTQC_CONTAMINANT_FILE_KEY));
      } else {
        // FastQC v0.11.2
        is = ClassLoader
            .getSystemResourceAsStream("Configuration/contaminant_list.txt");
        // FastQC v0.10
        // ClassLoader
        // .getSystemResourceAsStream("Contaminants/contaminant_list.txt");

      }

      final BufferedReader br = new BufferedReader(
          new InputStreamReader(is, Globals.DEFAULT_FILE_ENCODING));

      String line;
      while ((line = br.readLine()) != null) {

        if (line.startsWith("#")) {
          continue; // Skip comments
        }
        if (line.isEmpty()) {
          continue; // Skip blank lines
        }

        final String[] sections = line.split("\\t+");

        if (sections.length != 2) {
          LOGGER.warning("Expected 2 sections for contaminant line but got "
              + sections.length + " from " + line);
          continue;
        }
        final Contaminant con = new Contaminant(sections[0], sections[1]);
        c.add(con);
      }

      br.close();
    } catch (final IOException e) {
      throw new AozanRuntimeException(
          "In contaminant finder class, fail to reads contaminant list.");
    }

    return c.toArray(new Contaminant[c.size()]);
  }

}
