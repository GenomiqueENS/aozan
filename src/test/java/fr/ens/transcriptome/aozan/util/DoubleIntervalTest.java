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
import junit.framework.TestCase;

public class DoubleIntervalTest extends TestCase {

  public void testIsInInterval() {

    DoubleInterval di = new DoubleInterval(10.0, 20.0);
    assertFalse(di.isInInterval(Double.NaN));
    assertFalse(di.isInInterval(Double.NEGATIVE_INFINITY));
    assertFalse(di.isInInterval(Double.POSITIVE_INFINITY));
    assertFalse(di.isInInterval(5.0));
    assertFalse(di.isInInterval(9.99999));
    assertTrue(di.isInInterval(10));
    assertTrue(di.isInInterval(10.0));
    assertTrue(di.isInInterval(15.0));
    assertTrue(di.isInInterval(20.0));
    assertTrue(di.isInInterval(20));
    assertFalse(di.isInInterval(20.00001));
    assertFalse(di.isInInterval(25.0));

    di = new DoubleInterval(Double.NEGATIVE_INFINITY, 20.0);
    assertFalse(di.isInInterval(Double.NaN));
    assertTrue(di.isInInterval(Double.NEGATIVE_INFINITY));
    assertTrue(di.isInInterval(Double.MIN_NORMAL));
    assertTrue(di.isInInterval(Double.MIN_VALUE));
    assertFalse(di.isInInterval(Double.POSITIVE_INFINITY));
    assertTrue(di.isInInterval(10));

    di = new DoubleInterval(Double.NEGATIVE_INFINITY, 20.0);
    assertFalse(di.isInInterval(Double.NaN));
    assertTrue(di.isInInterval(Double.NEGATIVE_INFINITY));
    assertTrue(di.isInInterval(Double.MIN_NORMAL));
    assertTrue(di.isInInterval(Double.MIN_VALUE));
    assertFalse(di.isInInterval(Double.POSITIVE_INFINITY));
    assertTrue(di.isInInterval(10));
    assertFalse(di.isInInterval(25.0));

    di = new DoubleInterval(10.0, Double.POSITIVE_INFINITY);
    assertFalse(di.isInInterval(Double.NaN));
    assertFalse(di.isInInterval(Double.NEGATIVE_INFINITY));
    assertFalse(di.isInInterval(5.0));
    assertTrue(di.isInInterval(10));
    assertTrue(di.isInInterval(25.0));
    assertTrue(di.isInInterval(Double.MAX_VALUE));
    assertTrue(di.isInInterval(Double.POSITIVE_INFINITY));
  }

  public void testDoubleIntervalDoubleDouble() {

    DoubleInterval di = new DoubleInterval(10.0, 20.0);
    assertEquals(10.0, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    di = new DoubleInterval(20.0, 10.0);
    assertEquals(10.0, di.getMin(), 0.001);
    assertEquals(20.0, di.getMax(), 0.001);

    di = new DoubleInterval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    assertEquals(Double.NEGATIVE_INFINITY, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(Double.POSITIVE_INFINITY, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    di = new DoubleInterval(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    assertEquals(Double.NEGATIVE_INFINITY, di.getMin(), 0.001);
    assertEquals(Double.POSITIVE_INFINITY, di.getMax(), 0.001);

    try {
      di = new DoubleInterval(Double.NaN, 20.0);
      assertTrue(false);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }

  }

  public void testDoubleIntervalDoubleBooleanDoubleBoolean() {

    DoubleInterval di = new DoubleInterval(10.0, true, 20.0, true);
    assertEquals(10.0, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    di = new DoubleInterval(10.0, false, 20.0, true);
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    di = new DoubleInterval(10.0, true, 20.0, false);
    assertEquals(10.0, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    di = new DoubleInterval(10.0, false, 20.0, false);
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());
  }

  public void testDoubleIntervalString() {

    DoubleInterval di = null;
    try {
      di = new DoubleInterval("[10.0, 20.0]");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    try {
      di = new DoubleInterval("]10.0, 20.0]");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    try {
      di = new DoubleInterval("(10.0, 20.0]");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    try {
      di = new DoubleInterval("[10.0, 20.0[");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("[10.0, 20.0)");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("]10.0, 20.0[");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("(10.0, 20.0)");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("]10,20[");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("]10,  20[");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("]10; 20[");
      assertTrue(false);
    } catch (AozanException e) {
      assertTrue(true);
    }

    try {
      di = new DoubleInterval(")10, 20(");
      assertTrue(false);
    } catch (AozanException e) {
      assertTrue(true);
    }

  }

}
