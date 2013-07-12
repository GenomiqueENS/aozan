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

package fr.ens.transcriptome.aozan.collectors.interop;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;

/**
 * This class define the method necessary for all reader of binary file in
 * InterOp directory.
 * @author Sandrine Perrin
 * @since 1.1
 */
abstract class AbstractBinaryFileCollector implements Collector {

  private static final double DENSITY_RATIO = 0.3472222;

  private static boolean firstCollector = true;
  protected static int lanesCount;
  protected static int readsCount;
  protected static int tilesCount;

  public String dirInterOpPath;

  /**
   * Get the name of the collectors required to run this collector.
   * @return a list of String with the name of the required collectors
   */
  public List<String> getCollectorsNamesRequiered() {
    return Collections.unmodifiableList(Lists
        .newArrayList(RunInfoCollector.COLLECTOR_NAME));
  }

  /**
   * Configure the collector with the path of the run data
   * @param properties object with the collector configuration
   */
  public void configure(Properties properties) {
    String RTAOutputDirPath = properties.getProperty(QC.RTA_OUTPUT_DIR);
    this.dirInterOpPath = RTAOutputDirPath + "/InterOp/";
  }

  /**
   * Define data necessary for all concrete InterOpReader
   * @param data result data object
   */
  public void collect(final RunData data) throws AozanException {

    if (!firstCollector)
      return;

    lanesCount = data.getInt("run.info.flow.cell.lane.count");
    readsCount = data.getInt("run.info.read.count");

    if (readsCount > 3)
      throw new AozanException(
          "Numbers of countReads > 3 not accepted for reading binary file in InterOp Directory.");

    tilesCount =
        data.getInt("run.info.flow.cell.tile.count")
            * data.getInt("run.info.flow.cell.surface.count")
            * data.getInt("run.info.flow.cell.swath.count");

    // Set global data not specific for one lane
    for (int read = 1; read <= readsCount; read++) {

      data.put("read" + read + ".density.ratio", DENSITY_RATIO);
      String s =
          data.getBoolean("run.info.read" + read + ".indexed") ? "(Index)" : "";
      data.put("read" + read + ".type", s);

    }

    firstCollector = false;
  }

  /**
   * Set unique id for each pair lane-read in a run
   * @param lane lane number
   * @param read read number
   * @return integer identifier unique
   */
  protected int getKeyMap(final int lane, final int read) {
    return lane * 100 + read;
  }

  /**
   * Remove temporary files
   */
  public void clear() {
  }
}
