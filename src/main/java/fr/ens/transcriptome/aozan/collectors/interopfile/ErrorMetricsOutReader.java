package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.interopfile.AbstractBinaryIteratorReader.IlluminaMetrics;
import fr.ens.transcriptome.aozan.collectors.interopfile.ErrorMetricsOutIterator.IlluminaErrorMetrics;

public class ErrorMetricsOutReader extends AbstractBinaryInterOpReader {

  static final int SEUIL_CYCLE_1 = 35;
  static final int SEUIL_CYCLE_2 = 75; // optional
  static final int SEUIL_CYCLE_3 = 100; // optional
  private final Map<LaneRead, ErrorRatesPerLane> internalMap =
      new TreeMap<LaneRead, ErrorRatesPerLane>();

  public void collect(final RunData data) {
    // parse map, each entry is added in data
    super.collect(data);

    try {
      parseCollection(data);

    } catch (Exception e1) {
      e1.printStackTrace();
    }

    System.out.println("ERROR size internal map " + internalMap.size());

    for (Map.Entry<LaneRead, ErrorRatesPerLane> e : internalMap.entrySet()) {
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

      if (e.getKey().getRead() == 1)
        System.out.println(e.getValue());
    }

  }

  private void parseCollection(RunData data) throws Exception {

    if (!ErrorMetricsOutIterator.errorMetricsOutFileExists())
      collectionEmpty(1, lanes);
    else {
      ErrorMetricsOutIterator binIterator = new ErrorMetricsOutIterator();

      Collection<IlluminaMetrics> collection = makeCollection(binIterator);

      System.out.println("collection size " + collection.size());

      System.out.println("minTile "
          + IlluminaErrorMetrics.minTile + " maxTile "
          + IlluminaErrorMetrics.maxTile);
      System.out.println("minCycle "
          + IlluminaErrorMetrics.minCycle + " maxCycle "
          + IlluminaErrorMetrics.maxCycle);
      System.out.println(" nbreads "
          + reads + "  Read1 nb cycles " + read1CyclesCumul);

      for (int lane = 1; lane <= lanes; lane++) {
        int nbNoErrorPerLane = 0;
        int nbNoError = 0;

        if (data.getBoolean("run.info.align.to.phix.lane" + lane)) {

          ListMultimap<Integer, Number> sumErrorRate =
              ArrayListMultimap.create();
          ListMultimap<Integer, Number> error35 = ArrayListMultimap.create();
          ListMultimap<Integer, Number> error75 = ArrayListMultimap.create();
          ListMultimap<Integer, Number> error100 = ArrayListMultimap.create();

          for (int cycle = IlluminaErrorMetrics.minCycle; cycle <= IlluminaErrorMetrics.maxCycle; cycle++) {

            for (int tile = IlluminaErrorMetrics.minTile; tile <= IlluminaErrorMetrics.maxTile; tile++) {

              for (IlluminaMetrics im : collection) {
                IlluminaErrorMetrics iem = (IlluminaErrorMetrics) im;

                if (iem.getLaneNumber() == lane
                    && iem.getTileNumber() == tile
                    && iem.getCycleNumber() == cycle) {

                  // sumErrorRate.add(iem.getErrorRate());
                  sumErrorRate.put(tile, iem.getErrorRate());
                  // nbNoError += (iem.noError() == 0 ? 1 : 0);
                }
              }
            }// for tile

            switch (cycle) {
            case SEUIL_CYCLE_1:
              // error35.addAll(sumErrorRate);
              System.out.println("lane "
                  + lane + " create multimap 35 ?"
                  + error35.putAll(sumErrorRate));

              break;
            case SEUIL_CYCLE_2:
              // error75.addAll(sumErrorRate);
              System.out.println("lane "
                  + lane + " create multimap 75 ?"
                  + error75.putAll(sumErrorRate));
              break;
            case SEUIL_CYCLE_3:
              // error100.addAll(sumErrorRate);
              System.out.println("lane "
                  + lane + " create multimap 100 ?"
                  + error100.putAll(sumErrorRate));
              break;
            }

            // Add in map if the number cycle corresponding at the last
            // cycle of a read
            // Number cycles for each reads is defined in Rundata.
            if (cycle == read1CyclesCumul - 1) {
              // internalMap.put(new LaneRead(lane, 1), new ErrorRatesPerLane(
              // sumErrorRate, error35, error75, error100));

              internalMap.put(new LaneRead(lane, 1), new ErrorRatesPerLane(
                  sumErrorRate, error35, error75, error100));

              // System.out.println(compt
              // + " lane " + lane + " sum " + sum + " size "
              // + sumErrorRate.size() + " 35 " + error35.size());

            } else if (cycle == read3CyclesCumul) {
              internalMap.put(new LaneRead(lane, 3), new ErrorRatesPerLane(
                  sumErrorRate, error35, error75, error100));
            }
          } // for cycle

          if (reads >= 2) {
            // Put read2 for index, values null
            internalMap.put(new LaneRead(lane, 2), new ErrorRatesPerLane());
          }

        } else {
          // None phix in this lane, all value error are zero
          collectionEmpty(lane, lane);
        }
      }// for lane
    }
  }

  private void collectionEmpty(final int firstLane, final int lastLane) {

    for (int lane = firstLane; lane <= lastLane; lane++) {
      for (int read = 1; read <= reads; read++)
        internalMap.put(new LaneRead(lane, read), new ErrorRatesPerLane());
    }

  }
}
