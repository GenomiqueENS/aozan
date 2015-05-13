package fr.ens.transcriptome.aozan.illumina.samplesheet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;

public class SampleSheetVersion2 extends SampleSheet {

  private List<String> fields;

  private final Map<String, String> headerSession;
  private final Map<String, String> readsSession;
  private final Map<String, String> settingsSession;

  public void addSessionEntry(final List<String> fields,
      final String sessionName) throws AozanException {

    switch (sessionName) {

    case "[Header]":
      if (fields.size() == 1) {
        this.headerSession.put(fields.get(0).trim(), "");
      } else {
        this.headerSession.put(fields.get(0).trim(), fields.get(1).trim());
      }
      break;

    case "[Reads]":
      this.readsSession.put(fields.get(0).trim(), "");
      break;

    case "[Settings]":
      if (fields.size() == 1) {
        this.settingsSession.put(fields.get(0).trim(), "");
      } else {
        this.settingsSession.put(fields.get(0).trim(), fields.get(1).trim());
      }
      break;

    default:
      throw new AozanException(
          "Parsing sample sheet file, invalid session name " + sessionName);
    }

  }

  public Map<String, String> getHearderEntries() {
    return headerSession;
  }

  /**
   * @return the readsSession
   */
  public Map<String, String> getReadsSession() {
    return readsSession;
  }

  /**
   * @return the settingsSession
   */
  public Map<String, String> getSettingsSession() {
    return settingsSession;
  }

  public boolean existHeaderSession() {
    return !headerSession.isEmpty();
  }

  public boolean existReadsSession() {
    return !readsSession.isEmpty();
  }

  public boolean existSettingsSession() {
    return !settingsSession.isEmpty();
  }

  //
  // Constructor
  //

  public SampleSheetVersion2(String sampleSheetVersion) {
    super(sampleSheetVersion);

    this.headerSession = new HashMap<>();
    this.readsSession = new HashMap<>();
    this.settingsSession = new HashMap<>();

  }

}
