package fr.ens.biologie.genomique.aozan.aozan3.log;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.Globals;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.log.DummyLogger;
import fr.ens.biologie.genomique.kenetre.log.FileLogger;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import fr.ens.biologie.genomique.kenetre.log.StandardErrorLogger;

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
  public static GenericLogger newLogger(final Configuration conf,
      final GenericLogger oldLogger) throws Aozan3Exception {

    requireNonNull(conf);
    requireNonNull(oldLogger);

    if (!(oldLogger instanceof DummyLogger)) {

      return oldLogger;
    }

    // Disable old logger
    oldLogger.flush();
    oldLogger.close();

    String loggerName = conf.get("aozan.logger", "dummy").toLowerCase().trim();

    try {
      switch (loggerName) {

      case "dummy":
        return new DummyLogger();

      case "stderr":
        return new StandardErrorLogger(Globals.APP_NAME,
            aozanLogConfToKenetreLogConf(conf).toMap());

      case "file":
        return new FileLogger(Globals.APP_NAME,
            aozanLogConfToKenetreLogConf(conf).toMap());

      default:
        throw new Aozan3Exception("Unknown logger: " + loggerName);
      }
    } catch (KenetreException e) {
      throw new Aozan3Exception(e);
    }

  }

  private static final Configuration aozanLogConfToKenetreLogConf(
      Configuration aozanConf) {

    Configuration result = new Configuration();

    if (aozanConf.containsKey("aozan.log")) {
      result.set("log.file", aozanConf.get("aozan.log"));
    }

    if (aozanConf.containsKey("aozan.log.level")) {
      result.set("log.level", aozanConf.get("aozan.log.level"));
    }

    return result;
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
