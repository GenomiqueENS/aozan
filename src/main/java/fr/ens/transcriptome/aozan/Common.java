/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import static fr.ens.transcriptome.eoulsan.LocalEoulsanRuntime.initEoulsanRuntimeForExternalApp;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class contains common methods like logger initialization.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class Common {

  public static final Logger getLogger() {
    return Logger.getLogger(fr.ens.transcriptome.eoulsan.Globals.APP_NAME);
  }

  /**
   * Return the pid of the instance of jvm
   * @return pid of the instance of jvm, or null
   */
  public static final int getCurrentPid() {

    final String beanName = ManagementFactory.getRuntimeMXBean().getName();
    final int index = beanName.indexOf('@');

    return Integer.parseInt(beanName.substring(0, index));
  }

  /**
   * Initialize the logger for the application.
   * @param logPath path of the log file
   * @throws SecurityException if an error occurs while initializing the logger
   * @throws IOException if cannot open/create the log file
   */
  public static void initLogger(final String logPath) throws SecurityException,
      IOException, AozanException {

    initLogger(logPath, (String) null);
  }

  /**
   * Initialize the logger for the application.
   * @param logPath path of the log file
   * @param logLevel log level
   * @throws SecurityException if an error occurs while initializing the logger
   * @throws IOException if cannot open/create the log file
   */
  public static void initLogger(final String logPath, final String logLevel)
      throws SecurityException, IOException, AozanException {

    initLogger(
        logPath,
        logLevel == null ? Globals.LOG_LEVEL : Level.parse(logLevel
            .toUpperCase()));
  }

  /**
   * Initialize the logger for the application.
   * @param logPath path of the log file
   * @param logLevel log level
   * @throws SecurityException if an error occurs while initializing the logger
   * @throws IOException if cannot open/create the log file
   */
  public static void initLogger(final String logPath, final Level logLevel)
      throws SecurityException, IOException, AozanException {

    final Logger eoulsanLogger = getLogger();

    eoulsanLogger.setLevel(Level.OFF);

    // Remove default Handler
    eoulsanLogger.removeHandler(eoulsanLogger.getParent().getHandlers()[0]);

    try {
      initEoulsanRuntimeForExternalApp();
    } catch (EoulsanException ee) {
      throw new AozanException(ee);
    }

    // Set default log level
    eoulsanLogger.setLevel(logLevel);

    final Handler fh = new FileHandler(logPath, true);
    fh.setFormatter(fr.ens.transcriptome.eoulsan.Globals.LOG_FORMATTER);

    eoulsanLogger.setUseParentHandlers(false);

    // Remove default Handler
    eoulsanLogger.removeHandler(eoulsanLogger.getParent().getHandlers()[0]);

    eoulsanLogger.addHandler(fh);
  }

}
