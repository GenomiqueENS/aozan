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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.util.StatisticsUtils;

/**
 * This class collects run data by reading the TileMetricsOut.bin in InterOp
 * directory. The value are the same for all read in a lane.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class TileMetricsCollector extends AbstractBinaryFileCollector {

  /** The sub-collector name from ReadCollector. */
  public static final String NAME_COLLECTOR = "TileMetricsCollector";
  private Map<Integer, TileMetricsPerLane> tileMetrics = Maps.newHashMap();

  @Override
  public String getName() {
    return NAME_COLLECTOR;
  }

  /**
   * Collect data from TileMetric interOpFile.
   * @param data result data object
   */
  @Override
  public void collect(final RunData data) throws AozanException {
    super.collect(data);

    int lane;

    TileMetricsReader reader = new TileMetricsReader(dirInterOpPath);
    initMetricsMap();

    // Distribution of metrics between lane and code
    for (IlluminaTileMetrics itm : reader.getSetIlluminaMetrics()) {
      lane = itm.getLaneNumber();
      tileMetrics.get(lane).addMetric(itm);
    }

    // Build runData
    for (Map.Entry<Integer, TileMetricsPerLane> entry : tileMetrics.entrySet()) {

      entry.getValue().computeData();
      data.put(entry.getValue().getRunData());
    }
  }

  /**
   * Initialize TileMetrics map.
   * @return map
   */
  private void initMetricsMap() {

    for (int lane = 1; lane <= lanesCount; lane++)
      tileMetrics.put(lane,
          new TileMetricsPerLane(lane, readsCount, tilesCount));

  }

  //
  // Internal class
  //

  /**
   * This class contains all tile metrics for a lane extracted from binary file
   * (TileMetricsOut.bin in InterOp directory).
   * @author Sandrine Perrin
   * @since 1.1
   */
  final class TileMetricsPerLane {

    private int laneNumber;
    private int countTiles;

    private double clusterDensity = 0.0; // code in binary file 100
    private double clusterDensityPF = 0.0; // code in binary file 101
    private long numberCluster = 0L; // code in binary file 102
    private long numberClusterPF = 0L; // code in binary file 103
    private double prcPFClusters = 0.0;
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
        case 100:
          this.clusterDensity = mean.doubleValue();
          this.clusterDensitySD = sd.doubleValue();
          break;

        case 101:
          this.clusterDensityPF = mean.doubleValue();
          this.clusterDensityPFSD = sd.doubleValue();
          break;

        case 102:
          this.numberCluster = mean.longValue();
          this.numberClusterSD = sd.doubleValue();
          break;

        case 103:
          this.numberClusterPF = mean.longValue();
          this.numberClusterPFSD = sd.doubleValue();
          break;

        case 400:
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

            double prc =
                new Double(clustersPF.getValue())
                    / new Double(clusters.getValue());

            stat.addValues(prc);

            break;
          }
        }
      }

      this.prcPFClusters = stat.getMean();
      this.prcPFClustersSD = stat.getStandardDeviation();

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
      if (code >= 300)
        numeroRead = code - 300 + 1;
      else {
        // code pair and < 300
        if (code % 2 == 0)
          numeroRead = (code - 200) / 2 + 1;
        else
          // code impair and < 300
          numeroRead = (code - 201) / 2 + 1;
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
        data.put(key + ".clusters.pf", getNumberClusterPF());
        data.put(key + ".clusters.pf.sd", getNumberClusterPFSD());

        data.put(key + ".clusters.raw", getNumberCluster());
        data.put(key + ".clusters.raw.sd", getNumberClusterSD());

        data.put(key + ".prc.pf.clusters", getPrcPFClusters());
        data.put(key + ".prc.pf.clusters.sd", getPrcPFClustersSD());

        data.put(key + ".tile.count", countTiles);

        // Specific value of align on phix for each read
        data.put(key + ".prc.align", rm.getPercentAlignedPhix());
        data.put(key + ".prc.align.sd", rm.getPercentAlignedPhixSD());

        data.put(key + ".phasing", rm.getPhasing());
        data.put(key + ".prephasing", rm.getPrephasing());

      }

      return data;
    }

    /** Get the mean of percent cluster PF */
    public double getPrcPFClusters() {
      return prcPFClusters * 100;
    }

    /** Get the standard deviation of percent cluster PF */
    public double getPrcPFClustersSD() {
      return prcPFClustersSD * 100;
    }

    /** Get the mean of cluster density */
    public double getClusterDensity() {
      return clusterDensity;
    }

    /** Get the standard deviation of cluster density */
    public double getClusterDensitySD() {
      return clusterDensitySD;
    }

    /** Get the mean of cluster density PF */
    public double getClusterDensityPF() {
      return clusterDensityPF;
    }

    /** Get the standard deviation of cluster density PF */
    public double getClusterDensityPFSD() {
      return clusterDensityPFSD;
    }

    /** Get the mean of number cluster */
    public long getNumberCluster() {
      return numberCluster;
    }

    /** Get the mean of number cluster PF */
    public long getNumberClusterPF() {
      return numberClusterPF;
    }

    /** Get the standard deviation of number cluster */
    public double getNumberClusterSD() {
      return numberClusterSD;
    }

    /** Get the standard deviation of number cluster PF */
    public double getNumberClusterPFSD() {
      return numberClusterPFSD;
    }

    /** Get the number lane from control */
    public int getControlLane() {
      return controlLane.intValue();
    }

    /** Get the set of read metrics */
    public List<ReadTileMetrics> getListReads() {
      return listReads;
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
        final int countTiles) {

      this.laneNumber = laneNumber;
      this.countTiles = countTiles;

      this.metricsPerTilePerCode = Maps.newHashMap();
      this.listReads = new LinkedList<ReadTileMetrics>();

      for (int read = 1; read <= countReads; read++)
        listReads.add(new ReadTileMetrics(read));

    }

    //
    // Internal class
    //

    /**
     * This class contains all tile metrics specific on a read
     * @author Sandrine Perrin
     * @since 1.1
     */
    class ReadTileMetrics {

      private int readNumber; // N
      private double phasing = 0.0; // code in binary file 200+(N-1)*2
      private double prephasing = 0.0; // code in binary file 201+(N-1)*2
      private double percentAlignedPhix = 0.0; // code in binary file 300+N-1
      private double percentAlignedPhixSD = 0.0;

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

        if (code >= 300) {
          this.percentAlignedPhix = new Double(stat.getMean()).doubleValue();
          this.percentAlignedPhixSD =
              new Double(stat.getStandardDeviation()).doubleValue();

        } else if (code % 2 == 0) {
          // Compute the mediane with using value = 0.0
          this.phasing =
              new Double(stat.getMedianWithoutZero()).doubleValue() * 100;

        } else {
          this.prephasing =
              new Double(stat.getMedianWithoutZero()).doubleValue() * 100;
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