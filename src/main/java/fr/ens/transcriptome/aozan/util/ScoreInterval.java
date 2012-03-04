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

public class ScoreInterval {

  private final Interval[] intervals = new Interval[9];

  public void setInterval(final int score, final Interval interval) {

    if (score < 1 || score > 9)
      throw new IllegalArgumentException("Invalid score: " + score);

    if (interval == null)
      throw new NullPointerException("Interval is null");

    this.intervals[score - 1] = interval;

  }

  public int getScore(final Number value) {

    if (value == null)
      return 0;

    for (int i = 9; i > 0; i--)
      if (this.intervals[i] != null && this.intervals[i].isInInterval(value))
        return i;

    return 0;
  }

  public boolean configureLongInterval(final String key, final String value)
      throws AozanException {

    return configure(key, value, false);
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
