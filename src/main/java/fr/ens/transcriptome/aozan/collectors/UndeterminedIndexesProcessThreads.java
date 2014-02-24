package fr.ens.transcriptome.aozan.collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.IlluminaReadId;

/**
 * This class allow to process the undetermined fastq file of a lane to extract
 * indices that can be recovered.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class UndeterminedIndexesProcessThreads extends
    AbstractFastqProcessThread {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  private final RunData data;
  private final int lane;
  private final int read;
  private final SequenceFile seqFile;
  private final int maxMismatches;
  private final File reportDir;

  private final Map<String, String> sampleIndexes;
  private Multiset<String> rawUndeterminedIndices = HashMultiset.create();
  private Multiset<String> pfUndeterminedIndices = HashMultiset.create();
  private final Multimap<String, String> newSamplesIndexes = ArrayListMultimap
      .create();
  private final Multimap<String, String> newIndexes = ArrayListMultimap
      .create();

  /**
   * This class store a result entry for the whole lane.
   */
  private static final class LaneResultEntry implements
      Comparable<LaneResultEntry> {

    private final String index;
    private final int rawClusterCount;
    private final int pfClusterCount;
    private final double pfPercent;
    private final double inRawUndeterminedIndicePercent;
    private final double inPFUndeterminedIndicePercent;
    private final String comment;

    /**
     * Get CSV header.
     * @return a string with the CSV header
     */
    public static String headerCSV() {

      return "Index\tRaw cluster count\tPF cluster count\tPF %\tRaw clusters in undetermined indices %\tPF clusters in undetermined indices %\tComment\n";
    }

    /**
     * Get the entry in CSV format.
     * @return the entry in CSV format in a string
     */
    public String toCSV() {

      return String.format("%s\t%d\t%d\t%.02f%%\t%.02f%%\t%.02f%%\t%s\n",
          this.index, this.rawClusterCount, this.pfClusterCount,
          this.pfPercent, this.inRawUndeterminedIndicePercent,
          this.inPFUndeterminedIndicePercent, this.comment);
    }

    @Override
    public int compareTo(final LaneResultEntry that) {

      return -((Integer) this.pfClusterCount).compareTo(that.pfClusterCount);
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param index the index of the entry
     * @param rawClusterCount raw cluster count
     * @param pfClusterCount passing filter cluster count
     * @param totalRawClusterCount total raw cluster count
     * @param totalPFRawClusterCount total passing filter cluster count
     * @param comment a comment about the entry
     */
    public LaneResultEntry(final String index, final int rawClusterCount,
        final int pfClusterCount, final int totalRawClusterCount,
        final int totalPFRawClusterCount, final String comment) {

      this.index = index;
      this.rawClusterCount = rawClusterCount;
      this.pfClusterCount = pfClusterCount;
      this.pfPercent = 100.0 * pfClusterCount / rawClusterCount;
      this.inRawUndeterminedIndicePercent =
          100.0 * rawClusterCount / totalRawClusterCount;
      this.inPFUndeterminedIndicePercent =
          100.0 * pfClusterCount / totalPFRawClusterCount;

      this.comment = comment;
    }

  }

  /**
   * This class store a result entry for a sample.
   */
  private static final class SampleResultEntry implements
      Comparable<SampleResultEntry> {

    private final String index;
    private final int rawClusterCount;
    private final int pfClusterCount;
    private final String comment;
    private final double pfPercent;
    private final double rawClusterPercent;
    private final double pfClusterPercent;

    /**
     * Get CSV header.
     * @return a string with the CSV header
     */
    public static String headerCSV() {

      return "Index\tRaw cluster count\tPF cluster count\tPF %\tRaw cluster %\tPF cluster count %\tComment\n";
    }

    /**
     * Get the entry in CSV format.
     * @return the entry in CSV format in a string
     */
    public String toCSV() {

      return String.format("%s\t%d\t%d\t%.02f%%\t%.02f%%\t%.02f%%\t%s\n",
          this.index, this.rawClusterCount, this.pfClusterCount,
          this.pfPercent, this.rawClusterPercent, this.pfClusterPercent,
          this.comment);
    }

    @Override
    public int compareTo(final SampleResultEntry that) {

      return -((Integer) this.pfClusterCount).compareTo(that.pfClusterCount);
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param index the index of the entry
     * @param rawClusterCount raw cluster count
     * @param pfClusterCount passing filter cluster count
     * @param totalRawClusterCount total raw cluster count
     * @param totalPFRawClusterCount total passing filter cluster count
     * @param comment a comment about the entry
     */

    /**
     * Constructor.
     * @param index the index of the entry
     * @param rawClusterCount raw cluster count
     * @param pfClusterCount passing filter cluster count
     * @param demuxRawClusterCount demux raw cluster count
     * @param demuxPFClusterCount demux passing filter cluster count
     * @param comment a comment about the entry
     */
    public SampleResultEntry(final String index, final int rawClusterCount,
        final int pfClusterCount, final int demuxRawClusterCount,
        final int demuxPFClusterCount, final String comment) {

      this.index = index;
      this.rawClusterCount = rawClusterCount;
      this.pfClusterCount = pfClusterCount;
      this.comment = comment;

      this.pfPercent = 100.0 * pfClusterCount / rawClusterCount;
      this.rawClusterPercent = 100.0 * rawClusterCount / demuxRawClusterCount;
      this.pfClusterPercent = 100.0 * pfClusterCount / demuxPFClusterCount;
    }

  }

  @Override
  public void run() {

    // Timer
    final Stopwatch timer = Stopwatch.createStarted();

    LOGGER.fine("Undetermined indexes: start for "
        + getFastqSample().getKeyFastqSample());
    try {
      processSequences(this.seqFile);
      setSuccess(true);

    } catch (AozanException e) {
      setException(e);

    } finally {

      timer.stop();

      LOGGER.fine("Undetermined indexes: end for "
          + getFastqSample().getKeyFastqSample() + " in "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));
    }
  }

  /**
   * Read FASTQ file and process the data by FastQC modules
   * @param seqFile input file
   * @throws AozanException if an error occurs while processing file
   */
  private void processSequences(final SequenceFile seqFile)
      throws AozanException {

    IlluminaReadId irid = null;

    try {

      while (seqFile.hasNext()) {

        final Sequence seq = seqFile.next();

        // Parse sequence id
        try {
          if (irid == null)
            irid = new IlluminaReadId(seq.getID().substring(1));
          else
            irid.parse(seq.getID().substring(1));
        } catch (EoulsanException e) {

          // This is not an Illumina id
          return;
        }

        this.rawUndeterminedIndices.add(irid.getSequenceIndex());
        if (!irid.isFiltered())
          this.pfUndeterminedIndices.add(irid.getSequenceIndex());
      }

      // Process results
      processResults();

    } catch (SequenceFormatException e) {
      throw new AozanException(e);
    }

  }

  @Override
  protected void processResults() throws AozanException {

    // For each sample find the indexes sequences that can be recovered
    for (Map.Entry<String, String> e : this.sampleIndexes.entrySet()) {

      final String sampleName = e.getKey();
      final String index = e.getValue();

      for (String i : this.rawUndeterminedIndices.elementSet()) {

        final int mismatches = mismatches(index, i);

        if (mismatches > 0 && mismatches <= this.maxMismatches) {

          this.newSamplesIndexes.put(sampleName, i);
          this.newIndexes.put(i, index);
        }
      }
    }

    int recoverableRawClusterCount = 0;
    int recoverablePFClusterCount = 0;

    // Compute results for each sample
    for (String sampleName : getSampleNames()) {
      recoverableRawClusterCount +=
          computeRecoverableSampleClusterCount(sampleName,
              this.rawUndeterminedIndices, ".recoverable.raw.cluster.count");
      recoverablePFClusterCount +=
          computeRecoverableSampleClusterCount(sampleName,
              this.pfUndeterminedIndices, ".recoverable.pf.cluster.count");
    }

    // Set the result for the lane
    getResults().put(
        "undeterminedindices.lane"
            + this.lane + ".recoverable.raw.cluster.count",
        recoverableRawClusterCount);
    getResults().put(
        "undeterminedindices.lane"
            + this.lane + ".recoverable.pf.cluster.count",
        recoverablePFClusterCount);

    // Create report
    try {
      createReportFile();
    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Compute for a sample the number of clusters that can be recovered.
   * @param sampleName sample name
   * @param indicesCounts multiset that contain data to process
   * @param resultKeySuffix the suffix for the run data key entry
   * @return the number of cluster that can be recovered for the sample
   */
  private int computeRecoverableSampleClusterCount(final String sampleName,
      final Multiset<String> indicesCounts, final String resultKeySuffix) {

    int recoverableClusterCount = 0;

    // Sum the number of cluster that can be recovered
    if (this.newSamplesIndexes.containsKey(sampleName))
      for (String newIndex : this.newSamplesIndexes.get(sampleName)) {

        if (indicesCounts.contains(newIndex)) {
          final int count = indicesCounts.count(newIndex);
          recoverableClusterCount += count;
        }
      }

    final String fastqDir =
        this.seqFile.getFile().getParentFile().getAbsolutePath() + "/../..";
    final FastqSample s =
        new FastqSample(fastqDir, this.read, this.lane, sampleName,
            getProjectSample(sampleName), getSampleDescription(sampleName),
            this.sampleIndexes.get(sampleName));

    // Set the result for the sample
    getResults().put(
        "undeterminedindices" + s.getPrefixRundata() + resultKeySuffix,
        recoverableClusterCount);

    return recoverableClusterCount;
  }

  @Override
  protected void createReportFile() throws AozanException, IOException {

    // Create the report for the lane
    createReportForLane();

    // Create the report for each samples
    for (String sampleName : getSampleNames())
      createReportForSample(sampleName);
  }

  /**
   * Create the report for the lane.
   * @throws IOException if an error occurs while creating the report
   */
  private void createReportForLane() throws IOException {

    // Test if demultiplexing with mismatches is possible
    boolean oneMismatcheDemuxPossible = true;
    for (Map.Entry<String, Collection<String>> e : this.newIndexes.asMap()
        .entrySet())
      if (e.getValue().size() > 1)
        oneMismatcheDemuxPossible = false;

    final int totalRawClusterCount = this.rawUndeterminedIndices.size();
    final int totalPFClusterCount = this.pfUndeterminedIndices.size();

    // Create sorted set
    final Set<LaneResultEntry> entries = Sets.newTreeSet();

    // Total entry
    final LaneResultEntry totalEntry =
        new LaneResultEntry(
            "Total",
            totalRawClusterCount,
            totalPFClusterCount,
            totalRawClusterCount,
            totalPFClusterCount,
            oneMismatcheDemuxPossible
                ? ""
                : "Demultiplexing with one mismatche is not possible due to indexes conflicts");

    final Joiner joiner = Joiner.on(", ");
    for (Multiset.Entry<String> e : this.rawUndeterminedIndices.entrySet()) {

      final String index = e.getElement();
      final int rawClusterCount = e.getCount();
      final int pfClusterCount =
          this.pfUndeterminedIndices.count(e.getElement());

      final Collection<String> samplesCollection = this.newIndexes.get(index);
      final String samples =
          samplesCollection.size() > 0 ? "Recovery possible for sample(s) "
              + joiner.join(samplesCollection) : "";

      entries.add(new LaneResultEntry(index, rawClusterCount, pfClusterCount,
          totalRawClusterCount, totalPFClusterCount, samples));
    }

    writeCSV(entries, totalEntry);
  }

  /**
   * Create the lane result file.
   * @param extension extension of the file
   * @return a File object
   */
  private File createLaneResultFile(final String extension) {

    final File reportFile =
        new File(this.reportDir, getFastqSample().getKeyFastqSample()
            + "-potentialindices" + extension);

    // Create parent directory if necessary
    final File parentDir = reportFile.getParentFile();
    if (!parentDir.exists())
      parentDir.mkdirs();

    return reportFile;
  }

  /**
   * Write the lane result file in CSV format.
   * @param entries entries to write
   * @param totalEntry total entries summary
   * @throws IOException if an error occurs while writing the file
   */
  private void writeCSV(final Set<LaneResultEntry> entries,
      final LaneResultEntry totalEntry) throws IOException {

    BufferedWriter br =
        Files.newWriter(createLaneResultFile(".csv"), Charsets.UTF_8);

    // Header
    br.write(LaneResultEntry.headerCSV());

    // Total recoverable result
    br.write(totalEntry.toCSV());

    // All the other results
    for (LaneResultEntry e : entries)
      br.write(e.toCSV());

    br.close();
  }

  /**
   * Create the report for a sample.
   * @param sampleName the sample name
   * @throws IOException if an error occurs while creating the report
   */
  private void createReportForSample(final String sampleName)
      throws IOException {

    // Create sorted set
    final Set<SampleResultEntry> entries = Sets.newTreeSet();

    final int sampleRawClusterCount = getSampleRawClusterCount(sampleName);
    final int samplePFClusterCount = getSamplePFClusterCount(sampleName);

    // Original result
    final SampleResultEntry demuxEntry =
        new SampleResultEntry(this.sampleIndexes.get(sampleName),
            sampleRawClusterCount, samplePFClusterCount, sampleRawClusterCount,
            samplePFClusterCount, "Samplesheet index for " + sampleName);

    int newIndexesRawClusterCount = 0;
    int newIndexesPFClusterCount = 0;

    // Add the new index found
    if (this.newSamplesIndexes.containsKey(sampleName))
      for (String newIndex : this.newSamplesIndexes.get(sampleName)) {

        final int newIndexRawClusterCount =
            this.rawUndeterminedIndices.count(newIndex);
        final int newIndexPFClusterCount =
            this.pfUndeterminedIndices.count(newIndex);
        final String comment =
            this.newIndexes.get(newIndex).size() > 1
                ? "Conflict if run demultiplexing with mismatches" : "";

        // Add the entry
        entries.add(new SampleResultEntry(newIndex, newIndexRawClusterCount,
            newIndexPFClusterCount, sampleRawClusterCount,
            samplePFClusterCount, comment));

        newIndexesRawClusterCount += newIndexRawClusterCount;
        newIndexesPFClusterCount += newIndexPFClusterCount;
      }

    // Total result
    final SampleResultEntry totalEntry =
        new SampleResultEntry("Total recoverable indices",
            newIndexesRawClusterCount, newIndexesPFClusterCount,
            sampleRawClusterCount, samplePFClusterCount, "");

    writeCSV(sampleName, demuxEntry, entries, totalEntry);
  }

  /**
   * Create the sample result file.
   * @param sampleName the sample name
   * @param extension extension of the file
   * @return a File object
   */
  private File createSampleResultFile(final String sampleName,
      final String extension) {

    final File reportFile =
        new File(this.reportDir.getAbsolutePath()
            + "/../Project_" + getProjectSample(sampleName) + "/" + sampleName
            + "-potentialindices" + extension);

    // Create parent directory if necessary
    final File parentDir = reportFile.getParentFile();
    if (!parentDir.exists())
      parentDir.mkdirs();

    return reportFile;
  }

  /**
   * Write the sample result file in CSV format.
   * @param demuxEntry original demux result
   * @param entries entries to write
   * @param totalEntry total entries summary
   * @throws IOException if an error occurs while writing the file
   */
  private void writeCSV(final String sampleName,
      final SampleResultEntry demuxEntry, final Set<SampleResultEntry> entries,
      final SampleResultEntry totalEntry) throws IOException {

    BufferedWriter br =
        Files.newWriter(createSampleResultFile(sampleName, ".csv"),
            Charsets.UTF_8);

    // Header
    br.write(SampleResultEntry.headerCSV());

    // Original demux result
    br.write(demuxEntry.toCSV());

    // Total recoverable result
    br.write(totalEntry.toCSV());

    // All the other results
    for (SampleResultEntry e : entries)
      br.write(e.toCSV());

    br.close();
  }

  //
  // Utility methods
  //

  /**
   * Get the number of mismatches of two string of the same length.
   * @param a the first string
   * @param b the second string
   * @return the number of mismatches
   */
  private static final int mismatches(final String a, String b) {

    Preconditions.checkNotNull(a, "a cannot be null");
    Preconditions.checkNotNull(b, "b cannot be null");
    Preconditions.checkArgument(a.length() == b.length(),
        "The length of the 2 String must be equals");

    final int len = a.length();
    int result = 0;

    for (int i = 0; i < len; i++)
      if (a.charAt(i) != b.charAt(i))
        result++;

    return result;
  }

  /**
   * Get a map with for each sample the index.
   * @return a Map object
   */
  private Map<String, String> getSampleIndexes() {

    final Map<String, String> result = Maps.newHashMap();

    for (String sampleName : getSampleNames()) {

      // Get the sample index
      String index =
          data.get("design.lane" + this.lane + "." + sampleName + ".index");

      result.put(sampleName, index);
    }

    return result;
  }

  /**
   * Get the sample names.
   * @return a list with the sample names
   */
  private List<String> getSampleNames() {

    return Lists.newArrayList(Splitter.on(',').split(
        this.data.get("design.lane" + this.lane + ".samples.names")));
  }

  /**
   * Get the project related to a sample of the lane.
   * @param sampleName the sample name
   * @return the project related to the sample
   */
  private String getProjectSample(final String sampleName) {

    return this.data.get("design.lane"
        + this.lane + "." + sampleName + ".sample.project");
  }

  /**
   * Get the description of a sample.
   * @param sampleName the sample name
   * @return the description of the sample
   */
  private String getSampleDescription(final String sampleName) {

    return this.data.get("design.lane"
        + this.lane + "." + sampleName + ".description");
  }

  /**
   * Get the raw cluster count for a sample.
   * @param sampleName sample name
   * @return the raw cluster count of the sample
   */
  private int getSampleRawClusterCount(final String sampleName) {

    return this.data.getInt("demux.lane"
        + this.lane + ".sample." + sampleName + ".read" + this.read
        + ".raw.cluster.count");
  }

  /**
   * Get the passing filter cluster count for a sample.
   * @param sampleName sample name
   * @return the passing filter cluster count of the sample
   */
  private int getSamplePFClusterCount(final String sampleName) {

    return this.data.getInt("demux.lane"
        + this.lane + ".sample." + sampleName + ".read" + this.read
        + ".pf.cluster.count");
  }

  //
  // Public constructor
  //

  /**
   * Constructor.
   * @param fastqSample sample to process
   * @throws AozanException if sample cannot be processed
   */
  public UndeterminedIndexesProcessThreads(final RunData data,
      final FastqSample fastqSample, int maxMismatches, final File reportDir)
      throws AozanException {

    super(fastqSample);

    checkNotNull(data, "data argument cannot be null");
    checkArgument(maxMismatches > 0, "maxMismatches argument must be > 0");
    checkNotNull(reportDir, "reportDir argument cannot be null");

    this.data = data;
    this.lane = fastqSample.getLane();
    this.read = fastqSample.getRead();
    this.maxMismatches = maxMismatches;
    this.reportDir = reportDir;

    this.sampleIndexes = getSampleIndexes();

    try {

      this.seqFile =
          SequenceFactory.getSequenceFile(fastqSample.getFastqFiles().toArray(
              new File[fastqSample.getFastqFiles().size()]));

    } catch (IOException io) {
      throw new AozanException(io);

    } catch (SequenceFormatException e) {
      throw new AozanException(e);
    }

  }

}
