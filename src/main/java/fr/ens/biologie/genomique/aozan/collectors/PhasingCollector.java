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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
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

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.eoulsan.util.XMLUtils;

/**
 * This class define a collector for phasing/prephasing statistics data.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class PhasingCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "phasing";

  private String casavaOutputPath;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public boolean isStatisticCollector() {
    return false;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(RunInfoCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final QC qc, final Properties properties) {

    if (properties == null) {
      return;
    }

    this.casavaOutputPath = qc.getFastqDir().getAbsolutePath();
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null) {
      return;
    }

    try {

      // Demux summary path
      final File phasingStatsDir =
          new File(this.casavaOutputPath
              + "/Basecall_Stats_" + data.get("run.info.flow.cell.id")
              + "/Phasing");

      // Get lane count
      final int laneCount = data.getLaneCount();

      for (final Map.Entry<Integer, Integer> entry : getCyclesPhasing(data)
          .entrySet()) {

        final int read = entry.getKey();
        final int cycle = entry.getValue();

        for (int lane = 1; lane <= laneCount; lane++) {

          final File phasingFile =
              new File(phasingStatsDir, String.format("s_%d_%02d_phasing.xml",
                  lane, cycle));
          parse(phasingFile, read, lane, data);
        }
      }

    } catch (final IOException | SAXException | ParserConfigurationException e) {
      throw new AozanException(e);
    }
  }

  private static void parse(final File file, final int read,
      final int lane, final RunData data) throws ParserConfigurationException,
      SAXException, IOException {

    // Create the input stream
    final InputStream is = new FileInputStream(file);

    final DocumentBuilder dBuilder =
        DocumentBuilderFactory.newInstance().newDocumentBuilder();
    final Document document = dBuilder.parse(is);
    document.getDocumentElement().normalize();

    for (final Element e : XMLUtils.getElementsByTagName(document, "Parameters")) {

      final double phasing =
          Double.parseDouble(XMLUtils.getTagValue(e, "Phasing"));
      final double prephasing =
          Double.parseDouble(XMLUtils.getTagValue(e, "Prephasing"));

      final String prefix = "phasing.read" + read + ".lane" + lane;
      data.put(prefix + ".phasing", phasing);
      data.put(prefix + ".prephasing", prephasing);
    }

    is.close();
  }

  private static Map<Integer, Integer> getCyclesPhasing(final RunData data) {

    // run.info.read.count=2
    // run.info.read1.cycles=51

    final Map<Integer, Integer> result = new LinkedHashMap<>();

    final int readCount = data.getReadCount();

    int cyclesCount = 0;
    for (int read = 1; read <= readCount; read++) {
      result.put(read, cyclesCount + 2);
      cyclesCount += data.getReadCyclesCount(read);
    }

    return result;
  }

  @Override
  public void clear() {
  }
}
