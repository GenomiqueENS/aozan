package fr.ens.biologie.genomique.aozan.aozan3;

import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * This class define implements a logger on the standard error.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class StandardErrorAozanLogger extends AbstractAzoanLogger {

  @Override
  protected Handler createHandler(Configuration conf) throws Aozan3Exception {

    return new StreamHandler(System.err, new SimpleFormatter());
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param conf configuration
   * @throws Aozan3Exception if an error occurs while creating the logger
   */
  public StandardErrorAozanLogger(Configuration conf) throws Aozan3Exception {
    super(conf);
  }

}
