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

package fr.ens.transcriptome.aozan.illumina;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeNames;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getElementsByTagName;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getTagValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class handle RTA run info data. It is updated with new data related to
 * RTA version 2.X, published in mars 2015.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class RunInfo {

  private String id;
  private int number;
  private String flowCell;
  private String instrument;
  private String date;
  private final List<Read> reads = new ArrayList<>();

  // FlowcellLayout
  private int flowCellLaneCount;
  private int flowCellSurfaceCount;
  private int flowCellSwathCount;
  private int flowCellTileCount;

  // FlowcellLayout specific to RTA version 2.X
  private int flowCellSectionPerLane = -1;
  private int flowCellLanePerSection = -1;

  private final List<Integer> alignToPhix = new ArrayList<>();

  // Data specific to RTA version 2.X
  private final List<String> imageChannels = new ArrayList<>();

  //
  // Parser
  //

  public void parse(final String filepath) throws ParserConfigurationException,
      SAXException, IOException {

    checkArgument(!Strings.isNullOrEmpty(filepath),
        "RunInfo.xml path");

    parse(new File(filepath));
  }

  /**
   * Parses the run info file
   * @param file the run info file
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void parse(final File file) throws ParserConfigurationException,
      SAXException, IOException {

    checkArgument(file.exists(), "RunInfo.xml not exists");

    parse(FileUtils.createInputStream(file));
  }

  /**
   * Parses the run info file.
   * @param is the input stream on run info file
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void parse(final InputStream is) throws ParserConfigurationException,
      SAXException, IOException {

    final Document doc;

    final DocumentBuilderFactory dbFactory =
        DocumentBuilderFactory.newInstance();
    final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    doc = dBuilder.parse(is);
    doc.getDocumentElement().normalize();

    parse(doc);

    is.close();
  }

  /**
   * Parses the run info file.
   * @param document the document from run info XML file
   */
  private void parse(final Document document) {

    for (Element e : XMLUtils.getElementsByTagName(document, "RunInfo")) {

      for (Element runElement : XMLUtils.getElementsByTagName(e, "Run")) {

        // Parse attribute of the Run tag
        for (String name : XMLUtils.getAttributeNames(runElement)) {

          final String value = runElement.getAttribute(name);

          switch (name) {
          case "Id":
            this.id = value;
            break;
          case "Number":
            this.number = Integer.parseInt(value);
            break;
          }
        }

        this.flowCell = getTagValue(runElement, "Flowcell");
        this.instrument = getTagValue(runElement, "Instrument");
        this.date = getTagValue(runElement, "Date");

        // Extract read data
        extractReadData(runElement);

        // Extract variable from Flowcell element
        extractFlowcellElement(runElement);

        // After extract lane with PhiX
        extractAlignToPhiXElement(runElement);

        // Extract images channel data
        extractImageChannels(runElement);
      }
    }

  }

  /**
   * Extract image channels data.
   * @param runElement the run element
   */
  private void extractImageChannels(final Element runElement) {

    // Check if element name exists
    if (XMLUtils.getElementsByTagName(runElement, "ImageChannels") == null) {

      return;
    }

    // Parse element
    for (Element e2 : getElementsByTagName(runElement, "ImageChannels")) {

      // Extract content on name element
      for (Element name : getElementsByTagName(e2, "Name")) {
        this.imageChannels.add(name.getTextContent());
      }
    }
  }

  /**
   * Extract the read data.
   * @param runElement the run element
   */
  private void extractReadData(final Element runElement) {

    int readCount = 0;
    // Parse Reads tag
    for (Element e2 : XMLUtils.getElementsByTagName(runElement, "Reads")) {
      for (Element e3 : XMLUtils.getElementsByTagName(e2, "Read")) {

        final Read read = new Read();
        readCount++;

        for (String name : XMLUtils.getAttributeNames(e3)) {

          final String value = e3.getAttribute(name);

          switch (name) {
          case "Number":
            read.number = Integer.parseInt(value);
            break;
          case "NumCycles":
            read.numberCycles = Integer.parseInt(value);
            break;
          case "IsIndexedRead":
            read.indexedRead = "Y".equals(value.toUpperCase().trim());
            break;
          }

          if (read.getNumber() == 0) {
            read.number = readCount;
          }
        }

        this.reads.add(read);
      }
    }

  }

  /**
   * Parses the flowcell element to set variable used to compute tile number.
   * @param runElement the element parent.
   */
  private void extractFlowcellElement(final Element runElement) {

    // Parse FlowcellLayout tag
    for (Element e2 : getElementsByTagName(runElement, "FlowcellLayout")) {

      for (String name : getAttributeNames(e2)) {

        final int value = Integer.parseInt(e2.getAttribute(name));

        switch (name) {
        case "LaneCount":
          this.flowCellLaneCount = value;
          break;
        case "SurfaceCount":
          this.flowCellSurfaceCount = value;
          break;
        case "SwathCount":
          this.flowCellSwathCount = value;
          break;
        case "TileCount":
          this.flowCellTileCount = value;
          break;

        // Value specific on RTA version 2.X
        case "SectionPerLane":
          this.flowCellSectionPerLane = value;
          break;
        case "LanePerSection":
          this.flowCellLanePerSection = value;
          break;
        }
      }
    }

  }

  /**
   * Set the lane with Phix. Per default all for a NextSeq, specified for an
   * HiSeq.
   * @param runElement the element parent
   */
  private void extractAlignToPhiXElement(final Element runElement) {

    // Check if element name exists
    if (isElementExistsByTagName(runElement, "AlignToPhiX")) {

      // Element exist
      for (Element e2 : getElementsByTagName(runElement, "AlignToPhiX")) {
        // Parse lane number
        for (Element e3 : getElementsByTagName(e2, "Lane")) {

          this.alignToPhix.add(Integer.parseInt(e3.getTextContent()));
        }
      }

    } else {

      // Not found, aligned to PhiX in all lanes
      for (int lane = 1; lane <= this.flowCellLaneCount; lane++) {

        this.alignToPhix.add(lane);
      }
    }
  }

  /**
   * Gets the Nextseq tiles count per lane (surfaces × swaths × camera segments
   * × tiles per swath per segment).
   * @return the Nextseq tiles count
   */
  private int getNextseqTilesCount() {
    return this.flowCellSurfaceCount
        * this.flowCellSwathCount * this.flowCellTileCount
        * this.flowCellSectionPerLane;
  }

  /**
   * Gets the Hiseq tiles count per lane (surfaces × swaths × tiles per swath
   * per segment).
   * @return the Hiseq tiles count
   */
  private int getHiseqTilesCount() {

    return this.flowCellSurfaceCount
        * this.flowCellSwathCount * this.flowCellTileCount;
  }

  //
  // Getters
  //

  /**
   * Gets the tiles count.
   * @return the tiles count
   */
  public int getTilesCount() {

    if (isHiseqSequencer())
      // Case Hiseq compute tile count
      return getHiseqTilesCount();

    // Case NextSeq compute tile count, add data from camera number
    return getNextseqTilesCount();
  }

  /**
   * Checks if is next seq sequencer.
   * @return true, if is next seq sequencer
   */
  public boolean isNextSeqSequencer() {
    return this.flowCellSectionPerLane > 0;
  }

  /**
   * Checks if is hiseq sequencer.
   * @return true, if is hiseq sequencer
   */
  public boolean isHiseqSequencer() {
    return this.flowCellSectionPerLane == -1;
  }

  /**
   * Gets the sequencer type.
   * @return the sequencer type
   */
  public String getSequencerType() {

    return (isNextSeqSequencer() ? "nextseq" : "hiseq");
  }

  /**
   * @return Returns the id
   */
  public String getId() {
    return this.id;
  }

  /**
   * @return Returns the number
   */
  public int getNumber() {
    return this.number;
  }

  /**
   * @return Returns the flowCell
   */
  public String getFlowCell() {
    return this.flowCell;
  }

  /**
   * @return Returns the instrument
   */
  public String getInstrument() {
    return this.instrument;
  }

  /**
   * @return Returns the date
   */
  public String getDate() {
    return this.date;
  }

  /**
   * @return Returns the reads
   */
  public List<Read> getReads() {
    return Collections.unmodifiableList(this.reads);
  }

  /**
   * @return Returns the flowCellLaneCount
   */
  public int getFlowCellLaneCount() {
    return this.flowCellLaneCount;
  }

  /**
   * @return Returns the flowCellSurfaceCount
   */
  public int getFlowCellSurfaceCount() {
    return this.flowCellSurfaceCount;
  }

  /**
   * @return Returns the flowCellSwathCount
   */
  public int getFlowCellSwathCount() {
    return this.flowCellSwathCount;
  }

  /**
   * @return Returns the flowCellTileCount
   */
  public int getFlowCellTileCount() {
    return this.flowCellTileCount;
  }

  /**
   * @return Returns the flowCellSectionPerLane
   */
  public int getFlowCellSectionPerLane() {
    return this.flowCellSectionPerLane;
  }

  /**
   * @return Returns the flowCellLanePerSection
   */
  public int getFlowCellLanePerSection() {
    return this.flowCellLanePerSection;
  }

  /**
   * @return Returns the alignToPhix
   */
  public List<Integer> getAlignToPhix() {
    return Collections.unmodifiableList(this.alignToPhix);
  }

  /**
   * @return Returns the image channels
   */
  public List<String> getImageChannels() {
    return Collections.unmodifiableList(this.imageChannels);
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{id=" + this.id + ", number=" + this.number + ", flowCell="
        + this.flowCell + ", instrument=" + this.instrument + ", date="
        + this.date + ", reads=" + this.reads + ", flowCellLaneCount="
        + this.flowCellLaneCount + ", flowCellSurfaceCount="
        + this.flowCellSurfaceCount + ", flowCellSwathCount="
        + this.flowCellSwathCount + ", flowCellTileCount="
        + this.flowCellTileCount + ", alignToPhix=" + this.alignToPhix + "}";

  }

  //
  // Internal class
  //

  /**
   * Handle information about a read.
   * @author Laurent Jourdren
   */
  public static class Read {

    private int number;
    private int numberCycles;
    private boolean indexedRead;

    /**
     * @return Returns the number
     */
    public int getNumber() {
      return this.number;
    }

    /**
     * @return Returns the numberCycles
     */
    public int getNumberCycles() {
      return this.numberCycles;
    }

    /**
     * @return Returns the indexedRead
     */
    public boolean isIndexedRead() {
      return this.indexedRead;
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName()
          + "{number=" + this.number + ", numberCycles=" + this.numberCycles
          + ", indexedRead=" + this.indexedRead + "}";
    }

  }

  // TODO to remove used version in next Eoulsan version
  /**
   * Checks if is element exists by tag name.
   * @param element the element
   * @param tagName the tag name
   * @return true, if is element exists by tag name
   */
  public static boolean isElementExistsByTagName(final Element element,
      final String tagName) {

    if (element == null || tagName == null || tagName.isEmpty()) {
      return false;
    }

    // Extract all children on element
    final NodeList res = element.getChildNodes();

    for (int i = 0; i < res.getLength(); i++) {

      final Node node = res.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        final Element elem = (Element) node;

        // Check matching with tagname expected
        if (elem.getTagName().equals(tagName)) {
          return true;
        }
      }
    }

    return false;
  }
}
