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

import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.interopfile.AbstractBinaryIteratorReader.IlluminaMetrics;
import fr.ens.transcriptome.aozan.collectors.interopfile.ExtractionMetricsOutIterator.IlluminaIntensitiesMetrics;

/**
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ExtractionMetricsOutReader extends AbstractBinaryInterOpReader {

  private Collection<IlluminaMetrics> collection = null;
  private Map<LaneRead, ExtractionMetricsPerLane> intensity =
      new TreeMap<LaneRead, ExtractionMetricsPerLane>();

  @Override
  public void collect(final RunData data) {
    super.collect(data);
    String key = "";
    parseCollection();

    System.out.println("TILE size internal map " + intensity.size());

    for (int read = 1; read <= reads; read++) {
      // TODO to define
      data.put("read" + read + ".density.ratio", "0.3472222");
    }

    // Parse map intensity defined for each pair : lane-read for update run data
    for (Map.Entry<LaneRead, ExtractionMetricsPerLane> e : intensity.entrySet()) {
      key = "read" + e.getKey().getRead() + ".lane" + e.getKey().getLane();

      data.put(key + ".first.cycle.int.pf", e.getValue().getIntensityCycle1());
      data.put(key + ".first.cycle.int.pf.sd", e.getValue()
          .getIntensityCycle1SD());
      data.put(key + ".prc.intensity.after.20.cycles.pf", e.getValue()
          .getRatioIntensityCycle20());
      data.put(key + ".prc.intensity.after.20.cycles.pf.sd", e.getValue()
          .getRatioIntensityCycle20SD());

      // TODO to remove after test
      // System.out.println(e.getValue());
    }

  }

  /**
   * 
   */
  private void parseCollection() {

    // Set an iterator on extractionMetric binary file
    ExtractionMetricsOutIterator binIterator =
        new ExtractionMetricsOutIterator();

    // Reading the binary file and set a collection of records
    collection = makeCollection(binIterator);

    // Set the intensity value used for each lane and each read
    for (int lane = 1; lane <= lanes; lane++) {

      // For read1
      countIntensityRead(lane, 1);
      // For read2
      countIntensityRead(lane, read1CyclesCumul + 1);
      // For read3, if it exists
      countIntensityRead(lane, read2CyclesCumul + 1);
    }

  }

  /**
   * For a lane,
   * @param lane the number of lane to treat
   * @param start the number of the first cycle of a read
   */
  private void countIntensityRead(int lane, int start) {
    if (start <= 0)
      return;

    // Set the number of read
    int read =
        (start == 1) ? 1 : (start == read1CyclesCumul + 1
            ? 2 : (start == read2CyclesCumul + 1 ? 3 : -1));

    if (read == -1) {
      System.out.println("Error in compt intensity for lane " + lane);
      return;
    }

    if (read > reads)
      return;

    int sum = 0;
    int sum2 = 0;
    int c20 = 0;
    int n = 0;
    int nn = 0;
    List<Number> intensityCycle1 = Lists.newArrayList();
    List<Number> intensityCycle20 = Lists.newArrayList();

    for (int tile = IlluminaIntensitiesMetrics.minTile; tile <= IlluminaIntensitiesMetrics.maxTile; tile++) {
      for (IlluminaMetrics im : collection) {
        IlluminaIntensitiesMetrics iem = (IlluminaIntensitiesMetrics) im;

        if (iem.getLaneNumber() == lane && iem.getTileNumber() == tile) {

          // cycle 1
          if (iem.getCycleNumber() == start) {
            // intensityCycle1.add(iem.getSumIntensitiesByChannel());
            intensityCycle1.add(iem.getIntensities()[iem.BASE_A]);
            sum += iem.getSumIntensitiesByChannel();
            sum2 += iem.getIntensities()[iem.BASE_A];
            n++;
          }
          // cycle 20th of the current read
          if (iem.getCycleNumber() == start + 19) {
            // intensityCycle20.add(iem.getSumIntensitiesByChannel());
            intensityCycle20.add(iem.getIntensities()[iem.BASE_A]);
            c20 += iem.getIntensities()[iem.BASE_A];
            nn++;
          }
        }
      }
    }

    // System.out.println("start "
    // + start + " lane " + lane + " sum intensity C1 " + sum + " average "
    // + (sum / n) + "\t sum intensity C1 " + sum2 + " average2 " + (sum2 / n)
    // + (nn > 0 ? "\tc20 " + c20 + " ave " + (c20 / nn) : ""));

    intensity.put(new LaneRead(lane, read), new ExtractionMetricsPerLane(tiles,
        intensityCycle1, intensityCycle20));
  }
}
