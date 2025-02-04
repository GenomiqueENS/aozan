package fr.ens.biologie.genomique.aozan.aozan3.samplesheetconverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;

/**
 * This abstract class define a converter between a samplesheet in XLS format to
 * CSV.
 * @author Laurent Jourdren
 * @since 3.1
 */
public abstract class AbstractSampleSheetConverter {

  protected final File inputFile;
  protected final File outputDir;
  protected final File outputFile;
  protected final File errorFile;
  protected final File warningFile;

  private final List<String> warnings = new ArrayList<>();

  /**
   * Convert the samplesheet.
   * @throws Aozan3Exception if an error occurs while converting the samplesheet
   */
  public void convert() throws Aozan3Exception {

    // Check if the input file name ends with ".xls"
    if (!this.inputFile.getName().toLowerCase().endsWith(".xls")) {
      throw new Aozan3Exception(
          "Invalid input file, only XLS file are allowed: " + this.inputFile);
    }

    // Check if input file exists
    if (!Files.isRegularFile(this.inputFile.toPath())) {
      throw new Aozan3Exception("Input file not found: " + this.inputFile);
    }

    // Check if output directory exists
    if (!Files.isDirectory(this.outputDir.toPath())) {
      throw new Aozan3Exception(
          "Output directory not found: " + this.outputDir);
    }

    // If the input and output files have the same date do nothing
    if (Files.isRegularFile(this.outputFile.toPath())
        && this.inputFile.lastModified() == this.outputFile.lastModified()) {
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

    loadSampleSheet();
    fixSampleSheet(this.warnings);
    checkSampleSheet(this.warnings);
    saveSampleSheet();

    // Set the last modified time of the output file as the same of the input
    // file
    this.outputFile.setLastModified(this.inputFile.lastModified());

    // Save the warning in a dedicated file
    saveWarnings(this.warnings, this.inputFile, this.warningFile);
  }

  /**
   * Load the samplesheet.
   * @throws Aozan3Exception if an error occurs while loading the samplesheet
   */
  protected abstract void loadSampleSheet() throws Aozan3Exception;

  /**
   * Fix the samplesheet.
   * @throws Aozan3Exception if an error occurs while fixing the samplesheet
   */
  protected abstract void fixSampleSheet(List<String> warnings)
      throws Aozan3Exception;

  /**
   * Check the samplesheet.
   * @throws Aozan3Exception if an error occurs while checking the samplesheet
   */
  protected abstract void checkSampleSheet(List<String> warnings)
      throws Aozan3Exception;

  /**
   * Save the samplesheet.
   * @throws Aozan3Exception if an error occurs while saving the samplesheet
   */
  protected abstract void saveSampleSheet() throws Aozan3Exception;

  //
  // Method to define files
  //

  private static File defineFile(File inputFile, File destDir,
      String extension) {

    return new File(destDir,
        inputFile.getName().subSequence(0, inputFile.getName().length() - 4)
            + extension);
  }

  public File defineOutputFile(File inputFile, File destDir) {

    return defineFile(inputFile, destDir, ".csv");
  }

  public File defineErrorFile(File inputFile, File destDir) {

    return defineFile(inputFile, destDir, "-ERROR.txt");
  }

  public File defineWarningFile(File inputFile, File destDir) {

    return defineFile(inputFile, destDir, "-WARNING.txt");
  }

  //
  // Output files saving
  //

  /**
   * Save error message.
   * @param e exception
   */
  public void saveError(Aozan3Exception e) {

    saveFile(e.getMessage(), this.inputFile, this.errorFile);
  }

  /**
   * Save warning message.
   * @param warnings warnings
   * @param inputFile input file
   * @param warningFile warning file
   */
  protected static void saveWarnings(List<String> warnings, File inputFile,
      File warningFile) {

    if (warnings == null || warnings.isEmpty()) {
      return;
    }

    saveFile(String.join("\n", warnings), inputFile, warningFile);
  }

  /**
   * Save file.
   * @param inputFile input file
   * @param outputFile output file
   */
  protected static void saveFile(String text, File inputFile, File outputFile) {

    if (text == null || text.isEmpty()) {
      return;
    }

    try (Writer writer = new FileWriter(outputFile, Charset.defaultCharset())) {

      writer.write(text);
      writer.write('\n');

    } catch (IOException e) {
    }

    // Set the last modified time of the error file as the same of the input
    // file
    outputFile.setLastModified(inputFile.lastModified());
  }

  //
  // cConstructor
  //

  /**
   * Abstract constructor.
   * @param inputFile input file
   * @param outputDir output directory
   */
  AbstractSampleSheetConverter(File inputFile, File outputDir) {

    this.inputFile = inputFile;
    this.outputDir = outputDir;

    this.outputFile = defineOutputFile(inputFile, outputDir);
    this.errorFile = defineErrorFile(inputFile, outputDir);
    this.warningFile = defineWarningFile(inputFile, outputDir);
  }

}
