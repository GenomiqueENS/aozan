/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan.collectors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;

public class FastQScreenCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "fastqscreen";
  private static final String TEST_KEY_ENABLED_SUFFIX = ".enable";
  private static final String TEST_KEY_PREFIX = "qc.fastqscreen.";
  private static final String EXTENSION_INDEX = ".1.ebwt";

  private List<File> listGenome = new ArrayList<File>();

  private FastqScreen fastqscreen = new FastqScreen();
  private boolean success;

  private final String[] legend = {"unmapped", "one.hit.one.library",
      "multiple.hits.one.library", "one.hit.multiple.libraries",
      "multiple.hits.multiple.libraries"};

  private final String PATH_DIR_TEST =
      "/home/sperrin/Documents/FastqScreenTest/runtest58";
  private final String SEP = "_";

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

  public void configure(Map<String, String> properties, String indexDir) {

    for (Map.Entry<String, String> e : properties.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();

      if (key.startsWith(TEST_KEY_PREFIX)
          && key.endsWith(TEST_KEY_ENABLED_SUFFIX) && value != null
          && "true".equals(value.trim().toLowerCase())) {

        String nameGenome =
            key.substring(TEST_KEY_PREFIX.length(), key.length()
                - TEST_KEY_ENABLED_SUFFIX.length());

        this.listGenome.add(new File(indexDir
            + "/" + nameGenome + "/" + nameGenome + EXTENSION_INDEX));
      }
    }
  }

  // TODO temporary method, to remove after test
  public static void print(String s) {

    if (true)
      System.out.println(s);
  }

  @Override
  public void collect(RunData data) throws AozanException {

    Map<String, float[]> resultsFastqscreen;

    final int readCount = data.getInt("run.info.read.count");
    final int laneCount = data.getInt("run.info.flow.cell.lane.count");
    int readSample = 0;

    for (int read = 1; read <= readCount; read++) {

      if (data.getBoolean("run.info.read" + read + ".indexed"))
        continue;

      readSample++;

      for (int lane = 1; lane <= laneCount; lane++) {

        print("lane current " + lane);
        if (!(lane == 4 || lane == 5))
          continue;

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
          String fileNameFastq =
              "/Project_"
                  + projectName + "/Sample_" + sampleName + "/" + sampleName
                  + SEP + index + SEP + "L00" + lane + SEP + "R" + read + SEP
                  + "001.fq";

          print("name file " + PATH_DIR_TEST + fileNameFastq);

          resultsFastqscreen =
              fastqscreen.execute(PATH_DIR_TEST + fileNameFastq,
                  this.listGenome);
          processResults(data, resultsFastqscreen, lane, sampleName, read);
        }
      } // lane
    } // read

    // TODO test method
    // print completed rundata with results of fastqscreen
    try {
      FileWriter fw =
          new FileWriter(new File(PATH_DIR_TEST + "/RunDataCompleted.txt"));
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(data.toString());
      bw.close();
      fw.close();
    } catch (IOException io) {
      System.out.println(io.getMessage());
    }
  } // end collect

  /**
   * Process results after the end of the thread.
   * @throws AozanException if an error occurs while generate FastqScreen
   *           results
   */
  private void processResults(RunData data, Map<String, float[]> result,
      int lane, String sampleName, int read) throws AozanException {

    // Set the prefix for the run data entries
    String prefix =
        "fastqscreen.lane"
            + lane + ".sample." + sampleName + ".read" + read + "."
            + sampleName;

    // for each genome reference
    for (Map.Entry<String, float[]> e : result.entrySet()) {
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
    data.put(prefix + ".hit_no_libraries.percent",
        fastqscreen.getPercentHitNoLibraries());

  }// processResults

}
