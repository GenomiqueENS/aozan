package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.util.List;
import java.util.Properties;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;

public class ProjectStatsCollector extends AbstractFastqCollector {

  private static final String COLLECTOR_NAME = "projectstats";

  private static final String COLLECTOR_PREFIX = "projectstats.";

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void configure(Properties properties) {
    // TODO Auto-generated method stub

  }

  @Override
  public void collect(RunData data) throws AozanException {
    // TODO Auto-generated method stub

  }

  @Override
  public void clear() {
    // TODO Auto-generated method stub

  }

  @Override
  protected AbstractFastqProcessThread collectSample(RunData data,
      FastqSample fastqSample, File reportDir, boolean runPE)
      throws AozanException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected int getThreadsNumber() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  protected boolean isProcessUndeterminedIndicesSamples() {
    // TODO Auto-generated method stub
    return false;
  }

}
