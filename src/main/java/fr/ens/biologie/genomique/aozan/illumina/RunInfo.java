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

package fr.ens.biologie.genomique.aozan.illumina;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.biologie.genomique.eoulsan.util.XMLUtils.getAttributeNames;
import static fr.ens.biologie.genomique.eoulsan.util.XMLUtils.getElementsByTagName;
import static fr.ens.biologie.genomique.eoulsan.util.XMLUtils.getTagValue;
import static java.util.Objects.requireNonNull;

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

import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.XMLUtils;

/**
 * This class handle RTA run info data. It is updated with new data related to
 * RTA version 2.X, published in mars 2015.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class RunInfo {

  private final String id;
  private final int number;
  private final String flowCell;
  private final String instrument;
  private final String date;
  private final List<Read> reads;

  private final FlowCellLayout flowCellLayout;

  private final List<Integer> alignToPhix;

  // Data specific to RTA version 2.X
  private final List<String> imageChannels;

  //
  // Internal classes
  //

  /**
   * Handle information about a read.
   * @author Laurent Jourdren
   */
  public static class Read {

    private final int number;
    private final int numberCycles;
    private final boolean indexedRead;

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

    //
    // Constructor
    //

    private Read(final int number, final int numberCycles,
        final boolean indexedRead) {

      this.number = number;
      this.numberCycles = numberCycles;
      this.indexedRead = indexedRead;
    }

  }

  /*
   * TODO Merge getNextseqTilesCount() and getHiseqTilesCount()
   */
  private static class FlowCellLayout {

    private final int laneCount;
    private final int surfaceCount;
    private final int swathCount;
    private final int tileCount;

    // FlowcellLayout specific to RTA version 2.X
    private final int sectionPerLane;
    private final int lanePerSection;

    /**
     * Gets the Nextseq tiles count per lane (surfaces × swaths × camera
     * segments × tiles per swath per segment).
     * @return the Nextseq tiles count
     */
    private int getNextseqTilesCount() {
      return this.surfaceCount
          * this.swathCount * this.tileCount * this.sectionPerLane;
    }

    /**
     * Gets the Hiseq tiles count per lane (surfaces × swaths × tiles per swath
     * per segment).
     * @return the Hiseq tiles count
     */
    private int getHiseqTilesCount() {

      return this.surfaceCount * this.swathCount * this.tileCount;
    }

    //
    // Constructor
    //

    private FlowCellLayout(final int laneCount, final int surfaceCount,
        final int swathCount, final int tileCount, final int sectionPerLane,
        final int lanePerSection) {

      checkArgument(laneCount > 0,
          "laneCount has not been defined in RunInfo.xml");
      checkArgument(surfaceCount > 0,
          "surfaceCount has not been defined in RunInfo.xml");
      checkArgument(swathCount > 0,
          "swathCount has not been defined in RunInfo.xml");
      checkArgument(tileCount > 0,
          "tileCount has not been defined in RunInfo.xml");

      this.laneCount = laneCount;
      this.surfaceCount = surfaceCount;
      this.swathCount = swathCount;
      this.tileCount = tileCount;

      this.sectionPerLane = sectionPerLane;
      this.lanePerSection = lanePerSection;
    }
  }

  //
  // Parsers
  //

  /**
   * Parses the run info file.
   * @param filepath the path to the run info file
   * @return a RunInfo object with the information of the parsed file
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static RunInfo parse(final String filepath)
      throws ParserConfigurationException, SAXException, IOException {

    requireNonNull(filepath, "RunInfo.xml path cannot be null");

    return parse(new File(filepath));
  }

  /**
   * Parses the run info file.
   * @param file the run info file
   * @return a RunInfo object with the information of the parsed file
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static RunInfo parse(final File file)
      throws ParserConfigurationException, SAXException, IOException {

    requireNonNull(file, "RunInfo.xml file cannot be null");

    checkArgument(file.isFile(),
        "RunInfo.xml does not exists or is not a file");

    return parse(FileUtils.createInputStream(file));
  }

  /**
   * Parses the run info file.
   * @param is the input stream on run info file
   * @return a RunInfo object with the information of the parsed file
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException the SAX exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static RunInfo parse(final InputStream is)
      throws ParserConfigurationException, SAXException, IOException {

    requireNonNull(is, "RunInfo.xml input stream cannot be null");

    try (InputStream in = is) {

      final Document doc;

      final DocumentBuilderFactory dbFactory =
          DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(in);
      doc.getDocumentElement().normalize();

      return parse(doc);
    }
  }

  /**
   * Parses the run info file.
   * @param document the document from run info XML file
   */
  private static RunInfo parse(final Document document) {

    for (Element e : XMLUtils.getElementsByTagName(document, "RunInfo")) {

      final List<Element> elements = XMLUtils.getElementsByTagName(e, "Run");
      if (!elements.isEmpty()) {

        final Element runElement = elements.get(0);
        String id = null;
        int number = -1;

        // Parse attribute of the Run tag
        for (String name : XMLUtils.getAttributeNames(runElement)) {

          final String value = runElement.getAttribute(name);

          switch (name) {
          case "Id":
            id = value;
            break;
          case "Number":
            number = Integer.parseInt(value);
            break;
          default:
            throw new AozanRuntimeException(
                "in RunInfo unvalid value for name attribute on run tag.");
          }
        }

        final String flowCell = getTagValue(runElement, "Flowcell");
        final String instrument = getTagValue(runElement, "Instrument");
        final String date = getTagValue(runElement, "Date");

        // Extract read data
        final List<Read> readList = extractReadData(runElement);

        // Extract variable from Flowcell element
        final FlowCellLayout layout = extractFlowcellElement(runElement);

        // After extract lane with PhiX
        final List<Integer> lanesWithPhiX =
            extractAlignToPhiXElement(runElement, layout.laneCount);

        // Extract images channel data
        final List<String> imageChannels = extractImageChannels(runElement);

        return new RunInfo(id, number, flowCell, instrument, date, readList,
            layout, lanesWithPhiX, imageChannels);
      }
    }

    throw new AozanRuntimeException("No \"Run\" tag found in RunInfo.xml");
  }

  /**
   * Extract image channels data.
   * @param runElement the run element
   * @return a list
   */
  private static List<String> extractImageChannels(final Element runElement) {

    final List<String> result = new ArrayList<>();

    // Check if element name exists
    if (XMLUtils.getElementsByTagName(runElement, "ImageChannels") == null) {

      return result;
    }

    // Parse element
    for (Element e2 : getElementsByTagName(runElement, "ImageChannels")) {

      // Extract content on name element
      for (Element name : getElementsByTagName(e2, "Name")) {
        result.add(name.getTextContent());
      }
    }

    return result;
  }

  /**
   * Extract the read data.
   * @param runElement the run element
   */
  private static List<Read> extractReadData(final Element runElement) {

    final List<Read> result = new ArrayList<>();

    int readCount = 0;
    // Parse Reads tag
    for (Element e2 : XMLUtils.getElementsByTagName(runElement, "Reads")) {
      for (Element e3 : XMLUtils.getElementsByTagName(e2, "Read")) {

        int readNumber = 0;
        int readNumberCycles = 0;
        boolean readIndexedRead = false;

        readCount++;

        for (String name : XMLUtils.getAttributeNames(e3)) {

          final String value = e3.getAttribute(name);

          switch (name) {
          case "Number":
            readNumber = Integer.parseInt(value);
            break;
          case "NumCycles":
            readNumberCycles = Integer.parseInt(value);
            break;
          case "IsIndexedRead":
            readIndexedRead = "Y".equals(value.toUpperCase().trim());
            break;
          default:
            throw new AozanRuntimeException(
                "in RunInfo unvalid value for name attribute on read tag.");

          }
        }

        final Read read = new Read(readNumber == 0 ? readCount : readNumber,
            readNumberCycles, readIndexedRead);

        result.add(read);
      }
    }

    return result;
  }

  /**
   * Parses the flowcell element to set variable used to compute tile number.
   * @param runElement the element parent.
   */
  private static FlowCellLayout extractFlowcellElement(
      final Element runElement) {

    int laneCount = -1;
    int surfaceCount = -1;
    int swathCount = -1;
    int tileCount = -1;

    // FlowcellLayout specific to RTA version 2.X
    int sectionPerLane = -1;
    int lanePerSection = -1;

    // Parse FlowcellLayout tag
    for (Element e2 : getElementsByTagName(runElement, "FlowcellLayout")) {

      for (String name : getAttributeNames(e2)) {

        final int value = Integer.parseInt(e2.getAttribute(name));

        switch (name) {
        case "LaneCount":
          laneCount = value;
          break;
        case "SurfaceCount":
          surfaceCount = value;
          break;
        case "SwathCount":
          swathCount = value;
          break;
        case "TileCount":
          tileCount = value;
          break;

        // Value specific on RTA version 2.X
        case "SectionPerLane":
          sectionPerLane = value;
          break;
        case "LanePerSection":
          lanePerSection = value;
          break;
        default:
          throw new AozanRuntimeException(
              "in RunInfo unvalid value for name attribute FlowcellLayout tag.");

        }
      }
    }

    return new FlowCellLayout(laneCount, surfaceCount, swathCount, tileCount,
        sectionPerLane, lanePerSection);
  }

  /**
   * Set the lane with Phix. Per default all for a NextSeq, specified for an
   * HiSeq.
   * @param runElement the element parent
   * @return a list with the lane number with PhiX
   */
  private static List<Integer> extractAlignToPhiXElement(
      final Element runElement, final int flowCellLaneCount) {

    final List<Integer> result = new ArrayList<>();

    // Check if element name exists
    if (isElementExistsByTagName(runElement, "AlignToPhiX")) {

      // Element exist
      for (Element e2 : getElementsByTagName(runElement, "AlignToPhiX")) {
        // Parse lane number
        for (Element e3 : getElementsByTagName(e2, "Lane")) {

          result.add(Integer.parseInt(e3.getTextContent()));
        }
      }

    } else {

      // Not found, aligned to PhiX in all lanes
      for (int lane = 1; lane <= flowCellLaneCount; lane++) {

        result.add(lane);
      }
    }

    return result;
  }

  //
  // Getters
  //

  /**
   * Gets the tiles count.
   * @return the tiles count
   */
  public int getTilesCount() {

    if (this.flowCellLayout.sectionPerLane > 0) {

      // Case NextSeq compute tile count, add data from camera number
      return this.flowCellLayout.getNextseqTilesCount();
    }

    // Case Hiseq compute tile count
    return this.flowCellLayout.getHiseqTilesCount();
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
    return this.flowCellLayout.laneCount;
  }

  /**
   * @return Returns the flowCellSurfaceCount
   */
  public int getFlowCellSurfaceCount() {
    return this.flowCellLayout.surfaceCount;
  }

  /**
   * @return Returns the flowCellSwathCount
   */
  public int getFlowCellSwathCount() {
    return this.flowCellLayout.swathCount;
  }

  /**
   * @return Returns the flowCellTileCount
   */
  public int getFlowCellTileCount() {
    return this.flowCellLayout.tileCount;
  }

  /**
   * @return Returns the flowCellSectionPerLane
   */
  public int getFlowCellSectionPerLane() {
    return this.flowCellLayout.sectionPerLane;
  }

  /**
   * @return Returns the flowCellLanePerSection
   */
  public int getFlowCellLanePerSection() {
    return this.flowCellLayout.lanePerSection;
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
        + this.flowCellLayout.laneCount + ", flowCellSurfaceCount="
        + this.flowCellLayout.surfaceCount + ", flowCellSwathCount="
        + this.flowCellLayout.swathCount + ", flowCellTileCount="
        + this.flowCellLayout.tileCount + ", alignToPhix=" + this.alignToPhix
        + "}";

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

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private RunInfo(final String id, final int number, final String flowCell,
      final String instrument, final String date, final List<Read> readList,
      final FlowCellLayout layout, final List<Integer> alignToPhiX,
      final List<String> imageChannels) {

    requireNonNull(id, "Run id argument cannot be null");
    checkArgument(number > 0, "Run number must be greater than 0: " + number);
    requireNonNull(flowCell, "Flowcell id argument cannot be null");
    requireNonNull(instrument, "Instrument id argument cannot be null");
    requireNonNull(date, "date argument cannot be null");
    requireNonNull(readList, "readList argument cannot be null");
    requireNonNull(layout, "layout argument cannot be null");
    requireNonNull(alignToPhiX, "alignToPhiX argument cannot be null");
    requireNonNull(imageChannels, "imageChannels argument cannot be null");

    this.id = id;
    this.number = number;
    this.flowCell = flowCell;
    this.instrument = instrument;
    this.date = date;
    this.reads = readList;
    this.flowCellLayout = layout;
    this.alignToPhix = alignToPhiX;
    this.imageChannels = imageChannels;

  }

}
