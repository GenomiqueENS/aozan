/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */
package fr.ens.biologie.genomique.aozan.illumina.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.python.google.common.base.Strings;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheetUtils;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVWriter;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetXLSReader;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import junit.framework.TestCase;

public class SampleSheetReaderTest extends TestCase {

  // TODO Use Junit 4.11 instead of 3.8
  
  private static final Splitter NEWLINE_SPLITTER =
      Splitter.on("\n").trimResults().omitEmptyStrings();

  private static final Splitter COMMA_SPLITTER = Splitter.on(",").trimResults();

  // private static final String EXPECTED_WIHTHEADER_SMALLER_CSV = "[Header]\n"
  // + "IEMFileVersion,4\n" + "Experiment Name,141216\n" + "Date,17/12/2014\n"
  // + "Workflow,GenerateFASTQ\n" + "Application,NextSeq FASTQ Only"
  // + "Assay,TruSeq LT\n" + "Description\n" + "Chemistry,Default\n\n"
  // + "[Reads]\n43\n43\n\n" + "[Settings]\n\n" + "[Data]\nSample_ID\n"
  // + "2015_067\n" + "2015_068\n" + "2015_069";

  private static final String EXPECTED_WIHTHEADER_SMALLER_CSV =
      "Sample_ID,index,Sample_Project,Description,Sample_Ref\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis";

  private static final String EXPECTED_SMALLER_CSV =
      "[Data]\nSample_ID\n" + "2015_067\n" + "2015_068\n" + "2015_069";

  private static final String EXPECTED_CSV =
      "Sample_ID,index,Sample_Project,Description,Sample_Ref\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis";

  private static final String EXPECTED_WITH_DUALINDEX_CSV =
      "Sample_ID,index,Sample_Project,Description,Sample_Ref,index2\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,GTCGAT\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,GATGTG\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,TAGAGT";

  private static final String EXPECTED_CSV_FULL =
      "Sample_ID,index,Sample_Project,Description,Sample_Ref,lane,FCID,Control,Recipe,Operator\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,1,H9RLKADXX,N,R1,plateform,\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,1,H9RLKADXX,N,R1,plateform,\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,1,H9RLKADXX,N,R1,plateform,\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,2,H9RLKADXX,N,R1,plateform,\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,2,H9RLKADXX,N,R1,plateform,\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,2,H9RLKADXX,N,R1,plateform";

  private static final String EXPECTED_FULL_WITH_DUALINDEX_CSV =
      "Sample_ID,index,Sample_Project,Description,Sample_Ref,lane,index2,FCID,Control,Recipe,Operator\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,1,GTCGAT,H9RLKADXX,N,R1,plateform\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,1,GATGTG,H9RLKADXX,N,R1,plateform\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,1,TAGAGT,H9RLKADXX,N,R1,plateform\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,2,GTCGAT,H9RLKADXX,N,R1,plateform\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,2,GATGTG,H9RLKADXX,N,R1,plateform\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,2,TAGAGT,H9RLKADXX,N,R1,plateform";

  private static final String SAMPLESHEET_BCL2FASTQ_V2_FILENAME =
      "samplesheet_version_bcl2fastq2.xls";

  private static final String SAMPLESHEET_DUALINDEX_BCL2FASTQ_V2_FILENAME =
      "samplesheet_version_bcl2fastq2_v2.xls";

  private static final String SAMPLESHEET_WITH_HEADER_BCL2FASTQ_V2_FILENAME =
      "samplesheet_short_bcl2fastq2.xls";

  private static final String SAMPLESHEET_BCL2FASTQ_V1_FILENAME =
      "design_version_bcl2fastq.xls";

  private static final String SAMPLESHEET_CSV = "samplesheet.csv";

  private String path;
  private File outputFile;

  public void testReadsWithHeaderVersion2() {

    testSampleSheet(SAMPLESHEET_WITH_HEADER_BCL2FASTQ_V2_FILENAME, 2,
        EXPECTED_WIHTHEADER_SMALLER_CSV, false);
  }

  private void testSampleSheet(final String samplesheetFileToTest,
      final int version, final String expectedDataName,
      final boolean withLane) {

    final File samplesheet = new File(path, samplesheetFileToTest);

    int laneNumber = (withLane ? 2 : -1);

    // Create CSV file
    convertSamplesheetToCSV(samplesheet, outputFile, 0, version);

    // Load sample sheet CSV
    final SampleSheet samplesheetTested =
        readSamplesheetCSV(outputFile, laneNumber);

    // Compare with expected content
    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(expectedDataName, version);

    compareSamplesheetV2(samplesheetExpected, samplesheetTested, withLane);
  }

  /**
   * Test reads short csv version.
   */
  public void testReadsShortCSVVersion() {

    try {
      new SampleSheetCSVReader(
          new ByteArrayInputStream(EXPECTED_SMALLER_CSV.getBytes())).read();

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  /**
   * Reads xls sample sheet file and check sample sheet instance is the same
   * that expected, with right converting index sequences.
   */
  public void testReadsXLSVersion2WithoutLaneColumnToCreate() {

    testSampleSheet(SAMPLESHEET_BCL2FASTQ_V2_FILENAME, 2, EXPECTED_CSV, false);
  }

  public void testReadsXLSVersion2WithLaneColumnToCreate() {

    testSampleSheet(SAMPLESHEET_BCL2FASTQ_V2_FILENAME, 2, EXPECTED_CSV_FULL,
        true);

  }

  public void testReadsXLSVersion2DualIndexWithLaneColumnToCreate() {

    testSampleSheet(SAMPLESHEET_DUALINDEX_BCL2FASTQ_V2_FILENAME, 2,
        EXPECTED_FULL_WITH_DUALINDEX_CSV, true);
  }

  public void testReadsCSVVersion2WithLaneColumnToCreate() {

    final int laneCount = 2;

    final File csvFile = writeCSVFromTabulatedString(EXPECTED_CSV);

    // Load samplesheet csv
    final SampleSheet samplesheetTested =
        readSamplesheetCSV(csvFile, laneCount);

    // Compare with expected content with content column lane
    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(EXPECTED_CSV_FULL, 2);

    assertTrue("no sample read in expected string ",
        samplesheetExpected.size() > 0);

    compareSamplesheetV2(samplesheetExpected, samplesheetTested, true);
  }

  public void testReadsCSVVersion2WithoutLaneColumnToCreate() {

    final File csvFile = writeCSVFromTabulatedString(EXPECTED_CSV);

    // Load samplesheet csv
    final SampleSheet samplesheetTested = readSamplesheetCSV(csvFile, 0);

    // Compare with expected content with content column lane
    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(EXPECTED_CSV, 2);

    assertTrue("no sample read in expected string ",
        samplesheetExpected.size() > 0);

    compareSamplesheetV2(samplesheetExpected, samplesheetTested, false);
  }

  /**
   * Reads xls samplesheet file and check samplesheet instance is the same that
   * expected, with right converting index sequences.
   */
  public void testReadsXLSVersion1() {

    final File samplesheet = new File(path, SAMPLESHEET_BCL2FASTQ_V1_FILENAME);
    final int laneCount = 2;

    // Create CSV file
    convertSamplesheetToCSV(samplesheet, outputFile, laneCount, 1);

    // Load samplesheet csv
    final SampleSheet samplesheetTested =
        readSamplesheetCSV(outputFile, laneCount);

    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(EXPECTED_CSV_FULL, 1);

    // Compare with expected content
    compareSamplesheetV1(samplesheetExpected, samplesheetTested);
  }

  //
  // Private methods
  //

  private void compareSamplesheetV2(
      final Map<String, SampleSheetTest> samplesheetExpected,
      final SampleSheet tested, final boolean withLane) {

    assertFalse("No sample expected loaded ", samplesheetExpected.isEmpty());

    for (Sample e : tested) {

      final String sampleId = e.getSampleId();
      final int laneNumber = e.getLane() == -1 ? 0 : e.getLane();

      if (withLane)
        assertFalse("Lane number should be define in " + e, laneNumber == 0);

      final SampleSheetTest expected =
          samplesheetExpected.get(sampleId + "_" + laneNumber);

      assertNotNull("Sample id "
          + sampleId + "_" + laneNumber
          + " not found in expected dataset, it contains\n" + Joiner.on("\n\t")
              .withKeyValueSeparator(",").join(samplesheetExpected),
          expected);

      compareSamplesheetEntryV2(expected, e, withLane);

      // Remove entry in expected map
      samplesheetExpected.remove(sampleId + "_" + laneNumber);
    }

    assertEquals(
        "expected sample(s) missing: "
            + Joiner.on(",").join(samplesheetExpected.keySet()),
        0, samplesheetExpected.size());
  }

  private void compareSamplesheetEntryV2(final SampleSheetTest expected,
      final Sample tested, final boolean withLane) {

    assertEquals("Sample ref", expected.getSampleRef(), tested.getSampleRef());
    assertEquals("Sample description", expected.getDescription(),
        tested.getDescription());
    assertEquals("Sample project", expected.getSampleProject(),
        tested.getSampleProject());
    assertEquals("Sample index", expected.getIndex(),
        Strings.nullToEmpty(tested.getIndex1()));

    assertEquals("Sample index2", expected.getIndex2(),
        Strings.nullToEmpty(tested.getIndex2()));

    if (withLane) {
      assertEquals("Sample lane", expected.getLane(), tested.getLane());
    }
  }

  private void compareSamplesheetV1(
      final Map<String, SampleSheetTest> samplesheetExpected,
      final SampleSheet tested) {

    for (Sample s : tested) {

      final String sampleId = s.getSampleId();
      final int lane = s.getLane();

      final SampleSheetTest expected =
          samplesheetExpected.get(sampleId + "_" + lane);

      assertNotNull("Sample id " + sampleId + "not found in expected dataset",
          Joiner.on(",").join(samplesheetExpected.keySet()));

      assertEquals("Sample ref", expected.getSampleRef(), s.getSampleRef());
      assertEquals("Sample description", expected.getDescription(),
          s.getDescription());
      assertEquals("Sample project", expected.getSampleProject(),
          s.getSampleProject());
      assertEquals("Sample index", expected.getIndex(), s.getIndex1());

      assertEquals("Sample lane", expected.getLane(), s.getLane());
      assertEquals("flowcell id", expected.getFCID(),
          s.getSampleSheet().getFlowCellId());
      assertEquals("Recipe", expected.getRecipe(), s.get("Recipe"));
      assertEquals("operator", expected.getOperator(), s.get("Operator"));

      // Remove entry in expected map
      samplesheetExpected.remove(sampleId + "_" + lane);
    }

    assertEquals(
        "expected sample(s) missing: "
            + Joiner.on(",").join(samplesheetExpected.keySet()),
        samplesheetExpected.size(), 0);
  }

  private File writeCSVFromTabulatedString(String stringCSV) {

    try (Writer writer = FileUtils.createFastBufferedWriter(outputFile)) {
      writer.write("[Data]\n");
      writer.write(stringCSV);
      writer.flush();

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return outputFile;
  }

  private SampleSheet readSamplesheetCSV(final File file, final int laneCount) {

    try {

      final SampleSheet result = new SampleSheetCSVReader(file).read();

      if (!result.isLaneSampleField() && laneCount > 1) {
        SampleSheetUtils.duplicateSamplesIfLaneFieldNotSet(result, laneCount);
      }

      return result;

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    throw new RuntimeException();

  }

  private void convertSamplesheetToCSV(final File samplesheet,
      final File outputFile, final int laneCount, final int version) {

    try {

      SampleSheetXLSReader reader = new SampleSheetXLSReader(samplesheet);
      reader.setVersion(version);

      final SampleSheet result = reader.read();

      if (!result.isLaneSampleField() && laneCount > 0) {
        SampleSheetUtils.duplicateSamplesIfLaneFieldNotSet(result, laneCount);
      }

      // Replace index sequence shortcuts by sequences
      SampleSheetUtils.replaceIndexShortcutsBySequences(result,
          loadIndexSequences());

      // Write CSV samplesheet file
      SampleSheetCSVWriter writer = new SampleSheetCSVWriter(outputFile);
      writer.setVersion(version);
      writer.writer(result);

    } catch (IOException | AozanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private Map<String, String> loadIndexSequences() {

    final Map<String, String> indexes = new HashMap<>();
    indexes.put("b1", "CGATGT");
    indexes.put("b2", "TGACCA");
    indexes.put("b4", "GCCAAT");

    return Collections.unmodifiableMap(indexes);
  }

  private Map<String, SampleSheetTest> buildSamplesheetExpected(
      final String expected, final int version) {

    final Map<String, SampleSheetTest> s = new TreeMap<>();
    boolean first = true;

    boolean isLaneColumnExist = false;
    boolean isIndex2ColumnExist = false;

    for (String line : NEWLINE_SPLITTER.splitToList(expected)) {

      if (first) {
        // Skip header
        first = false;
        isLaneColumnExist = line.contains("lane");
        isIndex2ColumnExist = line.contains("index2");

      } else {

        final SampleSheetTest sample = new SampleSheetTest(line, version,
            isLaneColumnExist, isIndex2ColumnExist);
        s.put(sample.getKey(), sample);
      }
    }

    return s;
  }

  //
  // Common methods
  //

  @Override
  public void setUp() {

    // Path to samplesheet directory
    path = new File(
        new File(".").getAbsolutePath() + "/src/test/java/files/samplesheets")
            .getAbsolutePath();
    outputFile = new File(path, SAMPLESHEET_CSV);

  }

  public void tearDown() {

    if (outputFile.exists()) {
      outputFile.delete();
    }
  }

  //
  // Internal class
  //

  class SampleSheetTest {

    private final boolean index2ColumnExist;
    private final boolean laneColumnExist;

    private String sampleID;
    private String index;
    private String sampleProject;
    private String description;
    private String sampleRef;
    private boolean optionalFieds;

    // Additional fields
    private String index2 = "";
    private int lane = 0;

    private String FCID = "";
    private String control = "";
    private String recipe = "";
    private String operator = "";

    private void parse(final String csv) {

      Iterator<String> fields = COMMA_SPLITTER.split(csv).iterator();
      // fields.next();

      this.sampleID = fields.next();
      this.index = fields.next();
      this.sampleProject = fields.next();
      this.description = fields.next();
      this.sampleRef = fields.next();

      if (laneColumnExist) {
        this.lane = Integer.parseInt(fields.next());
      }

      if (index2ColumnExist) {
        this.index2 = fields.next();
      }

      if (optionalFieds) {
        this.FCID = (fields.hasNext() ? fields.next() : "");
        this.control = (fields.hasNext() ? fields.next() : "");
        this.recipe = (fields.hasNext() ? fields.next() : "");
        this.operator = (fields.hasNext() ? fields.next() : "");
      }
    }

    //
    // Getters
    //

    public String getKey() {
      return getSampleID() + "_" + getLane();
    }

    public String getSampleID() {
      return sampleID;
    }

    public String getIndex() {
      return index;
    }

    public String getIndex2() {
      return index2;
    }

    public String getSampleProject() {
      return sampleProject;
    }

    public String getDescription() {
      return description;
    }

    public String getSampleRef() {
      return sampleRef;
    }

    public int getLane() {
      return lane;
    }

    public String getFCID() {
      return FCID;
    }

    public String getControl() {
      return control;
    }

    public String getRecipe() {
      return recipe;
    }

    public String getOperator() {
      return operator;
    }

    @Override
    public String toString() {
      return "SampleSheetTest [ lane= "
          + lane + ", sampleID=" + sampleID + ", index=" + index
          + ", sampleProject=" + sampleProject + ", description=" + description
          + ", sampleRef=" + sampleRef + "]";
    }

    //
    // Constructor
    //
    SampleSheetTest(final String line, final int version,
        boolean isLaneColumnExist, boolean isIndex2ColumnExist) {
      this.optionalFieds = (version == 1);
      this.laneColumnExist = isLaneColumnExist;
      this.index2ColumnExist = isIndex2ColumnExist;
      parse(line);

    }

  }
}
