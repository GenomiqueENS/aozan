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

package fr.ens.transcriptome.aozan.util;

import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import com.google.common.collect.Lists;

/**
 * This class contains statistics methods, using the class DescriptiveStatistics
 * from commons.apache.math
 * @author Sandrine Perrin
 * @since 1.1
 */
/**
 * @author sperrin
 */
public class StatisticsUtils {

  /** object maintains a dataset of values of a single variable */
  private final DescriptiveStatistics ds;
  /** copy dataset without values == 0 */
  private DescriptiveStatistics dsWithoutZero = null;

  /**
   * Compute the mean for values
   * @return mean or NaN if no values have been added, or 0.0 for a single value
   *         set.
   */
  public Double getMean() {

    return ds.getMean();
  }

  /**
   * Compute the mean for values which are different of 0
   * @return mean or NaN if no values have been added, or 0.0 for a single value
   *         set.
   */
  public Double getMeanWithoutZero() {

    buildDescriptiveStatisticsWithZero();

    // Build dataset of values from dataset initial
    for (Double d : ds.getValues()) {
      if (d != 0.0)
        dsWithoutZero.addValue(d);
    }

    return dsWithoutZero.getMean();
  }

  /**
   * Compute the median for values which are different of 0
   * @return median or NaN if no values have been added, or 0.0 for a single
   *         value set.
   */
  public Double getMedianWithoutZero() {

    buildDescriptiveStatisticsWithZero();

    return (dsWithoutZero.getN() == 0 ? 0.0 : new Median()
        .evaluate(dsWithoutZero.getValues()));

  }

  /**
   * Compute the median for values
   * @return median or NaN if no values have been added, or 0.0 for a single
   *         value set.
   */
  public Double getMediane() {
    return new Median().evaluate(ds.getValues());
  }

  /**
   * Compute the sum of values
   * @return sum of values.
   */
  public Double getSum() {
    return this.ds.getSum();
  }

  /**
   * Compute the standard deviation for values
   * @return standard deviation or NaN if no values have been added, or 0.0 for
   *         a single value set.
   */
  public Double getStandardDeviation() {
    return getStandardDeviation(false);
  }

  /**
   * Compute the mean for values
   * @return mean or NaN if no values have been added, or 0.0 for a single value
   *         set.
   */
  public int getMeanToInteger() {

    return getMean().intValue();
  }

  /**
   * Compute the mean for values which are different of 0
   * @return mean or NaN if no values have been added, or 0.0 for a single value
   *         set.
   */
  public int getMeanWithoutZeroToInteger() {
    return getMeanWithoutZero().intValue();
  }

  /**
   * Compute the median for values which are different of 0
   * @return median or NaN if no values have been added, or 0.0 for a single
   *         value set.
   */
  public int getMedianWithoutZeroToInteger() {
    return getMedianWithoutZero().intValue();
  }

  /**
   * Compute the median for values
   * @return median or NaN if no values have been added, or 0.0 for a single
   *         value set.
   */
  public int getMedianeToInteger() {
    return getMediane().intValue();
  }

  /**
   * Compute the sum of values
   * @return sum of values.
   */
  public int getSumToInteger() {
    return getSum().intValue();
  }

  /**
   * Compute the standard deviation for values
   * @return standard deviation or NaN if no values have been added, or 0.0 for
   *         a single value set.
   */
  public int getStandardDeviationToInteger() {
    return getStandardDeviation().intValue();
  }

  /**
   * Returns the instance of DescriptiveStatistics
   * @return instance of DescriptiveStatistics
   */
  public DescriptiveStatistics getDescriptiveStatistics() {
    return this.ds;
  }

  /**
   * Returns the instance of DescriptiveStatistics which doesn't contain the
   * value zero
   * @return instance of DescriptiveStatistics
   */
  public DescriptiveStatistics getDescriptiveStatisticsWithZero() {
    buildDescriptiveStatisticsWithZero();

    return this.dsWithoutZero;
  }

  /**
   * Print dataset
   * @return string of dataset
   */
  public String printValuesWithoutZero() {
    buildDescriptiveStatisticsWithZero();

    return printValues(dsWithoutZero);
  }

  /**
   * Print dataset
   * @return string of dataset
   */
  public String printValues() {
    return printValues(ds);
  }

  /**
   * Print dataset
   * @return string of dataset
   */
  private String printValues(final DescriptiveStatistics stat) {
    StringBuilder s = new StringBuilder();

    for (double d : stat.getValues()) {
      s.append(d);
      s.append("\n");
    }

    return s.toString();
  }

  /**
   * Compute the standard deviation for values
   * @param isBiasCorrected false per default
   * @return standard deviation NaN if no values have been added, or 0.0 for a
   *         single value set.
   */
  public Double getStandardDeviation(final boolean isBiasCorrected) {
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
   * Add values in dataset
   * @param number new values to put in dataset
   */
  public void addValues(Number number) {
    Double val = number.doubleValue();

    if (val != null)
      if (!val.isInfinite())
        this.ds.addValue(val);
  }

  /**
   * Builds dataset without value zero of each call, if the dataset source has
   * been modified
   */
  private void buildDescriptiveStatisticsWithZero() {

    this.dsWithoutZero = new DescriptiveStatistics();

    for (Double d : ds.getValues()) {
      if (d != 0.0)
        dsWithoutZero.addValue(d);
    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor, build a list of values used for compute statistics.
   * Infinity values are ignored.
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

  /**
   * Public constructor, build a list of values used for compute statistics.
   * Infinity values are ignored.
   * @param list values
   */
  public StatisticsUtils(final Collection<? extends Number> list) {
    this(Lists.newArrayList(list));

  }

  /**
   * Public constructor with a empty dataset.
   */
  public StatisticsUtils() {
    this.ds = new DescriptiveStatistics();
  }

}
