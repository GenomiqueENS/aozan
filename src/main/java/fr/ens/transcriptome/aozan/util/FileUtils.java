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

package fr.ens.transcriptome.aozan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * This class defines useful method to handle files.
 * @since 1.2
 * @author Sandrine Perrin
 */
public class FileUtils {

  /**
   * Convert a file in string list 
   * @param is input stream 
   * @return list of string from file
   * @throws IOException
   */
  public static List<String> readFileByLines(final InputStream is)
      throws IOException {

    if (is == null)
      return null;

    final List<String> result = Lists.newArrayList();

    BufferedReader reader = new BufferedReader(new InputStreamReader(is));

    String line = null;

    while ((line = reader.readLine()) != null) {
      result.add(line);
    }

    reader.close();
    return result;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private FileUtils() {
  }

}
