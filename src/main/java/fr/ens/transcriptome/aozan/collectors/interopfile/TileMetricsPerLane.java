package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

public class TileMetricsPerLane extends ValuesPerLane {

  private double clusterDensity = 0.0; // 100
  private double clusterDensityPF = 0.0; // 101
  private long numberCluster = 0L; // 102
  private long numberClusterPF = 0L; // 103
  private double prcPFClusters = 0.0;
  private List<Number> numberClusterValues = Lists.newArrayList();
  private List<Number> numberClusterPFValues = Lists.newArrayList();
  private List<Number> clusterDensityValues = Lists.newArrayList();
  private List<Number> clusterDensityPFValues = Lists.newArrayList();

  // Standard deviation
  private double clusterDensitySD = 0.0;
  private double clusterDensityPFSD = 0.0;
  private double numberClusterSD = 0.0;
  private double numberClusterPFSD = 0.0;
  private double prcPFClustersSD = 0.0;

  private double controlLane = 0.0; // 400
  private int tileCount = 1;
  private boolean prcClusterCompute = false;

  private List<ReadMetrics> list = new LinkedList<ReadMetrics>();

  public TileMetricsPerLane(final int tileCount,
      final ListMultimap<Integer, Number> metrics) {

    this.tileCount = tileCount;
    Map<Integer, Collection<Number>> map = metrics.asMap();

    for (Map.Entry<Integer, Collection<Number>> entry : map.entrySet()) {
      List<Number> list =
          Arrays.asList(entry.getValue().toArray(new Number[] {}));
      addMetrics(entry.getKey(), list);
    }
  }

  public void addMetrics(final int code, final List<Number> sum) {

    Number value;

    if (code >= 200 && code <= 299) {
      double d = 0.0;
      for (Number n : sum) {
        d += n.doubleValue();
      }
      value = d;
    } else {
      value = average(sum);
    }
    Number sd = standardDeviation(sum, value);

    switch (code) {
    case 100:
      this.clusterDensity = value.doubleValue();
      this.clusterDensitySD = sd.doubleValue();
      this.clusterDensityValues = sum;
      break;
    case 101:
      this.clusterDensityPF = value.doubleValue();
      this.clusterDensityPFSD = sd.doubleValue();
      this.clusterDensityPFValues = sum;
      break;
    case 102:
      this.numberCluster = value.longValue();
      this.numberClusterSD = sd.doubleValue();
      this.numberClusterValues = sum;
      break;
    case 103:
      this.numberClusterPF = value.longValue();
      this.numberClusterPFSD = sd.doubleValue();
      this.numberClusterPFValues = sum;
      break;
    case 400:
      this.controlLane = value.longValue();
      break;
    default:
      addReadMetrics(code, value, sd);
    }

    computePercentClusterPF_old();
  }

  private void computePercentClusterPF() {

    if (clusterDensityPFValues.size() == 0 || clusterDensityValues.size() == 0)
      return;

    if (prcClusterCompute)
      return;

    List<Number> prcPFClustersDetail =
        Lists.newArrayListWithCapacity(clusterDensityValues.size());

    for (int i = 0; i < clusterDensityPFValues.size(); i++) {

      // if (clusterDensityValues.get(i).intValue() > 0) {
      double prc =
          new Double(clusterDensityPFValues.get(i).intValue())
              / new Double(clusterDensityValues.get(i).intValue());

      prcPFClustersDetail.add(prc);
      // }
    }

    this.prcPFClusters = (average(prcPFClustersDetail)).doubleValue();
    this.prcPFClustersSD =
        standardDeviation(prcPFClustersDetail, this.prcPFClusters);

    prcClusterCompute = true;
  }

  private void computePercentClusterPF_old() {

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
      if (read.getNumeroRead() == numeroRead) {
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

  public int getTileCount() {
    return tileCount;
  }

  public double getControlLane() {
    return controlLane;
  }

  public List<ReadMetrics> getList() {
    return list;
  }

  //
  // Internal class
  //

  class ReadMetrics {

    private int numeroRead;
    private double phasing = 0.0; // 20X
    private double prephasing = 0.0; // 20(X+1)
    private double percentAlignedPhix = 0.0; // 30X
    private double percentAlignedPhixSD = 0.0;

    ReadMetrics(final int numeroRead) {
      this.numeroRead = numeroRead;
    }

    public void addValue(final int code, final Number average, final Number sd) {

      if (code >= 300) {
        this.percentAlignedPhix = average.doubleValue();
        this.percentAlignedPhixSD = sd.doubleValue();

      } else if (code % 2 == 0)
        this.phasing = average.doubleValue();
      else
        this.prephasing = average.doubleValue();
    }

    public int getNumeroRead() {
      return numeroRead;
    }

    public double getPhasing() {
      return phasing;
    }

    public double getPrephasing() {
      return prephasing;
    }

    public double getPercentAlignedPhix() {
      return percentAlignedPhix;
    }

    public double getPercentAlignedPhixSD() {
      return percentAlignedPhixSD;
    }
  }
}
