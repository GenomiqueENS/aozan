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
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.collectors.interop;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;
import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.util.StatisticsUtils;

/**
 * This class collects run data by reading the TileMetricsOut.bin in InterOp
 * directory. The value are the same for all read in a lane.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class TileMetricsCollector implements Collector {

  /** The sub-collector name from ReadCollector. */
  public static final String NAME_COLLECTOR = "TileMetricsCollector";

  private double densityRatio = 0.0;

  private final Map<Integer, TileMetricsPerLane> tileMetrics = Maps
      .newHashMap();
  private String dirInterOpPath;

  public String getName() {
    return NAME_COLLECTOR;
  }

  /**
   * Get the name of the collectors required to run this collector.
   * @return a list of String with the name of the required collectors
   */
  public List<String> getCollectorsNamesRequiered() {
    return Collections.unmodifiableList(Lists
        .newArrayList(RunInfoCollector.COLLECTOR_NAME));
  }

  /**
   * Configure the collector with the path of the run data
   * @param properties object with the collector configuration
   */
  public void configure(Properties properties) {
    String RTAOutputDirPath = properties.getProperty(QC.RTA_OUTPUT_DIR);
    this.dirInterOpPath = RTAOutputDirPath + "/InterOp/";

    densityRatio =
        Double.parseDouble(properties
            .getProperty("qc.conf.cluster.density.ratio"));
  }

  /**
   * Collect data from TileMetric interOpFile.
   * @param data result data object
   */
  public void collect(final RunData data) throws AozanException {

    TileMetricsReader reader = new TileMetricsReader(dirInterOpPath);
    initMetricsMap(data);

    // Distribution of metrics between lane and code
    for (IlluminaTileMetrics itm : reader.getSetIlluminaMetrics()) {

      tileMetrics.get(itm.getLaneNumber()).addMetric(itm);
    }

    // Build runData
    for (TileMetricsPerLane value : tileMetrics.values()) {

      value.computeData();
      data.put(value.getRunData());
    }
  }

  /**
   * Initialize TileMetrics map.
   * @return map
   */
  private void initMetricsMap(final RunData data) {

    int tilesCount =
        data.getInt("run.info.flow.cell.tile.count")
            * data.getInt("run.info.flow.cell.surface.count")
            * data.getInt("run.info.flow.cell.swath.count");

    int lanesCount = data.getInt("run.info.flow.cell.lane.count");
    int readsCount = data.getInt("run.info.read.count");

    for (int lane = 1; lane <= lanesCount; lane++)
      tileMetrics.put(lane, new TileMetricsPerLane(lane, readsCount,
          tilesCount, this.densityRatio));

  }

  /**
   * Remove temporary files
   */
  public void clear() {
  }

  //
  // Inner class
  //

  /**
   * This class contains all tile metrics for a lane extracted from binary file
   * (TileMetricsOut.bin in InterOp directory).
   * @author Sandrine Perrin
   * @since 1.1
   */
  private static final class TileMetricsPerLane {

    /** Set code used for TileMetrics */
    private static final int CLUSTER_DENSITY_CODE = 100;
    private static final int CLUSTER_DENSITY_PF_CODE = 101;
    private static final int NUMBER_CLUSTER_CODE = 102;
    private static final int NUMBER_CLUSTER_PF_CODE = 103;
    private static final int CONTROL_LANE = 400;

    // code set for read1 and used to compute code for other reads
    private static final int PHASING_CODE = 200;
    private static final int PREPHASING_CODE = 201;
    private static final int PCR_ALIGNED_PHIX_CODE = 300;

    private int laneNumber;
    private int countTiles;
    private double densityRatio;

    private double clusterDensity = 0.0; // code in binary file 100
    private double clusterDensityPF = 0.0; // code in binary file 101
    private long numberCluster = 0L; // code in binary file 102
    private long numberClusterPF = 0L; // code in binary file 103
    private double prcPFClusters = 0.0;
    @SuppressWarnings("unused")
    private Double controlLane = 0.0; // code in binary file 400

    private Map<Integer, Map<Integer, Double>> metricsPerTilePerCode;

    // Standard deviation
    private double clusterDensitySD = 0.0;
    private double clusterDensityPFSD = 0.0;
    private double numberClusterSD = 0.0;
    private double numberClusterPFSD = 0.0;
    private double prcPFClustersSD = 0.0;

    // All metrics per read
    private List<ReadTileMetrics> listReads;
    private boolean dataToCompute = true;

    /**
     * Save a record from TileMetricsOut.bin file.
     * @param itm illumina tile metrics
     */
    public void addMetric(final IlluminaTileMetrics itm) {
      int tileNumber = itm.getTileNumber();
      int code = itm.getMetricCode();
      double value = itm.getMetricValue();

      if (metricsPerTilePerCode.containsKey(code)) {

        // One value by tile by code
        if (!metricsPerTilePerCode.get(code).containsKey(tileNumber)) {
          metricsPerTilePerCode.get(code).put(tileNumber, value);
        }

      } else {
        Map<Integer, Double> m = new TreeMap<Integer, Double>();
        m.put(tileNumber, value);
        metricsPerTilePerCode.put(code, m);
      }
    }

    /**
     * Compute mean and standard deviation from metrics reading in
     * TileMetricsOut.bin file.
     */
    public void computeData() {

      StatisticsUtils stat = null;
      Number mean = null;
      Number sd = null;

      // Compute statistics for each code
      for (Map.Entry<Integer, Map<Integer, Double>> entry : metricsPerTilePerCode
          .entrySet()) {

        // list values for the code metric
        List<Double> listValues =
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

      dataToCompute = false;
    }

    /**
     * Compute mean and standard deviation for percent cluster PF.
     */
    private void computePercentClusterPF() {

      Map<Integer, Double> numberClusterValues = metricsPerTilePerCode.get(102);
      Map<Integer, Double> numberClusterPFValues =
          metricsPerTilePerCode.get(103);

      if (numberClusterPFValues.size() == 0 || numberClusterValues.size() == 0)
        return;

      StatisticsUtils stat = new StatisticsUtils();

      // Set the percent cluster PF for each tile
      for (Map.Entry<Integer, Double> clusters : numberClusterValues.entrySet()) {

        for (Map.Entry<Integer, Double> clustersPF : numberClusterPFValues
            .entrySet()) {

          // For each tile
          if (clusters.getKey().equals(clustersPF.getKey())) {

            Double prc = clustersPF.getValue() / clusters.getValue();

            stat.addValues(prc);

            break;
          }
        }
      }

      this.prcPFClusters = stat.getMean() * 100;
      this.prcPFClustersSD = stat.getStandardDeviation() * 100;

    }

    /**
     * Add a metric (mean and standard deviation) to a read from the code
     * metric.
     * @param code metric
     * @param stat object contains all metrics
     */
    private void addReadMetrics(final int code, final StatisticsUtils stat) {

      int numeroRead = -1;

      // code >= 300
      if (code >= PCR_ALIGNED_PHIX_CODE)
        numeroRead = code - PCR_ALIGNED_PHIX_CODE + 1;
      else {
        // code pair and < 300
        if (code % 2 == 0)
          numeroRead = (code - PHASING_CODE) / 2 + 1;
        else
          // code impair and < 300
          numeroRead = (code - PREPHASING_CODE) / 2 + 1;
      }

      // update ReadMetrics for the read number if it exists
      for (ReadTileMetrics read : listReads) {
        if (read.getNumberRead() == numeroRead) {
          read.addValue(code, stat);
        }
      }
    }

    /**
     * Save data from tile metrics for a run in a RunData
     * @return rundata data from tile metrics for a run
     */
    public RunData getRunData() {

      if (dataToCompute)
        computeData();

      RunData data = new RunData();

      for (ReadTileMetrics rm : listReads) {

        String key = "read" + rm.getNumberRead() + ".lane" + laneNumber;

        // Same values for all read in a lane, values for one tile
        data.put(key + ".clusters.pf", numberClusterPF);
        data.put(key + ".clusters.pf.sd", numberClusterPFSD);

        data.put(key + ".clusters.raw", numberCluster);
        data.put(key + ".clusters.raw.sd", numberClusterSD);

        data.put(key + ".prc.pf.clusters", prcPFClusters);
        data.put(key + ".prc.pf.clusters.sd", prcPFClustersSD);

        data.put(key + ".tile.count", countTiles);

        // Specific value of align on phix for each read
        data.put(key + ".prc.align", rm.getPercentAlignedPhix());
        data.put(key + ".prc.align.sd", rm.getPercentAlignedPhixSD());

        data.put(key + ".phasing", rm.getPhasing());
        data.put(key + ".prephasing", rm.getPrephasing());

        data.put("read" + rm.getNumberRead() + ".density.ratio", densityRatio);
        String s =
            data.getBoolean("run.info.read" + rm.getNumberRead() + ".indexed")
                ? "(Index)" : "";
        data.put("read" + rm.getNumberRead() + ".type", s);

      }

      return data;
    }

    @Override
    public String toString() {
      return String
          .format(
              "Density %.2f\t -PF %.2f\t nbC %s\t PF %s\t prc %.2f\tsd Density %.3f\t -PF %.3f\t nbC %.3f\t PF %.3f\t prc %.3f",
              clusterDensity, clusterDensityPF, numberCluster, numberClusterPF,
              prcPFClusters, clusterDensitySD, clusterDensityPFSD,
              numberClusterSD, numberClusterPFSD, prcPFClustersSD);
    }

    //
    // Constructor
    //

    TileMetricsPerLane(final int laneNumber, final int countReads,
        final int countTiles, final double densityRatio) {

      this.laneNumber = laneNumber;
      this.countTiles = countTiles;
      this.densityRatio = densityRatio;

      this.metricsPerTilePerCode = Maps.newHashMap();
      this.listReads = new LinkedList<ReadTileMetrics>();

      for (int read = 1; read <= countReads; read++)
        listReads.add(new ReadTileMetrics(read));

    }

    //
    // Inner class
    //

    /**
     * This class contains all tile metrics specific on a read
     * @author Sandrine Perrin
     * @since 1.1
     */
    private static final class ReadTileMetrics {

      private int readNumber; // N
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
       * @param code
       * @param stat object contains all metrics
       */
      public void addValue(final int code, final StatisticsUtils stat) {

        if (code >= PCR_ALIGNED_PHIX_CODE && code < 400) {
          this.percentAlignedPhix = stat.getMean();
          this.percentAlignedPhixSD = stat.getStandardDeviation();

        } else if (code % 2 == 0) {
          // Compute the mediane with using value = 0.0
          this.phasing = stat.getMedianWithoutZero() * 100;
        } else {
          this.prephasing = stat.getMedianWithoutZero() * 100;
        }
      }

      /**
       * Get the number read.
       * @return number read
       */
      public int getNumberRead() {
        return readNumber;
      }

      /**
       * Get the percent of phasing.
       * @return percent of phasing
       */
      public double getPhasing() {
        return phasing;
      }

      /**
       * Get the percent of prephasing.
       * @return percent of prephasing
       */
      public double getPrephasing() {
        return prephasing;
      }

      /**
       * Get the percentage of countReads passing filter that were uniquely
       * aligned to the reference.
       * @return percent of cluster aligned on Phix
       */
      public double getPercentAlignedPhix() {
        return percentAlignedPhix;
      }

      /**
       * Get the standard deviation of countReads passing filter that were
       * uniquely aligned to the reference.
       * @return percent of cluster aligned on Phix
       */
      public double getPercentAlignedPhixSD() {
        return percentAlignedPhixSD;
      }
    }
  }
}