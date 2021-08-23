package fr.ens.biologie.genomique.aozan.aozan3.log;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;

/**
 * This class define implements a logger on the standard error.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class StandardErrorAozanLogger extends AbstractAzoanLogger {

  @Override
  protected Handler createHandler(Configuration conf) throws Aozan3Exception {

    Handler result = new StreamHandler(System.err, new SimpleFormatter()) {

      // Force flush after each log
      @Override
      public synchronized void publish(final LogRecord record) {
        super.publish(record);
        flush();
      }

    };

    if (conf.containsKey("aozan.log.level")) {
      String logLevelName = conf.get("aozan.log.level");
      Level logLevel = Level.parse(logLevelName.toUpperCase());
      result.setLevel(logLevel);
    }

    return result;
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
