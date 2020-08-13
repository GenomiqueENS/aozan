package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.SequencerSource;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;

/**
 * This class define a processed data provider for Illumina runs.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IlluminaProcessedRunDataProvider implements RunDataProvider {

  public static final String PROVIDER_NAME = "illumina_fastq";

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public void init(DataStorage storage, Configuration conf, AozanLogger logger)
      throws Aozan3Exception {
    // TODO Auto-generated method stub
  }

  @Override
  public boolean canProvideRunData() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<RunData> listInProgressRunData() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<RunData> listCompletedRunData() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DataStorage getDataStorage() {
    // TODO Auto-generated method stub
    return null;
  }

}
