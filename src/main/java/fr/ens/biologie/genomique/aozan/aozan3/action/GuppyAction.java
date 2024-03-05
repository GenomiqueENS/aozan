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
import fr.ens.biologie.genomique.aozan.aozan3.dataprocessor.GuppyONTBasecallingDataProcessor;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define an guppy action that will launch Guppy.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class GuppyAction implements Action {

  /** Name of this action. */
  public static final String ACTION_NAME = "guppy";

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "exec guppy";
  }

  @Override
  public void action(Configuration conf, List<String> arguments,
      GenericLogger logger) {

    final Options options = makeOptions();
    final CommandLineParser parser = new DefaultParser();

    String flowcell = "";
    String kit = "";
    String barcodeKits = "";
    String config = "";
    String runId = "";
    String guppyVersion = "";
    String cudaDevice = "";
    int gpuRunnersPerDevice = -1;
    int chunksPerRunner = -1;
    boolean trimBarcode = false;
    boolean fast5Output = false;
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
        guppyVersion = line.getOptionValue("g");
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
        gpuRunnersPerDevice = Integer.parseInt(line.getOptionValue("u"));
      }

      if (line.hasOption("p")) {
        chunksPerRunner = Integer.parseInt(line.getOptionValue("p"));
      }

      if (line.hasOption("o")) {
        fast5Output = true;
      }

      if (line.hasOption("e")) {
        keepTemporaryFiles = true;
      }
      args = Arrays.asList(line.getArgs());

      if (args.size() < 3) {
        help(options);
      }

      System.out.println(args);

      final Path inputTar = Paths.get(args.get(0));
      final Path outputDir = Paths.get(args.get(1));
      final Path tmpPath = Paths.get(args.get(2));

      GuppyONTBasecallingDataProcessor.run(inputTar, outputDir, runId,
          guppyVersion, tmpPath, flowcell, kit, barcodeKits, trimBarcode,
          minQscore, config, cudaDevice, gpuRunnersPerDevice, chunksPerRunner,
          fast5Output, keepTemporaryFiles, logger);

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
    options.addOption(builder("g").longOpt("guppy-version").hasArg()
        .argName("version").desc("guppy version").build());

    // Flowcell option
    options.addOption(builder("f").longOpt("flow-cell-type").hasArg()
        .argName("type").desc("flow cell type").build());

    // Kit option
    options.addOption(builder("k").longOpt("kit").hasArg().argName("kitname")
        .desc("kit name").build());

    // Config option
    options.addOption(builder("c").longOpt("config").hasArg()
        .argName("configname").desc("configuration filename").build());

    // GPU device option
    options.addOption(builder("d").longOpt("device").hasArg()
        .argName("cudadevice").desc("Cuda device name").build());

    // GPU runners
    options.addOption(builder("u").longOpt("gpu-runners-per-device").hasArg()
        .argName("runners").desc("GPU runners per device").build());

    // Chunks per runner
    options.addOption(builder("p").longOpt("chunks-per-runner").hasArg()
        .argName("chunks").desc("chunks per runner").build());

    // Run id option
    options.addOption(builder("r").longOpt("run-id").hasArg().argName("id")
        .desc("run id").build());

    // Barcode kits option
    options.addOption(builder("b").longOpt("barcode-kits").hasArg()
        .argName("kits").desc("barcode kits").build());

    // Trim barcode option
    options.addOption("t", "trim-barcodes", false, "trim barcodes");

    // Fast5 output
    options.addOption("o", "fast5-output", false, "Fast5 output");

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
    formatter.printHelp(
        Globals.APP_NAME_LOWER_CASE
            + ".sh " + ACTION_NAME + " [options] inputTar outputDir tmpDir",
        options);

    Common.exit(0);
  }

}
