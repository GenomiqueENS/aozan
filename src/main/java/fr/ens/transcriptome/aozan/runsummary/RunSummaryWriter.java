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

package fr.ens.transcriptome.aozan.runsummary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.illumina.RTALaneSummary;
import fr.ens.transcriptome.eoulsan.illumina.RTAReadSummary;
import fr.ens.transcriptome.eoulsan.illumina.RunInfo.Read;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;

public class RunSummaryWriter {

  private final OutputStream os;

  public void write(final RunSummary rs) throws IOException {

    final Writer writer = new OutputStreamWriter(this.os);

    final StringBuilder sb = new StringBuilder();
    final int laneCount = rs.getRunInfo().getFlowCellLaneCount();
    final int maxSampleByLane = findMaxSampleByLane(rs);

    boolean first = true;

    for (int i = 1; i <= laneCount; i++) {

      for (Read read : rs.getRunInfo().getReads()) {

        final ReadSummary readSummary = rs.getReadSummary(read.getNumber());
        final RTAReadSummary rtaReadSummary = readSummary.getRTAReadSummary();

        for (RTALaneSummary rtaLaneSummary : rtaReadSummary) {

          if (rtaLaneSummary.getLane() == i) {

            if (first) {

              sb.append(rtaLaneSummary.toHeaderString());
              sb.append('\t');

              sb.append(header("Total reads"));
              sb.append('\t');

              for (int j = 0; j < maxSampleByLane; j++) {
                sb.append("Index " + (j + 1) + " reads");
                sb.append('\t');
              }

              sb.append(header("All indexed reads"));
              sb.append('\t');

              sb.append(header("Unknown index reads"));
              sb.append('\n');
              writer.write(sb.toString());
              sb.setLength(0);
            }

            // RTA summary
            sb.append(rtaLaneSummary.toString());

            if (read.isIndexedRead())
              continue;

            sb.append('\t');
            sb.append(readSummary.getTotalReadsStats(i));
            sb.append('\t');

            int count = 0;
            for (CasavaSample sample : rs.getDesign().getSampleInLane(i)) {
              count++;
              sb.append(readSummary.getSampleReadsStats(sample));
              sb.append('\t');
            }

            for (int j = count; j < maxSampleByLane; j++)
              sb.append("\t\t");

            sb.append(readSummary.getAllIndexedReadsStats(i));
            sb.append('\t');
            sb.append(readSummary.getUnknownIndexReadsStats(i));
            sb.append('\n');

            writer.write(sb.toString());
            sb.setLength(0);
          }

        }

      }

    }

  }

  private static final String header(String name) {

    return name
        + "total\t" + name + "PF\t" + name + "%PF\t" + name + "Q30\t" + name
        + "% Q30";
  }

  private static final int findMaxSampleByLane(final RunSummary rs) {

    final int laneCount = rs.getRunInfo().getFlowCellLaneCount();
    int max = 0;

    for (int i = 1; i <= laneCount; i++)
      max = Math.max(max, rs.getDesign().getSampleInLane(i).size());

    return max;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws EoulsanIOException if an error occurs while reading the file or if
   *           the file is null.
   * @throws FileNotFoundException if the file is not found
   */
  public RunSummaryWriter(final File file) throws EoulsanIOException,
      FileNotFoundException {

    this(new FileOutputStream(file));

  }

  /**
   * Public constructor
   * @param os Input stream to read
   * @throws EoulsanIOException if the stream is null
   */
  public RunSummaryWriter(final OutputStream os) throws EoulsanIOException {

    this.os = os;
  }

}
