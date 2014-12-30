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
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.tests.global;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.tests.AozanTest;
import fr.ens.transcriptome.aozan.tests.TestResult;
import fr.ens.transcriptome.aozan.util.ScoreInterval;

/**
 * This class define a simple global test.
 * @since 1.3
 * @author Laurent Jourdren
 */
public abstract class AbstractSimpleGlobalTest extends AbstractGlobalTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null) {
      throw new NullPointerException("The properties object is null");
    }

    this.interval.configureDoubleInterval(properties);

    return Collections.singletonList((AozanTest) this);
  }

  //
  // Protected methods
  //

  /**
   * Get the the key in the RunData object for the value to test.
   * @return a String with the required key
   */
  protected abstract String getKey();

  /**
   * Transform the value.
   * @param value value to transform
   * @param data run data
   * @return the transformed value
   */
  protected Number transformValue(final Number value, final RunData data) {

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
  public TestResult test(final RunData data) {

    final String key = this.getKey();

    if (key == null) {
      return null;
    }

    final Class<?> clazz = this.getValueType();
    final String msg;
    final Number value;

    try {

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
      if (value == null) {
        return new TestResult(msg);
      }

      // Transform the value id needed
      final Number transformedValue = this.transformValue(value, data);

      // Do the test ?
      if (this.interval == null) {
        return new TestResult(transformedValue, this.isValuePercent());
      }

      return new TestResult(this.interval.getScore(transformedValue),
          transformedValue, this.isValuePercent());

    } catch (final NumberFormatException e) {

      return new TestResult("NA");
    }
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
  protected AbstractSimpleGlobalTest(final String name,
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
  protected AbstractSimpleGlobalTest(final String name,
      final String description, final String columnName, final String unit) {

    super(name, description, columnName, unit);
  }

}
