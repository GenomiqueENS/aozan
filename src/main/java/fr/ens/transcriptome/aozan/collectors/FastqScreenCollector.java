/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.collectors;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.FastqScreenDemo;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenResult;
import fr.ens.transcriptome.aozan.io.FastqStorage;
import fr.ens.transcriptome.eoulsan.Globals;

public class FastqScreenCollector implements Collector {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final String COLLECTOR_NAME = "fastqscreen";

  private static final String KEY_GENOMES = "qc.fastqscreen.genomes";
  private static final String KEY_FASTQ_DIR = "qc.fastqscreen.fastqDir";
  private static final String KEY_READ_COUNT = "run.info.read.count";
  private static final String KEY_READ_X_INDEXED = "run.info.read";
  private static final String KEY_TMP_DIR = "tmp.dir";
  private static final String COMPRESSION_EXTENSION = "fq.bz2";

  private FastqScreen fastqscreen;
  private FastqStorage fastqStorage;
  private List<String> listGenomes = new ArrayList<String>();

  private String pathDirTest;
  private boolean paired = false;

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collectors to execute before fastqscreen Collector
   */
  @Override
  public String[] getCollectorsNamesRequiered() {
    return new String[] {RunInfoCollector.COLLECTOR_NAME,
        DesignCollector.COLLECTOR_NAME};
  }

  @Override
  public void configure(final Properties properties) {

  }

  public void configure(final Map<String, String> properties) {

    this.fastqscreen = new FastqScreen(properties);

    String tmpDir = properties.get(KEY_TMP_DIR);
    this.fastqStorage = FastqStorage.getFastqStorage(tmpDir);

    System.out.println("In properties, the tmp directory is "
        + properties.get(KEY_TMP_DIR) + " space free : "
        + (new File(tmpDir).getFreeSpace() / 1048576) + "Go");

    pathDirTest = properties.get(KEY_FASTQ_DIR);
    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();

    for (String genome : s.split(properties.get(KEY_GENOMES))) {
      this.listGenomes.add(genome);
    }
  }

  // TODO temporary method, to remove after test
  public static void print(String s) {
    if (true)
      System.out.println(s);
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    FastqScreenResult resultsFastqscreen = null;

    final long startTime = System.currentTimeMillis();

    // mode paired or single-end present in Rundata
    final int readCount = data.getInt(KEY_READ_COUNT);
    final boolean lastReadIndexed =
        data.getBoolean(KEY_READ_X_INDEXED + readCount + ".indexed");
    File read1 = null;
    File read2 = null;

    final int laneCount = data.getInt("run.info.flow.cell.lane.count");

    paired = readCount > 1 && !lastReadIndexed;
    System.out.println("mode paired " + paired);

    for (int read = 1; read <= readCount - 1; read++) {

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;

      for (int lane = 1; lane <= laneCount; lane++) {

        final List<String> sampleNames =
            Lists.newArrayList(Splitter.on(',').split(
                data.get("design.lane" + lane + ".samples.names")));

        for (String sampleName : sampleNames) {

          // Get the sample index
          final String index =
              data.get("design.lane" + lane + "." + sampleName + ".index");

          // Get project name
          final String projectName =
              data.get("design.lane"
                  + lane + "." + sampleName + ".sample.project");

          print("lane current "
              + lane + "\tsample current " + sampleName + "\tproject name "
              + projectName);

          // Set the prefix of the file
          final String prefixRead1 =
              String.format("%s_%s_L%03d_R%d_", sampleName, "".equals(index)
                  ? "NoIndex" : index, lane, read);

          final String fastqDir =
              String.format("/Project_%s/Sample_%s", projectName, sampleName);

          // Set the list of the files for the FASTQ data
          final File[] fastqFiles = createListFastqFiles(fastqDir, prefixRead1);

          if (fastqFiles.length == 0)
            continue;

          // concatenate fastq files of one sample
          read1 = fastqStorage.getFastqFile(fastqFiles);
          if (read1 == null || !read1.exists())
            continue;

          if (paired) {
            // mode paired

            final String prefixRead2 =
                String.format("%s_%s_L%03d_R%d_", sampleName, "".equals(index)
                    ? "NoIndex" : index, lane, read + 1);
            final File[] fastqFilesRead2 =
                createListFastqFiles(fastqDir, prefixRead2);

            if (fastqFilesRead2.length == 0)
              continue;

            // concatenate fastq files of one sample
            read2 = fastqStorage.getFastqFile(fastqFilesRead2);
            if (read2 == null || !read2.exists())
              continue;
          }

          // add read2 in command line
          resultsFastqscreen =
              fastqscreen.execute(read1, read2, this.listGenomes);

          if (resultsFastqscreen == null)
            throw new AozanException("Fastqscreen return no result for sample "
                + fastqDir);

          processResults(data, resultsFastqscreen, lane, sampleName, read);

          fastqStorage.clear();

          final long endTime = System.currentTimeMillis();
          LOGGER.info("End test with sample "
              + sampleName + " " + toTimeHumanReadable(endTime - startTime));

          System.out.println("End test with sample "
              + sampleName + " " + toTimeHumanReadable(endTime - startTime));

        } // sample
      } // lane
    } // read

  }// end method collect

  private File[] createListFastqFiles(final String dirPath, final String prefix) {

    // test parameters
    String projetTestSingle = "Project_microbrain_A2012";
    String projetTestPaired = "Project_accepi_2012a";

    if (!(dirPath.startsWith(projetTestPaired) || dirPath
        .startsWith(projetTestSingle)))
      return null;

    return new File(pathDirTest + dirPath + "/").listFiles(new FileFilter() {

      @Override
      public boolean accept(final File pathname) {

        return pathname.getName().startsWith(prefix)
            && pathname.getName().endsWith(COMPRESSION_EXTENSION);
      }
    });
  }

  /**
   * Process results after the end of the thread.
   * @param data
   * @param result
   * @param lane
   * @param sampleName
   * @param read
   * @throws AozanException if an error occurs while generate FastqScreen
   *           results
   */
  private void processResults(final RunData data,
      final FastqScreenResult result, final int lane, final String sampleName,
      final int read) throws AozanException {

    // Set the prefix for the run data entries
    String prefix =
        "fastqscreen.lane"
            + lane + ".sample." + sampleName + ".read" + read + "."
            + sampleName;

    Map<String, double[]> resultPerGenome = result.getResultsPerGenome();
    String[] legend = result.getLegendRunData();

    // for each reference genome
    for (Map.Entry<String, double[]> e : resultPerGenome.entrySet()) {
      String genome = "." + e.getKey();

      // 5 values per genome
      for (int i = 0; i < 5; i++) {
        String key = "." + legend[i] + ".percent";
        String value = e.getValue()[i] + "";

        // add line in RunData
        data.put(prefix + genome + key, value);

        print("add in runData \t" + prefix + genome + key + "=" + value);
      }
    }
    // print last line of report FastqScreen
    data.put(prefix + "." + result.getFinalLineRunData(),
        result.getPercentHitNoLibraries());

    print("add in runData \t"
        + prefix + "." + result.getFinalLineRunData() + "="
        + result.getPercentHitNoLibraries());

    print(result.statisticalTableToString());
  }// processResults

  /**
   * Remove temporary files created in temporary directory which is defined in
   * properties of Aozan
   */
  public void clean() {

  }

  //
  // Constructor
  //

  public FastqScreenCollector() {

  }

}
