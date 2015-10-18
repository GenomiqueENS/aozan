package fr.ens.transcriptome.aozan.illumina.samplesheet.io;

import static fr.ens.transcriptome.aozan.illumina.samplesheet.io.SampleSheetReaderUtils.trimFields;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;

public class SampleSheetDiscoverFormatParser implements SampleSheetParser {

  private static String[] SAMPLESHEET_V1_FIELDS =
      {"FCID", "Lane", "SampleID", "SampleRef", "Index", "Description",
          "Control", "Recipe", "Operator", "SampleProject"};

  private SampleSheetParser parser;
  private final List<String> sampleSheetV1HearderFields =
      Arrays.asList(SAMPLESHEET_V1_FIELDS);
  private final List<List<String>> cache = new ArrayList<>();

  @Override
  public void parseLine(final List<String> fields) throws IOException {

    trimFields(fields);

    if (fields.isEmpty()) {
      return;
    }

    if (this.parser != null) {
      parser.parseLine(fields);
      return;
    }

    if (fields.get(0).startsWith("[")) {
      this.parser = new SampleSheetV2Parser();
    } else if (this.sampleSheetV1HearderFields.equals(fields)) {
      this.parser = new SampleSheetV1Parser();
    }

    if (this.parser != null) {

      // Parse the cache
      for (List<String> fieldsCached : this.cache) {
        this.parser.parseLine(fieldsCached);
      }

      // Clear the cache
      this.cache.clear();

      // Parse current fields
      this.parser.parseLine(fields);

      return;
    }

    this.cache.add(new ArrayList<String>(fields));
  }

  @Override
  public SampleSheet getSampleSheet() {

    if (this.parser == null) {
      throw new AozanRuntimeException(
          "Unable to discovert the format of the samplesheet to read");
    }

    return this.parser.getSampleSheet();
  }

}
