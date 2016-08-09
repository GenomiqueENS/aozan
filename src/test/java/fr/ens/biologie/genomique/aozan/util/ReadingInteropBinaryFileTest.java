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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.collectors.CollectorConfiguration;
import fr.ens.biologie.genomique.aozan.collectors.ReadCollector;
import fr.ens.biologie.genomique.aozan.collectors.RunInfoCollector;
import org.junit.Assert;

public class ReadingInteropBinaryFileTest {
  private static final String SR50_FILE = "SR50.data";
  private static final String PE100_FILE = "PE100.data";

  private final Map<String, String> props = new HashMap<>();
  private String path;

  @Before
  public void init() {

    // Path to directory InterOP
    path = new File(new File(".").getAbsolutePath() + "/src/test/java/files")
        .getAbsolutePath();

  }

  @Test
  public void testPE100() throws AozanException, IOException {
    compareRunData("PE100", "InterOp_PE100", PE100_FILE);
  }

  @Test
  public void testSR50() throws AozanException, IOException {
    compareRunData("SR50", "InterOp_SR50", SR50_FILE);
  }

  private void compareRunData(String runName, String dir, String runDataFile)
      throws AozanException, IOException {

    // Reading binary files
    props.put(Settings.QC_CONF_READ_XML_COLLECTOR_USED_KEY, "false");
    // Constant
    props.put("qc.conf.cluster.density.ratio", "0.3472222");

    // Define path to file utils
    props.put("rta.output.dir", path + "/" + dir);
    props.put("bcl2fastq.output.dir", path + "/" + dir);

    final ReadCollector readCollector = new ReadCollector();
    final RunInfoCollector runInfoCollector = new RunInfoCollector();

    // Path to save rundata
    runInfoCollector.configure(null, new CollectorConfiguration(props));
    readCollector.configure(null, new CollectorConfiguration(props));

    // Create runData from binary files
    RunData dataTest = new RunData();
    runInfoCollector.collect(dataTest);
    readCollector.collect(dataTest);

    // Read source rundata
    BufferedReader br = new BufferedReader(
        new FileReader(new File(path + "/" + dir, runDataFile)));
    String line;

    while ((line = br.readLine()) != null) {

      final int pos = line.indexOf('=');
      if (pos == -1)
        continue;

      final String key = line.substring(0, pos);
      final String value = line.substring(pos + 1);
      // Compare runData test and original for each line
      Assert.assertEquals("For " + runName + " : " + key + " must be same ? ",
          value, dataTest.get(key));
    }
    br.close();
  }

}
