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

import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.util.Interval;

/**
 * This class define a simple lane test.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractSimpleLaneTest extends AbstractLaneTest {

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
  // Other methods
  //

  @Override
  public void configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

  }

  //
  // Protected methods
  //

  /**
   * Get the the key in the RunData object for the value to test
   * @param read index of the read
   * @param indexedRead true if the read is indexed
   * @param lane index of the lane
   * @return a String with the required key
   */
  protected abstract String getKey(final int read, final boolean indexedRead,
      final int lane);

  /**
   * Transform the value.
   * @param value value to transform
   * @param data run data
   * @param read index of read
   * @param lane lane index
   * @param indexedRead true if the read is indexed
   * @return the transformed value
   */
  protected Number transformValue(final Number value, final RunData data,
      final int read, final boolean indexedRead, final int lane) {

    return value;
  }

  /**
   * Test if the value is a percent.
   * @return true if the value is a percent
   */
  protected boolean isValuePercent() {

    return false;
  }

  /**
   * Get the type of the value.
   * @return a Class object with the type
   */
  protected abstract Class<?> getValueType();

  @Override
  public TestResult test(final RunData data, final int read,
      final boolean indexedRead, final int lane) {

    final String key = getKey(read, indexedRead, lane);

    if (key == null)
      return null;

    final Class<?> clazz = getValueType();
    final String msg;
    final Number value;

    if (clazz == Integer.class) {

      value = data.getInt(key);
      msg = null;
    } else if (clazz == Long.class) {

      value = data.getLong(key);
      msg = null;
    } else if (clazz == Float.class) {

      value = data.getFloat(key);

      msg = null;
    } else if (clazz == Double.class) {

      value = data.getDouble(key);
      msg = null;
    } else {

      msg = data.get(key);
      value = null;
    }

    // Is result a string ?
    if (value == null)
      return new TestResult(msg);

    // Transform the value id needed
    final Number transformedValue =
        transformValue(value, data, read, indexedRead, lane);

    // Do the test ?
    if (interval == null || (indexedRead && !testIndexedRead()))
      return new TestResult(transformedValue, isValuePercent());

    return new TestResult(interval.isInInterval(transformedValue) ? 9 : 0,
        transformedValue, isValuePercent());
  }

  /**
   * Test if indexed read test must return a score >=0.
   * @return if indexed read must return a score
   */
  protected boolean testIndexedRead() {

    return false;
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
  protected AbstractSimpleLaneTest(final String name, final String description,
      final String columnName) {

    super(name, description, columnName);
  }

  /**
   * Constructor that set the field of this abstract test.
   * @param name name of the test
   * @param description description of the test
   * @param columnName column name of the test
   * @param unit unit of the test
   */
  protected AbstractSimpleLaneTest(final String name, final String description,
      final String columnName, final String unit) {

    super(name, description, columnName, unit);
  }

}
