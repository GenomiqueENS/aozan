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
 *      http://tools.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.util;

/**
 * This interface define an interval.
 * @since 0.8
 * @author Laurent Jourdren
 */
public interface Interval {

  /**
   * Test if a number is in an interval.
   * @param value value to test
   * @return true if the number is in the interval
   */
  boolean isInInterval(Number value);

}
