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

import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeValue;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getTagValue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class define a collector for demux statistics data.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class FlowcellDemuxSummaryCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "demux";

  private String casavaOutputPath;

  private static final class TileStats {

    private long yield;
    private long yieldQ30;
    private long clusterCount;
    private long clusterCount0MismatchBarcode;
    private long clusterCount1MismatchBarcode;
    private long qualityScoreSum;

    public void add(final TileStats t) {

      if (t == null)
        return;

      this.yield += t.yield;
      this.yieldQ30 += t.yieldQ30;
      this.clusterCount += t.clusterCount;
      this.clusterCount0MismatchBarcode += t.clusterCount0MismatchBarcode;
      this.clusterCount1MismatchBarcode += t.clusterCount1MismatchBarcode;
      this.qualityScoreSum += t.qualityScoreSum;
    }

    @Override
    public String toString() {

      return Objects.toStringHelper(this).add("yield", yield)
          .add("yieldQ30", yieldQ30).add("clusterCount", clusterCount)
          .add("clusterCount0MismatchBarcode", clusterCount0MismatchBarcode)
          .add("clusterCount1MismatchBarcode", clusterCount1MismatchBarcode)
          .add("qualityScoreSum", qualityScoreSum).toString();
    }

    public void putData(final RunData runData, final String prefix) {

      if (runData == null || prefix == null)
        return;

      runData.put(prefix + ".yield", this.yield);
      runData.put(prefix + ".yield.q30", this.yieldQ30);
      runData.put(prefix + ".cluster.count", this.clusterCount);
      runData.put(prefix + ".cluster.count.0.mismatch.barcode",
          this.clusterCount0MismatchBarcode);
      runData.put(prefix + ".cluster.count.1.mismatch.barcode",
          this.clusterCount1MismatchBarcode);
      runData.put(prefix + ".quality.score.sum", this.qualityScoreSum);
    }

    //
    // Constructors
    //

    public TileStats() {
    }

    public TileStats(final Element e) {
      this.yield = Long.parseLong(getTagValue(e, "Yield"));
      this.yieldQ30 = Long.parseLong(getTagValue(e, "YieldQ30"));
      this.clusterCount = Long.parseLong(getTagValue(e, "ClusterCount"));
      this.clusterCount0MismatchBarcode =
          Long.parseLong(getTagValue(e, "ClusterCount0MismatchBarcode"));
      this.clusterCount1MismatchBarcode =
          Long.parseLong(getTagValue(e, "ClusterCount1MismatchBarcode"));
      this.qualityScoreSum = Long.parseLong(getTagValue(e, "QualityScoreSum"));
    }

  }

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

    if (properties == null)
      return;

    this.casavaOutputPath = properties.getProperty(QC.CASAVA_OUTPUT_DIR);
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null)
      return;

    try {

      // Demux summary path
      final String demuxSummaryPath =
          this.casavaOutputPath
              + "/Basecall_Stats_" + data.get("run.info.flow.cell.id")
              + "/Flowcell_demux_summary.xml";

      // Create the input stream
      final InputStream is = new FileInputStream(demuxSummaryPath);

      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      parse(doc, data);

      is.close();
    } catch (IOException e) {

      throw new AozanException(e);

    } catch (SAXException e) {

      throw new AozanException(e);
    } catch (ParserConfigurationException e) {

      throw new AozanException(e);
    }
  }

  private void parse(final Document document, final RunData data) {

    for (Element e1 : XMLUtils.getElementsByTagName(document, "Summary"))
      for (Element e2 : XMLUtils.getElementsByTagName(e1, "Lane"))
        parseLane(e2, data);

  }

  private void parseLane(final Element e, final RunData data) {

    final int lane = Integer.parseInt(getAttributeValue(e, "index"));

    final Map<Integer, TileStats> rawLine = Maps.newHashMap();
    final Map<Integer, TileStats> pfLine = Maps.newHashMap();

    // Parse samples of the line
    for (Element e1 : XMLUtils.getElementsByTagName(e, "Sample"))
      parseSample(e1, lane, data, rawLine, pfLine);

    // Put the line stats
    for (Map.Entry<Integer, TileStats> entry : rawLine.entrySet())
      entry.getValue().putData(data,
          "demux.lane" + lane + ".all.read" + entry.getKey() + ".raw");

    for (Map.Entry<Integer, TileStats> entry : pfLine.entrySet())
      entry.getValue().putData(data,
          "demux.lane" + lane + ".all.read" + entry.getKey() + ".pf");
  }

  private void parseSample(final Element e, final int lane, final RunData data,
      final Map<Integer, TileStats> rawLine,
      final Map<Integer, TileStats> pfLine) {

    final String sample = getAttributeValue(e, "index").trim();

    for (final Element e1 : XMLUtils.getElementsByTagName(e, "Barcode")) {
      final String barcode = getAttributeValue(e1, "index").trim();

      Map<Integer, TileStats> mapRaw = Maps.newHashMap();
      Map<Integer, TileStats> mapPF = Maps.newHashMap();

      for (final Element e2 : XMLUtils.getElementsByTagName(e1, "Tile")) {
        // final int tile = Integer.parseInt(getAttributeValue(e2, "index"));

        for (final Element e3 : XMLUtils.getElementsByTagName(e2, "Read")) {
          final int read = Integer.parseInt(getAttributeValue(e3, "index"));

          if (!rawLine.containsKey(read)) {
            rawLine.put(read, new TileStats());
            pfLine.put(read, new TileStats());
          }

          final TileStats raw;
          final TileStats pf;

          if (!mapRaw.containsKey(read)) {
            raw = new TileStats();
            pf = new TileStats();
            mapRaw.put(read, raw);
            mapPF.put(read, pf);
          } else {
            raw = mapRaw.get(read);
            pf = mapPF.get(read);
          }

          for (final Element e4 : XMLUtils.getElementsByTagName(e3, "Raw")) {
            raw.add(new TileStats(e4));
          }

          for (final Element e5 : XMLUtils.getElementsByTagName(e3, "Pf")) {
            pf.add(new TileStats(e5));
          }

        }

      }

      final String prefix = "demux.lane" + lane + ".sample." + sample;

      data.put(prefix + ".barcode", barcode);

      for (Map.Entry<Integer, TileStats> entry : mapRaw.entrySet()) {
        final int read = entry.getKey();
        final TileStats ts = entry.getValue();
        rawLine.get(read).add(ts);
        ts.putData(data, prefix + ".read" + entry.getKey() + ".raw");
      }

      for (Map.Entry<Integer, TileStats> entry : mapPF.entrySet()) {
        final int read = entry.getKey();
        final TileStats ts = entry.getValue();
        pfLine.get(read).add(ts);
        ts.putData(data, prefix + ".read" + entry.getKey() + ".pf");
      }
    }
  }

  @Override
  public void clear() {
    return;
  }
}
