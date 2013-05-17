package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;

class ErrorRatesPerLane extends ValuesPerLane {
  private double errorRate = 0.0; // average errorRate
  private double errorRateCycle35 = 0.0;
  private double errorRateCycle75 = 0.0;
  private double errorRateCycle100 = 0.0;

  private double errorRateSD = 0.0; // average errorRate
  private double errorRateCycle35SD = 0.0;
  private double errorRateCycle75SD = 0.0;
  private double errorRateCycle100SD = 0.0;

  private int calledCyclesMin = 0;
  private int calledCyclesMax = 0;

  ErrorRatesPerLane(final ListMultimap<Integer, Number> sumErrorRate,
      ListMultimap<Integer, Number> error35,
      final ListMultimap<Integer, Number> error75,
      final ListMultimap<Integer, Number> error100) throws AozanException {

    List<Number> errorRatePerTile = computeErrorRatePerTile(sumErrorRate);
    this.errorRate = (average(errorRatePerTile)).doubleValue();
    this.errorRateSD = standardDeviation(errorRatePerTile, this.errorRate);

    if (error35.size() > 0) {
      errorRatePerTile = computeErrorRatePerTile(error35);
      this.errorRateCycle35 = (average(errorRatePerTile)).doubleValue();
      this.errorRateCycle35SD =
          standardDeviation(errorRatePerTile, this.errorRateCycle35);
    }

    if (error75.size() > 0) {
      errorRatePerTile = computeErrorRatePerTile(error75);
      this.errorRateCycle75 = (average(errorRatePerTile)).doubleValue();
      this.errorRateCycle75SD =
          standardDeviation(errorRatePerTile, this.errorRateCycle75);
    }

    if (error100.size() > 0) {
      errorRatePerTile = computeErrorRatePerTile(error100);
      this.errorRateCycle100 = (average(errorRatePerTile)).doubleValue();
      this.errorRateCycle100SD =
          standardDeviation(errorRatePerTile, this.errorRateCycle100);
    }
  }

  ErrorRatesPerLane() {
  }

  public List<Number> computeErrorRatePerTile(
      final ListMultimap<Integer, Number> values) {

    Map<Integer, Collection<Number>> errorValuePerTile = values.asMap();
    List<Number> errorRatePerTile = Lists.newArrayList();

    for (Map.Entry<Integer, Collection<Number>> entry : errorValuePerTile
        .entrySet()) {
      List<Number> list =
          Arrays.asList(entry.getValue().toArray(new Number[] {}));

      errorRatePerTile.add(average(list));

    }
    return errorRatePerTile;
  }

  public double getErrorRate() {
    return this.errorRate;
  }

  public double getErrorRateCycle35() {
    return this.errorRateCycle35;
  }

  public double getErrorRateCycle75() {
    return this.errorRateCycle75;
  }

  public double getErrorRateCycle100() {
    return this.errorRateCycle100;
  }

  public double getErrorRateSD() {
    return errorRateSD;
  }

  public double getErrorRateCycle35SD() {
    return errorRateCycle35SD;
  }

  public double getErrorRateCycle75SD() {
    return errorRateCycle75SD;
  }

  public double getErrorRateCycle100SD() {
    return errorRateCycle100SD;
  }

  @Override
  public String toString() {
    return String.format("\trate %.2f\trate sd %.3f\t35 %.2f\t35 sd %.3f",
        errorRate, errorRateSD, errorRateCycle35, errorRateCycle35SD);

  }

}