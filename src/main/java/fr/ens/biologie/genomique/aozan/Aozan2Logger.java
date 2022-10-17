package fr.ens.biologie.genomique.aozan;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

public class Aozan2Logger {

  private static class EoulsanRuntimeLogger
      implements GenericLogger, Serializable {

    private static final long serialVersionUID = 7316420644292128626L;

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
    public void flush() {
    }

    @Override
    public void close() {
    }

  }

  private static String loggerName = Globals.APP_NAME;

  private static final Map<String, Logger> threadGroupLoggers = new HashMap<>();

  private static Logger logger;

  /**
   * Get the logger object.
   * @return a logger object for Eoulsan
   */
  public static Logger getLogger() {

    // Search Thread logger only if thread logger has been registered
    if (!threadGroupLoggers.isEmpty()) {

      ThreadGroup tg = Thread.currentThread().getThreadGroup();
      do {

        if (threadGroupLoggers.containsKey(tg.getName())) {
          return threadGroupLoggers.get(tg.getName());
        }

        tg = tg.getParent();
      } while (tg != null);
    }

    // Keep a reference of the Logger to avoid disappearance of the Handlers.
    // LogManager only keeps weak references to the Loggers it creates.
    if (logger == null) {
      logger = Logger.getLogger(loggerName);
    }

    return logger;
  }

  /**
   * Set the logger name.
   * @param newLoggerName the new logger name
   */
  public static void setLoggerName(final String newLoggerName) {

    if (newLoggerName == null) {
      throw new NullPointerException("New logger name is null");
    }

    loggerName = newLoggerName;
    logger = null;
  }

  /**
   * Get the logger name.
   * @return the logger name
   */
  public static String getLoggerName() {

    return loggerName;
  }

  /**
   * Register a logger for a thread group.
   * @param threadGroup thread group
   * @param logger logger to register
   */
  public static void registerThreadGroupLogger(final ThreadGroup threadGroup,
      final Logger logger) {

    if (threadGroup == null || logger == null) {
      return;
    }

    threadGroupLoggers.put(threadGroup.getName(), logger);
  }

  /**
   * Remove a logger for a thread group.
   * @param threadGroup thread group
   */
  public static void removeThreadGroupLogger(final ThreadGroup threadGroup) {

    if (threadGroup == null) {
      return;
    }

    threadGroupLoggers.remove(threadGroup.getName());
  }

  /**
   * Initialize the console logger handler for Hadoop mappers and reducers. This
   * method set the Eoulsan logger format and define the logger level.
   */
  public static void initConsoleHandler() {
    initConsoleHandler(null);
  }

  /**
   * Initialize the console logger handler for Hadoop mappers and reducers.
   * @param level log level to use
   */
  public static void initConsoleHandler(final Level level) {

    // Disable parent Handler
    getLogger().setUseParentHandlers(false);

    // Create the new console handler
    final Handler handler = new ConsoleHandler();
    handler.setLevel(level != null ? level : Globals.LOG_LEVEL);
    handler.setFormatter(Globals.LOG_FORMATTER);

    // Add the handler to the logger
    getLogger().addHandler(handler);

    // Set the log level of the logger
    getLogger().setLevel(handler.getLevel());
  }

  /**
   * Log an SEVERE message using <code>getLogger().severe()</code>.
   * @param msg The string message
   */
  public static void logSevere(final String msg) {

    getLogger().severe(msg);
  }

  /**
   * Log an WARNING message using <code>getLogger().warning()</code>.
   * @param msg The string message
   */
  public static void logWarning(final String msg) {

    getLogger().warning(msg);
  }

  /**
   * Log an INFO message using <code>getLogger().info()</code>.
   * @param msg The string message
   */
  public static void logInfo(final String msg) {

    getLogger().info(msg);
  }

  /**
   * Log a CONFIG message using <code>getLogger().config()</code>.
   * @param msg The string message
   */
  public static void logConfig(final String msg) {

    getLogger().config(msg);
  }

  /**
   * Log a FINER message using <code>getLogger().finer()</code>.
   * @param msg The string message
   */
  public static void logFiner(final String msg) {

    getLogger().finer(msg);
  }

  /**
   * Log a FINEST message using <code>getLogger().finest()</code>.
   * @param message The string message
   */
  public static void logFinest(final String message) {

    getLogger().finest(message);
  }

  public static GenericLogger getGenericLogger() {

    return new EoulsanRuntimeLogger();
  }

}
