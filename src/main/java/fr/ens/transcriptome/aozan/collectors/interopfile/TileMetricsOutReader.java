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
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.interopfile.AbstractBinaryIteratorReader.IlluminaMetrics;
import fr.ens.transcriptome.aozan.collectors.interopfile.TileMetricsOutIterator.IlluminaTileMetrics;
import fr.ens.transcriptome.aozan.collectors.interopfile.TileMetricsPerLane.ReadMetrics;

/**
 * This class collects run data by reading the TileMetricsOut.bin in InterOp
 * directory. The value are the same for all read in a lane.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class TileMetricsOutReader extends AbstractBinaryInterOpReader {

  private Collection<IlluminaMetrics> collection;

  private Map<Integer, TileMetricsPerLane> tileMetricsPerLane =
      new TreeMap<Integer, TileMetricsPerLane>();

  /**
   * Collect data.
   * @param data result data object
   */
  @Override
  public void collect(final RunData data) throws AozanException {
    super.collect(data);

    // Parse map, each entry is added in run data
    parseCollection();

    // Parse tiles metrics for each lanes
    for (Map.Entry<Integer, TileMetricsPerLane> e : tileMetricsPerLane
        .entrySet()) {

      List<ReadMetrics> readsMetrics = e.getValue().getList();

      for (ReadMetrics rm : readsMetrics) {

        String key = "read" + rm.getNumberRead() + ".lane" + e.getKey();

        // Same values for all read in a lane, values for one tile
        data.put(key + ".clusters.pf", e.getValue().getNumberClusterPF());
        data.put(key + ".clusters.pf.sd", e.getValue().getNumberClusterPFSD());

        data.put(key + ".clusters.raw", e.getValue().getNumberCluster());
        data.put(key + ".clusters.raw.sd", e.getValue().getNumberClusterSD());

        data.put(key + ".prc.pf.clusters", e.getValue().getPrcPFClusters());
        data.put(key + ".prc.pf.clusters.sd", e.getValue().getPrcPFClustersSD());

        data.put(key + ".tile.count", tiles);

        // Specific value of align on phix at each read
        data.put(key + ".prc.align", rm.getPercentAlignedPhix());
        data.put(key + ".prc.align.sd", rm.getPercentAlignedPhixSD());

        data.put(key + ".phasing", rm.getPhasing());
        data.put(key + ".prephasing", rm.getPrephasing());

        // TODO to remove after test
        if (TestCollectorReadBinary.PRINT_DETAIL)
          System.out.println(e.getKey()
              + (e.getKey() == e.getValue().getControlLane() ? "(C)" : "   ")
              + e.getValue()
              + String.format(
                  "\tpf %.1f\t pf-sd %.2f\t phasing %.7f\t pre %.7f\n", e
                      .getValue().getPrcPFClusters(), e.getValue()
                      .getPrcPFClustersSD(), rm.getPhasing(), rm
                      .getPrephasing()));
      }
    }
  }

  /**
   * Compute values necessary from collection of records provided by a binary
   * file.
   * @throws AozanException it occurs during the initialize the object iterable
   *           on binary file.
   */
  private void parseCollection() throws AozanException {

    TileMetricsOutIterator binIterator = new TileMetricsOutIterator();
    collection = makeCollection(binIterator);

    for (int lane = 1; lane <= lanes; lane++) {

      collectPerLane(lane);
    }
  }

  /**
   * Compute values per lane.
   * @param lane number lane
   */
  private void collectPerLane(final int lane) {
    ListMultimap<Integer, Number> metrics = ArrayListMultimap.create();

    for (IlluminaMetrics im : collection) {

      if (im.getLaneNumber() == lane) {

        IlluminaTileMetrics itm = (IlluminaTileMetrics) im;
        int code = itm.getMetricCode();
        double val = itm.getMetricValue();

        metrics.put(code, val);

      }
    }
    // Set a object Tile Metrics for a lane and reads
    tileMetricsPerLane.put(lane, new TileMetricsPerLane(metrics));
  }
}
