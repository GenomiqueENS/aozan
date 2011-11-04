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

import static fr.ens.transcriptome.eoulsan.util.Utils.newArrayList;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import fr.ens.transcriptome.aozan.runsummary.ReadSummary;
import fr.ens.transcriptome.aozan.runsummary.RunSummary;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.RunInfo;
import fr.ens.transcriptome.eoulsan.illumina.io.CasavaDesignCSVReader;

public class RunSummaryImpl implements RunSummary {

  private CasavaDesign design;
  private RunInfo runInfo;
  private List<ReadSummary> readsSummary = newArrayList();

  public CasavaDesign getDesign() {

    return this.design;
  }

  public RunInfo getRunInfo() {

    return this.runInfo;
  }

  public ReadSummary getReadSummary(final int readId) {

    return this.readsSummary.get(readId);
  }

  //
  // Constructor
  //

  public RunSummaryImpl(final File casavaDesign,
      final File casavaOutputDir, final File RTAOutputDir)
      throws IOException, ParserConfigurationException, SAXException,
      EoulsanException, BadBioEntryException {

    // Read Casava design file
    this.design = new CasavaDesignCSVReader(casavaDesign).read();

    // Parse runInfo
    this.runInfo = new RunInfo();
    runInfo.parse(new File(casavaOutputDir, "RunInfo.xml"));

    for (RunInfo.Read read : runInfo.getReads()) {

      final int readId = read.getNumber();

      final ReadSummary rs =
          new ReadSummaryImpl(this, readId, read.isIndexedRead(), new File(
              RTAOutputDir, "Data/reports/Summary/read" + readId + ".xml"),
              casavaOutputDir);
      this.readsSummary.add(rs);

    }

  }

}
