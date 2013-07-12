/*
 *                  Aozan development code
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

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.util.StatisticsUtils;

/**
 * This class collects run data by reading the ErrorMetricsOut.bin in InterOp
 * directory. This file exists if the lane is spiking with PhiX, if it doesn't
 * exist the values are been defined at 0.0.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ErrorMetricsCollector extends AbstractBinaryFileCollector {

  /** The sub-collector name from ReadCollector. */
  public static final String NAME_COLLECTOR = "ErrorMetricsCollector";

  private int read1LastCycleNumber = -1;
  private int read2LastCycleNumber = -1;

  private Map<Integer, ErrorRatesPerLane> errorRatesMetrics = Maps.newHashMap();

  @Override
  public String getName() {
    return NAME_COLLECTOR;
  }

  /**
   * Collect data.
   * @param data result data object
   */
  @Override
  public void collect(final RunData data) throws AozanException {
    super.collect(data);

    try {
      ErrorMetricsReader reader = new ErrorMetricsReader(dirInterOpPath);
      initMetricsMap(data);

      int lane;
      int read;

      // Distribution of metrics between lane and code
      for (IlluminaErrorMetrics iem : reader.getSetIlluminaMetrics()) {
        lane = iem.getLaneNumber();
        read = getReadNumber(iem.getCycleNumber());

        int keyMap = lane * 100 + read;
        errorRatesMetrics.get(keyMap).addMetric(iem);
      }

    } catch (FileNotFoundException e) {

      // Case : ErrorMetricsOut.bin doesn't exist, all values are 0.0
      collectionEmpty(1, lanesCount);

    }

    // Build runData
    for (Map.Entry<Integer, ErrorRatesPerLane> entry : errorRatesMetrics.entrySet()) {

      entry.getValue().computeData();
      data.put(entry.getValue().getRunData());
    }

  }

  /**
   * Initialize ErrorMetrics map.
   * @return map
   */
  private void initMetricsMap(final RunData data) {

    defineReadLastCycleNumber(data);

    for (int lane = 1; lane <= lanesCount; lane++) {
      for (int read = 1; read <= readsCount; read++) {

        // lane skiping with phix
        if (data.getBoolean("run.info.align.to.phix.lane" + lane)) {
          errorRatesMetrics.put(getKeyMap(lane, read), new ErrorRatesPerLane(lane,
              read, readsCount));

        } else {
          // None phix in this lane, all value error are 0
          collectionEmpty(lane, lane);
        }
      }
    }

  }

  /**
   * Define the readNumber corresponding to the cycle
   * @param cycle
   * @return readNumber
   */
  private int getReadNumber(final int cycle) {
    if (read1LastCycleNumber >= cycle)
      return 1;

    if (read2LastCycleNumber >= cycle)
      return 2;

    return 3;
  }

  /**
   * Add in error metrics map a new instance of errorRatesPerLaneRead with
   * default values, none metrics in file for the pair lane-read.
   * @param firstLane number lane
   * @param lastLane number lane
   */
  private void collectionEmpty(final int firstLane, final int lastLane) {

    for (int lane = firstLane; lane <= lastLane; lane++) {
      for (int read = 1; read <= readsCount; read++) {

        int keyMap = lane * 100 + read;
        errorRatesMetrics.put(keyMap, new ErrorRatesPerLane(lane, read, readsCount));
      }
    }

  }

  /**
   * In SR : count cycles final = 50 only for read1 In PE : count cycles final =
   * 208, ie read1:100 (instead of 101), read2:107, read3:208.
   * @param data
   * @return
   */
  private void defineReadLastCycleNumber(final RunData data) {

    // for Error metrics ignores first cycle
    int cyclesCount = -1;

    cyclesCount += data.getInt("run.info.read" + 1 + ".cycles");
    this.read1LastCycleNumber = cyclesCount;

    if (readsCount == 2) {
      // Case SR
      this.read2LastCycleNumber = cyclesCount;

    } else {
      // case PE
      cyclesCount += data.getInt("run.info.read" + 2 + ".cycles");
      this.read2LastCycleNumber = cyclesCount;

    }

  }

  //
  // Internal class
  //

  /**
   * This class contains all error values for a lane extracted from binary file
   * (ErrorMetricsOut.bin in InterOp directory).
   * @author Sandrine Perrin
   * @since 1.1
   */
  class ErrorRatesPerLane {

    private int seuil_35_cycle;
    private int seuil_75_cycle;
    private int seuil_100_cycle;

    private int laneNumber;
    private int readNumber;

    // average errorRate
    private double errorRate = 0.0;
    private double errorRateCycle35 = 0.0;
    private double errorRateCycle75 = 0.0;
    private double errorRateCycle100 = 0.0;

    // standard deviation errorRate
    private double errorRateSD = 0.0;
    private double errorRateCycle35SD = 0.0;
    private double errorRateCycle75SD = 0.0;
    private double errorRateCycle100SD = 0.0;

    private int calledCyclesMin = 0;
    private int calledCyclesMax = 0;

    private boolean dataToCompute = true;

    // Save pair tile-rate error for all cycles for a lane
    private ListMultimap<Integer, Number> allErrorRates = ArrayListMultimap
        .create();

    // Save pair tile-rate error for cycles (1 to 35) for all tiles
    private ListMultimap<Integer, Number> error35 = ArrayListMultimap.create();

    // Save pair tile-rate error for cycles (1 to 75) for all tiles, for run PE
    private ListMultimap<Integer, Number> error75 = ArrayListMultimap.create();

    // Save pair tile-rate error for cycles (1 to 100) for all tiles, for run PE
    private ListMultimap<Integer, Number> error100 = ArrayListMultimap.create();

    /**
     * Save a record from TileMetricsOut.bin file.
     * @param itm illumina tile metrics
     */
    public void addMetric(final IlluminaErrorMetrics iem) {

      allErrorRates.put(iem.getTileNumber(), iem.getErrorRate());
      int cycle = iem.getCycleNumber();

      if (cycle <= seuil_35_cycle) {
        error35.put(iem.getTileNumber(), iem.getErrorRate());
      }

      // seuil = 0 in run SR
      if (cycle <= seuil_75_cycle) {
        error75.put(iem.getTileNumber(), iem.getErrorRate());
      }

      // seuil = 0 in run SR
      if (cycle <= seuil_100_cycle) {
        error100.put(iem.getTileNumber(), iem.getErrorRate());
      }
    }

    /**
     * Compute mean and standard deviation from metrics reading in
     * ErrorMetricsOut.bin file.
     */
    public void computeData() {

      List<Number> errorRatePerTile;

      if (allErrorRates.size() > 0) {
        errorRatePerTile = computeErrorRatePerTile(allErrorRates);
        StatisticsUtils stat = new StatisticsUtils(errorRatePerTile);

        this.errorRate = stat.getMean();
        this.errorRateSD = stat.getStandardDeviation();

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

      dataToCompute = false;
    }

    /**
     * Compute the rate error for each Tiles with the mean by cycle.
     * @param values of rate error per countTiles, per cycles
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
     * Save data from error metrics for a run in a RunData
     * @return rundata data from tile metrics for a run
     */
    public RunData getRunData() {

      if (dataToCompute)
        computeData();

      RunData data = new RunData();

      String key = "read" + readNumber + ".lane" + laneNumber;

      data.put(key + ".err.rate.100", errorRateCycle100);
      data.put(key + ".err.rate.100.sd", errorRateCycle100SD);
      data.put(key + ".err.rate.35", errorRateCycle35);
      data.put(key + ".err.rate.35.sd", errorRateCycle35SD);
      data.put(key + ".err.rate.75", errorRateCycle75);
      data.put(key + ".err.rate.75.sd", errorRateCycle75SD);
      data.put(key + ".err.rate.phix", errorRate);
      data.put(key + ".err.rate.phix.sd", errorRateSD);

      // TODO to define
      data.put(key + ".called.cycles.max", 0);
      data.put(key + ".called.cycles.min", 0);

      return data;
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
     * Get the standard deviation for the rate error for a lane, all cycles
     * used.
     * @return rate error for a lane.
     */
    public double getErrorRateSD() {
      return errorRateSD;
    }

    /**
     * Get the standard deviation for the rate error for a lane, cycles used
     * from 1 to 35.
     * @return rate error for a lane.
     */
    public double getErrorRateCycle35SD() {
      return errorRateCycle35SD;
    }

    /**
     * Get the standard deviation for the rate error for a lane, cycles used
     * from 1 to 75.
     * @return rate error for a lane.
     */
    public double getErrorRateCycle75SD() {
      return errorRateCycle75SD;
    }

    /**
     * Get the standard deviation for the rate error for a lane, cycles used
     * from 1 to 100.
     * @return rate error for a lane.
     */
    public double getErrorRateCycle100SD() {
      return errorRateCycle100SD;
    }

    @Override
    public String toString() {
      return String
          .format(
              "\t%d\t%d\trate %.2f\trate sd %.3f\t35 %.2f\t35 sd %.3f\t75 %.2f\t75 sd %.3f\t100 %.2f\t100 sd %.3f\tmin %d\tmax %d",
              laneNumber, readNumber, errorRate, errorRateSD, errorRateCycle35,
              errorRateCycle35SD, errorRateCycle75, errorRateCycle75SD,
              errorRateCycle100, errorRateCycle100SD, calledCyclesMin,
              calledCyclesMax);

    }

    //
    // Constructor
    //

    /**
     * Constructor
     * @param lane lane number
     * @param read read number
     * @param readsCount number read for the run
     */
    ErrorRatesPerLane(final int lane, final int read, final int readsCount) {
      this.laneNumber = lane;
      this.readNumber = read;

      if (readsCount == 3) {
        // Case PE
        if (read == 3) {
          // case PE - read2
          // Set first cycle for read 3
          int startCycleNumberR3 = read2LastCycleNumber + 1;

          seuil_35_cycle = startCycleNumberR3 + 35;
          seuil_75_cycle = startCycleNumberR3 + 75;
          seuil_100_cycle = startCycleNumberR3 + 100;

        } else {
          // Case SR and PE - read1
          seuil_35_cycle = 35;
          seuil_75_cycle = 75;
          seuil_100_cycle = 100;
        }
      } else {
        // case SR - 50
        seuil_35_cycle = 35;
        seuil_75_cycle = -1;
        seuil_100_cycle = -1;
      }
    }
  }
}
