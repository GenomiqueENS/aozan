package fr.ens.biologie.genomique.aozan.aozan3.dataprovider;

import static com.google.common.base.Strings.nullToEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.common.base.Strings;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.DataStorage;
import fr.ens.biologie.genomique.aozan.aozan3.IlluminaUtils;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunDataFactory;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.aozan.aozan3.SequencerSource;
import fr.ens.biologie.genomique.aozan.aozan3.util.Utils;
import fr.ens.biologie.genomique.kenetre.illumina.RunInfo;
import fr.ens.biologie.genomique.kenetre.illumina.RunParameters;
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
    private final Set<String> excludedRunIds;

    @Override
    public boolean accept(final File file) {

      // Do not accept excluded run ids
      if (this.excludedRunIds.contains(file.getName())) {
        return false;
      }

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

      boolean runCompleted = runCompleted(file.toPath());
      boolean tempDirectory = file.getName().endsWith(".tmp");

      return this.completedRuns
          ? runCompleted && !tempDirectory : !runCompleted || tempDirectory;
    }

    RunDirectoryFileFilter(Set<String> excludedRunIds, boolean completedRuns) {
      this.excludedRunIds = excludedRunIds;
      this.completedRuns = completedRuns;
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
  public List<RunData> listInProgressRunData(Collection<RunId> excludedRuns) {

    checkInitialization();

    return listRuns(excludedRuns, false);
  }

  @Override
  public List<RunData> listCompletedRunData(Collection<RunId> excludedRuns) {

    checkInitialization();

    return listRuns(excludedRuns, true);
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

  private List<RunData> listRuns(Collection<RunId> excludedRuns,
      boolean completedRuns) {

    Set<String> excludedRunIds = new HashSet<>();

    if (excludedRuns != null) {
      for (RunId r : excludedRuns) {
        excludedRunIds.add(r.getId());
      }
    }

    List<RunData> result = new ArrayList<>();

    File[] runDirectories = this.storage.getPath().toFile()
        .listFiles(new RunDirectoryFileFilter(excludedRunIds, completedRuns));

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

  /**
   * Test if a run is completed.
   * @param runDirectory the path to the run directory
   * @return true if the run is completed
   */
  private static boolean runCompleted(Path runDirectory) {

    requireNonNull(runDirectory);
    File dir = runDirectory.toFile();

    // RTA >= 2 or MiSeq
    File runParameterFile = new File(dir, "RunParameters.xml");
    if (runParameterFile.exists()) {

      int rtaVersion = getRTAVersionFast(runParameterFile);

      switch (rtaVersion) {

      // MiSeq
      case 1:
        try {
          var rp = RunParameters.parse(runParameterFile);

          if ("miseq"
              .equals(nullToEmpty(rp.getSequencerFamily()).toLowerCase())) {
            return runMiSeqCompleted(rp, dir);
          }

          // Case not handled
          return false;

        } catch (ParserConfigurationException | SAXException | IOException e) {
          return false;
        }

      // RTA 2
      case 2:
        return fileExists(dir, "RTAComplete.txt", "RunCompletionStatus.xml")
            && completeFileExists(dir, "RTARead", "Complete.txt");

      // RTA 3 or RTA 4
      case 3:
      case 4:
        return fileExists(dir, "CopyComplete.txt", "RTAComplete.txt",
            "RunCompletionStatus.xml");
      default:
        break;
      }

    }

    // RTA 1
    runParameterFile = new File(dir, "runParameters.xml");
    if (runParameterFile.exists()) {

      return fileExists(dir, "RTAComplete.txt",
          "ImageAnalysis_Netcopy_complete.txt")
          && completeFileExists(dir, "Basecalling_Netcopy_complete_Read",
              ".txt");
    }

    // No parameter file
    return false;
  }

  /**
   * Test if a MiSeq run is completed.
   * @param rp run parameters object
   * @param runDirectory the path to the run directory
   * @return true if the run is completed
   */
  private static boolean runMiSeqCompleted(RunParameters rp,
      File runDirectory) {

    requireNonNull(rp);
    requireNonNull(runDirectory);

    // Common files
    if (!(fileExists(runDirectory, "RTAComplete.txt",
        "Basecalling_Netcopy_complete.txt")
        && completeFileExists(runDirectory, "Basecalling_Netcopy_complete_Read",
            ".txt"))) {
      return false;
    }

    switch (Strings.nullToEmpty(rp.getRunParametersVersion()).toLowerCase()) {

    case "miseq_1_1":
      return true;

    case "miseq_1_3":
    default:
      return fileExists(runDirectory, "RunCompletionStatus.xml");
    }

  }

  /**
   * Check if all the files exists.
   * @param runDirectory directory where to check files
   * @param filenames filenames of the files
   * @return true if all the files exists
   */
  private static boolean fileExists(File runDirectory, String... filenames) {

    requireNonNull(runDirectory);

    for (String filename : filenames) {

      if (!new File(runDirectory, filename).isFile()) {
        return false;
      }
    }

    return true;
  }

  /**
   * Check if a complete files exists
   * @param runDirectory run directory where to check files
   * @param prefix prefix of the complete files
   * @param suffix suffix of the complete files
   * @return true if the complete files exists
   */
  private static boolean completeFileExists(File runDirectory, String prefix,
      String suffix) {

    requireNonNull(runDirectory);
    requireNonNull(prefix);
    requireNonNull(suffix);

    int readCount = readCount(runDirectory);

    if (readCount < 1) {
      return false;
    }

    for (int i = 1; i <= readCount; i++) {

      if (!new File(runDirectory, prefix + i + suffix).isFile()) {
        return false;
      }
    }

    return true;
  }

  /**
   * Get the read count of a run.
   * @param runDirectory run directory
   * @return the read count of the run
   */
  private static int readCount(File runDirectory) {

    requireNonNull(runDirectory);

    File runInfoFile = new File(runDirectory, "RunInfo.xml");
    if (!runInfoFile.exists()) {
      return -1;
    }

    try {
      RunInfo runInfo = RunInfo.parse(runInfoFile);
      return runInfo.getReads().size();

    } catch (ParserConfigurationException | SAXException | IOException e) {
      return -1;
    }

  }

  /**
   * Get the RTA version from a run parameter file.
   * @param rtaFile the run parameter version
   * @return the RTA major version or -1 if cannot be found
   */
  private static int getRTAVersionFast(File rtaFile) {

    try (BufferedReader reader =
        new BufferedReader(new FileReader(rtaFile, UTF_8))) {

      String line = null;
      while ((line = reader.readLine()) != null) {

        int found = line.toLowerCase().indexOf("<rtaversion>");
        if (found > -1
            && line.length() > (found + "<rtaversion>".length() + 1)) {
          return line.charAt(found + "<rtaversion>".length()) - 48;
        }
      }

    } catch (IOException e) {
      return -1;
    }

    return -1;
  }

}
