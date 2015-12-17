/*
 *                   Aozan development code
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
 *      http://tools.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors;

import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeNames;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeValue;

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
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This internal class define a Collector for read?.xml files.
 * @author Laurent Jourdren
 * @since 1.1
 */
public class ReadXMLCollector implements Collector {

  /** Sub-collector for readCollector. */
  private static final String NAME_COLLECTOR = "ReadXMLCollector";

  private String RTAOutputDirPath;

  /**
   * Collect data for each read?.xml file.
   * @param data RunData object
   * @throws AozanException if an error occurs while collecting data
   */
  @Override
  public void collect(final RunData data) throws AozanException {

    // Collect read info
    final int readCount = data.getReadCount();

    for (int i = 1; i <= readCount; i++) {
      final String readInfoFilePath =
          this.RTAOutputDirPath + "/Data/reports/Summary/read" + i + ".xml";
      collectXMLFile(data, readInfoFilePath);
    }
  }

  /**
   * Collect data for a read?.xml file.
   * @param data RunData object
   * @param readInfoFilePath the read?.xml file path
   */
  private void collectXMLFile(final RunData data, final String readInfoFilePath)
      throws AozanException {

    if (data == null) {
      return;
    }

    try {

      // Create the input stream
      final InputStream is = new FileInputStream(new File(readInfoFilePath));

      // Read the XML file
      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      // Parse the document
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

  /**
   * Parse the XML document.
   * @param document Document DOM object
   * @param data the result
   */
  private void parse(final Document document, final RunData data) {

    for (final Element e : XMLUtils.getElementsByTagName(document, "Summary")) {

      int readNumber = -1;
      double densityRatio = -1.0;
      String readType = "";

      // Parse Summary tag attributes
      for (final String attributeName : getAttributeNames(e)) {

        if ("Read".equals(attributeName)) {
          readNumber = Integer.parseInt(getAttributeValue(e, attributeName));
        } else if ("ReadType".equals(attributeName)) {
          readType = getAttributeValue(e, attributeName);
        } else if ("densityRatio".equals(attributeName)) {
          densityRatio =
              Double.parseDouble(getAttributeValue(e, attributeName));
        }
      }

      final String prefix = "read" + readNumber;
      data.put(prefix + ".density.ratio", densityRatio);
      data.put(prefix + ".type", readType);

      // Parse Lane tag
      for (final Element laneElement : XMLUtils.getElementsByTagName(e, "Lane")) {

        int lane = -1;
        final Map<String, String> map = new LinkedHashMap<>();

        for (final String key : XMLUtils.getAttributeNames(laneElement)) {

          final String value = laneElement.getAttribute(key);

          if ("key".equals(key)) {
            lane = Integer.parseInt(value);
          } else {
            map.put(convertKey(key), value);
          }
        }

        for (final Map.Entry<String, String> entry : map.entrySet()) {
          data.put(prefix + ".lane" + lane + "." + entry.getKey(),
              entry.getValue());
        }
      }
    }
  }

  /**
   * Convert automatically attribute names to keys in RunData keys format.
   * @param key key to convert
   * @return the converted key
   */
  private String convertKey(final String key) {

    if (key == null) {
      return null;
    }

    if (key.length() == 1) {
      return key.toLowerCase();
    }

    String k = key;
    if (k.endsWith("SD")) {
      k = k.substring(0, k.length() - 2) + ".sd";
    }

    k = k.replace("PhiX", ".phix");

    final StringBuilder sb = new StringBuilder();
    final int len = k.length();

    int lastUp = 0;

    boolean lastDigit = false;
    int startWord = 0;

    for (int i = 1; i < len; i++) {

      final char c = k.charAt(i);
      final boolean up = Character.isUpperCase(c);
      final boolean digit = Character.isDigit(c);

      if (up && lastUp > 0) {
        lastUp++;
        continue;
      }

      if (digit && lastDigit) {
        continue;
      }

      if (lastUp > 0 && !up) {

        if (i > 1 && lastUp > 2) {
          sb.append(k.substring(startWord, i - 1));
          sb.append('.');
          startWord = i - 1;
        }

        lastUp = 0;
        continue;
      }

      if (up || digit) {
        sb.append(k.substring(startWord, i));
        sb.append('.');
        startWord = i;

        lastUp = up ? 1 : 0;
        lastDigit = digit;
      }

    }
    sb.append(k.substring(startWord));

    return sb.toString();
  }

  @Override
  public String getName() {
    return NAME_COLLECTOR;
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

    this.RTAOutputDirPath = qc.getBclDir().getAbsolutePath();
  }

  @Override
  public void clear() {

  }
}
