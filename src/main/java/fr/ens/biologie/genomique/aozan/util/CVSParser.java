package fr.ens.biologie.genomique.aozan.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;

/**
 * This class allow to parse CSV files
 * @author Laurent Jourdren
 * @since 3.0
 */
public class CVSParser implements Iterable<CSVLineParser> {

  final Splitter splitter;
  final Map<String, Integer> header;
  private final List<String> headerOrder;
  private final List<String> lines;

  /**
   * Test if the file contains a field
   * @param fieldName field name
   * @return true if the field exists in the file
   */
  public boolean contains(String fieldName) {

    return this.header.containsKey(fieldName);
  }

  /**
   * Get the list of the fields.
   * @return a list with the fields of the file
   */
  public List<String> fields() {

    return Collections.unmodifiableList(this.headerOrder);
  }

  /**
   * Parse a line.
   * @param line line to parse
   * @return a CSVLineParser object
   */
  public CSVLineParser parse(String line) {

    return new CSVLineParser(this, line);
  }

  @Override
  public Iterator<CSVLineParser> iterator() {

    final CVSParser parser = this;
    final Iterator<String> it = lines.iterator();

    return new Iterator<CSVLineParser>() {

      @Override
      public boolean hasNext() {

        return it.hasNext();
      }

      @Override
      public CSVLineParser next() {

        return new CSVLineParser(parser, it.next());
      }

    };
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param file file to read
   * @throws IOException if an error occurs while reading file
   */
  public CVSParser(Path file) throws IOException {
    this(file, ',');
  }

  /**
   * Constructor.
   * @param file file to read
   * @param separator separator char
   * @throws IOException if an error occurs while reading file
   */
  public CVSParser(Path file, char separator) throws IOException {
    this(Files.readAllLines(file), separator);
  }

  /**
   * Constructor.
   * @param lines lines to parse
   */
  public CVSParser(List<String> lines) {
    this(lines, ',');
  }

  /**
   * Constructor.
   * @param lines lines to parse
   * @param separator separator char
   */
  public CVSParser(List<String> lines, char separator) {

    if (!(separator == ',' || separator == ';' || separator == '\t')) {
      throw new IllegalArgumentException(
          "Invalid separator character: " + separator);
    }

    requireNonNull(lines);

    Splitter splitter = null;
    List<String> headerOrder = null;
    Map<String, Integer> header = null;
    int count = 0;

    for (String line : lines) {

      count++;
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }

      splitter = Splitter.on(separator).trimResults();

      headerOrder = splitter.splitToList(line);
      header = new HashMap<>(headerOrder.size());

      for (int i = 0; i < headerOrder.size(); i++) {
        header.put(headerOrder.get(i), i);
      }

      break;
    }

    this.lines = lines.subList(count, lines.size());
    this.splitter = splitter;
    this.headerOrder = headerOrder;
    this.header = header;
  }

}