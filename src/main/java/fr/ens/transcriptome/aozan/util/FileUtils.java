package fr.ens.transcriptome.aozan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.common.collect.Lists;

public class FileUtils {

  /**
   * Convert a file in string list 
   * @param is input stream 
   * @return list of string from file
   * @throws IOException
   */
  public static List<String> readFileByLines(final InputStream is)
      throws IOException {

    if (is == null)
      return null;

    final List<String> result = Lists.newArrayList();

    BufferedReader reader = new BufferedReader(new InputStreamReader(is));

    String line = null;

    while ((line = reader.readLine()) != null) {
      result.add(line);
    }

    reader.close();
    return result;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private FileUtils() {
  }

}
