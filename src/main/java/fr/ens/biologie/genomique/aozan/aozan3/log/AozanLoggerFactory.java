package fr.ens.biologie.genomique.aozan.aozan3.log;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;

/**
 * This class define a factory for loggers.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class AozanLoggerFactory {

  /**
   * Configure logger.
   * @param conf the recipe configuration
   * @param oldLogger old logger
   * @return a new AozanLogger object
   * @throws Aozan3Exception if an error occurs while creating the logger
   */
  public static AozanLogger newLogger(final Configuration conf,
      final AozanLogger oldLogger) throws Aozan3Exception {

    requireNonNull(conf);
    requireNonNull(oldLogger);

    // Disable old logger
    AozanLogger old = oldLogger != null ? oldLogger : new DummyAzoanLogger();
    old.flush();
    old.close();

    String loggerName = conf.get("aozan.logger", "dummy").toLowerCase().trim();

    switch (loggerName) {

    case "dummy":
      return new DummyAzoanLogger();

    case "stderr":
      return new StandardErrorAozanLogger(conf);

    case "file":
      return new FileAzoanLogger(conf);

    default:
      throw new Aozan3Exception("Unknown logger: " + loggerName);
    }

  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private AozanLoggerFactory() {
  }

}
