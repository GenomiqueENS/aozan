package fr.ens.transcriptome.aozan.tests;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.util.ScoreInterval;

/**
 * This class define a simple global test.
 * @since 1.x
 * @author Laurent Jourdren
 */
public abstract class AbstractSimpleGlobalTest extends AbstractGlobalTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    this.interval.configureDoubleInterval(properties);

    return Collections.singletonList((AozanTest) this);
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

    final String key = getKey();

    if (key == null)
      return null;

    final Class<?> clazz = getValueType();
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
      if (value == null)
        return new TestResult(msg);

      // Transform the value id needed
      final Number transformedValue = transformValue(value, data);

      // Do the test ?
      if (interval == null)
        return new TestResult(transformedValue, isValuePercent());

      return new TestResult(this.interval.getScore(transformedValue),
          transformedValue, isValuePercent());

    } catch (NumberFormatException e) {

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
