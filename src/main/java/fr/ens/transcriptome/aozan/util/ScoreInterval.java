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

import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;

public class ScoreInterval {

  private final Interval[] intervals = new Interval[9];
  private int intervalsDefined = 0;

  public void setInterval(final int score, final Interval interval) {

    if (score < 1 || score > 9)
      throw new IllegalArgumentException("Invalid score: " + score);

    if (interval == null)
      throw new NullPointerException("Interval is null");

    final int index = score - 1;

    if (this.intervals[index] == null)
      this.intervalsDefined++;
    this.intervals[index] = interval;
  }

  public int getScore(final Number value) {

    if (value == null)
      return 0;

    if (this.intervalsDefined == 0)
      return -1;

    for (int i = 8; i >= 0; i--)
      if (this.intervals[i] != null && this.intervals[i].isInInterval(value))
        return i + 1;

    return 0;
  }

  public void configureLongInterval(final Map<String, String> properties)
      throws AozanException {

    if (properties != null)
      for (Map.Entry<String, String> e : properties.entrySet())
        configureLongInterval(e.getKey(), e.getValue());
  }

  public boolean configureLongInterval(final String key, final String value)
      throws AozanException {

    return configure(key, value, false);
  }

  public void configureDoubleInterval(final Map<String, String> properties)
      throws AozanException {

    if (properties != null)
      for (Map.Entry<String, String> e : properties.entrySet())
        configureDoubleInterval(e.getKey(), e.getValue());
  }

  public boolean configureDoubleInterval(final String key, final String value)
      throws AozanException {

    return configure(key, value, true);
  }

  private boolean configure(final String key, final String value,
      final boolean doubleInterval) throws AozanException {

    if (key == null || value == null)
      return false;

    String trimmedKey = key.trim().toLowerCase();

    if ("interval".equals(trimmedKey)) {
      setInterval(9, doubleInterval
          ? new DoubleInterval(value) : new LongInterval(value));
      return true;
    }

    for (int i = 1; i <= 9; i++) {
      if (("score" + i + ".interval").equals(trimmedKey)) {
        setInterval(i, doubleInterval
            ? new DoubleInterval(value) : new LongInterval(value));
        return true;
      }
    }

    return false;
  }
}
