package fr.ens.biologie.genomique.aozan.collectors;

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
   * @return the value of the setting
   */
  public String get(final String key) {

    return this.map.get(key);
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
