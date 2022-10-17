/*
 *                  Aozan development code
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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.util;

import com.google.common.base.MoreObjects;

import fr.ens.biologie.genomique.aozan.AozanException;

/**
 * This class define a double interval.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class DoubleInterval implements Interval {

  private final double min;
  private final boolean minInclude;
  private final double max;
  private final boolean maxInclude;

  //
  // Getters
  //

  /**
   * Get the minimal value of the interval.
   * @return the minimal value of the interval
   */
  public double getMin() {

    return this.min;
  }

  /**
   * Test if the minimal value is included in the interval.
   * @return true if the minimal value is included in the interval.
   */
  public boolean isMinIncluded() {

    return this.minInclude;
  }

  /**
   * Get the maximal value of the interval.
   * @return the maximal value of the interval
   */
  public double getMax() {

    return this.max;
  }

  /**
   * Test if the maximal value is included in the interval.
   * @return true if the maximal value is included in the interval.
   */
  public boolean isMaxIncluded() {

    return this.maxInclude;
  }

  //
  // Other methods
  //

  @Override
  public boolean isInInterval(final Number value) {

    if (value == null) {
      return false;
    }

    final double val = value.doubleValue();

    if (Double.isNaN(val)) {
      return false;
    }

    if (this.min != Double.NEGATIVE_INFINITY) {

      if (this.minInclude && val < this.min) {
        return false;
      }

      if (!this.minInclude && val <= this.min) {
        return false;
      }
    }

    if (this.max != Double.POSITIVE_INFINITY) {

      if (this.maxInclude && val > this.max) {
        return false;
      }

      if (!this.maxInclude && val >= this.max) {
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this).add("min", this.min)
        .add("minInclude", this.minInclude).add("max", this.max)
        .add("maxInclude", this.maxInclude).toString();
  }

  //
  // Constructor
  //

  public DoubleInterval(final double min, final double max) {

    this(min, true, max, true);
  }

  public DoubleInterval(final double min, final boolean minIncluded,
      final double max, final boolean maxIncluded) {

    if (Double.isNaN(min)) {
      throw new IllegalArgumentException("Invalid min argument: " + min);
    }

    if (Double.isNaN(max)) {
      throw new IllegalArgumentException("Invalid max argument: " + max);
    }

    this.min = Math.min(min, max);
    this.minInclude = minIncluded;
    this.max = Math.max(min, max);
    this.maxInclude = maxIncluded;
  }

  public DoubleInterval(final String s) throws AozanException {

    if (s == null) {
      throw new NullPointerException("The interval string is null");
    }

    final String trimmed = s.trim();

    // Exclude minimal end point ?
    switch (trimmed.charAt(0)) {

    case '[':
      this.minInclude = true;
      break;
    case ']':
    case '(':
      this.minInclude = false;
      break;
    default:
      throw new AozanException("Invalid interval: " + s);
    }

    // Exclude maximal end point ?
    switch (trimmed.charAt(trimmed.length() - 1)) {

    case ']':
      this.maxInclude = true;
      break;
    case '[':
    case ')':
      this.maxInclude = false;
      break;
    default:
      throw new AozanException("Invalid interval: " + s);
    }

    // Get the values of end points
    final String[] values =
        (trimmed.substring(1, trimmed.length() - 1) + " ").split(",");

    if (values == null || values.length != 2) {
      throw new AozanException("Invalid interval: " + s);
    }

    // Trim the values
    final String minString = values[0].trim();
    final String maxString = values[1].trim();

    try {

      if ("".equals(minString)) {
        this.min = Double.NEGATIVE_INFINITY;
      } else {
        this.min = Double.parseDouble(minString);
      }

      if ("".equals(maxString)) {
        this.max = Double.POSITIVE_INFINITY;
      } else {
        this.max = Double.parseDouble(maxString);
      }

    } catch (final NumberFormatException e) {
      throw new AozanException("Invalid interval: " + s);
    }

  }

}
