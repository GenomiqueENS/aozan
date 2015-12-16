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

package fr.ens.biologie.genomique.aozan.collectors.interop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.interop.ReadsData.ReadData;
import fr.ens.biologie.genomique.aozan.util.StatisticsUtils;

/**
 * This class collects run data by reading the ExtractionMetricsOut.bin in
 * InterOp directory.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ExtractionMetricsCollector extends AbstractMetricsCollector {

  /** The sub-collector name from ReadCollector. */
  public static final String NAME_COLLECTOR = "ExtractionMetricsCollector";

  /** The intensity metrics. */
  private final Map<Integer, ExtractionMetricsPerLane> intensityMetrics =
      new HashMap<>();

  @Override
  public String getName() {
    return NAME_COLLECTOR;
  }

  /**
   * Collect data from TileMetric interOpFile.
   * @param data result data object
   */
  @Override
  public void collect(final RunData data) throws AozanException {

    super.collect(data);

    int keyMap;

    final ExtractionMetricsReader reader =
        new ExtractionMetricsReader(getInterOpDir());
    initMetricsMap();

    // Distribution of metrics between lane and code
    for (final IlluminaIntensityMetrics iim : reader.getSetIlluminaMetrics()) {

      // key : number read, value(pair:first number cycle, last number cycle)
      keyMap =
          getKeyMap(iim.getLaneNumber(),
              getReadFromCycleNumber(iim.getCycleNumber()));

      this.intensityMetrics.get(keyMap).addMetric(iim);
    }

    // Build runData
    for (final Map.Entry<Integer, ExtractionMetricsPerLane> entry : this.intensityMetrics
        .entrySet()) {

      entry.getValue().computeData();
      data.put(entry.getValue().getRunData());
    }
  }

  /**
   * Initialize TileMetrics map.
   */
  private void initMetricsMap() {

    for (int lane = 1; lane <= getLanesCount(); lane++) {
      for (int read = 1; read <= getReadsCount(); read++) {

        // Extract readData from setting from run
        final ReadData readData = getReadData(read);

        this.intensityMetrics.put(getKeyMap(lane, read),
            new ExtractionMetricsPerLane(lane, read, readData));
      }
    }
  }

  /**
   * Set unique id for each pair lane-read in a run.
   * @param lane lane number
   * @param read read number
   * @return integer identifier unique
   */
  private int getKeyMap(final int lane, final int read) {
    return lane * 100 + read;
  }

  //
  // Inner Class
  //

  /**
   * This class contains all intensity values for a lane extracted from binary
   * file (ExtractionMetricsOut.bin in InterOp directory).
   * @author Sandrine Perrin
   * @since 1.1
   */
  private static final class ExtractionMetricsPerLane {

    private static final int STEP = 19;

    private final int laneNumber;
    private final int readNumber;

    private int firstCycleNumber;
    private int twentiethCycleNumber;

    private int intensityCycle1 = 0;
    private double intensityCycle1SD = 0.0;

    private double ratioIntensityCycle20 = 0.0;
    private double ratioIntensityCycle20SD = 0.0;

    private final Map<Integer, Number> intensityCycle1ValuesPerTile;
    private final Map<Integer, Number> intensityCycle20ValuesPerTile;

    private boolean dataToCompute = true;

    /**
     * Save a record from ExtractionMetricsOut.bin file, only for the first and
     * the twentieth cycle.
     * @param iim illumina tile metrics
     */
    public void addMetric(final IlluminaIntensityMetrics iim) {
      final int cycle = iim.getCycleNumber();

      // here use only the value for base A, like in the Illumina files.
      // TODO Good compute : iim.getAverageIntensities();
      if (cycle == this.firstCycleNumber) {
        this.intensityCycle1ValuesPerTile.put(iim.getTileNumber(),
            iim.getIntensities()[0]);

      } else if (cycle == this.twentiethCycleNumber) {
        this.intensityCycle20ValuesPerTile.put(iim.getTileNumber(),
            iim.getIntensities()[0]);

      }
    }

    /**
     * Compute mean and standard deviation from metrics reading in
     * TileMetricsOut.bin file.
     */
    public void computeData() {

      final StatisticsUtils statCycle1 =
          new StatisticsUtils(this.intensityCycle1ValuesPerTile.values());

      // TODO to check, used only intensity for the base A
      this.intensityCycle1 = statCycle1.getMean().intValue();

      // intensityCycle1 somme intensity / compt(tile) / 4
      this.intensityCycle1SD = statCycle1.getStandardDeviation();

      // Check if count cycle > 20
      if (this.intensityCycle20ValuesPerTile.size() > 0) {

        // Compute intensity statistic at cycle 20 as a percentage of that at
        // the first cycle.
        computeRatioIntensityCycle20();
      }

      this.dataToCompute = false;
    }

    /**
     * Save data from tile metrics for a run in a RunData.
     * @return rundata data from tile metrics for a run
     */
    public RunData getRunData() {

      if (this.dataToCompute) {
        computeData();
      }

      final RunData data = new RunData();

      final String key = "read" + this.readNumber + ".lane" + this.laneNumber;

      data.put(key + ".first.cycle.int.pf", this.intensityCycle1);
      data.put(key + ".first.cycle.int.pf.sd", this.intensityCycle1SD);
      data.put(key + ".prc.intensity.after.20.cycles.pf",
          this.ratioIntensityCycle20);
      data.put(key + ".prc.intensity.after.20.cycles.pf.sd",
          this.ratioIntensityCycle20SD);

      return data;
    }

    /**
     * Compute mean and standard deviation for intensity cycle 20th.
     */
    private void computeRatioIntensityCycle20() {

      final List<Number> intensityCycle1Values =
          Lists.newArrayList(this.intensityCycle1ValuesPerTile.values());

      final List<Number> intensityCycle20Values =
          Lists.newArrayList(this.intensityCycle20ValuesPerTile.values());

      final StatisticsUtils stat = new StatisticsUtils();

      // Compute % intensity C20 / intensity C1 for each tile
      for (int i = 0; i < intensityCycle1Values.size(); i++) {

        final Double intensityC1 = intensityCycle1Values.get(i).doubleValue();
        final Double intensityC20 = intensityCycle20Values.get(i).doubleValue();

        if (intensityC1 > 0) {
          stat.addValues(intensityC20 / intensityC1 * 100);
        }
      }

      this.ratioIntensityCycle20 = stat.getMean();
      this.ratioIntensityCycle20SD = stat.getStandardDeviation();
    }

    @Override
    public String toString() {
      return String.format("c1 %s\tsd %.4f \tratio %.4f \tratio.sd %.4f",
          this.intensityCycle1, this.intensityCycle1SD,
          this.ratioIntensityCycle20, this.ratioIntensityCycle20SD);
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param lane lane number
     * @param read read number
     */
    ExtractionMetricsPerLane(final int lane, final int read,
        final ReadData readData) {

      this.laneNumber = lane;
      this.readNumber = read;

      this.firstCycleNumber = readData.getFirstCycleNumber();

      this.twentiethCycleNumber =
          (readData.getNumberCycles() >= 20 ? this.firstCycleNumber + STEP : -1);

      this.intensityCycle1ValuesPerTile = new TreeMap<>();
      this.intensityCycle20ValuesPerTile = new TreeMap<>();
    }
  }

}
