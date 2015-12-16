package fr.ens.biologie.genomique.aozan.illumina.samplesheet.io;

import java.io.IOException;
import java.util.List;

import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;

public interface SampleSheetParser {

  void parseLine(final List<String> fields) throws IOException;

  SampleSheet getSampleSheet();
}
