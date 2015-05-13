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

public class SampleEntryVersion1 extends AbstractSampleEntry {

  private String flowCellId;
  private String recipe;
  private String operator;
  private boolean control;

  /**
   * Get the flow cell id for the sample.
   * @return Returns the flowCellId
   */
  @Override
  public String getFlowCellId() {
    return this.flowCellId;
  }

  /**
   * Test if the sample is a control
   * @return Returns the control
   */
  @Override
  public boolean isControl() {
    return this.control;
  }

  /**
   * Get the recipe use to make the sample
   * @return Returns the recipe
   */
  @Override
  public String getRecipe() {
    return this.recipe;
  }

  /**
   * Get the operator who has made the sample.
   * @return Returns the operator
   */
  @Override
  public String getOperator() {
    return this.operator;
  }

  /**
   * Set if the sample is a control.
   * @param control The control to set
   */
  @Override
  public void setControl(final boolean control) {
    this.control = control;
  }

  /**
   * Set the recipe used to make the sample
   * @param recipe The recipe to set
   */
  @Override
  public void setRecipe(final String recipe) {
    this.recipe = recipe;
  }

  /**
   * Set the operator who has made the sample.
   * @param operator The operator to set
   */
  @Override
  public void setOperator(final String operator) {
    this.operator = operator;
  }

  @Override
  public void setFlowCellId(final String flowCellId) {
    this.flowCellId = flowCellId;
  }

  @Override
  public int getOrderNumber() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setOrderNumber(int orderNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "SampleEntryVersion1 "
        + super.toString() + "[flowCellId=" + flowCellId + ", recipe=" + recipe
        + ", operator=" + operator + ", control=" + control + "]";
  }

  @Override
  public String getDemultiplexedFilenamePrefix(final int readNumber) {

    final StringBuilder sb = new StringBuilder();

    sb.append(getSampleId());
    sb.append('_');
    sb.append(getIndex() == null || "".equals(getIndex().trim())
        ? "NoIndex" : getIndex());
    sb.append('_');

    sb.append(getDemultiplexedFilenameSuffix(readNumber));

    return sb.toString();
  }

  @Override
  public String getNotDemultiplexedFilenamePrefix(final int readNumber) {

    final StringBuilder sb = new StringBuilder();

    sb.append("lane");
    sb.append(getLane());
    sb.append('_');
    sb.append("Undetermined");
    sb.append('_');

    sb.append(getDemultiplexedFilenameSuffix(readNumber));

    return sb.toString();
  }

}
