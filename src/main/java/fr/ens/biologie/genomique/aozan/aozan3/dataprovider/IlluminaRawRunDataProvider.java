package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.IlluminaUtils;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunDataFactory;
import fr.ens.biologie.genomique.aozan.aozan3.SequencerSource;
import fr.ens.biologie.genomique.aozan.aozan3.util.Utils;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define a processed data provider for Illumina runs.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IlluminaRawRunDataProvider implements RunDataProvider {

  public static final String PROVIDER_NAME = "illumina_bcl";

  private DataStorage storage;
  private SequencerSource name;
  private boolean initialized;

  private static final class RunDirectoryFileFilter implements FileFilter {

    private final boolean completedRuns;

    @Override
    public boolean accept(final File file) {

      // File must be a directory
      if (!file.isDirectory()) {
        return false;
      }

      // Check if the name of the directory is a valid Illumina run id
      if (!IlluminaUtils.checkRunId(Utils.removeTmpExtension(file.getName()))) {
        return false;
      }

      File runInfoFile = new File(file, "RunInfo.xml");

      // A RunInfo file must exists
      if (!runInfoFile.isFile()) {
        return false;
      }

      boolean rtaCompleteExists =
          new File(file, "RunCompletionStatus.xml").isFile();
      boolean tempDirectory = file.getName().endsWith(".tmp");

      return this.completedRuns
          ? rtaCompleteExists && !tempDirectory
          : !rtaCompleteExists || tempDirectory;
    }

    RunDirectoryFileFilter(boolean completedRuns) {
      this.completedRuns = completedRuns;
    }

  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public void init(DataStorage storage, Configuration conf, GenericLogger logger)
      throws Aozan3Exception {

    // Check if step has not been already initialized
    if (this.initialized) {
      throw new IllegalStateException();
    }

    this.name = storage.getSequencerSource();
    this.storage = storage;

    this.initialized = true;
  }

  @Override
  public DataStorage getDataStorage() {

    checkInitialization();

    return this.storage;
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

  private List<RunData> listRuns(boolean completedRuns) {

    List<RunData> result = new ArrayList<>();

    File[] runDirectories = this.storage.getPath().toFile()
        .listFiles(new RunDirectoryFileFilter(completedRuns));

    if (runDirectories != null) {
      for (File f : runDirectories) {
        result.add(completedRuns
            ? RunDataFactory.newRawIlluminaRunData(this.storage, f.toPath(),
                this.name)
            : RunDataFactory.newPartialRawIlluminaRunData(this.storage,
                f.toPath(), this.name));
      }
    }

    return Collections.unmodifiableList(result);
  }

}
