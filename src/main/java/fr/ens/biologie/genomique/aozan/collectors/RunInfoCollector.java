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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.aozan.Aozan2Logger;
import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.kenetre.illumina.RunInfo;
import fr.ens.biologie.genomique.kenetre.illumina.RunParameters;

/**
 * This collector collect data from the RunInfo.xml file, working with all RTA
 * versions.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class RunInfoCollector implements Collector {

  private static final Logger LOGGER = Aozan2Logger.getLogger();

  /** The collector name. */
  public static final String COLLECTOR_NAME = "runinfo";

  /** Prefix for run data */
  public static final String PREFIX = "run.info";

  private File runInfoFile;
  private File runParametersFile;
  private Settings settings;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(AozanCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final QC qc, final CollectorConfiguration conf) {

    if (conf == null)
      return;

    // Fix for unit Test
    if (qc != null) {
      this.settings = qc.getSettings();
    }

    this.runInfoFile = runInfoFilePath(qc, conf);
    this.runParametersFile = runParametersFile(qc, conf);
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null)
      return;

    LOGGER.fine("RunInfo file: " + this.runInfoFile);
    LOGGER.fine("RunParameters file: " + this.runParametersFile);

    try {
      // Parse run info file
      final RunInfo runInfo = RunInfo.parse(this.runInfoFile);
      final RunParameters runParameters =
          RunParameters.parse(this.runParametersFile);

      data.put(PREFIX + ".run.id", runInfo.getId());
      data.put(PREFIX + ".run.number", runInfo.getNumber());
      data.put(PREFIX + ".sequencer.family",
          runParameters.getSequencerFamily());
      data.put(PREFIX + ".instrument", runInfo.getInstrument());
      data.put(PREFIX + ".sequencer.name",
          getSequencerName(runInfo, runParameters));

      data.put(PREFIX + ".application.name",
          runParameters.getApplicationName());
      data.put(PREFIX + ".application.version",
          runParameters.getApplicationVersion());
      data.put(PREFIX + ".rta.version", runParameters.getRTAVersion());
      data.put(PREFIX + ".rta.major.version",
          runParameters.getRTAMajorVersion());

      data.put(PREFIX + ".flow.cell.id", runInfo.getFlowCell());
      data.put(PREFIX + ".flow.cell.lane.count",
          runInfo.getFlowCellLaneCount());
      data.put(PREFIX + ".flow.cell.surface.count",
          runInfo.getFlowCellSurfaceCount());
      data.put(PREFIX + ".flow.cell.swath.count",
          runInfo.getFlowCellSwathCount());
      data.put(PREFIX + ".flow.cell.tile.count",
          runInfo.getFlowCellTileCount());

      data.put(PREFIX + ".date", dateToYYMMDD(runInfo.getDate()));

      // Value specific on RTA version 1.X otherwise value is -1
      data.put(PREFIX + ".flow.cell.lane.per.section",
          runInfo.getFlowCellLanePerSection());
      // Value specific on RTA version 2.X otherwise value is -1
      data.put(PREFIX + ".flow.cell.section.per.lane",
          runInfo.getFlowCellSectionPerLane());

      data.put(PREFIX + ".read.count", runInfo.getReads().size());

      data.put(PREFIX + ".image.channels",
          Joiner.on(",").join(runInfo.getImageChannels()));

      data.put(PREFIX + ".tiles.per.lane.count", runInfo.getTilesCount());

      int readIndexedCount = 0;

      for (RunInfo.Read read : runInfo.getReads()) {
        data.put(PREFIX + ".read" + read.getNumber() + ".cycles",
            read.getNumberCycles());

        data.put(PREFIX + ".read" + read.getNumber() + ".indexed",
            read.isIndexedRead());

        if (!read.isIndexedRead()) {
          readIndexedCount++;
        }
      }
      data.put(PREFIX + ".indexed.read.count", readIndexedCount);

      final Set<Integer> lanesToAlign =
          Sets.newHashSet(runInfo.getAlignToPhix());
      for (int i = 1; i <= runInfo.getFlowCellLaneCount(); i++)
        data.put(PREFIX + ".align.to.phix.lane" + i, lanesToAlign.contains(i));

      // Add new entry in data : run mode
      data.put(PREFIX + ".run.mode", (readIndexedCount == 1
          ? "SR" : (readIndexedCount == 2 ? "PE" : "other")));

    } catch (IOException | ParserConfigurationException | SAXException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Get the sequencer name.
   * @param runInfo the RunInfo object
   * @param runParameters the RunParameter object
   * @return the name of the sequencer
   */
  private String getSequencerName(final RunInfo runInfo,
      final RunParameters runParameters) {

    final String key = "sequencer.name." + runInfo.getInstrument();

    if (this.settings != null && this.settings.containsKey(key)) {
      return this.settings.get(key);
    }

    return runParameters.getSequencerFamily() + " " + runInfo.getInstrument();
  }

  @Override
  public void clear() {
  }

  @Override
  public boolean isSummaryCollector() {
    return false;
  }

  private File runInfoFilePath(final QC qc, final CollectorConfiguration conf) {

    final File parentFile;

    if (qc == null) {
      // Unit Test
      parentFile = conf.getFile(QC.RTA_OUTPUT_DIR);
    } else {
      parentFile = qc.getBclDir();
      this.settings = qc.getSettings();
    }

    return new File(parentFile, "RunInfo.xml");
  }

  private File runParametersFile(final QC qc,
      final CollectorConfiguration conf) {

    final File parentFile;

    if (qc == null) {
      // Unit Test
      parentFile = conf.getFile(QC.RTA_OUTPUT_DIR);
    } else {
      parentFile = qc.getBclDir();
      this.settings = qc.getSettings();
    }

    File result = new File(parentFile, "runParameters.xml");

    return result.exists() ? result : new File(parentFile, "RunParameters.xml");
  }

  /**
   * Convert a date in ISO format to original Illumina date format.
   * @param s Date in a String to convert
   * @return a String with the date in the old format
   */
  private static String dateToYYMMDD(String s) {

    requireNonNull(s);

    if (!s.endsWith("Z")) {
      return s;
    }

    try {
      DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      Date d = df1.parse(s);
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(d);
      return String.format("%02d%02d%02d", calendar.get(Calendar.YEAR) - 2000,
          calendar.get(Calendar.MONTH) + 1,
          calendar.get(Calendar.DAY_OF_MONTH));
    } catch (ParseException e) {
      return s;
    }

  }

}
