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

package fr.ens.biologie.genomique.aozan.fastqscreen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.io.FastqSample;
import fr.ens.biologie.genomique.aozan.util.XMLUtilsWriter;
import fr.ens.biologie.genomique.eoulsan.util.XMLUtils;

/**
 * This class manages results from fastqscreen.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenResult {

  // Legend for global value in fastqscreen
  private static final String PERCENT_MAPPED_NONE_GENOME =
      "percentMappedNoneGenome";
  private static final String PERCENT_MAPPED_AT_LEAST_ONE_GENOME =
      "percentMappedAtLeastOneGenome";
  private static final String PERCENT_MAPPED_EXCEPT_GENOME_SAMPLE =
      "mappedexceptgenomesample";

  private static final String HEADER_COLUMNS_TEXT =
      "Library \t %Mapped \t %Unmapped \t %One_hit_one_library"
          + "\t %Multiple_hits_one_library \t %One_hit_multiple_libraries \t "
          + "%Multiple_hits_multiple_libraries";

  private static final String HEADER_COLUMNS_TEXT_V2 =
      "Library\t#Reads_processed\t"
          + "#Unmapped\t%Unmapped\t"
          + "#One_hit_one_library\t%One_hit_one_library\t"
          + "#Multiple_hits_one_library\t%Multiple_hits_one_library\t"
          + "#One_hit_multiple_libraries\t%One_hit_multiple_libraries\t"
          + "Multiple_hits_multiple_libraries";

  private final Map<String, DataPerGenome> resultsPerGenome = new HashMap<>();
  private double percentUnmappedNoneGenome = 0.0;
  private double percentMappedAtLeastOneGenome = 0.0;
  private double percentMappedExceptGenomeSample = 0.0;
  private boolean isComputedPercent = false;

  private int readsMapped;
  private int readsprocessed;

  /**
   * Print table percent in format use by fastqscreen program with rounding
   * value.
   * @return string with results from fastqscreen
   */
  public String reportToCSV(final FastqSample fastqSample,
      final String genomeSample) throws AozanException {

    if (!this.isComputedPercent) {
      throw new AozanException(
          "Error writing a csv report fastqScreen : no values available.");
    }

    final StringBuilder s = new StringBuilder();

    s.append("#Fastq_screen version: 0.5.0\t#Reads in subset: ");
    s.append(this.readsprocessed);
    s.append("\n#FastqScreen : for Projet ");
    s.append(fastqSample.getProjectName());
    s.append(genomeSample == null
        ? "" : " (genome reference for sample " + genomeSample + ").");
    s.append("\n#Sample name: ");
    s.append(fastqSample.getSampleName());
    s.append(" on lane ");
    s.append(fastqSample.getLane());
    s.append("\n# Sample description: ");
    s.append(fastqSample.getDescription());

    s.append('\n');
    s.append(HEADER_COLUMNS_TEXT_V2);
    s.append('\n');

    for (final Map.Entry<String, DataPerGenome> e : this.resultsPerGenome
        .entrySet()) {
      s.append(e.getValue().getAllCountAndPercentValues());
      s.append("\n");
    }

    // add last lines for percentage of reads
    s.append('\n');
    s.append("%Hit_no_libraries: ");
    s.append(DataPerGenome.roundDouble(this.percentUnmappedNoneGenome));
    s.append('\n');
    return s.toString();
  }

  /**
   * Create report html file.
   * @param fastqSample fastqSample instance
   * @param data object rundata on the run
   * @param genomeSample genome reference corresponding to sample
   * @param fastqscreenXSLFile xsl file, can be null to use default file
   * @throws AozanException if an error occurs during creation document xml or
   *           transforming document xml in html file
   * @throws IOException if an error occurs during paring the xsl file
   */
  public void reportToHtml(final FastqSample fastqSample, final RunData data,
      final String genomeSample, final File reportHtml,
      final File fastqscreenXSLFile) throws AozanException, IOException {

    if (!this.isComputedPercent) {
      throw new AozanException(
          "Error writing a html report fastqScreen : no values available.");
    }

    if (reportHtml == null) {
      throw new AozanException(
          "Error writing a html report fastqScreen : no values available.");
    }

    // Create filename report xml, change extension from report html
    final File reportXML =
        new File(reportHtml.getAbsolutePath().replaceAll(".html$", ".xml"));

    // Call stylesheet file for report
    InputStream is = null;
    if (fastqscreenXSLFile == null) {
      is = this.getClass()
          .getResourceAsStream(Globals.EMBEDDED_FASTQSCREEN_XSL);
    } else {
      is = new FileInputStream(fastqscreenXSLFile);
    }

    // Create document XML
    final Document doc = createDocumentXML(fastqSample, data, genomeSample);

    // Create xml report
    XMLUtilsWriter.createXMLFile(doc, reportXML);

    // Create html report from xml with xsl file
    XMLUtilsWriter.createHTMLFileFromXSL(doc, is, reportHtml);

  }

  /**
   * Create document xml.
   * @param fastqSample fastq sample instance
   * @param data object rundata on the run
   * @param genomeSample genome reference for the sample
   * @return document xml completed
   * @throws AozanException if an error occurs during building document xml
   */
  private Document createDocumentXML(final FastqSample fastqSample,
      final RunData data, final String genomeSample) throws AozanException {

    final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = null;

    Document doc = null;

    try {
      docBuilder = dbfac.newDocumentBuilder();
      doc = docBuilder.newDocument();

      if (doc == null) {
        throw new AozanException(
            "Fastsqscreen : creating xml file, DocumentBuilder return null");
      }

    } catch (final ParserConfigurationException e) {
      throw new AozanException(e);
    }

    // Create the root element and add it to the document

    final Element root = doc.createElement("ReportFastqScreen");
    root.setAttribute("formatversion", "1.0");
    doc.appendChild(root);

    // Common tag header in document xml
    XMLUtilsWriter.buildXMLCommonTagHeader(doc, root, data);

    // Specific data on sample
    XMLUtils.addTagValue(doc, root, "projectName",
        fastqSample.getProjectName());
    XMLUtils.addTagValue(doc, root, "genomeSample",
        (genomeSample == null ? "no genome" : genomeSample));
    XMLUtils.addTagValue(doc, root, "sampleName", fastqSample.getSampleName());
    XMLUtils.addTagValue(doc, root, "descriptionSample",
        fastqSample.getDescription());
    XMLUtils.addTagValue(doc, root, "lane", "" + fastqSample.getLane());

    final Element report = doc.createElement("Report");
    root.appendChild(report);

    // Table - column
    final Element columns = doc.createElement("Columns");
    report.appendChild(columns);

    final String headerColumns = HEADER_COLUMNS_TEXT.replace('_', ' ');

    for (final String header : Splitter.on('\t').split(headerColumns)) {

      final Element columnElement = doc.createElement("Column");
      columnElement.setAttribute("name", header.trim());
      columnElement.setTextContent(header.trim());
      columns.appendChild(columnElement);

    }

    // Values per genome
    final Element genomes = doc.createElement("Genomes");
    report.appendChild(genomes);

    for (final Map.Entry<String, DataPerGenome> e : this.resultsPerGenome
        .entrySet()) {
      final String val = e.getValue().getAllPercentValues();

      final Element genome = doc.createElement("Genome");
      genomes.appendChild(genome);

      boolean first = true;

      for (final String value : Splitter.on('\t').split(val)) {
        if (first) {
          // Genome name (first column)
          genome.setAttribute("name", value.trim());
          first = false;
        } else {
          final Element columnElement = doc.createElement("Value");
          columnElement.setAttribute("name", value.trim());
          columnElement.setTextContent(value.trim());
          genome.appendChild(columnElement);
        }
      }
    }

    // Build report fastqscreen
    final Element unmappedElement = doc.createElement("ReadsUnmapped");
    unmappedElement.setAttribute("name", "reads_unmapped_none_genome");
    unmappedElement.setTextContent(Double
        .toString(DataPerGenome.roundDouble(this.percentUnmappedNoneGenome)));
    report.appendChild(unmappedElement);

    final Element mappedElement = doc.createElement("ReadsMappedOneGenome");
    mappedElement.setAttribute("name", "reads_mapped_at_least_one_genome");
    mappedElement.setTextContent(Double.toString(
        DataPerGenome.roundDouble(this.percentMappedAtLeastOneGenome)));
    report.appendChild(mappedElement);

    final Element mappedExceptGenomeElement =
        doc.createElement("ReadsMappedExceptGenomeSample");
    mappedExceptGenomeElement.setAttribute("name",
        "reads_mapped_at_except_genome_sample");
    mappedExceptGenomeElement.setTextContent(Double.toString(
        DataPerGenome.roundDouble(this.percentMappedExceptGenomeSample)));
    report.appendChild(mappedExceptGenomeElement);

    final Element readsMappedElement = doc.createElement("ReadsMapped");
    readsMappedElement.setAttribute("name", "reads_mapped");
    readsMappedElement.setTextContent(Integer.toString(this.readsMapped));
    report.appendChild(readsMappedElement);

    final Element readsProcessedElement = doc.createElement("ReadsProcessed");
    readsProcessedElement.setAttribute("name", "reads_processed");
    readsProcessedElement.setTextContent(Integer.toString(this.readsprocessed));
    report.appendChild(readsProcessedElement);

    return doc;
  }

  /**
   * Update the list of reference genome used by fastqscreen.
   * @param genome name of new reference genome
   * @param genomeSample genome reference corresponding to sample
   */
  public void addGenome(final String genome, final String genomeSample) {

    if (genome == null) {
      return;
    }

    if (!this.resultsPerGenome.containsKey(genome)) {
      this.resultsPerGenome.put(genome,
          new DataPerGenome(genome, genomeSample));
    }
  }

  /**
   * Count for each read number of hit per reference genome.
   * @param genome genome name
   * @param oneHit true if read mapped one time on genome else false
   * @param oneGenome true if read mapped on several genome else false
   */
  public void countHitPerGenome(final String genome, final boolean oneHit,
      final boolean oneGenome) {
    this.resultsPerGenome.get(genome).countHitPerGenome(oneHit, oneGenome);
  }

  /**
   * Convert values from fastqscreen in percentage.
   * @param readsMapped number reads mapped
   * @param readsprocessed number reads total
   * @throws AozanException if no value
   */
  public void countPercentValue(final int readsMapped, final int readsprocessed)
      throws AozanException {

    if (this.resultsPerGenome.isEmpty()) {
      throw new AozanException(
          "During fastqScreen execution: no genome receive");
    }
    this.readsMapped = readsMapped;
    this.readsprocessed = readsprocessed;

    double percentMappedOnlyOnGenomeSample = 0.0;

    // Convert value in percentage for all results of each genome
    for (final Map.Entry<String, DataPerGenome> e : this.resultsPerGenome
        .entrySet()) {
      e.getValue().countPercentValue(readsprocessed);

      percentMappedOnlyOnGenomeSample +=
          e.getValue().getPercentMappedOnlyOnGenomeSample();
    }

    // Convert value in percentage for the global values
    this.percentUnmappedNoneGenome =
        ((double) (readsprocessed - readsMapped)) / readsprocessed;

    this.percentMappedAtLeastOneGenome =
        Math.round((1.0 - this.percentUnmappedNoneGenome) * 100000.0)
            / 100000.0;

    this.percentMappedExceptGenomeSample = Math.round(
        (this.percentMappedAtLeastOneGenome - percentMappedOnlyOnGenomeSample)
            * 100000.0)
        / 100000.0;

    this.isComputedPercent = true;
  }

  /**
   * Update rundata with results from fastqscreen.
   * @param prefix name of sample
   * @throws AozanException if no value.
   */
  public RunData createRundata(final String prefix) throws AozanException {

    if (this.resultsPerGenome.isEmpty()) {
      throw new AozanException(
          "During fastqScreen execution : no genome receive");
    }

    if (!this.isComputedPercent) {
      throw new AozanException(
          "During fastqScreen execution : no value ​for genome");
    }

    final RunData data = new RunData();

    // Add in data all results for each genome
    for (final Map.Entry<String, DataPerGenome> e : this.resultsPerGenome
        .entrySet()) {
      e.getValue().updateRundata(data, prefix);
    }

    // Add global values of FastqScreen report
    data.put(prefix + "." + PERCENT_MAPPED_NONE_GENOME,
        this.percentUnmappedNoneGenome);
    data.put(prefix + "." + PERCENT_MAPPED_AT_LEAST_ONE_GENOME,
        this.percentMappedAtLeastOneGenome);
    data.put(prefix + "." + PERCENT_MAPPED_EXCEPT_GENOME_SAMPLE,
        this.percentMappedExceptGenomeSample);

    data.put(prefix + ".read.mapped.count", this.readsMapped);
    data.put(prefix + ".read.processed.count", this.readsprocessed);

    return data;
  }

  public int getCountReadsMapped() {
    return this.readsMapped;
  }

  public int getCountReadsProcessed() {
    return this.readsprocessed;
  }

  //
  // Internal class
  //

  /**
   * This internal class saves values of fastqscreen from one reference genome.
   * @author Sandrine Perrin
   */
  private static class DataPerGenome {

    private final String genome;
    private boolean isGenomeSample;

    // Specific legend : represent key in rundata
    private static final String UN_MAPPED_LEGEND = "unmapped";
    private static final String ONE_HIT_ONE_LIBRARY_LEGEND =
        "one.hit.one.library";
    private static final String MULTIPLE_HITS_ONE_LIBRARY_LEGEND =
        "multiple.hits.one.library";
    private static final String ONE_HIT_MULTIPLE_LIBRARIES_LEGEND =
        "one.hit.multiple.libraries";
    private static final String MULTIPLE_HITS_MULTIPLE_LIBRARIES_LEGEND =
        "multiple.hits.multiple.libraries";
    private static final String MAPPED_LEGEND = "mapped";

    private double oneHitOneLibraryPercent = 0.0;
    private double multipleHitsOneLibraryPercent = 0.0;
    private double oneHitMultipleLibrariesPercent = 0.0;
    private double multipleHitsMultipleLibrariesPercent = 0.0;
    private double unMappedPercent = 0.0;
    private double mappedPercent = 0.0;

    private int readCount;
    private int mappedCount;
    private int unmappedCount;
    private int oneHitOneLibraryCount = 0;
    private int multipleHitsOneLibraryCount = 0;
    private int oneHitMultipleLibrariesCount = 0;
    private int multipleHitsMultipleLibrariesCount = 0;

    /**
     * Count for each read number of hit per reference genome.
     * @param oneHit true if read mapped one time on genome else false
     * @param oneGenome true if read mapped on several genome else false
     */
    void countHitPerGenome(final boolean oneHit, final boolean oneGenome) {

      if (oneHit && oneGenome) {
        this.oneHitOneLibraryCount++;

      } else if (!oneHit && oneGenome) {
        this.multipleHitsOneLibraryCount++;

      } else if (oneHit && !oneGenome) {
        this.oneHitMultipleLibrariesCount++;

      } else if (!oneHit && !oneGenome) {
        this.multipleHitsMultipleLibrariesCount++;
      }
    }

    /**
     * Convert values from fastqscreen in percentage.
     * @param readCount number reads total
     */
    void countPercentValue(final int readCount) {

      this.readCount = readCount;
      final double readsprocessed = readCount * 1.0;
      this.oneHitOneLibraryPercent =
          this.oneHitOneLibraryCount / readsprocessed;
      this.multipleHitsOneLibraryPercent =
          this.multipleHitsOneLibraryCount / readsprocessed;
      this.oneHitMultipleLibrariesPercent =
          this.oneHitMultipleLibrariesCount / readsprocessed;
      this.multipleHitsMultipleLibrariesPercent =
          this.multipleHitsMultipleLibrariesCount / readsprocessed;

      this.mappedCount = this.oneHitOneLibraryCount
          + this.multipleHitsOneLibraryCount + this.oneHitMultipleLibrariesCount
          + this.multipleHitsMultipleLibrariesCount;
      this.unmappedCount = this.readCount - this.mappedCount;
      this.mappedPercent = this.mappedCount / readsprocessed;

      this.unMappedPercent = 1.0 - this.mappedPercent;

    }

    /**
     * Get string with all values rounded.
     * @return string
     */
    String getAllPercentValues(final int lengthName) {
      return writeGenomeName(this.genome, lengthName)
          + "\t" + roundDouble(this.mappedPercent) + "\t"
          + roundDouble(this.unMappedPercent) + "\t"
          + roundDouble(this.oneHitOneLibraryPercent) + "\t"
          + roundDouble(this.multipleHitsOneLibraryPercent) + "\t"
          + roundDouble(this.oneHitMultipleLibrariesPercent) + "\t"
          + roundDouble(this.multipleHitsMultipleLibrariesPercent);
    }

    /**
     * Get string with all values rounded.
     * @return string
     */
    String getAllCountAndPercentValues() {
      return this.genome
          + "\t" + this.readCount + "\t" + this.unmappedCount + "\t"
          + roundDouble(this.unMappedPercent) + "\t"
          + this.oneHitOneLibraryCount + "\t"
          + roundDouble(this.oneHitOneLibraryPercent) + "\t"
          + this.multipleHitsOneLibraryCount + "\t"
          + roundDouble(this.multipleHitsOneLibraryPercent) + "\t"
          + this.oneHitMultipleLibrariesCount + "\t"
          + roundDouble(this.oneHitMultipleLibrariesPercent) + "\t"
          + this.multipleHitsMultipleLibrariesCount + "\t"
          + roundDouble(this.multipleHitsMultipleLibrariesPercent);
    }

    String getAllPercentValues() {
      return getAllPercentValues(-1);
    }

    /**
     * Rounding in double.
     * @param n double
     * @return double value rounded
     */
    private static double roundDouble(final double n) {
      return Math.rint(n * 10000.0) / 100.0;
    }

    /**
     * Update rundata with result from fastqscreen.
     * @param data rundata
     * @param prefix name of sample
     */
    public void updateRundata(final RunData data, final String prefix) {
      // add line in RunData
      data.put(prefix + "." + this.genome + "." + MAPPED_LEGEND + ".percent",
          this.mappedPercent);
      data.put(prefix + "." + this.genome + "." + UN_MAPPED_LEGEND + ".percent",
          this.unMappedPercent);
      data.put(prefix
          + "." + this.genome + "." + ONE_HIT_ONE_LIBRARY_LEGEND + ".percent",
          this.oneHitOneLibraryPercent);
      data.put(prefix
          + "." + this.genome + "." + MULTIPLE_HITS_ONE_LIBRARY_LEGEND
          + ".percent", this.multipleHitsOneLibraryPercent);
      data.put(prefix
          + "." + this.genome + "." + ONE_HIT_MULTIPLE_LIBRARIES_LEGEND
          + ".percent", this.oneHitMultipleLibrariesPercent);
      data.put(
          prefix
              + "." + this.genome + "."
              + MULTIPLE_HITS_MULTIPLE_LIBRARIES_LEGEND + ".percent",
          this.multipleHitsMultipleLibrariesPercent);

    }

    /**
     * Retrieve the percent of reads which mapped only on genome sample, zero if
     * genome is not the genome sample.
     * @return percent
     */
    double getPercentMappedOnlyOnGenomeSample() {

      if (this.isGenomeSample) {

        return this.oneHitOneLibraryPercent
            + this.multipleHitsOneLibraryPercent;
      }
      return 0.0;
    }

    /**
     * Extends the genome name with "_" to the length max for writing in report.
     * @param genomeName name of genome.
     * @param lengthFinal final length expected
     * @return string of length equal to lengthFinal
     */
    private String writeGenomeName(final String genomeName,
        final int lengthFinal) {

      if (lengthFinal == -1) {
        return genomeName;
      }

      final StringBuilder s = new StringBuilder();
      s.append(genomeName);
      int length = genomeName.length();
      while (length < lengthFinal) {
        s.append('_');
        length++;
      }

      return s.toString();
    }

    //
    // Constructor
    //

    /**
     * Constructor for DataPerGenome.
     * @param genome genome name
     * @param genomeSample genome reference corresponding to sample
     */
    DataPerGenome(final String genome, final String genomeSample) {
      this.genome = genome;

      if (genomeSample != null) {
        this.isGenomeSample = genome.equals(genomeSample);
      }
    }
  }

}
