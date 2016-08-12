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

package fr.ens.biologie.genomique.aozan.tests.sample;

import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * This class define a simple sample test.
 * @since 0.8
 * @author Laurent Jourdren
 */
public abstract class AbstractSimpleSampleTest extends AbstractSampleTest {

  private final ScoreInterval interval = new ScoreInterval();

  /**
   * Get the the key in the RunData object for the value to test.
   * @param read index of the read
   * @param readSample index of read without indexed reads
   * @param sampleId the id of the sample
   * @param lane sample lane
   * @param undetermined true if the sample is an undetermined sample
   * @return a String with the required key
   */
  protected abstract String getKey(final int read, int readSample, int sampleId,
      int lane, boolean undetermined);

  /**
   * Transform the value.
   * @param value value to transform
   * @param data run data
   * @param read index of read
   * @param readSample index of read without indexed reads
   * @param sampleId the sample Id
   * @return the transformed value
   */
  protected Number transformValue(final Number value, final RunData data,
      final int read, final int readSample, final int sampleId) {

    return value;
  }

  /**
   * Transform the score.
   * @param score value to transform
   * @param data run data
   * @param read index of read
   * @param readSample index of read without indexed reads
   * @param sampleId the sample id
   * @return the transformed score
   */
  protected int transformScore(final int score, final RunData data,
      final int read, final int readSample, final int sampleId) {

    return score;
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
      final int readSample, final int sampleId) {

    final boolean undetermined = data.isUndeterminedSample(sampleId);
    final int lane = data.getSampleLane(sampleId);

    final String key = getKey(read, readSample, sampleId, lane, undetermined);

    if (key == null) {
      return null;
    }

    // Key not present in data, case with fastqscreen, a genome specific from a
    // project
    if (data.get(key) == null) {
      return new TestResult("NA");
    }

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
      if (value == null) {
        return new TestResult(msg);
      }

      // Transform the value id needed
      final Number transformedValue =
          transformValue(value, data, read, readSample, sampleId);

      // Do the test ?
      if (this.interval == null || undetermined) {
        return new TestResult(transformedValue, isValuePercent());
      }

      final int score = transformScore(this.interval.getScore(transformedValue),
          data, read, readSample, sampleId);

      return new TestResult(score, transformedValue, isValuePercent());

    } catch (final NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  //
  // Other methods
  //

  @Override
  public List<AozanTest> configure(final TestConfiguration conf)
      throws AozanException {

    if (conf == null) {
      throw new NullPointerException("The conf object is null");
    }

    this.interval.configureDoubleInterval(conf);

    return Collections.singletonList((AozanTest) this);
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
