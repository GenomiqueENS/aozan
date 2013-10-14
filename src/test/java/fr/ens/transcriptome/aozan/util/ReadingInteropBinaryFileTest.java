/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.ReadCollector;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;

public class ReadingInteropBinaryFileTest extends TestCase {
  static final String SR50_FILE = "SR50.data";
  static final String PE100_FILE = "PE100.data";

  final ReadCollector readCollector = new ReadCollector();
  final RunInfoCollector runInfoCollector = new RunInfoCollector();

  final List<String> KEYS = Lists.newArrayList();

  final Properties props = new Properties();
  String path;

  public void testPE100() throws AozanException, IOException {
    compareRunData("PE100", "InterOp_PE100", PE100_FILE);
  }

  public void testSR50() throws AozanException, IOException {
    compareRunData("SR50", "InterOp_SR50", SR50_FILE);
  }

  private void compareRunData(String runName, String dir, String runDataFile)
      throws AozanException, IOException {

    // Define path to file utils
    props.put("rta.output.dir", path + "/" + dir);
    props.put("casava.output.dir", path + "/" + dir);

    // Path to save rundata
    runInfoCollector.configure(props);
    readCollector.configure(props);

    // Create runData from binary files
    RunData dataTest = new RunData();
    runInfoCollector.collect(dataTest);
    readCollector.collect(dataTest);

    // Read source rundata
    BufferedReader br = null;
    br =
        new BufferedReader(new FileReader(new File(path + "/" + dir,
            runDataFile)));
    String line = "";

    while ((line = br.readLine()) != null) {

      final int pos = line.indexOf('=');
      if (pos == -1)
        continue;

      final String key = line.substring(0, pos);
      final String value = line.substring(pos + 1);

      // Compare runData test and original for each line
      assertTrue("For " + runName + " : " + key + " must be same ? ",
          value.equals(dataTest.get(key)));

    }
    br.close();
  }

  public void setUp() {

    // Reading binary files
    props.put(ReadCollector.READ_XML_COLLECTOR_SPECIFIED, "false");
    // Constant
    props.put("qc.conf.cluster.density.ratio", "0.3472222");
    // Path to directory InterOP
    path =
        new File(new File(".").getAbsolutePath() + "/src/test/java/files")
            .getAbsolutePath();

  }

  // private void test_to_remove() {
  //
  // RunData dataTest = new RunData();
  // RunData dataOriginal = new RunData();
  //
  // int readCount = 2;
  // int laneCount = 2;
  // String tileKey1;
  // String tileKey2;
  // String tileKey3;
  // String tileKey4;
  // String tileKey5;
  // String tileKey6;
  // String tileKey7;
  // String tileKey8;
  // String tileKey9;
  // String tileKey10;
  // String tileKey11;
  // String tileKey12;
  // String tileKey13;
  // String errorKey1;
  // String errorKey2;
  // String errorKey3;
  // String errorKey4;
  // String errorKey5;
  // String errorKey6;
  // String errorKey7;
  // String errorKey8;
  //
  // String extractionKey1;
  // String extractionKey2;
  // String extractionKey3;
  // String extractionKey4;
  // String prefix;
  //
  // for (int lane_number = 1; lane_number <= laneCount; lane_number++) {
  // for (int read_number = 1; read_number <= readCount; read_number++) {
  //
  // prefix = "read" + read_number + ".lane" + lane_number;
  // // Values from TileMetricsBin.out
  // tileKey1 = prefix + ".called.cycles.max";
  //
  // System.out.println(tileKey1
  // + " " + dataTest.get(tileKey1) + " vs "
  // + dataOriginal.get(tileKey1));
  //
  // assertTrue(tileKey1 + " idem ? ",
  // dataTest.get(tileKey1).equals(dataOriginal.get(tileKey1)));
  //
  // tileKey2 = prefix + ".called.cycles.min";
  // tileKey3 = "phasing." + prefix + ".phasing";
  // tileKey4 = "phasing." + prefix + ".prephasing";
  // tileKey5 = prefix + ".clusters.pf";
  // tileKey6 = prefix + ".clusters.pf.sd";
  // tileKey7 = prefix + ".clusters.raw";
  // tileKey8 = prefix + ".clusters.raw.sd";
  // tileKey9 = prefix + ".prc.pf.clusters";
  // tileKey10 =
  // "read"
  // + read_number + ".lane" + lane_number + ".prc.pf.clusters.sd";
  // tileKey11 = prefix + ".tile.count";
  // tileKey12 = prefix + ".prc.align";
  // tileKey13 = prefix + ".prc.align.sd";
  //
  // // Values from ErrorMetricsBin.out
  // errorKey1 = prefix + ".err.rate.100";
  // errorKey2 = prefix + ".err.rate.100.sd";
  // errorKey3 = prefix + ".err.rate.35";
  // errorKey4 = prefix + ".err.rate.35.sd";
  // errorKey5 = prefix + ".err.rate.75";
  // errorKey6 = prefix + ".err.rate.75.sd";
  // errorKey7 = prefix + ".err.rate.phix";
  // errorKey8 = prefix + ".err.rate.phix.sd";
  //
  // // Values from ExtractionMetricsBin.out
  // extractionKey1 = prefix + ".first.cycle.int.pf";
  // extractionKey2 = prefix + ".first.cycle.int.pf.sd";
  // extractionKey3 = prefix + ".prc.intensity.after.20.cycles.pf";
  // extractionKey4 = prefix + ".prc.intensity.after.20.cycles.pf.sd";
  //
  // }
  // }
  // }

}