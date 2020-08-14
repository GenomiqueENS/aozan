package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

import java.util.Set;

import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;

/**
 * This class define a simple ProcessResult.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class SimpleProcessResult implements DataProcessor.ProcessResult {

  private final Set<RunData> rundata;
  private final EmailMessage email;

  @Override
  public Set<RunData> getRunData() {

    return this.rundata;
  }

  @Override
  public EmailMessage getEmail() {

    return this.email;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param outputRunData output runData
   * @param email output email
   */
  public SimpleProcessResult(RunData outputRunData, EmailMessage email) {

    this(singleton(outputRunData), email);
  }

  /**
   * Constructor.
   * @param outputRunData output runData
   * @param email output email
   */
  public SimpleProcessResult(Set<RunData> outputRunData, EmailMessage email) {

    requireNonNull(outputRunData);
    requireNonNull(email);

    // Check if one or more element of the set is null
    for (RunData r : outputRunData) {
      if (r == null) {
        throw new IllegalArgumentException(
            "one or more output run data is null");
      }
    }

    this.rundata = outputRunData;
    this.email = email;
  }

}
