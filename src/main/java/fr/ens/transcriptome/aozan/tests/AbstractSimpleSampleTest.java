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
   * @param readSample index of read without indexed reads
   * @param lane index of the lane
   * @param sampleName name of the sample. If null, must return the key for
   *          undetermined indexes
   * @return a String with the required key
   */
  protected abstract String getKey(final int read, int readSample,
      final int lane, final String sampleName);

  /**
   * Transform the value.
   * @param value value to transform
   * @param data run data
   * @param read index of read
   * @param readSample index of read without indexed reads
   * @param lane lane index
   * @param sampleName sample name
   * @return the transformed value
   */
  protected Number transformValue(final Number value, final RunData data,
      final int read, int readSample, final int lane, final String sampleName) {

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
  public TestResult test(final RunData data, final int read, int readSample,
      final int lane, final String sampleName) {

    final String key = getKey(read, readSample, lane, sampleName);

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
        transformValue(value, data, read, readSample, lane, sampleName);

    // Do the test ?
    if (interval == null || sampleName == null)
      return new TestResult(transformedValue, isValuePercent());

    return new TestResult(interval.isInInterval(transformedValue) ? 9 : 0,
        transformedValue, isValuePercent());
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
