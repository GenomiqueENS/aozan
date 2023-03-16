package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunDataFactory;
import fr.ens.biologie.genomique.aozan.aozan3.SequencerSource;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define a processed data provider for Illumina runs.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IlluminaProcessedRunDataProvider implements RunDataProvider {

  public static final String PROVIDER_NAME = "illumina_fastq";

  private DataStorage storage;
  private SequencerSource name;
  private boolean initialized;

  private static final class RunDirectoryFileFilter implements FileFilter {

    private final boolean completedDemux;

    @Override
    public boolean accept(final File file) {

      // File must be a directory
      if (!file.isDirectory()) {
        return false;
      }

      // A RunInfo file must exists
      if (!isFastqFilesInDirectory(file)) {
        return false;
      }

      boolean interOpExists =
          new File(file, "InterOp/IndexMetricsOut.bin").isFile();

      boolean fastqCompleteExists =
          new File(file, "Logs/FastqComplete.txt").isFile();

      if (this.completedDemux) {
        return interOpExists || fastqCompleteExists;
      }

      return true;
    }

    RunDirectoryFileFilter(boolean completedDemux) {
      this.completedDemux = completedDemux;
    }

  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public void init(DataStorage storage, Configuration conf,
      GenericLogger logger) throws Aozan3Exception {

    // Check if step has not been already initialized
    if (this.initialized) {
      throw new IllegalStateException();
    }

    this.name = storage.getSequencerSource();
    this.storage = storage;

    this.initialized = true;
  }

  @Override
  public boolean canProvideRunData() {

    checkInitialization();

    return true;
  }

  @Override
  public List<RunData> listInProgressRunData() {

    checkInitialization();

    return listRuns(false);
  }

  @Override
  public List<RunData> listCompletedRunData() {

    checkInitialization();

    return listRuns(true);
  }

  @Override
  public DataStorage getDataStorage() {

    checkInitialization();

    return this.storage;
  }

  //
  // Other methods
  //

  /**
   * Check if step has been initialized.
   */
  private void checkInitialization() {

    if (!this.initialized) {
      throw new IllegalStateException();
    }
  }

  private static boolean isFastqFilesInDirectory(File dir) {

    try (Stream<Path> walk =
        Files.walk(dir.toPath(), FileVisitOption.FOLLOW_LINKS)) {

      long count = walk.map(x -> x.toString())
          .filter(f -> f.endsWith(".fastq.gz") || f.endsWith(".fastq.bz2"))
          .count();

      return count > 0 ? true : false;

    } catch (IOException e) {
      return false;
    }

  }

  private List<RunData> listRuns(boolean completedRuns) {

    List<RunData> result = new ArrayList<>();

    File[] runDirectories = this.storage.getPath().toFile()
        .listFiles(new RunDirectoryFileFilter(completedRuns));

    if (runDirectories != null) {
      for (File f : runDirectories) {
        result.add(completedRuns
            ? RunDataFactory.newProcessedIlluminaRunData(this.storage,
                f.toPath(), this.name)
            : RunDataFactory.newPartialProcessedIlluminaRunData(this.storage,
                f.toPath(), this.name));
      }
    }

    return Collections.unmodifiableList(result);
  }

}
