package fr.ens.transcriptome.aozan.illumina.samplesheet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntry;
import fr.ens.transcriptome.aozan.io.FastqSample;

public class SampleSheetVersion2 extends SampleSheet {

  // Save order sample entry from sample sheet file
  private Map<String, Integer> list;

  private final Map<String, String> headerSession;
  private final Map<String, String> readsSession;
  private final Map<String, String> settingsSession;

  public void addSample(final SampleEntry sample) {

    super.addSample(sample);

    // Save order

    final String key = buildKey(sample.getSampleId(), sample.getLane());
    final int indice = list.size() + 1;

    list.put(key, indice);
  }

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

  // TODO
  static boolean first = true;

  public int extractOrderNumberSample(final FastqSample fastqSample) {

    if (this.list == null || this.list.isEmpty())
      throw new AozanRuntimeException(
          "list sample order in sample sheet file no setting");

    // Case undetermined always return 0
    if (fastqSample.isIndeterminedIndices()) {
      return 0;
    }

    String key;

    // Case no lane column in sample sheet file
    if (useLaneNumberInKey()) {
      // Lane number is zero
      key = buildKey(fastqSample.getSampleName(), 0);
    } else {

      key = buildKey(fastqSample.getSampleName(), fastqSample.getLane());
    }

    if (first) {

      // TODO
      System.out.println("contains list sample with order \n"
          + Joiner.on("\n\t").withKeyValueSeparator("=").join(this.list));

      System.out.println("order find in samplesheet for sample "
          + fastqSample.getSampleName()
          + " -> "
          + this.list.get(fastqSample.getSampleName()
              + "_" + fastqSample.getLane()));

      first = false;
    }
    return this.list.get(key);
  }

  private boolean useLaneNumberInKey() {

    final String first = this.list.keySet().iterator().next();

    return first.endsWith("0");
  }

  private String buildKey(String sampleName, int laneNumber) {

    return String.format("%s_%s", sampleName, laneNumber);
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
    this.list = new HashMap<>();
  }

}