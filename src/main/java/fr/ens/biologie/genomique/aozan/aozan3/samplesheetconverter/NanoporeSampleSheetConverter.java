package fr.ens.biologie.genomique.aozan.aozan3.samplesheetconverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.SampleSheetChecker;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetCSVWriter;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetReader;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetWriter;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetXLSReader;

/**
 * This class define a converter between a Nanopore samplesheet in XLS format to
 * CSV.
 * @author Laurent Jourdren
 * @since 3.1
 */
public class NanoporeSampleSheetConverter extends AbstractSampleSheetConverter {

  SampleSheet sampleSheet;

  @Override
  protected void loadSampleSheet() throws Aozan3Exception {

    // Read the input sample sheet
    try (SampleSheetReader reader = new SampleSheetXLSReader(super.inputFile)) {
      reader.addAllowedFields("sample_ref", "project");
      this.sampleSheet = reader.read();
    } catch (IOException e) {
      throw new Aozan3Exception("Error while reading sample sheet: "
          + inputFile + " caused by: " + e.getMessage());
    }

  }

  @Override
  protected void fixSampleSheet(List<String> warnings) throws Aozan3Exception {
    // Nothing to do
  }

  @Override
  protected void checkSampleSheet(List<String> warnings)
      throws Aozan3Exception {

    String experimentId = this.sampleSheet.getExperimentId();
    String basename = super.inputFile.getName().replace(".xls", "");

    // Check if filename is the same as the experiement id
    if (experimentId != null
        && !experimentId.isBlank() && !basename.equals(experimentId)) {
      throw new Aozan3Exception("Invalid \"experiment id\", expected \""
          + basename + "\", found \"" + experimentId + "\".");
    }

    SampleSheetChecker checker = new SampleSheetChecker();

    try {
      // Load additional Nanopore product codes
      checker.addKnownExpansionKits(
          loadNanoporeProductCodes("nanopore.expansion.kits.path"));
      checker.addKnownFlowCellProductCodes(
          loadNanoporeProductCodes("nanopore.flow.cell.product.codes.path"));
      checker.addKnownSequencingKits(
          loadNanoporeProductCodes("nanopore.sequencing.kits.path"));

      // Check sample sheet
      warnings.addAll(checker.check(this.sampleSheet));
    } catch (IOException | KenetreException e) {
      throw new Aozan3Exception(e);
    }

  }

  @Override
  protected void saveSampleSheet() throws Aozan3Exception {

    // Write the output sample sheet in CSV format
    try (SampleSheetWriter writer = new SampleSheetCSVWriter(this.outputFile)) {
      writer.writer(this.sampleSheet);
    } catch (IOException e) {
      throw new Aozan3Exception(
          "Error while writing sample sheet: " + inputFile);
    }

  }

  //
  // Private methods
  //

  /**
   * Load Nanopore product codes from a file
   * @param javaPropertyName Java property
   * @return a list with the product codes
   * @throws IOException if an error occurs while reading the file with product
   *           codes
   */
  private List<String> loadNanoporeProductCodes(String javaPropertyName)
      throws IOException {

    List<String> result = new ArrayList<>();

    String value = System.getProperty(javaPropertyName);
    if (value == null) {
      return Collections.emptyList();
    }

    for (String line : Files.readAllLines(Paths.get(value))) {

      line = line.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }

      result.add(line);
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Abstract constructor.
   * @param inputFile input file
   * @param outputDir output directory
   */
  public NanoporeSampleSheetConverter(File inputFile, File outputDir) {

    super(inputFile, outputDir);
  }

  // TODO Warning for duplicate alias

  public static void main(String[] args) throws Aozan3Exception {

    NanoporeSampleSheetConverter converter = new NanoporeSampleSheetConverter(
        new File("/home/jourdren/Bureau/xls/bidon.xls"), new File("/tmp"));
    converter.convert();
    System.out.println("OK.");

  }

}
