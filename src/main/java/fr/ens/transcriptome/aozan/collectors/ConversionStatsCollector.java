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

package fr.ens.transcriptome.aozan.collectors;

import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getElementsByTagName;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getTagValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;

/**
 * This class define a collector for demux statistics data from bcl2fastq
 * version 2.
 * @since 2.0
 * @author Sandrine Perrin
 */
public class ConversionStatsCollector extends DemultiplexingCollector {

  /** The Constant ALL_NAME_KEY. */
  private static final String ALL_NAME_KEY = "all";

  /** The Constant UNKNOWN_NAME_KEY. */
  private static final String UNKNOWN_NAME_KEY = "unknown";

  /** The Constant UNDETERMINED_NAME_KEY. */
  private static final String UNDETERMINED_NAME_KEY = "Undetermined";

  /** The casava output path. */
  private String casavaOutputPath;

  @Override
  public void configure(final Properties properties) {

    if (properties == null) {
      return;
    }

    this.casavaOutputPath = properties.getProperty(QC.CASAVA_OUTPUT_DIR);
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null) {
      return;
    }

    // Demux summary path
    final String demuxSummaryPath =
        this.casavaOutputPath + "/Stats/ConversionStats.xml";

    if (!new File(demuxSummaryPath).exists()) {
      throw new AozanException(
          "Demultiplexing Collector: source file not exists "
              + demuxSummaryPath);
    }

    // Create the input stream
    try (InputStream is = new FileInputStream(demuxSummaryPath)) {

      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      // Parse document to update run data
      parse(doc, data);

    } catch (final IOException e) {

      throw new AozanException(e);

    } catch (final SAXException e) {

      throw new AozanException(e);
    } catch (final ParserConfigurationException e) {

      throw new AozanException(e);
    }
  }

  /**
   * Parses the document.
   * @param document the document
   * @param data the data
   * @throws AozanException, it throws if an error occurs when extract data from
   *           document.
   */
  private void parse(final Document document, final RunData data)
      throws AozanException {

    final int readIndexedCount = countReadIndexed(data);

    String projectName;
    String sampleName;
    String barcodeSeq;
    int laneNumber;

    for (final Element project : getElementsByTagName(document, "Project")) {
      projectName = project.getAttribute("name");

      for (final Element sample : getElementsByTagName(project, "Sample")) {
        sampleName = sample.getAttribute("name");

        for (final Element barcode : getElementsByTagName(sample, "Barcode")) {
          barcodeSeq = barcode.getAttribute("name");

          for (final Element lane : getElementsByTagName(barcode, "Lane")) {
            laneNumber = Integer.parseInt(lane.getAttribute("number"));

            // Create Tile stats for new group tiles related tuple
            // sample/barecode/lane
            final GroupTilesStats stats =
                new GroupTilesStats(projectName, sampleName, laneNumber,
                    readIndexedCount);

            // Add tiles data
            stats.addTilesStats(lane, checkBarcodeSeq(barcodeSeq));

            // Compile data on group tiles in global
            stats.putData(data);
          }
        }
      }
    }
  }

  private int countReadIndexed(RunData data) {

    int count = 0;
    for (int read = 1; read <= data.getReadCount(); read++) {

      if (!data.isReadIndexed(read))
        count++;
    }
    return count;
  }

  /**
   * Check barcode sequence and replace unknown by undetermined.
   * @param barcodeSeq the barcode sequence
   * @return valid barcode sequence
   */
  private String checkBarcodeSeq(String barcodeSeq) {

    if (barcodeSeq.equals(ALL_NAME_KEY)) {
      return barcodeSeq;
    }

    if (barcodeSeq.equals(UNKNOWN_NAME_KEY)) {
      // Replace name by usually term
      return UNDETERMINED_NAME_KEY;
    }

    // TODO parse
    for (int i = 0; i < barcodeSeq.length(); i++) {
      char c = barcodeSeq.charAt(i);

      // Check sequences corresponding to good bases
      if (c != 'A'
          && c != 'T' && c != 'C' && c != 'G' && c != 'a' && c != 't'
          && c != 'c' && c != 'g')

        throw new AozanRuntimeException(
            "Demultiplexing Collector: in barcode sequence a base is invalid.");
    }

    // Return sequence
    return barcodeSeq;

  }

  @Override
  public void clear() {
    return;
  }

  //
  // Internal class
  //

  /**
   * The internal class compile statistics data per read and type (raw and
   * passing filter) from tiles.
   * @author Sandrine Perrin
   * @since 2.0
   */
  private static final class ReadStats {

    private final String type;
    private final int readNumber;

    // Statistic data
    private long clusterSum = 0;
    private long yieldSum = 0;
    private long yieldQ30Sum = 0;
    private long qualityScoreSum = 0;

    /**
     * Adds a tile statistics data.
     * @param data the tiles elements.
     * @param clusterCount the cluster count for a tile.
     */
    public void add(final Element data, final long clusterCount) {

      this.yieldSum += Long.parseLong(getTagValue(data, "Yield"));
      this.yieldQ30Sum += Long.parseLong(getTagValue(data, "YieldQ30"));
      this.qualityScoreSum +=
          Long.parseLong(getTagValue(data, "QualityScoreSum"));

      this.clusterSum += clusterCount;
    }

    //
    // Getter
    //

    /**
     * Gets the yield sum.
     * @return the yield sum
     */
    public String getYieldSum() {
      return this.yieldSum + "";
    }

    /**
     * Gets the yield q30 sum.
     * @return the yield q30 sum
     */
    public String getYieldQ30Sum() {
      return this.yieldQ30Sum + "";
    }

    /**
     * Gets the quality score sum.
     * @return the quality score sum
     */
    public String getQualityScoreSum() {
      return this.qualityScoreSum + "";
    }

    /**
     * Gets the cluster sum.
     * @return the cluster sum
     */
    public String getClusterSum() {
      return this.clusterSum + "";
    }

    //
    // Constructor
    //

    /**
     * Public constructor
     * @param type the type
     * @param readNumber the read number
     */
    public ReadStats(final String type, final int readNumber) {
      this.type = type;
      this.readNumber = readNumber;
    }

  }

  /**
   * The internal class compile statistics demultiplexing results data per
   * project, per sample, per lane and per barcode sequence.
   * @author Sandrine Perrin
   * @since 2.0
   */
  private static final class GroupTilesStats {

    public static final String RAW_TYPE = "Raw";
    public static final String PF_TYPE = "Pf";

    private final int readCount;

    private final Integer lane;
    private final String sampleName;
    private final String projectName;

    // Barcode sequence, per default at least 2 : all and one index sequence
    // Can be use second index sequence
    private List<String> barcodeSeqs;

    /** Save for readNumber-type the reads stats instance. */
    private Map<String, ReadStats> readStats;

    //
    // Methods to compile tiles statistics
    //

    /**
     * Adds a tiles stats.
     * @param lane the lane element
     * @param barcodeSeq the barcode sequence
     * @throws AozanException, it throws if an error occurs when extract data
     *           from lane element.
     */
    public void addTilesStats(final Element lane, final String barcodeSeq)
        throws AozanException {

      // Update list barcode sequences
      if (!this.barcodeSeqs.contains(barcodeSeq)) {
        this.barcodeSeqs.add(barcodeSeq);
      }

      for (final Element tile : getElementsByTagName(lane, "Tile")) {
        // Compile tile statistics element
        this.add(new TileStats(tile));
      }

    }

    /**
     * Adds a tile data.
     * @param t the tile stats instance.
     * @throws AozanException the aozan exception
     */
    public void add(final TileStats t) throws AozanException {

      if (t == null) {
        return;
      }

      // Parse read sequence
      for (int read = 1; read <= readCount; read++) {

        // Add raw data
        addDataStats(t, RAW_TYPE, t.clusterCountRaw, read);

        // Add PF data
        addDataStats(t, PF_TYPE, t.clusterCountPF, read);
      }
    }

    /**
     * Adds the data stats in reads statistics instance to compute all tiles
     * data.
     * @param t the tile stats instance
     * @param type the type between raw or pf
     * @param clusterCount the cluster count of a tile
     * @param read the read number
     * @throws AozanException, it throws if an error occurs when extract data
     *           from document.
     */
    private void addDataStats(final TileStats t, final String type,
        long clusterCount, final int read) throws AozanException {

      final String key = type + "_" + read;

      if (!readStats.containsKey(key)) {
        readStats.put(key, new ReadStats(type, read));
      }

      // Add value
      final Element e = t.getElementRead(type, read);
      readStats.get(key).add(e, clusterCount);

    }

    //
    // Methods to update run data
    //

    /**
     * Update run data.
     * @param runData the run data
     */
    public void putData(final RunData runData) {

      if (runData == null) {
        return;
      }

      // Count barcode sequences sequence used
      int count = 0;

      for (String barcodeSeq : this.barcodeSeqs) {

        // Add barcode sequence, the index is not indices with a number to
        // respect actual syntax entry in rundata
        if (barcodeSeq.equals(UNDETERMINED_NAME_KEY)) {

          // Add barcodes
          runData.put(String.format(PREFIX + ".lane%s.sample.lane%s.barcode",
              lane, lane), barcodeSeq);

        } else if (!barcodeSeq.equals(ALL_NAME_KEY)) {

          // Add barcodes
          runData.put(String.format(PREFIX + ".lane%s.sample.%s.barcode%s",
              lane, sampleName, (count == 0 ? "" : count)), barcodeSeq);
          count++;
        }

        // Parse all barcode seq
        for (Map.Entry<String, ReadStats> e : this.readStats.entrySet()) {
          final ReadStats stats = e.getValue();

          putReadData(runData, stats, barcodeSeq);

        }
      }
    }

    /**
     * Builds the prefix run data.
     * @param stats the read stats instance
     * @param barcodeSeq the barcode sequence
     * @return the prefix
     */
    private String buildPrefixRunData(final ReadStats stats,
        final String barcodeSeq) {

      if (barcodeSeq.equals(ALL_NAME_KEY)
          && projectName.equals(ALL_NAME_KEY)
          && sampleName.equals(ALL_NAME_KEY)) {

        return String.format(PREFIX + ".lane%s.%s.read%s.%s", lane, sampleName,
            stats.readNumber, stats.type);
      }

      if (barcodeSeq.equals(ALL_NAME_KEY)) {

        return String.format(PREFIX + ".lane%s.sample.%s.read%s.%s", lane,
            sampleName, stats.readNumber, stats.type);
      }

      if (barcodeSeq.equals(UNDETERMINED_NAME_KEY)) {
        return String.format(PREFIX + ".lane%s.sample.lane%s.read%s.%s", lane,
            lane, stats.readNumber, stats.type);
      }

      return String.format(PREFIX + ".lane%s.sample.%s.read%s.%s.%s", lane,
          sampleName, stats.readNumber, barcodeSeq, stats.type);
    }

    /**
     * Put read data with statistic data.
     * @param runData the run data
     * @param stats the read stats instance
     * @param barcodeSeq the barcode sequence
     */
    private void putReadData(final RunData runData, final ReadStats stats,
        String barcodeSeq) {

      final String prefix = buildPrefixRunData(stats, barcodeSeq);

      // Summary sample
      runData.put(prefix + ".cluster.count", stats.getClusterSum());
      runData.put(prefix + ".yield", stats.getYieldSum());
      runData.put(prefix + ".yield.q30", stats.getYieldQ30Sum());
      runData.put(prefix + ".quality.score.sum", stats.getQualityScoreSum());

      // TODO to remove after test
      // final String debug =
      // String.format("%s;%s;%s;%s;lane%s;read%s;%s", "debug", projectName,
      // sampleName, barcodeSeq, lane, stats.readNumber, stats.type);
      // runData.put(debug + ".cluster.count", stats.getClusterSum());
      // runData.put(debug + ".yield", stats.getYieldSum());
      // runData.put(debug + ".yield.q30", stats.getYieldQ30Sum());
      // runData.put(debug + ".quality.score.sum", stats.getQualityScoreSum());

    }

    //
    // Constructor
    //

    /**
     * Public constructor
     * @param projectName the project name
     * @param sampleName the sample name
     * @param lane the lane
     */
    public GroupTilesStats(final String projectName, final String sampleName,
        final int lane, final int readCount) {
      this.lane = lane;
      this.sampleName = sampleName;
      this.projectName = projectName;
      this.readCount = readCount;

      this.readStats = new HashMap<>();
      this.barcodeSeqs = new ArrayList<>();

    }
  }

  /**
   * The internal class extract data from tile element.
   * @author Sandrine Perrin
   * @since 2.0
   */
  private static final class TileStats {

    private final Element tileElem;
    private final long clusterCountPF;
    private final long clusterCountRaw;

    /**
     * Gets the element read according to the type (raw pr pf) and read number.
     * @param type the type
     * @param readNumber the read number
     * @return the element read
     * @throws AozanException the aozan exception
     */
    public Element getElementRead(final String type, final int readNumber)
        throws AozanException {

      // Extract element related to the required type
      final Element typeElem = getElementsByTagName(this.tileElem, type).get(0);

      // Find element read to the required read number
      for (Element readElem : getElementsByTagName(typeElem, "Read")) {
        final String readAtt = readElem.getAttribute("number").trim();

        if (readAtt.equals(readNumber + "")) {

          // Return element
          return readElem;
        }
      }

      // No found expected element
      throw new AozanException(
          "Parse XML file, not found element in tile element for type "
              + type + " read " + readNumber);
    }

    /**
     * Extract cluster count.
     * @param type the type
     * @return cluster count for a tile
     */
    private long extractClusterCount(final String type) {

      final Element e = getElementsByTagName(this.tileElem, type).get(0);

      if (e == null) {
        throw new AozanRuntimeException(
            "Demultiplexing collector: no found tile element related to the type "
                + type);
      }

      return Long.parseLong(getTagValue(e, "ClusterCount"));
    }

    //
    // Constructors
    //

    /**
     * Public constructor
     * @param tileElem the tile element
     */
    public TileStats(final Element tileElem) {

      this.tileElem = tileElem;

      this.clusterCountRaw = extractClusterCount(GroupTilesStats.RAW_TYPE);
      this.clusterCountPF = extractClusterCount(GroupTilesStats.PF_TYPE);

    }

  }

}
