package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet.BCLCONVERT_DEMUX_TABLE_NAME;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.aozan.aozan3.Aozan3Exception;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheetUtils;

/**
 * This class define an Illumina demultiplexing data processor that use
 * bcl-convert.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class BclConvertIlluminaDemuxDataProcessor
    extends AbstractIlluminaDemuxDataProcessor {

  public static final String PROCESSOR_NAME = "illumina_bclconvert";

  private static final String DEFAULT_CONVERT_VERSION = "4.2.7";
  private static final String DEFAULT_DOCKER_IMAGE =
      "bclconvert:" + DEFAULT_CONVERT_VERSION;

  public static final String BCL_CONVERT_FORBIDDEN_DATA_SECTION =
      "BCLConvertForbidden_Data";

  @Override
  protected String getDemuxToolName() {

    return "bcl-convert";
  }

  @Override
  protected String getConfPrefix() {

    return "bclconvert";
  }

  @Override
  public String getName() {
    return PROCESSOR_NAME;
  }

  @Override
  protected void additionalInit(RunConfiguration conf) {

    conf.setIfNotExists("bclconvert.docker.image", DEFAULT_DOCKER_IMAGE);
    conf.setIfNotExists("bclconvert.path", "/usr/bin/bcl-convert");
  }

  @Override
  protected List<String> createDemuxCommandLine(Path inputPath, Path outputPath,
      Path samplesheetPath, String version, RunConfiguration runConf)
      throws IOException {

    //  List arg
    List<String> args = new ArrayList<>();

    args.add(runConf.get(getConfPrefix() + ".path", getDemuxToolName()));
    args.add("--bcl-input-directory");
    args.add(inputPath.toAbsolutePath().toString());
    args.add("--output-directory");
    args.add(outputPath.toAbsolutePath().toString());
    addCommandLineArgument(args, runConf, "--bcl-sampleproject-subdirectories",
        "true");

    addCommandLineArgument(args, runConf, "--bcl-num-parallel-tiles");
    addCommandLineArgument(args, runConf, "--bcl-num-conversion-threads");
    addCommandLineArgument(args, runConf, "--bcl-num-compression-threads");
    addCommandLineArgument(args, runConf, "--bcl-num-decompression-threads");

    args.addAll(asList("--sample-sheet", samplesheetPath.toString()));

    // Common parameter, setting per default
    if (runConf.containsKey("bclconvert.additionnal.arguments")) {

      String additionalArguments =
          runConf.get("bclconvert.additionnal.arguments");
      args.addAll(StringUtils.splitShellCommandLine(additionalArguments));
    }

    return args;
  }

  @Override
  protected String parseDemuxToolVersion(List<String> lines) {

    for (String line : lines) {

      if (line.startsWith(getDemuxToolName())) {

        String result = line.substring("bcl-convert Version ".length());
        result = result.replace("00.000.000.", "");

        int index = result.indexOf('-');
        return index != -1 ? result.substring(0, index) : result;
      }
    }

    return null;
  }

  @Override
  protected boolean isOutputMustExists() {

    return false;
  }

  @Override
  protected void processSampleSheet(SampleSheet samplesheet)
      throws Aozan3Exception {

    try {

      if (samplesheet.containsSection(BCLCONVERT_DEMUX_TABLE_NAME)) {

        SampleSheetUtils.moveBclConvertDataForbiddenFieldsInNewSection(
            samplesheet, BCL_CONVERT_FORBIDDEN_DATA_SECTION);
      }

    } catch (KenetreException e) {
      throw new Aozan3Exception(e);
    }
  }

}
