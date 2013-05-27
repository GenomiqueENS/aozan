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
import java.util.logging.Logger;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.Globals;

/**
 * This class contains all intensity values for a lane extracted from binary
 * file (ExtractionMetricsOut.bin in InterOp directory).
 * @author Sandrine Perrin
 * @since 1.1
 */
class ExtractionMetricsPerLane extends ValuesPerLane {

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

    List<Number> ratioIntensity = Lists.newArrayList();

    for (int i = 0; i < intensityCycle1Values.size(); i++) {
      double intensityC1 = new Double(intensityCycle1Values.get(i).intValue());
      double intensityC20 =
          new Double(intensityCycle20Values.get(i).intValue());

      if (intensityC1 > 0 && intensityC20 > 0) {
        ratioIntensity.add(intensityC20 / intensityC1);
      }
    }

    this.ratioIntensityCycle20 = (average(ratioIntensity)).doubleValue();
    result = standardDeviation(ratioIntensity, this.ratioIntensityCycle20);

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
      final List<Number> intensityCycle20Values, final int lane, final int read) {

    if (intensityCycle1Values.size() <= 0) {
      System.out.println("ERROR intensity ----> "
          + lane + "-" + read + " list sum rate error empty");
    } else {
      // TODO to check, used only intensity for the base A
      Number intensityC1_AllBases = average(intensityCycle1Values);

      this.intensityCycle1 = intensityC1_AllBases.intValue();

      // intensityCycle1 somme intensity / compt(tile) / 4
      this.intensityCycle1SD =
          standardDeviation(intensityCycle1Values, intensityC1_AllBases);

      // Check if count cycle > 20
      if (intensityCycle20Values.size() > 0) {
        Number intensityC20_AllBases = average(intensityCycle20Values);
        this.intensityCycle20 = intensityC20_AllBases.intValue(); // / 4;

        this.intensityCycle20SD =
            standardDeviation(intensityCycle20Values, this.intensityCycle20);

        // Compute intensity statistic at cycle 20 as a percentage of that at
        // the
        // first cycle.
        computeRatioIntensityCycle20(intensityCycle1Values,
            intensityCycle20Values);
      }
    }
  }
}
