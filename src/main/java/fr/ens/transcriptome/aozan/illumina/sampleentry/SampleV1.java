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
