package fr.ens.biologie.genomique.aozan.collectors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.kenetre.illumina.DemultiplexStats;
import fr.ens.biologie.genomique.kenetre.illumina.QualityMetrics;

/**
 * This collector parse Demultiplex_Stats.csv file generated by bcl-convert.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class DemultiplexStatsCollector extends DemultiplexingCollector {

  /** The Constant ALL_NAME_KEY. */
  private static final String ALL_NAME_KEY = "all";

  /** The Constant UNDETERMINED_NAME_KEY. */
  private static final String UNDETERMINED_NAME_KEY = "Undetermined";

  /** The Bcl2fastq output path. */
  private String bcl2FastqOutputPath;

  private static class PairEntry {

    private final DemultiplexStats.Entry demultiplexStats;
    private QualityMetrics.Entry qualityMetrics;

    String key() {

      return this.demultiplexStats.getLane()
          + "\t" + this.demultiplexStats.getSampleID();
    }

    static final String key(QualityMetrics.Entry entry) {

      return entry.getLane() + "\t" + entry.getSampleID();
    }

    private PairEntry(DemultiplexStats.Entry entry) {

      Objects.requireNonNull(entry);

      this.demultiplexStats = entry;
    }
  }

  @Override
  public void configure(final QC qc, final CollectorConfiguration conf) {

    if (conf == null) {
      return;
    }

    this.bcl2FastqOutputPath = qc.getFastqDir().getPath();
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null) {
      return;
    }

    // Demultiplexing stats CSV file
    final File demuxSummaryFile =
        new File(this.bcl2FastqOutputPath + "/Reports/Demultiplex_Stats.csv");

    if (!demuxSummaryFile.exists()) {
      throw new AozanException(
          "Demultiplexing Collector: source file not exists "
              + demuxSummaryFile);
    }

    // Quality metrics CSV file
    File qualityMetricsFile =
        new File(this.bcl2FastqOutputPath + "/Reports/Quality_Metrics.csv");

    try {
      collect(data, demuxSummaryFile,
          qualityMetricsFile.exists() ? qualityMetricsFile : null);
    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Collect values.
   * @param runData the RunData object
   * @param demuxSummaryFile CVS file to parse
   * @throws IOException if an error occurs while reading the CSV file
   */
  private static void collect(final RunData runData,
      final File demuxSummaryFile, final File qualityMetrics)
      throws IOException {

    List<Integer> runReads = runReads(runData);

    Map<String, PairEntry> pairEntries = new HashMap<>();

    DemultiplexStats ds = new DemultiplexStats(demuxSummaryFile);
    QualityMetrics qm =
        qualityMetrics == null ? null : new QualityMetrics(qualityMetrics);

    for (DemultiplexStats.Entry entry : ds.entries()) {
      PairEntry e = new PairEntry(entry);
      pairEntries.put(e.key(), e);
    }

    if (qm != null) {

      for (QualityMetrics.Entry entry : qm.entries()) {

        String key = PairEntry.key(entry);

        if (!pairEntries.containsKey(key)) {
          throw new IllegalStateException("Unknow sample: Lane "
              + entry.getLane() + " Sample " + entry.getSampleID());
        }

        pairEntries.get(key).qualityMetrics = entry;
      }
    }

    Map<String, Integer> indexMap = new HashMap<>();
    int count = 0;

    for (PairEntry entry : pairEntries.values()) {

      if (UNDETERMINED_NAME_KEY.equals(entry.demultiplexStats.getSampleID())) {

        // Create entries for undetermined clusters
        addUndeterminedStats(runData, runReads, entry);

      } else {

        String index = entry.demultiplexStats.getIndex();
        if (!indexMap.containsKey(index)) {
          indexMap.put(index, count++);
        }
        int sampleNumber = indexMap.get(index);

        // Create entries for sample clusters
        addSampleStats(runData, runReads, sampleNumber, entry);
      }

    }

  }

  /**
   * Add statistics in the RunData object for an undetermined entry.
   * @param runData RunData object
   * @param runReads list of reads
   * @param e Parsed CVS entry
   */
  private static void addUndeterminedStats(RunData runData,
      List<Integer> runReads, PairEntry e) {

    int lane = e.demultiplexStats.getLane();
    String sampleID = e.demultiplexStats.getSampleID();
    String index = e.demultiplexStats.getIndex();

    // Add barcodes
    runData.put(
        String.format(PREFIX + ".lane%s.sample.lane%s.barcode", lane, lane),
        index);

    for (int read : runReads) {

      int readSize =
          runData.getInt(String.format("run.info.read%s.cycles", read));

      List<String> prefixes = new ArrayList<>();
      prefixes.add(String.format(PREFIX + ".lane%s.sample.%s.read%d.%s", lane,
          sampleID, read, "Raw"));
      prefixes.add(String.format(PREFIX + ".lane%s.sample.%s.read%d.%s", lane,
          sampleID, read, "Pf"));

      prefixes.add(String.format(PREFIX + ".lane%s.sample.lane%s.read%d.%s",
          lane, lane, read, "Raw"));
      prefixes.add(String.format(PREFIX + ".lane%s.sample.lane%s.read%d.%s",
          lane, lane, read, "Pf"));

      populateRunData(e, runData, prefixes, readSize);
    }
  }

  /**
   * Add statistics in the RunData object for a sample entry.
   * @param runData RunData object
   * @param runReads list of reads
   * @param e Parsed CVS entry
   */
  private static void addSampleStats(RunData runData, List<Integer> runReads,
      int sampleNumber, PairEntry e) {

    int lane = e.demultiplexStats.getLane();
    String sampleID = e.demultiplexStats.getSampleID();
    String index = e.demultiplexStats.getIndex();

    // Add barcodes
    runData.put(String.format(PREFIX + ".lane%s.sample.%s.barcode%s", lane,
        sampleID, (sampleNumber == 0 ? "" : sampleNumber)), index);

    for (int read : runReads) {

      int readSize =
          runData.getInt(String.format("run.info.read%d.cycles", read));

      List<String> prefixes = new ArrayList<>();
      prefixes.add(String.format(PREFIX + ".lane%s.sample.%s.read%d.%s.%s",
          lane, sampleID, read, index, "Raw"));
      prefixes.add(String.format(PREFIX + ".lane%s.sample.%s.read%d.%s.%s",
          lane, sampleID, read, index, "Pf"));

      prefixes.add(String.format(PREFIX + ".lane%s.sample.%s.read%d.%s", lane,
          sampleID, read, "Raw"));
      prefixes.add(String.format(PREFIX + ".lane%s.sample.%s.read%d.%s", lane,
          sampleID, read, "Pf"));

      prefixes.add(String.format(PREFIX + ".lane%s.%s.read%d.%s", lane,
          ALL_NAME_KEY, read, "Raw"));
      prefixes.add(String.format(PREFIX + ".lane%s.%s.read%d.%s", lane,
          ALL_NAME_KEY, read, "Pf"));

      populateRunData(e, runData, prefixes, readSize);
    }
  }

  /**
   * Populate the RunData object with the CVS entry. If a key already exists,
   * add the value to the existing value.
   * @param entry entry to use
   * @param runData the runData
   * @param prefixes prefixes to use
   * @param readSize the read size
   */
  private static void populateRunData(PairEntry entry, RunData runData,
      List<String> prefixes, int readSize) {

    for (String prefix : prefixes) {

      add(runData, prefix, ".cluster.count",
          entry.demultiplexStats.getReadCount());

      add(runData, prefix, ".perfect.index.read.count",
          entry.demultiplexStats.getPerfectIndexReadCount());
      add(runData, prefix, ".one.mismatch.index.read.count",
          entry.demultiplexStats.getOneMismatchIndexReadCount());

      // BCLConvert version < 3.9
      if (entry.qualityMetrics == null) {

        long yield = ((long) entry.demultiplexStats.getReadCount()) * readSize;
        add(runData, prefix, ".yield", yield);
        add(runData, prefix, ".yield.q30",
            entry.demultiplexStats.getQ30BaseCount());

        long qualityScoreSum =
            (long) ((((long) entry.demultiplexStats.getReadCount()) * readSize)
                * entry.demultiplexStats.getMeanPFQualityScore());
        add(runData, prefix, ".quality.score.sum", qualityScoreSum);

      } else {

        add(runData, prefix, ".yield", entry.qualityMetrics.getYield());
        add(runData, prefix, ".yield.q30", entry.qualityMetrics.getYieldQ30());

        long qualityScoreSum =
            (long) ((((long) entry.demultiplexStats.getReadCount()) * readSize)
                * entry.qualityMetrics.getMeanQualityScorePF());
        add(runData, prefix, ".quality.score.sum", qualityScoreSum);
      }

    }

  }

  /**
   * Get the read number list.
   * @param runData RunData object
   * @return a list with read number
   */
  private static List<Integer> runReads(RunData runData) {

    int max = runData.getInt("run.info.read.count");
    List<Integer> result = new ArrayList<>(max);

    int count = 0;

    for (int read = 1; read <= max; read++) {
      if (!runData.getBoolean("run.info.read" + read + ".indexed")) {
        result.add(++count);
      }
    }

    return result;
  }

  /**
   * Add an entry to run data.
   * @param runData run data
   * @param prefix prefix of the key
   * @param key key to add
   * @param l value to add
   */
  private static void add(RunData runData, String prefix, String key, long l) {

    String k = prefix + key;

    if (!runData.contains(k)) {
      runData.put(k, 0);
    }

    runData.put(k, runData.getLong(k) + l);
  }

}
