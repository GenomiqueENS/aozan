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

public interface SampleV1 extends Sample {

  /**
   * Get the flow cell id for the sample.
   * @return Returns the flowCellId
   */
  public String getFlowCellId();

  /**
   * Test if the sample is a control
   * @return Returns the control
   */
  public boolean isControl();

  /**
   * Get the recipe use to make the sample
   * @return Returns the recipe
   */
  public String getRecipe();

  /**
   * Get the operator who has made the sample.
   * @return Returns the operator
   */
  public String getOperator();

  /**
   * Set the flow cell id for the sample.
   * @param flowCellId The flowCellId to set
   */
  public void setFlowCellId(String flowCellId);

  /**
   * Set if the sample is a control.
   * @param control The control to set
   */
  public void setControl(boolean control);

  /**
   * Set the recipe used to make the sample
   * @param recipe The recipe to set
   */
  public void setRecipe(String recipe);

  /**
   * Set the operator who has made the sample.
   * @param operator The operator to set
   */
  public void setOperator(String operator);

}
