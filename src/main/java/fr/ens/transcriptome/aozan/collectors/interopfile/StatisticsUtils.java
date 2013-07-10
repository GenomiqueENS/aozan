/*
 *                 Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * @author Sandrine Perrin
 * @since 1.1
 */
public class StatisticsUtils {

  private DescriptiveStatistics ds;
  private double mean = 0.0;

  /**
   * Compute the mean for values
   * @return mean or NaN if no values have been added, or 0.0 for a single value
   *         set.
   */
  public double getMean() {
    if (this.mean > 0.0)
      return this.mean;

    return ds.getMean();
  }

  public double getStandardDeviation() {
    return getStandardDeviation(false);
  }

  /**
   * Compute the standard deviation for values
   * @param isBiasCorrected false per default
   * @return standard deviation NaN if no values have been added, or 0.0 for a
   *         single value set.
   */
  public double getStandardDeviation(final boolean isBiasCorrected) {
    double result = Double.NaN;
    double average = getMean();
    double sum = 0.0;
    Double val = 0.0;
    double count = 0.0;

    if (ds.getN() > 0) {
      if (ds.getN() > 1) {

        for (int i = 0; i < ds.getN(); i++) {

          val = ds.getElement(i);

          val -= average;
          sum += val * val;
          count++;

        }

        // With bias corrected, division by count values - 1
        count -= isBiasCorrected ? 1 : 0;

        result = Math.sqrt(sum / count);

      } else {
        result = 0.0;
      }
    }
    return result;
  }

  /**
   * Add values
   * @param number
   */
  public void addValues(Number number) {
    Double val = number.doubleValue();

    if (val != null)
      if (!val.isInfinite())
        this.ds.addValue(val);
  }

  //
  // Constructor
  //

  /**
   * Public constructor, build a list of values used for compute statistics.
   * @param list values
   */
  public StatisticsUtils(final List<? extends Number> list) {

    this.ds = new DescriptiveStatistics();

    for (Number n : list) {
      Double val = n.doubleValue();

      if (val != null)
        if (!val.isInfinite())
          this.ds.addValue(val);
    }
  }

  public StatisticsUtils() {
    this.ds = new DescriptiveStatistics();
  }
}
