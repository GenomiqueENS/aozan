package fr.ens.biologie.genomique.aozan.collectors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.kenetre.illumina.PrimaryAnalysisMetrics;

/**
 * This collector collect data from the PrimaryAnalysisMetrics.csv file if
 * exists.
 * @since 3.0
 * @author Laurent Jourdren
 */
public class PrimaryAnalysisMetricsCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "primaryanalysismetrics";

  /** Prefix for run data */
  public static final String PREFIX = "primary.analysis.metrics";

  private File primaryAnalysisMetricsFile;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(AozanCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(QC qc, CollectorConfiguration conf) {

    if (conf == null) {
      return;
    }

    this.primaryAnalysisMetricsFile =
        new File(new File(qc.getBclDir(), "PrimaryAnalysisMetrics"),
            "PrimaryAnalysisMetrics.csv");
  }

  @Override
  public void collect(RunData data) throws AozanException {

    if (data == null
        || !Files.isReadable(this.primaryAnalysisMetricsFile.toPath())) {
      return;
    }

    try {
      PrimaryAnalysisMetrics metrics =
          new PrimaryAnalysisMetrics(this.primaryAnalysisMetricsFile);

      data.put(PREFIX + ".average.q30", metrics.getAverageQ30());
      data.put(PREFIX + ".total.yield", metrics.getTotalYield());
      data.put(PREFIX + ".total.readsPF", metrics.getTotalReadsPF());
      data.put(PREFIX + ".loading.concentration.percent",
          metrics.getLoadingConcentrationPercent());

    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  @Override
  public void clear() {
  }

  @Override
  public boolean isSummaryCollector() {
    return false;
  }

}
