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
package fr.ens.transcriptome.aozan.illumina.sampleentry;

import java.util.Map;
import java.util.Set;

public interface SampleV2 extends Sample {

  /**
   * Gets the order number from the sample sheet.
   * @return the order number
   */
  public int getOrderNumber();

  /**
   * Sets the order number from the sample sheet.
   * @param orderNumber the new order number
   */
  public void setOrderNumber(int orderNumber);

  /**
   * Sets the optional columns from the sample sheet.
   * @param key the key
   * @param value the value
   */
  void setAdditionalColumns(String key, String value);

  /**
   * Gets the additional columns.
   * @return the additional columns
   */
  Map<String, String> getAdditionalColumns();

  /**
   * Gets the additional header columns.
   * @return the additional header columns
   */
  Set<String> getAdditionalHeaderColumns();

  /**
   * Checks if is lane setting.
   * @return true, if is lane setting
   */
  public boolean isLaneSetting();

}
