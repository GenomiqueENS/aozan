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

package fr.ens.biologie.genomique.aozan.collectors.interop;

import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.eoulsan.core.Version;

/**
 * This class collects run data by reading the QualityMetricsOut.bin in InterOp
 * directory.
 * @author Cyril Firmo
 * @since 2.0
 */
public class QualityMetricsCollector extends AbstractMetricsCollector {

  /** The sub-collector name from ReadCollector. */
  public static final String COLLECTOR_NAME = "QualityMetricsCollector";
  public static final String DATA_PREFIX = "qualitymetrics";
  public static final String FORK_VERSION = "1.18.64";

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collect data from QualityMetric interOpFile.
   * @param data result data object
   */
  @Override
  public void collect(final RunData data) throws AozanException {

    super.collect(data);
    final QualityMetricsReader reader;
    final Version rtaVersion = new Version(data.get("run.info.rta.version"));

    if (rtaVersion.greaterThanOrEqualTo(new Version(FORK_VERSION))) {
      reader = new QMetricsReader(getInterOpDir());

    } else {
      reader = new QualityMetricsReader(getInterOpDir());

    }

    final long[][][] lanes = new long[data.getLaneCount()][][];
    final long[] global = new long[50];
    final List<Integer> readNumberFromCycle = new ArrayList<Integer>();
    final int maxRead = data.getInt("run.info.read.count");
    final int[] cyclePerReads = new int[maxRead];

    for (int i = 0; i < lanes.length; i++) {
      lanes[i] = new long[maxRead][];
      for (int j = 0; j < lanes[i].length; j++) {
        lanes[i][j] = new long[50];
      }

    }

    for (int i = 0; i < cyclePerReads.length; i++) {
      cyclePerReads[i] = data.getInt("run.info.read" + (i + 1) + ".cycles");
      for (int j = 0; j < cyclePerReads[i]; j++) {
        readNumberFromCycle.add(i + 1);

      }

    }

    for (final QualityMetrics qual : reader.getSetIlluminaMetrics()) {

      int readSource = readNumberFromCycle.get(qual.getCycleNumber() - 1) - 1;
      int lane = qual.getLaneNumber() - 1;
      long[] scores = qual.getClustersScore();
      for (int i = 0; i < scores.length; i++) {
        lanes[lane][readSource][i] += scores[i];
        global[i] += scores[i];
      }

    }

    for (int i = 0; i < lanes.length; i++) {
      for (int j = 0; j < maxRead; j++) {
        data.put(DATA_PREFIX + ".lane" + (i + 1) + ".read" + (j + 1),
            lanes[i][j]);
      }
    }
    data.put(DATA_PREFIX + ".global", global);

  }

}
