/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan.util;

import fr.ens.transcriptome.aozan.AozanException;

/**
 * This class define a double interval
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DoubleInterval implements Interval {

  private final double min;
  private final boolean minInclude;
  private final double max;
  private final boolean maxInclude;

  @Override
  public boolean isInInterval(final Number value) {

    if (value == null)
      return false;

    final double val = value.doubleValue();

    if (this.min != Double.NEGATIVE_INFINITY) {

      if (this.minInclude && val < min)
        return false;

      if (!this.minInclude && val <= min)
        return false;
    }

    if (this.max != Double.POSITIVE_INFINITY) {

      if (this.maxInclude && val > max)
        return false;

      if (!this.maxInclude && val >= max)
        return false;
    }

    return true;
  }

  //
  // Constructor
  //

  public DoubleInterval(final double min, final double max) {

    this(min, true, max, true);
  }

  public DoubleInterval(final double min, final boolean minIncluded,
      final double max, final boolean maxIncluded) {

    this.min = min;
    this.minInclude = minIncluded;
    this.max = max;
    this.maxInclude = maxIncluded;
  }

  public DoubleInterval(final String s) throws AozanException {

    if (s == null)
      throw new NullPointerException("The interval string is null");

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
        trimmed.substring(1, trimmed.length() - 1).split(",");

    if (values == null || values.length != 2)
      throw new AozanException("Invalid interval: " + s);

    // Trim the values
    final String minString = values[0].trim();
    final String maxString = values[0].trim();

    try {
      
      if ("".equals(minString))
        this.min = Double.NEGATIVE_INFINITY;
      else
        this.min = Double.parseDouble(minString);

      if ("".equals(maxString))
        this.max = Double.POSITIVE_INFINITY;
      else
        this.max = Double.parseDouble(maxString);
      
    } catch (NumberFormatException e) {
      throw new AozanException("Invalid interval: " + s);
    }

  }

}
