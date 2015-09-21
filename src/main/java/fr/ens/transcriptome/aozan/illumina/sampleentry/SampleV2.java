package fr.ens.transcriptome.aozan.illumina.sampleentry;

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
  void setOptionalColumns(String key, String value);

}
