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

public class SampleEntryVersion2 extends AbstractSampleEntry {

  private int orderNumber;

  @Override
  public void setControl(final boolean control) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setRecipe(final String recipe) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setOperator(final String operator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFlowCellId(final String flowCellId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the flow cell id for the sample.
   * @return Returns the flowCellId
   */
  @Override
  public String getFlowCellId() {
    throw new UnsupportedOperationException();
  }

  /**
   * Test if the sample is a control
   * @return Returns the control
   */
  @Override
  public boolean isControl() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the recipe use to make the sample
   * @return Returns the recipe
   */
  @Override
  public String getRecipe() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the operator who has made the sample.
   * @return Returns the operator
   */
  @Override
  public String getOperator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getOrderNumber() {
    return orderNumber;
  }

  @Override
  public void setOrderNumber(int orderNumber) {
    this.orderNumber = orderNumber;
  }

  @Override
  public String getDemultiplexedFilenamePrefix(final int readNumber) {

    final StringBuilder sb = new StringBuilder();

    sb.append(getSampleId().replaceAll("_", "-"));
    sb.append('_');
    sb.append("S" + getOrderNumber());
    sb.append('_');

    sb.append(getDemultiplexedFilenameSuffix(readNumber));

    return sb.toString();
  }

  @Override
  public String getNotDemultiplexedFilenamePrefix(final int readNumber) {

    final StringBuilder sb = new StringBuilder();

    sb.append("Undetermined_S0_");

    sb.append(getDemultiplexedFilenameSuffix(readNumber));

    return sb.toString();
  }

  @Override
  public String toString() {
    return "SampleEntryVersion2 [orderNumber="
        + orderNumber + ", getLane()=" + getLane() + ", getSampleId()="
        + getSampleId() + ", getSampleRef()=" + getSampleRef()
        + ", getIndex()=" + getIndex() + ", getIndex2()=" + getIndex2()
        + ", isIndex()=" + isIndex() + ", isDualIndex()=" + isDualIndex()
        + ", getDescription()=" + getDescription() + ", getSampleProject()="
        + getSampleProject() + "]";
  }

}
