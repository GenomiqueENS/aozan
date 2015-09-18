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
 *      http://www.transcriptome.ens.fr/aozan
 *
 */
package fr.ens.transcriptome.aozan.illumina.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntry;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils;
import fr.ens.transcriptome.aozan.io.CasavaDesignXLSReader;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class CasavaDesignReaderTest extends TestCase {

  private static final Splitter NEWLINE_SPLITTER = Splitter.on("\n")
      .trimResults().omitEmptyStrings();

  private static final Splitter COMMA_SPLITTER = Splitter.on(",").trimResults();

  private static final String EXPECTED_WIHTHEADER_SMALLER_CSV = "[Header]\n"
      + "IEMFileVersion,4\n" + "Experiment Name,141216\n" + "Date,17/12/2014\n"
      + "Workflow,GenerateFASTQ\n" + "Application,NextSeq FASTQ Only"
      + "Assay,TruSeq LT\n" + "Description\n" + "Chemistry,Default\n\n"
      + "[Reads]\n43\n43\n\n" + "[Settings]\n\n" + "[Data]\nSample_ID\n"
      + "2015_067\n" + "2015_068\n" + "2015_069";

  private static final String EXPECTED_SMALLER_CSV = "[Data]\nSample_ID\n"
      + "2015_067\n" + "2015_068\n" + "2015_069";

  private static final String EXPECTED_CSV =
      "Sample_ID,index,Sample_Project,Description,Sample_Ref,index2\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,";

  private static final String EXPECTED_WITH_DUALINDEX_CSV =
      "Sample_ID,index,Sample_Project,Description,Sample_Ref,index2\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,GTCGAT\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,GATGTG\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,TAGAGT";

  private static final String EXPECTED_CSV_FULL =
      "Sample_ID,index,Sample_Project,Description,Sample_Ref,index2,lane,FCID,Control,Recipe,Operator\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,,1,H9RLKADXX,N,R1,plateform,\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,,1,H9RLKADXX,N,R1,plateform,\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,,1,H9RLKADXX,N,R1,plateform,\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,,2,H9RLKADXX,N,R1,plateform,\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,,2,H9RLKADXX,N,R1,plateform,\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,,2,H9RLKADXX,N,R1,plateform";

  private static final String EXPECTED_FULL_WITH_DUALINDEX_CSV =
      "Sample_ID,index,Sample_Project,Description,Sample_Ref,index2,lane,FCID,Control,Recipe,Operator\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,GTCGAT,1,H9RLKADXX,N,R1,plateform\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,GATGTG,1,H9RLKADXX,N,R1,plateform\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,TAGAGT,1,H9RLKADXX,N,R1,plateform\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,GTCGAT,2,H9RLKADXX,N,R1,plateform\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,GATGTG,2,H9RLKADXX,N,R1,plateform\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,TAGAGT,2,H9RLKADXX,N,R1,plateform";

  private static final String SAMPLESHEET_BCL2FASTQ_V2_FILENAME =
      "samplesheet_version_bcl2fastq2.xls";

  private static final String SAMPLESHEET_DUALINDEX_BCL2FASTQ_V2_FILENAME =
      "samplesheet_version_bcl2fastq2_v2.xls";

  private static final String SAMPLESHEET_BCL2FASTQ_V1_FILENAME =
      "design_version_bcl2fastq.xls";
  private static final String SAMPLESHEET_CSV = "samplesheet.csv";

  private String path;
  private File outputFile;

  public void testReadsShortCSVVersion() {

    try {
      new CasavaDesignCSVReader(new ByteArrayInputStream(
          EXPECTED_SMALLER_CSV.getBytes())).read(SampleSheetUtils.VERSION_2);

    } catch (IOException | AozanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  /**
   * Reads xls samplesheet file and check samplesheet instance is the same that
   * expected, with right converting index sequences.
   */
  public void testReadsXLSVersion2WithoutLaneColumnToCreate() {

    final File samplesheet = new File(path, SAMPLESHEET_BCL2FASTQ_V2_FILENAME);
    final String bcl2fastqVersion = SampleSheetUtils.VERSION_2;

    // Create CSV file
    convertSamplesheetToCSV(samplesheet, bcl2fastqVersion, outputFile, 0);

    // Load samplesheet csv
    final SampleSheet samplesheetTested =
        readSamplesheetCSV(outputFile, bcl2fastqVersion, -1);

    // Compare with expected content
    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(EXPECTED_CSV, SampleSheetUtils.VERSION_2);

    compareSamplesheetV2(samplesheetExpected, samplesheetTested, false);
  }

  public void testReadsXLSVersion2WithLaneColumnToCreate() {

    final File samplesheet = new File(path, SAMPLESHEET_BCL2FASTQ_V2_FILENAME);
    final String bcl2fastqVersion = SampleSheetUtils.VERSION_2;
    final int laneCount = 2;

    // Create CSV file
    convertSamplesheetToCSV(samplesheet, bcl2fastqVersion, outputFile,
        laneCount);

    // Load samplesheet csv
    final SampleSheet samplesheetTested =
        readSamplesheetCSV(outputFile, bcl2fastqVersion, laneCount);

    // Compare with expected content with content column lane
    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(EXPECTED_CSV_FULL, SampleSheetUtils.VERSION_2);

    compareSamplesheetV2(samplesheetExpected, samplesheetTested, true);
  }

  public void testReadsXLSVersion2DualIndexWithLaneColumnToCreate() {

    final File samplesheet =
        new File(path, SAMPLESHEET_DUALINDEX_BCL2FASTQ_V2_FILENAME);
    final String bcl2fastqVersion = SampleSheetUtils.VERSION_2;
    final int laneCount = 2;

    // Create CSV file
    convertSamplesheetToCSV(samplesheet, bcl2fastqVersion, outputFile,
        laneCount);

    // Load samplesheet csv
    final SampleSheet samplesheetTested =
        readSamplesheetCSV(outputFile, bcl2fastqVersion, laneCount);

    // Compare with expected content with content column lane
    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(EXPECTED_FULL_WITH_DUALINDEX_CSV,
            SampleSheetUtils.VERSION_2);

    compareSamplesheetV2(samplesheetExpected, samplesheetTested, true);
  }

  public void testReadsCSVVersion2WithLaneColumnToCreate() {

    final String bcl2fastqVersion = SampleSheetUtils.VERSION_2;
    final int laneCount = 2;

    final File csvFile = writeCSVFromTabulatedString(EXPECTED_CSV);

    // Load samplesheet csv
    final SampleSheet samplesheetTested =
        readSamplesheetCSV(csvFile, bcl2fastqVersion, laneCount);

    // Compare with expected content with content column lane
    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(EXPECTED_CSV_FULL, SampleSheetUtils.VERSION_2);

    assertTrue("no sample read in expected string ",
        samplesheetExpected.size() > 0);

    compareSamplesheetV2(samplesheetExpected, samplesheetTested, true);
  }

  public void testReadsCSVVersion2WithoutLaneColumnToCreate() {

    final String bcl2fastqVersion = SampleSheetUtils.VERSION_2;

    final File csvFile = writeCSVFromTabulatedString(EXPECTED_CSV);

    // Load samplesheet csv
    final SampleSheet samplesheetTested =
        readSamplesheetCSV(csvFile, bcl2fastqVersion, 0);

    // Compare with expected content with content column lane
    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(EXPECTED_CSV, SampleSheetUtils.VERSION_2);

    assertTrue("no sample read in expected string ",
        samplesheetExpected.size() > 0);

    compareSamplesheetV2(samplesheetExpected, samplesheetTested, false);
  }

  /**
   * Reads xls samplesheet file and check samplesheet instance is the same that
   * expected, with right converting index sequences.
   */
  // public void testReadsXLSVersion1() {
  //
  // final File samplesheet = new File(path, SAMPLESHEET_BCL2FASTQ_V1_FILENAME);
  // final String bcl2fastqVersion = SampleSheetUtils.VERSION_1;
  // final int laneCount = 2;
  //
  // // Create CSV file
  // convertSamplesheetToCSV(samplesheet, bcl2fastqVersion, outputFile,
  // laneCount);
  //
  // // Load samplesheet csv
  // final SampleSheet samplesheetTested =
  // readSamplesheetCSV(outputFile, bcl2fastqVersion, laneCount);
  //
  // final Map<String, SampleSheetTest> samplesheetExpected =
  // buildSamplesheetExpected(EXPECTED_CSV_FULL, SampleSheetUtils.VERSION_1);
  //
  // // Compare with expected content
  // compareSamplesheetV1(samplesheetExpected, samplesheetTested);
  // }

  //
  // Private methods
  //

  private void compareSamplesheetV2(
      final Map<String, SampleSheetTest> samplesheetExpected,
      final SampleSheet tested, final boolean withLane) {

    assertFalse("No sample expected loaded ", samplesheetExpected.isEmpty());
    for (SampleEntry e : tested) {
      System.out.println("compared tested sample " + e);
    }
    
    for (SampleEntry e : tested) {
      System.out.println("compared tested sample " + e);
      final String sampleId = e.getSampleId();
      final int laneNumber = e.getLane();

      if (withLane)
        assertFalse("Lane number should be define in " + e, laneNumber == 0);

      final SampleSheetTest expected =
          samplesheetExpected.get(sampleId + "_" + laneNumber);

      assertNotNull(
          "Sample id "
              + sampleId
              + "_"
              + laneNumber
              + " not found in expected dataset, it contains\n"
              + Joiner.on("\n\t").withKeyValueSeparator(",")
                  .join(samplesheetExpected), expected);

      compareSamplesheetEntryV2(expected, e, withLane);

      // Remove entry in expected map
      samplesheetExpected.remove(sampleId + "_" + laneNumber);
    }

    assertEquals(
        "expected sample(s) missing: "
            + Joiner.on(",").join(samplesheetExpected.keySet()), 0,
        samplesheetExpected.size());
  }

  private void compareSamplesheetEntryV2(final SampleSheetTest expected,
      final SampleEntry tested, final boolean withLane) {

    assertEquals("Sample ref", expected.getSampleRef(), tested.getSampleRef());
    assertEquals("Sample description", expected.getDescription(),
        tested.getDescription());
    assertEquals("Sample project", expected.getSampleProject(),
        tested.getSampleProject());
    assertEquals("Sample index", expected.getIndex(), tested.getIndex());

    assertEquals("Sample index", expected.getIndex2(), tested.getIndex2());

    if (withLane) {
      assertEquals("Sample lane", expected.getLane(), tested.getLane());
    }
  }

  private void compareSamplesheetV1(
      final Map<String, SampleSheetTest> samplesheetExpected,
      final SampleSheet tested) {

    for (SampleEntry e : tested) {
      final String sampleId = e.getSampleId();
      final int lane = e.getLane();

      final SampleSheetTest expected =
          samplesheetExpected.get(sampleId + "_" + lane);

      assertNotNull("Sample id " + sampleId + "not found in expected dataset",
          Joiner.on(",").join(samplesheetExpected.keySet()));

      assertEquals("Sample ref", expected.getSampleRef(), e.getSampleRef());
      assertEquals("Sample description", expected.getDescription(),
          e.getDescription());
      assertEquals("Sample project", expected.getSampleProject(),
          e.getSampleProject());
      assertEquals("Sample index", expected.getIndex(), e.getIndex());

      assertEquals("Sample lane", expected.getLane(), e.getLane());
      assertEquals("flowcell id", expected.getFCID(), e.getFlowCellId());
      assertEquals("Recipe", expected.getRecipe(), e.getRecipe());
      assertEquals("operator", expected.getOperator(), e.getOperator());

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

  private SampleSheet readSamplesheetCSV(final File file,
      final String bcl2fastqVersion, final int laneCount) {

    try {

      if (laneCount == -1) {
        return new CasavaDesignCSVReader(file).read(bcl2fastqVersion);
      } else {
        return new CasavaDesignCSVReader(file).readForQCReport(
            bcl2fastqVersion, laneCount);
      }

    } catch (IOException | AozanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    throw new RuntimeException();

  }

  private void convertSamplesheetToCSV(final File samplesheet,
      final String bcl2fastqVersion, final File outputFile, final int laneCount) {

    try {

      SampleSheet design;
      if (laneCount == 0) {
        design = new CasavaDesignXLSReader(samplesheet).read(bcl2fastqVersion);
      } else {
        design =
            new CasavaDesignXLSReader(samplesheet).readForQCReport(
                bcl2fastqVersion, laneCount);
      }

      // Replace index sequence shortcuts by sequences
      SampleSheetUtils.replaceIndexShortcutsBySequences(design,
          loadIndexSequences());

      // Write CSV design file
      new CasavaDesignCSVWriter(outputFile).writer(design);

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
      final String expected, final String version) {

    final Map<String, SampleSheetTest> s = new TreeMap<>();
    boolean first = true;

    for (String line : NEWLINE_SPLITTER.splitToList(expected)) {

      if (first) {
        // Skip header
        first = false;
      } else {

        final SampleSheetTest sample = new SampleSheetTest(line, version);
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
    path =
        new File(new File(".").getAbsolutePath()
            + "/src/test/java/files/samplesheets").getAbsolutePath();
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

    private String sampleID;
    private String index;
    private String index2 = "";
    private String sampleProject;
    private String description;
    private String sampleRef;
    private boolean optionalFieds;

    // Additional fields
    private int lane;
    private String FCID;
    private String control;
    private String recipe;
    private String operator;

    private void parse(final String csv) {

      Iterator<String> fields = COMMA_SPLITTER.split(csv).iterator();
      // fields.next();

      this.sampleID = fields.next();
      this.index = fields.next();
      this.sampleProject = fields.next();
      this.description = fields.next();
      this.sampleRef = fields.next();
      this.index2 = fields.next();

      if (fields.hasNext()) {
        this.lane = Integer.parseInt(fields.next());
      } else {
        this.lane = 0;
      }

      if (optionalFieds) {
        this.FCID = fields.next();
        this.control = fields.next();
        this.recipe = fields.next();
        this.operator = fields.next();
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
      System.out.println("Compare index");
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
    SampleSheetTest(final String line, final String version) {
      this.optionalFieds = version.equals(SampleSheetUtils.VERSION_1);

      parse(line);

    }

  }
}
