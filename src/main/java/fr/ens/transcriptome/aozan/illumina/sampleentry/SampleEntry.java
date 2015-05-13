/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */
package fr.ens.transcriptome.aozan.illumina.sampleentry;

public interface SampleEntry {

  /**
   * Get the flow cell id for the sample.
   * @return Returns the flowCellId
   */
  public String getFlowCellId();

  /**
   * Get the lane for the sample.
   * @return Returns the lane
   */
  public int getLane();

  /**
   * Get the sample id.
   * @return Returns the sampleId
   */
  public String getSampleId();

  /**
   * Get the genome reference for the sample.
   * @return Returns the sampleRef
   */
  public String getSampleRef();

  /**
   * Get the index sequence for the sample.
   * @return Returns the index
   */
  public String getIndex();

  /**
   * Test if the sample is indexed.
   * @return true if the sample is indexed
   */
  public boolean isIndex();

  /**
   * Get the description of the sample.
   * @return Returns the description
   */
  public String getDescription();

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
   * Get the name of the project for the sample
   * @return Returns the sampleProject
   */
  public String getSampleProject();

  /**
   * Set the flow cell id for the sample.
   * @param flowCellId The flowCellId to set
   */
  public void setFlowCellId(String flowCellId);

  /**
   * Set the lane of the sample
   * @param lane The lane to set
   */
  public void setLane(int lane);

  /**
   * Set the sample id for the sample.
   * @param sampleId The sampleId to set
   */
  public void setSampleId(String sampleId);

  /**
   * Set the genome reference for the sample.
   * @param sampleRef The sampleRef to set
   */
  public void setSampleRef(String sampleRef);

  /**
   * Set the index sequence for the sample
   * @param index The index to set
   */
  public void setIndex(String index);

  /**
   * @param description The description to set
   */
  public void setDescription(String description);

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

  /**
   * Gets the order number from samplesheet.
   * @return the order number
   */
  public int getOrderNumber();

  /**
   * Sets the order number from samplesheet.
   * @param orderNumber the new order number
   */
  public void setOrderNumber(int orderNumber);

  /**
   * Set the name of the project for the sample.
   * @param sampleProject The sampleProject to set
   */
  public void setSampleProject(String sampleProject);

  public String getDemultiplexedFilenamePrefix(int readNumber);

  public String getNotDemultiplexedFilenamePrefix(int readNumber);

  public String toString();

}
