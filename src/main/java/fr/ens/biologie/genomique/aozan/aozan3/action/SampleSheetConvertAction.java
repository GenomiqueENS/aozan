package fr.ens.biologie.genomique.aozan.aozan3.action;

import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet.BCL2FASTQ_DEMUX_TABLE_NAME;
import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet.BCLCONVERT_DEMUX_TABLE_NAME;
import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetCheck.checkSampleSheet;
import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils.removeBclConvertDataForbiddenFields;
import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils.replaceUnderscoresByDashesInSampleIds;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.Common;
import fr.ens.biologie.genomique.aozan.aozan3.Configuration;
import fr.ens.biologie.genomique.aozan.aozan3.SendMail;
import fr.ens.biologie.genomique.aozan.aozan3.log.AozanLogger;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.PropertySection;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.TableSection;
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

    // Configure emails
    SendMail sendMail = null;

    try {
      sendMail = new SendMail(conf, logger);
    } catch (Aozan3Exception e) {
      Common.showErrorMessageAndExit("Unable to configure mail server");
    }

    File inputFile = new File(arguments.get(0));
    File outputDir = new File(arguments.get(1));

    try {
      convert(inputFile, outputDir);
    } catch (Aozan3Exception e) {
      saveError(e, inputFile, defineErrorFile(inputFile, outputDir));
      sendMail.sendMail("[Samplesheet converter] Error", e.getMessage(), true);
    }
  }

  private static void convert(File inputFile, File outputDir)
      throws Aozan3Exception {

    File outputFile = defineOutputFile(inputFile, outputDir);
    File errorFile = defineErrorFile(inputFile, outputDir);
    File warningFile = defineWarningFile(inputFile, outputDir);

    List<String> warnings = new ArrayList<>();

    // Check if the input file name ends with ".xls"
    if (!inputFile.getName().toLowerCase().endsWith(".xls")) {
      throw new Aozan3Exception(
          "Invalid input file, only XLS file are allowed: " + inputFile);
    }

    // Check if input file exists
    if (!Files.isRegularFile(inputFile.toPath())) {
      throw new Aozan3Exception("Input file not found: " + inputFile);
    }

    // Check if output directory exists
    if (!Files.isDirectory(outputDir.toPath())) {
      throw new Aozan3Exception("Output directory not found: " + outputDir);
    }

    // If the input and output files have the same date do nothing
    if (Files.isRegularFile(outputFile.toPath())
        && inputFile.lastModified() == outputFile.lastModified()) {
      return;
    }

    // Remove output samplesheet
    if (outputFile.exists()) {
      outputFile.delete();
    }

    // Remove existing error file
    if (errorFile.exists()) {
      errorFile.delete();
    }

    // Remove existing error file
    if (warningFile.exists()) {
      errorFile.delete();
    }

    SampleSheet sampleSheet = null;

    // Read the input sample sheet
    try (SampleSheetReader reader = new SampleSheetXLSReader(inputFile)) {
      sampleSheet = reader.read();
    } catch (IOException e) {
      throw new Aozan3Exception(
          "Error while reading sample sheet: " + inputFile);
    }

    // Fix bad column names
    warnings.addAll(fixColumnNames(sampleSheet));

    // Fix sample sheet
    try {

      replaceUnderscoresByDashesInSampleIds(sampleSheet);
      removeBclConvertDataForbiddenFields(sampleSheet);
    } catch (KenetreException e) {
      throw new Aozan3Exception(
          "Error while converting sample sheet: " + inputFile);
    }

    // Fix potential errors in BCLConvert_Settings section
    warnings.addAll(fixBCLConvertSettings(sampleSheet));

    // Check the samplesheet
    try {

      warnings.addAll(checkSampleSheet(sampleSheet));
      checkSettingsSections(sampleSheet);

    } catch (KenetreException e) {
      throw new Aozan3Exception("Error while checking sample sheet: "
          + inputFile + "\n" + e.getMessage());
    }

    // Write the output sample sheet in CSV format
    try (SampleSheetCSVWriter writer = new SampleSheetCSVWriter(outputFile)) {
      writer.writer(sampleSheet);
    } catch (IOException e) {
      throw new Aozan3Exception(
          "Error while reading sample sheet: " + inputFile);
    }

    // Set the last modified time of the output file as the same of the input
    // file
    outputFile.setLastModified(inputFile.lastModified());

    // Save the warning in a dedicated file
    saveWarnings(warnings, inputFile, warningFile);
  }

  //
  // Check methods
  //

  /**
   * Check setting sections of the sample sheet.
   * @param sampleSheet
   * @throws KenetreException if error is found in sample sheet
   */
  private static void checkSettingsSections(SampleSheet sampleSheet)
      throws KenetreException {

    for (String sectionName : sampleSheet.getSections()) {

      if (sampleSheet.containsPropertySection(sectionName)
          && (sectionName.toLowerCase().trim().endsWith("_Settings")
              || "header".equals(sectionName.trim().toLowerCase())
              || "reads".equals(sectionName.trim().toLowerCase()))) {

        PropertySection section = sampleSheet.getPropertySection(sectionName);

        for (Map.Entry<String, String> e : section.entrySet()) {

          if (e.getValue() == null || e.getValue().isBlank()) {
            throw new KenetreException("In "
                + sectionName + " section, the value of " + e.getKey()
                + " setting is empty.");
          }
        }
      }
    }
  }

  //
  // Fix samplesheet methods
  //

  /**
   * Fix column names in sample sheet.
   * @param sampleSheet the sample sheet
   * @return a list with the warnings
   */
  private static List<String> fixColumnNames(SampleSheet sampleSheet) {

    requireNonNull(sampleSheet);

    if (!(sampleSheet.containsSection(BCL2FASTQ_DEMUX_TABLE_NAME)
        || sampleSheet.containsSection(BCLCONVERT_DEMUX_TABLE_NAME))) {
      return emptyList();
    }

    TableSection demuxTable = sampleSheet.getDemuxSection();

    if (!demuxTable.isIndex1SampleField()
        && demuxTable.getSamplesFieldNames().contains("index1")) {

      for (Sample s : demuxTable.getSamples()) {

        s.setIndex1(s.get("index1"));
        s.remove("index1");
      }

      return singletonList("Rename \"index1\" column to \"index\".");
    }

    return emptyList();
  }

  /**
   * Fix column names in sample sheet.
   * @param sampleSheet the sample sheet
   * @return a list with the warnings
   */
  private static List<String> fixBCLConvertSettings(SampleSheet samplesheet) {

    requireNonNull(samplesheet);

    if (!samplesheet.containsPropertySection("Reads")) {
      return emptyList();
    }

    List<String> warnings = new ArrayList<>();

    PropertySection readSection = samplesheet.getPropertySection("Reads");

    boolean index1 = readSection.containsKey("Index1Cycles");
    boolean index2 = readSection.containsKey("Index2Cycles");

    if (samplesheet.containsPropertySection("BCLConvert_Settings")) {

      PropertySection section =
          samplesheet.getPropertySection("BCLConvert_Settings");

      if (section.containsKey("BarcodeMismatchesIndex1") && !index1) {
        section.remove("BarcodeMismatchesIndex1");
        warnings.add("Remove BarcodeMismatchesIndex1 setting"
            + " in BCLConvert_Settings section.");
      }

      if (section.containsKey("BarcodeMismatchesIndex2") && !index2) {
        section.remove("BarcodeMismatchesIndex2");
        warnings.add("Remove BarcodeMismatchesIndex2 setting"
            + " in BCLConvert_Settings section.");
      }
    }

    return warnings;
  }

  //
  // Output files saving
  //

  private static File defineFile(File inputFile, File destDir,
      String extension) {

    return new File(destDir,
        inputFile.getName().subSequence(0, inputFile.getName().length() - 4)
            + extension);
  }

  private static File defineOutputFile(File inputFile, File destDir) {

    return defineFile(inputFile, destDir, ".csv");
  }

  private static File defineErrorFile(File inputFile, File destDir) {

    return defineFile(inputFile, destDir, "-ERROR.txt");
  }

  private static File defineWarningFile(File inputFile, File destDir) {

    return defineFile(inputFile, destDir, "-WARNING.txt");
  }

  private static void saveError(Aozan3Exception e, File inputFile,
      File errorFile) {

    saveFile(e.getMessage(), inputFile, errorFile);
  }

  private static void saveWarnings(List<String> warnings, File inputFile,
      File warningFile) {

    if (warnings == null || warnings.isEmpty()) {
      return;
    }

    saveFile(String.join("\n", warnings), inputFile, warningFile);
  }

  private static void saveFile(String text, File inputFile, File outputFile) {

    if (text == null || text.isEmpty()) {
      return;
    }

    try (Writer writer = new FileWriter(outputFile, Charset.defaultCharset())) {

      writer.write(text);

    } catch (IOException e) {
    }

    // Set the last modified time of the error file as the same of the input
    // file
    outputFile.setLastModified(inputFile.lastModified());
  }

}
