package fr.ens.biologie.genomique.aozan.collectors.interop;

import static fr.ens.biologie.genomique.aozan.collectors.ReadCollector.READ_DATA_PREFIX;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.util.StatisticsUtils;

public class TileMetricsVersion3PerLaneStatistics {

  private final int laneNumber;
  private final int readCount;
  private final int countTiles;
  private final double densityRatio;

  private final Map<Long, Float> clusterCountMap;
  private final Map<Long, Float> pfClusterCountMap;
  private final Map<Long, Long> readNumberMap;
  private final Map<Long, Float> prcAlignedMap;

  private RunData data;

  /**
   * Save a record from TileMetricsOut.bin file.
   * @param itm illumina tile metrics
   */
  public void addMetric(final TileMetricsVersion3 itm) {

    requireNonNull(itm);

    long tileNumber = itm.getTileNumber();

    switch (itm.getType()) {

    case TILE:

      this.clusterCountMap.put(tileNumber, itm.getClusterCount());
      this.pfClusterCountMap.put(tileNumber, itm.getPfClusterCount());
      break;

    case READ:

      this.readNumberMap.put(tileNumber, itm.getReadNumber());
      this.prcAlignedMap.put(tileNumber, itm.getPfClusterCount());
      break;

    case ZERO:
      break;

    default:
      throw new IllegalStateException();
    }

  }

  /**
   * Compute mean and standard deviation from metrics reading in
   * TileMetricsOut.bin file.
   */
  public void computeData() {

    RunData data = new RunData();

    List<Float> prcPfClusters = new ArrayList<>(this.pfClusterCountMap.size());

    for (Map.Entry<Long, Float> e : this.pfClusterCountMap.entrySet()) {

      if (this.clusterCountMap.containsKey(e.getKey())) {
        prcPfClusters.add(e.getValue() / this.clusterCountMap.get(e.getKey()));
      }
    }

    for (int i = 0; i <= this.readCount; i++) {

      final String key =
          READ_DATA_PREFIX + ".read" + i + ".lane" + this.laneNumber;

      // Same values for all read in a lane, values for one tile

      StatisticsUtils pfClusterCountStats =
          new StatisticsUtils(this.pfClusterCountMap.values());
      data.put(key + ".clusters.pf", pfClusterCountStats.getMean().longValue());
      data.put(key + ".clusters.pf.sd",
          pfClusterCountStats.getStandardDeviation());

      StatisticsUtils clusterCountStats =
          new StatisticsUtils(this.clusterCountMap.values());
      data.put(key + ".clusters.raw", clusterCountStats.getMean().longValue());
      data.put(key + ".clusters.raw.sd",
          clusterCountStats.getStandardDeviation());

      StatisticsUtils prcPfClustersStats = new StatisticsUtils(prcPfClusters);
      data.put(key + ".prc.pf.clusters",
          prcPfClustersStats.getMean().longValue());
      data.put(key + ".prc.pf.clusters.sd",
          prcPfClustersStats.getStandardDeviation());

      data.put(key + ".tile.count", this.countTiles);

      // Specific value of align on phix for each read
      StatisticsUtils prcAlignedStats =
          new StatisticsUtils(this.prcAlignedMap.values());
      data.put(key + ".prc.align", prcAlignedStats.getMean());
      data.put(key + ".prc.align.sd", prcAlignedStats.getStandardDeviation());

      // data.put(key + ".phasing", rm.getPhasing());
      // data.put(key + ".prephasing", rm.getPrephasing());

      data.put(READ_DATA_PREFIX + ".read" + i + ".density.ratio",
          this.densityRatio);

      final String s = data.isReadIndexed(i) ? "(Index)" : "";
      data.put(READ_DATA_PREFIX + ".read" + i + ".type", s);
    }

    this.data = data;
  }

  /**
   * Save data from tile metrics for a run in a RunData.
   * @return rundata data from tile metrics for a run
   */
  public RunData getRunData() {

    if (this.data == null) {
      computeData();
    }

    return this.data;
  }

  //
  // Constructor
  //

  TileMetricsVersion3PerLaneStatistics(final int laneNumber,
      final int readsCount, final int countTiles, final double densityRatio) {

    this.laneNumber = laneNumber;
    this.countTiles = countTiles;
    this.densityRatio = densityRatio;
    this.readCount = readsCount;

    this.clusterCountMap = new HashMap<>(countTiles);
    this.pfClusterCountMap = new HashMap<>(countTiles);
    this.readNumberMap = new HashMap<>(countTiles);
    this.prcAlignedMap = new HashMap<>(countTiles);
  }
}
