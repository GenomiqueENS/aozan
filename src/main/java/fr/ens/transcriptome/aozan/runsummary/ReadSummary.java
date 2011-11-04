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

import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.illumina.RTAReadSummary;

public interface ReadSummary {

  /**
   * Get the read id
   * @return Returns the readId
   */
  int getReadId();

  /**
   * Get the index read
   * @return Returns the indexread
   */
  boolean isIndexread();

  /**
   * Get the RTA read summary.
   * @return the RTAReadSummary
   */
  RTAReadSummary getRTAReadSummary();

  /**
   * Get the total reads statistics for a lane.
   * @param laneId the identifier of the lane (usually 1 to 8)
   * @return a ReadsStats object or null if the lane does not exists
   */
  ReadsStats getTotalReadsStats(final int laneId);

  /**
   * Get the all indexed reads statistics for a lane.
   * @param laneId the identifier of the lane (usually 1 to 8)
   * @return a ReadsStats object or null if the lane does not exists
   */
  ReadsStats getAllIndexedReadsStats(final int laneId);

  /**
   * Get the unknown reads statistics for a lane.
   * @param laneId the identifier of the lane (usually 1 to 8)
   * @return a ReadsStats object or null if the lane does not exists
   */
  ReadsStats getUnknownIndexReadsStats(final int laneId);

  /**
   * Get the reads statistics for a sample.
   * @param sample the sample
   * @return a ReadsStats object or null if the sample does not exists
   */
  ReadsStats getSampleReadsStats(final CasavaSample sample);

}
