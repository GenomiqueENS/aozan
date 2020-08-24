package fr.ens.biologie.genomique.aozan.aozan3;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import fr.ens.biologie.genomique.eoulsan.core.Version;

/**
 * This class contains Globals constants.
 * @since 3.0
 * @author Laurent Jourdren
 */
public class Globals {

  private static Attributes manifestAttributes;

  //
  // Application constants
  //

  private static final String MANIFEST_FILE = "/META-INF/MANIFEST.MF";

  /** Default locale of the application. */
  public static final Locale DEFAULT_LOCALE = Locale.US;

  /** Minimal java version required by Aozan. */
  public static final int MINIMAL_JAVA_VERSION_REQUIRED = 8;

  //
  // Description constants
  //

  /** The name of the application. */
  public static final String APP_NAME = "Aozan";

  /** The name of the application. */
  public static final String APP_NAME_LOWER_CASE = APP_NAME.toLowerCase();

  /** The prefix of the parameters of the application. */
  public static final String PARAMETER_PREFIX =
      "fr.ens.biologie.genomique." + APP_NAME_LOWER_CASE;

  /** The version of the application. */
  public static final String APP_VERSION_STRING = getVersion();

  /** The version of the application. */
  public static final Version APP_VERSION = new Version(APP_VERSION_STRING);

  /** The built number of the application. */
  public static final String APP_BUILD_NUMBER = getBuiltNumber();

  /** The built commit of the application. */
  public static final String APP_BUILD_COMMIT = getBuiltCommit();

  /** The built host of the application. */
  public static final String APP_BUILD_HOST = getBuiltHost();

  /** The build date of the application. */
  public static final String APP_BUILD_DATE = getBuiltDate();

  /** The build year of the application. */
  public static final String APP_BUILD_YEAR = getBuiltYear();

  /** The welcome message. */
  public static final String WELCOME_MSG = Globals.APP_NAME
      + " version " + Globals.APP_VERSION_STRING + " (" + APP_BUILD_COMMIT
      + ", " + Globals.APP_BUILD_NUMBER + " build on " + APP_BUILD_HOST + ", "
      + Globals.APP_BUILD_DATE + ")";

  /** The prefix for temporary files. */
  public static final String TEMP_PREFIX = APP_NAME_LOWER_CASE
      + "-" + APP_VERSION_STRING + "-" + APP_BUILD_NUMBER + "-";

  /** Set the debug mode. */
  public static final boolean DEBUG = APP_VERSION_STRING.endsWith("-SNAPSHOT")
      || "UNKNOWN_VERSION".equals(APP_VERSION_STRING);

  private static final String WEBSITE_URL_DEFAULT =
      "http://outils.genomique.biologie.ens.fr/" + APP_NAME_LOWER_CASE;

  /** Application Website url. */
  public static final String WEBSITE_URL = getWebSiteURL();

  private static final String COPYRIGHT_DATE = "2011-" + APP_BUILD_YEAR;

  /** Licence text. */
  public static final String LICENSE_TXT =
      "This program is developed under the GNU General Public License"
          + " version 2 or later and CeCILL-A.";

  /** About string, plain text version. */
  public static final String ABOUT_TXT = Globals.APP_NAME
      + " version " + Globals.APP_VERSION_STRING + " (" + APP_BUILD_COMMIT
      + ", " + Globals.APP_BUILD_NUMBER + ")"
      + " is a pipeline for post sequencing processing.\n"
      + "This version has been built on " + APP_BUILD_DATE + ".\n\n"
      + "Authors:\n" + "  Laurent Jourdren <jourdren@biologie.ens.fr>\n"
      + "  Sandrine Perrin <sperrin@biologie.ens.fr>\n" + "Contacts:\n"
      + "  Mail: " + APP_NAME_LOWER_CASE + "@biologie.ens.fr\n"
      + "  Google group: http://groups.google.com/group/" + APP_NAME_LOWER_CASE
      + "\n" + "Copyright " + COPYRIGHT_DATE + " IBENS genomic platform\n"
      + LICENSE_TXT + "\n";

  //
  // Private constants
  //

  private static final String UNKNOWN_VERSION = "UNKNOWN_VERSION";
  private static final String UNKNOWN_BUILD = "UNKNOWN_BUILD";
  private static final String UNKNOWN_DATE = "UNKNOWN_DATE";
  private static final String UNKNOWN_YEAR = "UNKNOWN_YEAR";
  private static final String UNKNOWN_BUILD_COMMIT = "UNKNOWN_COMMIT";
  private static final String UNKNOWN_BUILD_HOST = "UNKNOWN_HOST";

  //
  // Methods
  //

  private static String getVersion() {

    final String version = getManifestProperty("Specification-Version");

    return version != null ? version : UNKNOWN_VERSION;
  }

  private static String getBuiltNumber() {

    final String builtNumber = getManifestProperty("Implementation-Version");

    return builtNumber != null ? builtNumber : UNKNOWN_BUILD;
  }

  private static String getBuiltDate() {

    final String builtDate = getManifestProperty("Built-Date");

    return builtDate != null ? builtDate : UNKNOWN_DATE;
  }

  private static String getBuiltYear() {

    final String builtYear = getManifestProperty("Built-Year");

    return builtYear != null ? builtYear : UNKNOWN_YEAR;
  }

  private static String getWebSiteURL() {

    final String url = getManifestProperty("url");

    return url != null ? url : WEBSITE_URL_DEFAULT;
  }

  private static String getBuiltCommit() {

    final String buildCommit = getManifestProperty("Built-Commit");

    return buildCommit != null ? buildCommit : UNKNOWN_BUILD_COMMIT;
  }

  private static String getBuiltHost() {

    final String buildHost = getManifestProperty("Built-Host");

    return buildHost != null ? buildHost : UNKNOWN_BUILD_HOST;
  }

  private static String getManifestProperty(final String propertyKey) {

    if (propertyKey == null) {
      return null;
    }

    readManifest();

    return manifestAttributes != null
        ? manifestAttributes.getValue(propertyKey) : null;
  }

  private static synchronized void readManifest() {

    if (manifestAttributes != null) {
      return;
    }

    try {

      final Class<?> clazz = Globals.class;
      final String className = clazz.getSimpleName() + ".class";
      final String classPath = clazz.getResource(className).toString();
      if (!classPath.startsWith("jar")) {
        // Class not from JAR
        return;
      }
      final String manifestPath =
          classPath.substring(0, classPath.lastIndexOf('!') + 1)
              + MANIFEST_FILE;
      final Manifest manifest =
          new Manifest(new URL(manifestPath).openStream());
      manifestAttributes = manifest.getMainAttributes();

    } catch (final IOException ignored) {
    }
  }

  /**
   * Set the default Local of the application
   */
  public static void setDefaultLocale() {

    Locale.setDefault(DEFAULT_LOCALE);
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private Globals() {
  }

}
