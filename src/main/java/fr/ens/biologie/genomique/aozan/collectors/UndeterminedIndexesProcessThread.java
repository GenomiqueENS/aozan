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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.aozan.collectors.UndeterminedIndexesCollector.COLLECTOR_NAME;
import static fr.ens.biologie.genomique.aozan.collectors.UndeterminedIndexesCollector.RUN_DATA_PREFIX;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.io.FastqSample;
import fr.ens.biologie.genomique.aozan.util.XMLUtilsWriter;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.IlluminaReadId;
import fr.ens.biologie.genomique.eoulsan.util.XMLUtils;
import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

/**
 * This class allow to process the undetermined fastq file of a lane to extract
 * indices that can be recovered.
 * @since 1.3
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
public class UndeterminedIndexesProcessThread
    extends AbstractFastqProcessThread {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private static final Splitter TAB_SPLITTER =
      Splitter.on("\t").trimResults().omitEmptyStrings();

  private static final Joiner JOINER = Joiner.on(", ");

  private final RunData data;
  private final int lane;
  private final int read;
  private final SequenceFile seqFile;
  private final File reportDir;
  private final File xslFile;

  private final Map<Integer, String> sampleIndexes;
  private final Map<String, Integer> reverseSampleIndexes;
  private final Multiset<String> rawUndeterminedIndices = HashMultiset.create();
  private final Multiset<String> pfUndeterminedIndices = HashMultiset.create();
  private final Multimap<Integer, String> newSamplesIndexes =
      ArrayListMultimap.create();
  private final Multimap<String, String> newIndexes =
      ArrayListMultimap.create();

  private int maxMismatches = 1;
  private boolean isSkipProcessResult = false;

  /**
   * This class store a result entry for the whole lane.
   */
  private static final class LaneResultEntry extends ResultEntry
      implements Comparable<LaneResultEntry> {

    static {
      headerType = Lists.newArrayList("string", "int", "int", "string",
          "string", "string", "string");

      headerNames = Lists.newArrayList("Index", "Raw cluster count",
          "PF cluster count", "PF %", "Raw cluster in undetermined %",
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
    @Override
    public String toCSV() {

      return String.format("%s\t%d\t%d\t%.02f%%\t%.02f%%\t%.02f%%\t%s%n",
          this.index, this.rawClusterCount, this.pfClusterCount, this.pfPercent,
          this.inRawUndeterminedIndicePercent,
          this.inPFUndeterminedIndicePercent, this.comment);
    }

    @Override
    public int compareTo(final LaneResultEntry that) {

      return ((Integer) that.pfClusterCount).compareTo(this.pfClusterCount);
    }

    @Override
    public int hashCode() {

      return Objects.hash(this.index, this.rawClusterCount, this.pfClusterCount,
          this.pfPercent, this.inRawUndeterminedIndicePercent,
          this.inPFUndeterminedIndicePercent, this.comment);
    }

    @Override
    public boolean equals(final Object obj) {

      if (this == obj) {
        return true;
      }

      if (obj == null || getClass() != obj.getClass())
        return false;

      final LaneResultEntry that = (LaneResultEntry) obj;

      return Objects.equals(this.index, that.index)
          && Objects.equals(this.rawClusterCount, that.rawClusterCount)
          && Objects.equals(this.pfClusterCount, that.pfClusterCount)
          && Objects.equals(this.pfPercent, that.pfPercent)
          && Objects.equals(this.inRawUndeterminedIndicePercent,
              that.inRawUndeterminedIndicePercent)
          && Objects.equals(this.inPFUndeterminedIndicePercent,
              that.inPFUndeterminedIndicePercent)
          && Objects.equals(this.comment, that.comment);
    }

    @Override
    public boolean isCommentFieldEmpty() {
      return this.comment == null || this.comment.trim().length() == 0;
    }

    @Override
    public String getAttributeClass() {
      // Entry corresponding total line
      if (this.index.toLowerCase(Globals.DEFAULT_LOCALE).equals("total")) {
        return TOTAL_TAG;
      }

      // Check several samples names
      if (this.comment.contains(",")) {
        return CONFLICT_TAG;
      }

      return this.comment;
    }

    /**
     * Add elements samples name in xml document to filter result table.
     * @param doc the doc xml
     * @param parent the parent element
     * @param data the data run object
     * @param lane the lane number
     * @param demultiplexingWithConflict true if demultiplexing conflict exist
     */
    public static void samplesNameXML(final Document doc, final Element parent,
        final RunData data, final int lane,
        final boolean demultiplexingWithConflict) {

      // Extract all samples names per lane
      final List<Integer> sampleIds =
          new ArrayList<>(data.getSamplesInLane(lane));
      final List<String> sampleNames = new ArrayList<>(sampleIds.size());
      for (int sampleId : sampleIds) {
        sampleNames.add(data.getSampleDemuxName(sampleId));
      }

      Collections.sort(sampleNames);

      // Add link on filter for conflict at the end of list
      if (demultiplexingWithConflict) {
        sampleNames.add(sampleNames.size(), CONFLICT_TAG);
      }

      // Write all samples name with correct syntax
      final String txt = "'" + Joiner.on("','").join(sampleNames) + "'";

      // Add list sample for lane undetermined sample
      final Element samples = doc.createElement("Samples");
      samples.setAttribute("classValue", "samples");
      samples.setAttribute("cmdJS", txt);
      parent.appendChild(samples);

      // Add tag XML per sample name
      for (final String sampleName : sampleNames) {
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
  private static final class SampleResultEntry extends ResultEntry
      implements Comparable<SampleResultEntry> {

    static {
      headerType = Lists.newArrayList("string", "int", "int", "string",
          "string", "string", "string");

      headerNames = Lists.newArrayList("Index", "Raw cluster count",
          "PF cluster count", "PF %", "Raw cluster in sample %",
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
    @Override
    public String toCSV() {

      return String.format("%s\t%d\t%d\t%.02f%%\t%.02f%%\t%.02f%%\t%s%n",
          this.index, this.rawClusterCount, this.pfClusterCount, this.pfPercent,
          this.rawClusterPercent, this.pfClusterPercent, this.comment);
    }

    @Override
    public int hashCode() {

      return Objects.hash(this.index, this.rawClusterCount, this.pfClusterCount,
          this.comment, this.pfPercent, this.rawClusterPercent,
          this.pfClusterPercent);
    }

    @Override
    public boolean equals(final Object obj) {

      if (this == obj) {
        return true;
      }

      if (obj == null || getClass() != obj.getClass())
        return false;

      final SampleResultEntry that = (SampleResultEntry) obj;

      return Objects.equals(this.index, that.index)
          && Objects.equals(this.rawClusterCount, that.rawClusterCount)
          && Objects.equals(this.pfClusterCount, that.pfClusterCount)
          && Objects.equals(this.pfPercent, that.pfPercent)
          && Objects.equals(this.rawClusterPercent, that.rawClusterPercent)
          && Objects.equals(this.pfClusterPercent, that.pfClusterPercent)
          && Objects.equals(this.comment, that.comment);
    }

    @Override
    public int compareTo(final SampleResultEntry that) {

      return ((Integer) that.pfClusterCount).compareTo(this.pfClusterCount);
    }

    @Override
    public boolean isCommentFieldEmpty() {
      return this.comment == null || this.comment.trim().length() == 0;
    }

    @Override
    public String getAttributeClass() {
      // Data entry
      if (this.comment.toLowerCase(Globals.DEFAULT_LOCALE)
          .startsWith("conflict")) {
        return CONFLICT_TAG;
      }
      return "";
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
  private abstract static class ResultEntry {

    protected static List<String> headerType;

    protected static List<String> headerNames;

    protected static final String CONFLICT_TAG = "conflict";
    protected static final String TOTAL_TAG = "total";

    abstract String toCSV();

    public abstract boolean isCommentFieldEmpty();

    public abstract String getAttributeClass();

    /**
     * Get CSV header.
     * @return a string with the CSV header
     */
    public static String headerCSV() {
      return Joiner.on("\t").join(headerNames) + "\n";
    }

    /**
     * Add tag XML header in document.
     * @param doc XML document
     * @param parent parent element in document XML
     */
    public static void headerXML(final Document doc, final Element parent) {

      final Element columns = doc.createElement("Columns");
      // columns.setAttribute("classValue", "headerColumns");
      parent.appendChild(columns);

      for (final String text : TAB_SPLITTER.split(headerCSV())) {

        final Element elem = doc.createElement("Column");
        elem.setTextContent(text);
        columns.appendChild(elem);
      }
    }

    /**
     * Add tag XML header in document.
     * @param doc XML document
     * @param parent parent element in document XML
     * @param defaultAttributeValue value for desc attribute
     */
    public void toXML(final Document doc, final Element parent,
        final String defaultAttributeValue) {

      final Element elemRoot = doc.createElement("Entry");
      parent.appendChild(elemRoot);

      int n = 0;
      for (final String text : TAB_SPLITTER.split(toCSV())) {

        final Element elem = doc.createElement("Data");
        elem.setAttribute("name",
            headerNames.get(n).toLowerCase().replaceAll(" ", "_"));
        elem.setAttribute("type", headerType.get(n));
        elem.setAttribute("score", "-1");

        elem.setTextContent(text);
        elemRoot.appendChild(elem);

        n++;
      }

      // Add empty element for comment field
      if (isCommentFieldEmpty()) {
        final Element elem = doc.createElement("Data");
        elem.setAttribute("name",
            headerNames.get(n).toLowerCase().replaceAll(" ", "_"));
        elem.setAttribute("type", headerType.get(n));
        elem.setAttribute("score", "-1");

        elem.setTextContent("");
        elemRoot.appendChild(elem);

        // Add class attribute at default value
        elemRoot.setAttribute("classValue", defaultAttributeValue);
      } else {
        // Add class attribute at sample name
        elemRoot.setAttribute("classValue", getAttributeClass());
      }

    }

  }

  @Override
  protected void logThreadStart() {
    LOGGER.fine(COLLECTOR_NAME.toUpperCase()
        + ": start for " + getFastqSample().getKeyFastqSample());
  }

  @Override
  protected void process() throws AozanException {

    processSequences(this.seqFile);
  }

  @Override
  protected void logThreadEnd(final String duration) {

    LOGGER.fine(COLLECTOR_NAME.toUpperCase()
        + ": end for " + getFastqSample().getKeyFastqSample() + " in "
        + duration);
  }

  /**
   * Read FASTQ file and process the data by FastQC modules.
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
          if (irid == null) {
            irid = new IlluminaReadId(seq.getID().substring(1));
          } else {
            irid.parse(seq.getID().substring(1));
          }
        } catch (final EoulsanException e) {

          // This is not an Illumina id
          return;
        }

        // Get the sequence indexes
        final List<String> indexes = irid.getSequenceIndexList();

        // Process only nucleotides sequences
        if (indexes.isEmpty()) {
          continue;
        }

        // TODO Process the secondary indexes
        final String index = indexes.get(0);

        if (index == null
            || index.isEmpty() || Character.isDigit(index.charAt(0))) {
          continue;
        }

        this.rawUndeterminedIndices.add(index);
        if (!irid.isFiltered()) {
          this.pfUndeterminedIndices.add(index);
        }
      }

      // Set max mismatches allowed
      computeMismatchesAllowed();

      // Process results
      processResults();

    } catch (final SequenceFormatException e) {
      throw new AozanException(e);
    }

  }

  private void computeMismatchesAllowed() {

    final int maxMismatchAllowed = 2;

    int minMismatchFound = Integer.MAX_VALUE;

    for (final Map.Entry<Integer, String> e : this.sampleIndexes.entrySet()) {

      for (final String i : this.rawUndeterminedIndices.elementSet()) {
        final String index = e.getValue();
        final int mismatches = mismatches(index, i);

        minMismatchFound = Math.min(minMismatchFound, mismatches);

        // Check minimum found
        if (minMismatchFound == 1) {
          break;
        }
      }

      if (minMismatchFound == 1) {
        break;
      }
    }

    if (minMismatchFound > maxMismatchAllowed) {
      // Set mismatches used to recovery reads
      this.isSkipProcessResult = true;
      this.maxMismatches = -1;
    } else {
      this.maxMismatches = minMismatchFound;
      getResults().put(
          RUN_DATA_PREFIX + ".lane" + this.lane + ".mismatch.recovery.cluster",
          this.maxMismatches);
    }
  }

  @Override
  protected void processResults() throws AozanException {

    int recoverableRawClusterCount = 0;
    int recoverablePFClusterCount = 0;

    // Initialize results for each sample of the lane
    for (final int sampleId : this.data.getSamplesInLane(this.lane)) {

      getResults().put(RUN_DATA_PREFIX
          + ".sample." + sampleId + ".recoverable.raw.cluster.count", 0);
      getResults().put(RUN_DATA_PREFIX
          + ".sample." + sampleId + ".recoverable.pf.cluster.count", 0);
    }

    if (!this.isSkipProcessResult) {
      // For each sample find the indexes sequences that can be recovered
      for (final Map.Entry<Integer, String> e : this.sampleIndexes.entrySet()) {

        final int sampleId = e.getKey();
        final String index = e.getValue();

        for (final String i : this.rawUndeterminedIndices.elementSet()) {

          final int mismatches = mismatches(index, i);

          if (mismatches > 0 && mismatches <= this.maxMismatches) {

            this.newSamplesIndexes.put(sampleId, i);
            this.newIndexes.put(i, index);
          }
        }
      }

      // Compute results for each sample
      for (final int sampleId : this.data.getSamplesInLane(this.lane)) {
        recoverableRawClusterCount +=
            computeRecoverableSampleClusterCount(sampleId,
                this.rawUndeterminedIndices, ".recoverable.raw.cluster.count");
        recoverablePFClusterCount +=
            computeRecoverableSampleClusterCount(sampleId,
                this.pfUndeterminedIndices, ".recoverable.pf.cluster.count");
      }
    }

    // Set the result for the lane
    getResults().put(
        RUN_DATA_PREFIX
            + ".lane" + this.lane + ".recoverable.raw.cluster.count",
        recoverableRawClusterCount);
    getResults().put(
        RUN_DATA_PREFIX + ".lane" + this.lane + ".recoverable.pf.cluster.count",
        recoverablePFClusterCount);

    // Create report
    try {
      createReportFile();
    } catch (final IOException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Compute for a sample the number of clusters that can be recovered.
   * @param sampleId sample Id
   * @param indicesCounts multiset that contain data to process
   * @param resultKeySuffix the suffix for the run data key entry
   * @return the number of cluster that can be recovered for the sample
   */
  private int computeRecoverableSampleClusterCount(final int sampleId,
      final Multiset<String> indicesCounts, final String resultKeySuffix) {

    int recoverableClusterCount = 0;

    if (!this.isSkipProcessResult) {
      // Sum the number of cluster that can be recovered
      if (this.newSamplesIndexes.containsKey(sampleId)) {
        for (final String newIndex : this.newSamplesIndexes.get(sampleId)) {

          if (indicesCounts.contains(newIndex)) {
            final int count = indicesCounts.count(newIndex);
            recoverableClusterCount += count;
          }
        }
      }
    }

    // Set the result for the sample
    getResults().put(RUN_DATA_PREFIX + ".sample" + sampleId + resultKeySuffix,
        recoverableClusterCount);

    return recoverableClusterCount;
  }

  @Override
  protected void createReportFile() throws AozanException, IOException {

    // Create the report for the lane
    createReportForLane();

    // Create the report for each samples
    for (final int sampleId : this.data.getSamplesInLane(this.lane)) {
      createReportForSample(sampleId);
    }
  }

  /**
   * Create the report for the lane.
   * @throws IOException if an error occurs while creating the report
   * @throws AozanException if an error occurs while building xml file
   */
  private void createReportForLane() throws IOException, AozanException {

    // Test if demultiplexing with mismatches is possible
    boolean oneMismatcheDemuxPossible = true;
    for (final Map.Entry<String, Collection<String>> e : this.newIndexes.asMap()
        .entrySet()) {
      if (e.getValue().size() > 1) {
        oneMismatcheDemuxPossible = false;
      }
    }

    final int totalRawClusterCount = this.rawUndeterminedIndices.size();
    final int totalPFClusterCount = this.pfUndeterminedIndices.size();

    // Create sorted set
    final List<LaneResultEntry> entries = new ArrayList<>();

    // Total entry
    final LaneResultEntry totalEntry = new LaneResultEntry("Total",
        totalRawClusterCount, totalPFClusterCount, totalRawClusterCount,
        totalPFClusterCount,
        oneMismatcheDemuxPossible
            ? ""
            : "Demultiplexing with one mismatche is not possible due to indexes conflicts");

    for (final Multiset.Entry<String> e : this.rawUndeterminedIndices
        .entrySet()) {

      final String index = e.getElement();
      final int rawClusterCount = e.getCount();
      final int pfClusterCount = this.pfUndeterminedIndices.count(index);

      final List<Integer> sampleIds = getSampleForNewIndex(index);
      final List<String> sampleNames = new ArrayList<>(sampleIds.size());
      for (int sampleId : sampleIds) {
        sampleNames.add(this.data.getSampleDemuxName(sampleId));
      }
      final String samples =
          sampleIds.size() > 0 ? JOINER.join(sampleNames) : "";

      entries.add(new LaneResultEntry(index, rawClusterCount, pfClusterCount,
          totalRawClusterCount, totalPFClusterCount, samples));
    }

    // Sort list
    Collections.sort(entries);

    writeCSV(entries, totalEntry);
    writeHTML(entries, totalEntry, !oneMismatcheDemuxPossible);
  }

  /**
   * Get the list of samples that can be recovered for an index.
   * @param newIndex the index
   * @return a list with the names of the samples
   */
  private List<Integer> getSampleForNewIndex(final String newIndex) {

    final Collection<String> indexesCollection = this.newIndexes.get(newIndex);
    final List<Integer> samplesCollection = new ArrayList<>();
    for (final String i : indexesCollection) {
      samplesCollection.add(this.reverseSampleIndexes.get(i));
    }

    return samplesCollection;
  }

  /**
   * Create the lane result file.
   * @param extension extension of the file
   * @return a File object
   * @throws IOException if it fails to create directory of report
   */
  private File createLaneResultFile(final String extension) throws IOException {

    final File reportFile = new File(this.reportDir,
        getFastqSample().getPrefixReport() + "-potentialindices" + extension);

    // Create parent directory if necessary
    final File parentDir = reportFile.getParentFile();
    if (!parentDir.exists()) {
      java.nio.file.Files.createDirectories(parentDir.toPath());
    }

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

    final BufferedWriter br =
        Files.newWriter(createLaneResultFile(".csv"), StandardCharsets.UTF_8);

    // Header
    br.write(ResultEntry.headerCSV());

    // Total recoverable result
    br.write(totalEntry.toCSV());

    // All the other results
    for (final LaneResultEntry e : entries) {
      br.write(e.toCSV());
    }

    br.close();
  }

  /**
   * Write the lane result file in CSV format.
   * @param entries entries to write
   * @param totalEntry total entries summary
   * @param demultiplexingWithConflict true if conflict occurs during recovering
   *          cluster
   * @throws IOException if an error occurs while writing the file
   * @throws AozanException if an error occurs while building xml file
   */
  private void writeHTML(final List<LaneResultEntry> entries,
      final LaneResultEntry totalEntry,
      final boolean demultiplexingWithConflict)
      throws IOException, AozanException {

    final File reportHtml = createLaneResultFile(".html");

    toXML(-1, null, entries, totalEntry, reportHtml, false,
        demultiplexingWithConflict);
  }

  /**
   * Create the report for a sample.
   * @param sampleId the sample name
   * @throws IOException if an error occurs while creating the report
   * @throws AozanException if an
   */
  private void createReportForSample(final int sampleId)
      throws IOException, AozanException {

    // Create sorted set
    final List<SampleResultEntry> entries = new ArrayList<>();

    final int sampleRawClusterCount =
        this.data.getSampleRawClusterCount(sampleId, this.read);
    final int samplePFClusterCount =
        this.data.getSamplePFClusterCount(sampleId, this.read);

    // Original result
    final SampleResultEntry demuxEntry =
        new SampleResultEntry(this.sampleIndexes.get(sampleId),
            sampleRawClusterCount, samplePFClusterCount, sampleRawClusterCount,
            samplePFClusterCount, "Demultiplexing result " + sampleId);

    int newIndexesRawClusterCount = 0;
    int newIndexesPFClusterCount = 0;

    // Add the new index found
    if (this.newSamplesIndexes.containsKey(sampleId)) {
      for (final String newIndex : this.newSamplesIndexes.get(sampleId)) {

        final int newIndexRawClusterCount =
            this.rawUndeterminedIndices.count(newIndex);
        final int newIndexPFClusterCount =
            this.pfUndeterminedIndices.count(newIndex);
        final String comment = this.newIndexes.get(newIndex).size() > 1
            ? "Conflict if run demultiplexing with "
                + this.maxMismatches + " mismatch(es) : "
                + JOINER.join(this.newIndexes.get(newIndex))
            : "";

        // Add the entry
        entries.add(new SampleResultEntry(newIndex, newIndexRawClusterCount,
            newIndexPFClusterCount, sampleRawClusterCount, samplePFClusterCount,
            comment));

        newIndexesRawClusterCount += newIndexRawClusterCount;
        newIndexesPFClusterCount += newIndexPFClusterCount;
      }
    }

    // Total result
    final SampleResultEntry totalEntry =
        new SampleResultEntry("Total recoverable indices",
            newIndexesRawClusterCount, newIndexesPFClusterCount,
            sampleRawClusterCount, samplePFClusterCount, "");

    // Sort lists
    Collections.sort(entries);

    writeCSV(sampleId, demuxEntry, entries, totalEntry);
    writeHTML(sampleId, demuxEntry, entries, totalEntry);
  }

  /**
   * Create the sample result file.
   * @param sampleId the sample name
   * @param extension extension of the file
   * @return a File object
   * @throws IOException if it fails to create directory of report
   */
  private File createSampleResultFile(final int sampleId,
      final String extension) throws IOException {

    final File reportFile =
        new File(this.reportDir.getParentFile().getAbsolutePath()
            + "/Project_" + this.data.getProjectSample(sampleId) + "/"
            + this.data.getSampleDemuxName(sampleId) + "_lane" + this.lane
            + "-potentialindices" + extension);

    // Create parent directory if necessary
    final File parentDir = reportFile.getParentFile();
    if (!parentDir.exists()) {
      java.nio.file.Files.createDirectories(parentDir.toPath());
    }

    return reportFile;
  }

  /**
   * Write the sample result file in csv format.
   * @param sampleId the sample Id
   * @param demuxEntry original demux result
   * @param entries entries to write
   * @param totalEntry total entries summary
   * @throws IOException if an error occurs while writing the file
   */
  private void writeCSV(final int sampleId, final SampleResultEntry demuxEntry,
      final List<SampleResultEntry> entries, final SampleResultEntry totalEntry)
      throws IOException {

    final BufferedWriter br = Files.newWriter(
        createSampleResultFile(sampleId, ".csv"), StandardCharsets.UTF_8);

    // Header
    br.write(ResultEntry.headerCSV());

    // Original demux result
    br.write(demuxEntry.toCSV());

    // Total recoverable result
    br.write(totalEntry.toCSV());

    // All the other results
    for (final SampleResultEntry e : entries) {
      br.write(e.toCSV());
    }

    br.close();
  }

  /**
   * Write the sample result file in HTML format.
   * @param sampleId sample id
   * @param demuxEntry original demux result
   * @param entries entries to write
   * @param totalEntry total entries summary
   * @throws IOException if an error occurs while writing the file
   * @throws AozanException if on useful file is not define or if an error
   *           occurs during transforming document.
   */
  private void writeHTML(final int sampleId, final SampleResultEntry demuxEntry,
      final List<SampleResultEntry> entries, final SampleResultEntry totalEntry)
      throws IOException, AozanException {

    final File reportHtml = createSampleResultFile(sampleId, ".html");

    toXML(sampleId, demuxEntry, entries, totalEntry, reportHtml, true, false);
  }

  /**
   * Write the sample result file in HTML format.
   * @param sampleId sample id
   * @param demuxEntry original demux result
   * @param entries entries to write
   * @param totalEntry total entries summary
   * @param reportHtml report output file in HTML
   * @param isSampleData true if it is a demultiplexed sample
   * @param demultiplexingWithConflict true if conflict occurs during recovering
   *          cluster
   * @throws IOException if an error occurs while writing the file
   * @throws AozanException if an usefull file are not define or if an error
   *           occurs during building document xml
   */
  private void toXML(final int sampleId, final ResultEntry demuxEntry,
      final List<? extends ResultEntry> entries, final ResultEntry totalEntry,
      final File reportHtml, final boolean isSampleData,
      final boolean demultiplexingWithConflict)
      throws IOException, AozanException {

    Document doc = null;

    try {
      final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = null;
      docBuilder = dbfac.newDocumentBuilder();
      doc = docBuilder.newDocument();

    } catch (final ParserConfigurationException e) {
      throw new AozanException(e);
    }

    // Create the root element and add it to the document
    final Element root = doc.createElement("RecoveryClusterReport");
    root.setAttribute("formatversion", "1.0");
    doc.appendChild(root);

    XMLUtils.addTagValue(doc, root, "Step",
        "Recovery cluster from undetermined fastq file");

    // Common XML tag
    XMLUtilsWriter.buildXMLCommonTagHeader(doc, root, this.data);

    if (sampleId == -1) {

      XMLUtils.addTagValue(doc, root, "sampleName",
          "lane" + this.lane + "_undetermined");
      XMLUtils.addTagValue(doc, root, "description", null);
      XMLUtils.addTagValue(doc, root, "condition",
          "Compile results on recovery clusters in undetermined fastq with "
              + this.maxMismatches + " mismatch(es).");

      // Add sample name in this lane
      LaneResultEntry.samplesNameXML(doc, root, this.data, this.lane,
          demultiplexingWithConflict);

    } else {

      XMLUtils.addTagValue(doc, root, "sampleName",
          this.data.getSampleDemuxName(sampleId));
      XMLUtils.addTagValue(doc, root, "projectName",
          this.data.getProjectSample(sampleId));
      XMLUtils.addTagValue(doc, root, "description",
          this.data.getSampleDescription(sampleId));
      XMLUtils.addTagValue(doc, root, "condition",
          "Compile results on recovery clusters in undetermined fastq with "
              + this.maxMismatches + " mismatch(es).");
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

    for (final ResultEntry e : entries) {
      e.toXML(doc, results, "entry");
    }

    // Set xsl file to write report HTML file
    InputStream is = null;
    if (this.xslFile == null) {
      is = this.getClass()
          .getResourceAsStream(Globals.EMBEDDED_UNDETERMINED_XSL);
    } else {
      is = new FileInputStream(this.xslFile);
    }

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
  public static final int mismatches(final String a, final String b) {

    Preconditions.checkNotNull(a, "a cannot be null");
    Preconditions.checkNotNull(b, "b cannot be null");
    Preconditions.checkArgument(a.length() == b.length(),
        "The length of the 2 String must be equals (a=" + a + ", b=" + b + ")");

    final int len = a.length();
    int result = 0;

    for (int i = 0; i < len; i++) {
      if (a.charAt(i) != b.charAt(i)) {
        result++;
      }
    }

    return result;
  }

  /**
   * Reverse a map.
   * @param map the original map
   * @return a new map with the inversed key-values
   */
  private Map<String, Integer> reverse(final Map<Integer, String> map) {

    if (map == null) {
      return null;
    }

    final Map<String, Integer> result = new HashMap<>();

    for (final Map.Entry<Integer, String> e : map.entrySet()) {
      result.put(e.getValue(), e.getKey());
    }

    return result;
  }

  /**
   * Get a map with for each sample the index.
   * @return a Map object
   */
  private Map<Integer, String> getSampleIndexes() {

    final Map<Integer, String> result = new HashMap<>();

    for (final int sampleId : this.data.getSamplesInLane(this.lane)) {

      // Get the sample index
      final String index = this.data.getIndexSample(sampleId);

      result.put(sampleId, index);
    }

    return result;
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
  public UndeterminedIndexesProcessThread(final RunData data,
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

      this.seqFile = SequenceFactory.getSequenceFile(fastqSample.getFastqFiles()
          .toArray(new File[fastqSample.getFastqFiles().size()]));

    } catch (final IOException | SequenceFormatException e) {
      throw new AozanException(e);

    }

  }
}
