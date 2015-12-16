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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.interop.ReadsData.ReadData;
import fr.ens.transcriptome.aozan.util.StatisticsUtils;

/**
 * This class collects run data by reading the ErrorMetricsOut.bin in InterOp
 * directory. This file exists if the lane is spiking with PhiX, if it doesn't
 * exist the values are been defined at 0.0.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ErrorMetricsCollector extends AbstractMetricsCollector {

  /** The sub-collector name from ReadCollector. */
  public static final String NAME_COLLECTOR = "ErrorMetricsCollector";

  private final Map<Integer, ErrorRatesPerLane> errorRatesMetrics =
      new HashMap<>();

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
      final ErrorMetricsReader reader =
          new ErrorMetricsReader(getInterOpDir());
      initMetricsMap(data);

      int keyMap;

      // Distribution of metrics between lane and code
      for (final IlluminaErrorMetrics iem : reader.getSetIlluminaMetrics()) {
        keyMap =
            getKeyMap(iem.getLaneNumber(),
                getReadFromCycleNumber(iem.getCycleNumber()));

        this.errorRatesMetrics.get(keyMap).addMetric(iem);
      }

    } catch (final FileNotFoundException e) {

      // Case : ErrorMetricsOut.bin doesn't exist, all values are 0.0
      collectionEmpty(1, getLanesCount());

    }

    // Build runData
    for (final Map.Entry<Integer, ErrorRatesPerLane> entry : this.errorRatesMetrics
        .entrySet()) {

      entry.getValue().computeData();
      data.put(entry.getValue().getRunData());
    }

  }

  /**
   * Set unique id for each pair lane-read in a run.
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

    for (int lane = 1; lane <= getLanesCount(); lane++) {
      for (int read = 1; read <= getReadsCount(); read++) {

        // lane skiping with phix
        if (data.getBoolean("run.info.align.to.phix.lane" + lane)) {

          // Extract readData from setting from run
          final ReadData readData = getReadData(read);

          this.errorRatesMetrics.put(getKeyMap(lane, read),
              new ErrorRatesPerLane(lane, read, readData));

        } else {
          // None phix in this lane, all values error are 0
          collectionEmpty(lane, lane);
        }
      }
    }
  }

  /**
   * Add in error metrics map a new instance of errorRatesPerLaneRead with
   * default values, none metrics in file for the pair lane-read.
   * @param firstLane number lane
   * @param lastLane number lane
   */
  private void collectionEmpty(final int firstLane, final int lastLane) {

    for (int lane = firstLane; lane <= lastLane; lane++) {
      for (int read = 1; read <= getReadsCount(); read++) {

        // Extract readData from setting from run
        final ReadData readData = getReadData(read);

        final int keyMap = lane * 100 + read;
        this.errorRatesMetrics.put(keyMap, new ErrorRatesPerLane(lane, read,
            true, readData));
      }
    }
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

    private int threshold35thCycle = -1;
    private int threshold75thCycle = -1;
    private int threshold100thCycle = -1;

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
    private final ListMultimap<Integer, Number> allErrorRates =
        ArrayListMultimap.create();

    // Save pair tile-rate error for cycles (1 to 35) for all tiles
    private final ListMultimap<Integer, Number> error35 = ArrayListMultimap
        .create();

    // Save pair tile-rate error for cycles (1 to 75) for all tiles, for run PE
    private final ListMultimap<Integer, Number> error75 = ArrayListMultimap
        .create();

    // Save pair tile-rate error for cycles (1 to 100) for all tiles, for run PE
    private final ListMultimap<Integer, Number> error100 = ArrayListMultimap
        .create();

    /**
     * Save a record from TileMetricsOut.bin file.
     * @param iem Illumina tile metrics
     */
    public void addMetric(final IlluminaErrorMetrics iem) {

      this.allErrorRates.put(iem.getTileNumber(), iem.getErrorRate());
      final int cycle = iem.getCycleNumber();

      if (cycle <= this.threshold35thCycle) {
        this.error35.put(iem.getTileNumber(), iem.getErrorRate());
      }

      // Threshold = 0 in run SR
      if (cycle <= this.threshold75thCycle) {
        this.error75.put(iem.getTileNumber(), iem.getErrorRate());
      }

      // Threshold = 0 in run SR
      if (cycle <= this.threshold100thCycle) {
        this.error100.put(iem.getTileNumber(), iem.getErrorRate());
      }
    }

    /**
     * Compute mean and standard deviation from metrics reading in
     * ErrorMetricsOut.bin file.
     */
    public void computeData() {

      List<Number> errorRatePerTile;

      if (this.allErrorRates.size() > 0) {
        errorRatePerTile = computeErrorRatePerTile(this.allErrorRates);
        final StatisticsUtils stat = new StatisticsUtils(errorRatePerTile);

        this.errorRate = stat.getMean();
        this.errorRateSD = stat.getStandardDeviation();

      }

      // Check if number cycle > 35, else values are 0.0
      if (this.error35.size() > 0) {
        errorRatePerTile = computeErrorRatePerTile(this.error35);
        final StatisticsUtils stat = new StatisticsUtils(errorRatePerTile);

        this.errorRateCycle35 = stat.getMean();
        this.errorRateCycle35SD = stat.getStandardDeviation();

      }

      // Check if number cycle > 75, else values are 0.0
      if (this.error75.size() > 0) {
        errorRatePerTile = computeErrorRatePerTile(this.error75);
        final StatisticsUtils stat = new StatisticsUtils(errorRatePerTile);

        this.errorRateCycle75 = stat.getMean();
        this.errorRateCycle75SD = stat.getStandardDeviation();

      }

      // Check if number cycle > 100, else values are 0.0
      if (this.error100.size() > 0) {
        errorRatePerTile = computeErrorRatePerTile(this.error100);
        final StatisticsUtils stat = new StatisticsUtils(errorRatePerTile);

        this.errorRateCycle100 = stat.getMean();
        this.errorRateCycle100SD = stat.getStandardDeviation();

      }

      this.dataToCompute = false;
    }

    /**
     * Compute the rate error for each Tiles with the mean by cycle.
     * @param values of rate error per countTiles, per cycles
     * @return list of rate error for each number tile
     */
    public List<Number> computeErrorRatePerTile(
        final ListMultimap<Integer, Number> values) {

      // Define map : tile and list of rate error, one per cycle to use
      final Map<Integer, Collection<Number>> errorValuePerTile = values.asMap();

      // Save rate error per tile
      final List<Number> errorRatePerTile = new ArrayList<>();

      for (final Map.Entry<Integer, Collection<Number>> entry : errorValuePerTile
          .entrySet()) {

        final Collection<Number> value = entry.getValue();
        final List<Number> list =
            Arrays.asList(value.toArray(new Number[value.size()]));

        final StatisticsUtils stat = new StatisticsUtils(list);
        errorRatePerTile.add(stat.getMean());

      }

      return errorRatePerTile;
    }

    /**
     * Save data from error metrics for a run in a RunData.
     * @return rundata data from tile metrics for a run
     */
    public RunData getRunData() {

      if (this.dataToCompute) {
        computeData();
      }

      final RunData data = new RunData();

      final String key = "read" + this.readNumber + ".lane" + this.laneNumber;

      data.put(key + ".err.rate.100", this.errorRateCycle100);
      data.put(key + ".err.rate.100.sd", this.errorRateCycle100SD);
      data.put(key + ".err.rate.35", this.errorRateCycle35);
      data.put(key + ".err.rate.35.sd", this.errorRateCycle35SD);
      data.put(key + ".err.rate.75", this.errorRateCycle75);
      data.put(key + ".err.rate.75.sd", this.errorRateCycle75SD);
      data.put(key + ".err.rate.phix", this.errorRate);
      data.put(key + ".err.rate.phix.sd", this.errorRateSD);
      data.put(key + ".called.cycles.max", this.calledCyclesMax);
      data.put(key + ".called.cycles.min", this.calledCyclesMin);

      return data;
    }

    @Override
    public String toString() {
      return String
          .format(
              "\t%d\t%d\trate %.2f\trate sd %.3f\t35 %.2f\t35 sd %.3f\t75 %.2f\t75 sd %.3f\t100 %.2f\t100 sd %.3f\tmin %d\tmax %d",
              this.laneNumber, this.readNumber, this.errorRate,
              this.errorRateSD, this.errorRateCycle35, this.errorRateCycle35SD,
              this.errorRateCycle75, this.errorRateCycle75SD,
              this.errorRateCycle100, this.errorRateCycle100SD,
              this.calledCyclesMin, this.calledCyclesMax);

    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param lane lane number
     * @param read read number
     * @param empty if true, all values are default values (0.0),
     *          corresponding to a control lane or without skipping Phix
     */
    public ErrorRatesPerLane(final int lane, final int read, boolean empty,
        final ReadData readData) {
      this.laneNumber = lane;
      this.readNumber = read;

      if (!empty) {

        // Compute error rate on not indexed read
        if (!readData.isIndexedRead()) {

          final int readCycleCount = readData.getNumberCycles();
          final int initCycle = readData.getFirstCycleNumber() - 1;

          this.calledCyclesMin = readCycleCount + initCycle - 1;
          this.calledCyclesMax = readCycleCount + initCycle - 1;

          // check threshold computed > at the cycle count
          if (35 <= readCycleCount) {
            this.threshold35thCycle = initCycle + 35;
          }

          if (75 <= readCycleCount) {
            this.threshold75thCycle = initCycle + 75;
          }

          if (100 <= readCycleCount) {
            this.threshold100thCycle = initCycle + 100;
          }
        }
      }
    }

    public ErrorRatesPerLane(final int lane, final int read,
        final ReadData readData) {
      this(lane, read, false, readData);

    }

  }

}
