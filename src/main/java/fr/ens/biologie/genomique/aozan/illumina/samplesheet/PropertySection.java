package fr.ens.biologie.genomique.aozan.illumina.samplesheet;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Map;
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
   * Get property value.
   * @param key the key for the metadata
   * @return the value of the metadata
   */
  public String get(String key) {

    requireNonNull(key);

    Collection<String> result = this.properties.get(key.trim());

    return result == null || result.isEmpty() ? null : result.iterator().next();
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
  public String remove(String key) {

    requireNonNull(key);

    return this.remove(key.trim());
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

}
