package fr.ens.biologie.genomique.aozan.aozan3.log;

import static java.util.Objects.requireNonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;

/**
 * This class define an abstract logger for the AozanLogger implementations
 * @author Laurent Jourdren
 * @since 3.0
 */
public abstract class AbstractAzoanLogger implements AozanLogger {

  private static final String DEFAULT_LOG_LEVEL = "INFO";

  /** Format of the log. */
  public static final Formatter LOG_FORMATTER = new Formatter() {

    private final DateFormat df =
        new SimpleDateFormat("yyyy.MM.dd kk:mm:ss", Locale.US);

    @Override
    public String format(final LogRecord record) {
      return record.getLevel()
          + "\t" + this.df.format(new Date(record.getMillis())) + "\t"
          + record.getMessage() + "\n";
    }
  };

  /**
   * Get the Java logger. Compatibility method.
   * @return the Java logger
   */
  public static final Logger getLogger() {
    return Logger.getLogger(Globals.APP_NAME);
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
    getLogger().severe((formatMessage(runId,
        exception != null ? exception.getMessage() : "UNKNOWN EXCEPTION")));
  }

  @Override
  public void debug(RunData runData, String message) {
    getLogger().fine(
        formatMessage(runData != null ? runData.getRunId() : null, message));
  }

  @Override
  public void info(RunData runData, String message) {
    getLogger().info(
        formatMessage(runData != null ? runData.getRunId() : null, message));
  }

  @Override
  public void warn(RunData runData, String message) {
    getLogger().warning(
        formatMessage(runData != null ? runData.getRunId() : null, message));
  }

  @Override
  public void error(RunData runData, String message) {
    getLogger().severe(
        formatMessage(runData != null ? runData.getRunId() : null, message));
  }

  @Override
  public void error(RunData runData, Throwable exception) {
    getLogger()
        .severe(formatMessage(runData != null ? runData.getRunId() : null,
            exception != null ? exception.getMessage() : "UNKNOWN EXCEPTION"));
  }

  @Override
  public void flush() {

    Handler[] handlers = getLogger().getHandlers();

    if (handlers != null && handlers.length > 0) {
      getLogger().getHandlers()[0].flush();
    }
  }

  @Override
  public void close() {

    Handler[] handlers = getLogger().getHandlers();

    if (handlers != null && handlers.length > 0) {
      getLogger().removeHandler(getLogger().getHandlers()[0]);
    }
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
    fh.setFormatter(LOG_FORMATTER);

    logger.setUseParentHandlers(false);

    // Remove default Handler
    logger.removeHandler(logger.getParent().getHandlers()[0]);

    logger.addHandler(fh);
  }

}
