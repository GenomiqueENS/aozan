package fr.ens.biologie.genomique.aozan.aozan3;

/**
 * This interfacce define a logger for Aozan
 * @author Laurent Jourdren
 * @since 3.0
 */
public interface AozanLogger {

  /**
   * Log a debug message.
   * @param message message to log
   */
  void debug(String message);

  /**
   * Log an info message.
   * @param message message to log
   */
  void info(String message);

  /**
   * Log a warning message.
   * @param message message to log
   */
  void warn(String message);

  /**
   * Log an error message.
   * @param message message to log
   */
  void error(String message);

  /**
   * Log an error message.
   * @param exception exception to log
   */
  void error(Throwable exception);

  /**
   * Log a debug message.
   * @param runId run id
   * @param message message to log
   */
  void debug(RunId runId, String message);

  /**
   * Log an info message.
   * @param runId run id
   * @param message message to log
   */
  void info(RunId runId, String message);

  /**
   * Log a warning message.
   * @param runId run id
   * @param message message to log
   */
  void warn(RunId runId, String message);

  /**
   * Log an error message.
   * @param runId run id
   * @param message message to log
   */
  void error(RunId runId, String message);

  /**
   * Log an error message.
   * @param runId run id
   * @param exception exception to log
   */
  void error(RunId runId, Throwable exception);
}
