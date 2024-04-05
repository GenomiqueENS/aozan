package fr.ens.biologie.genomique.aozan.aozan3.samplesheetconverter;

import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet.BCL2FASTQ_DEMUX_TABLE_NAME;
import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet.BCLCONVERT_DEMUX_TABLE_NAME;
import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils.removeBclConvertDataForbiddenFields;
import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils.replaceUnderscoresByDashesInSampleIds;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.PropertySection;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetChecker;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.TableSection;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetCSVWriter;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetReader;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetXLSReader;

/**
 * This class define a converter between an Illumina samplesheet in XLS format
 * to CSV.
 * @author Laurent Jourdren
 * @since 3.1
 */
public class IlluminaSampleSheetConverter extends AbstractSampleSheetConverter {

  private SampleSheet sampleSheet;

  @Override
  protected void loadSampleSheet() throws Aozan3Exception {

    // Read the input sample sheet
    try (SampleSheetReader reader = new SampleSheetXLSReader(this.inputFile)) {
      this.sampleSheet = reader.read();
    } catch (IOException e) {
      throw new Aozan3Exception(
          "Error while reading sample sheet: " + inputFile);
    }

  }

  @Override
  protected void fixSampleSheet(List<String> warnings) throws Aozan3Exception {

    // Fix bad column names
    warnings.addAll(fixColumnNames(sampleSheet));

    // Fix sample sheet
    try {
      replaceUnderscoresByDashesInSampleIds(sampleSheet);
    } catch (KenetreException e) {
      throw new Aozan3Exception(
          "Error while converting sample sheet: " + inputFile);
    }

    // Fix potential errors in BCLConvert_Settings section
    warnings.addAll(fixBCLConvertSettings(sampleSheet));
  }

  @Override
  protected void checkSampleSheet(List<String> warnings)
      throws Aozan3Exception {

    // Check the samplesheet
    try {

      SampleSheetChecker checker = new SampleSheetChecker();
      warnings.addAll(checker.checkSampleSheet(sampleSheet));
      checkSettingsSections(sampleSheet);

    } catch (KenetreException e) {
      throw new Aozan3Exception("Error while checking sample sheet: "
          + inputFile + "\n" + e.getMessage());
    }
  }

  @Override
  protected void saveSampleSheet() throws Aozan3Exception {

    // Fix sample sheet
    try {
      removeBclConvertDataForbiddenFields(sampleSheet);
    } catch (KenetreException e) {
      throw new Aozan3Exception(
          "Error while converting sample sheet: " + inputFile);
    }

    // Write the output sample sheet in CSV format
    try (SampleSheetCSVWriter writer = new SampleSheetCSVWriter(outputFile)) {
      writer.writer(sampleSheet);
    } catch (IOException e) {
      throw new Aozan3Exception(
          "Error while reading sample sheet: " + inputFile);
    }

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
  // Constructor
  //

  /**
   * Abstract constructor.
   * @param inputFile input file
   * @param outputDir output directory
   */
  public IlluminaSampleSheetConverter(File inputFile, File outputDir) {

    super(inputFile, outputDir);
  }

}
