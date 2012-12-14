/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.FastqScreenDemo;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenResult;
import fr.ens.transcriptome.aozan.io.FastqStorage;

public class FastqScreenCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "fastqscreen";
  private static final String KEY_GENOMES = "qc.fastqscreen.genomes";
  private static final String KEY_FASTQ_DIR = "qc.fastqscreen.fastqDir";
  private static final String KEY_PAIRED = "run.info.read3.cycles";
  private static final String KEY_TMP_DIR = "tmp.dir";
  private static final String COMPRESSION_EXTENSION = ".bz2";
  
  private FastqScreen fastqscreen;
  private static FastqStorage fastqStorage;
  private List<String> listGenomes = new ArrayList<String>();

  private String pathDirTest;
  private boolean paired = false;

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  @Override
  public String[] getCollectorsNamesRequiered() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void configure(Properties properties) {

  }

  public void configure(Map<String, String> properties) {

    fastqscreen = new FastqScreen(properties);
    fastqStorage = FastqStorage.getFastqStorage(properties.get(KEY_TMP_DIR));

    pathDirTest = properties.get(KEY_FASTQ_DIR);
    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();

    paired = FastqScreenDemo.paired;

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
  public void collect(RunData data) throws AozanException {

    FastqScreenResult resultsFastqscreen = null;

    final int readCount = data.getInt("run.info.read.count");
    final int laneCount = data.getInt("run.info.flow.cell.lane.count");
    
    for (int read = 1; read <= readCount-1; read++) {

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;

      for (int lane = 1; lane <= laneCount; lane++) {

        // print("lane current " + lane);

        final List<String> sampleNames =
            Lists.newArrayList(Splitter.on(',').split(
                data.get("design.lane" + lane + ".samples.names")));

        for (String sampleName : sampleNames) {

          // print("lane current " + lane +"\tsample current " + sampleName);
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
          final String prefix = 
              String.format("%s_%s_L%03d_R%d_", sampleName, "".equals(index)
                  ? "NoIndex" : index, lane, read);

          final String fastqDir =
              String.format("/Project_%s/Sample_%s", projectName, sampleName);

          if (!paired && lane == 4 || lane == 5 ) {
            // Set the list of the files for the FASTQ data
            final File[] fastqFiles =
                new File(pathDirTest + fastqDir + "/")
                    .listFiles(new FileFilter() {

                      @Override
                      public boolean accept(final File pathname) {

                        return pathname.getName().startsWith(prefix)
                            && pathname.getName().endsWith(
                                /*".fq" +*/ COMPRESSION_EXTENSION);
                      }
                    });

            if (fastqFiles.length == 0)
              continue;
            
            // concatenate fastq files of one sample
            final File f = fastqStorage.getFastqFile(fastqFiles);

            if (f == null || !f.exists())
              continue;

            resultsFastqscreen = fastqscreen.execute(f, this.listGenomes);
            processResults(data, resultsFastqscreen, lane, sampleName, read);

            if (resultsFastqscreen == null)
              throw new AozanException(
                  "fastqscreen return no result for sample " + fastqDir);

            fastqStorage.clear();

          } else if (paired && sampleName.equals("2012-0159")) {

            final File[] fastqFilesRead1 =
                new File(pathDirTest + fastqDir + "/")
                    .listFiles(new FileFilter() {

                      @Override
                      public boolean accept(final File pathname) {

                        return pathname.getName().startsWith(prefix)
                            && pathname.getName().endsWith(
                                ".fq" + COMPRESSION_EXTENSION);
                      }
                    });
            
            if (fastqFilesRead1.length == 0)
              continue;
            
            // concatenate fastq files of one sample
            final File read1 = fastqStorage.getFastqFile(fastqFilesRead1);

            if (read1 == null || ! read1.exists())
              continue;
            
            final String prefixRead2 = 
                String.format("%s_%s_L%03d_R%d_", sampleName, "".equals(index)
                    ? "NoIndex" : index, lane, read+1);
            
            final File[] fastqFilesRead2 =
                new File(pathDirTest + fastqDir + "/")
                    .listFiles(new FileFilter() {

                      @Override
                      public boolean accept(final File pathname) {

                        return pathname.getName().startsWith(prefixRead2)
                            && pathname.getName().endsWith(
                                ".fq" + COMPRESSION_EXTENSION);
                      }
                    });
            
            if (fastqFilesRead2.length == 0)
              continue;
            
            // concatenate fastq files of one sample
            final File read2 = fastqStorage.getFastqFile(fastqFilesRead2);

            if (read2 == null || ! read2.exists())
              continue;
            
            // add read2 in command line
            resultsFastqscreen =
                fastqscreen.execute(read1, read2, this.listGenomes);
            processResults(data, resultsFastqscreen, lane, sampleName, read);
          }

        } // sample
      } // lane
    } // read

  }// end method collect

  /**
   * Process results after the end of the thread.
   * @throws AozanException if an error occurs while generate FastqScreen
   *           results
   */
  private void processResults(RunData data, FastqScreenResult result, int lane,
      String sampleName, int read) throws AozanException {

    // Set the prefix for the run data entries
    String prefix =
        "fastqscreen.lane"
            + lane + ".sample." + sampleName + ".read" + read + "."
            + sampleName;

    Map<String, float[]> resultPerGenome = result.getResultsPerGenome();
    String[] legend = result.getLegendRunData();

    // for each reference genome
    for (Map.Entry<String, float[]> e : resultPerGenome.entrySet()) {
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
    data.put(prefix +"."+ result.getFinalLineRunData(),
        result.getPercentHitNoLibraries());

    print("add in runData \t"
        + prefix + "." + result.getFinalLineRunData() + "="
        + result.getPercentHitNoLibraries());
    
    print(result.statisticalTableToString());
  }// processResults

  /**
   * remove temporary files created in temporary directory which is defined in
   * properties of Aozan
   */
  public void clean() {

  }

  //
  // COLLECTORS
  //

  public FastqScreenCollector() {

  }

}
