package fr.ens.biologie.genomique.aozan.illumina.samplesheet;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

/**
 * This class define a property section of a samplesheet.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class PropertySection {

  private final Multimap<String, String> properties =
      LinkedListMultimap.create();

  /**
   * Test if the property section contains a key.
   * @param key the key for the metadata
   * @return true if the key exists
   */
  public boolean containsKey(String key) {

    return this.containsKey(key);
  }

  /**
   * Get property value.
   * @param key the key for the metadata
   * @return the value of the metadata
   */
  public String get(String key) {

    return get(key, null);
  }

  /**
   * Get property value.
   * @param key the key for the metadata
   * @param defaultValue default value
   * @return the value of the metadata
   */
  public String get(String key, String defaultValue) {

    requireNonNull(key);

    Collection<String> result = this.properties.get(key.trim());

    if (result == null || result.isEmpty()) {
      return defaultValue;
    }

    return result.iterator().next();
  }

  /**
   * Get an integer property value.
   * @param key the key for the metadata
   * @param defaultValue default value
   * @return the value of the metadata
   */
  public int getInt(String key, int defaultValue) {

    String value = get(key);

    return value == null ? defaultValue : Integer.parseInt(value);
  }

  /**
   * Get an integer property value.
   * @param key the key for the metadata
   * @return the value of the metadata
   */
  public int getInt(String key) {

    String value = get(key);

    Objects.requireNonNull(value);

    return Integer.parseInt(value);
  }

  /**
   * Set a property.
   * @param key key of the property
   * @param value value of the property
   */
  public void set(String key, String value) {

    requireNonNull(key);
    requireNonNull(value);

    String trimmedKey = key.trim();

    if (trimmedKey.isEmpty()) {
      return;
    }

    this.properties.put(trimmedKey, value.trim());
  }

  /**
   * Remove a property.
   * @param key name of the property
   * @return the value of the removed property
   */
  public void remove(String key) {

    requireNonNull(key);

    if (this.properties.containsKey(key)) {
      this.properties.remove(key, get(key.trim()));
    }
  }

  /**
   * Get the names of the properties.
   * @return a set with the names of the properties
   */
  public Set<String> keySet() {

    return this.properties.keySet();
  }

  /**
   * Get the names and values of the properties.
   * @return a set with the names and values of the properties
   */
  public Collection<Map.Entry<String, String>> entrySet() {

    return this.properties.entries();
  }

  /**
   * Clear the properties.
   */
  public void clear() {

    this.clear();
  }

  /**
   * Get the number of properties in the section.
   * @return the number of properties in the section
   */
  public int size() {
    return this.properties.size();
  }

  /**
   * Test if the section is empty.
   * @return true if the section is empty
   */
  public boolean isEmpty() {

    return this.properties.isEmpty();
  }

  @Override
  public String toString() {

    return this.properties.toString();
  }

}
