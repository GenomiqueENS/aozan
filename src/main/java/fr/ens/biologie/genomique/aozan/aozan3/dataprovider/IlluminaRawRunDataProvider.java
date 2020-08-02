package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import static fr.ens.biologie.genomique.aozan.aozan3.SequencerSource.unknownSequencerSource;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.IlluminaUtils;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunDataFactory;
import fr.ens.biologie.genomique.aozan.aozan3.SequencerSource;
import fr.ens.biologie.genomique.aozan.aozan3.util.Utils;

/**
 * This class define a processed data provider for Illumina runs.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class IlluminaRawRunDataProvider implements RunDataProvider {

  private final DataStorage storage;
  private final SequencerSource name;

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
  public DataStorage getDataStorage() {

    return this.storage;
  }

  @Override
  public boolean canProvideRunData() {

    return true;
  }

  @Override
  public List<RunData> listInProgressRunData() {

    return listRuns(false);
  }

  @Override
  public List<RunData> listCompletedRunData() {

    return listRuns(true);
  }

  private List<RunData> listRuns(boolean completedRuns) {

    List<RunData> result = new ArrayList<>();

    File[] runDirectories = this.storage.getPath().toFile()
        .listFiles(new RunDirectoryFileFilter(completedRuns));

    for (File f : runDirectories) {
      result.add(completedRuns
          ? RunDataFactory.newRawIlluminaRunData(this.storage, f.toPath(),
              this.name)
          : RunDataFactory.newPartialRawIlluminaRunData(this.storage,
              f.toPath(), this.name));
    }

    return Collections.unmodifiableList(result);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param source sequencer source
   * @param storage storage used by the provider
   * @throws IOException if an error occurs while creating the object
   */
  public IlluminaRawRunDataProvider(final SequencerSource source,
      final DataStorage storage) throws IOException {

    Objects.requireNonNull(source);
    Objects.requireNonNull(storage);

    this.name = source != null ? source : unknownSequencerSource();
    this.storage = storage;
  }
}
