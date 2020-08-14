package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.aozan3.DataType.Category.RAW;
import static fr.ens.biologie.genomique.aozan.aozan3.DataType.SequencingTechnology.ILLUMINA;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.CategoryDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.DataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.MultiDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.datatypefilter.TechnologyDataTypeFilter;
import fr.ens.biologie.genomique.aozan.aozan3.util.RSync;

/**
 * This class define an Illumina synchronization data processor.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IlluminaSyncDataProcessor extends SyncDataProcessor {

  public static final String PROCESSOR_NAME = "illumina_sync";

  @Override
  public String getName() {
    return PROCESSOR_NAME;
  }

  @Override
  public Set<DataTypeFilter> getInputRequirements() {

    DataTypeFilter filter =
        new MultiDataTypeFilter(new CategoryDataTypeFilter(RAW),
            new TechnologyDataTypeFilter(ILLUMINA));

    return Collections.singleton(filter);
  }

  protected void sync(Path inputPath, Path outputPath) throws IOException {

    RSync rsync =
        new RSync(inputPath, outputPath, 0, Collections.<String> emptyList());
    rsync.sync();
  }

  protected void partialSync(Path inputPath, Path outputPath)
      throws IOException {
    RSync rsync =
        new RSync(inputPath, outputPath, 0, Arrays.asList("*.bin", "*.txt"));
    rsync.sync();
  }

}
