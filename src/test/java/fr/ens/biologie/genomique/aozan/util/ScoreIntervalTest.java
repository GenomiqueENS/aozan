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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import org.junit.Assert;

public class ScoreIntervalTest {

  @Test
  public void testSetInterval() {

    ScoreInterval si = new ScoreInterval();

    Assert.assertEquals(-1, si.getScore(0));
    Assert.assertEquals(-1, si.getScore(0.0));
    Assert.assertEquals(-1, si.getScore(Double.NaN));
    Assert.assertEquals(-1, si.getScore(Double.NEGATIVE_INFINITY));
    Assert.assertEquals(-1, si.getScore(Double.POSITIVE_INFINITY));

    si.setInterval(9, new DoubleInterval(10, 20));
    Assert.assertEquals(0, si.getScore(5));
    Assert.assertEquals(0, si.getScore(9));
    Assert.assertEquals(9, si.getScore(15));
    Assert.assertEquals(0, si.getScore(21));
    Assert.assertEquals(0, si.getScore(25));

    si.setInterval(5, new DoubleInterval(8, 22));
    Assert.assertEquals(0, si.getScore(5));
    Assert.assertEquals(5, si.getScore(9));
    Assert.assertEquals(9, si.getScore(15));
    Assert.assertEquals(5, si.getScore(21));
    Assert.assertEquals(0, si.getScore(25));

    si.setInterval(8, new DoubleInterval(12, 18));
    Assert.assertEquals(0, si.getScore(5));
    Assert.assertEquals(5, si.getScore(9));
    Assert.assertEquals(9, si.getScore(15));
    Assert.assertEquals(5, si.getScore(21));
    Assert.assertEquals(0, si.getScore(25));

    si.setInterval(1, new DoubleInterval(2, 28));
    Assert.assertEquals(1, si.getScore(5));
    Assert.assertEquals(5, si.getScore(9));
    Assert.assertEquals(9, si.getScore(15));
    Assert.assertEquals(5, si.getScore(21));
    Assert.assertEquals(1, si.getScore(25));

    si.setInterval(9, new DoubleInterval(16, 17));
    Assert.assertEquals(1, si.getScore(5));
    Assert.assertEquals(5, si.getScore(9));
    Assert.assertEquals(8, si.getScore(15));
    Assert.assertEquals(9, si.getScore(16));
    Assert.assertEquals(5, si.getScore(21));
    Assert.assertEquals(1, si.getScore(25));

    try {
      si.setInterval(0, new DoubleInterval(12, 18));
      Assert.assertTrue(false);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(true);
    }

    try {
      si.setInterval(10, new DoubleInterval(12, 18));
      Assert.assertTrue(false);
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(true);
    }

  }

  @Test
  public void testSetIntervalConstructor() {

    Map<String, String> map = new HashMap<>();
    map.put("score9.interval", "[10,20]");
    map.put("score5.interval", "[8,22]");
    map.put("score8.interval", "[12,18]");
    map.put("score1.interval", "[2,28]");
    map.put("score9.interval", "[16,17]");

    ScoreInterval si = new ScoreInterval();
    try {
      si.configureDoubleInterval(new TestConfiguration(map));
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }

    Assert.assertEquals(1, si.getScore(5));
    Assert.assertEquals(5, si.getScore(9));
    Assert.assertEquals(8, si.getScore(15));
    Assert.assertEquals(9, si.getScore(16));
    Assert.assertEquals(5, si.getScore(21));
    Assert.assertEquals(1, si.getScore(25));

    map = new HashMap<>();
    map.put("interval", "[10,20]");

    si = new ScoreInterval();
    try {
      si.configureDoubleInterval(new TestConfiguration(map));
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }

    Assert.assertEquals(0, si.getScore(5));
    Assert.assertEquals(0, si.getScore(9));
    Assert.assertEquals(9, si.getScore(15));
    Assert.assertEquals(0, si.getScore(21));
    Assert.assertEquals(0, si.getScore(25));

    map = new HashMap<>();
    map.put("toto", "[10,20]");

    si = new ScoreInterval();
    try {
      si.configureDoubleInterval(new TestConfiguration(map));
      Assert.assertTrue(true);
    } catch (AozanException e) {
      Assert.assertTrue(false);
    }

    Assert.assertEquals(-1, si.getScore(5));
    Assert.assertEquals(-1, si.getScore(9));
    Assert.assertEquals(-1, si.getScore(15));
    Assert.assertEquals(-1, si.getScore(21));
    Assert.assertEquals(-1, si.getScore(25));
  }
}
