package fr.ens.transcriptome.aozan.illumina.samplesheet;

import java.io.File;
import java.io.IOException;

import fr.ens.transcriptome.aozan.illumina.samplesheet.io.SampleSheetCSVWriter;
import fr.ens.transcriptome.aozan.illumina.samplesheet.io.SampleSheetXLSReader;

public class Demo {

  public static final void main(String[] args) throws IOException {

    File inputDir = new File(
        "/home/jourdren/workspace/aozan/src/test/java/files/samplesheets");

    // File inputFile = new File(inputDir, "design_SNL110_0174.xls");
    // File inputFile = new File(inputDir, "design_version_bcl2fastq.xls");
    File inputFile = new File(inputDir, "samplesheet_short_bcl2fastq2.xls");

    File outputFile = new File("/home/jourdren/tmp/output.csv");

    SampleSheetXLSReader reader = new SampleSheetXLSReader(inputFile);

    final SampleSheet samplesheet = reader.read();

    SampleSheetUtils.duplicateSamplesIfLaneFieldNotSet(samplesheet, 4);
    
    SampleSheetCSVWriter writer = new SampleSheetCSVWriter(outputFile);
    writer.writer(samplesheet);

  }

}
