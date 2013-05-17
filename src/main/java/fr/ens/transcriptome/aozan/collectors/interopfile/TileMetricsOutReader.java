package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.interopfile.AbstractBinaryIteratorReader.IlluminaMetrics;
import fr.ens.transcriptome.aozan.collectors.interopfile.TileMetricsOutIterator.IlluminaTileMetrics;
import fr.ens.transcriptome.aozan.collectors.interopfile.TileMetricsPerLane.ReadMetrics;

public class TileMetricsOutReader extends AbstractBinaryInterOpReader {

  Collection<IlluminaMetrics> collection;
  Map<Integer, TileMetricsPerLane> internalMap =
      new TreeMap<Integer, TileMetricsPerLane>();

  public void collect(final RunData data) {
    super.collect(data);
    // parse map, each entry is added in data

    try {
      internalMap = parseCollection();
    } catch (Exception e1) {
      e1.printStackTrace();
    }

    System.out.println("TILE size internal map " + internalMap.size());

    // Parse tiles metrics for each lanes
    for (Map.Entry<Integer, TileMetricsPerLane> e : internalMap.entrySet()) {

      List<ReadMetrics> readsMetrics = e.getValue().getList();

      for (ReadMetrics rm : readsMetrics) {

        String key = "read" + rm.getNumeroRead() + ".lane" + e.getKey();
        // Same values for all read in a lane, values for one tile
        data.put(key + ".clusters.pf", e.getValue().getNumberClusterPF());
        data.put(key + ".clusters.pf.sd", e.getValue().getNumberClusterPFSD());

        data.put(key + ".clusters.raw", e.getValue().getNumberCluster());
        data.put(key + ".clusters.raw.sd", e.getValue().getNumberClusterSD());

        data.put(key + ".prc.pf.clusters", e.getValue().getPrcPFClusters());
        data.put(key + ".prc.pf.clusters.sd", e.getValue().getPrcPFClustersSD());

        data.put(key + ".tile.count", e.getValue().getTileCount());

        // Specific value of align on phix at each read
        data.put(key + ".prc.align", rm.getPercentAlignedPhix());
        data.put(key + ".prc.align.sd", rm.getPercentAlignedPhixSD());
        
        data.put(key + ".phasing", rm.getPhasing());
        data.put(key + ".prephasing", rm.getPrephasing());

        if (rm.getNumeroRead() == 1)
          System.out.printf(
              "%s pf %.1f pf sd %.2f phasing %.7f pre %.7f control %.3f\n",
              key, e.getValue().getPrcPFClusters(), e.getValue()
                  .getPrcPFClustersSD(), rm.getPhasing(), rm.getPrephasing(), e
                  .getValue().getControlLane());
      }

    }
    // System.out.println("collect tile metrics \n" + data);
  }

  private Map<Integer, TileMetricsPerLane> parseCollection() throws Exception {

    TileMetricsOutIterator binIterator = new TileMetricsOutIterator();
    collection = makeCollection(binIterator);

    for (int lane = 1; lane <= lanes; lane++) {

      metricsPerLanes(lane);
    }
    return internalMap;
  }

  private void metricsPerLanes(final int lane) {
    ListMultimap<Integer, Number> metrics = ArrayListMultimap.create();

    for (IlluminaMetrics im : collection) {

      if (im.getLaneNumber() == lane) {
        IlluminaTileMetrics itm = (IlluminaTileMetrics) im;
        int code = itm.getMetricCode();
        double val = itm.getMetricValue();

        metrics.put(code, val);

      }
    }
    // Set a object Tile Metrics for a lane and all reads
    internalMap.put(lane, new TileMetricsPerLane(tiles, metrics));
  }
}
