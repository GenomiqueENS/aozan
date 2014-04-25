package fr.ens.transcriptome.aozan.collectors;

import java.util.List;
import java.util.Properties;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;

/**
 * This class define a Collector for global statistics of the run.
 * @since 1.x
 * @author Laurent Jourdren
 */
public class GlobalStatsCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "globalstats";

  private static final String COLLECTOR_PREFIX = "globalstats.";

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(RunInfoCollector.COLLECTOR_NAME,
        ReadCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final Properties properties) {

    System.out.println("coucou from configure() " + getName());
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    final int laneCount = data.getInt("run.info.flow.cell.lane.count");
    final int readCount = data.getInt("run.info.read.count");
    final int tiles = data.getInt("read1.lane1.tile.count");

    long rawClusterSum = 0;
    long pfClusterSum = 0;
    int cyclesSum = 0;
    int nonIndexedCyclesSum = 0;
    int indexesCount = 0;

    for (int lane = 1; lane <= laneCount; lane++) {

      rawClusterSum += data.getInt("read1.lane" + lane + ".clusters.raw");
      pfClusterSum += data.getInt("read1.lane" + lane + ".clusters.pf");

      for (int read = 1; read <= readCount; read++) {

        final boolean indexedRead =
            data.getBoolean("run.info.read" + read + ".indexed");
        final int cycles = data.getInt("run.info.read" + read + ".cycles");

        if (lane == 1) {

          cyclesSum += cycles;

          if (indexedRead) {
            indexesCount++;
          } else {
            nonIndexedCyclesSum += cycles;
          }
        }
      }
    }

    data.put(COLLECTOR_PREFIX + "clusters.raw", rawClusterSum);
    data.put(COLLECTOR_PREFIX + "clusters.pf", pfClusterSum);
    data.put(COLLECTOR_PREFIX + "cycles", cyclesSum);
    data.put(COLLECTOR_PREFIX + "cycles.non.indexed", nonIndexedCyclesSum);
    data.put(COLLECTOR_PREFIX + "indexes", indexesCount);
    data.put(COLLECTOR_PREFIX + "prc.pf.clusters", (double) pfClusterSum
        / rawClusterSum);
    data.put(COLLECTOR_PREFIX + "bases", rawClusterSum * cyclesSum * tiles);
    data.put(COLLECTOR_PREFIX + "bases.non.indexed", rawClusterSum
        * nonIndexedCyclesSum * tiles);
  }

  @Override
  public void clear() {
  }

  public GlobalStatsCollector() {
    System.out.println("coucou from constructor " + getName());
  }

}
