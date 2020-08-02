package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import java.util.Objects;

import fr.ens.biologie.genomique.aozan.aozan3.EmailMessage;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;

/**
 * This class define a simple ProcessResult.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class SimpleProcessResult implements DataProcessor.ProcessResult {

  private final RunData rundata;
  private final EmailMessage email;

  @Override
  public RunData getRunData() {

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

    Objects.requireNonNull(outputRunData);
    Objects.requireNonNull(email);

    this.rundata = outputRunData;
    this.email = email;
  }

}
