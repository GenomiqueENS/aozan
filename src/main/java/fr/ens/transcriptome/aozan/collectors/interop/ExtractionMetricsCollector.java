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
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;
import fr.ens.transcriptome.aozan.util.StatisticsUtils;

/**
 * This class collects run data by reading the ExtractionMetricsOut.bin in
 * InterOp directory.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ExtractionMetricsCollector implements Collector {

  /** The sub-collector name from ReadCollector. */
  public static final String NAME_COLLECTOR = "ExtractionMetricsCollector";

  private String dirInterOpPath;
  private final Map<Integer, ExtractionMetricsPerLane> intensityMetrics = Maps
      .newHashMap();

  // number cycle corresponding to the start of a read
  private int read1FirstCycleNumber;
  private int read1LastCycleNumber;

  private int read2FirstCycleNumber;
  private int read2LastCycleNumber;

  private int read3FirstCycleNumber;

  public String getName() {
    return NAME_COLLECTOR;
  }

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
   * Collect data from TileMetric interOpFile.
   * @param data result data object
   */
  public void collect(final RunData data) throws AozanException {

    int keyMap;

    ExtractionMetricsReader reader =
        new ExtractionMetricsReader(dirInterOpPath);
    initMetricsMap(data);

    // Distribution of metrics between lane and code
    for (IlluminaIntensityMetrics iim : reader.getSetIlluminaMetrics()) {

      // key : number read, value(pair:first number cycle, last number cycle)
      keyMap =
          getKeyMap(iim.getLaneNumber(), getReadNumber(iim.getCycleNumber()));

      intensityMetrics.get(keyMap).addMetric(iim);
    }

    // Build runData
    for (Map.Entry<Integer, ExtractionMetricsPerLane> entry : intensityMetrics
        .entrySet()) {

      entry.getValue().computeData();
      data.put(entry.getValue().getRunData());
    }
  }

  /**
   * Initialize TileMetrics map.
   */
  private void initMetricsMap(final RunData data) throws AozanException {

    final int lanesCount = data.getInt("run.info.flow.cell.lane.count");
    final int readsCount = data.getInt("run.info.read.count");

    defineReadsStartEndCycleNumber(data);

    for (int lane = 1; lane <= lanesCount; lane++)
      for (int read = 1; read <= readsCount; read++) {

        intensityMetrics.put(getKeyMap(lane, read),
            new ExtractionMetricsPerLane(lane, read, read1FirstCycleNumber,
                read2FirstCycleNumber, read3FirstCycleNumber));
      }

  }

  /**
   * Define the readNumber corresponding to the cycle
   * @param cycle number cycle in the run
   * @return readNumber
   */
  private int getReadNumber(final int cycle) {

    if (cycle <= this.read1LastCycleNumber)
      return 1;

    if (cycle <= this.read2LastCycleNumber)
      return 2;

    return 3;

  }

  /**
   * Set unique id for each pair lane-read in a run
   * @param lane lane number
   * @param read read number
   * @return integer identifier unique
   */
  private int getKeyMap(final int lane, final int read) {
    return lane * 100 + read;
  }

  /**
   * Build a map : number read with number cycle corresponding to end of read
   * @param data on run
   */
  private void defineReadsStartEndCycleNumber(final RunData data) {

    final int readCount = data.getInt("run.info.read.count");

    this.read1FirstCycleNumber = 1;
    this.read1LastCycleNumber = data.getInt("run.info.read" + 1 + ".cycles");

    this.read2FirstCycleNumber = this.read1LastCycleNumber + 1;
    this.read2LastCycleNumber =
        this.read1LastCycleNumber
            + data.getInt("run.info.read" + 2 + ".cycles");

    if (readCount > 2) {
      this.read3FirstCycleNumber = this.read2LastCycleNumber + 1;
    }

  }

  /**
   * Remove temporary files
   */
  public void clear() {
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
     * the twentieth cycle
     * @param iim illumina tile metrics
     */
    public void addMetric(final IlluminaIntensityMetrics iim) {
      int cycle = iim.getCycleNumber();

      // here use only the value for base A, like in the Illumina files.
      // TODO Good compute : iim.getAverageIntensities();
      if (cycle == firstCycleNumber) {
        intensityCycle1ValuesPerTile.put(iim.getTileNumber(),
            iim.getIntensities()[0]);

      } else if (cycle == twentiethCycleNumber) {
        intensityCycle20ValuesPerTile.put(iim.getTileNumber(),
            iim.getIntensities()[0]);

      }
    }

    /**
     * Compute mean and standard deviation from metrics reading in
     * TileMetricsOut.bin file.
     */
    public void computeData() {

      StatisticsUtils statCycle1 =
          new StatisticsUtils(intensityCycle1ValuesPerTile.values());

      // TODO to check, used only intensity for the base A
      this.intensityCycle1 = statCycle1.getMean().intValue();

      // intensityCycle1 somme intensity / compt(tile) / 4
      this.intensityCycle1SD = statCycle1.getStandardDeviation();

      // Check if count cycle > 20
      if (intensityCycle20ValuesPerTile.size() > 0) {

        // Compute intensity statistic at cycle 20 as a percentage of that at
        // the first cycle.
        computeRatioIntensityCycle20();
      }

      dataToCompute = false;
    }

    /**
     * Save data from tile metrics for a run in a RunData
     * @return rundata data from tile metrics for a run
     */
    public RunData getRunData() {

      if (dataToCompute)
        computeData();

      RunData data = new RunData();

      String key = "read" + readNumber + ".lane" + laneNumber;

      data.put(key + ".first.cycle.int.pf", intensityCycle1);
      data.put(key + ".first.cycle.int.pf.sd", intensityCycle1SD);
      data.put(key + ".prc.intensity.after.20.cycles.pf", ratioIntensityCycle20);
      data.put(key + ".prc.intensity.after.20.cycles.pf.sd",
          ratioIntensityCycle20SD);

      return data;
    }

    /**
     * Compute mean and standard deviation for intensity cycle 20th
     */
    private void computeRatioIntensityCycle20() {

      final List<Number> intensityCycle1Values =
          Lists.newArrayList(intensityCycle1ValuesPerTile.values());

      final List<Number> intensityCycle20Values =
          Lists.newArrayList(intensityCycle20ValuesPerTile.values());

      StatisticsUtils stat = new StatisticsUtils();

      // Compute % intensity C20 / intensity C1 for each tile
      for (int i = 0; i < intensityCycle1Values.size(); i++) {

        Double intensityC1 = (double) intensityCycle1Values.get(i).intValue();
        Double intensityC20 = (double) intensityCycle20Values.get(i).intValue();

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
     * Constructor
     * @param lane lane number
     * @param read read number
     */
    ExtractionMetricsPerLane(final int lane, final int read,
        final int read1FirstCycleNumber, final int read2FirstCycleNumber,
        final int read3FirstCycleNumber) throws AozanException {

      this.laneNumber = lane;
      this.readNumber = read;

      switch (readNumber) {
      case 1:
        this.firstCycleNumber = read1FirstCycleNumber;
        this.twentiethCycleNumber = read1FirstCycleNumber + 19;
        break;

      case 2:
        this.firstCycleNumber = read2FirstCycleNumber;
        this.twentiethCycleNumber = read2FirstCycleNumber + 19;
        break;

      case 3:
        this.firstCycleNumber = read3FirstCycleNumber;
        this.twentiethCycleNumber = read3FirstCycleNumber + 19;
        break;

      default:
        throw new AozanException(
            "In ExtractionMetricsCollector can't define the number of first and twentieth cycle for the reads of the run");
      }

      this.intensityCycle1ValuesPerTile = Maps.newTreeMap();
      this.intensityCycle20ValuesPerTile = Maps.newTreeMap();
    }
  }

}
