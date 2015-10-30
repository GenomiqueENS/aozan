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

package fr.ens.transcriptome.aozan.fastqc;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import uk.ac.babraham.FastQC.Modules.AbstractQCModule;
import uk.ac.babraham.FastQC.Report.HTMLReportArchive;
import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.QualityEncoding.PhredEncoding;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.aozan.illumina.IlluminaReadId;

/**
 * This class define a QCModule for FastQC that list the bad tiles in a fastq
 * file.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class BadTiles extends AbstractQCModule {

  private static final int LANE_OFFSET = 10000;
  private static final double MIN_MEDIAN_SCORE = 20.0;

  // public QualityCount[] qualityCounts = new QualityCount[0];
  private final Map<Integer, QualityCount[]> tiles =
      new HashMap<Integer, QualityCount[]>();
  private final List<BadTile> badTiles = new ArrayList<BadTile>();
  private final Set<Integer> tilesWithOneOrMoreDefect = new HashSet<Integer>();

  private IlluminaReadId irid;
  private boolean calculated = false;

  private static class QualityCount {
    private final HashMap<Character, Long> counts =
        new HashMap<Character, Long>();

    private long totalCounts = 0;

    public void addValue(char c) {
      totalCounts++;
      if (counts.containsKey(c)) {
        counts.put(c, counts.get(c) + 1);
      } else {
        counts.put(c, 1L);
      }
    }

    @SuppressWarnings("unused")
    public long getTotalCount() {
      return totalCounts;
    }

    public char getMinChar() {
      char minChar = 10000;
      for (Character thisChar : counts.keySet()) {
        if (thisChar < minChar)
          minChar = thisChar;
      }

      return minChar;
    }

    public char getMaxChar() {
      char maxChar = 0;

      for (Character thisChar : counts.keySet()) {
        if (thisChar > maxChar)
          maxChar = thisChar;
      }

      return maxChar;

    }

    @SuppressWarnings("unused")
    public double getMean(int offset) {
      long total = 0;
      long count = 0;

      for (Character thisChar : counts.keySet()) {
        total += counts.get(thisChar) * (thisChar - offset);
        count += counts.get(thisChar);
      }

      return ((double) total) / count;
    }

    public double getPercentile(int offset, int percentile) {
      Character[] chars = counts.keySet().toArray(new Character[0]);
      Arrays.sort(chars);
      long total = 0;
      for (Character aChar : chars) {
        total += counts.get(aChar);
      }

      total *= percentile;
      total /= 100;

      long count = 0;
      for (Character aChar : chars) {
        count += counts.get(aChar);
        if (count >= total) {
          return (aChar - offset);
        }
      }

      return -1;

    }

  }

  private class ResultsTable extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public int getColumnCount() {
      return 4;
    }

    public int getRowCount() {
      return badTiles.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

      final BadTile bt = badTiles.get(rowIndex);

      switch (columnIndex) {
      case 0:
        return bt.lane;
      case 1:
        return bt.cycle;
      case 2:
        return bt.tile;
      case 3:
        return bt.medianScore;
      }
      return null;
    }

    public String getColumnName(int columnIndex) {
      switch (columnIndex) {
      case 0:
        return "Lane";
      case 1:
        return "Cycle";
      case 2:
        return "Tile";
      case 3:
        return "Median score";
      }
      return null;
    }

    public Class<?> getColumnClass(int columnIndex) {
      switch (columnIndex) {
      case 0:
        return Integer.class;
      case 1:
        return Integer.class;
      case 2:
        return Integer.class;
      case 3:
        return Double.class;
      }
      return null;

    }
  }

  /**
   * This private class define a tile.
   * @author Laurent Jourdren
   */
  private static final class BadTile implements Comparable<BadTile> {

    private final int lane;
    private final int tile;
    private final int cycle;
    private final double medianScore;

    @Override
    public int compareTo(final BadTile that) {

      if (that == null)
        return 1;

      if (this == that)
        return 0;

      final int r1 = this.lane - that.lane;
      if (r1 != 0)
        return r1;

      final int r2 = this.cycle - that.cycle;
      if (r2 != 0)
        return r2;

      final int r3 = this.tile - that.tile;
      if (r3 != 0)
        return r3;

      return Double.compare(this.medianScore, that.medianScore);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + cycle;
      result = prime * result + lane;
      long temp;
      temp = Double.doubleToLongBits(medianScore);
      result = prime * result + (int) (temp ^ (temp >>> 32));
      result = prime * result + tile;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      BadTile other = (BadTile) obj;
      if (cycle != other.cycle)
        return false;
      if (lane != other.lane)
        return false;
      if (Double.doubleToLongBits(medianScore) != Double
          .doubleToLongBits(other.medianScore))
        return false;
      return tile == other.tile;
    }

    @Override
    public String toString() {

      return "Lane: "
          + this.lane + ", Cycle: " + this.cycle + ", Tile: " + this.tile
          + ", Score: " + medianScore;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param tileId tile id (with lane encoded)
     * @param cycle cycle of the bad tile
     * @param medianScore median score of the tile
     */
    public BadTile(final int tileId, final int cycle, final double medianScore) {

      this.lane = tileId / LANE_OFFSET;
      this.tile = tileId % LANE_OFFSET;
      this.cycle = cycle;
      this.medianScore = medianScore;
    }

  }

  @Override
  public String name() {

    return "Bad tiles";
  }

  @Override
  public String description() {

    return "Bad tiles finder";
  }

  @Override
  public JPanel getResultsPanel() {

    if (!calculated)
      computeResults();

    JPanel returnPanel = new JPanel();
    returnPanel.setLayout(new BorderLayout());
    returnPanel.add(new JLabel("Basic sequence stats", JLabel.CENTER),
        BorderLayout.NORTH);

    TableModel model = new ResultsTable();
    returnPanel.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);

    return returnPanel;
  }

  @Override
  public boolean ignoreFilteredSequences() {

    return true;
  }

  @Override
  public void makeReport(HTMLReportArchive report) throws IOException,
      XMLStreamException {

    if (!calculated)
      computeResults();

    ResultsTable table = new ResultsTable();

    // Check bad tiles found
    if (this.badTiles.size() == 0) {
      XMLStreamWriter w = report.xhtmlStream();
      w.writeStartElement("p");
      w.writeCharacters("No bad tiles");
      w.writeEndElement();
    }

    else {
      super.writeTable(report, table);
    }

  }

  @Override
  public boolean raisesError() {

    if (!this.calculated)
      computeResults();

    if (this.badTiles.size() > 0) {

      final double ratio =
          (double) this.tilesWithOneOrMoreDefect.size()
              / (double) this.tiles.size();

      return ratio > 0.25;

    }
    return false;
  }

  @Override
  public boolean raisesWarning() {

    if (!this.calculated)
      computeResults();

    return this.badTiles.size() > 0;
  }

  @Override
  public void reset() {

    this.tiles.clear();
    this.badTiles.clear();
    this.tilesWithOneOrMoreDefect.clear();
    this.irid = null;
    this.calculated = false;
  }

  @Override
  public void processSequence(final Sequence sequence) {

    this.calculated = false;

    // Parse sequence id
    try {
      if (this.irid == null)
        this.irid = new IlluminaReadId(sequence.getID().substring(1));
      else
        this.irid.parse(sequence.getID().substring(1));
    } catch (EoulsanException e) {

      // This is not an illumina id
      return;
    }

    // Encode in a integer the lane number and the tile number
    final int tileId =
        this.irid.getFlowCellLane()
            * LANE_OFFSET + this.irid.getTileNumberInFlowCellLane();

    // Get the quality of each cycle in an array
    final char[] qual = sequence.getQualityString().toCharArray();

    final QualityCount[] qualityCounts;

    // Get the the quality score object for the tileId from this.tiles
    if (this.tiles.containsKey(tileId)) {

      final QualityCount[] qc = this.tiles.get(tileId);
      if (qc.length < qual.length) {

        qualityCounts = new QualityCount[qual.length];
        System.arraycopy(qc, 0, qualityCounts, 0, qc.length);
        for (int i = qc.length; i < qualityCounts.length; i++)
          qualityCounts[i] = new QualityCount();

      } else {
        qualityCounts = qc;
      }

    } else {
      qualityCounts = new QualityCount[qual.length];
      this.tiles.put(tileId, qualityCounts);
      for (int i = 0; i < qualityCounts.length; i++)
        qualityCounts[i] = new QualityCount();
    }

    // The quality score of each score to qualityCounts
    for (int i = 0; i < qual.length; i++) {
      qualityCounts[i].addValue(qual[i]);
    }

  }

  private static char[] calculateOffsets(final QualityCount[] qualityCounts) {
    // Works out from the set of chars what is the most
    // likely encoding scale for this file.

    char minChar = 0;
    char maxChar = 0;

    for (int q = 0; q < qualityCounts.length; q++) {
      if (q == 0) {
        minChar = qualityCounts[q].getMinChar();
        maxChar = qualityCounts[q].getMaxChar();
      } else {
        if (qualityCounts[q].getMinChar() < minChar) {
          minChar = qualityCounts[q].getMinChar();
        }
        if (qualityCounts[q].getMaxChar() > maxChar) {
          maxChar = qualityCounts[q].getMaxChar();
        }
      }
    }

    return new char[] {minChar, maxChar};
  }

  /**
   * Compute the result.
   */
  private synchronized void computeResults() {

    PhredEncoding encodingScheme = null;

    // for each tile found
    for (Map.Entry<Integer, QualityCount[]> e : this.tiles.entrySet()) {

      final int tileId = e.getKey();
      final QualityCount[] qualityCounts = e.getValue();

      final char[] range = calculateOffsets(qualityCounts);

      // Get the encoding
      if (encodingScheme == null)
        encodingScheme = PhredEncoding.getFastQEncodingOffset(range[0]);

      // Get the offset
      final int offset = encodingScheme.offset();

      // Compute the median for cycles of the tile
      for (int i = 0; i < qualityCounts.length; i++) {

        final double median = qualityCounts[i].getPercentile(offset, 50);

        // Test if the median quality score of the tile is bad
        if (median < MIN_MEDIAN_SCORE) {

          badTiles.add(new BadTile(tileId, i + 1, median));
          this.tilesWithOneOrMoreDefect.add(tileId);
        }

      }
    }

    // Sort bad tiles
    Collections.sort(this.badTiles);

    this.calculated = true;
  }

  @Override
  public boolean ignoreInReport() {
    return false;
  }

}
