package fr.ens.biologie.genomique.aozan.util;

public class MathUtils {

  /**
   * Get the Q30 score from an array of longs with number of cluster for each
   * quality score
   * @return Q30 score.
   */
  public static double computeQ30(long[] values) {

    long count = 0;
    long count30 = 0;

    for (int i = 0; i < values.length; i++) {
      count += values[i];
      if (i >= 29) {
        count30 += values[i];
      }
    }

    return ((double) count30 / count);

  }
}
