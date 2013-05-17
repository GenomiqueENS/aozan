package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.List;

import com.google.common.collect.Lists;

public class ExtractionMetricsPerLane extends ValuesPerLane {

  private int compt = 0;
  private int intensityCycle1 = 0;
  private int intensityCycle20 = 0;
  private double intensityCycle1SD = 0.0;
  private double intensityCycle20SD = 0.0;
  private double ratioIntensityCycle20 = 0.0;
  private double ratioIntensityCycle20SD = 0.0;

  ExtractionMetricsPerLane(int compt, List<Number> intensityCycle1Values,
      List<Number> intensityCycle20Values) {
    this.compt = compt;

    // System.out.println("tile "
    // + compt + " list size " + sumIntensityCycle1.size());

    Number intensityC1_AllBases = average(intensityCycle1Values);
    this.intensityCycle1 = intensityC1_AllBases.intValue(); // / 4;

    // intensityCycle1 somme intensity / compt(tile) / 4
    this.intensityCycle1SD =
        standardDeviation(intensityCycle1Values, intensityC1_AllBases); // this.intensityCycle1);

    if (intensityCycle20Values.size() > 0) {
      Number intensityC20_AllBases = average(intensityCycle20Values);
      this.intensityCycle20 = intensityC20_AllBases.intValue(); // / 4;

      this.intensityCycle20SD =
          standardDeviation(intensityCycle20Values, this.intensityCycle20);

      this.ratioIntensityCycle20 =
          intensityCycle20 / (double) intensityCycle1 * 100;

      this.ratioIntensityCycle20SD =
          setRatioIntensityCycle20SD(intensityCycle1Values,
              intensityCycle20Values);
    }
  }

  private double setRatioIntensityCycle20SD(List<Number> intensityCycle1Values,
      List<Number> intensityCycle20Values) {
    double result = 0.0;

    List<Number> ratioIntensity = Lists.newArrayList();

    // System.out.println("nb c1 "
    // + intensityCycle1Values.size() + "nb c20"
    // + intensityCycle20Values.size());

    for (int i = 0; i < intensityCycle1Values.size(); i++) {
      double ratio =
          new Double(intensityCycle20Values.get(i).intValue())
              / new Double(intensityCycle1Values.get(i).intValue());

      ratioIntensity.add(ratio);
    }

    double average = (average(ratioIntensity)).doubleValue();
    result = standardDeviation(ratioIntensity, average);
    System.out.printf("size %s \t%.04f\t", ratioIntensity.size(), average);
    System.out.println("moy "
        + this.ratioIntensityCycle20 + " new moy " + average * 100 + " sd "
        + standardDeviation(ratioIntensity, average) * 100 + " sd "
        + standardDeviation(ratioIntensity, this.ratioIntensityCycle20 / 100)
        * 100);

    return result * 100;
  }

  public int getCompt() {
    return compt;
  }

  public int getIntensityCycle1() {
    return intensityCycle1;
  }

  public double getIntensityCycle1SD() {
    return intensityCycle1SD;
  }

  public double getIntensityCycle20() {
    return intensityCycle20;
  }

  public double getIntensityCycle20SD() {
    return intensityCycle20SD;
  }

  public double getRatioIntensityCycle20() {
    return ratioIntensityCycle20;
  }

  public double getRatioIntensityCycle20SD() {
    return ratioIntensityCycle20SD;
  }

  @Override
  public String toString() {
    return String.format("c1 %s\tsd %.4f \tratio %.4f \tratio.sd %.4f",
        this.intensityCycle1, this.intensityCycle1SD,
        this.ratioIntensityCycle20, this.ratioIntensityCycle20SD);
  }
}
