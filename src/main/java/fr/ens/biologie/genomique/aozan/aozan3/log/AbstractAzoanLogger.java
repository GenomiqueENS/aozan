package fr.ens.biologie.genomique.aozan.aozan3.log;

import static java.util.Objects.requireNonNull;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;

/**
 * This class define an abstract logger for the AozanLogger implementations
 * @author Laurent Jourdren
 * @since 3.0
 */
public abstract class AbstractAzoanLogger implements AozanLogger {

  private static final String DEFAULT_LOG_LEVEL = "INFO";

  /**
   * Get the Java logger. Compatibility method.
   * @return the Java logger
   */
  public static final Logger getLogger() {
    return Logger.getLogger(fr.ens.biologie.genomique.eoulsan.Globals.APP_NAME);
  }

  @Override
  public void debug(String message) {
    getLogger().fine(message);
  }

  @Override
  public void info(String message) {
    getLogger().info(message);
  }

  @Override
  public void warn(String message) {
    getLogger().warning(message);
  }

  @Override
  public void error(String message) {
    getLogger().severe(message);
  }

  @Override
  public void error(Throwable exception) {
    getLogger().severe(exception.getMessage());
  }

  @Override
  public void debug(RunId runId, String message) {
    getLogger().fine(formatMessage(runId, message));
  }

  @Override
  public void info(RunId runId, String message) {
    getLogger().info((formatMessage(runId, message)));

  }

  @Override
  public void warn(RunId runId, String message) {
    getLogger().warning((formatMessage(runId, message)));

  }

  @Override
  public void error(RunId runId, String message) {
    getLogger().severe((formatMessage(runId, message)));

  }

  @Override
  public void error(RunId runId, Throwable exception) {
    getLogger().severe((formatMessage(runId, exception.getMessage())));
  }

  //
  // Other methods
  //

  private static String formatMessage(RunId runId, String message) {

    String r = runId != null ? runId.getId() : "UNKNOWN";
    String msg = message != null ? message : "NO MESSAGE";

    return "[Run " + r + "] " + msg;
  }

  //
  // Abstract methods
  //

  abstract protected Handler createHandler(Configuration conf)
      throws Aozan3Exception;

  //
  // Constructor
  //

  public AbstractAzoanLogger(Configuration conf) throws Aozan3Exception {

    requireNonNull(conf);

    // Get Log level
    String logLevelName = conf.get("aozan.log.level", DEFAULT_LOG_LEVEL);
    Level logLevel = Level.parse(logLevelName.toUpperCase());

    final Logger logger = getLogger();

    logger.setLevel(Level.OFF);

    // Remove default Handler
    logger.removeHandler(logger.getParent().getHandlers()[0]);

    // Set default log level
    logger.setLevel(logLevel);

    final Handler fh = createHandler(conf);
    fh.setFormatter(fr.ens.biologie.genomique.eoulsan.Globals.LOG_FORMATTER);

    logger.setUseParentHandlers(false);

    // Remove default Handler
    logger.removeHandler(logger.getParent().getHandlers()[0]);

    logger.addHandler(fh);
  }

}
