/*
 *                 Aozan development code
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

import java.util.HashMap;
import java.util.Map;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.collectors.CollectorConfiguration;
import fr.ens.biologie.genomique.eoulsan.core.Version;

/**
 * This class collects run data by reading the TileMetricsOut.bin in InterOp
 * directory. The value are the same for all read in a lane.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class TileMetricsCollector extends AbstractMetricsCollector {

  /** The sub-collector name from ReadCollector. */
  public static final String COLLECTOR_NAME = "TileMetricsCollector";

  private double densityRatio = 0.0;

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  @Override
  public void configure(final QC qc, final CollectorConfiguration conf) {

    super.configure(qc, conf);

    this.densityRatio =
        conf.getDouble(Settings.QC_CONF_CLUSTER_DENSITY_RATIO_KEY, 0.3472222);
  }

  /**
   * Collect data from TileMetric interOpFile.
   * @param data result data object
   */
  @Override
  public void collect(final RunData data) throws AozanException {

    super.collect(data);

    final Version rtaVersion = new Version(data.get("run.info.rta.version"));

    if (rtaVersion.greaterThanOrEqualTo(new Version("3.0.0"))) {
      collectVersion3(data);
    } else {
      collectVersion2(data);
    }
  }

  private void collectVersion2(final RunData data) throws AozanException {

    final Map<Integer, TileMetricsVersion2PerLaneStatistics> tileMetrics =
        initMetricsMapVersion2(data);

    final TileMetricsVersion2Reader reader =
        new TileMetricsVersion2Reader(getInterOpDir());

    // Distribution of metrics between lane and code
    for (final TileMetricsVersion2 itm : reader.readMetrics()) {

      tileMetrics.get(itm.getLaneNumber()).addMetric(itm);
    }

    // Build runData
    for (final TileMetricsVersion2PerLaneStatistics value : tileMetrics
        .values()) {

      value.computeData();
      data.put(value.getRunData());
    }
  }

  /**
   * Initialize TileMetrics map.
   * @param data result data object
   */
  private Map<Integer, TileMetricsVersion2PerLaneStatistics> initMetricsMapVersion2(
      final RunData data) {

    final Map<Integer, TileMetricsVersion2PerLaneStatistics> result =
        new HashMap<>();

    final int tilesCount = computeTilesCount(data);

    final int lanesCount = data.getLaneCount();
    final int readsCount = data.getReadCount();

    for (int lane = 1; lane <= lanesCount; lane++) {
      result.put(lane, new TileMetricsVersion2PerLaneStatistics(lane,
          readsCount, tilesCount, this.densityRatio));
    }

    return result;
  }

  /**
   * Initialize TileMetrics map.
   * @param data result data object
   */
  private Map<Integer, TileMetricsVersion3PerLaneStatistics> initMetricsMapVersion3(
      final RunData data) {

    final Map<Integer, TileMetricsVersion3PerLaneStatistics> result =
        new HashMap<>();

    final int tilesCount = computeTilesCount(data);

    final int lanesCount = data.getLaneCount();
    final int readsCount = data.getReadCount();

    for (int lane = 1; lane <= lanesCount; lane++) {
      result.put(lane, new TileMetricsVersion3PerLaneStatistics(lane,
          readsCount, tilesCount, this.densityRatio));
    }

    return result;
  }

  private void collectVersion3(final RunData data) throws AozanException {

    final Map<Integer, TileMetricsVersion3PerLaneStatistics> tileMetrics =
        initMetricsMapVersion3(data);

    final TileMetricsVersion3Reader reader =
        new TileMetricsVersion3Reader(getInterOpDir());

    // Distribution of metrics between lane and code
    for (final TileMetricsVersion3 itm : reader.readMetrics()) {

      tileMetrics.get(itm.getLaneNumber()).addMetric(itm);
    }

    // Build runData
    for (final TileMetricsVersion3PerLaneStatistics value : tileMetrics
        .values()) {

      value.computeData();
      data.put(value.getRunData());
    }
  }

  //
  // Getters
  //

  /**
   * Gets the tiles count.
   * @return the tiles count
   */
  public int computeTilesCount(final RunData data) {

    if (data.contains("run.info.flow.cell.section.per.lane")
        && data.getInt("run.info.flow.cell.section.per.lane") > 0) {

      // NextSeq case, compute tile count and add data from camera number
      return data.getInt("run.info.flow.cell.tile.count")
          * data.getInt("run.info.flow.cell.surface.count")
          * data.getInt("run.info.flow.cell.swath.count")
          * data.getInt("run.info.flow.cell.section.per.lane");
    }

    return data.getInt("run.info.flow.cell.tile.count")
        * data.getInt("run.info.flow.cell.surface.count")
        * data.getInt("run.info.flow.cell.swath.count");
  }

  //
  // Internal class
  //

}