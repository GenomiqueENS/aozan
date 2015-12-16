package fr.ens.biologie.genomique.aozan.illumina.samplesheet.io;

import java.io.IOException;
import java.util.List;

public class SampleSheetReaderUtils {

  public static final void trimFields(final List<String> fields)
      throws IOException {

    if (fields == null) {
      throw new IOException("The fields are null");
    }

    // Trim fields
    for (int i = 0; i < fields.size(); i++) {
      final String val = fields.get(i);
      if (val == null) {
        throw new IOException("Found null field.");
      }
      fields.set(i, val.trim());
    }

  }

  public static final void checkFields(final List<String> fields)
      throws IOException {

    if (fields.size() == 10) {
      return;
    }

    if (fields.size() < 10) {
      throw new IOException(
          "Invalid number of field (" + fields.size() + "), 10 excepted.");
    }

    for (int i = 10; i < fields.size(); i++) {
      if (!"".equals(fields.get(i).trim())) {
        throw new IOException(
            "Invalid number of field (" + fields.size() + "), 10 excepted.");
      }
    }
  }

  public static final int parseLane(final String s) throws IOException {

    if (s == null) {
      return 0;
    }

    final double d;
    try {
      d = Double.parseDouble(s);

    } catch (NumberFormatException e) {
      throw new IOException("Invalid lane number: " + s);
    }

    final int result = (int) d;

    if (d - result > 0) {
      throw new IOException("Invalid lane number: " + s);
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private SampleSheetReaderUtils() {
  }

}
