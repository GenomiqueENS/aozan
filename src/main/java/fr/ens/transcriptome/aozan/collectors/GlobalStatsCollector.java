package fr.ens.transcriptome.aozan.collectors;

import java.util.List;
import java.util.Properties;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.util.StatisticsUtils;

/**
 * This class define a Collector for global statistics of the run.
 * @since 1.3
 * @author Laurent Jourdren
 * @author Sandrine Perrin
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
    // Nothing to do
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    final int laneCount = data.getInt("run.info.flow.cell.lane.count");
    final int readCount = data.getInt("run.info.read.count");
    final int tiles = data.getInt("read1.lane1.tile.count");

    final long rawClusterSum;
    final long pfClusterSum;
    final long phixClusterSum;

    int cyclesSum = 0;
    int nonIndexedCyclesSum = 0;
    int indexesCount = 0;

    final StatisticsUtils rawStats = new StatisticsUtils();
    final StatisticsUtils pfStats = new StatisticsUtils();
    final StatisticsUtils phixStats = new StatisticsUtils();

    for (int lane = 1; lane <= laneCount; lane++) {

      // Cluster count per tile
      rawStats.addValues(data.getInt("read1.lane" + lane + ".clusters.raw"));
      pfStats.addValues(data.getInt("read1.lane" + lane + ".clusters.pf"));

      final double alignPhixPrc =
          data.getDouble("read1.lane" + lane + ".prc.align") / 100.0;
      final int rawClusterCount =
          data.getInt("read1.lane" + lane + ".clusters.raw");
      phixStats
          .addValues(new Double(rawClusterCount * alignPhixPrc).intValue());

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

    rawClusterSum = rawStats.getSumToInteger();
    pfClusterSum = pfStats.getSumToInteger();
    phixClusterSum = phixStats.getSumToInteger();

    // Set raw clusters data
    data.put(COLLECTOR_PREFIX + "clusters.raw.count", rawClusterSum);
    data.put(COLLECTOR_PREFIX + "clusters.raw.mediane",
        rawStats.getMedianeToInteger());
    data.put(COLLECTOR_PREFIX + "clusters.raw.mean",
        rawStats.getMeanToInteger());
    data.put(COLLECTOR_PREFIX + "clusters.raw.sd",
        rawStats.getStandardDeviationToInteger());

    // Set passing filter data
    data.put(COLLECTOR_PREFIX + "clusters.pf.count", pfClusterSum);
    data.put(COLLECTOR_PREFIX + "clusters.pf.mediane",
        pfStats.getMedianeToInteger());
    data.put(COLLECTOR_PREFIX + "clusters.pf.mean", pfStats.getMeanToInteger());
    data.put(COLLECTOR_PREFIX + "clusters.pf.sd",
        pfStats.getStandardDeviationToInteger());
    data.put(COLLECTOR_PREFIX + "prc.pf.clusters", (double) pfClusterSum
        / rawClusterSum);

    // Set phix align data
    data.put(COLLECTOR_PREFIX + "clusters.raw.phix.count", phixClusterSum);
    data.put(COLLECTOR_PREFIX + "clusters.raw.phix.mediane",
        phixStats.getMedianeToInteger());
    data.put(COLLECTOR_PREFIX + "clusters.raw.phix.mean",
        phixStats.getMeanToInteger());
    data.put(COLLECTOR_PREFIX + "clusters.raw.phix.sd",
        phixStats.getStandardDeviationToInteger());
    data.put(COLLECTOR_PREFIX + "prc.pf.clusters.phix", (double) phixClusterSum
        / rawClusterSum);

    // Run data
    data.put(COLLECTOR_PREFIX + "cycles", cyclesSum);
    data.put(COLLECTOR_PREFIX + "cycles.non.indexed", nonIndexedCyclesSum);
    data.put(COLLECTOR_PREFIX + "indexes", indexesCount);
    data.put(COLLECTOR_PREFIX + "bases", rawClusterSum * cyclesSum * tiles);
    data.put(COLLECTOR_PREFIX + "bases.non.indexed", rawClusterSum
        * nonIndexedCyclesSum * tiles);

  }

  @Override
  public void clear() {
  }

  public GlobalStatsCollector() {
  }

}
