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

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.runsummary.ReadsStats;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.bio.io.FastqWriter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.IlluminaFilterFlagReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.ReadFilter;
import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.util.StatUtils;

public final class ReadsLaneStatsGenerator {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final int lane;
  private final int read;

  private List<CasavaSample> samples = Lists.newArrayList();
  private String fastqExtension = ".fastq.bz2";

  private final ReadsStatsImpl totalStats = new ReadsStatsImpl(null);
  private final Map<CasavaSample, ReadsStatsImpl> indexedStats =
      new LinkedHashMap<CasavaSample, ReadsStatsImpl>();
  private final ReadsStatsImpl allIndexedStats = new ReadsStatsImpl(
      "All indexes");;
  private final ReadsStatsImpl unknownIndexStats =
      new ReadsStatsImpl("Unknown");

  public ReadsStats getTotalStats() {
    return totalStats;
  }

  public ReadsStats getAllIndexedStats() {
    return allIndexedStats;
  }

  public ReadsStats getUnknownIndexedStats() {
    return unknownIndexStats;
  }

  public ReadsStats getSampleStats(final CasavaSample sample) {

    return this.indexedStats.get(sample);
  }

  public int sampleCount() {
    return samples.size();
  }

  private void addSamples(final CasavaDesign design) {

    if (design == null)
      return;

    for (CasavaSample sample : design)
      if (sample.getLane() == this.lane)
        samples.add(sample);
  }

  public void countReads(final File fastqDir) throws EoulsanException,
      IOException, BadBioEntryException {

    // Count reads with known index

    for (int i = 0; i < sampleCount(); i++) {

      final CasavaSample sample = this.samples.get(i);

      final File dir =
          new File(fastqDir, "Project_"
              + sample.getSampleProject() + "/Sample_" + sample.getSampleId());

      final List<File> files =
          findFiles(dir, sample.getDemultiplexedFilenamePrefix(this.read),
              this.fastqExtension);

      // Create a temporary filtered fastq file
      final File fastqTmp =
          EoulsanRuntime.getRuntime().createTempFile("filtered_fastq-", ".fq");

      final ReadsStatsImpl result = new ReadsStatsImpl("Index " + (i + 1));
      this.indexedStats.put(sample, result);

      final int[] resultCount = countReads(files, fastqTmp);

      result.total = resultCount[0];
      result.passingFilters = resultCount[1];
      result.q30 = resultCount[2];

      this.allIndexedStats.add(result);

      if (!fastqTmp.delete())
        LOGGER.warning("Unable to remove temporary file: " + fastqTmp);
    }

    // Count reads with unknown index
    final List<File> files =
        findFiles(fastqDir, "Undetermined_indices/Sample_lane"
            + this.samples.get(0).getLane() + "/"
            + this.samples.get(0).getNotDemultiplexedFilenamePrefix(1),
            this.fastqExtension);

    if (files.size() > 0) {

      final File fastqTmp =
          EoulsanRuntime.getRuntime().createTempFile("filtered_fastq-", ".fq");

      final int[] resultCount =
          countReads(files, new File(fastqTmp.getAbsolutePath()));

      this.unknownIndexStats.total = resultCount[0];
      this.unknownIndexStats.passingFilters = resultCount[1];
      this.unknownIndexStats.q30 = resultCount[2];

      if (!fastqTmp.delete())
        LOGGER.warning("Unable to remove temporary file: " + fastqTmp);
    }

    this.totalStats.add(this.allIndexedStats);
    this.totalStats.add(this.unknownIndexStats);
  }

  private static final List<File> findFiles(final File parentDir,
      final String prefix, final String suffix) {

    final List<File> result = Lists.newArrayList();

    int count = 1;
    boolean exists = false;

    do {

      final File f =
          new File(parentDir, prefix + String.format("%03d", count) + suffix);
      System.out.println("Look for: " + f);
      exists = f.exists();

      if (exists)
        result.add(f);

      count++;

    } while (exists);

    return result;
  }

  private static final int[] countReads(final List<File> files,
      final File fastqOutput) throws EoulsanException, IOException,
      BadBioEntryException {

    if (files == null)
      return null;

    int reads = 0;
    int accepted = 0;
    int q30 = 0;

    final ReadFilter filter = new IlluminaFilterFlagReadFilter();

    final FastqWriter writer = new FastqWriter(fastqOutput);

    for (File f : files) {

      final FastqReader reader = new FastqReader(f);

      for (final ReadSequence read : reader) {

        reads++;

        if (filter.accept(read)) {
          accepted++;
          writer.write(read);
        }

        if (StatUtils.mean(read.qualityScores()) >= 30)
          q30++;

      }

      reader.close();
    }

    writer.close();

    return new int[] {reads, accepted, q30};
  }

  public String toHeaderString(final int maxSamples) {

    final StringBuilder sb = new StringBuilder();

    sb.append("Lane\t");

    sb.append(this.totalStats.header());
    sb.append('\t');

    for (ReadsStatsImpl rs : this.indexedStats.values()) {
      sb.append(rs.header());
      sb.append('\t');
    }

    sb.append(this.allIndexedStats.header());
    sb.append('\t');

    sb.append(this.unknownIndexStats.header());

    return sb.toString();
  }

  public String toString(final int maxSamples) {

    final StringBuilder sb = new StringBuilder();
    sb.append(this.lane);
    sb.append('\t');

    sb.append(this.totalStats.values());
    sb.append('\t');

    for (ReadsStatsImpl rs : this.indexedStats.values()) {
      sb.append(rs.values());
      sb.append('\t');
    }

    sb.append(this.allIndexedStats.values());
    sb.append('\t');

    sb.append(this.unknownIndexStats.values());

    return sb.toString();
  }

  //
  // Constructor
  //

  public ReadsLaneStatsGenerator(final CasavaDesign design, final int lane,
      final int read, final File casavaOutputDir) throws EoulsanException,
      IOException, BadBioEntryException {

    this.lane = lane;
    this.read = read;
    addSamples(design);
    countReads(casavaOutputDir);
  }

}