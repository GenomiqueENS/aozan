package fr.ens.biologie.genomique.aozan.util;

import static java.util.Objects.requireNonNull;

import java.util.List;

/**
 * This class define a line entry of a CSV file.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class CSVLineParser {

  private final CVSParser parser;
  private final List<String> fields;

  /**
   * Test if the line contains a field.
   * @param fieldName the name of the field to test
   * @return true if the line contains a field
   */
  public boolean contains(String fieldName) {

    Integer pos = this.parser.header.get(fieldName);

    return pos != null && pos < this.fields.size();
  }

  /**
   * Get the value of a field.
   * @param fieldName the field name
   * @return the value of the field. Can be null
   */
  public String get(String fieldName) {

    return get(fieldName, null);
  }

  /**
   * Get the value of a field.
   * @param fieldName the field name
   * @param defaultValue the default value if the field does not exists
   * @return the value of the field. Can be null
   */
  public String get(String fieldName, String defaultValue) {

    Integer pos = this.parser.header.get(fieldName);

    if (pos == null || pos >= fields.size()) {
      return defaultValue;
    }

    return this.fields.get(pos);
  }

  /**
   * Get the value of a field as an int.
   * @param fieldName the field name
   * @return the value of the field. Can be null
   * @throws NullPointerException if value does not exists
   */
  public int getInt(String fieldName) {

    String result = get(fieldName);

    requireNonNull(result);

    return Integer.parseInt(result);
  }

  /**
   * Get the value of a field as an integer.
   * @param fieldName the field name
   * @param defaultValue default value if value does not exists
   * @return the value of the field. Can be null
   * @throws NullPointerException if value is null
   */
  public int getInt(String fieldName, int defaultValue) {

    String result = get(fieldName, "" + defaultValue);

    requireNonNull(result);

    return Integer.parseInt(result);
  }

  /**
   * Get the value of a field as a long.
   * @param fieldName the field name
   * @return the value of the field. Can be null
   * @throws NullPointerException if value does not exists
   */
  public long getLong(String fieldName) {

    String result = get(fieldName);

    requireNonNull(result);

    return Long.parseLong(result);
  }

  /**
   * Get the value of a field as a long.
   * @param fieldName the field name
   * @param defaultValue default value if value does not exists
   * @return the value of the field. Can be null
   * @throws NullPointerException if value is null
   */
  public long getLong(String fieldName, int defaultValue) {

    String result = get(fieldName, "" + defaultValue);

    requireNonNull(result);

    return Long.parseLong(result);
  }

  /**
   * Get the value of a field as a float.
   * @param fieldName the field name
   * @return the value of the field. Can be null
   * @throws NullPointerException if value does not exists
   */
  public float getFloat(String fieldName) {

    String result = get(fieldName);

    requireNonNull(result);

    return Float.parseFloat(result);
  }

  /**
   * Get the value of a field as a float.
   * @param fieldName the field name
   * @param defaultValue default value if value does not exists
   * @return the value of the field. Can be null
   * @throws NullPointerException if value is null
   */
  public float getFloat(String fieldName, float defaultValue) {

    String result = get(fieldName, "" + defaultValue);

    requireNonNull(result);

    return Float.parseFloat(result);
  }

  /**
   * Get the value of a field as a double.
   * @param fieldName the field name
   * @return the value of the field. Can be null
   * @throws NullPointerException if value does not exists
   */
  public double getDouble(String fieldName) {

    String result = get(fieldName);

    requireNonNull(result);

    return Double.parseDouble(result);
  }

  /**
   * Get the value of a field as a double.
   * @param fieldName the field name
   * @param defaultValue default value if value does not exists
   * @return the value of the field. Can be null
   * @throws NullPointerException if value is null
   */
  public double getDouble(String fieldName, double defaultValue) {

    String result = get(fieldName, "" + defaultValue);

    requireNonNull(result);

    return Double.parseDouble(result);
  }

  //
  // Constructor
  //

  /**
   * Package constructor.
   * @param parser parent parser
   * @param line line to parse
   */
  CSVLineParser(CVSParser parser, String line) {

    requireNonNull(parser);
    requireNonNull(line);

    this.parser = parser;
    this.fields = parser.splitter.splitToList(line);
  }
}