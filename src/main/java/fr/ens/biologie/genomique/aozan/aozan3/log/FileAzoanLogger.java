package fr.ens.biologie.genomique.aozan.aozan3.log;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;

/**
 * This class implements an Aozan Logger based on a log file
 * @author Laurent Jourdren
 * @since 3.0
 */
public class FileAzoanLogger extends AbstractAzoanLogger {

  @Override
  protected Handler createHandler(Configuration conf) throws Aozan3Exception {

    // Get Log path
    String logPath = conf.get("aozan.log", "");
    if (logPath.isEmpty()) {
      throw new Aozan3Exception("No log file defined");
    }

    try {

      Handler result = new FileHandler(logPath, true);

      if (conf.containsKey("aozan.log.level")) {
        String logLevelName = conf.get("aozan.log.level");
        Level logLevel = Level.parse(logLevelName.toUpperCase());
        result.setLevel(logLevel);
      }

      return result;
    } catch (SecurityException | IOException e) {
      throw new Aozan3Exception(e);
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param conf configuration
   * @throws Aozan3Exception if an error occurs while creating the logger
   */
  public FileAzoanLogger(Configuration conf) throws Aozan3Exception {
    super(conf);
  }

}
