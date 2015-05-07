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

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;

/**
 * This class define a collector for demux statistics data.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class DemultiplexingCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "demux";

  private String casavaOutputPath;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(RunInfoCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final Properties properties) {

    if (properties == null) {
      return;
    }

    this.casavaOutputPath = properties.getProperty(QC.CASAVA_OUTPUT_DIR);
  }

  @Override
  public void collect(final RunData data) {
  }

  // @Override
  public void collect(final RunData data, final String file)
      throws AozanException {

    if (data == null) {
      return;
    }

    try {

      // Demux summary path
      final String demuxSummaryPath = file;
      // this.casavaOutputPath
      // + "/Basecall_Stats_" + data.get("run.info.flow.cell.id")
      // + "/Flowcell_demux_summary.xml";

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
    final List<TilesStats> demuxData = new ArrayList<>();

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

            final TilesStats stats =
                new TilesStats(projectName, sampleName, barcodeSeq, laneNumber);
            // demuxData.add(stats);

            parseStats(lane, stats);

            stats.putData(data);
          }
        }
      }
    }

  }

  private void parseStats(final Element lane, final TilesStats stats)
      throws AozanException {

    for (final Element tile : getElementsByTagName(lane, "Tile")) {
      stats.add(new TileStats(tile));
    }

  }

  @Override
  public void clear() {
    return;
  }

  //
  // Internal class
  //

  private static final class ReadStats {

    private final String type;
    private final int readNumber;

    private long yieldSum = 0;
    private long yieldQ30Sum = 0;
    private long qualityScoreSum = 0;

    public void add(final Element data) {

      this.yieldSum += Long.parseLong(getTagValue(data, "Yield"));
      this.yieldQ30Sum += Long.parseLong(getTagValue(data, "YieldQ30"));
      this.qualityScoreSum +=
          Long.parseLong(getTagValue(data, "QualityScoreSum"));

    }

    public ReadStats(final String type, final int readNumber) {
      this.type = type;
      this.readNumber = readNumber;
    }

  }

  private static final class TilesStats {

    private final int readCount = 1;

    private final Integer lane;
    private final String barcode;
    private final String sampleName;
    private final String projectName;

    private long clusterSum;

    private Map<String, ReadStats> readStats;

    // private long yieldPFSum;
    // private long yieldQ30PFSum;
    // private long qualityScorePFSum;

    public void putData(final RunData runData) {

      if (runData == null) {
        return;
      }

      for (Map.Entry<String, ReadStats> e : this.readStats.entrySet()) {
        final ReadStats stats = e.getValue();
        final String prefix = buildPrefixRunData(stats);

        putReadData(runData, stats, prefix, stats.readNumber);

      }
    }

    private String buildPrefixRunData(final ReadStats stats) {

      return String.format("demux.lane%s.%s.read%s.%s", lane, sampleName,
          stats.readNumber, stats.type);
    }

    private void putReadData(final RunData runData, final ReadStats stats,
        final String prefix, final int read) {

      runData.put(prefix + ".yield", stats.yieldSum);
      runData.put(prefix + ".yield.q30", stats.yieldQ30Sum);
      runData.put(prefix + ".cluster.count", clusterSum);
      runData.put(prefix + ".quality.score.sum", stats.qualityScoreSum);
    }

    public void add(final TileStats t) throws AozanException {

      if (t == null) {
        return;
      }
      this.clusterSum += t.clusterCount;

      // Parse read sequence
      for (int read = 1; read <= readCount; read++) {

        // Add raw data
        addDataStats(t, "Raw", read);

        // Add PF data
        addDataStats(t, "Pf", read);
      }
    }

    private void addDataStats(final TileStats t, final String type,
        final int read) throws AozanException {

      final String key = type + "_" + read;

      if (!readStats.containsKey(key)) {
        readStats.put(key, new ReadStats(type, read));
      }

      // Add value
      final Element e = t.getElementRead(type, read);
      readStats.get(key).add(e);

    }

    public TilesStats(final String projectName, final String sampleName,
        final String barcode, final int lane) {
      this.lane = lane;
      this.barcode = barcode;
      this.sampleName = sampleName;
      this.projectName = projectName;

      this.readStats = new HashMap<>();

    }
  }

  private static final class TileStats {

    static final String RAW_TYPE = "Raw";
    static final String PF_TYPE = "Pf";

    private final Element tileElem;
    private final long clusterCount;

    public Element getElementRead(final String type, final int readValue)
        throws AozanException {

      final Element typeElem = getElementsByTagName(this.tileElem, type).get(0);

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

    //
    // Constructors
    //

    public TileStats(final Element tileElem) {

      this.clusterCount = Long.parseLong(getTagValue(tileElem, "ClusterCount"));
      this.tileElem = tileElem;

      // this.yieldPF = Long.parseLong(getTagValue(pfData, "Yield"));
      // this.yieldQ30PF = Long.parseLong(getTagValue(pfData, "YieldQ30"));
      // this.qualityScoreSumPF =
      // Long.parseLong(getTagValue(pfData, "QualityScoreSum"));

    }
  }

  public static void main(String[] argv) throws Exception {
    final String f =
        "/Users/sandrine/Documents/IBENS/demux/ConversionStats.xml";
    final RunData data = new RunData();

    final DemultiplexingCollector demux = new DemultiplexingCollector();

    demux.collect(data, f);
    data.createRunDataFile("/Users/sandrine/Documents/IBENS/demux/demux.data");
  }
}
