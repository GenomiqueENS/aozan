package fr.ens.transcriptome.aozan.illumina.io;

import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntry;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntryVersion1;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;

class SampleSheetLineReaderV1 extends SampleSheetLineReader {

  private boolean firstLine = true;
  private int fieldsCountExpected;

  private String currentSessionName;

  private boolean firstData = true;

  private Map<String, Integer> posFields;

  // Required in this order columns header for version1
  private static final String[] FIELDNAMES_VERSION1 = new String[] {"FCID",
      "Lane", "SampleID", "SampleRef", "Index", "Description", "Control",
      "Recipe", "Operator", "SampleProject"};

  @Override
  public void parseLine(final SampleSheet design, final List<String> fields)
      throws AozanException {

    trimAndCheckFields(fields);

    if (this.firstLine) {
      this.firstLine = false;

      for (int i = 0; i < fields.size(); i++) {
        if (!FIELDNAMES_VERSION1[i].toLowerCase().equals(
            fields.get(i).toLowerCase())) {

          throw new AozanException("Invalid field name: " + fields.get(i));
        }
      }

      return;
    }

    final SampleEntry sample = new SampleEntryVersion1();

    sample.setFlowCellId(fields.get(0));
    sample.setLane(parseLane(fields.get(1)));
    sample.setSampleId(fields.get(2));
    sample.setSampleRef(fields.get(3));
    sample.setIndex(fields.get(4));
    sample.setDescription(fields.get(5));
    sample.setControl(parseControlField(fields.get(6)));
    sample.setRecipe(fields.get(7));
    sample.setOperator(fields.get(8));
    sample.setSampleProject(fields.get(9));

    design.addSample(sample);
  }

}