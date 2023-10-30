package fr.ens.biologie.genomique.aozan.aozan3.action;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Common;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.Globals;
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.DoradoONTBasecallingDataProcessor;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define an guppy action that will launch Dorado.
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
    final CommandLineParser parser = new GnuParser();

    String flowcell = "";
    String kit = "";
    String barcodeKits = "";
    String config = "";
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
      if (line.hasOption("t")) {
        trimBarcode = true;
      }

      if (line.hasOption("f")) {
        flowcell = line.getOptionValue("f");
      }

      if (line.hasOption("k")) {
        kit = line.getOptionValue("k");
      }

      if (line.hasOption("b")) {
        barcodeKits = line.getOptionValue("b");
      }

      if (line.hasOption("r")) {
        runId = line.getOptionValue("r");
      }

      if (line.hasOption("g")) {
        doradoVersion = line.getOptionValue("g");
      }

      if (line.hasOption("c")) {
        config = line.getOptionValue("c");
      }

      if (line.hasOption("m")) {
        config = line.getOptionValue("m");
      }

      if (line.hasOption("d")) {
        cudaDevice = line.getOptionValue("d");
      }

      if (line.hasOption("u")) {
        batchSize = Integer.parseInt(line.getOptionValue("u"));
      }

      if (line.hasOption("p")) {
        chunkSize = Integer.parseInt(line.getOptionValue("p"));
      }

      if (line.hasOption("e")) {
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
          trimBarcode, minQscore, config, cudaDevice, batchSize, chunkSize,
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

    // Guppy version option
    options.addOption(OptionBuilder.withLongOpt("dorado-version").hasArg()
        .withArgName("version").withDescription("dorado version").create('g'));

    // Flowcell option
    options.addOption(OptionBuilder.withLongOpt("flow-cell-type").hasArg()
        .withArgName("type").withDescription("flow cell type").create('f'));

    // Kit option
    options.addOption(OptionBuilder.withLongOpt("kit").hasArg()
        .withArgName("kitname").withDescription("kit name").create('k'));

    // Config option
    options.addOption(
        OptionBuilder.withLongOpt("config").hasArg().withArgName("configname")
            .withDescription("configuration filename").create('c'));

    // GPU device option
    options.addOption(
        OptionBuilder.withLongOpt("device").hasArg().withArgName("cudadevice")
            .withDescription("Cuda device name").create('d'));

    // Batch size
    options.addOption(OptionBuilder.withLongOpt("batch-size").hasArg()
        .withArgName("size").withDescription("batch size").create('u'));

    // Chunks size
    options.addOption(OptionBuilder.withLongOpt("chunk-size").hasArg()
        .withArgName("chunks").withDescription("chunk size").create('p'));

    // Run id option
    options.addOption(OptionBuilder.withLongOpt("run-id").hasArg()
        .withArgName("id").withDescription("run id").create('r'));

    // Barcode kits option
    options.addOption(OptionBuilder.withLongOpt("barcode-kits").hasArg()
        .withArgName("kits").withDescription("barcode kits").create('b'));

    // Trim barcode option
    options.addOption("t", "trim-barcodes", false, "trim barcodes");

    // Fast5 output
    options.addOption("e", "keep-temporary-files", false, "Fast5 output");

    // Barcode kits option
    options.addOption(
        OptionBuilder.withLongOpt("min-qscore").hasArg().withArgName("value")
            .withDescription("minimal qscore for pass reads").create('m'));

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
