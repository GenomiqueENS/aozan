package fr.ens.biologie.genomique.aozan.collectors;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class define the configuration of a collector.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class CollectorConfiguration {

  private final Map<String, String> map;

  /**
   * Get a setting value.
   * @param key the setting key
   * @return the value of the setting or null if the setting does not exists
   */
  public String get(final String key) {

    return this.map.get(key);
  }

  /**
   * Get a setting value as an integer value.
   * @param key the setting key
   * @return the integer value of the setting
   */
  public int getInt(final String key, final int defaultValue) {

    String value = get(key);

    if (value == null) {
      return defaultValue;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Get a setting value as a double value.
   * @param key the setting key
   * @return the integer value of the setting
   */
  public double getDouble(final String key, final double defaultValue) {

    String value = get(key);

    if (value == null) {
      return defaultValue;
    }

    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Get a setting value in lower case.
   * @param key the setting key
   * @return the trimmed lower case value of the setting or null if the setting
   *         does not exists
   */
  public String getTrimmedLowerCase(final String key) {

    String value = get(key);

    if (value == null) {
      return null;
    }

    return value.trim().toLowerCase();
  }

  /**
   * Get a setting value as a File object.
   * @param key the setting key
   * @return the File value of the setting or null if the setting does not
   *         exists
   */
  public File getFile(final String key) {

    String value = get(key);

    if (value == null) {
      return null;
    }

    return new File(value.trim());
  }

  /**
   * Get a setting value.
   * @param key the setting key
   * @return the boolean value of the setting
   */
  public boolean getBoolean(final String key) {

    String value = get(key);

    if (value == null) {
      return false;
    }

    return Boolean.parseBoolean(value.trim());
  }

  /**
   * Test if the configuration contains a key.
   * @param key the key to test
   * @return true if the configuration contains the key
   */
  public boolean containsKey(final String key) {

    return this.map.containsKey(key);
  }

  /**
   * Get an entry set of the setting values.
   * @return an entry set of the settings
   */
  public Set<Map.Entry<String, String>> entrySet() {

    return this.map.entrySet();
  }

  @Override
  public String toString() {
    return this.map.toString();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param map Collector configuration
   */
  public CollectorConfiguration(final Map<String, String> map) {

    if (map == null) {
      throw new NullPointerException("The map object is null");
    }

    this.map = new LinkedHashMap<>(map);
  }

  /**
   * Public constructor.
   * @param conf Collector configuration
   */
  public CollectorConfiguration(final CollectorConfiguration conf) {

    if (conf == null) {
      throw new NullPointerException("The conf object is null");
    }

    this.map = new LinkedHashMap<>(conf.map);
  }

}
