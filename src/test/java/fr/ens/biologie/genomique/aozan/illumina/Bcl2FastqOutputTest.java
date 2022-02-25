package fr.ens.biologie.genomique.aozan.illumina;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import fr.ens.biologie.genomique.aozan.illumina.Bcl2FastqOutput.Bcl2FastqVersion;

import fr.ens.biologie.genomique.aozan.io.FastqSample;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetCSVReader;

public class Bcl2FastqOutputTest {

  private static final String SAMPLESHEET_CSV_BCL2FASTQ_V2_FILENAME_SORTED =
      "design_version_bcl2fastq2_sorted.csv";

  private static final String SAMPLESHEET_CSV_BCL2FASTQ_V2_FILENAME_SHUFFLED_LANE =
      "design_version_bcl2fastq2_shuffled_lane.csv";

  private static final String SAMPLESHEET_CSV_BCL2FASTQ_V2_FILENAME_SHUFFLED_NOLANE =
      "design_version_bcl2fastq2_shuffled_nolane.csv";

  private static final String SAMPLESHEET_CSV_BCL2FASTQ_V2_FILENAME_SHUFFLED_SAMPLE =
      "design_version_bcl2fastq2_shuffled_sample.csv";

  @Test
  public void testSampleNumberUnsortedLanes() throws IOException {

    List<String> files = new ArrayList<String>();

    files.add(SAMPLESHEET_CSV_BCL2FASTQ_V2_FILENAME_SORTED);
    files.add(SAMPLESHEET_CSV_BCL2FASTQ_V2_FILENAME_SHUFFLED_LANE);
    files.add(SAMPLESHEET_CSV_BCL2FASTQ_V2_FILENAME_SHUFFLED_NOLANE);
    files.add(SAMPLESHEET_CSV_BCL2FASTQ_V2_FILENAME_SHUFFLED_SAMPLE);
    int nbLanes = 4;

    Map<String, Integer> sampleNames = new HashMap<String, Integer>();
    sampleNames.put("2015_067", 1);
    sampleNames.put("2015_068", 2);
    sampleNames.put("2015_069", 3);
    for (String filename : files) {

      Bcl2FastqOutput bclfile =
          new Bcl2FastqOutput(readSamplesheetCSV(filename, nbLanes),
              new File("."), Bcl2FastqVersion.BCL2FASTQ_2, null, false);
      for (int lane = 1; lane <= nbLanes; lane++) {
        for (String sampleName : sampleNames.keySet()) {
          FastqSample fastqSample =
              new FastqSample(bclfile, new File("."), "", 1, 1, lane, null,
                  sampleName, "Project_A2015", "desc", "Bindex", false, false);

          Assert.assertEquals(sampleName
              + "_S" + sampleNames.get(sampleName) + "_L00" + lane + "_R1_001",
              fastqSample.getFilenamePrefix());
        }
      }
    }
  }

  private SampleSheet readSamplesheetCSV(final String filename,
      final int laneCount) throws FileNotFoundException {

    return readSamplesheetCSV(loadRessource(filename), laneCount);
  }

  private InputStream loadRessource(String filename) {

    return this.getClass().getResourceAsStream("/samplesheets/" + filename);
  }

  private SampleSheet readSamplesheetCSV(final InputStream in,
      final int laneCount) {

    try (SampleSheetCSVReader reader = new SampleSheetCSVReader(in)) {

      final SampleSheet result = reader.read();

      if (!result.getDemuxSection().isLaneSampleField() && laneCount > 1) {
        SampleSheetUtils.duplicateSamplesIfLaneFieldNotSet(result, laneCount);
      }

      return result;

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    throw new RuntimeException();

  }

}
