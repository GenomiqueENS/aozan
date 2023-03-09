package fr.ens.biologie.genomique.aozan.aozan3;

/**
 * This class define common methods for CLI.
 * @since 3.0
 * @author Laurent Jourdren
 */
public class Common {

  /**
   * Exit the application.
   * @param exitCode exit code
   */
  public static void exit(final int exitCode) {

    System.exit(exitCode);
  }

  /**
   * Print error message to the user and exits the application.
   * @param e Exception
   * @param message message to show to the use
   */
  public static void errorExit(final Throwable e, final String message) {

    errorExit(e, message, true);
  }

  /**
   * Print error message to the user and exits the application.
   * @param e Exception
   * @param message message to show to the use
   * @param logMessage true if message must be logged
   */
  public static void errorExit(final Throwable e, final String message,
      final boolean logMessage) {

    errorExit(e, message, logMessage, 1);
  }

  /**
   * Print error message to the user and exits the application.
   * @param e Exception
   * @param message message to show to the use
   */
  public static void errorExit(final Throwable e, final String message,
      int exitCode) {

    errorExit(e, message, true, exitCode);
  }

  /**
   * Print error message to the user and exits the application.
   * @param e Exception
   * @param message message to show to the use
   * @param logMessage true if message must be logged
   */
  public static void errorExit(final Throwable e, final String message,
      final boolean logMessage, int exitCode) {

    System.err.println("\n=== " + Globals.APP_NAME + " Error ===");
    System.err.println(message);

    printStackTrace(e);

    exit(exitCode);
  }

  /**
   * Show a message and then exit.
   * @param message the message to show
   */
  public static void showMessageAndExit(final String message) {

    System.out.println(message);
    exit(0);
  }

  /**
   * Show a message and then exit.
   * @param message the message to show
   */
  public static void showErrorMessageAndExit(final String message) {

    System.err.println(message);
    exit(1);
  }

  //
  // Utility methods
  //

  /**
   * Print the stack trace for an exception.
   * @param e Exception
   */
  private static void printStackTrace(final Throwable e) {

    System.err.println("\n=== " + Globals.APP_NAME + " Debug Stack Trace ===");
    e.printStackTrace();
    System.err.println();
  }

  //
  // Constructor
  //

  private Common() {
  }
}
