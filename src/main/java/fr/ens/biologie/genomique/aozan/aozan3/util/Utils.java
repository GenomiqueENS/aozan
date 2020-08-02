package fr.ens.biologie.genomique.aozan.aozan3.util;

import static java.util.Objects.requireNonNull;

/**
 * This class contains some utility methods.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class Utils {

  /**
   * Remove the ".tmp" suffix of a string if exists.
   * @param s the string to process
   * @return a string without ".tmp" suffix
   */
  public static String removeTmpExtension(String s) {

    requireNonNull(s);

    if (s.endsWith(".tmp")) {
      return s.substring(0, s.length() - 4);
    }

    return s;
  }

}
