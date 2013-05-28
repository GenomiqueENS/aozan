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

package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.interopfile.AbstractBinaryIteratorReader.IlluminaMetrics;
import fr.ens.transcriptome.aozan.collectors.interopfile.ExtractionMetricsOutIterator.IlluminaIntensitiesMetrics;

/**
 * This class collects run data by reading the ExtractionMetricsOut.bin in
 * InterOp directory.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ExtractionMetricsOutReader extends AbstractBinaryInterOpReader {

  private Map<LaneRead, ExtractionMetricsPerLane> intensityPerLaneRead =
      new TreeMap<LaneRead, ExtractionMetricsPerLane>();

  /**
   * Collect data.
   * @param data result data object
   */
  @Override
  public void collect(final RunData data) throws AozanException {

    super.collect(data);
    String key = "";

    parseCollection(getCyclesStartRead(data));

    // for (int read = 1; read <= reads; read++) {
    // // TODO to define
    // data.put("read" + read + ".density.ratio", "0.3472222");
    // }

    // Parse map defined for each pair : lane-read
    for (Map.Entry<LaneRead, ExtractionMetricsPerLane> e : intensityPerLaneRead
        .entrySet()) {
      key = "read" + e.getKey().getRead() + ".lane" + e.getKey().getLane();

      data.put(key + ".first.cycle.int.pf", e.getValue().getIntensityCycle1());
      data.put(key + ".first.cycle.int.pf.sd", e.getValue()
          .getIntensityCycle1SD());
      data.put(key + ".prc.intensity.after.20.cycles.pf", e.getValue()
          .getRatioIntensityCycle20());
      data.put(key + ".prc.intensity.after.20.cycles.pf.sd", e.getValue()
          .getRatioIntensityCycle20SD());

      // TODO to remove after test
      if (TestCollectorReadBinary.PRINT_DETAIL)
        System.out.println(e.getKey() + "  " + e.getValue());
    }

  }

  /**
   * Compute values needed.
   * @param cyclesStartRead list number reads with the number cycle
   *          corresponding to the first of a read
   */
  private void parseCollection(Map<Integer, List<Integer>> cyclesStartRead)
      throws AozanException {

    // Set an iterator on extractionMetric binary file
    ExtractionMetricsOutIterator binIterator =
        new ExtractionMetricsOutIterator();

    // Reading the binary file and set a collection of records
    Collection<IlluminaMetrics> collection = makeCollection(binIterator);

    // Set the intensityPerLaneRead value used for each lane and each read
    for (int lane = 1; lane <= lanes; lane++) {

      for (Map.Entry<Integer, List<Integer>> e : cyclesStartRead.entrySet()) {
        int firstCycle = e.getValue().get(0);
        int lastCycle = e.getValue().get(1);
        collectIntensityPerLaneRead(lane, firstCycle, lastCycle, collection,
            e.getKey());
      }
    }
  }

  /**
   * Compute intensity values needed for a pair lane-read
   * @param lane number lane
   * @param startCycle number cycle corresponding to start of a read
   * @param collection all record from binary file
   * @param read number read
   */
  private void collectIntensityPerLaneRead(final int lane,
      final int startCycle, final int lastCycle,
      final Collection<IlluminaMetrics> collection, final int read) {

    List<Number> intensityCycle1 = Lists.newArrayList();
    List<Number> intensityCycle20 = Lists.newArrayList();
    int numberCycleTwentieth = startCycle + 19;
    // Parse number tile
    for (Integer tile : IlluminaIntensitiesMetrics.getTilesNumberList()) {

      for (IlluminaMetrics im : collection) {
        IlluminaIntensitiesMetrics iem = (IlluminaIntensitiesMetrics) im;

        if (iem.getLaneNumber() == lane && iem.getTileNumber() == tile) {

          // cycle 1
          if (iem.getCycleNumber() == startCycle) {
            // intensityCycle1.add(iem.getSumIntensitiesByChannel());
            intensityCycle1.add(iem.getIntensities()[iem.BASE_A]);
          }
          // cycle 20th of the current read
          if (numberCycleTwentieth <= lastCycle
              && iem.getCycleNumber() == numberCycleTwentieth) {
            // intensityCycle20.add(iem.getSumIntensitiesByChannel());
            intensityCycle20.add(iem.getIntensities()[iem.BASE_A]);
          }
        }
      }
    }

    intensityPerLaneRead.put(new LaneRead(lane, read),
        new ExtractionMetricsPerLane(intensityCycle1, intensityCycle20));
  }

  /**
   * Build a map : number read with number cycle corresponding to end of read
   * @param data on run
   * @return map
   */
  private final Map<Integer, List<Integer>> getCyclesStartRead(
      final RunData data) {

    // key : number read, value(pair:first number cycle, last number cycle) :
    // number cycle corresponding to the start of a read
    final Map<Integer, List<Integer>> result = Maps.newLinkedHashMap();

    final int readCount = data.getInt("run.info.read.count");

    int firstNumberCycle = 0;
    int lastNumberCycle = 0;
    for (int read = 1; read <= readCount; read++) {
      firstNumberCycle = lastNumberCycle + 1;
      lastNumberCycle += data.getInt("run.info.read" + read + ".cycles");

      List<Integer> l = Lists.newLinkedList();
      l.add(firstNumberCycle);
      l.add(lastNumberCycle);
      result.put(read, l);
    }

    return result;
  }

}
