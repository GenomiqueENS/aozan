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

    String flowcell = "";
    String kit = "";
    String barcodeKits = "";
    String modelName = "";
    String runId = "";
    String doradoVersion = "";
    String cudaDevice = "";
    int batchSize = -1;
    int chunkSize = -1;
    boolean trimBarcode = false;
    boolean keepTemporaryFiles = false;
    String minQscore = "";
    List<String> args = null;

    try {

      // parse the command line arguments
      final CommandLine line =
          parser.parse(options, arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      // Trim barcodes option
      if (line.hasOption("trim-barcodes")) {
        trimBarcode = true;
      }

      if (line.hasOption("flowcell")) {
        flowcell = line.getOptionValue("flowcell");
      }

      if (line.hasOption("kit")) {
        kit = line.getOptionValue("kit");
      }

      if (line.hasOption("barcode-kit")) {
        barcodeKits = line.getOptionValue("barcode-kit");
      }

      if (line.hasOption("run-id")) {
        runId = line.getOptionValue("run-id");
      }

      if (line.hasOption("dorado-version")) {
        doradoVersion = line.getOptionValue("dorado-version");
      }

      if (line.hasOption("config")) {
        modelName = line.getOptionValue("config");
      }

      if (line.hasOption("min-qscore")) {
        minQscore = line.getOptionValue("min-qscore");
      }

      if (line.hasOption("device")) {
        cudaDevice = line.getOptionValue("device");
      }

      if (line.hasOption("batch-size")) {
        batchSize = Integer.parseInt(line.getOptionValue("batch-size"));
      }

      if (line.hasOption("chunk-size")) {
        chunkSize = Integer.parseInt(line.getOptionValue("chunk-size"));
      }

      if (line.hasOption("keep-temporary-files")) {
        keepTemporaryFiles = true;
      }
      args = Arrays.asList(line.getArgs());

      if (args.size() < 4) {
        help(options);
      }

      final Path inputTar = Paths.get(args.get(0));
      final Path outputDir = Paths.get(args.get(1));
      final Path modelsPath = Paths.get(args.get(2));
      final Path tmpPath = Paths.get(args.get(3));

      DoradoONTBasecallingDataProcessor.run(inputTar, outputDir, modelsPath,
          runId, doradoVersion, tmpPath, flowcell, kit, barcodeKits,
          trimBarcode, minQscore, modelName, cudaDevice, batchSize, chunkSize,
          keepTemporaryFiles, logger);

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

    // Dorado version option
    options.addOption(builder("g").longOpt("dorado-version").hasArg()
        .argName("version").desc("dorado version").build());

    // Flowcell option
    options.addOption(builder("f").longOpt("flowcell").hasArg().argName("type")
        .desc("flow cell type").build());

    // Kit option
    options.addOption(builder("k").longOpt("kit").hasArg().argName("kitname")
        .desc("kit name").build());

    // Config option
    options.addOption(builder("c").longOpt("config").hasArg()
        .argName("configname").desc("configuration filename").build());

    // GPU device option
    options.addOption(builder("d").longOpt("device").hasArg()
        .argName("cudadevice").desc("Cuda device name").build());

    // Batch size
    options.addOption(builder("u").longOpt("batch-size").hasArg()
        .argName("size").desc("batch size").build());

    // Chunks size
    options.addOption(builder("p").longOpt("chunk-size").hasArg()
        .argName("chunks").desc("chunk size").build());

    // Run id option
    options.addOption(builder("r").longOpt("run-id").hasArg().argName("id")
        .desc("run id").build());

    // Barcode kits option
    options.addOption(builder("").longOpt("barcode-kit").hasArg()
        .argName("kits").desc("barcode kit").build());

    // Trim barcode option
    options.addOption("t", "trim-barcodes", false, "trim barcodes");

    // Fast5 output
    options.addOption("e", "keep-temporary-files", false, "Fast5 output");

    // Barcode kits option
    options.addOption(builder("m").longOpt("min-qscore").hasArg()
        .argName("value").desc("minimal qscore for pass reads").build());

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
