package fr.ens.biologie.genomique.aozan.aozan3.action;

import static org.apache.commons.cli.Option.builder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Common;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.Globals;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.DoradoONTBasecallingDataProcessor;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define an "dorado" action that will launch Dorado.
 * @author Laurent Jourdren
 * @since 3.1
 */
public class DoradoAction implements Action {

  /** Name of this action. */
  public static final String ACTION_NAME = "dorado";

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "exec dorado";
  }

  @Override
  public void action(Configuration conf, List<String> arguments,
      GenericLogger logger) {

    final Options options = makeOptions();
    final CommandLineParser parser = new DefaultParser();
    final Configuration doradoConf = new Configuration();

    String runId = "";
    String cudaDevice = "";
    boolean keepTemporaryFiles = false;
    List<String> args = null;

    try {

      // parse the command line arguments
      final CommandLine line =
          parser.parse(options, arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      // Set the configuration settings
      if (line.hasOption('s')) {

        List<String> settings = Arrays.asList(line.getOptionValues('s'));

        Splitter splitter = Splitter.on('=').trimResults();
        for (String s : settings) {
          List<String> elements = splitter.splitToList(s);
          if (elements.size() != 2) {
            throw new ParseException("Invalid setting format: " + s);
          }
          doradoConf.set(elements.get(0).trim(), elements.get(1).trim());
        }

      }

      if (line.hasOption("run-id")) {
        runId = line.getOptionValue("run-id");
      }

      if (line.hasOption("device")) {
        cudaDevice = line.getOptionValue("device");
      }

      if (line.hasOption("keep-temporary-files")) {
        keepTemporaryFiles = true;
      }
      args = Arrays.asList(line.getArgs());

      if (args.size() < 3) {
        help(options);
      }

      final Path inputTar = Paths.get(args.get(0));
      final Path outputDir = Paths.get(args.get(1));
      final Path tmpPath = Paths.get(args.get(2));

      DoradoONTBasecallingDataProcessor.run(inputTar, outputDir, runId, tmpPath,
          cudaDevice, keepTemporaryFiles, doradoConf, logger);

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing command line arguments: " + e.getMessage());
    } catch (Aozan3Exception e) {
      e.printStackTrace();
      Common.errorExit(e, "Error");
    }

  }

  @Override
  public boolean isHidden() {
    return true;
  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  private static Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Define setting
    options.addOption(builder("s").longOpt("setting").hasArg()
        .argName("property=value").desc("set a configuration setting. This "
            + "option can be used several times")
        .build());

    // GPU device option
    options.addOption(builder("d").longOpt("device").hasArg()
        .argName("cudadevice").desc("Cuda device name").build());

    // Run id option
    options.addOption(builder("r").longOpt("run-id").hasArg().argName("id")
        .desc("run id").build());

    // Fast5 output
    options.addOption("e", "keep-temporary-files", false, "Fast5 output");

    // Help option
    options.addOption("h", "help", false, "display this help");

    return options;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + ACTION_NAME
        + " [options] inputTar outputDir modelsDir tmpDir", options);

    Common.exit(0);
  }

}
