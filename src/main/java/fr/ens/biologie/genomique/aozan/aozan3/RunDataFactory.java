package fr.ens.biologie.genomique.aozan.aozan3;

import static fr.ens.biologie.genomique.aozan.aozan3.SequencingTechnology.ILLUMINA;

import java.nio.file.Path;

import fr.ens.biologie.genomique.aozan.aozan3.util.Utils;

/**
 * This class define a RunData factory
 * @author Laurent Jourdren
 * @since 3.0
 */
public class RunDataFactory {

  /**
   * Create a new Illumina run data.
   * @param storage the storage used for the data
   * @param path path of the data
   * @param source source of the data
   * @return a new RunData object
   */
  public static RunData newRawIlluminaRunData(final DataStorage storage,
      final Path path, final SequencerSource source) {

    RunId runId = new RunId(path.getFileName().toString(), ILLUMINA);

    return new RunData(runId, source, RunData.Type.RAW, false,
        new DataLocation(storage, path));
  }

  /**
   * Create a new partial Illumina run data.
   * @param storage the storage used for the data
   * @param path path of the data
   * @param source source of the data
   * @return a new RunData object
   */
  public static RunData newPartialRawIlluminaRunData(final DataStorage storage,
      final Path path, final SequencerSource source) {

    RunId runId = new RunId(
        Utils.removeTmpExtension(path.getFileName().toString()), ILLUMINA);

    return new RunData(runId, source, RunData.Type.RAW, true,
        new DataLocation(storage, path));
  }

  //
  // Constructor
  //

  /**
   * Private consctructor
   */
  private RunDataFactory() {
  }

}
