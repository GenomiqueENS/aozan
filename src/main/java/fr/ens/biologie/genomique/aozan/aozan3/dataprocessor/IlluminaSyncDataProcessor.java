package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import fr.ens.biologie.genomique.aozan.aozan3.AozanLogger;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
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

  //
  // Constructor
  //

  public IlluminaSyncDataProcessor(final Configuration conf,
      final AozanLogger logger) throws IOException {

    super(conf, logger);
  }

}
