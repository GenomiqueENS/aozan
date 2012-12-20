/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;

public class FastqScreenDemo {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final Properties properties = new Properties();

  public static final String RESOURCE_ROOT =
      "/home/sperrin/Documents/FastqScreenTest/resources";
  public static final String SRC_RUN =
      "/home/sperrin/Documents/FastqScreenTest/runtest";
  public static final String TMP_DIR =
      "/home/sperrin/Documents/FastqScreenTest/tmp";

  public static final String GENOMES_DESC_PATH = RESOURCE_ROOT
      + "/genomes_descs";
  public static final String MAPPERS_INDEXES_PATH = RESOURCE_ROOT
      + "/mappers_indexes";
  public static final String GENOMES_PATH = RESOURCE_ROOT + "/genomes";

  private static boolean paired = false;

  public static final void main(String[] args) throws AozanException,
      IOException, EoulsanException {

    Locale.setDefault(Locale.US);
    final long startTime = System.currentTimeMillis();

    String runId;
    if (paired) {
      // run test pair-end
      runId = "120830_SNL110_0055_AD16D9ACXX";
    } else {
      // run test single-end
      runId = "121116_SNL110_0058_AC11HRACXX";
    }

    // include in RunDataGenerator
    final String fastqDir = SRC_RUN + "/qc_" + runId + "/" + runId;

    // Sample tests
    properties.put("qc.fastqscreen.genomes", "phix,lsuref_dna,adapters2");
    properties.put("qc.fastqscreen.fastqDir", fastqDir);
    properties.put("tmp.dir", TMP_DIR);

    // number threads used for fastqscreen is defined in aozan.conf
    properties.put("qc.conf.fastqscreen.threads", "4");

    // elements for configuration of eoulsanRuntime settings
    // use for create index
    properties.put("conf.settings.genomes.desc.path", GENOMES_DESC_PATH);
    properties.put("conf.settings.genomes", GENOMES_PATH);
    properties.put("conf.settings.mappers.indexes.path", MAPPERS_INDEXES_PATH);

    // process for one run
    FastqScreenCollector fsqCollector = new FastqScreenCollector();

    File f = new File(fastqDir + "/data-" + runId + ".txt");

    try {
      final RunData data = new RunData(f);

      // Configure : create list of reference genome
      fsqCollector.configure(properties);

      // And collect data
      fsqCollector.collect(data);

      // TODO test method
      // print completed rundata with results of fastqscreen
      FileWriter fw =
          new FileWriter(new File(fastqDir
              + "/RunDataCompleted_" + runId + ".txt"));
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(data.toString());
      bw.close();
    } catch (IOException io) {
      System.out.println(io.getMessage());
    }

    final long endTime = System.currentTimeMillis();
    LOGGER.info("Runtime for demo with a run "
        + runId + " " + toTimeHumanReadable(endTime - startTime));

    System.out.println("Runtime for demo with a run "
        + runId + " " + toTimeHumanReadable(endTime - startTime));
  }

}
