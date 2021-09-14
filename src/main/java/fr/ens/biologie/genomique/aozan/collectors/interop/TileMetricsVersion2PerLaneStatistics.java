package fr.ens.biologie.genomique.aozan.collectors.interop;

import static fr.ens.biologie.genomique.aozan.collectors.ReadCollector.READ_DATA_PREFIX;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.util.StatisticsUtils;

/**
 * This class contains all tile metrics for a lane extracted from binary file
 * (TileMetricsOut.bin in InterOp directory).
 * @author Sandrine Perrin
 * @since 1.1
 */
public final class TileMetricsVersion2PerLaneStatistics {

  /** Set code used for TileMetrics. */
  private static final int CLUSTER_DENSITY_CODE = 100;
  private static final int CLUSTER_DENSITY_PF_CODE = 101;
  private static final int NUMBER_CLUSTER_CODE = 102;
  private static final int NUMBER_CLUSTER_PF_CODE = 103;
  private static final int CONTROL_LANE = 400;

  // code set for read1 and used to compute code for other reads
  private static final int PHASING_CODE = 200;
  private static final int PREPHASING_CODE = 201;
  private static final int PCR_ALIGNED_PHIX_CODE = 300;

  private final int laneNumber;
  private final int countTiles;
  private final double densityRatio;

  private double clusterDensity = 0.0; // code in binary file 100
  private double clusterDensityPF = 0.0; // code in binary file 101
  private long numberCluster = 0L; // code in binary file 102
  private long numberClusterPF = 0L; // code in binary file 103
  private double prcPFClusters = 0.0;
  @SuppressWarnings("unused")
  private Double controlLane = 0.0; // code in binary file 400

  private final Map<Integer, Map<Integer, Double>> metricsPerTilePerCode;

  // Standard deviation
  private double clusterDensitySD = 0.0;
  private double clusterDensityPFSD = 0.0;
  private double numberClusterSD = 0.0;
  private double numberClusterPFSD = 0.0;
  private double prcPFClustersSD = 0.0;

  // All metrics per read
  private final List<TileMetricsVersion2PerLaneStatistics.ReadTileMetrics> listReads;
  private boolean dataToCompute = true;

  /**
   * Save a record from TileMetricsOut.bin file.
   * @param itm illumina tile metrics
   */
  public void addMetric(final TileMetricsVersion2 itm) {
    final int tileNumber = itm.getTileNumber();
    final int code = itm.getMetricCode();
    final double value = itm.getMetricValue();

    if (this.metricsPerTilePerCode.containsKey(code)) {

      // One value by tile by code
      if (!this.metricsPerTilePerCode.get(code).containsKey(tileNumber)) {
        this.metricsPerTilePerCode.get(code).put(tileNumber, value);

      } else if (this.metricsPerTilePerCode.get(code).get(tileNumber) == 0.0) {

        // Replace value by numeric in case several value exist per tile
        this.metricsPerTilePerCode.get(code).put(tileNumber, value);
      }

    } else {
      final Map<Integer, Double> m = new TreeMap<>();
      m.put(tileNumber, value);
      this.metricsPerTilePerCode.put(code, m);
    }
  }

  /**
   * Compute mean and standard deviation from metrics reading in
   * TileMetricsOut.bin file.
   */
  public void computeData() {

    StatisticsUtils stat;
    Number mean;
    Number sd;

    // Compute statistics for each code
    for (final Map.Entry<Integer, Map<Integer, Double>> entry : this.metricsPerTilePerCode
        .entrySet()) {

      // list values for the code metric
      final List<Double> listValues =
          Lists.newArrayList((entry.getValue().values()));

      // compute mean and standard deviation
      stat = new StatisticsUtils(listValues);
      mean = stat.getMean();
      sd = stat.getStandardDeviation();

      switch (entry.getKey()) {
      case CLUSTER_DENSITY_CODE:
        this.clusterDensity = mean.doubleValue();
        this.clusterDensitySD = sd.doubleValue();
        break;

      case CLUSTER_DENSITY_PF_CODE:
        this.clusterDensityPF = mean.doubleValue();
        this.clusterDensityPFSD = sd.doubleValue();
        break;

      case NUMBER_CLUSTER_CODE:
        this.numberCluster = mean.longValue();
        this.numberClusterSD = sd.doubleValue();
        break;

      case NUMBER_CLUSTER_PF_CODE:
        this.numberClusterPF = mean.longValue();
        this.numberClusterPFSD = sd.doubleValue();
        break;

      case CONTROL_LANE:
        // value unique to a run, read first value in list
        this.controlLane = mean.doubleValue();
        break;

      default: // code 20X and 30X
        addReadMetrics(entry.getKey(), stat);
      }
    }

    computePercentClusterPF();

    this.dataToCompute = false;
  }

  /**
   * Compute mean and standard deviation for percent cluster PF.
   */
  private void computePercentClusterPF() {

    final Map<Integer, Double> numberClusterValues =
        this.metricsPerTilePerCode.get(102);
    final Map<Integer, Double> numberClusterPFValues =
        this.metricsPerTilePerCode.get(103);

    if (numberClusterPFValues.size() == 0 || numberClusterValues.size() == 0) {
      return;
    }

    final StatisticsUtils stat = new StatisticsUtils();

    // Set the percent cluster PF for each tile
    for (final Map.Entry<Integer, Double> clusters : numberClusterValues
        .entrySet()) {

      for (final Map.Entry<Integer, Double> clustersPF : numberClusterPFValues
          .entrySet()) {

        // For each tile
        if (clusters.getKey().equals(clustersPF.getKey())) {

          final Double prc = clustersPF.getValue() / clusters.getValue();

          stat.addValues(prc);

          break;
        }
      }
    }

    this.prcPFClusters = stat.getMean() * 100;
    this.prcPFClustersSD = stat.getStandardDeviation() * 100;

  }

  /**
   * Add a metric (mean and standard deviation) to a read from the code metric.
   * @param code metric
   * @param stat object contains all metrics
   */
  private void addReadMetrics(final int code, final StatisticsUtils stat) {

    int numeroRead = -1;

    // code >= 300
    if (code >= PCR_ALIGNED_PHIX_CODE) {
      numeroRead = code - PCR_ALIGNED_PHIX_CODE + 1;
    } else {
      // code pair and < 300
      if (code % 2 == 0) {
        numeroRead = (code - PHASING_CODE) / 2 + 1;
      } else {
        // code impair and < 300
        numeroRead = (code - PREPHASING_CODE) / 2 + 1;
      }
    }

    // update ReadMetrics for the read number if it exists
    for (final TileMetricsVersion2PerLaneStatistics.ReadTileMetrics read : this.listReads) {
      if (read.getNumberRead() == numeroRead) {
        read.addValue(code, stat);
      }
    }
  }

  /**
   * Save data from tile metrics for a run in a RunData.
   * @return rundata data from tile metrics for a run
   */
  public RunData getRunData() {

    if (this.dataToCompute) {
      computeData();
    }

    final RunData data = new RunData();

    for (final TileMetricsVersion2PerLaneStatistics.ReadTileMetrics rm : this.listReads) {

      final String key = READ_DATA_PREFIX
          + ".read" + rm.getNumberRead() + ".lane" + this.laneNumber;

      // Same values for all read in a lane, values for one tile
      data.put(key + ".clusters.pf", this.numberClusterPF);
      data.put(key + ".clusters.pf.sd", this.numberClusterPFSD);

      data.put(key + ".clusters.raw", this.numberCluster);
      data.put(key + ".clusters.raw.sd", this.numberClusterSD);

      data.put(key + ".prc.pf.clusters", this.prcPFClusters);
      data.put(key + ".prc.pf.clusters.sd", this.prcPFClustersSD);

      data.put(key + ".tile.count", this.countTiles);

      // Specific value of align on phix for each read
      data.put(key + ".prc.align", rm.getPercentAlignedPhix());
      data.put(key + ".prc.align.sd", rm.getPercentAlignedPhixSD());

      data.put(key + ".phasing", rm.getPhasing());
      data.put(key + ".prephasing", rm.getPrephasing());

      data.put(
          READ_DATA_PREFIX + ".read" + rm.getNumberRead() + ".density.ratio",
          this.densityRatio);

      final String s = data.isReadIndexed(rm.getNumberRead()) ? "(Index)" : "";
      data.put(READ_DATA_PREFIX + ".read" + rm.getNumberRead() + ".type", s);

    }

    return data;
  }

  @Override
  public String toString() {
    return String.format(
        "Density %.2f\t -PF %.2f\t nbC %s\t PF %s\t prc %.2f\tsd Density %.3f\t -PF %.3f\t nbC %.3f\t PF %.3f\t prc %.3f",
        this.clusterDensity, this.clusterDensityPF, this.numberCluster,
        this.numberClusterPF, this.prcPFClusters, this.clusterDensitySD,
        this.clusterDensityPFSD, this.numberClusterSD, this.numberClusterPFSD,
        this.prcPFClustersSD);
  }

  //
  // Constructor
  //

  TileMetricsVersion2PerLaneStatistics(final int laneNumber,
      final int readsCount, final int countTiles, final double densityRatio) {

    this.laneNumber = laneNumber;
    this.countTiles = countTiles;
    this.densityRatio = densityRatio;

    this.metricsPerTilePerCode = new HashMap<>();
    this.listReads = new LinkedList<>();

    for (int read = 1; read <= readsCount; read++) {
      this.listReads.add(new ReadTileMetrics(read));
    }

  }

  //
  // Inner class
  //

  /**
   * This class contains all tile metrics specific on a read.
   * @author Sandrine Perrin
   * @since 1.1
   */
  private static final class ReadTileMetrics {

    private final int readNumber; // N
    private Double phasing = 0.0; // code in binary file 200+(N-1)*2
    private Double prephasing = 0.0; // code in binary file 201+(N-1)*2
    private Double percentAlignedPhix = 0.0; // code in binary file 300+N-1
    private Double percentAlignedPhixSD = 0.0;

    /**
     * Constructor
     * @param numberRead number read
     */
    ReadTileMetrics(final int numberRead) {
      this.readNumber = numberRead;
    }

    /**
     * Initialize a value according to code.
     * @param code metrics code
     * @param stat object contains all metrics
     */
    public void addValue(final int code, final StatisticsUtils stat) {

      if (code >= PCR_ALIGNED_PHIX_CODE && code < 400) {
        this.percentAlignedPhix = stat.getMean();
        this.percentAlignedPhixSD = stat.getStandardDeviation();

      } else if (code % 2 == 0) {
        // Compute the mediane with using value = 0.0
        this.phasing = stat.getMean() * 100;
      } else {
        this.prephasing = stat.getMean() * 100;
      }
    }

    /**
     * Get the number read.
     * @return number read
     */
    public int getNumberRead() {
      return this.readNumber;
    }

    /**
     * Get the percent of phasing.
     * @return percent of phasing
     */
    public double getPhasing() {
      return this.phasing;
    }

    /**
     * Get the percent of prephasing.
     * @return percent of prephasing
     */
    public double getPrephasing() {
      return this.prephasing;
    }

    /**
     * Get the percentage of countReads passing filter that were uniquely
     * aligned to the reference.
     * @return percent of cluster aligned on Phix
     */
    public double getPercentAlignedPhix() {
      return this.percentAlignedPhix;
    }

    /**
     * Get the standard deviation of countReads passing filter that were
     * uniquely aligned to the reference.
     * @return percent of cluster aligned on Phix
     */
    public double getPercentAlignedPhixSD() {
      return this.percentAlignedPhixSD;
    }
  }
}