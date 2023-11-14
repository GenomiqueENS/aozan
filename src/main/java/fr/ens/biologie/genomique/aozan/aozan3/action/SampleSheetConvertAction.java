package fr.ens.biologie.genomique.aozan.aozan3.action;

import java.io.File;
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
import fr.ens.biologie.genomique.aozan.aozan3.SendMail;
import fr.ens.biologie.genomique.aozan.aozan3.samplesheetconverter.AbstractSampleSheetConverter;
import fr.ens.biologie.genomique.aozan.aozan3.samplesheetconverter.IlluminaSampleSheetConverter;
import fr.ens.biologie.genomique.aozan.aozan3.samplesheetconverter.NanoporeSampleSheetConverter;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define a sample sheet convert action.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class SampleSheetConvertAction implements Action {

  /** Name of this action. */
  public static final String ACTION_NAME = "samplesheetconvert";

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "convert sample sheet";
  }

  @Override
  public boolean isHidden() {
    return false;
  }

  @Override
  public void action(Configuration conf, List<String> arguments,
      GenericLogger logger) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();
    boolean nanopore = false;

    CommandLine line = null;
    try {

      // parse the command line arguments
      line = parser.parse(options, arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      if (line.hasOption("t")) {

        String sequencingType = line.getOptionValue("t");
        switch (sequencingType.toLowerCase().trim()) {
        case "illumina":
          nanopore = false;
          break;
        case "nanopore":
          nanopore = true;
          break;
        default:
          Common.showErrorMessageAndExit(
              "Error while parsing command line arguments: Unknown sequencing type: "
                  + sequencingType);
          break;
        }
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing command line arguments: " + e.getMessage());
    }

    List<String> args = line.getArgList();

    if (args.size() != 2) {
      help(options);
      Common.exit(1);
    }

    // Configure emails
    SendMail sendMail = null;

    try {
      sendMail = new SendMail(conf, logger);
    } catch (Aozan3Exception e) {
      Common.showErrorMessageAndExit("Unable to configure mail server");
    }

    File inputFile = new File(args.get(0));
    File outputDir = new File(args.get(1));

    AbstractSampleSheetConverter converter = nanopore
        ? new NanoporeSampleSheetConverter(inputFile, outputDir)
        : new IlluminaSampleSheetConverter(inputFile, outputDir);

    try {
      converter.convert();
    } catch (Aozan3Exception e) {
      converter.saveError(e);
      sendMail.sendMail("[Samplesheet converter] Error", e.getMessage(), true);
    }
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

    // Help option
    options.addOption("h", "help", false, "display this help");

    // Description option
    options.addOption(OptionBuilder.withArgName("type").hasArg()
        .withDescription(
            "sequencing type (Illumina or Nanopore, default: Illumina)")
        .withLongOpt("sequencing-type").create('t'));

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
        + ".sh " + ACTION_NAME + " input_file output_dir", options);

    Common.exit(0);
  }

}
