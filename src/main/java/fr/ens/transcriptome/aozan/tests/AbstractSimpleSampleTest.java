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

package fr.ens.transcriptome.aozan.tests;

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.util.Interval;

public abstract class AbstractSimpleSampleTest extends AbstractSampleTest {

  private Interval interval;

  //
  // Getters
  //

  /**
   * Get the interval.
   * @return Returns the interval
   */
  public Interval getInterval() {
    return interval;
  }

  //
  // Setters
  //

  /**
   * Set the interval.
   * @param interval The interval to set
   */
  public void setInterval(final Interval interval) {
    this.interval = interval;
  }

  //
  // Protected method
  //

  /**
   * Get the the key in the RunData object for the value to test
   * @param read index of the read
   * @param lane index of the lane
   * @param sampleName name of the sample. If null, must return the key for
   *          undetermined indexes
   * @return a String with the required key
   */
  protected abstract String getKey(final int read, final int lane,
      final String sampleName);

  //
  // AbstractLaneTest methods
  //

  /**
   * Get the type of the value.
   * @return a Class object with the type
   */
  protected abstract Class<?> getValueType();

  @Override
  public TestResult test(final RunData data, final int read, final int lane,
      final String sampleName) {

    final String key = getKey(read, lane, sampleName);

    if (key == null)
      return null;

    final Class<?> clazz = getValueType();
    final String msg;
    final Number value;

    if (clazz == Integer.class) {

      final int val = data.getInt(key);
      msg = String.format(INTEGER_FORMAT, val);
      value = val;
    } else if (clazz == Long.class) {

      final long val = data.getLong(key);
      msg = String.format(INTEGER_FORMAT, val);
      value = val;
    } else if (clazz == Float.class) {

      final float val = data.getFloat(key);
      msg = String.format(DOUBLE_FORMAT, val);
      value = val;
    } else if (clazz == Double.class) {

      final double val = data.getDouble(key);
      msg = String.format(DOUBLE_FORMAT, val);
      value = val;
    } else {

      msg = data.get(key);
      value = null;
    }

    if (value == null || interval == null)
      return new TestResult(msg);

    return new TestResult(interval.isInInterval(value) ? 9 : 0, msg);
  }

  //
  // Constructor
  //

  /**
   * Constructor that set the field of this abstract test.
   * @param name name of the test
   * @param description description of the test
   * @param columnName column name of the test
   */
  protected AbstractSimpleSampleTest(final String name,
      final String description, final String columnName) {

    super(name, description, columnName);
  }

  /**
   * Constructor that set the field of this abstract test.
   * @param name name of the test
   * @param description description of the test
   * @param columnName column name of the test
   * @param unit unit of the test
   */
  protected AbstractSimpleSampleTest(final String name,
      final String description, final String columnName, final String unit) {

    super(name, description, columnName, unit);
  }

}
