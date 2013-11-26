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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;
import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.util.StatisticsUtils;

/**
 * This class collects run data by reading the ErrorMetricsOut.bin in InterOp
 * directory. This file exists if the lane is spiking with PhiX, if it doesn't
 * exist the values are been defined at 0.0.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ErrorMetricsCollector implements Collector {

  /** The sub-collector name from ReadCollector. */
  public static final String NAME_COLLECTOR = "ErrorMetricsCollector";

  private String dirInterOpPath;

  private int lanesCount;
  private int readsCount;

  private int read1CycleCount;
  private int read1LastCycleNumber = -1;

  private int read2LastCycleNumber = -1;

  private int read3CycleCount;

  private final Map<Integer, ErrorRatesPerLane> errorRatesMetrics = Maps
      .newHashMap();

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
  }

  /**
   * Collect data.
   * @param data result data object
   */
  public void collect(final RunData data) throws AozanException {

    lanesCount = data.getInt("run.info.flow.cell.lane.count");
    readsCount = data.getInt("run.info.read.count");
    read1CycleCount = data.getInt("run.info.read1.cycles");

    if (readsCount == 3)
      read3CycleCount = data.getInt("run.info.read3.cycles");

    try {
      ErrorMetricsReader reader = new ErrorMetricsReader(dirInterOpPath);
      initMetricsMap(data);

      int keyMap;

      // Distribution of metrics between lane and code
      for (IlluminaErrorMetrics iem : reader.getSetIlluminaMetrics()) {
        keyMap =
            getKeyMap(iem.getLaneNumber(), getReadNumber(iem.getCycleNumber()));
        errorRatesMetrics.get(keyMap).addMetric(iem);
      }

    } catch (FileNotFoundException e) {

      // Case : ErrorMetricsOut.bin doesn't exist, all values are 0.0
      collectionEmpty(1, lanesCount);

    }

    // Build runData
    for (Map.Entry<Integer, ErrorRatesPerLane> entry : errorRatesMetrics
        .entrySet()) {

      entry.getValue().computeData();
      data.put(entry.getValue().getRunData());
    }

  }

  /**
   * Set unique id for each pair lane-read in a run
   * @param lane lane number
   * @param read read number
   * @return integer identifier unique
   */
  protected int getKeyMap(final int lane, final int read) {
    return lane * 100 + read;
  }

  /**
   * Initialize ErrorMetrics map.
   * @param data result data object
   */
  private void initMetricsMap(final RunData data) {

    defineReadLastCycleNumber(data);

    for (int lane = 1; lane <= lanesCount; lane++) {
      for (int read = 1; read <= readsCount; read++) {

        // lane skiping with phix
        if (data.getBoolean("run.info.align.to.phix.lane" + lane)) {
          errorRatesMetrics.put(getKeyMap(lane, read), new ErrorRatesPerLane(
              lane, read, read1CycleCount, read2LastCycleNumber,
              read3CycleCount));

        } else {
          // None phix in this lane, all values error are 0
          collectionEmpty(lane, lane);
        }
      }
    }

  }

  /**
   * Define the readNumber corresponding to the cycle
   * @param cycle number cycle in the run
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
        errorRatesMetrics.put(keyMap, new ErrorRatesPerLane(lane, read,
            true, read1CycleCount, read2LastCycleNumber,
            read3CycleCount));
      }
    }

  }

  /**
   * In SR : count cycles final = 50 only for read1 In PE : count cycles final =
   * 208, ie read1:100 (instead of 101), read2:107, read3:208.
   * @param data result data object
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

  /**
   * Remove temporary files
   */
  public void clear() {
  }

  //
  // Inner class
  //

  /**
   * This class contains all error values for a lane extracted from binary file
   * (ErrorMetricsOut.bin in InterOp directory).
   * @author Sandrine Perrin
   * @since 1.1
   */
  private static final class ErrorRatesPerLane {

    private final int laneNumber;
    private final int readNumber;

    private int threshold_35_cycle = -1;
    private int threshold_75_cycle = -1;
    private int threshold_100_cycle = -1;

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
    private final ListMultimap<Integer, Number> allErrorRates = ArrayListMultimap
        .create();

    // Save pair tile-rate error for cycles (1 to 35) for all tiles
    private final ListMultimap<Integer, Number> error35 = ArrayListMultimap.create();

    // Save pair tile-rate error for cycles (1 to 75) for all tiles, for run PE
    private final ListMultimap<Integer, Number> error75 = ArrayListMultimap.create();

    // Save pair tile-rate error for cycles (1 to 100) for all tiles, for run PE
    private final ListMultimap<Integer, Number> error100 = ArrayListMultimap.create();

    /**
     * Save a record from TileMetricsOut.bin file.
     * @param iem Illumina tile metrics
     */
    public void addMetric(final IlluminaErrorMetrics iem) {

      allErrorRates.put(iem.getTileNumber(), iem.getErrorRate());
      int cycle = iem.getCycleNumber();

      if (cycle <= threshold_35_cycle) {
        error35.put(iem.getTileNumber(), iem.getErrorRate());
      }

      // Threshold = 0 in run SR
      if (cycle <= threshold_75_cycle) {
        error75.put(iem.getTileNumber(), iem.getErrorRate());
      }

      // Threshold = 0 in run SR
      if (cycle <= threshold_100_cycle) {
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

          Collection<Number> value = entry.getValue();
          List<Number> list =
            Arrays.asList(value.toArray(new Number[value.size()]));

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
      data.put(key + ".called.cycles.max", calledCyclesMax);
      data.put(key + ".called.cycles.min", calledCyclesMin);

      return data;
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
     * @param asEmpty if true, all values are default values (0.0),
     *          corresponding to a control lane or without skipping Phix
     */
    ErrorRatesPerLane(final int lane, final int read,
        final boolean asEmpty, final int read1CycleCount,
        final int read2LastCycleNumber, final int read3CycleCount) {
      this.laneNumber = lane;
      this.readNumber = read;

      if (!asEmpty) {
        int firstCycleNumber = -1;
        int readCycleCount = -1;

        switch (readNumber) {
        case 1:
          firstCycleNumber = 0;
          readCycleCount = read1CycleCount;
          calledCyclesMin = firstCycleNumber + readCycleCount - 1;
          calledCyclesMax = firstCycleNumber + readCycleCount - 1;
          break;

        case 3:
          firstCycleNumber = read2LastCycleNumber + 1;
          readCycleCount = read3CycleCount;
          calledCyclesMin = firstCycleNumber + readCycleCount - 1;
          calledCyclesMax = firstCycleNumber + readCycleCount - 1;
          break;
        default:
          calledCyclesMax = 0;
          calledCyclesMin = 0;
        }

        // check threshold computed > at the cycle count
        // no error rate for read2 (index), cycle absent from
        // ErrorMetricsOut.bin
        if (35 <= readCycleCount && readNumber != 2)
          threshold_35_cycle = firstCycleNumber + 35;

        if (75 <= readCycleCount && readNumber != 2)
          threshold_75_cycle = firstCycleNumber + 75;

        if (100 <= readCycleCount && readNumber != 2)
          threshold_100_cycle = firstCycleNumber + 100;

      }

    }

    ErrorRatesPerLane(final int lane, final int read,
        final int read1CycleCount, final int read2LastCycleNumber,
        final int read3CycleCount) {
      this(lane, read, false, read1CycleCount,
          read2LastCycleNumber, read3CycleCount);
    }
  }

}
