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
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;

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
        new File(""), Bcl2FastqVersion.BCL2FASTQ_2, false);

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
