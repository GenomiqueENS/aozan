package fr.ens.biologie.genomique.aozan.tests;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheetUtils;

/**
 * This class define the configuration of a test.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TestConfiguration {

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
   * Get a sample sheet.
   * @param key the setting key
   * @return the sample sheet in the configuration or an empty sample sheet
   */
  public SampleSheet getSampleSheet(final String key) {

    String value = get(key);

    if (value == null) {
      return new SampleSheet();
    }

    try {
      return SampleSheetUtils.deSerialize(value);
    } catch (IOException e) {
      return new SampleSheet();
    }
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

  /**
   * Test if the configuration is empty.
   * @return true if the configuration is empty
   */
  public boolean isEmpty() {
    return this.map.isEmpty();
  }

  /**
   * Create the test configuration from the Aozan settings and the prefix of the
   * test.
   * @param settings Aozan configuration
   * @param prefix test prefix
   * @param globalConf global configuration
   * @return a map with the test configuration
   */
  private static final Map<String, String> createConfiguration(
      final Settings settings, final String prefix,
      final Map<String, String> globalConf) {

    if (settings == null) {
      throw new NullPointerException("The settings object is null");
    }

    if (prefix == null) {
      throw new NullPointerException("The prefix object is null");
    }

    if (globalConf == null) {
      throw new NullPointerException("The globalConf object is null");
    }

    final Map<String, String> result = new LinkedHashMap<>();

    for (final Map.Entry<String, String> e : settings.entrySet()) {

      final String key = e.getKey();

      if (key.startsWith(prefix) && !key.endsWith(QC.TEST_KEY_ENABLED_SUFFIX)) {

        final String confKey = key.substring(prefix.length());
        final String confValue = e.getValue();

        result.put(confKey, confValue);

      }
    }

    result.putAll(globalConf);

    return result;
  }

  @Override
  public String toString() {
    return this.map.toString();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param settings Aozan settings
   * @param prefix test prefix
   * @param globalConf global configuration
   */
  public TestConfiguration(final Settings settings, final String prefix,
      final Map<String, String> globalConf) {

    this(createConfiguration(settings, prefix, globalConf));
  }

  /**
   * Public constructor.
   * @param map Collector configuration
   */
  public TestConfiguration(final Map<String, String> map) {

    if (map == null) {
      throw new NullPointerException("The map object is null");
    }

    this.map = new LinkedHashMap<>(map);
  }

}
