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
import org.junit.Assert;
import org.junit.Test;

public class DoubleIntervalTest {

  @Test
  public void testIsInInterval() {

    DoubleInterval di = new DoubleInterval(10.0, 20.0);
    Assert.assertFalse(di.isInInterval(Double.NaN));
    Assert.assertFalse(di.isInInterval(Double.NEGATIVE_INFINITY));
    Assert.assertFalse(di.isInInterval(Double.POSITIVE_INFINITY));
    Assert.assertFalse(di.isInInterval(5.0));
    Assert.assertFalse(di.isInInterval(9.99999));
    Assert.assertTrue(di.isInInterval(10));
    Assert.assertTrue(di.isInInterval(10.0));
    Assert.assertTrue(di.isInInterval(15.0));
    Assert.assertTrue(di.isInInterval(20.0));
    Assert.assertTrue(di.isInInterval(20));
    Assert.assertFalse(di.isInInterval(20.00001));
    Assert.assertFalse(di.isInInterval(25.0));

    di = new DoubleInterval(Double.NEGATIVE_INFINITY, 20.0);
    Assert.assertFalse(di.isInInterval(Double.NaN));
    Assert.assertTrue(di.isInInterval(Double.NEGATIVE_INFINITY));
    Assert.assertTrue(di.isInInterval(Double.MIN_NORMAL));
    Assert.assertTrue(di.isInInterval(Double.MIN_VALUE));
    Assert.assertFalse(di.isInInterval(Double.POSITIVE_INFINITY));
    Assert.assertTrue(di.isInInterval(10));

    di = new DoubleInterval(Double.NEGATIVE_INFINITY, 20.0);
    Assert.assertFalse(di.isInInterval(Double.NaN));
    Assert.assertTrue(di.isInInterval(Double.NEGATIVE_INFINITY));
    Assert.assertTrue(di.isInInterval(Double.MIN_NORMAL));
    Assert.assertTrue(di.isInInterval(Double.MIN_VALUE));
    Assert.assertFalse(di.isInInterval(Double.POSITIVE_INFINITY));
    Assert.assertTrue(di.isInInterval(10));
    Assert.assertFalse(di.isInInterval(25.0));

    di = new DoubleInterval(10.0, Double.POSITIVE_INFINITY);
    Assert.assertFalse(di.isInInterval(Double.NaN));
    Assert.assertFalse(di.isInInterval(Double.NEGATIVE_INFINITY));
    Assert.assertFalse(di.isInInterval(5.0));
    Assert.assertTrue(di.isInInterval(10));
    Assert.assertTrue(di.isInInterval(25.0));
    Assert.assertTrue(di.isInInterval(Double.MAX_VALUE));
    Assert.assertTrue(di.isInInterval(Double.POSITIVE_INFINITY));
  }

  @Test
  public void testDoubleIntervalDoubleDouble() {

    DoubleInterval di = new DoubleInterval(10.0, 20.0);
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    di = new DoubleInterval(20.0, 10.0);
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertEquals(20.0, di.getMax(), 0.001);

    di = new DoubleInterval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    Assert.assertEquals(Double.NEGATIVE_INFINITY, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(Double.POSITIVE_INFINITY, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    di = new DoubleInterval(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    Assert.assertEquals(Double.NEGATIVE_INFINITY, di.getMin(), 0.001);
    Assert.assertEquals(Double.POSITIVE_INFINITY, di.getMax(), 0.001);

    try {
      di = new DoubleInterval(Double.NaN, 20.0);
      Assert.assertTrue(false);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(true);
    }

  }

  @Test
  public void testDoubleIntervalDoubleBooleanDoubleBoolean() {

    DoubleInterval di = new DoubleInterval(10.0, true, 20.0, true);
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    di = new DoubleInterval(10.0, false, 20.0, true);
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    di = new DoubleInterval(10.0, true, 20.0, false);
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    di = new DoubleInterval(10.0, false, 20.0, false);
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());
  }

  @Test
  public void testDoubleIntervalString() {

    DoubleInterval di = null;
    try {
      di = new DoubleInterval("[10.0, 20.0]");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    try {
      di = new DoubleInterval("]10.0, 20.0]");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    try {
      di = new DoubleInterval("(10.0, 20.0]");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertTrue(di.isMaxIncluded());

    try {
      di = new DoubleInterval("[10.0, 20.0[");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("[10.0, 20.0)");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertTrue(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("]10.0, 20.0[");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("(10.0, 20.0)");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("]10,20[");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("]10,  20[");
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }
    Assert.assertEquals(10.0, di.getMin(), 0.001);
    Assert.assertFalse(di.isMinIncluded());
    Assert.assertEquals(20.0, di.getMax(), 0.001);
    Assert.assertFalse(di.isMaxIncluded());

    try {
      di = new DoubleInterval("]10; 20[");
      Assert.assertTrue(false);
    } catch (AozanException e) {
      Assert.assertTrue(true);
    }

    try {
      di = new DoubleInterval(")10, 20(");
      Assert.assertTrue(false);
    } catch (AozanException e) {
      Assert.assertTrue(true);
    }

  }

}
