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
