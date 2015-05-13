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

public abstract class AbstractSampleEntry implements SampleEntry {

  // Required fields for SampleEntry
  private int lane;
  private String sampleId;
  private String sampleRef;
  private String index;
  private String description;
  private String sampleProject;

  //
  // Getters
  //

  /**
   * Get the lane for the sample.
   * @return Returns the lane
   */
  @Override
  public int getLane() {
    return this.lane;
  }

  /**
   * Get the sample id.
   * @return Returns the sampleId
   */
  @Override
  public String getSampleId() {
    return this.sampleId;
  }

  /**
   * Get the genome reference for the sample.
   * @return Returns the sampleRef
   */
  @Override
  public String getSampleRef() {
    return this.sampleRef;
  }

  /**
   * Get the index sequence for the sample.
   * @return Returns the index
   */
  @Override
  public String getIndex() {
    return this.index;
  }

  /**
   * Test if the sample is indexed.
   * @return true if the sample is indexed
   */
  @Override
  public boolean isIndex() {

    return this.index != null && !"".equals(this.index.trim());
  }

  /**
   * Get the description of the sample.
   * @return Returns the description
   */
  @Override
  public String getDescription() {
    return this.description;
  }

  /**
   * Get the name of the project for the sample
   * @return Returns the sampleProject
   */
  @Override
  public String getSampleProject() {
    return this.sampleProject;
  }

  //
  // Setters
  //

  @Override
  public void setLane(final int lane) {
    this.lane = lane;
  }

  /**
   * Set the sample id for the sample.
   * @param sampleId The sampleId to set
   */
  @Override
  public void setSampleId(final String sampleId) {
    this.sampleId = sampleId;
  }

  /**
   * Set the genome reference for the sample.
   * @param sampleRef The sampleRef to set
   */
  @Override
  public void setSampleRef(final String sampleRef) {
    this.sampleRef = sampleRef;
  }

  /**
   * Set the index sequence for the sample
   * @param index The index to set
   */
  @Override
  public void setIndex(final String index) {
    this.index = index;
  }

  /**
   * @param description The description to set
   */
  @Override
  public void setDescription(final String description) {
    this.description = description;
  }

  /**
   * Set the name of the project for the sample.
   * @param sampleProject The sampleProject to set
   */
  @Override
  public void setSampleProject(final String sampleProject) {
    this.sampleProject = sampleProject;
  }

  //
  // Abstracts methods
  //

  String getDemultiplexedFilenameSuffix(final int readNumber) {
    final StringBuilder sb = new StringBuilder();

    sb.append("L00");
    sb.append(getLane());
    sb.append('_');
    sb.append("R");
    sb.append(readNumber);
    sb.append('_');

    return sb.toString();
  }

  @Override
  abstract public String getDemultiplexedFilenamePrefix(final int readNumber);

  @Override
  abstract public String getNotDemultiplexedFilenamePrefix(final int readNumber);

  @Override
  public String toString() {
    return "AbstractSampleEntry [lane="
        + lane + ", sampleId=" + sampleId + ", sampleRef=" + sampleRef
        + ", index=" + index + ", description=" + description
        + ", sampleProject=" + sampleProject + "]";
  }

}
