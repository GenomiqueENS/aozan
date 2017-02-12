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

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.DemultiplexingCollector;
import fr.ens.biologie.genomique.aozan.collectors.UndeterminedIndexesCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * This class define a recoverable passing filter clusters percent test for
 * samples.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class RecoverableRawClusterPercentSampleTest extends AbstractSampleTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(UndeterminedIndexesCollector.COLLECTOR_NAME,
        DemultiplexingCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int read,
      final int readSample, final int sampleId) {

    final boolean undetermined = data.isUndeterminedSample(sampleId);
    final int lane = data.getSampleLane(sampleId);

    String recoveryCountKey;
    String sampleCountKey;

    if (!data.isLaneIndexed(lane)) {
      return new TestResult("NA");
    }

    if (undetermined) {
      // Undetermined case
      recoveryCountKey =
          "undeterminedindices.lane" + lane + ".recoverable.raw.cluster.count";
    } else {

      // Case sample
      recoveryCountKey = "undeterminedindices.sample"
          + sampleId + ".recoverable.raw.cluster.count";
    }

    sampleCountKey =
        "demux.sample" + sampleId + ".read" + readSample + ".raw.cluster.count";

    try {
      final long recoveryCount;

      if (data.get(recoveryCountKey) == null) {
        recoveryCount = 0;
      } else {
        recoveryCount = data.getLong(recoveryCountKey);
      }

      final long sampleCount = data.getLong(sampleCountKey);
      final double percent = (double) recoveryCount / (double) sampleCount;

      if (interval == null || undetermined)
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
  public List<AozanTest> configure(final TestConfiguration conf)
      throws AozanException {

    if (conf == null)
      throw new NullPointerException("The conf object is null");

    this.interval.configureDoubleInterval(conf);

    return Collections.singletonList((AozanTest) this);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public RecoverableRawClusterPercentSampleTest() {

    super("sample.recoverable.raw.cluster.percent", "Recoverable Raw Cluster",
        "Recoverable Raw Cluster", "%");
  }

}
