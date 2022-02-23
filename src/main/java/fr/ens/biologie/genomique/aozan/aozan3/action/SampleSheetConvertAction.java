package fr.ens.biologie.genomique.aozan.aozan3.action;

import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils.removeBclConvertDataForbiddenFields;
import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils.replaceUnderscoresByDashesInSampleIds;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Common;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetCSVWriter;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetReader;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetXLSReader;

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
      AozanLogger logger) {

    if (arguments.size() != 2) {
      Common.showErrorMessageAndExit(
          "Syntax: aozan.sh" + ACTION_NAME + " XLS_samplesheet output_dir");
    }

    File inputFile = new File(arguments.get(0));
    File outputDir = new File(arguments.get(1));
    File outputFile = new File(outputDir,
        inputFile.getName().subSequence(0, inputFile.getName().length() - 4)
            + ".csv");

    // Check if the input file name ends with ".xls"
    if (!inputFile.getName().toLowerCase().endsWith(".xls")) {
      Common.showErrorMessageAndExit(
          "Invalid input file, only XLS file are allowed: " + inputFile);
    }

    // Check if input file exists
    if (!Files.isRegularFile(inputFile.toPath())) {
      Common.showErrorMessageAndExit("Input file not found: " + inputFile);
    }

    // Check if output directory exists
    if (!Files.isDirectory(outputDir.toPath())) {
      Common
          .showErrorMessageAndExit("Output directory not found: " + outputDir);
    }

    // If the input and output files have the same date do nothing
    if (Files.isRegularFile(outputFile.toPath())
        && inputFile.lastModified() == outputFile.lastModified()) {
      return;
    }

    SampleSheet sampleSheet = null;

    // Read the input sample sheet
    try (SampleSheetReader reader = new SampleSheetXLSReader(inputFile)) {
      sampleSheet = reader.read();
    } catch (IOException e) {
      Common.showErrorMessageAndExit(
          "Error while reading sample sheet: " + inputFile);
    }

    // Fix sample sheet
    try {
      replaceUnderscoresByDashesInSampleIds(sampleSheet);
      removeBclConvertDataForbiddenFields(sampleSheet);
    } catch (KenetreException e1) {
      Common.showErrorMessageAndExit(
          "Error while converting sample sheet: " + inputFile);
    }

    // Write the output sample sheet in CSV format
    try (SampleSheetCSVWriter writer = new SampleSheetCSVWriter(outputFile)) {
      writer.writer(sampleSheet);
    } catch (IOException e) {
      Common.showErrorMessageAndExit(
          "Error while reading sample sheet: " + inputFile);
    }

    // Set the last modified time of the output file as the same of the input
    // file
    outputFile.setLastModified(inputFile.lastModified());
  }

}
