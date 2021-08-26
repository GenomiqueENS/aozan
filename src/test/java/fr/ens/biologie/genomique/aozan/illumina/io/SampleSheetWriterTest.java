package fr.ens.biologie.genomique.aozan.illumina.io;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVWriter;

public class SampleSheetWriterTest {

  private static final String[] filesToTest = {
      "Sample Sheet Template Covid.csv", "SampleSheet_v2_template.csv",
      "illumina-dragen-covid-pipeline-sample-sheet-template-nextseq.csv",
      "illumina-dragen-covidseq-test-v1.2-sample-sheet-template-nextseq.20201001.csv",
      "illumina-dragen-covidseq-test-v1.3-sample-sheet-template-nextseq.20201001.csv",
      "illumina-dragen-covidseq-test-v1.3-sample-sheet-template-novaseq.20201001.csv"};

  @Test
  public void testReadWriteCSV() throws IOException {

    for (String filename : filesToTest) {
      testFile(filename, true);
    }

    testFile("SampleSheet_FSE_PQ.csv", false);
  }

  private void testFile(String filename, boolean addMissingCommas)
      throws IOException {

    List<String> inputLines = readFile(loadRessource(filename));

    SampleSheetCSVReader reader =
        new SampleSheetCSVReader(loadRessource(filename));
    SampleSheet samplesheet = reader.read();
    reader.close();

    List<String> outputLines = writeFile(samplesheet, addMissingCommas);

    assertEquals(inputLines.size(), outputLines.size());

    for (int i = 0; i < inputLines.size(); i++) {

      assertEquals(inputLines.get(i), outputLines.get(i));
    }
  }

  //
  // Common methods
  //

  private InputStream loadRessource(String filename) {

    return this.getClass().getResourceAsStream("/samplesheets/" + filename);
  }

  private List<String> readFile(InputStream in) throws IOException {

    ArrayList<String> result = new ArrayList<>();

    BufferedReader br = new BufferedReader(new InputStreamReader(in));

    String line;
    while ((line = br.readLine()) != null) {
      result.add(line);
    }

    br.close();

    return result;
  }

  private List<String> writeFile(SampleSheet samplesheet,
      boolean addMissingCommas) throws IOException {

    final String s;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SampleSheetCSVWriter writer = new SampleSheetCSVWriter(out);

    writer.addMissingCommas(addMissingCommas);
    writer.writer(samplesheet);
    writer.close();
    out.close();

    s = out.toString("UTF-8");

    ArrayList<String> result = new ArrayList<>();

    for (String line : s.split("\n")) {
      result.add(line);
    }

    return result;
  }

}
