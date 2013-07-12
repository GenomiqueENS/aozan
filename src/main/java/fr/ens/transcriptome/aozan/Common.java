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
import static fr.ens.transcriptome.eoulsan.LocalEoulsanRuntime.initEoulsanRuntimeForExternalApp;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class contains common methods like logger initialization.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class Common {

  // TODO to fix
  public static void initLogger(final String logPath) throws AozanException,
      IOException {
    try {
      initEoulsanRuntimeForExternalApp();
    } catch (EoulsanException ee) {
      throw new AozanException(ee.getMessage());
    }
  }

  /**
   * Initialize the logger for the application.
   * @param logPath path of the log file
   * @throws SecurityException if an error occurs while initializing the logger
   * @throws IOException if cannot open/create the log file
   */
  public static void initLogger_OLD(final String logPath)
      throws SecurityException, IOException {

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

}
