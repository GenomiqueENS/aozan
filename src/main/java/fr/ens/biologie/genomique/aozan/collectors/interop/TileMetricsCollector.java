/*
 *                 Aozan development code
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

package fr.ens.biologie.genomique.aozan.collectors.interop;

import static fr.ens.biologie.genomique.aozan.collectors.ReadCollector.READ_DATA_PREFIX;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.util.StatisticsUtils;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.illumina.interop.ExtendedTileMetric;
import fr.ens.biologie.genomique.kenetre.illumina.interop.ExtendedTileMetricsReader;
import fr.ens.biologie.genomique.kenetre.illumina.interop.TileMetric;
import fr.ens.biologie.genomique.kenetre.illumina.interop.TileMetricsReader;

/**
 * This class collects run data by reading the TileMetricsOut.bin in InterOp
 * directory. The value are the same for all read in a lane.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class TileMetricsCollector extends AbstractMetricsCollector {

  /** The sub-collector name from ReadCollector. */
  public static final String COLLECTOR_NAME = "TileMetricsCollector";

  private double densityRatio = 0.0;

  private class TileMetricsPerLaneStatistics {

    private final int laneNumber;
    private final int tileCount;
    private final int readCount;

    private final Map<Long, Float> clusterCountMap = new HashMap<>();
    private final List<Float> clusterCountPFList = new ArrayList<>();
    private final List<Float> clusterCountPFRatioList = new ArrayList<>();
    private final List<Float> densityList = new ArrayList<>();
    private final List<Float> densityPFList = new ArrayList<>();
    private final List<Float> densityPFRatioList = new ArrayList<>();
    private final Map<Integer, List<Float>> alignedMap = new HashMap<>();
    private final Map<Integer, List<Float>> prephasingMap = new HashMap<>();
    private final Map<Integer, List<Float>> phasingMap = new HashMap<>();
    private final List<Float> percentOccupiedList = new ArrayList<>();

    void addMetric(TileMetric tm) {

      this.clusterCountMap.put(tm.getTileNumber(), tm.getClusterCount());
      this.clusterCountPFList.add(tm.getClusterCountPF());
      this.clusterCountPFRatioList
          .add(tm.getClusterCountPF() / tm.getClusterCount() * 100.0f);
      this.densityList.add(tm.getClusterDensity());
      this.densityPFList.add(tm.getClusterDensityPF());
      this.densityPFRatioList
          .add(tm.getClusterDensityPF() / tm.getClusterDensity());

      for (int readNumber = 1; readNumber <= tm.getReadCount(); readNumber++) {

        if (!this.alignedMap.containsKey(readNumber)) {
          this.alignedMap.put(readNumber, new ArrayList<>());
          this.prephasingMap.put(readNumber, new ArrayList<>());
          this.phasingMap.put(readNumber, new ArrayList<>());
        }

        int i = readNumber - 1;

        this.alignedMap.get(readNumber).add(tm.getPercentAligned(i));
        this.prephasingMap.get(readNumber).add(tm.getPercentPrephasing(i));
        this.phasingMap.get(readNumber).add(tm.getPercentPhasing(i));
      }
    }

    void addMetric(ExtendedTileMetric etm) {

      float clusterCount = this.clusterCountMap.get(etm.getTileNumber());
      float clusterCountOccupied = etm.getClusterCountOccupied();

      this.percentOccupiedList.add(clusterCountOccupied / clusterCount);
    }

    private void meanAndSD(String key, Collection<Float> c, RunData data) {
      meanAndSD(key, c, data, false);
    }

    private void meanAndSD(String key, Collection<Float> c, RunData data,
        boolean intValue) {

      StatisticsUtils stat = new StatisticsUtils(c);

      if (intValue) {
        data.put(key, stat.getMean().longValue());
      } else {
        data.put(key, stat.getMean());
      }
      data.put(key + ".sd", stat.getStandardDeviation());
    }

    private void mean(String key, Collection<Float> c, RunData data) {

      StatisticsUtils stat = new StatisticsUtils(c);
      data.put(key, stat.getMean());
    }

    /**
     * Save data from tile metrics for a run in a RunData.
     * @return rundata data from tile metrics for a run
     */
    public RunData getRunData() {

      final RunData data = new RunData();

      for (int readNumber = 1; readNumber <= this.readCount; readNumber++) {

        final String key =
            READ_DATA_PREFIX + ".read" + readNumber + ".lane" + this.laneNumber;

        // Same values for all read in a lane, values for one tile
        meanAndSD(key + ".clusters.raw", this.clusterCountMap.values(), data,
            true);
        meanAndSD(key + ".clusters.pf", this.clusterCountPFList, data, true);
        meanAndSD(key + ".prc.pf.clusters", this.clusterCountPFRatioList, data);

        meanAndSD(key + ".density.raw", this.densityList, data);
        meanAndSD(key + ".density.pf", this.densityPFList, data);
        meanAndSD(key + ".density.ratio", this.densityPFRatioList, data);

        data.put(key + ".tile.count", this.tileCount);

        // Specific value of align on phix for each read
        if (this.alignedMap.containsKey(readNumber)) {
          meanAndSD(key + ".prc.align", this.alignedMap.get(readNumber), data);
          mean(key + ".phasing", this.phasingMap.get(readNumber), data);
          mean(key + ".prephasing", this.prephasingMap.get(readNumber), data);
        } else {
          data.put(key + ".prc.align", 0);
          data.put(key + ".prc.align.sd", 0);
          data.put(key + ".phasing", Float.NaN);
          data.put(key + ".prephasing", Float.NaN);
        }

        // Percent occupied
        if (!this.percentOccupiedList.isEmpty()) {
          meanAndSD(key + ".prc.occupied", this.percentOccupiedList, data);
        } else {
          data.put(key + ".prc.occupied", Float.NaN);
          data.put(key + ".prc.occupied.sd", Float.NaN);
        }

        final String s = data.isReadIndexed(readNumber) ? "(Index)" : "";
        data.put(READ_DATA_PREFIX + ".read" + readNumber + ".type", s);

      }

      return data;
    }

    TileMetricsPerLaneStatistics(final int laneNumber, final int readCount,
        final int tileCount, final double densityRatio) {

      this.laneNumber = laneNumber;
      this.tileCount = tileCount;
      this.readCount = readCount;
    }

  }

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collect data from TileMetric interOpFile.
   * @param data result data object
   */
  @Override
  public void collect(final RunData data) throws AozanException {

    super.collect(data);

    Map<Integer, TileMetricsPerLaneStatistics> tileMetrics = null;
    boolean first = true;

    // Parse TileMetrics file
    try {
      for (TileMetric tm : new TileMetricsReader(getInterOpDir())
          .readMetrics()) {

        if (first) {
          this.densityRatio = tm.getClusterDensity();
          tileMetrics = initMetricsMap(data);
          first = false;
        }

        tileMetrics.get(tm.getLaneNumber()).addMetric(tm);
      }
    } catch (KenetreException e) {
      throw new AozanException(e);
    }

    // Parse ExtendedTileMetrics
    if (new File(getInterOpDir(), ExtendedTileMetricsReader.METRICS_FILE)
        .exists()) {

      try {
        for (ExtendedTileMetric tm : new ExtendedTileMetricsReader(
            getInterOpDir()).readMetrics()) {

          tileMetrics.get(tm.getLaneNumber()).addMetric(tm);
        }
      } catch (KenetreException e) {
        throw new AozanException(e);
      }
    }

    // Build runData
    for (final TileMetricsPerLaneStatistics value : tileMetrics.values()) {

      // value.computeData();
      data.put(value.getRunData());
    }
  }

  /**
   * Initialize TileMetrics map.
   * @param data result data object
   */
  private Map<Integer, TileMetricsPerLaneStatistics> initMetricsMap(
      final RunData data) {

    final Map<Integer, TileMetricsPerLaneStatistics> result = new HashMap<>();

    final int tilesCount = computeTilesCount(data);

    final int lanesCount = data.getLaneCount();
    final int readsCount = data.getReadCount();

    for (int lane = 1; lane <= lanesCount; lane++) {
      result.put(lane, new TileMetricsPerLaneStatistics(lane, readsCount,
          tilesCount, this.densityRatio));
    }

    return result;
  }

  //
  // Getters
  //

  /**
   * Gets the tiles count.
   * @return the tiles count
   */
  private int computeTilesCount(final RunData data) {

    if (data.contains("run.info.flow.cell.section.per.lane")
        && data.getInt("run.info.flow.cell.section.per.lane") > 0) {

      // NextSeq case, compute tile count and add data from camera number
      return data.getInt("run.info.flow.cell.tile.count")
          * data.getInt("run.info.flow.cell.surface.count")
          * data.getInt("run.info.flow.cell.swath.count")
          * data.getInt("run.info.flow.cell.section.per.lane");
    }

    return data.getInt("run.info.flow.cell.tile.count")
        * data.getInt("run.info.flow.cell.surface.count")
        * data.getInt("run.info.flow.cell.swath.count");
  }

  //
  // Internal class
  //

}