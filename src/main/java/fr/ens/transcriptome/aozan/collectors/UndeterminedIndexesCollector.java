package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * This class allow to collect information about the undetermined indices that
 * can be recovered.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class UndeterminedIndexesCollector extends AbstractFastqCollector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "undeterminedindexes";

  private int numberThreads = Runtime.getRuntime().availableProcessors();
  private int maxMismatches = 1;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  protected boolean isProcessStandardSamples() {

    return false;
  }

  @Override
  protected boolean isProcessUndeterminedIndicesSamples() {

    return true;
  }

  @Override
  protected boolean isProcessAllReads() {

    return false;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    final List<String> result =
        Lists.newArrayList(super.getCollectorsNamesRequiered());
    result.add(FlowcellDemuxSummaryCollector.COLLECTOR_NAME);

    return Collections.unmodifiableList(result);
  }

  @Override
  public void configure(final Properties properties) {

    super.configure(properties);

    // Set the number of threads
    if (properties.containsKey(Settings.QC_CONF_THREADS_KEY)) {

      try {
        int confThreads =
            Integer.parseInt(properties.getProperty(
                Settings.QC_CONF_THREADS_KEY).trim());
        if (confThreads > 0)
          this.numberThreads = confThreads;

      } catch (NumberFormatException ignored) {
      }
    }

    // TODO Parse setting for maximum number of mismatches

    // TODO check demultiplexage make with 1 mismatch, if true all values in
    // runData at 0.
  }

  @Override
  protected int getThreadsNumber() {

    return this.numberThreads;
  }

  @Override
  protected AbstractFastqProcessThread collectSample(final RunData data,
      final FastqSample fastqSample, final File reportDir, boolean runPE)
      throws AozanException {

    if (fastqSample.getFastqFiles().isEmpty()) {
      return null;
    }

    return new UndeterminedIndexesProcessThreads(data, fastqSample,
        this.maxMismatches, reportDir);
  }

}
