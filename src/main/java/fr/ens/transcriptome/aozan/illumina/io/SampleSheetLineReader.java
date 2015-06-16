package fr.ens.transcriptome.aozan.illumina.io;

import java.io.IOException;
import java.util.List;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;

public abstract class SampleSheetLineReader extends
    AbstractCasavaDesignTextReader {

  @Override
  public SampleSheet read(String version) throws AozanException, IOException {
    return null;
  }

  public abstract void parseLine(final SampleSheet design,
      final List<String> fields) throws AozanException;

}
