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

package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

/**
 * This class contains all tile metrics for a lane extracted from binary file
 * (TileMetricsOut.bin in InterOp directory).
 * @author Sandrine Perrin
 * @since 1.1
 */
class TileMetricsPerLane extends ValuesPerLane {

  private double clusterDensity = 0.0; // code in binary file 100
  private double clusterDensityPF = 0.0; // code in binary file 101
  private long numberCluster = 0L; // code in binary file 102
  private long numberClusterPF = 0L; // code in binary file 103
  private double prcPFClusters = 0.0;
  private List<Number> numberClusterValues = Lists.newArrayList();
  private List<Number> numberClusterPFValues = Lists.newArrayList();
  // private List<Number> clusterDensityValues = Lists.newArrayList();
  // private List<Number> clusterDensityPFValues = Lists.newArrayList();

  // Standard deviation
  private double clusterDensitySD = 0.0;
  private double clusterDensityPFSD = 0.0;
  private double numberClusterSD = 0.0;
  private double numberClusterPFSD = 0.0;
  private double prcPFClustersSD = 0.0;

  private Double controlLane = 0.0; // code in binary file 400
  private int tileCount = 1;
  private boolean prcClusterCompute = false;

  // All tile metrics per read
  private List<ReadMetrics> list = new LinkedList<ReadMetrics>();

  public void addMetrics(final int code, final List<Number> sum) {

    Number value = null;
    Number sd = null;
    // if (code >= 200 && code <= 299) {
    // double d = 0.0;
    // for (Number n : sum) {
    // d += n.doubleValue();
    // }
    // value = d;
    // } else {
    value = average(sum);
    sd = standardDeviation(sum, value);
    // }

    switch (code) {
    case 100:
      this.clusterDensity = value.doubleValue();
      this.clusterDensitySD = sd.doubleValue();
      // this.clusterDensityValues = sum;
      break;
    case 101:
      this.clusterDensityPF = value.doubleValue();
      this.clusterDensityPFSD = sd.doubleValue();
      // this.clusterDensityPFValues = sum;
      break;
    case 102:
      this.numberCluster = value.longValue();
      this.numberClusterSD = sd.doubleValue();
      this.numberClusterValues = sum;
      // System.out.println("nb_clusters " + Joiner.on(" ").join(sum));
      break;
    case 103:
      this.numberClusterPF = value.longValue();
      this.numberClusterPFSD = sd.doubleValue();
      this.numberClusterPFValues = sum;
      // System.out.println("nb_clusters_PF " + Joiner.on(" ").join(sum));
      break;
    case 400:
      // value unique to a run, read first value in list
      this.controlLane = sum.get(0).doubleValue();
      break;
    default: // code 20X and 30X
      addReadMetrics(code, value, sd);
    }

    computePercentClusterPF();
  }

  private void computePercentClusterPF() {

    if (numberClusterPFValues.size() == 0 || numberClusterValues.size() == 0)
      return;

    if (prcClusterCompute)
      return;

    List<Number> prcPFClustersDetail =
        Lists.newArrayListWithCapacity(numberClusterValues.size());

    for (int i = 0; i < numberClusterValues.size(); i++) {

      // if (numberClusterPFValues.get(i).intValue() > 0) {
      double prc =
          new Double(numberClusterPFValues.get(i).intValue())
              / new Double(numberClusterValues.get(i).intValue());

      prcPFClustersDetail.add(prc);
      // }
    }

    this.prcPFClusters = (average(prcPFClustersDetail)).doubleValue();
    this.prcPFClustersSD =
        standardDeviation(prcPFClustersDetail, this.prcPFClusters);

    prcClusterCompute = true;
  }

  private void addReadMetrics(final int code, final Number average,
      final Number sd) {

    // code >= 300
    int numeroRead = -1;
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

    boolean find = false;
    for (ReadMetrics read : list) {
      if (read.getNumberRead() == numeroRead) {
        read.addValue(code, average, sd);
        find = true;
      }
    }

    if (!find) {
      // add new ReadMetrics
      ReadMetrics readMetrics = new ReadMetrics(numeroRead);
      readMetrics.addValue(code, average, sd);
      list.add(readMetrics);
    }
  }

  public double getPrcPFClusters() {
    return prcPFClusters * 100;
  }

  public double getPrcPFClustersSD() {
    return prcPFClustersSD * 100;
  }

  public double getClusterDensity() {
    return clusterDensity;
  }

  public double getClusterDensityPF() {
    return clusterDensityPF;
  }

  public long getNumberCluster() {
    return numberCluster;
  }

  public long getNumberClusterPF() {
    return numberClusterPF;
  }

  public double getClusterDensitySD() {
    return clusterDensitySD;
  }

  public double getClusterDensityPFSD() {
    return clusterDensityPFSD;
  }

  public double getNumberClusterSD() {
    return numberClusterSD;
  }

  public double getNumberClusterPFSD() {
    return numberClusterPFSD;
  }

  public int getControlLane() {
    return controlLane.intValue();
  }

  public List<ReadMetrics> getList() {
    return list;
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

  /**
   * Constructor
   * @param metrics set of tile metrics (code and value) for a lane
   */
  TileMetricsPerLane(final ListMultimap<Integer, Number> metrics) {

    // Map key is code metric, values are values per tile
    Map<Integer, Collection<Number>> map = metrics.asMap();

    for (Map.Entry<Integer, Collection<Number>> entry : map.entrySet()) {
      List<Number> list =
          Arrays.asList(entry.getValue().toArray(new Number[] {}));
      addMetrics(entry.getKey(), list);
    }
  }

  //
  // Internal class
  //

  /**
   * This class contains all tile metrics specific on a read
   * @author Sandrine Perrin
   * @since 1.1
   */
  class ReadMetrics {

    private int numberRead; // N
    private double phasing = 0.0; // code in binary file 20N
    private double prephasing = 0.0; // code in binary file 20(N+1)
    private double percentAlignedPhix = 0.0; // code in binary file 30N
    private double percentAlignedPhixSD = 0.0;

    /**
     * Constructor
     * @param numberRead number read
     */
    ReadMetrics(final int numberRead) {
      this.numberRead = numberRead;
    }

    /**
     * Initialize a value according to code.
     * @param code
     * @param average
     * @param sd
     */
    public void addValue(final int code, final Number average, final Number sd) {

      if (code >= 300) {
        this.percentAlignedPhix = average.doubleValue();
        this.percentAlignedPhixSD = sd.doubleValue();

      } else if (code % 2 == 0)
        this.phasing = average.doubleValue() * 100;
      else
        this.prephasing = average.doubleValue() * 100;
    }

    /**
     * Get the number read.
     * @return number read
     */
    public int getNumberRead() {
      return numberRead;
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
     * Get the percentage of reads passing filter that were uniquely aligned to
     * the reference.
     * @return percent of cluster aligned on Phix
     */
    public double getPercentAlignedPhix() {
      return percentAlignedPhix;
    }

    /**
     * Get the standard deviation of reads passing filter that were uniquely
     * aligned to the reference.
     * @return percent of cluster aligned on Phix
     */
    public double getPercentAlignedPhixSD() {
      return percentAlignedPhixSD;
    }
  }
}
