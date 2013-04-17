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

package fr.ens.transcriptome.aozan.util;

import junit.framework.TestCase;
import fr.ens.transcriptome.aozan.AozanException;

public class LongIntervalTest extends TestCase {

  public void testIsInInterval() {

    LongInterval di = new LongInterval(10, 20);
    assertFalse(di.isInInterval(Long.MIN_VALUE));
    assertFalse(di.isInInterval(Long.MAX_VALUE));
    assertFalse(di.isInInterval(5));
    assertFalse(di.isInInterval(9));
    assertTrue(di.isInInterval(10));
    assertTrue(di.isInInterval(15));
    assertTrue(di.isInInterval(20));
    assertTrue(di.isInInterval(20));
    assertFalse(di.isInInterval(25));

    di = new LongInterval(Long.MIN_VALUE, 20);
    assertTrue(di.isInInterval(Long.MIN_VALUE));
    assertFalse(di.isInInterval(Long.MAX_VALUE));
    assertTrue(di.isInInterval(10));

    di = new LongInterval(Long.MIN_VALUE, 20);
    assertTrue(di.isInInterval(Long.MIN_VALUE));
    assertFalse(di.isInInterval(Long.MAX_VALUE));
    assertTrue(di.isInInterval(10));
    assertFalse(di.isInInterval(25));

    di = new LongInterval(10, Long.MAX_VALUE);
    assertFalse(di.isInInterval(5));
    assertTrue(di.isInInterval(10));
    assertTrue(di.isInInterval(25));
    assertTrue(di.isInInterval(Long.MAX_VALUE));

  }

  public void testLongIntervalDoubleDouble() {

    LongInterval di = new LongInterval(10, 20);
    assertEquals(10, di.getMin());
    assertTrue(di.isMinIncluded());
    assertEquals(20, di.getMax());
    assertTrue(di.isMaxIncluded());

    di = new LongInterval(20, 10);
    assertEquals(10, di.getMin());
    assertEquals(20, di.getMax());

    di = new LongInterval(Long.MIN_VALUE, Long.MAX_VALUE);
    assertEquals(Long.MIN_VALUE, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(Long.MAX_VALUE, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    di = new LongInterval(Long.MAX_VALUE, Long.MIN_VALUE);
    assertEquals(Long.MIN_VALUE, di.getMin(), 0.001);
    assertEquals(Long.MAX_VALUE, di.getMax(), 0.001);

  }

  public void testLongIntervalDoubleBooleanDoubleBoolean() {

    LongInterval di = new LongInterval(10, true, 20, true);
    assertEquals(10, di.getMin());
    assertTrue(di.isMinIncluded());
    assertEquals(20, di.getMax());
    assertTrue(di.isMaxIncluded());

    di = new LongInterval(10, false, 20, true);
    assertEquals(10, di.getMin());
    assertFalse(di.isMinIncluded());
    assertEquals(20, di.getMax());
    assertTrue(di.isMaxIncluded());

    di = new LongInterval(10, true, 20, false);
    assertEquals(10, di.getMin());
    assertTrue(di.isMinIncluded());
    assertEquals(20, di.getMax());
    assertFalse(di.isMaxIncluded());

    di = new LongInterval(10, false, 20, false);
    assertEquals(10, di.getMin());
    assertFalse(di.isMinIncluded());
    assertEquals(20, di.getMax());
    assertFalse(di.isMaxIncluded());
  }

  public void testLongIntervalString() {

    LongInterval di = null;
    try {
      di = new LongInterval("[10, 20]");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    try {
      di = new LongInterval("]10, 20]");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    try {
      di = new LongInterval("(10, 20]");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertTrue(di.isMaxIncluded());

    try {
      di = new LongInterval("[10, 20[");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("[10, 20)");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertTrue(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("]10, 20[");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("(10, 20)");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("]10,20[");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("]10,  20[");
      assertTrue(true);
    } catch (AozanException e) {
      assertTrue(false);
    }
    assertEquals(10.0, di.getMin(), 0.001);
    assertFalse(di.isMinIncluded());
    assertEquals(20.0, di.getMax(), 0.001);
    assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("]10; 20[");
      assertTrue(false);
    } catch (AozanException e) {
      assertTrue(true);
    }

    try {
      di = new LongInterval(")10, 20(");
      assertTrue(false);
    } catch (AozanException e) {
      assertTrue(true);
    }

  }

}
