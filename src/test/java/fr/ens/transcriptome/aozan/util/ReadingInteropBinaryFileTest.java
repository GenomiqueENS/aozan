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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.ReadCollector;

public class ReadingInteropBinaryFileTest extends TestCase {
  static final String SR50_FILE = "./SR50.data";
  static final String PE100_FILE = "./PE100.data";

  // int read_number = 0;
  // int lane_number = 0;
  final ReadCollector readCollector = new ReadCollector();

  final List<String> KEYS = Lists.newArrayList();

  public void testSR50() {
    RunData dataTest = new RunData();
    RunData dataOriginal = new RunData();

    int readCount = 2;
    int laneCount = 2;

    // Retrieve original runData
    try {
      dataOriginal.createRunDataFile(SR50_FILE);
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    // Create runData from binary files
    try {
      readCollector.collect(dataTest);
    } catch (AozanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    String tileKey1;
    String tileKey2;
    String tileKey3;
    String tileKey4;
    String tileKey5;
    String tileKey6;
    String tileKey7;
    String tileKey8;
    String tileKey9;
    String tileKey10;
    String tileKey11;
    String tileKey12;
    String tileKey13;
    String errorKey1;
    String errorKey2;
    String errorKey3;
    String errorKey4;
    String errorKey5;
    String errorKey6;
    String errorKey7;
    String errorKey8;

    String extractionKey1;
    String extractionKey2;
    String extractionKey3;
    String extractionKey4;
    String prefix;
    for (int lane_number = 1; lane_number <= laneCount; lane_number++) {
      for (int read_number = 1; read_number <= readCount; read_number++) {

        prefix = "read" + read_number + ".lane" + lane_number;
        // Values from TileMetricsBin.out
        tileKey1 = prefix + ".called.cycles.max";
        assertTrue(tileKey1 + " idem ? ",
            dataTest.get(tileKey1).equals(dataOriginal.get(tileKey1)));
        
        tileKey2 = prefix + ".called.cycles.min";
        tileKey3 = "phasing." + prefix + ".phasing";
        tileKey4 = "phasing." + prefix + ".prephasing";
        tileKey5 = prefix + ".clusters.pf";
        tileKey6 = prefix + ".clusters.pf.sd";
        tileKey7 = prefix + ".clusters.raw";
        tileKey8 = prefix + ".clusters.raw.sd";
        tileKey9 = prefix + ".prc.pf.clusters";
        tileKey10 =
            "read"
                + read_number + ".lane" + lane_number + ".prc.pf.clusters.sd";
        tileKey11 = prefix + ".tile.count";
        tileKey12 = prefix + ".prc.align";
        tileKey13 = prefix + ".prc.align.sd";

        // Values from ErrorMetricsBin.out
        errorKey1 = prefix + ".err.rate.100";
        errorKey2 = prefix + ".err.rate.100.sd";
        errorKey3 = prefix + ".err.rate.35";
        errorKey4 = prefix + ".err.rate.35.sd";
        errorKey5 = prefix + ".err.rate.75";
        errorKey6 = prefix + ".err.rate.75.sd";
        errorKey7 = prefix + ".err.rate.phix";
        errorKey8 = prefix + ".err.rate.phix.sd";

        // Values from ExtractionMetricsBin.out
        extractionKey1 = prefix + ".first.cycle.int.pf";
        extractionKey2 = prefix + ".first.cycle.int.pf.sd";
        extractionKey3 = prefix + ".prc.intensity.after.20.cycles.pf";
        extractionKey4 = prefix + ".prc.intensity.after.20.cycles.pf.sd";

      }
    }
  }

  public void testPE100() {
  }

  public void setUp() {
    Properties props = new Properties();
    // Reading binary files
    props.put(ReadCollector.READ_XML_COLLECTOR_SPECIFIED, "false");
    // Constant
    props.put("qc.conf.cluster.density.ratio", "0.3472222");
    // Path to directory InterOP
    String path = "/home/sperrin/workspace/aozan/src/test/java/files";
    // new File(
    // fr.ens.transcriptome.aozan.util.ReadingInteropBinaryFileTest.class
    // .getCanonicalName()).getAbsolutePath();

    props.put("rta.output.dir", path);
    props.put("casava.output.dir", path);

    // Path to save rundata

    readCollector.configure(props);

    // // Values from TileMetricsBin.out
    // KEYS.add("read"
    // + read_number + ".lane" + lane_number + ".called.cycles.max");
    // KEYS.add("read"
    // + read_number + ".lane" + lane_number + ".called.cycles.min");
    // KEYS.add("phasing.read" + read_number + ".lane" + lane_number +
    // ".phasing");
    // KEYS.add("phasing.read"
    // + read_number + ".lane" + lane_number + ".prephasing");
    // KEYS.add("read" + read_number + ".lane" + lane_number + ".clusters.pf");
    // KEYS.add("read" + read_number + ".lane" + lane_number +
    // ".clusters.pf.sd");
    // KEYS.add("read" + read_number + ".lane" + lane_number + ".clusters.raw");
    // KEYS.add("read" + read_number + ".lane" + lane_number +
    // ".clusters.raw.sd");
    // KEYS.add("read" + read_number + ".lane" + lane_number +
    // ".prc.pf.clusters");
    // KEYS.add("read"
    // + read_number + ".lane" + lane_number + ".prc.pf.clusters.sd");
    // KEYS.add("read" + read_number + ".lane" + lane_number + ".tile.count");
    // KEYS.add("read" + read_number + ".lane" + lane_number + ".prc.align");
    // KEYS.add("read" + read_number + ".lane" + lane_number + ".prc.align.sd");
    //
    // // Values from ErrorMetricsBin.out
    // KEYS.add("read" + read_number + ".lane" + lane_number + ".err.rate.100");
    // KEYS.add("read" + read_number + ".lane" + lane_number +
    // ".err.rate.100.sd");
    // KEYS.add("read" + read_number + ".lane" + lane_number + ".err.rate.35");
    // KEYS.add("read" + read_number + ".lane" + lane_number +
    // ".err.rate.35.sd");
    // KEYS.add("read" + read_number + ".lane" + lane_number + ".err.rate.75");
    // KEYS.add("read" + read_number + ".lane" + lane_number +
    // ".err.rate.75.sd");
    // KEYS.add("read" + read_number + ".lane" + lane_number +
    // ".err.rate.phix");
    // KEYS.add("read" + read_number + ".lane" + lane_number +
    // ".err.rate.phix.sd");
    //
    // // Values from ExtractionMetricsBin.out
    // KEYS.add("read"
    // + read_number + ".lane" + lane_number + ".first.cycle.int.pf");
    // KEYS.add("read"
    // + read_number + ".lane" + lane_number + ".first.cycle.int.pf.sd");
    // KEYS.add("read"
    // + read_number + ".lane" + lane_number
    // + ".prc.intensity.after.20.cycles.pf");
    // KEYS.add("read"
    // + read_number + ".lane" + lane_number
    // + ".prc.intensity.after.20.cycles.pf.sd");

  }
}