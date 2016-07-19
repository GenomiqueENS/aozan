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

package fr.ens.biologie.genomique.aozan.util;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.util.LongInterval;
import junit.framework.Assert;
import org.junit.Test;

public class LongIntervalTest {

  @Test
  public void testIsInInterval() {

    LongInterval di = new LongInterval(10, 20);
    Assert.assertFalse(di.isInInterval(Long.MIN_VALUE));
    Assert.assertFalse(di.isInInterval(Long.MAX_VALUE));
    Assert.assertFalse(di.isInInterval(5));
    Assert.assertFalse(di.isInInterval(9));
    Assert.assertTrue(di.isInInterval(10));
    Assert.assertTrue(di.isInInterval(15));
    Assert.assertTrue(di.isInInterval(20));
    Assert.assertTrue(di.isInInterval(20));
    Assert.assertFalse(di.isInInterval(25));

    di = new LongInterval(Long.MIN_VALUE, 20);
    Assert.assertTrue(di.isInInterval(Long.MIN_VALUE));
    Assert.assertFalse(di.isInInterval(Long.MAX_VALUE));
    Assert.assertTrue(di.isInInterval(10));

    di = new LongInterval(Long.MIN_VALUE, 20);
    Assert.assertTrue(di.isInInterval(Long.MIN_VALUE));
    Assert.assertFalse(di.isInInterval(Long.MAX_VALUE));
    Assert.assertTrue(di.isInInterval(10));
    Assert.assertFalse(di.isInInterval(25));

    di = new LongInterval(10, Long.MAX_VALUE);
    Assert.assertFalse(di.isInInterval(5));
    Assert.assertTrue(di.isInInterval(10));
    Assert.assertTrue(di.isInInterval(25));
    Assert.assertTrue(di.isInInterval(Long.MAX_VALUE));

  }

  @Test
  public void testLongIntervalDoubleDouble() {

    LongInterval di = new LongInterval(10, 20);
    Assert.assertEquals(10, di.getMin());
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20, di.getMax());
    Assert.assertTrue(di.isMaxIncluded());

    di = new LongInterval(20, 10);
    Assert.assertEquals(10, di.getMin());
    Assert.assertEquals(20, di.getMax());

    di = new LongInterval(Long.MIN_VALUE, Long.MAX_VALUE);
    Assert.assertEquals(Long.MIN_VALUE, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(Long.MAX_VALUE, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    di = new LongInterval(Long.MAX_VALUE, Long.MIN_VALUE);
    Assert.assertEquals(Long.MIN_VALUE, di.getMin(), 0.001);
    Assert.assertEquals(Long.MAX_VALUE, di.getMax(), 0.001);

  }

  @Test
  public void testLongIntervalDoubleBooleanDoubleBoolean() {

    LongInterval di = new LongInterval(10, true, 20, true);
    Assert.assertEquals(10, di.getMin());
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20, di.getMax());
    Assert.assertTrue(di.isMaxIncluded());

    di = new LongInterval(10, false, 20, true);
    Assert.assertEquals(10, di.getMin());
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20, di.getMax());
    Assert.assertTrue(di.isMaxIncluded());

    di = new LongInterval(10, true, 20, false);
    Assert.assertEquals(10, di.getMin());
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20, di.getMax());
    Assert.assertFalse(di.isMaxIncluded());

    di = new LongInterval(10, false, 20, false);
    Assert.assertEquals(10, di.getMin());
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20, di.getMax());
    Assert.assertFalse(di.isMaxIncluded());
  }

  @Test
  public void testLongIntervalString() {

    LongInterval di = null;
    try {
      di = new LongInterval("[10, 20]");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    try {
      di = new LongInterval("]10, 20]");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    try {
      di = new LongInterval("(10, 20]");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    try {
      di = new LongInterval("[10, 20[");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("[10, 20)");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("]10, 20[");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("(10, 20)");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("]10,20[");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("]10,  20[");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new LongInterval("]10; 20[");
      Assert.assertTrue(false);
    } catch (AozanException e) {
      Assert.assertTrue(true);
    }

    try {
      di = new LongInterval(")10, 20(");
      Assert.assertTrue(false);
    } catch (AozanException e) {
      Assert.assertTrue(true);
    }

  }

}
