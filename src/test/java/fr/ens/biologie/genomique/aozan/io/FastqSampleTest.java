package fr.ens.biologie.genomique.aozan.io;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import fr.ens.biologie.genomique.aozan.illumina.Bcl2FastqOutput;
import fr.ens.biologie.genomique.aozan.illumina.Bcl2FastqOutput.Bcl2FastqVersion;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetCSVReader;


public class FastqSampleTest {

  private static final String[] SAMPLENAMES_FILENAMES =
      new String[] {"/Project_A2015/2015_067/sample67_S1_L001_R1_001.fastq.gz",
          "/Project_A2015/2015_067/sample67_S1_L001_R2_001.fastq.gz",
          "/Project_A2015/2015_067/sample67_S1_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_067/sample67_S1_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_067/sample67_S1_L003_R1_001.fastq.gz",
          "/Project_A2015/2015_067/sample67_S1_L003_R2_001.fastq.gz",
          "/Project_A2015/2015_067/sample67_S1_L004_R1_001.fastq.gz",
          "/Project_A2015/2015_067/sample67_S1_L004_R2_001.fastq.gz",
          "/Project_A2015/2015_068/sample68_S2_L001_R1_001.fastq.gz",
          "/Project_A2015/2015_068/sample68_S2_L001_R2_001.fastq.gz",
          "/Project_A2015/2015_068/sample68_S2_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_068/sample68_S2_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_068/sample68_S2_L003_R1_001.fastq.gz",
          "/Project_A2015/2015_068/sample68_S2_L003_R2_001.fastq.gz",
          "/Project_A2015/2015_068/sample68_S2_L004_R1_001.fastq.gz",
          "/Project_A2015/2015_068/sample68_S2_L004_R2_001.fastq.gz",
          "/Project_A2015/2015_069/sample69_S3_L001_R1_001.fastq.gz",
          "/Project_A2015/2015_069/sample69_S3_L001_R2_001.fastq.gz",
          "/Project_A2015/2015_069/sample69_S3_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_069/sample69_S3_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_069/sample69_S3_L003_R1_001.fastq.gz",
          "/Project_A2015/2015_069/sample69_S3_L003_R2_001.fastq.gz",
          "/Project_A2015/2015_069/sample69_S3_L004_R1_001.fastq.gz",
          "/Project_A2015/2015_069/sample69_S3_L004_R2_001.fastq.gz"};

  private static final String[] PROJECT3_FILENAMES =
      new String[] {"/Project_B2015/2015_067_S1_L001_R1_001.fastq.gz",
          "/Project_B2015/2015_067_S1_L001_R2_001.fastq.gz",
          "/Project_B2015/2015_068_S2_L001_R1_001.fastq.gz",
          "/Project_B2015/2015_068_S2_L001_R2_001.fastq.gz",
          "/Project_B2015/2015_069_S3_L001_R1_001.fastq.gz",
          "/Project_B2015/2015_069_S3_L001_R2_001.fastq.gz",
          "/Project_B2015/2015_067_S1_L003_R1_001.fastq.gz",
          "/Project_B2015/2015_067_S1_L003_R2_001.fastq.gz",
          "/Project_B2015/2015_068_S2_L003_R1_001.fastq.gz",
          "/Project_B2015/2015_068_S2_L003_R2_001.fastq.gz",
          "/Project_B2015/2015_069_S3_L003_R1_001.fastq.gz",
          "/Project_B2015/2015_069_S3_L003_R2_001.fastq.gz",
          "/Project_A2015/2015_167_S4_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_167_S4_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_168_S5_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_168_S5_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_169_S6_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_169_S6_L002_R2_001.fastq.gz",
          "/Project_C2015/2015_267_S7_L004_R1_001.fastq.gz",
          "/Project_C2015/2015_267_S7_L004_R2_001.fastq.gz",
          "/Project_C2015/2015_268_S8_L004_R1_001.fastq.gz",
          "/Project_C2015/2015_268_S8_L004_R2_001.fastq.gz",
          "/Project_C2015/2015_269_S9_L004_R1_001.fastq.gz",
          "/Project_C2015/2015_269_S9_L004_R2_001.fastq.gz"};

  private static final String[] COMMON_FILENAMES =
      new String[] {"/Project_A2015/2015_067_S1_L001_R1_001.fastq.gz",
          "/Project_A2015/2015_067_S1_L001_R2_001.fastq.gz",
          "/Project_A2015/2015_068_S2_L001_R1_001.fastq.gz",
          "/Project_A2015/2015_068_S2_L001_R2_001.fastq.gz",
          "/Project_A2015/2015_069_S3_L001_R1_001.fastq.gz",
          "/Project_A2015/2015_069_S3_L001_R2_001.fastq.gz",
          "/Project_A2015/2015_067_S1_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_067_S1_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_068_S2_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_068_S2_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_069_S3_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_069_S3_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_067_S1_L003_R1_001.fastq.gz",
          "/Project_A2015/2015_067_S1_L003_R2_001.fastq.gz",
          "/Project_A2015/2015_068_S2_L003_R1_001.fastq.gz",
          "/Project_A2015/2015_068_S2_L003_R2_001.fastq.gz",
          "/Project_A2015/2015_069_S3_L003_R1_001.fastq.gz",
          "/Project_A2015/2015_069_S3_L003_R2_001.fastq.gz",
          "/Project_A2015/2015_067_S1_L004_R1_001.fastq.gz",
          "/Project_A2015/2015_067_S1_L004_R2_001.fastq.gz",
          "/Project_A2015/2015_068_S2_L004_R1_001.fastq.gz",
          "/Project_A2015/2015_068_S2_L004_R2_001.fastq.gz",
          "/Project_A2015/2015_069_S3_L004_R1_001.fastq.gz",
          "/Project_A2015/2015_069_S3_L004_R2_001.fastq.gz"};

  private static final String[] PROJECT_FILENAMES =
      new String[] {"/Project_B2015/2015_167_S1_L001_R1_001.fastq.gz",
          "/Project_B2015/2015_167_S1_L001_R2_001.fastq.gz",
          "/Project_B2015/2015_168_S2_L001_R1_001.fastq.gz",
          "/Project_B2015/2015_168_S2_L001_R2_001.fastq.gz",
          "/Project_B2015/2015_169_S3_L001_R1_001.fastq.gz",
          "/Project_B2015/2015_169_S3_L001_R2_001.fastq.gz",
          "/Project_B2015/2015_167_S1_L003_R1_001.fastq.gz",
          "/Project_B2015/2015_167_S1_L003_R2_001.fastq.gz",
          "/Project_B2015/2015_168_S2_L003_R1_001.fastq.gz",
          "/Project_B2015/2015_168_S2_L003_R2_001.fastq.gz",
          "/Project_B2015/2015_169_S3_L003_R1_001.fastq.gz",
          "/Project_B2015/2015_169_S3_L003_R2_001.fastq.gz",
          "/Project_A2015/2015_067_S4_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_067_S4_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_068_S5_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_068_S5_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_069_S6_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_069_S6_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_067_S4_L004_R1_001.fastq.gz",
          "/Project_A2015/2015_067_S4_L004_R2_001.fastq.gz",
          "/Project_A2015/2015_068_S5_L004_R1_001.fastq.gz",
          "/Project_A2015/2015_068_S5_L004_R2_001.fastq.gz",
          "/Project_A2015/2015_069_S6_L004_R1_001.fastq.gz",
          "/Project_A2015/2015_069_S6_L004_R2_001.fastq.gz"};

  private static final String[] PROJECT4_FILENAMES =
      new String[] {"/Project_C2015/sample10_S10_L003_R1_001.fastq.gz",
          "/Project_C2015/sample10_S10_L003_R2_001.fastq.gz",
          "/Project_C2015/sample10_S10_L004_R1_001.fastq.gz",
          "/Project_C2015/sample10_S10_L004_R2_001.fastq.gz",
          "/Project_C2015/sample11_S11_L003_R1_001.fastq.gz",
          "/Project_C2015/sample11_S11_L003_R2_001.fastq.gz",
          "/Project_C2015/sample11_S11_L004_R1_001.fastq.gz",
          "/Project_C2015/sample11_S11_L004_R2_001.fastq.gz",
          "/Project_C2015/sample12_S12_L003_R1_001.fastq.gz",
          "/Project_C2015/sample12_S12_L003_R2_001.fastq.gz",
          "/Project_C2015/sample12_S12_L004_R1_001.fastq.gz",
          "/Project_C2015/sample12_S12_L004_R2_001.fastq.gz",
          "/Project_C2015/sample13_S13_L005_R1_001.fastq.gz",
          "/Project_C2015/sample13_S13_L005_R2_001.fastq.gz",
          "/Project_C2015/sample13_S13_L006_R1_001.fastq.gz",
          "/Project_C2015/sample13_S13_L006_R2_001.fastq.gz",
          "/Project_C2015/sample14_S14_L005_R1_001.fastq.gz",
          "/Project_C2015/sample14_S14_L005_R2_001.fastq.gz",
          "/Project_C2015/sample14_S14_L006_R1_001.fastq.gz",
          "/Project_C2015/sample14_S14_L006_R2_001.fastq.gz",
          "/Project_C2015/sample15_S19_L007_R1_001.fastq.gz",
          "/Project_C2015/sample15_S19_L007_R2_001.fastq.gz",
          "/Project_C2015/sample15_S19_L008_R1_001.fastq.gz",
          "/Project_C2015/sample15_S19_L008_R2_001.fastq.gz",
          "/Project_C2015/sample16_S15_L005_R1_001.fastq.gz",
          "/Project_C2015/sample16_S15_L005_R2_001.fastq.gz",
          "/Project_C2015/sample16_S15_L006_R1_001.fastq.gz",
          "/Project_C2015/sample16_S15_L006_R2_001.fastq.gz",
          "/Project_C2015/sample17_S16_L005_R1_001.fastq.gz",
          "/Project_C2015/sample17_S16_L005_R2_001.fastq.gz",
          "/Project_C2015/sample17_S16_L006_R1_001.fastq.gz",
          "/Project_C2015/sample17_S16_L006_R2_001.fastq.gz",
          "/Project_C2015/sample18_S17_L005_R1_001.fastq.gz",
          "/Project_C2015/sample18_S17_L005_R2_001.fastq.gz",
          "/Project_C2015/sample18_S17_L006_R1_001.fastq.gz",
          "/Project_C2015/sample18_S17_L006_R2_001.fastq.gz",
          "/Project_C2015/sample19_S18_L005_R1_001.fastq.gz",
          "/Project_C2015/sample19_S18_L005_R2_001.fastq.gz",
          "/Project_C2015/sample19_S18_L006_R1_001.fastq.gz",
          "/Project_C2015/sample19_S18_L006_R2_001.fastq.gz",
          "/Project_C2015/sample1_S1_L001_R1_001.fastq.gz",
          "/Project_C2015/sample1_S1_L001_R2_001.fastq.gz",
          "/Project_C2015/sample1_S1_L002_R1_001.fastq.gz",
          "/Project_C2015/sample1_S1_L002_R2_001.fastq.gz",
          "/Project_C2015/sample20_S20_L007_R1_001.fastq.gz",
          "/Project_C2015/sample20_S20_L007_R2_001.fastq.gz",
          "/Project_C2015/sample20_S20_L008_R1_001.fastq.gz",
          "/Project_C2015/sample20_S20_L008_R2_001.fastq.gz",
          "/Project_C2015/sample21_S21_L007_R1_001.fastq.gz",
          "/Project_C2015/sample21_S21_L007_R2_001.fastq.gz",
          "/Project_C2015/sample21_S21_L008_R1_001.fastq.gz",
          "/Project_C2015/sample21_S21_L008_R2_001.fastq.gz",
          "/Project_C2015/sample22_S22_L007_R1_001.fastq.gz",
          "/Project_C2015/sample22_S22_L007_R2_001.fastq.gz",
          "/Project_C2015/sample22_S22_L008_R1_001.fastq.gz",
          "/Project_C2015/sample22_S22_L008_R2_001.fastq.gz",
          "/Project_C2015/sample23_S23_L007_R1_001.fastq.gz",
          "/Project_C2015/sample23_S23_L007_R2_001.fastq.gz",
          "/Project_C2015/sample23_S23_L008_R1_001.fastq.gz",
          "/Project_C2015/sample23_S23_L008_R2_001.fastq.gz",
          "/Project_C2015/sample24_S24_L007_R1_001.fastq.gz",
          "/Project_C2015/sample24_S24_L007_R2_001.fastq.gz",
          "/Project_C2015/sample24_S24_L008_R1_001.fastq.gz",
          "/Project_C2015/sample24_S24_L008_R2_001.fastq.gz",
          "/Project_C2015/sample2_S2_L001_R1_001.fastq.gz",
          "/Project_C2015/sample2_S2_L001_R2_001.fastq.gz",
          "/Project_C2015/sample2_S2_L002_R1_001.fastq.gz",
          "/Project_C2015/sample2_S2_L002_R2_001.fastq.gz",
          "/Project_C2015/sample3_S3_L001_R1_001.fastq.gz",
          "/Project_C2015/sample3_S3_L001_R2_001.fastq.gz",
          "/Project_C2015/sample3_S3_L002_R1_001.fastq.gz",
          "/Project_C2015/sample3_S3_L002_R2_001.fastq.gz",
          "/Project_C2015/sample4_S4_L001_R1_001.fastq.gz",
          "/Project_C2015/sample4_S4_L001_R2_001.fastq.gz",
          "/Project_C2015/sample4_S4_L002_R1_001.fastq.gz",
          "/Project_C2015/sample4_S4_L002_R2_001.fastq.gz",
          "/Project_C2015/sample5_S5_L001_R1_001.fastq.gz",
          "/Project_C2015/sample5_S5_L001_R2_001.fastq.gz",
          "/Project_C2015/sample5_S5_L002_R1_001.fastq.gz",
          "/Project_C2015/sample5_S5_L002_R2_001.fastq.gz",
          "/Project_C2015/sample6_S6_L001_R1_001.fastq.gz",
          "/Project_C2015/sample6_S6_L001_R2_001.fastq.gz",
          "/Project_C2015/sample6_S6_L002_R1_001.fastq.gz",
          "/Project_C2015/sample6_S6_L002_R2_001.fastq.gz",
          "/Project_C2015/sample7_S7_L003_R1_001.fastq.gz",
          "/Project_C2015/sample7_S7_L003_R2_001.fastq.gz",
          "/Project_C2015/sample7_S7_L004_R1_001.fastq.gz",
          "/Project_C2015/sample7_S7_L004_R2_001.fastq.gz",
          "/Project_C2015/sample8_S8_L003_R1_001.fastq.gz",
          "/Project_C2015/sample8_S8_L003_R2_001.fastq.gz",
          "/Project_C2015/sample8_S8_L004_R1_001.fastq.gz",
          "/Project_C2015/sample8_S8_L004_R2_001.fastq.gz",
          "/Project_C2015/sample9_S9_L003_R1_001.fastq.gz",
          "/Project_C2015/sample9_S9_L003_R2_001.fastq.gz",
          "/Project_C2015/sample9_S9_L004_R1_001.fastq.gz",
          "/Project_C2015/sample9_S9_L004_R2_001.fastq.gz"};

  private static final String[] PROJECT5_FILENAMES =
      new String[] {
          "/Project_A2015/2015_267/sample267_S1_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_267/sample267_S1_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_268/sample268_S2_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_268/sample268_S2_L002_R2_001.fastq.gz",
          "/Project_A2015/2015_269/sample269_S3_L002_R1_001.fastq.gz",
          "/Project_A2015/2015_269/sample269_S3_L002_R2_001.fastq.gz",
          "/Project_B2015/2015_167/sample167_S4_L001_R1_001.fastq.gz",
          "/Project_B2015/2015_167/sample167_S4_L001_R2_001.fastq.gz",
          "/Project_B2015/2015_168/sample168_S5_L001_R1_001.fastq.gz",
          "/Project_B2015/2015_168/sample168_S5_L001_R2_001.fastq.gz",
          "/Project_B2015/2015_169/sample169_S6_L001_R1_001.fastq.gz",
          "/Project_B2015/2015_169/sample169_S6_L001_R2_001.fastq.gz"};

  @Test
  public void testGetSampleId() throws IOException {

    testSampleSheet("design_version_bcl2fastq2_shuffled_lane.csv",
        COMMON_FILENAMES, 2);
    testSampleSheet("design_version_bcl2fastq2_shuffled_sample.csv",
        COMMON_FILENAMES, 2);
    testSampleSheet("design_version_bcl2fastq2_sorted.csv", COMMON_FILENAMES,
        2);

    testSampleSheet("design_version_bcl2fastq2_shuffled_project.csv",
        PROJECT_FILENAMES, 2);
    testSampleSheet("design_version_bcl2fastq2_shuffled_3projects.csv",
        PROJECT3_FILENAMES, 2);
    testSampleSheet("design_version_bcl2fastq2_samplename.csv",
        SAMPLENAMES_FILENAMES, 2);
    testSampleSheet("samplesheet_big.csv", PROJECT4_FILENAMES, 2);
    testSampleSheet("design_lane_order.csv", PROJECT5_FILENAMES, 2);

  }

  private void testSampleSheet(String samplesheetName,
      String[] expectedOutputFilenames, int reads) throws IOException {

    // Read samplesheet
    SampleSheet samplesheet =
        new SampleSheetCSVReader(this.getClass().getClassLoader()
            .getResourceAsStream("samplesheets/" + samplesheetName)).read();

    File tmpDir = new File(System.getProperty("java.io.tmpdir"));

    // Create Bcl2FastqOutput object
    Bcl2FastqOutput bcl2FastqOutput = new Bcl2FastqOutput(samplesheet,
        new File(""), Bcl2FastqVersion.BCL2FASTQ_2, null, false);

    Set<String> expected = new HashSet<>();
    List<String> expectedList = Arrays.asList(expectedOutputFilenames);
    expected.addAll(expectedList);

    Set<String> outputFilenames = new HashSet<>();

    for (int read = 1; read <= reads; read++) {
      for (Sample sample : samplesheet) {

        FastqSample fs =
            new FastqSample(bcl2FastqOutput, tmpDir, "runid", 1, read, sample);

        String filename = fs.getFastqSampleParentDir()
            + "/" + fs.getFilenamePrefix() + ".fastq.gz";
        outputFilenames.add(filename);
      }
    }

    assertEquals(expected, outputFilenames);
  }

}
