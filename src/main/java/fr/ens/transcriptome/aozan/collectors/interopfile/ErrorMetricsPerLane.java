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
import java.util.List;
import java.util.Map;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

/**
 * This class contains all error values for a lane extracted from binary file
 * (ErrorMetricsOut.bin in InterOp directory).
 * @author Sandrine Perrin
 * @since 1.1
 */
class ErrorRatesPerLane {

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

  /**
   * Compute the rate error for each number tiles.
   * @param values of rate error per tiles, per cycles
   * @return list of rate error for each number tile
   */
  public List<Number> computeErrorRatePerTile(
      final ListMultimap<Integer, Number> values) {

    // Define map : tile and list of rate error, one per cycle to use
    Map<Integer, Collection<Number>> errorValuePerTile = values.asMap();

    // Save rate error per tile
    List<Number> errorRatePerTile = Lists.newArrayList();

    for (Map.Entry<Integer, Collection<Number>> entry : errorValuePerTile
        .entrySet()) {
      List<Number> list =
          Arrays.asList(entry.getValue().toArray(new Number[] {}));

      StatisticsUtils stat = new StatisticsUtils(list);
      errorRatePerTile.add(stat.getMean());

    }
    return errorRatePerTile;
  }

  /**
   * Get the rate error for a lane, all cycles used.
   * @return rate error for a lane.
   */
  public double getErrorRate() {
    return this.errorRate;
  }

  /**
   * Get the rate error for a lane, cycles used from 1 to 35.
   * @return rate error for a lane.
   */
  public double getErrorRateCycle35() {
    return this.errorRateCycle35;
  }

  /**
   * Get the rate error for a lane, cycles used from 1 to 75.
   * @return rate error for a lane.
   */
  public double getErrorRateCycle75() {
    return this.errorRateCycle75;
  }

  /**
   * Get the rate error for a lane, cycles used from 1 to 100.
   * @return rate error for a lane.
   */
  public double getErrorRateCycle100() {
    return this.errorRateCycle100;
  }

  /**
   * Get the standard deviation for the rate error for a lane, all cycles used.
   * @return rate error for a lane.
   */
  public double getErrorRateSD() {
    return errorRateSD;
  }

  /**
   * Get the standard deviation for the rate error for a lane, cycles used from
   * 1 to 35.
   * @return rate error for a lane.
   */
  public double getErrorRateCycle35SD() {
    return errorRateCycle35SD;
  }

  /**
   * Get the standard deviation for the rate error for a lane, cycles used from
   * 1 to 75.
   * @return rate error for a lane.
   */
  public double getErrorRateCycle75SD() {
    return errorRateCycle75SD;
  }

  /**
   * Get the standard deviation for the rate error for a lane, cycles used from
   * 1 to 100.
   * @return rate error for a lane.
   */
  public double getErrorRateCycle100SD() {
    return errorRateCycle100SD;
  }

  @Override
  public String toString() {
    return String
        .format(
            "\trate %.2f\trate sd %.3f\t35 %.2f\t35 sd %.3f\t75 %.2f\t75 sd %.3f\t100 %.2f\t100 sd %.3f",
            errorRate, errorRateSD, errorRateCycle35, errorRateCycle35SD,
            errorRateCycle75, errorRateCycle75SD, errorRateCycle100,
            errorRateCycle100SD);

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param sumErrorRate values of rate error for each tile and all cycles
   * @param error35 error values of rate error for each tile and cycles from 1
   *          to 35
   * @param error75 error values of rate error for each tile and cycles from 1
   *          to 75, if count of cycle < 75, ListMultimap empty
   * @param error100 error values of rate error for each tile and cycles from 1
   *          to 100 if count of cycle < 100, ListMultimap empty
   */
  ErrorRatesPerLane(final ListMultimap<Integer, Number> sumErrorRate,
      ListMultimap<Integer, Number> error35,
      final ListMultimap<Integer, Number> error75,
      final ListMultimap<Integer, Number> error100, final int lane,
      final int read) {

    // Need rate error per tile before compute rate error for a lane
    List<Number> errorRatePerTile;

    if (sumErrorRate.size() > 0) {
      errorRatePerTile = computeErrorRatePerTile(sumErrorRate);
      StatisticsUtils stat = new StatisticsUtils(errorRatePerTile);

      this.errorRate = stat.getMean();
      this.errorRateSD = stat.getStandardDeviation();

    } else {
      System.out.println("ERROR error ----> "
          + lane + "-" + read + " list sum rate error empty");
    }

    // Check if number cycle > 35, else values are 0.0
    if (error35.size() > 0) {
      errorRatePerTile = computeErrorRatePerTile(error35);
      StatisticsUtils stat = new StatisticsUtils(errorRatePerTile);

      this.errorRateCycle35 = stat.getMean();
      this.errorRateCycle35SD = stat.getStandardDeviation();

    }

    // Check if number cycle > 75, else values are 0.0
    if (error75.size() > 0) {
      errorRatePerTile = computeErrorRatePerTile(error75);
      StatisticsUtils stat = new StatisticsUtils(errorRatePerTile);

      this.errorRateCycle75 = stat.getMean();
      this.errorRateCycle75SD = stat.getStandardDeviation();

    }

    // Check if number cycle > 100, else values are 0.0
    if (error100.size() > 0) {
      errorRatePerTile = computeErrorRatePerTile(error100);
      StatisticsUtils stat = new StatisticsUtils(errorRatePerTile);

      this.errorRateCycle100 = stat.getMean();
      this.errorRateCycle100SD = stat.getStandardDeviation();

    }

  }

  /**
   * Public constructor, all variations are default (0.0).
   */
  ErrorRatesPerLane() {
  }
}