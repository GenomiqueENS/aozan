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
package fr.ens.biologie.genomique.aozan.tests.lane;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.DesignCollector;
import fr.ens.biologie.genomique.aozan.collectors.ReadCollector;
import fr.ens.biologie.genomique.aozan.tests.AozanTest;
import fr.ens.biologie.genomique.aozan.tests.TestResult;
import fr.ens.biologie.genomique.aozan.util.ScoreInterval;

/**
 * This class define a lane test on PhiX raw cluster count.
 * @since 1.3
 * @author Sandrine Perrin
 */
public class RawClustersPhixLaneTest extends AbstractLaneTest {

  private final ScoreInterval interval = new ScoreInterval();

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ReadCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME);
  }

  @Override
  public TestResult test(final RunData data, final int read,
      final boolean indexedRead, final int lane) {

    try {

      final double alignPhixPrc = data.getReadPrcAlign(lane, read) / 100.0;

      // Tiles count in run
      final int tiles = data.getTilesCount();

      final long rawClusterCount =
          data.getReadRawClusterCount(lane, read) * tiles;

      // Compute raw cluster corresponding to percent Phix
      final int rawClusterPhix = (int) (rawClusterCount * alignPhixPrc);

      final List<String> sampleNames = data.getSamplesNameListInLane(lane);

      final boolean control =
          sampleNames.size() == 1
              && data.isLaneControl(lane, sampleNames.get(0));

      // No score for indexed read
      if (indexedRead || !control)
        return new TestResult(rawClusterPhix, false);

      return new TestResult(this.interval.getScore(rawClusterPhix),
          rawClusterPhix, false);

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
  public RawClustersPhixLaneTest() {

    super("rawclusterphix", "", "PhiX raw cluster Est.");
  }

}
