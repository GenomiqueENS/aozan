package fr.ens.biologie.genomique.aozan.aozan3;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * This class allow to define names for sequencers.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class SequencerNames {

  private static final String CONF_PREFIX = "sequencer.name.";
  private final Map<String, String> alias = new HashMap<>();

  //
  // Static method
  //

  /**
   * Add a sequencer.
   * @param sn serial number
   * @param name name of the sequencer
   */
  public void addSequencerName(String sn, String name) {

    requireNonNull(sn);
    requireNonNull(name);

    this.alias.put(sn.trim(), name.trim());
  }

  /**
   * Get the name of a sequencer from its serial number.
   * @param sn serial number
   * @return the name of the sequencer or the serial number if the serial number
   *         is unknown
   */
  public String getNameFromSerialNumber(String sn) {

    requireNonNull(sn);

    String result = this.alias.get(sn.toString());

    return result == null ? sn : result;
  }

  /**
   * Get the name of an Illumina sequencer from a runId.
   * @param runId a run Id
   * @return the name of the sequencer or the serial number if the serial number
   *         is unknown
   */
  public String getIlluminaSequencerName(RunId runId) {

    return getNameFromSerialNumber(
        new IlluminaRunIdWrapper(runId).getInstrumentSerialNumber());
  }

  /**
   * Add sequencers in the aliases table from a configuration object.
   * @param conf the configuration
   */
  public void addSequencersFromConfiguration(Configuration conf) {

    requireNonNull(conf);

    for (Map.Entry<String, String> e : conf.toMap().entrySet()) {

      String key = e.getKey();

      if (key != null && key.startsWith(CONF_PREFIX)) {

        key = key.substring(CONF_PREFIX.length());
        addSequencerName(key, e.getValue());
      }
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param conf the configuration
   */
  public SequencerNames(Configuration conf) {

    addSequencersFromConfiguration(conf);
  }

}
