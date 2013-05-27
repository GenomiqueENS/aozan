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

package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.interopfile.AbstractBinaryIteratorReader.IlluminaMetrics;
import fr.ens.transcriptome.aozan.collectors.interopfile.ErrorMetricsOutIterator.IlluminaErrorMetrics;

/**
 * This class collects run data by reading the ErrorMetricsOut.bin in InterOp
 * directory. This file exists if the lane is spiking with PhiX, if it doesn't
 * exist the values are been defined at 0.0.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ErrorMetricsOutReader extends AbstractBinaryInterOpReader {
  /** Logger */
  // private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private String dirInterOpPath;
  static int seuil35ForRead = 35;
  static int seuil75ForRead = 75; // optional
  static int seuil100ForRead = 100; // optional
  private final Map<LaneRead, ErrorRatesPerLane> errorMetricsPerLaneRead =
      new TreeMap<LaneRead, ErrorRatesPerLane>();

  private Map<Integer, Integer> readLastCycle;

  // private int numberCycleForRead1;

  /**
   * Collect data.
   * @param data result data object
   */
  @Override
  public void collect(final RunData data) throws AozanException {
    // parse map, each entry is added in data
    super.collect(data);

    // this.numberCycleForRead1 = data.getInt("run.info.read1.cycles") - 1;
    this.readLastCycle = getCyclesEndRead(data);
    // System.out.println("ERROR value seuil reads " + numberCycleForRead1);

    parseCollection(data);

    // System.out.println("ERROR size internal map "
    // + errorMetricsPerLaneRead.size());

    for (Map.Entry<LaneRead, ErrorRatesPerLane> e : errorMetricsPerLaneRead
        .entrySet()) {
      // System.out.println(e.getKey() + " \t" + e.getValue());
      String key =
          "read" + e.getKey().getRead() + ".lane" + e.getKey().getLane();
      data.put(key + ".err.rate.100", e.getValue().getErrorRateCycle100());
      data.put(key + ".err.rate.100.sd", e.getValue().getErrorRateCycle100SD());
      data.put(key + ".err.rate.35", e.getValue().getErrorRateCycle35());
      data.put(key + ".err.rate.35.sd", e.getValue().getErrorRateCycle35SD());
      data.put(key + ".err.rate.75", e.getValue().getErrorRateCycle75());
      data.put(key + ".err.rate.75.sd", e.getValue().getErrorRateCycle75SD());
      data.put(key + ".err.rate.phix", e.getValue().getErrorRate());
      data.put(key + ".err.rate.phix.sd", e.getValue().getErrorRateSD());

      data.put(key + ".called.cycles.max", 0);
      data.put(key + ".called.cycles.min", 0);

      if (TestCollectorReadBinary.PRINT_DETAIL)
        if (e.getKey().getRead() != 2)
          System.out.println(e.getKey() + " " + e.getValue());
      // LOGGER.info(e.getKey() + "  " + e.getValue());
    }

  }

  /**
   * Compute values necessary from collection of records provided by a binary
   * file.
   * @param data on run
   * @throws AozanException it occurs during the initialize the object iterable
   *           on binary file.
   */
  private void parseCollection(RunData data) throws AozanException {


    if (!ErrorMetricsOutIterator.errorMetricsOutFileExists())
      collectionEmpty(1, lanes);

    else {
      ErrorMetricsOutIterator binIterator =
          new ErrorMetricsOutIterator();
      Collection<IlluminaMetrics> collection = makeCollection(binIterator);
      // Map<Integer, Integer> cyclesStartRead = getCyclesEndRead(data);

      // System.out.println("ERROR cycles min "
      // + IlluminaErrorMetrics.minCycle + " max "
      // + IlluminaErrorMetrics.maxCycle);

      for (int lane = 1; lane <= lanes; lane++) {

        if (data.getBoolean("run.info.align.to.phix.lane" + lane)) {
          collectPerLaneRead(lane, collection);

        } else {
          // None phix in this lane, all value error are 0
          collectionEmpty(lane, lane);
        }
      }// for lane
    }
  }

  /**
   * Compute values needed.
   * @param lane number lane
   * @param collection set of record from binary file.
   */
  private void collectPerLaneRead(final int lane,
      final Collection<IlluminaMetrics> collection) {
    seuil35ForRead = 35;
    seuil75ForRead = 75;
    seuil100ForRead = 100;

    // Save pair tile - rate error for all cycles for a lane
    ListMultimap<Integer, Number> sumErrorRate = ArrayListMultimap.create();

    // Save pair tile - rate error for cycles (1 to 35) for a lane
    ListMultimap<Integer, Number> error35 = ArrayListMultimap.create();

    // Save pair tile - rate error for cycles (1 to 75) for a lane
    ListMultimap<Integer, Number> error75 = ArrayListMultimap.create();

    // Save pair tile - rate error for cycles (1 to 100) for a lane
    ListMultimap<Integer, Number> error100 = ArrayListMultimap.create();

    for (int cycle = IlluminaErrorMetrics.minCycle; cycle <= IlluminaErrorMetrics.maxCycle; cycle++) {

      // Parse number tile
      for (Integer tile : IlluminaErrorMetrics.getTilesNumberList()) {

        for (IlluminaMetrics im : collection) {
          IlluminaErrorMetrics iem = (IlluminaErrorMetrics) im;

          if (iem.getLaneNumber() == lane
              && iem.getTileNumber() == tile && iem.getCycleNumber() == cycle) {

            sumErrorRate.put(tile, iem.getErrorRate());
          }
        }
      }// for tile

      if (cycle == seuil35ForRead)
        error35.putAll(sumErrorRate);
      else if (cycle == seuil75ForRead)
        error75.putAll(sumErrorRate);
      else if (cycle == seuil100ForRead)
        error100.putAll(sumErrorRate);

      toto(sumErrorRate, error35, error75, error100, lane, cycle);
    }
  }

  private void toto(ListMultimap<Integer, Number> sumErrorRate,
      ListMultimap<Integer, Number> error35,
      ListMultimap<Integer, Number> error75,
      ListMultimap<Integer, Number> error100, final int lane, final int cycle) {

    for (Map.Entry<Integer, Integer> e : readLastCycle.entrySet()) {
      boolean initErrorMap = false;
      if (cycle == e.getValue()) {
        // For read2 (index) : default values (0.0)
        if (e.getKey() == 2) {
          errorMetricsPerLaneRead.put(new LaneRead(lane, e.getKey()),
              new ErrorRatesPerLane());
          initErrorMap = true;

        } else {
          errorMetricsPerLaneRead.put(new LaneRead(lane, e.getKey()),
              new ErrorRatesPerLane(sumErrorRate, error35, error75, error100,
                  lane, e.getKey()));
          initErrorMap = true;
        }

        if (initErrorMap) {
          // Re-initialize ListMultimap error for the next read
          sumErrorRate.clear();
          error35.clear();
          error75.clear();
          error100.clear();

          // Re-initialize seuil values for the new read
          seuil35ForRead = cycle + 35;
          seuil75ForRead = cycle + 75;
          seuil100ForRead = cycle + 100;
        }
      }

    }

    // // In SR count cycle = 50
    // // Check if cycle is the last for read 1
    // if (cycle == numberCycleForRead1) {
    //
    // errorMetricsPerLaneRead.put(new LaneRead(lane, 1),
    // new ErrorRatesPerLane(sumErrorRate, error35, error75, error100));
    //
    // // Check if cycle is the last for read3
    // } else if (cycle == IlluminaErrorMetrics.maxCycle) {
    //
    // // TODO test with paired, if record for read2 are set in file.
    // errorMetricsPerLaneRead.put(new LaneRead(lane, 3),
    // new ErrorRatesPerLane(sumErrorRate, error35, error75, error100));
    // }
    // } // for cycle
    //
    // // TODO to check with a PE run
    // // None cycle corresponding for read2 (index)
    // if (reads >= 2) {
    // // Put read2 for index, values null
    // errorMetricsPerLaneRead.put(new LaneRead(lane, 2),
    // new ErrorRatesPerLane());
    // }

  }

  /**
   * Add in error metrics map a new instance of errorRatesPerLaneRead with
   * default values, none metrics in file for the pair lane-read.
   * @param firstLane number lane
   * @param lastLane number lane
   */
  private void collectionEmpty(final int firstLane, final int lastLane) {

    for (int lane = firstLane; lane <= lastLane; lane++) {
      for (int read = 1; read <= reads; read++)
        errorMetricsPerLaneRead.put(new LaneRead(lane, read),
            new ErrorRatesPerLane());
    }

  }

  /**
   * In SR : count cycles final = 50 only for read1 In PE : count cycles final =
   * 208, ie read1:100 (instead of 101), read2:7, read3:101.
   * @param data
   * @return
   */
  private static final Map<Integer, Integer> getCyclesEndRead(final RunData data) {

    // run.info.read.count=2
    // run.info.read1.cycles=51

    final Map<Integer, Integer> result = Maps.newLinkedHashMap();

    final int readCount = data.getInt("run.info.read.count");

    int cyclesCount = 0;

    for (int read = 1; read <= readCount; read++) {

      if (read == 2 && readCount == 2) {
        // Case : SR
        result.put(read, cyclesCount - 1);
      } else {
        cyclesCount += data.getInt("run.info.read" + read + ".cycles");
        result.put(read, cyclesCount - 1);
      }

    }
    System.out.println("read/last cycles " + result);
    return result;
  }

  //
  // Constructor
  //

  public ErrorMetricsOutReader() {
    // TODO Auto-generated constructor stub
  }

  public ErrorMetricsOutReader(final String dirInterOpPath) {
    this.dirInterOpPath = dirInterOpPath;
  }
}
