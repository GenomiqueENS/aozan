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
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.UndeterminedIndexesCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * This class define a recoverable passing filter clusters percent test for
 * samples.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class RecoverablePFClusterPercentSampleTest extends AbstractSampleTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(UndeterminedIndexesCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int read,
      final int readSample, final int lane, final String sampleName) {
    
    String recoveryCountKey;
    String sampleCountKey;

    if (!data.isLaneIndexed(lane)) {
      return new TestResult("NA");
    }

    
    if (sampleName == null) {
      // Case undetermined
      recoveryCountKey =
          "undeterminedindices.lane" + lane + ".recoverable.pf.cluster.count";

      sampleCountKey =
          "demux.lane"
              + lane + ".sample.lane" + lane + ".read" + readSample
              + ".pf.cluster.count";

    } else {

      // Case sample
      recoveryCountKey =
          "undeterminedindices.lane"
              + lane + ".sample." + sampleName
              + ".recoverable.pf.cluster.count";

      sampleCountKey =
          "demux.lane"
              + lane + ".sample." + sampleName + ".read" + readSample
              + ".pf.cluster.count";
    }

    try {
      final long recoveryCount;

      if (data.get(recoveryCountKey) == null) {
        recoveryCount = 0;
      } else {
        recoveryCount = data.getLong(recoveryCountKey);
      }

      final long sampleCount = data.getLong(sampleCountKey);
      final double percent = (double) recoveryCount / (double) sampleCount;

      if (interval == null || sampleName == null)
        return new TestResult(percent, true);

      return new TestResult(this.interval.getScore(percent), percent, true);

    } catch (NumberFormatException e) {

      return new TestResult("NA");
    }
  }

  //
  // Other methods
  //

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null)
      throw new NullPointerException("The properties object is null");

    this.interval.configureDoubleInterval(properties);

    return Collections.singletonList((AozanTest) this);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public RecoverablePFClusterPercentSampleTest() {
    super("recoverablepfclusterssamplespercent", "", "Recoverable PF clusters", "%");
  }

}
