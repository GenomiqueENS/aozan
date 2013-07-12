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
 * @author Sandrine Perrin
 * @since 1.1
 */
public class StatisticsUtils {

  private DescriptiveStatistics ds;
  private double mean = 0.0;

  private DescriptiveStatistics dsWithZero = null;

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

  // value 0.0 not remove
  public double getMeanWithoutZero() {

    if (dsWithZero == null) {

      dsWithZero = new DescriptiveStatistics();

      for (Double d : ds.getValues()) {
        if (d != 0.0)
          dsWithZero.addValue(d);
      }
    }
    return dsWithZero.getMean();
  }

  public double getMedianWithoutZero() {

    if (dsWithZero == null) {
      dsWithZero = new DescriptiveStatistics();

      for (Double d : ds.getValues()) {
        if (d != 0.0)
          dsWithZero.addValue(d);
      }
    }
    return new Median().evaluate(dsWithZero.getValues());

  }

  public double getStandardDeviation() {
    return getStandardDeviation(false);
  }

  public double getMediane() {
    return new Median().evaluate(ds.getValues());
  }

  public DescriptiveStatistics getDescriptiveStatistics() {
    return this.ds;
  }

  public DescriptiveStatistics getDSWithZero() {
    return this.dsWithZero;
  }

  public String printValues() {
    StringBuilder s = new StringBuilder();

    for (double d : ds.getValues()) {
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

  public StatisticsUtils(final Collection<? extends Number> list) {
    this(Lists.newArrayList(list));

  }

  public StatisticsUtils() {
    this.ds = new DescriptiveStatistics();
  }
}
