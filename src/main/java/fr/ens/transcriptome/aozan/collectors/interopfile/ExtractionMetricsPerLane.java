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

import java.util.List;

/**
 * This class contains all intensity values for a lane extracted from binary
 * file (ExtractionMetricsOut.bin in InterOp directory).
 * @author Sandrine Perrin
 * @since 1.1
 */
class ExtractionMetricsPerLane {

  private int intensityCycle1 = 0;
  private double intensityCycle1SD = 0.0;
  private int intensityCycle20 = 0;
  private double intensityCycle20SD = 0.0;
  private double ratioIntensityCycle20 = 0.0;
  private double ratioIntensityCycle20SD = 0.0;

  /**
   * @param intensityCycle1Values all intensities values for the cycle 1
   * @param intensityCycle20Values all intensities values for the cycle 20
   * @return standard deviation to the ratio intensity in cycle 20.
   */
  private void computeRatioIntensityCycle20(List<Number> intensityCycle1Values,
      List<Number> intensityCycle20Values) {
    double result = 0.0;

    StatisticsUtils stat = new StatisticsUtils();

    for (int i = 0; i < intensityCycle1Values.size(); i++) {
      double intensityC1 = new Double(intensityCycle1Values.get(i).intValue());
      double intensityC20 =
          new Double(intensityCycle20Values.get(i).intValue());

      if (intensityC1 > 0 && intensityC20 > 0) {
        stat.addValues(intensityC20 / intensityC1);
      }
    }

    this.ratioIntensityCycle20 = stat.getMean();
    result = stat.getStandardDeviation();

    this.ratioIntensityCycle20SD = result * 100;
  }

  /**
   * Get the average intensity for cycle 1.
   * @return average intensity for cycle 1
   */
  public int getIntensityCycle1() {
    return intensityCycle1;
  }

  /**
   * Get the standard deviation intensity for cycle 1.
   * @return standard deviation intensity for cycle 1
   */
  public double getIntensityCycle1SD() {
    return intensityCycle1SD;
  }

  /**
   * Get the average intensity for cycle 20.
   * @return average intensity for cycle 20
   */
  public double getIntensityCycle20() {
    return intensityCycle20;
  }

  /**
   * Get the standard deviation intensity for cycle 20.
   * @return standard deviation intensity for cycle 1
   */
  public double getIntensityCycle20SD() {
    return intensityCycle20SD;
  }

  /**
   * Get the average for intensity statistic at cycle 20 as a percentage of that
   * at the first cycle.
   * @return average ratio intensity for cycle 20 compare to cycle 1
   */
  public double getRatioIntensityCycle20() {
    return ratioIntensityCycle20 * 100;
  }

  /**
   * Get the standard deviation for intensity statistic at cycle 20 as a
   * percentage of that at the first cycle.
   * @return standard deviation ratio intensity for cycle 20 compare to cycle 1
   */
  public double getRatioIntensityCycle20SD() {
    return ratioIntensityCycle20SD;
  }

  @Override
  public String toString() {
    return String.format("c1 %s\tsd %.4f \tratio %.4f \tratio.sd %.4f",
        this.intensityCycle1, this.intensityCycle1SD,
        this.ratioIntensityCycle20, this.ratioIntensityCycle20SD);
  }

  //
  // Constructor
  //

  /**
   * Constructor to compute intensities values.
   * @param intensityCycle1Values all intensities values for the cycle 1
   * @param intensityCycle20Values all intensities values for the cycle 20
   */
  ExtractionMetricsPerLane(final List<Number> intensityCycle1Values,
      final List<Number> intensityCycle20Values) {

    StatisticsUtils statCycle1 = new StatisticsUtils(intensityCycle1Values);
    StatisticsUtils statCycle20 = new StatisticsUtils(intensityCycle20Values);

    // TODO to check, used only intensity for the base A
    this.intensityCycle1 = new Double(statCycle1.getMean()).intValue();

    // intensityCycle1 somme intensity / compt(tile) / 4
    this.intensityCycle1SD = statCycle1.getStandardDeviation();

    // Check if count cycle > 20
    if (intensityCycle20Values.size() > 0) {
      this.intensityCycle20 = new Double(statCycle20.getMean()).intValue();

      this.intensityCycle20SD = statCycle20.getStandardDeviation();

      // Compute intensity statistic at cycle 20 as a percentage of that at
      // the first cycle.
      computeRatioIntensityCycle20(intensityCycle1Values,
          intensityCycle20Values);
    }
  }
}
