/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.aozan.runsummary.impl;

import static fr.ens.transcriptome.eoulsan.util.Utils.newHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import fr.ens.transcriptome.aozan.runsummary.ReadSummary;
import fr.ens.transcriptome.aozan.runsummary.ReadsStats;
import fr.ens.transcriptome.aozan.runsummary.RunSummary;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.illumina.RTAReadSummary;

public class ReadSummaryImpl implements ReadSummary {

  private final RunSummary parent;
  private final int readId;
  private final boolean indexread;
  private final RTAReadSummary rtaReadSummary;
  private final Map<Integer, ReadsStats> totalReadsStats = newHashMap();
  private final Map<Integer, ReadsStats> allIndexedReadsStats = newHashMap();
  private final Map<Integer, ReadsStats> unknownIndexReadsStats = newHashMap();
  private final Map<CasavaSample, ReadsStats> indexedReadsStats = newHashMap();

  //
  // Getters
  //

  @Override
  public int getReadId() {

    return this.readId;
  }

  @Override
  public boolean isIndexread() {

    return this.indexread;
  }

  @Override
  public RTAReadSummary getRTAReadSummary() {

    return this.rtaReadSummary;
  }

  @Override
  public ReadsStats getTotalReadsStats(final int laneId) {

    return this.totalReadsStats.get(laneId);
  }

  @Override
  public ReadsStats getAllIndexedReadsStats(final int laneId) {

    return this.allIndexedReadsStats.get(laneId);
  }

  @Override
  public ReadsStats getUnknownIndexReadsStats(final int laneId) {

    return this.unknownIndexReadsStats.get(laneId);
  }

  @Override
  public ReadsStats getSampleReadsStats(final CasavaSample sample) {

    return this.indexedReadsStats.get(sample);
  }

  //
  // Constructor
  //

  ReadSummaryImpl(final RunSummary parent, final int readId,
      final boolean indexRead, final File RTAReadSummaryFile,
      final File casavaOutputDir) throws ParserConfigurationException,
      SAXException, IOException, EoulsanException, BadBioEntryException {

    this.parent = parent;
    this.readId = readId;
    this.indexread = indexRead;
    final int laneCount = this.parent.getRunInfo().getFlowCellLaneCount();

    // Read and parse RTA Summary
    this.rtaReadSummary = new RTAReadSummary();
    this.rtaReadSummary.parse(RTAReadSummaryFile);

    // Generate reads statistics of each lanes
    for (int i = 1; i <= laneCount; i++) {

      final ReadsLaneStatsGenerator gen =
          new ReadsLaneStatsGenerator(parent.getDesign(), laneCount,
              this.readId, casavaOutputDir);

      this.totalReadsStats.put(i, gen.getTotalStats());
      this.allIndexedReadsStats.put(i, gen.getAllIndexedStats());
      this.unknownIndexReadsStats.put(i, gen.getUnknownIndexedStats());

      for (CasavaSample sample : this.parent.getDesign().getSampleInLane(i))
        this.indexedReadsStats.put(sample, gen.getSampleStats(sample));
    }

  }

}
