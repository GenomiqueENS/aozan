package fr.ens.biologie.genomique.aozan.aozan3.log;

import static java.util.Objects.requireNonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import fr.ens.biologie.genomique.aozan.aozan3.RunData;
import fr.ens.biologie.genomique.aozan.aozan3.RunId;
import fr.ens.biologie.genomique.kenetre.log.DummyLogger;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

public class Aozan3Logger {

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

  private final GenericLogger logger;

  /**
   * Get the underlying logger.
   * @return the underlying logger
   */
  public GenericLogger getLogger() {
    return this.logger;
  }

  /**
   * Log a debug message.
   * @param message message to log
   */
  public void debug(String message) {
    this.logger.debug(message);
  }

  /**
   * Log an info message.
   * @param message message to log
   */
  public void info(String message) {
    this.logger.info(message);
  }

  /**
   * Log a warning message.
   * @param message message to log
   */
  public void warn(String message) {
    this.logger.warn(message);
  }

  /**
   * Log an error message.
   * @param message message to log
   */
  public void error(String message) {
    this.logger.error(message);
  }

  /**
   * Log an error message.
   * @param exception exception to log
   */
  public void error(Throwable exception) {
    this.logger.error(exception.getMessage());
  }

  /**
   * Log a debug message.
   * @param runId run id
   * @param message message to log
   */
  public void debug(RunId runId, String message) {
    this.logger.debug(formatMessage(runId, message));
  }

  /**
   * Log an info message.
   * @param runId run id
   * @param message message to log
   */
  public void info(RunId runId, String message) {
    this.logger.debug((formatMessage(runId, message)));
  }

  /**
   * Log a warning message.
   * @param runId run id
   * @param message message to log
   */
  public void warn(RunId runId, String message) {
    this.logger.warn((formatMessage(runId, message)));

  }

  /**
   * Log an error message.
   * @param runId run id
   * @param message message to log
   */
  public void error(RunId runId, String message) {
    this.logger.error((formatMessage(runId, message)));

  }

  /**
   * Log an error message.
   * @param runId run id
   * @param exception exception to log
   */
  public void error(RunId runId, Throwable exception) {

    error(this.logger, runId, exception);

  }

  /**
   * Log a debug message.
   * @param RunData run data
   * @param message message to log
   */
  public void debug(RunData runData, String message) {

    debug(this.logger, runData, message);
  }

  /**
   * Log an info message.
   * @param RunData run data
   * @param message message to log
   */
  public void info(RunData runData, String message) {

    info(this.logger, runData, message);
  }

  /**
   * Log a warning message.
   * @param RunData run data
   * @param message message to log
   */
  public void warn(RunData runData, String message) {
    this.logger.warn(
        formatMessage(runData != null ? runData.getRunId() : null, message));
  }

  /**
   * Log an error message.
   * @param RunData run data
   * @param message message to log
   */
  public void error(RunData runData, String message) {

    error(this.logger, runData, message);
  }

  /**
   * Log an error message.
   * @param RunData run data
   * @param exception exception to log
   */
  public void error(RunData runData, Throwable exception) {

    error(this.logger, runData, exception);
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
  // Static methods
  //

  /**
   * Log an error message.
   * @param logger the logger to use
   * @param runId run id
   * @param exception exception to log
   */
  public static void error(GenericLogger logger, RunId runId,
      Throwable exception) {

    requireNonNull(logger);

    logger.error((formatMessage(runId,
        exception != null ? exception.getMessage() : "UNKNOWN EXCEPTION")));
  }

  /**
   * Log an error message.
   * @param logger the logger to use
   * @param runId run id
   * @param message message to log
   */
  public static void error(GenericLogger logger, RunId runId, String message) {

    requireNonNull(logger);

    logger.error(formatMessage(runId, message));
  }

  /**
   * Log an info message.
   * @param logger the logger to use
   * @param runId run id
   * @param message message to log
   */
  public static void info(GenericLogger logger, RunId runId, String message) {

    requireNonNull(logger);

    logger.info(formatMessage(runId, message));
  }

  /**
   * Log a debug message.
   * @param logger the logger to use
   * @param RunData run data
   * @param message message to log
   */
  public static void debug(GenericLogger logger, RunData runData,
      String message) {

    requireNonNull(logger);

    logger.debug(
        formatMessage(runData != null ? runData.getRunId() : null, message));
  }

  /**
   * Log an info message.
   * @param logger the logger to use
   * @param RunData run data
   * @param message message to log
   */
  public static void info(GenericLogger logger, RunData runData,
      String message) {

    requireNonNull(logger);

    logger.info(
        formatMessage(runData != null ? runData.getRunId() : null, message));
  }

  /**
   * Log a warning message.
   * @param logger the logger to use
   * @param RunData run data
   * @param message message to log
   */
  public static void warn(GenericLogger logger, RunData runData,
      String message) {

    requireNonNull(logger);

    logger.warn(
        formatMessage(runData != null ? runData.getRunId() : null, message));
  }

  /**
   * Log an error message.
   * @param logger the logger to use
   * @param RunData run data
   * @param message message to log
   */
  public static void error(GenericLogger logger, RunData runData,
      String message) {

    requireNonNull(logger);

    logger.error(
        formatMessage(runData != null ? runData.getRunId() : null, message));
  }

  /**
   * Log an error message.
   * @param logger the logger to use
   * @param RunData run data
   * @param exception exception to log
   */
  public static void error(GenericLogger logger, RunData runData,
      Throwable exception) {

    requireNonNull(logger);

    logger.error(formatMessage(runData != null ? runData.getRunId() : null,
        exception != null ? exception.getMessage() : "UNKNOWN EXCEPTION"));
  }

  /**
   * Create dummy logger.
   * @return a dummy logger
   */
  public static Aozan3Logger newDummyLogger() {

    return new Aozan3Logger(new DummyLogger());
  }

  /**
   * Create an Aozan 3 Logger from a GenericLogger object.
   * @return an Aozan3Logger object
   */
  public static Aozan3Logger newAozanLogger(GenericLogger logger) {

    return new Aozan3Logger(logger);
  }

  //
  // Constructor
  //

  private Aozan3Logger(GenericLogger logger) {

    requireNonNull(logger);

    this.logger = logger;
  }

}
