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

import com.google.common.base.Joiner;

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

  private static final String ALL_NAME_KEY = "all";
  private static final String UNKNOWN_NAME_KEY = "unknown";
  private static final String UNDETERMINED_NAME_KEY = "Undetermined";

  private String casavaOutputPath;

  @Override
  public void configure(final Properties properties) {

    if (properties == null) {
      return;
    }

    this.casavaOutputPath = properties.getProperty(QC.CASAVA_OUTPUT_DIR);
  }

  public void collect(final RunData data) throws AozanException {

    if (data == null) {
      return;
    }

    try {

      // Demux summary path
      final String demuxSummaryPath =
          this.casavaOutputPath + "/Stats/ConversionStats.xml";

      if (!new File(demuxSummaryPath).exists()) {
        throw new AozanException(
            "Demultiplexing Collector: source file not exists "
                + demuxSummaryPath);
      }

      // Create the input stream
      final InputStream is = new FileInputStream(demuxSummaryPath);

      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      parse(doc, data);

      is.close();
    } catch (final IOException e) {

      throw new AozanException(e);

    } catch (final SAXException e) {

      throw new AozanException(e);
    } catch (final ParserConfigurationException e) {

      throw new AozanException(e);
    }
  }

  private void parse(final Document document, final RunData data)
      throws AozanException {

    System.out.println("\t demux star parsing xml.");

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

            System.out.println(String.format(
                "project %s\tsample %s\tbarcode %s\tlane %s", projectName,
                sampleName, barcodeSeq, laneNumber));

            // Create Tile stats for new group tiles related tuple
            // sample/barecode/lane
            final GroupTilesStats stats =
                new GroupTilesStats(projectName, sampleName, laneNumber);
            // demuxData.add(stats);

            // Add tiles data
            stats.addTilesStats(lane, checkBarcodeSeq(barcodeSeq));

            // Compile data on group tiles in global
            stats.putData(data);
          }
        }
      }
    }
  }

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
      if (c != 'A' && c != 'T' && c != 'C' && c != 'G')
        throw new AozanRuntimeException("");
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

  private static final class BarcodeStats {

    private final String barcodeSeq;
    private final String type;

    private long clusterSum = 0;

    private long yieldSum = 0;
    private long yieldQ30Sum = 0;
    private long qualityScoreSum = 0;

    public void add(final Element data, final long clusterCount) {

      this.yieldSum += Long.parseLong(getTagValue(data, "Yield"));
      this.yieldQ30Sum += Long.parseLong(getTagValue(data, "YieldQ30"));
      this.qualityScoreSum +=
          Long.parseLong(getTagValue(data, "QualityScoreSum"));

      this.clusterSum += clusterCount;
    }

    public String getBarcodeSeq() {
      return barcodeSeq;
    }

    public String getYieldSum() {
      return this.yieldSum + "";
    }

    public String getYieldQ30Sum() {
      return this.yieldQ30Sum + "";
    }

    public String getQualityScoreSum() {
      return this.qualityScoreSum + "";
    }

    public String getClusterSum() {
      return this.clusterSum + "";
    }

    @Override
    public String toString() {
      return "BarcodeStats [barcodeSeq="
          + barcodeSeq + ", type=" + type + ", clusterSum=" + clusterSum
          + ", yieldSum=" + yieldSum + ", yieldQ30Sum=" + yieldQ30Sum
          + ", qualityScoreSum=" + qualityScoreSum + "]";
    }

    public BarcodeStats(final String barcodeSeq, final String type) {

      this.barcodeSeq = barcodeSeq;
      this.type = type;
    }

  }

  private static final class ReadStats {

    private final String type;
    private final int readNumber;
    private final Map<String, BarcodeStats> allStats;

    public void add(final Element data, final String barcodeSeq,
        final long clusterCount) {

      if (!this.allStats.containsKey(barcodeSeq)) {
        // Add new entry in map stats
        this.allStats.put(barcodeSeq, new BarcodeStats(barcodeSeq, this.type));

      }

      // Add new statistics data
      this.allStats.get(barcodeSeq).add(data, clusterCount);

    }

    public String getClusterCount(String barcodeSeq) {

      if (this.allStats.containsKey(barcodeSeq))

        return this.allStats.get(barcodeSeq).getYieldSum();

      throw new AozanRuntimeException(
          "DemuxCollector: missing tiles statistics Yield sum data related to barcode "
              + barcodeSeq);
    }

    public String getYieldSum(final String barcodeSeq) {

      if (this.allStats.containsKey(barcodeSeq))

        return this.allStats.get(barcodeSeq).getYieldSum();

      throw new AozanRuntimeException(
          "DemuxCollector: missing tiles statistics Yield sum data related to barcode "
              + barcodeSeq);
    }

    public String getYieldQ30Sum(final String barcodeSeq) {

      if (this.allStats.containsKey(barcodeSeq))

        return this.allStats.get(barcodeSeq).getYieldQ30Sum();

      throw new AozanRuntimeException(
          "DemuxCollector: missing tiles statistics Yield Q30 sum data related to barcode "
              + barcodeSeq);
    }

    public String getQualityScoreSum(final String barcodeSeq) {

      if (this.allStats.containsKey(barcodeSeq))

        return this.allStats.get(barcodeSeq).getQualityScoreSum();

      throw new AozanRuntimeException(
          "DemuxCollector: missing tiles statistics Quality Score sum data related to barcode "
              + barcodeSeq);
    }

    @Override
    public String toString() {
      return "ReadStats [type="
          + type + ", readNumber=" + readNumber + ", allStats="
          + Joiner.on("\n").withKeyValueSeparator("=").join(allStats) + "]";
    }

    public ReadStats(final String type, final int readNumber) {
      this.type = type;
      this.readNumber = readNumber;

      this.allStats = new HashMap<>();
    }

  }

  private static final class GroupTilesStats {

    public static final String RAW_TYPE = "Raw";
    public static final String PF_TYPE = "Pf";

    private final int readCount = 1;

    private final Integer lane;
    private final String sampleName;
    private final String projectName;

    private List<String> barcodeSeqs;

    private Map<String, ReadStats> readStats;

    //
    // Methods to compile tiles statistics
    //

    public void addTilesStats(final Element lane, final String barcodeSeq)
        throws AozanException {

      // Update list barcode sequences
      if (!this.barcodeSeqs.contains(barcodeSeq)) {
        this.barcodeSeqs.add(barcodeSeq);
      }

      for (final Element tile : getElementsByTagName(lane, "Tile")) {
        // Compile tile statistics element
        this.add(new TileStats(tile), barcodeSeq);
      }

    }

    public void add(final TileStats t, final String barcodeSeq)
        throws AozanException {

      if (t == null) {
        return;
      }

      // Parse read sequence
      for (int read = 1; read <= readCount; read++) {

        // Add raw data
        addDataStats(t, RAW_TYPE, t.clusterCountRaw, read, barcodeSeq);

        // Add PF data
        addDataStats(t, PF_TYPE, t.clusterCountPF, read, barcodeSeq);
      }
    }

    private void addDataStats(final TileStats t, final String type,
        long clusterCount, final int read, final String barcodeSeq)
        throws AozanException {

      final String key = type + "_" + read;

      if (!readStats.containsKey(key)) {
        readStats.put(key, new ReadStats(type, read));
      }

      // Add value
      final Element e = t.getElementRead(type, read);
      readStats.get(key).add(e, barcodeSeq, clusterCount);

    }

    //
    // Methods to update run data
    //

    public void putData(final RunData runData) {

      if (runData == null) {
        return;
      }

      System.out.println(" PUT data contains map "
          + Joiner.on("\n").withKeyValueSeparator("\t").join(this.readStats));

      // Count barcode sequences sequence used
      int count = 0;

      // Parse all barcode seq
      for (String barcodeSeq : this.barcodeSeqs) {

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

        for (Map.Entry<String, ReadStats> e : this.readStats.entrySet()) {
          final ReadStats stats = e.getValue();

          putReadData(runData, stats, barcodeSeq);

        }
      }
    }

    private String buildPrefixRunData(final ReadStats stats,
        final String barcodeSeq) {

      if (barcodeSeq.equals(ALL_NAME_KEY)) {
        return String.format(PREFIX + ".lane%s.sample.%s.read%s.%s", lane,
            sampleName, stats.readNumber, stats.type);
      }

      if (barcodeSeq.equals(UNDETERMINED_NAME_KEY)) {
        return String.format(PREFIX + ".lane%s.sample.read%s.%s", lane,
            stats.readNumber, stats.type);
      }

      return String.format(PREFIX + ".lane%s.sample.%s.read%s.%s.%s", lane,
          sampleName, stats.readNumber, barcodeSeq, stats.type);
    }

    private void putReadData(final RunData runData, final ReadStats stats,
        String barcodeSeq) {

      final String prefix = buildPrefixRunData(stats, barcodeSeq);

      // Summary sample
      runData.put(prefix + ".cluster.count", stats.getClusterCount(barcodeSeq));
      runData.put(prefix + ".yield", stats.getYieldSum(barcodeSeq));
      runData.put(prefix + ".yield.q30", stats.getYieldQ30Sum(barcodeSeq));
      runData.put(prefix + ".quality.score.sum",
          stats.getQualityScoreSum(barcodeSeq));

    }

    public GroupTilesStats(final String projectName, final String sampleName,
        final int lane) {
      this.lane = lane;
      this.sampleName = sampleName;
      this.projectName = projectName;

      this.readStats = new HashMap<>();
      this.barcodeSeqs = new ArrayList<>();

    }
  }

  /**
   * The Class TileStats.
   * @author Sandrine Perrin
   * @since 2.0
   */
  private static final class TileStats {

    private final Element tileElem;
    private final long clusterCountPF;
    private final long clusterCountRaw;

    public Element getElementRead(final String type, final int readValue)
        throws AozanException {

      final Element typeElem = getElementsByTagName(this.tileElem, type).get(0);

      if (this.tileElem.getAttribute("number").equals("1101"))
        System.out.println("extract tile element for type "
            + type + " and read " + readValue);

      for (Element readElem : getElementsByTagName(typeElem, "Read")) {
        final String readAtt = readElem.getAttribute("number").trim();

        if (readAtt.equals(readValue + "")) {
          return readElem;
        }
      }

      throw new AozanException(
          "Parse XML file, not found element in tile element for type "
              + type + " read " + readValue);
    }

    private long extractClusterCount(final String type) {

      final Element e = getElementsByTagName(this.tileElem, type).get(0);

      if (e == null) {

      }
      return Long.parseLong(getTagValue(e, "ClusterCount"));
    }

    //
    // Constructors
    //

    public TileStats(final Element tileElem) {

      this.tileElem = tileElem;

      this.clusterCountRaw = extractClusterCount(GroupTilesStats.RAW_TYPE);
      this.clusterCountPF = extractClusterCount(GroupTilesStats.PF_TYPE);

    }

  }

  //
  // Main method for test
  //

  // TODO remove after test
  private void collect(final RunData data, final String demuxSummaryPath)
      throws AozanException {

    if (data == null) {
      return;
    }

    try {

      if (!new File(demuxSummaryPath).exists()) {
        throw new AozanException(
            "Demultiplexing Collector: source file not exists "
                + demuxSummaryPath);
      }

      // Create the input stream
      final InputStream is = new FileInputStream(demuxSummaryPath);

      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      parse(doc, data);

      is.close();
    } catch (final Exception e) {

      throw new AozanException(e);
    }
  }

  // TODO remove after test
  public static void main(String[] argv) throws Exception {
    final String f =
        "/import/rhodos01/shares-net/sequencages/nextseq_500/fastq/"
            + "150416_NB500892_0002_AH7MNKBGXX/Stats/ConversionStats.xml";

    final RunData data = new RunData();

    final ConversionStatsCollector demux = new ConversionStatsCollector();

    demux.collect(data, f);
    data.createRunDataFile("/tmp/demux.data");
  }
}
