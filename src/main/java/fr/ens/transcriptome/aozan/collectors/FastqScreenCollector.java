/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenResult;

public class FastqScreenCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "fastqscreen";
  private static final String KEY_GENOMES = "qc.fastqscreen.genomes";
  private static final String KEY_FASTQ_DIR = "qc.fastqscreen.fastqDir";

  private List<String> listGenomes = new ArrayList<String>();

  private FastqScreen fastqscreen;
  private String pathDirTest;

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
  public void collect(RunData data) throws AozanException {

    FastqScreenResult resultsFastqscreen;

    final int readCount = data.getInt("run.info.read.count");
    final int laneCount = data.getInt("run.info.flow.cell.lane.count");

    for (int read = 1; read <= readCount; read++) {

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;

      for (int lane = 1; lane <= laneCount; lane++) {

        print("lane current " + lane);

        final List<String> sampleNames =
            Lists.newArrayList(Splitter.on(',').split(
                data.get("design.lane" + lane + ".samples.names")));

        for (String sampleName : sampleNames) {
          print("sample current " + sampleName);
          // Get the sample index
          final String index =
              data.get("design.lane" + lane + "." + sampleName + ".index");

          // Get project name
          final String projectName =
              data.get("design.lane"
                  + lane + "." + sampleName + ".sample.project");

          print("project name " + projectName);

          // execute fastqScreen
          final String fastqFileName =
              String.format("/Project_%s/Sample_%s/%s_%s_L%03d_R%d_001.fq",
                  projectName, sampleName, sampleName, "".equals(index)
                      ? "NoIndex" : index, lane, read);

          print("name file "
              + pathDirTest + fastqFileName);

          if (lane == 4 || lane == 5) {
            resultsFastqscreen =
                fastqscreen.execute(pathDirTest + fastqFileName,
                    this.listGenomes);
            processResults(data, resultsFastqscreen, lane, sampleName, read);
          }
        }
      } // lane
    } // read

  } // end collect

  /**
   * Process results after the end of the thread.
   * @throws AozanException if an error occurs while generate FastqScreen
   *           results
   */
  private void processResults(RunData data, FastqScreenResult result,
      int lane, String sampleName, int read) throws AozanException {

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
    data.put(prefix + result.getFinalLineRunData(),
        result.getPercentHitNoLibraries());

    print("add in runData \t"
        + prefix + "."+ result.getFinalLineRunData()+"="
        + result.getPercentHitNoLibraries());
  }// processResults

  
  /**
   * remove temporary files created in temporary directory which is defined in properties of Aozan
   */
  public void clean(){
    
  }
  
  
  //
  // COLLECTORS
  //

  public FastqScreenCollector() {

  }

}
