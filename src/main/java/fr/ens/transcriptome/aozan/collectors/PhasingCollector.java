/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan.collectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.RunDataGenerator;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class define a collector for phasing/prephasing statistics data.
 * @since 1.0
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
  public String[] getCollectorsNamesRequiered() {

    return new String[] {RunInfoCollector.COLLECTOR_NAME};
  }

  @Override
  public void configure(final Properties properties) {

    if (properties == null)
      return;

    this.casavaOutputPath =
        properties.getProperty(RunDataGenerator.CASAVA_OUTPUT_DIR);
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null)
      return;

    try {

      // Demux summary path
      final File phasingStatsDir =
          new File(this.casavaOutputPath
              + "/Basecall_Stats_" + data.get("run.info.flow.cell.id")
              + "/Phasing");

      // Get lane count
      final int laneCount = data.getInt("run.info.flow.cell.lane.count");

      for (Map.Entry<Integer, Integer> entry : getCyclesPhasing(data)
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

    } catch (IOException e) {

      throw new AozanException(e);

    } catch (SAXException e) {

      throw new AozanException(e);
    } catch (ParserConfigurationException e) {

      throw new AozanException(e);
    }
  }

  private static final void parse(final File file, final int read,
      final int lane, final RunData data) throws ParserConfigurationException,
      SAXException, IOException {

    // Create the input stream
    final InputStream is = new FileInputStream(file);

    final DocumentBuilder dBuilder =
        DocumentBuilderFactory.newInstance().newDocumentBuilder();
    final Document document = dBuilder.parse(is);
    document.getDocumentElement().normalize();

    for (Element e : XMLUtils.getElementsByTagName(document, "Parameters")) {

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

  private static final Map<Integer, Integer> getCyclesPhasing(final RunData data) {

    // run.info.read.count=2
    // run.info.read1.cycles=51

    final Map<Integer, Integer> result = Maps.newLinkedHashMap();

    final int readCount = data.getInt("run.info.read.count");

    int cyclesCount = 0;
    for (int read = 1; read <= readCount; read++) {
      result.put(read, cyclesCount + 2);
      cyclesCount += data.getInt("run.info.read" + read + ".cycles");
    }

    return result;
  }

}
