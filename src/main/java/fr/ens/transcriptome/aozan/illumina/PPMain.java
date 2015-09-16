package fr.ens.transcriptome.aozan.illumina;

import java.io.File;
import java.io.IOException;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils;
import fr.ens.transcriptome.aozan.io.CasavaDesignXLSReader;
import fr.ens.transcriptome.aozan.illumina.io.CasavaDesignCSVWriter;

public class PPMain {

  public static void main(String[] arg) throws IOException, AozanException {

    final String dir =
        "/home/sperrin/shares-net/sequencages/nextseq_500/samplesheets";

    final File csvV2Long = new File(dir, "samplesheet_run147.xls");
    final File csvV2Short = new File(dir, "samplesheet_TESTHISR_0151.xls");
    final File csvV1 = new File(dir, "design_SNL110_0151.xls");

    SampleSheet design =
        new CasavaDesignXLSReader(csvV1)
            .read(SampleSheetUtils.VERSION_1);

    new CasavaDesignCSVWriter(new File("/tmp", csvV1.getName().replace("xls",
        "csv"))).writer(design);

    design =
        new CasavaDesignXLSReader(csvV2Short)
            .read(SampleSheetUtils.VERSION_2);

    new CasavaDesignCSVWriter(new File("/tmp", csvV2Short.getName().replace(
        "xls", "csv"))).writer(design);

    design =
        new CasavaDesignXLSReader(csvV2Long)
            .read(SampleSheetUtils.VERSION_2);

    new CasavaDesignCSVWriter(new File("/tmp", csvV2Long.getName().replace(
        "xls", "csv"))).writer(design);

  }
}
