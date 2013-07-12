package fr.ens.transcriptome.aozan.collectors.interop;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.FlowcellDemuxSummaryCollector;
import fr.ens.transcriptome.aozan.collectors.PhasingCollector;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;

public class PPCollectorReadBinary {

  // static final String runInfoPath =
  // "/home/sperrin/Documents/FastqScreenTest/reading_interop/InterOp"
  // + "_67" + "/"; // RunInfo.xml";

  // hiseq_log_130326_SNL110_0066_AD1GG4ACXX/
  static String runId = "hiseq_log_" + "120830_SNL110_0055_AD16D9ACXX/";

  static final String runInfoPath =
      "/home/sperrin/Documents/FastqScreenTest/tmp/interOp/" + runId;

  static final String dir =
      "/home/sperrin/Documents/FastqScreenTest/tmp/interOp/";

  static int LANES;
  static int TILES;
  static int READS;

  static int[] nbCyclesPerRead;
  static int sumCycles = 0;

  static List<Integer> list = new LinkedList<Integer>();

  public static void main(String[] argv) throws Exception {

    new PPCollectorReadBinary().collectRun(dir
        + "hiseq_log_121116_SNL110_0058_AC11HRACXX/");

    // new PPCollectorReadBinary().collectRun(dir
    // + "hiseq_log_120830_SNL110_0055_AD16D9ACXX/");

    // new PPCollectorReadBinary().collectRun(dir
    // + "hiseq_log_130326_SNL110_0066_AD1GG4ACXX/");
    //
    // new PPCollectorReadBinary().collectRun(dir
    // + "hiseq_log_120830_SNL110_0055_AD16D9ACXX/");
  }

  void collectRun(String runInfoPath) throws Exception {
    // Collect
    RunData data = new RunData();
    RunInfoCollector runInfoColl = new RunInfoCollector();
    Properties props = new Properties();
    props.put(QC.RTA_OUTPUT_DIR, runInfoPath);
    props
        .put(
            QC.CASAVA_OUTPUT_DIR,
            "/home/sperrin/Documents/FastqScreenTest/runtest/qc_121116_SNL110_0058_AC11HRACXX/121116_SNL110_0058_AC11HRACXX");
    runInfoColl.configure(props);

    runInfoColl.collect(data);
    System.out.println("run " + runInfoPath);

    // AbstractBinaryFileReader.setDirectory(runInfoPath + "InterOp/");
    // new TileMetricsCollector().collect(data);
    // new ErrorMetricsCollector().collect(data);
    // new ExtractionMetricsCollector().collect(data);

    Collector c = new FlowcellDemuxSummaryCollector();
    c.configure(props);
    c.collect(data);
    // data.createRunDataFile(runInfoPath + "data_" + (new Date()) + ".data");
    System.out.println("nb countLanes " + LANES + "  data \n" + data);
  }

  // static int numeroRead(int nbCycles) throws Exception {
  // int n = 1;
  // if (nbCycles <= 0)
  // throw new Exception(
  // "Error number Cycles for define numero read is incorrect");
  //
  // for (int i = 0; i < READS; i++)
  // n = (nbCycles > nbCyclesPerRead[i] ? n + 1 : n);
  //
  // return n;
  // }
}
