package fr.ens.transcriptome.aozan.illumina.sampleentry;

public interface Sample {

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
   * Gets the index2, in case on dual indexes.
   * @return the index2
   */
  public String getIndex2();

  /**
   * Test if the sample is indexed.
   * @return true if the sample is indexed
   */
  public boolean isIndex();

  /**
   * Checks if is dual index.
   * @return true, if is dual index
   */
  public boolean isDualIndex();

  /**
   * Get the description of the sample.
   * @return Returns the description
   */
  public String getDescription();

  /**
   * Get the name of the project for the sample
   * @return Returns the sampleProject
   */
  public String getSampleProject();

  /**
   * Gets the demultiplexed filename prefix.
   * @param readNumber the read number
   * @return the demultiplexed filename prefix
   */
  public String getDemultiplexedFilenamePrefix(final int readNumber);

  /**
   * Gets the not demultiplexed filename prefix.
   * @param readNumber the read number
   * @return the not demultiplexed filename prefix
   */
  public String getNotDemultiplexedFilenamePrefix(final int readNumber);

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
   * Set the index sequence for the sample
   * @param index The index to set
   */
  public void setIndex2(String index);

  /**
   * @param description The description to set
   */
  public void setDescription(String description);

  /**
   * Set the name of the project for the sample.
   * @param sampleProject The sampleProject to set
   */
  public void setSampleProject(String sampleProject);
}
