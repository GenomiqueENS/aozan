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
package fr.ens.biologie.genomique.aozan.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The Class StringUtils.
 * @author Sandrine Perrin
 * @since 2.4
 */
public class StringUtils {

  /**
   * Convert a stack trace of an exception into a string. TODO Remove this
   * method once Eoulsan 2.0-alpha7 will be used, and replace by
   * StringUtils.stackTraceToString()
   * @param t the throwable exception
   * @return a string with the stack trace
   */
  public static String stackTraceToString(final Throwable t) {

    if (t == null) {
      return null;
    }

    final StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));

    return sw.toString();
  }

}
