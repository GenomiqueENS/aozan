package fr.ens.biologie.genomique.aozan.aozan3;

import static fr.ens.biologie.genomique.aozan.aozan3.Globals.MINIMAL_JAVA_VERSION_REQUIRED;
import static fr.ens.biologie.genomique.kenetre.util.SystemUtils.getJavaVersion;
import static java.util.Collections.unmodifiableList;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Strings;

import fr.ens.biologie.genomique.aozan.Aozan2Logger;
import fr.ens.biologie.genomique.aozan.aozan3.action.Action;
import fr.ens.biologie.genomique.aozan.aozan3.action.ActionService;
import fr.ens.biologie.genomique.aozan.aozan3.action.LegacyAction;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLoggerFactory;
import fr.ens.biologie.genomique.kenetre.log.DummyLogger;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define the main class.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class Main {

  public static final String APPLICATION_CLASSPATH_JVM_ARG =
      "application.classpath";
  public static final String APPLICATION_SCRIPT =
      "application.launch.script.path";
  public static final String APPLICATION_PATH = "application.path";
  public static final String APPLICATION_MEMORY = "application.memory";

  private static Main main;

  private final List<String> args;
  private String configurationPath;
  private Action action;
  private List<String> actionArgs;
  private List<String> commandLineSettings;

  private final Configuration conf = new Configuration();
  private GenericLogger logger = new DummyLogger();

  //
  // Getters
  //

  /**
   * Get the instance of the Main class.
   * @return a Main object
   */
  public static Main getInstance() {

    return main;
  }

  /**
   * Get the the startup configuration.
   * @return the startup configuration
   */
  public Configuration getConfiguration() {
    return this.conf;
  }

  /**
   * Get the logger.
   * @return the logger
   */
  public GenericLogger getLogger() {
    return this.logger;
  }

  /**
   * Get java executable path.
   * @return the path to the java executable
   */
  public String getJavaExecutablePath() {

    return System.getProperty("java.home") + "/bin/java";
  }

  /**
   * Get JVM arguments.
   * @return the JVM arguments as an array
   */
  public List<String> getJVMArgs() {

    return ManagementFactory.getRuntimeMXBean().getInputArguments();
  }

  /**
   * Get the application classpath. The result of the method is based on the
   * content of the dedicated JVM argument.
   * @return the JVM class as a String
   */
  public String getApplicationClassPath() {

    return System.getProperty(APPLICATION_CLASSPATH_JVM_ARG);
  }

  /**
   * Get the application script path.
   * @return the application script path
   */
  public String getApplicationScriptPath() {

    return System.getProperty(APPLICATION_SCRIPT);
  }

  /**
   * Get the application memory requirement.
   * @return the application memory requirement
   */
  public int getApplicationMemory() {

    String value = System.getProperty(APPLICATION_MEMORY);

    if (value == null) {
      return -1;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Get the application directory.
   * @return the application directory
   */
  public Path getApplicationDirectory() {

    String aozanPath = System.getProperty(APPLICATION_PATH);

    if (aozanPath == null) {
      throw new NullPointerException(
          "Unknown install path of " + Globals.APP_NAME);
    }

    return Paths.get(aozanPath);
  }

  /**
   * Get command line arguments.
   * @return Returns the arguments
   */
  public List<String> getArgs() {

    return unmodifiableList(this.args);
  }

  /**
   * Get the action.
   * @return Returns the action
   */
  public Action getAction() {

    return this.action;
  }

  /**
   * Get the action arguments.
   * @return Returns the actionArgs
   */
  public List<String> getActionArgs() {

    return unmodifiableList(this.actionArgs);
  }

  //
  // Parsing methods
  //

  /**
   * Show command line help.
   * @param options Options of the software
   */
  protected void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Aozan [options] action arguments", options);

    System.out.println("Available actions:");
    for (Action action : ActionService.getInstance().getActions()) {

      if (!action.isHidden()) {

        System.out.println(Strings.padEnd(" - " + action.getName(), 23, ' ')
            + action.getDescription());
      }
    }

    Common.exit(0);
  }

  /**
   * Create options for command line
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  protected Options makeOptions() {

    // Create Options object
    final Options options = new Options();

    options.addOption("version", false, "show version of the software");
    options.addOption("about", false,
        "display information about this software");
    options.addOption("h", "help", false, "display this help");
    options.addOption("license", false,
        "display information about the license of this software");

    options.addOption(OptionBuilder.withArgName("file").hasArg()
        .withDescription("configuration file to use").create("conf"));

    options.addOption(OptionBuilder.withArgName("property=value").hasArg()
        .withDescription("set a configuration setting. This "
            + "option can be used several times")
        .create('s'));

    // Application shell script options
    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("JAVA_HOME path").create('j'));

    options.addOption(OptionBuilder.withArgName("size").hasArg()
        .withDescription("maximal memory usage for JVM in MB (4096 by default)")
        .create('m'));

    options.addOption(OptionBuilder.withArgName("args").hasArg()
        .withDescription("JVM arguments (-server by default)").create('J'));

    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("JVM working directory").create('w'));

    options.addOption(OptionBuilder.withArgName("classpath").hasArg()
        .withDescription(
            "additional classpath for " + Globals.APP_NAME + " plugins")
        .create('p'));

    return options;
  }

  /**
   * Parse the options of the command line
   * @return the number of options argument in the command line
   */
  private int parseCommandLine() {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();
    final String[] argsArray = this.args.toArray(new String[0]);

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, argsArray, true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      // About option
      if (line.hasOption("about")) {
        Common.showMessageAndExit(Globals.ABOUT_TXT);
      }

      // Version option
      if (line.hasOption("version")) {
        Common.showMessageAndExit(Globals.WELCOME_MSG);
      }

      // Licence option
      if (line.hasOption("license")) {
        Common.showMessageAndExit(Globals.LICENSE_TXT);
      }

      // Set the configuration file
      if (line.hasOption("conf")) {

        argsOptions += 2;
        this.configurationPath = line.getOptionValue("conf");
      }

      // Set the configuration settings
      if (line.hasOption('s')) {

        this.commandLineSettings = Arrays.asList(line.getOptionValues('s'));
        argsOptions += 2 * this.commandLineSettings.size();
      }

      // Shell script options
      if (line.hasOption('j')) {
        argsOptions += 2;
      }
      if (line.hasOption('m')) {
        argsOptions += 2;
      }
      if (line.hasOption('J')) {
        argsOptions += 2;
      }
      if (line.hasOption('p')) {
        argsOptions += 2;
      }
      if (line.hasOption('w')) {
        argsOptions += 2;
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing command line arguments: " + e.getMessage());
    }

    // No arguments found
    if (this.args == null || this.args.size() == argsOptions) {

      Common.showErrorMessageAndExit("This program needs one argument."
          + " Use the -h option to get more information.\n");
    }

    return argsOptions;
  }

  /**
   * Parse the action name and arguments from command line.
   * @param optionsCount number of options in the command line
   */
  private void parseAction(final int optionsCount) {

    // Set action name and arguments
    final String actionName = this.args.get(optionsCount).trim().toLowerCase();
    this.actionArgs = this.args.subList(optionsCount + 1, this.args.size());

    // Search action
    this.action = ActionService.getInstance().newService(actionName);

    // Action not found ?
    if (this.action == null) {

      // Legacy mode ?
      Path path = Paths.get(this.args.get(optionsCount));
      if (Files.isRegularFile(path)) {
        this.action =
            ActionService.getInstance().newService(LegacyAction.ACTION_NAME);
        this.actionArgs = Collections.singletonList(path.toString());
      } else {

        Common.showErrorMessageAndExit("Unknown action: "
            + actionName + ".\n" + "type: " + Globals.APP_NAME_LOWER_CASE
            + " -help for more help.\n");
      }
    }
  }

  //
  // Other methods
  //

  /**
   * Load the configuration file if exists.
   * @throws Aozan3Exception if an error occurs while reading the configuration
   *           file
   */
  private void loadConfigurationFile() throws Aozan3Exception {

    Path path = null;

    // Load the setting file if has been defined in command line
    if (this.configurationPath != null) {
      path = Paths.get(this.configurationPath);

      // Check if the configuration file exists
      if (!Files.isReadable(path)) {
        throw new Aozan3Exception("Configuration file not found: " + path);
      }
    } else {

      path = Paths.get(getApplicationDirectory().toString(), "conf",
          Globals.APP_NAME_LOWER_CASE);

      if (!Files.isReadable(path)) {
        path = null;
      }

    }

    if (path != null) {
      this.conf.load(path);
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param args command line argument.
   */
  Main(final String[] args) {

    this.args = Arrays.asList(args);

    // Parse the command line
    final int optionsCount = parseCommandLine();

    try {

      // Disable logging for Eoulsan runtime startup
      Aozan2Logger.getLogger().setLevel(Level.OFF);

      // Initialize Eoulsan runtime
      // LocalEoulsanRuntime.initEoulsanRuntimeForExternalApp();

      // Load configuration file (if needed)
      loadConfigurationFile();

      // Initialize log
      this.logger = AozanLoggerFactory.newLogger(this.conf, this.logger);

    } catch (Aozan3Exception e) {
      Common.errorExit(e, e.getMessage());
    }

    // Parse action name and action arguments from command line
    parseAction(optionsCount);
  }

  //
  // Main method
  //

  /**
   * Main method of the program.
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    if (main != null) {
      throw new IllegalAccessError("Main method cannot be run twice.");
    }

    // Set the default local for all the application
    Globals.setDefaultLocale();

    // Check Java version
    if (getJavaVersion() < MINIMAL_JAVA_VERSION_REQUIRED) {
      Common.showErrorMessageAndExit(Globals.WELCOME_MSG
          + "\nError: " + Globals.APP_NAME + " requires Java "
          + MINIMAL_JAVA_VERSION_REQUIRED + " (found Java " + getJavaVersion()
          + ").");
    }

    // Initialize the main class
    main = new Main(args);

    // Get the action to execute
    final Action action = main.getAction();

    try {

      main.getLogger().info("Start " + action.getName() + " action");

      // Run action
      action.action(main.getConfiguration(), main.getActionArgs(),
          main.getLogger());

      main.getLogger().info("End of " + action.getName() + " action");

    } catch (Throwable e) {
      Common.errorExit(e, e.getMessage());
    }
  }

}
