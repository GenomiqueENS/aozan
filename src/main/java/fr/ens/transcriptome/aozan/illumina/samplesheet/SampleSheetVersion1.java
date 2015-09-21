package fr.ens.transcriptome.aozan.illumina.samplesheet;

import static fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils.SEP;
import static fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils.quote;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.aozan.illumina.sampleentry.Sample;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleV1;

public class SampleSheetVersion1 {

  private final static List<String> COLUMNS_HEADER = Arrays.asList("\"FCID\"",
      "\"Lane\"", "\"SampleID\"", "\"SampleRef\"", "\"Index\"",
      "\"Description\"", "\"Control\"", "\"Recipe\"", "\"Operator\"",
      "\"SampleProject\"");

  public static String toCSV(final SampleSheet design) {

    final StringBuilder sb = new StringBuilder();

    sb.append(Joiner.on(SEP).join(COLUMNS_HEADER) + "\n");

    if (design == null) {
      return sb.toString();
    }

    for (Sample e : design) {

      final SampleV1 s = (SampleV1) e;

      sb.append(s.getFlowCellId().trim().toUpperCase());
      sb.append(SEP);
      sb.append(s.getLane());
      sb.append(SEP);
      sb.append(quote(s.getSampleId().trim()));
      sb.append(SEP);
      sb.append(quote(s.getSampleRef().trim()));
      sb.append(SEP);
      sb.append(quote(s.getIndex().toUpperCase()));
      sb.append(SEP);
      sb.append(quote(s.getDescription().trim()));
      sb.append(SEP);
      sb.append(s.isControl() ? 'Y' : 'N');
      sb.append(SEP);
      sb.append(quote(s.getRecipe().trim()));
      sb.append(SEP);
      sb.append(quote(s.getOperator().trim()));
      sb.append(SEP);
      sb.append(quote(s.getSampleProject()));

      sb.append('\n');

    }

    return sb.toString();
  }

}
