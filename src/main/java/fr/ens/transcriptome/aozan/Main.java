package fr.ens.transcriptome.aozan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.python.util.PythonInterpreter;

/**
 * This the main class of Aozan. This class only execute a jython script with
 * its arguments.
 * @author Laurent Jourdren
 */
public class Main {

  /**
   * Execute a Jython script.
   * @param scriptFile Jython script file
   * @param args arguments of the file
   */
  private static final void exexJythonScript(final File scriptFile,
      final String[] args) {

    final Properties props = new Properties();

    props.setProperty("python.home", System.getProperty("jython.home"));
    props.setProperty("python.path", System.getProperty("python.module.path"));
    props.setProperty("python.cachedir", System.getProperty("user.home")
        + File.separator + (File.separator.equals("/") ? "." : "") + "jython");

    PythonInterpreter.initialize(System.getProperties(), props, args);
    final PythonInterpreter interp = new PythonInterpreter();

    try {
      interp.execfile(new FileInputStream(scriptFile), scriptFile.toString());

    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + scriptFile);
      System.exit(1);
    }
  }

  /**
   * Initialize the logger for the application.
   * @param logPath path of the log file
   * @throws SecurityException if an error occurs while initializing the logger
   * @throws IOException if cannot open/create the log file
   */
  public static void initLogger(final String logPath) throws SecurityException,
      IOException {

    final Logger aozanLogger =
        Logger.getLogger(fr.ens.transcriptome.aozan.Globals.APP_NAME);
    final Logger eoulsanLogger =
        Logger.getLogger(fr.ens.transcriptome.eoulsan.Globals.APP_NAME);

    // Set default log level
    aozanLogger.setLevel(Globals.LOG_LEVEL);
    eoulsanLogger.setLevel(Globals.LOG_LEVEL);

    final Handler fh = new FileHandler(logPath, true);
    fh.setFormatter(Globals.LOG_FORMATTER);

    aozanLogger.setUseParentHandlers(false);
    eoulsanLogger.setUseParentHandlers(false);

    // Remove default Handler
    aozanLogger.removeHandler(aozanLogger.getParent().getHandlers()[0]);
    eoulsanLogger.removeHandler(eoulsanLogger.getParent().getHandlers()[0]);

    aozanLogger.addHandler(fh);
    // eoulsanLogger.addHandler(fh);
  }

  //
  // Main method
  //

  /**
   * Main method.
   * @param args command line argument. One or more arguments are required. The
   *          first argument is the path of the script to execte and remaining
   *          are the scripts arguments
   */
  public static void main(String[] args) {

    String filetoExecute = null;
    String[] finalArgs = null;

    if (args != null && args.length > 0) {

      filetoExecute = args[0];

      if (args.length > 1) {

        finalArgs = new String[args.length - 1];
        for (int i = 1; i < args.length; i++)
          finalArgs[i - 1] = args[i];
      }

      exexJythonScript(new File(filetoExecute), finalArgs);
    }

  }
}
