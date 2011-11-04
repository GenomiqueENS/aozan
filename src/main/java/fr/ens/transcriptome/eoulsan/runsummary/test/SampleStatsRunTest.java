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

package fr.ens.transcriptome.eoulsan.runsummary.test;

import fr.ens.transcriptome.aozan.runsummary.ReadSummary;
import fr.ens.transcriptome.aozan.runsummary.ReadsStats;
import fr.ens.transcriptome.aozan.runsummary.RunSummary;
import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;

public abstract class SampleStatsRunTest {

  public RunTestResult test(final RunSummary rs, final int readId,
      final CasavaSample sample) {

    if (rs == null)
      return null;

    final ReadSummary readSummary = rs.getReadSummary(readId);
    if (readSummary == null)
      return null;

    final ReadsStats readStats = readSummary.getSampleReadsStats(sample);

    if (readStats == null)
      return null;

    return test(readStats);
  }

  protected abstract RunTestResult test(final ReadsStats readStats);

}
