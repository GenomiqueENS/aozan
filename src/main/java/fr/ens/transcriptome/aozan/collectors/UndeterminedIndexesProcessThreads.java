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

package fr.ens.transcriptome.aozan.collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.internal.annotations.Sets;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.aozan.util.XMLUtilsWriter;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.IlluminaReadId;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

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

  private static final Splitter TAB_SPLITTER = Splitter.on("\t").trimResults()
      .omitEmptyStrings();
  private static final Splitter COMMA_SPLITTER = Splitter.on(",").trimResults()
      .omitEmptyStrings();

  private static final Joiner JOINER = Joiner.on(", ");

  private final RunData data;
  private final int lane;
  private final int read;
  private final SequenceFile seqFile;
  private final File reportDir;
  private final File xslFile;

  private final Map<String, String> sampleIndexes;
  private final Map<String, String> reverseSampleIndexes;
  private Multiset<String> rawUndeterminedIndices = HashMultiset.create();
  private Multiset<String> pfUndeterminedIndices = HashMultiset.create();
  private final Multimap<String, String> newSamplesIndexes = ArrayListMultimap
      .create();
  private final Multimap<String, String> newIndexes = ArrayListMultimap
      .create();

  private int maxMismatches = 1;
  private boolean isSkipProcessResult = false;

  /**
   * This class store a result entry for the whole lane.
   */
  private static final class LaneResultEntry extends ResultEntry implements
      Comparable<LaneResultEntry> {

    static {
      HEADER_TYPE =
          Lists.newArrayList("string", "int", "int", "string", "string",
              "string", "string");

      HEADER_NAMES =
          Lists.newArrayList("Index", "Raw cluster count", "PF cluster count",
              "PF %", "Raw cluster in undetermined %",
              "PF cluster count in undetermined %",
              "Recovery possible for sample(s)");
    }

    private final String index;
    private final int rawClusterCount;
    private final int pfClusterCount;
    private final double pfPercent;
    private final double inRawUndeterminedIndicePercent;
    private final double inPFUndeterminedIndicePercent;
    private final String comment;

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

    @Override
    public boolean isCommentFieldEmpty() {
      return this.comment == null || this.comment.trim().length() == 0;
    }

    @Override
    public String getComment() {
      return this.comment;
    }

    public static void samplesNameXML(final Document doc, final Element parent,
        final RunData data, final int lane) {

      final Set<String> samplesName = Sets.newHashSet();

      // Extract all samples names per lane
      samplesName.addAll(COMMA_SPLITTER.splitToList(data
          .getSamplesNameInLane(lane)));

      // Write all samples name with correct syntax
      final String txt = "'" + Joiner.on("','").join(samplesName) + "'";

      // Add list sample for lane undetermined sample
      final Element samples = doc.createElement("Samples");
      samples.setAttribute("classValue", "samples");
      samples.setAttribute("cmdJS", txt);
      parent.appendChild(samples);

      // Add tag XML per sample name
      for (String sampleName : samplesName) {
        final Element sample = doc.createElement("Sample");
        sample.setAttribute("classValue", "sample");
        sample.setAttribute("cmdJS", "'" + sampleName + "'");
        sample.setTextContent(sampleName);
        samples.appendChild(sample);
      }
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
  private static final class SampleResultEntry extends ResultEntry implements
      Comparable<SampleResultEntry> {

    static {
      HEADER_TYPE =
          Lists.newArrayList("string", "int", "int", "string", "string",
              "string", "string");

      HEADER_NAMES =
          Lists.newArrayList("Index", "Raw cluster count", "PF cluster count",
              "PF %", "Raw cluster in sample %",
              "PF cluster count in sample %", "Comment");
    }

    private final String index;
    private final int rawClusterCount;
    private final int pfClusterCount;
    private final String comment;
    private final double pfPercent;
    private final double rawClusterPercent;
    private final double pfClusterPercent;

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

    @Override
    public boolean isCommentFieldEmpty() {
      return this.comment == null || this.comment.trim().length() == 0;
    }

    public String getComment() {
      return this.comment;
    }

    //
    // Constructor
    //
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

  /**
   * This class store a result entry for a sample.
   */
  private static abstract class ResultEntry {

    protected static List<String> HEADER_TYPE;

    protected static List<String> HEADER_NAMES;

    abstract String toCSV();

    abstract public boolean isCommentFieldEmpty();

    abstract public String getComment();

    /**
     * Get CSV header.
     * @return a string with the CSV header
     */
    public static String headerCSV() {
      return Joiner.on("\t").join(HEADER_NAMES) + "\n";
    }

    /**
     * Add tag XML header in document.
     * @param document document XML
     * @param parent parent element in document XML
     */
    public static void headerXML(final Document doc, final Element parent) {

      final Element columns = doc.createElement("Columns");
      // columns.setAttribute("classValue", "headerColumns");
      parent.appendChild(columns);

      for (String text : TAB_SPLITTER.split(headerCSV())) {

        final Element elem = doc.createElement("Column");
        elem.setTextContent(text);
        columns.appendChild(elem);
      }
    }

    /**
     * Add tag XML header in document.
     * @param document document XML
     * @param parent parent element in document XML
     * @param attributeValue value for desc attribute
     */
    public void toXML(final Document doc, final Element parent,
        final String attributeValue) {

      final Element elemRoot = doc.createElement("Entry");
      parent.appendChild(elemRoot);

      int n = 0;
      for (String text : TAB_SPLITTER.split(toCSV())) {

        final Element elem = doc.createElement("Data");
        elem.setAttribute("name",
            HEADER_NAMES.get(n).toLowerCase().replaceAll(" ", "_"));
        elem.setAttribute("type", HEADER_TYPE.get(n));
        elem.setAttribute("score", "-1");

        elem.setTextContent(text);
        elemRoot.appendChild(elem);

        n++;
      }

      // Add empty element for comment field
      if (isCommentFieldEmpty()) {
        final Element elem = doc.createElement("Data");
        elem.setAttribute("name",
            HEADER_NAMES.get(n).toLowerCase().replaceAll(" ", "_"));
        elem.setAttribute("type", HEADER_TYPE.get(n));
        elem.setAttribute("score", "-1");

        elem.setTextContent("");
        elemRoot.appendChild(elem);

        // Add class attribute at default value
        elemRoot.setAttribute("classValue", attributeValue);
      } else {
        // Add class attribute at sample name
        elemRoot.setAttribute("classValue", getComment());
      }

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

      // Set max mismatches allowed
      computeMismatchesAllowed();

      // Process results
      processResults();

    } catch (SequenceFormatException e) {
      throw new AozanException(e);
    }

  }

  private void computeMismatchesAllowed() {

    final int maxMismatchAllowed = 2;

    int minMismatchFound = Integer.MAX_VALUE;

    for (Map.Entry<String, String> e : this.sampleIndexes.entrySet()) {

      for (String i : this.rawUndeterminedIndices.elementSet()) {
        final String index = e.getValue();
        final int mismatches = mismatches(index, i);

        minMismatchFound = Math.min(minMismatchFound, mismatches);

        // Check minimum found
        if (minMismatchFound == 1)
          break;
      }

      if (minMismatchFound == 1)
        break;
    }

    if (minMismatchFound > maxMismatchAllowed) {
      // Set mismatches used to recovery reads
      this.isSkipProcessResult = true;
      this.maxMismatches = -1;
    } else {
      this.maxMismatches = minMismatchFound;
      getResults()
          .put(
              "undeterminedindices.lane"
                  + this.lane + ".mismatch.recovery.cluster", maxMismatches);
    }
  }

  @Override
  protected void processResults() throws AozanException {

    int recoverableRawClusterCount = 0;
    int recoverablePFClusterCount = 0;

    if (!this.isSkipProcessResult) {
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

      // Compute results for each sample
      for (String sampleName : this.data.getSamplesNameListInLane(this.lane)) {
        recoverableRawClusterCount +=
            computeRecoverableSampleClusterCount(sampleName,
                this.rawUndeterminedIndices, ".recoverable.raw.cluster.count");
        recoverablePFClusterCount +=
            computeRecoverableSampleClusterCount(sampleName,
                this.pfUndeterminedIndices, ".recoverable.pf.cluster.count");
      }
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

    if (!this.isSkipProcessResult) {
      // Sum the number of cluster that can be recovered
      if (this.newSamplesIndexes.containsKey(sampleName))
        for (String newIndex : this.newSamplesIndexes.get(sampleName)) {

          if (indicesCounts.contains(newIndex)) {
            final int count = indicesCounts.count(newIndex);
            recoverableClusterCount += count;
          }
        }
    }

    // Set the result for the sample
    getResults().put(
        "undeterminedindices"
            + ".lane" + this.lane + ".sample." + sampleName + resultKeySuffix,
        recoverableClusterCount);

    return recoverableClusterCount;
  }

  @Override
  protected void createReportFile() throws AozanException, IOException {

    // Create the report for the lane
    createReportForLane();

    // Create the report for each samples
    for (String sampleName : this.data.getSamplesNameListInLane(this.lane))
      createReportForSample(sampleName);
  }

  /**
   * Create the report for the lane.
   * @throws IOException if an error occurs while creating the report
   * @throws AozanException if an error occurs while building xml file
   */
  private void createReportForLane() throws IOException, AozanException {

    // Test if demultiplexing with mismatches is possible
    boolean oneMismatcheDemuxPossible = true;
    for (Map.Entry<String, Collection<String>> e : this.newIndexes.asMap()
        .entrySet())
      if (e.getValue().size() > 1)
        oneMismatcheDemuxPossible = false;

    final int totalRawClusterCount = this.rawUndeterminedIndices.size();
    final int totalPFClusterCount = this.pfUndeterminedIndices.size();

    // Create sorted set
    final List<LaneResultEntry> entries = Lists.newArrayList();

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

    for (Multiset.Entry<String> e : this.rawUndeterminedIndices.entrySet()) {

      final String index = e.getElement();
      final int rawClusterCount = e.getCount();
      final int pfClusterCount = this.pfUndeterminedIndices.count(index);

      List<String> samplesCollection = getSampleForNewIndex(index);
      final String samples =
          samplesCollection.size() > 0 ? JOINER.join(samplesCollection) : "";

      entries.add(new LaneResultEntry(index, rawClusterCount, pfClusterCount,
          totalRawClusterCount, totalPFClusterCount, samples));
    }

    // Sort list
    Collections.sort(entries);

    writeCSV(entries, totalEntry);
    writeHTML(entries, totalEntry);
  }

  /**
   * Get the list of samples that can be recovered for an index.
   * @param newIndex the index
   * @return a list with the names of the samples
   */
  private List<String> getSampleForNewIndex(final String newIndex) {

    final Collection<String> indexesCollection = this.newIndexes.get(newIndex);
    final List<String> samplesCollection = Lists.newArrayList();
    for (String i : indexesCollection)
      samplesCollection.add(this.reverseSampleIndexes.get(i));

    return samplesCollection;
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
  private void writeCSV(final List<LaneResultEntry> entries,
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
   * Write the lane result file in CSV format.
   * @param entries entries to write
   * @param totalEntry total entries summary
   * @throws IOException if an error occurs while writing the file
   * @throws AozanException if an error occurs while building xml file
   */
  private void writeHTML(final List<LaneResultEntry> entries,
      final LaneResultEntry totalEntry) throws IOException, AozanException {

    final File reportHtml = createLaneResultFile(".html");

    toXML("lane" + lane + "_undetermined", null, entries, totalEntry,
        reportHtml, false);

  }

  /**
   * Create the report for a sample.
   * @param sampleName the sample name
   * @throws IOException if an error occurs while creating the report
   * @throws AozanException if an
   */
  private void createReportForSample(final String sampleName)
      throws IOException, AozanException {

    // Create sorted set
    final List<SampleResultEntry> entries = Lists.newArrayList();

    final int sampleRawClusterCount = getSampleRawClusterCount(sampleName);
    final int samplePFClusterCount = getSamplePFClusterCount(sampleName);

    // Original result
    final SampleResultEntry demuxEntry =
        new SampleResultEntry(this.sampleIndexes.get(sampleName),
            sampleRawClusterCount, samplePFClusterCount, sampleRawClusterCount,
            samplePFClusterCount, "Demultiplexing result " + sampleName);

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
                ? "Conflict if run demultiplexing with mismatches : "
                    + JOINER.join(this.newIndexes.get(newIndex)) : "";

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

    // Sort lists
    Collections.sort(entries);

    writeCSV(sampleName, demuxEntry, entries, totalEntry);
    writeHTML(sampleName, demuxEntry, entries, totalEntry);
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
            + "_lane" + lane + "-potentialindices" + extension);

    // Create parent directory if necessary
    final File parentDir = reportFile.getParentFile();
    if (!parentDir.exists())
      parentDir.mkdirs();

    return reportFile;
  }

  /**
   * Write the sample result file in csv format.
   * @param demuxEntry original demux result
   * @param entries entries to write
   * @param totalEntry total entries summary
   * @throws IOException if an error occurs while writing the file
   */
  private void writeCSV(final String sampleName,
      final SampleResultEntry demuxEntry,
      final List<SampleResultEntry> entries, final SampleResultEntry totalEntry)
      throws IOException {

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

  /**
   * Write the sample result file in HTML format.
   * @param sampleName sample name
   * @param demuxEntry original demux result
   * @param entries entries to write
   * @param totalEntry total entries summary
   * @throws IOException if an error occurs while writing the file
   * @throws AozanException if on useful file is not define or if an error
   *           occurs during transforming document.
   */
  private void writeHTML(final String sampleName,
      final SampleResultEntry demuxEntry,
      final List<SampleResultEntry> entries, final SampleResultEntry totalEntry)
      throws IOException, AozanException {

    final File reportHtml = createSampleResultFile(sampleName, ".html");

    toXML(sampleName, demuxEntry, entries, totalEntry, reportHtml, true);

  }

  /**
   * Write the sample result file in HTML format.
   * @param sampleName sample name
   * @param demuxEntry original demux result
   * @param entries entries to write
   * @param totalEntry total entries summary
   * @param reportHtml report output file in HTML
   * @param isSampleData true if it is a demultiplexed sample
   * @throws IOException if an error occurs while writing the file
   * @throws AozanException if an usefull file are not define or if an error
   *           occurs during building document xml
   */
  private void toXML(final String sampleName, final ResultEntry demuxEntry,
      final List<? extends ResultEntry> entries, final ResultEntry totalEntry,
      final File reportHtml, final boolean isSampleData) throws IOException,
      AozanException {

    Document doc = null;

    try {
      DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = null;
      docBuilder = dbfac.newDocumentBuilder();
      doc = docBuilder.newDocument();

    } catch (ParserConfigurationException e) {
      throw new AozanException(e);
    }

    // Create the root element and add it to the document
    Element root = doc.createElement("RecoveryClusterReport");
    root.setAttribute("formatversion", "1.0");
    doc.appendChild(root);

    XMLUtils.addTagValue(doc, root, "Step",
        "Recovery cluster from undetermined fastq file");

    // Common XML tag
    XMLUtilsWriter.buildXMLCommonTagHeader(doc, root, this.data);

    XMLUtils.addTagValue(doc, root, "sampleName", sampleName);

    // Case Undetermined indices samples, no project name
    if (isSampleData)
      XMLUtils.addTagValue(doc, root, "projectName",
          getProjectSample(sampleName));

    XMLUtils.addTagValue(doc, root, "description",
        this.data.getSampleDescription(this.lane, sampleName));

    XMLUtils.addTagValue(doc, root, "condition",
        "Compile results on recovery reads in undetermined fastq with "
            + this.maxMismatches + " mismatches.");

    if (!isSampleData) {
      // Add sample name in this lane
      LaneResultEntry.samplesNameXML(doc, root, this.data, this.lane);
    }

    // Table - column
    ResultEntry.headerXML(doc, root);

    final Element results = doc.createElement("Results");
    results.setAttribute("id", "data");
    root.appendChild(results);

    // Only exists for sample report
    if (demuxEntry != null) {
      // Demultiplexing result
      demuxEntry.toXML(doc, results, "demultiplexing");
    }

    // Total entry
    totalEntry.toXML(doc, results, "total");

    for (ResultEntry e : entries) {
      e.toXML(doc, results, "Entry");
    }

    // TODO debug
    // XMLUtilsWriter.createXMLFile(doc, reportHtml);

    // Set xsl file to write report HTML file
    InputStream is = null;
    if (this.xslFile == null)
      is =
          this.getClass()
              .getResourceAsStream(Globals.EMBEDDED_UNDETERMINED_XSL);
    else
      is = new FileInputStream(this.xslFile);

    // Write report HTML
    XMLUtilsWriter.createHTMLFileFromXSL(doc, is, reportHtml);
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
   * Reverse a map.
   * @param map the original map
   * @return a new map with the inversed key-values
   */
  private Map<String, String> reverse(Map<String, String> map) {

    if (map == null)
      return null;

    final Map<String, String> result = Maps.newHashMap();

    for (Map.Entry<String, String> e : map.entrySet())
      result.put(e.getValue(), e.getKey());

    return result;
  }

  /**
   * Get a map with for each sample the index.
   * @return a Map object
   */
  private Map<String, String> getSampleIndexes() {

    final Map<String, String> result = Maps.newHashMap();

    for (String sampleName : this.data.getSamplesNameListInLane(this.lane)) {

      // Get the sample index
      String index = this.data.getIndexSample(this.lane, sampleName);

      result.put(sampleName, index);
    }

    return result;
  }

  /**
   * Get the project related to a sample of the lane.
   * @param sampleName the sample name
   * @return the project related to the sample
   */
  private String getProjectSample(final String sampleName) {

    return this.data.getProjectSample(this.lane, sampleName);
  }

  /**
   * Get the raw cluster count for a sample.
   * @param sampleName sample name
   * @return the raw cluster count of the sample
   */
  private int getSampleRawClusterCount(final String sampleName) {

    return this.data.getSampleRawClusterCount(this.lane, this.read, sampleName);
  }

  /**
   * Get the passing filter cluster count for a sample.
   * @param sampleName sample name
   * @return the passing filter cluster count of the sample
   */
  private int getSamplePFClusterCount(final String sampleName) {

    return this.data.getSamplePFClusterCount(this.lane, this.read, sampleName);
  }

  //
  // Public constructor
  //

  /**
   * Constructor.
   * @param data run data instance
   * @param fastqSample sample to process
   * @param reportDir output report directory
   * @param undeterminedIndexedXSLFile xsl file use to create report html
   * @throws AozanException if sample cannot be processed
   */
  public UndeterminedIndexesProcessThreads(final RunData data,
      final FastqSample fastqSample, final File reportDir,
      final File undeterminedIndexedXSLFile) throws AozanException {

    super(fastqSample);

    checkNotNull(data, "data argument cannot be null");
    checkNotNull(reportDir, "reportDir argument cannot be null");

    this.data = data;
    this.lane = fastqSample.getLane();
    this.read = fastqSample.getRead();
    this.reportDir = reportDir;
    this.xslFile = undeterminedIndexedXSLFile;

    this.sampleIndexes = getSampleIndexes();
    this.reverseSampleIndexes = reverse(this.sampleIndexes);

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
