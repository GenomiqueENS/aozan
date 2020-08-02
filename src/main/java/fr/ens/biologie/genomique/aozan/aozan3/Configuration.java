package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class define a configuration
 * @since 3.0
 * @author Laurent Jourdren
 */
public class Configuration {

  private final Map<String, String> conf = new HashMap<>();

  //
  // Getter
  //

  /**
   * Test if configuration contains a key.
   * @param key the key to test
   * @return true if the configuration contains the key
   */
  public boolean containsKey(String key) {

    requireNonNull(key);

    return this.conf.containsKey(key.trim());
  }

  /**
   * Get the value for a configuration key.
   * @param key the key. The key must exists
   * @return a String with the value
   */
  public String get(String key) {

    if (!containsKey(key)) {
      throw new NoSuchElementException(key);
    }

    return this.conf.get(key.trim());
  }

  /**
   * Get a boolean configuration key.
   * @param key the key The key must exists
   * @return a boolean value
   */
  public boolean getBoolean(String key) {

    return Boolean.parseBoolean(get(key));
  }

  /**
   * Get an int configuration key.
   * @param key the key The key must exists
   * @return a integer value
   */
  public int getInt(String key) {

    return Integer.parseInt(get(key));
  }

  /**
   * Get a double configuration key.
   * @param key the key The key must exists
   * @return a double value
   */
  public double getDouble(String key) {

    return Double.parseDouble(get(key));
  }

  /**
   * Get Path configuration key.
   * @param key the key The key must exists
   * @return a Path value
   */
  public Path getPath(String key) {

    return Paths.get(get(key));
  }

  /**
   * Get the value for a configuration key.
   * @param key the key
   * @param defaultValue value if the key does not exists
   * @return a String with the value
   */
  public String get(String key, String defaultValue) {

    requireNonNull(defaultValue);

    if (!containsKey(key)) {
      return defaultValue;
    }

    return this.conf.get(key.trim());
  }

  /**
   * Get a boolean value for a configuration key.
   * @param key the key
   * @param defaultValue value if the key does not exists
   * @return a String with the value
   */
  public boolean getBoolean(String key, boolean defaultValue) {

    return Boolean.parseBoolean(get(key, Boolean.toString(defaultValue)));
  }

  /**
   * Get an integer value for a configuration key.
   * @param key the key
   * @param defaultValue value if the key does not exists
   * @return a integer
   */
  public int getInt(String key, int defaultValue) {

    return Integer.parseInt(get(key, Integer.toString(defaultValue)));
  }

  /**
   * Get a double value for a configuration key.
   * @param key the key
   * @param defaultValue value if the key does not exists
   * @return a double
   */
  public double getDouble(String key, double defaultValue) {

    return Double.parseDouble(get(key, Double.toString(defaultValue)));
  }

  /**
   * Get a Path value for a configuration key.
   * @param key the key
   * @param defaultValue value if the key does not exists
   * @return a Path
   */
  public Path getPath(String key, Path defaultValue) {

    requireNonNull(defaultValue);

    if (!containsKey(key)) {
      return defaultValue;
    }

    return Paths.get(get(key));
  }

  //
  // Setter
  //

  /**
   * Set a value.
   * @param key the key
   * @param value the value
   */
  public void set(String key, String value) {

    requireNonNull(key);
    requireNonNull(value);

    this.conf.put(key.trim(), value);
  }

  /**
   * Set a value if key does not exists.
   * @param key the key
   * @param value the value
   */
  public void setIfNotExists(String key, String value) {

    if (this.conf.containsKey(key)) {
      return;
    }

    set(key, value);
  }

  /**
   * Set a value.
   * @param key the key
   * @param value the value
   */
  public void set(String key, boolean value) {

    requireNonNull(key);
    requireNonNull(value);

    this.conf.put(key.trim(), Boolean.toString(value));
  }

  /**
   * Set a value if key does not exists.
   * @param key the key
   * @param value the value
   */
  public void setIfNotExists(String key, boolean value) {

    if (this.conf.containsKey(key)) {
      return;
    }

    set(key, value);
  }

  /**
   * Set a value.
   * @param key the key
   * @param value the value
   */
  public void set(String key, Number value) {

    set(key, value.toString());
  }

  /**
   * Set a value if key does not exists.
   * @param key the key
   * @param value the value
   */
  public void setIfNotExists(String key, Number value) {

    if (this.conf.containsKey(key)) {
      return;
    }

    set(key, value);
  }

  /**
   * Add all the key and value of a Configuration object in this one.
   * @param conf the configuration to add
   */
  public void set(Configuration conf) {

    requireNonNull(conf);
    this.conf.putAll(conf.conf);
  }

  //
  // Other
  //

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [conf=" + conf + "]";
  }

  //
  // Constructors
  //

  /**
   * Default constructor.
   */
  public Configuration() {
  }

  /**
   * Constructor.
   * @param conf configuration to copy
   */
  public Configuration(Configuration conf) {

    requireNonNull(conf);

    set(conf);
  }

}
