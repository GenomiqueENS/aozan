package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;

/**
 * This class define an Illumina demultiplexing data processor that use
 * bcl-convert.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class Bcl2FastqIlluminaDemuxDataProcessor
    extends AbstractIlluminaDemuxDataProcessor {

  public static final String PROCESSOR_NAME = "illumina_demux";

  private static final String DEFAULT_BCL2FASTQ_VERSION = "2.18.0.12";
  private static final String DEFAULT_DOCKER_IMAGE =
      "genomicpariscentre/bcl2fastq2:" + DEFAULT_BCL2FASTQ_VERSION;

  private static final int DEFAULT_MISMATCHES = 0;

  @Override
  protected String getDemuxToolName() {

    return "bcl2fastq";
  }

  @Override
  protected String getConfPrefix() {

    return "bcl2fastq";
  }

  @Override
  public String getName() {
    return PROCESSOR_NAME;
  }

  @Override
  protected void additionalInit(RunConfiguration conf) {

    conf.setIfNotExists("bcl2fastq.docker.image", DEFAULT_DOCKER_IMAGE);
    conf.setIfNotExists("bcl2fastq.path", "/usr/local/bin/bcl2fastq");

  }

  @Override
  protected List<String> createDemuxCommandLine(Path inputPath, Path outputPath,
      Path samplesheetPath, String bcl2fastqVersion, RunConfiguration runConf)
      throws IOException {

    // Get parameter values
    String finalCommandPath = runConf.get("bcl2fastq.path", "bcl2fastq");

    int threadCount = runConf.getInt("bcl2fastq.threads",
        Runtime.getRuntime().availableProcessors());

    if (!runConf.containsKey("bcl2fastq.processing.threads")) {
      runConf.set("bcl2fastq.processing.threads", threadCount);
    }

    int mismatchCount =
        runConf.getInt("illumina.demux.allowed.mismatches", DEFAULT_MISMATCHES);
    int bcl2fastqMinorVersion =
        Integer.parseInt(bcl2fastqVersion.split("\\.")[1]);

    //  List arg
    List<String> args = new ArrayList<>();

    args.add(finalCommandPath);
    addCommandLineArgument(args, runConf, "--loading-threads");
    addCommandLineArgument(args, runConf, "--processing-threads");
    addCommandLineArgument(args, runConf, "--writing-threads");

    if (bcl2fastqMinorVersion < 19) {
      args.addAll(asList("--demultiplexing-threads", "" + threadCount));
    }

    args.addAll(asList("--sample-sheet", samplesheetPath.toString()));
    args.addAll(asList("--barcode-mismatches", "" + mismatchCount));

    // Common parameter, setting per default
    args.addAll(
        asList("--input-dir", inputPath + "/Data/Intensities/BaseCalls"));
    args.addAll(asList("--output-dir", outputPath.toString()));

    if (runConf.getBoolean("bcl2fastq.with.failed.reads", true)) {
      args.add("--with-failed-reads");
    }

    // Specific parameter
    args.addAll(asList("--runfolder-dir", inputPath.toString()));
    args.addAll(asList("--interop-dir", outputPath + "/InterOp"));
    args.addAll(asList("--min-log-level", "TRACE"));
    args.addAll(asList("--stats-dir", outputPath + "/Stats"));
    args.addAll(asList("--reports-dir", outputPath + "/Reports"));

    // Set the compression level

    if (runConf.containsKey("bcl2fastq.compression.level")) {

      int level = runConf.getInt("bcl2fastq.compression.level");
      if (level < 0 || level > 10) {
        throw new IOException("Invalid Bcl2fastq compression level: " + level);
      }

      args.addAll(asList("--fastq-compression-level", "" + level));
    }

    if (runConf.containsKey("bcl2fastq.additionnal.arguments")) {

      String additionalArguments =
          runConf.get("bcl2fastq.additionnal.arguments");
      args.addAll(StringUtils.splitShellCommandLine(additionalArguments));
    }

    return args;
  }

  @Override
  protected String parseDemuxToolVersion(List<String> lines) {

    for (String line : lines) {

      if (line.startsWith(getDemuxToolName())) {
        return line.substring("bcl2fastq v".length());
      }
    }

    return null;
  }

  @Override
  protected boolean isOutputMustExists() {

    return true;
  }

}
