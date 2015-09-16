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

public class CasavaDesignReaderTest extends TestCase {

  private static final Splitter NEWLINE_SPLITTER = Splitter.on("\n")
      .trimResults().omitEmptyStrings();

  private static final Splitter COMMA_SPLITTER = Splitter.on(",").trimResults()
      .omitEmptyStrings();

  private static final String EXPECTED_SMALLER_CSV = "[Data]\nSampleID\n"
      + "2015_067\n" + "2015_068\n" + "2015_069";

  private static final String EXPECTED_CSV =
      "SampleID,index,Sample_Project,Description,Sample_Ref\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis";

  private static final String EXPECTED_CSV_FULL =
      "SampleID,index,Sample_Project,Description,Sample_Ref,lane,FCID,Control,Recipe,Operator\n"
          + "2015_067,CGATGT,Project_A2015,Description,arabidopsis,1,H9RLKADXX,N,R1,plateform\n"
          + "2015_068,TGACCA,Project_A2015,Description,arabidopsis,1,H9RLKADXX,N,R1,plateform\n"
          + "2015_069,GCCAAT,Project_A2015,Description,arabidopsis,1,H9RLKADXX,N,R1,plateform\n"
          + "2015_167,CGATGT,Project_A2015,Description,arabidopsis,2,H9RLKADXX,N,R1,plateform\n"
          + "2015_268,TGACCA,Project_A2015,Description,arabidopsis,2,H9RLKADXX,N,R1,plateform\n"
          + "2015_369,GCCAAT,Project_A2015,Description,arabidopsis,2,H9RLKADXX,N,R1,plateform";

  private static final String SAMPLESHEET_BCL2FASTQ_V2_FILENAME =
      "samplesheet_version_bcl2fastq2.xls";
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
    convertSamplesheetToCSV(samplesheet, bcl2fastqVersion, outputFile);

    // Load samplesheet csv
    final SampleSheet samplesheetTested =
        readSamplesheetCSV(bcl2fastqVersion, -1);

    // Compare with expected content
    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(EXPECTED_CSV, SampleSheetUtils.VERSION_2);

    compareSamplesheetV2(samplesheetExpected, samplesheetTested, false);
  }

  //
  // public void testReadsXLSVersion2WithLaneColumnToCreate() {
  //
  // final File samplesheet = new File(path, SAMPLESHEET_BCL2FASTQ_V2_FILENAME);
  // final String bcl2fastqVersion = VERSION_2;
  // final int laneCount = 2;
  //
  // // Create CSV file
  // convertSamplesheetToCSV(samplesheet, bcl2fastqVersion, outputFile);
  //
  // // Load samplesheet csv
  // final SampleSheet samplesheetTested =
  // readSamplesheetCSV(bcl2fastqVersion, laneCount);
  //
  // // Compare with expected content with content column lane
  // final Map<String, SampleSheetTest> samplesheetExpected =
  // buildSamplesheetExpected(EXPECTED_CSV, VERSION_2);
  //
  // compareSamplesheetV2(samplesheetExpected, samplesheetTested, true);
  // }

  /**
   * Reads xls samplesheet file and check samplesheet instance is the same that
   * expected, with right converting index sequences.
   */
  public void testReadsXLSVersion1() {

    final File samplesheet = new File(path, SAMPLESHEET_BCL2FASTQ_V1_FILENAME);
    final String bcl2fastqVersion = SampleSheetUtils.VERSION_1;
    final int laneCount = 2;

    // Create CSV file
    convertSamplesheetToCSV(samplesheet, bcl2fastqVersion, outputFile);

    // Load samplesheet csv
    final SampleSheet samplesheetTested =
        readSamplesheetCSV(bcl2fastqVersion, laneCount);

    final Map<String, SampleSheetTest> samplesheetExpected =
        buildSamplesheetExpected(EXPECTED_CSV_FULL, SampleSheetUtils.VERSION_1);

    // Compare with expected content
    compareSamplesheetV1(samplesheetExpected, samplesheetTested);
  }

  //
  // Private methods
  //

  private void compareSamplesheetV2(
      final Map<String, SampleSheetTest> samplesheetExpected,
      final SampleSheet tested, final boolean withLane) {

    for (SampleEntry e : tested) {
      final String sampleId = e.getSampleId();

      final SampleSheetTest expected = samplesheetExpected.get(sampleId);

      assertNotNull("Sample id " + sampleId + " not found in expected dataset",
          expected);

      assertEquals("Sample ref", expected.getSampleRef(), e.getSampleRef());
      assertEquals("Sample description", expected.getDescription(),
          e.getDescription());
      assertEquals("Sample project", expected.getSampleProject(),
          e.getSampleProject());
      assertEquals("Sample index", expected.getIndex(), e.getIndex());

      if (withLane) {
        assertEquals("Sample lane", expected.getLane(), e.getLane());
      }

      // Remove entry in expected map
      samplesheetExpected.remove(sampleId);
    }

    assertEquals(
        "expected sample(s) missing: "
            + Joiner.on(",").join(samplesheetExpected.keySet()),
        samplesheetExpected.size(), 0);
  }

  private void compareSamplesheetV1(
      final Map<String, SampleSheetTest> samplesheetExpected,
      final SampleSheet tested) {

    for (SampleEntry e : tested) {
      final String sampleId = e.getSampleId();

      final SampleSheetTest expected = samplesheetExpected.get(sampleId);

      assertNotNull("Sample id " + sampleId + "not found in expected dataset",
          expected);

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
      samplesheetExpected.remove(sampleId);
    }

    assertEquals(
        "expected sample(s) missing: "
            + Joiner.on(",").join(samplesheetExpected.keySet()),
        samplesheetExpected.size(), 0);
  }

  private SampleSheet readSamplesheetCSV(final String bcl2fastqVersion,
      final int laneCount) {

    try {

      if (laneCount == -1) {
        return new CasavaDesignCSVReader(outputFile).read(bcl2fastqVersion);
      } else {
        return new CasavaDesignCSVReader(outputFile).readForQCReport(
            bcl2fastqVersion, laneCount);
      }

    } catch (IOException | AozanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return null;

  }

  private void convertSamplesheetToCSV(final File samplesheet,
      final String bcl2fastqVersion, final File outputFile) {

    try {

      SampleSheet design =
          new CasavaDesignXLSReader(samplesheet).read(bcl2fastqVersion);

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
        s.put(sample.getSampleID(), sample);
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

      if (optionalFieds) {
        this.lane = Integer.parseInt(fields.next());
        this.FCID = fields.next();
        this.control = fields.next();
        this.recipe = fields.next();
        this.operator = fields.next();
      }
    }

    //
    // Getters
    //

    public String getSampleID() {
      return sampleID;
    }

    public String getIndex() {
      return index;
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
      return "SampleSheetTest [sampleID="
          + sampleID + ", index=" + index + ", sampleProject=" + sampleProject
          + ", description=" + description + ", sampleRef=" + sampleRef + "]";
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
