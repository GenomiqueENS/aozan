package fr.ens.transcriptome.aozan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.illumina.io.CasavaDesignCSVReader;
import fr.ens.transcriptome.aozan.illumina.sampleentry.Sample;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils;
import fr.ens.transcriptome.aozan.util.StatisticsUtils;
import fr.ens.transcriptome.eoulsan.bio.IlluminaReadId;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class DemuxNextSeqCheckingResult {

  private final static String DIR =
      "/import/rhodos01/shares-net/sequencages/nextseq_500/fastq/checkDemuxResult";
  private final static String OUTPUT_DIR =
      "/import/rhodos01/shares-net/sequencages/nextseq_500/fastq/checkDemuxResult";
  private static final String DIR_SAMPLESHEET =
      "/import/rhodos01/shares-net/sequencages/nextseq_500/samplesheets";

  private static final String ID = "onetile-run04_";

  public static void main(String[] argv) throws Exception {

    // Init
    final String runId = "150626_NB500892_0004_AHF7KNBGXX";
    final String bcl2fastqVersion = SampleSheetUtils.VERSION_2;

    SampleSheet design = null;
    try {
      design = readDesign(runId, bcl2fastqVersion);

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    checkIndexesInUndeterminedFile(design, runId);

    // Check passing filter reads in Undetermined fastq file
    checkPFReadsUndeterminedFile(design, runId);

    // Check quality in sample fastq file
    checkQualityInSampleFastq(design, runId);

  }

  private static void checkIndexesInUndeterminedFile(SampleSheet design,
      String runId) throws Exception {

    createResultFile(design, runId, "Undetermined_", "_R1_001.fastq.gz", ID
        + "demuxWithIndex_", "forDemuxWithoutIndex");

  }

  private static void checkQualityInSampleFastq(final SampleSheet design,
      final String runId) throws Exception {

    for (Sample entry : design) {
      final String sampleName = entry.getSampleProject();

      createResultFile(design, runId + "/" + sampleName, "2015_00",
          "_R1_001.fastq.gz", ID + "sampleQuality_", "forSample");

    }
  }

  private static void checkPFReadsUndeterminedFile(final SampleSheet design,
      final String runId) throws Exception {

    createResultFile(design, runId, "Undetermined_", "_R1_001.fastq.gz", ID
        + "readNoPF_", "forUndetermined");
  }

  private static void createResultFile(final SampleSheet design,
      final String directory, final String startFilename,
      final String endFilename, final String prefixOutputFile,
      final String outputID) throws Exception {

    final List<File> fastqFiles =
        Arrays.asList(new File(DIR, directory).listFiles(new FileFilter() {

          @Override
          public boolean accept(final File pathname) {

            return pathname.length() > 0
                && pathname.getName().startsWith(startFilename)
                && pathname.getName().contains(endFilename);
          }
        }));

    System.out.println("Find sample fastq file "
        + fastqFiles.size() + "\tin dir " + directory);

    // Get compression value
    final CompressionType zType = CompressionType.GZIP;

    System.out.println("operation " + outputID);

    for (File f : fastqFiles) {
      long countPF = 0;
      long countNoPF = 0;

      File output =
          new File(OUTPUT_DIR, prefixOutputFile
              + StringUtils.filenameWithoutExtension(f.getName()) + ".tsv");

      try (BufferedWriter bw =
          Files.newWriter(output, Globals.DEFAULT_FILE_ENCODING);
          InputStream is = zType.createInputStream(new FileInputStream(f));
          FastqReader fastqReader = new FastqReader(is)) {

        IlluminaReadId ill = null;

        final List<IndexStat> indexes = new ArrayList<>();

        for (final ReadSequence seq : fastqReader) {

          if (ill == null) {
            ill = new IlluminaReadId(seq);
          } else {
            ill.parse(seq);
          }

          final int val = qualityValue(seq.qualityScores());
          final boolean asPF = val > 30;
          long i = (asPF ? countPF++ : countNoPF++);

          switch (outputID) {

          case "forSample":

            // Write in tmp fastq file
            bw.write(seq.getName()
                + "\t" + seq.getSequence() + "\t" + seq.getQuality() + "\t"
                + val + "\t" + asPF + "\t"
                + searchSample(ill.getSequenceIndex(), design));
            bw.write("\n");
            bw.flush();

            break;

          case "forUndetermined":

            if (ill.isFiltered()) {

              // Write in tmp fastq file
              bw.write(seq.getName()
                  + "\t" + seq.getSequence() + "\t" + seq.getQuality() + "\t"
                  + val + "\t" + asPF + "\t"
                  + searchSample(ill.getSequenceIndex(), design));
              bw.write("\n");
              bw.flush();

            }

            break;
          case "forDemuxWithoutIndex":

            final String seqIndex = ill.getSequenceIndex();

            boolean found = false;
            // Update stat for index
            for (IndexStat stat : indexes) {

              if (stat.getSequenceIndex().equals(seqIndex)) {
                stat.updateStat(ill);
                found = true;
              }
            }

            // Check new Index
            if (!found) {
              indexes.add(new IndexStat(seqIndex, ill));
            }

            break;
          default:
            throw new Exception("Invalid operation " + outputID);
          }

        }// End parsing file

        if (outputID.equals("forDemuxWithoutIndex")) {

          // Write output file
          for (IndexStat stat : indexes) {
            // Write in tmp fastq file
            bw.write(stat.toString());
            bw.write("\n");
            bw.flush();
          }

        }

        // Throw an exception if an error has occurred while reading data
        fastqReader.throwException();

      }

      System.out.println("------- End read fastq file "
          + f.getName() + " find PF " + countPF + " noPF " + countNoPF);
    }// end for files
  }

  private static String searchSample(final String sequenceIndex,
      final SampleSheet design) {

    for (Sample entry : design) {
      if (sequenceIndex.equals(entry.getIndex())) {
        return entry.getSampleId();
      }
    }

    return "NoFound";
  }

  private static int qualityValue(final int[] qualityScores) {

    StatisticsUtils stat = new StatisticsUtils();

    for (int i = 0; i < qualityScores.length; i++) {
      stat.addValues(new Integer(qualityScores[i]));
    }

    return stat.getMeanToInteger();

  }

  private static SampleSheet readDesign(final String runId,
      final String bcl2fastqVersion) throws Exception {

    // Design file path
    final File f = new File(DIR_SAMPLESHEET, "/samplesheet_" + runId + ".csv");

    return new CasavaDesignCSVReader(f).read(bcl2fastqVersion);
  }

  //
  //
  //

  static class IndexStat {

    private final String sequenceIndex;

    public String getSequenceIndex() {
      return sequenceIndex;
    }

    private long count = 0;
    private long countKeepedRead = 0;
    private long countIgnoredRead = 0;

    public void updateStat(final IlluminaReadId ill) {

      if (ill.getSequenceIndex().equals(sequenceIndex)) {
        count++;

        long x = (ill.isFiltered() ? countIgnoredRead++ : countKeepedRead++);
      }
    }

    @Override
    public String toString() {
      return "IndexStat [sequenceIndex="
          + sequenceIndex + "\t count=" + count + "\t countKeepedRead="
          + countKeepedRead + "\t countIgnoredRead=" + countIgnoredRead + "]";
    }

    IndexStat(final String sequence) {
      this.sequenceIndex = sequence;
    }

    public IndexStat(final String seqIndex, final IlluminaReadId ill) {
      this(seqIndex);

      updateStat(ill);
    }

  }
}
