package fr.ens.biologie.genomique.aozan.aozan3.samplesheetconverter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetReader;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetXLSReader;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.io.SampleSheetCSVWriter;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.nanopore.samplesheet.SampleSheetChecker;

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
    try (SampleSheetReader reader = new SampleSheetXLSReader(this.inputFile)) {
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

    SampleSheetChecker checker = new SampleSheetChecker();
    try {
      checker.check(this.sampleSheet);
    } catch (KenetreException e) {
      throw new Aozan3Exception(e);
    }

  }

  @Override
  protected void saveSampleSheet() throws Aozan3Exception {

    // Write the output sample sheet in CSV format
    try (SampleSheetCSVWriter writer = new SampleSheetCSVWriter(outputFile)) {
      writer.writer(sampleSheet);
    } catch (IOException e) {
      throw new Aozan3Exception(
          "Error while writing sample sheet: " + inputFile);
    }

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

  public static void main(String[] args) throws Aozan3Exception {

    NanoporeSampleSheetConverter converter = new NanoporeSampleSheetConverter(
        new File(
            "/home/jourdren/shares-net/sequencages/nanopore/samplesheets/20241111_MonProjetD_A2024.xls"),
        new File("/tmp"));

    converter.convert();
  }

}
