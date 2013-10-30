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

package fr.ens.transcriptome.aozan.fastqscreen;

import static fr.ens.transcriptome.aozan.Globals.DATE_FORMAT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

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

  private Map<String, DataPerGenome> resultsPerGenome =
      new HashMap<String, DataPerGenome>();
  private double percentUnmappedNoneGenome = 0.0;
  private double percentMappedAtLeastOneGenome = 0.0;
  private double percentMappedExceptGenomeSample = 0.0;
  private boolean isComputedPercent = false;

  private int readsMapped;
  private int readsprocessed;

  /**
   * Print table percent in format use by fastqscreen program with rounding
   * value
   * @return string with results from fastqscreen
   */
  public String reportToCSV(final FastqSample fastqSample,
      final String genomeSample) throws AozanException {

    if (!isComputedPercent)
      throw new AozanException(
          "Error writing a csv report fastqScreen : no values available.");

    StringBuilder s = new StringBuilder();

    s.append("FastqScreen : for Projet "
        + fastqSample.getProjectName()
        + (genomeSample == null ? "" : " (genome reference for sample "
            + genomeSample) + ").\nresult for sample : "
        + fastqSample.getSampleName());

    s.append("\n");
    s.append("\n" + HEADER_COLUMNS_TEXT + "\n");

    // length max genome
    int nameLength = lengthMaxGenomeName(this.resultsPerGenome.keySet());

    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
      s.append(e.getValue().getAllPercentValues(nameLength) + "\n");
    }

    // add last lines for percentage of reads
    s.append("\n");
    s.append("% reads_unmapped_none_genome : "
        + DataPerGenome.roundDouble(this.percentUnmappedNoneGenome));
    s.append("\n");
    s.append("% reads_mapped_at_least_one_genome : "
        + DataPerGenome.roundDouble(this.percentMappedAtLeastOneGenome));
    s.append("\n");
    s.append("% reads_mapped_except_genome_sample : "
        + DataPerGenome.roundDouble(this.percentMappedExceptGenomeSample));
    s.append("\n");

    s.append("reads mapped "
        + this.readsMapped + " / reads processed " + this.readsprocessed);
    return s.toString();
  }

  public String reportToHtml(final FastqSample fastqSample, final RunData data,
      final String genomeSample, final File fastqscreenXSLFile)
      throws AozanException {

    if (!isComputedPercent)
      throw new AozanException(
          "Error writing a html report fastqScreen : no values available.");

    // Call stylesheet file for report
    InputStream is = null;
    try {
      if (fastqscreenXSLFile == null)
        is =
            this.getClass().getResourceAsStream(
                Globals.EMBEDDED_FASTQSCREEN_XSL);
      else
        is = new FileInputStream(fastqscreenXSLFile);

    } catch (FileNotFoundException e) {
      throw new AozanException(e);

    } finally {
      try {
        if (is != null)
          is.close();
      } catch (IOException e) {
      }
    }

    return toXML(fastqSample, data, genomeSample, is);
  }

  private String toXML(final FastqSample fastqSample, final RunData data,
      final String genomeSample, final InputStream isXSLFile)
      throws AozanException {

    if (!isComputedPercent)
      return null;

    try {
      // set up a transformer
      TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer trans = transfac.newTransformer();
      // trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      trans.setOutputProperty(OutputKeys.INDENT, "yes");
      trans.setOutputProperty(OutputKeys.METHOD, "xml");

      // create string from xml tree
      StringWriter sw = new StringWriter();
      StreamResult result = new StreamResult(sw);

      Document doc = createDocumentXML(fastqSample, data, genomeSample);
      DOMSource source = new DOMSource(doc);
      trans.transform(source, result);
      // Create the transformer
      final Transformer transformer =
          TransformerFactory.newInstance().newTransformer(
              new javax.xml.transform.stream.StreamSource(isXSLFile));

      // Create the String writer
      final StringWriter writer = new StringWriter();

      // Transform the document
      transformer.transform(new DOMSource(doc), new StreamResult(writer));

      // Close input stream
      isXSLFile.close();

      // Print document XML
      // StringWriter swxml = new StringWriter();
      // StreamResult resultxml = new StreamResult(swxml);
      // DOMSource sourcexml = new DOMSource(doc);
      // trans.transform(sourcexml, resultxml);
      // System.out.println(swxml.toString());

      // Return the result of the transformation
      return writer.toString();

    } catch (TransformerException e) {
      throw new AozanException(e);
    } catch (IOException e) {
      throw new AozanException(e);
    }

  }

  private Document createDocumentXML(final FastqSample fastqSample,
      final RunData data, final String genomeSample) throws AozanException {
    DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();

    DocumentBuilder docBuilder = null;
    Document doc = null;

    try {
      docBuilder = dbfac.newDocumentBuilder();
      doc = docBuilder.newDocument();

      if (doc == null)
        throw new AozanException(
            "Fastsqscreen : creating xml file, DocumentBuilder return null");

    } catch (ParserConfigurationException e) {
      throw new AozanException(e);
    }

    // Create the root element and add it to the document

    Element root = doc.createElement("ReportFastqScreen");
    root.setAttribute("formatversion", "1.0");
    doc.appendChild(root);

    // Header
    XMLUtils.addTagValue(doc, root, "AozanStep", "Detection contamination");
    XMLUtils.addTagValue(doc, root, "GeneratorName", Globals.APP_NAME);
    XMLUtils.addTagValue(doc, root, "GeneratorVersion",
        Globals.APP_VERSION_STRING);
    XMLUtils.addTagValue(doc, root, "GeneratorWebsite", Globals.WEBSITE_URL);
    XMLUtils.addTagValue(doc, root, "GeneratorRevision",
        Globals.APP_BUILD_COMMIT);

    XMLUtils.addTagValue(doc, root, "RunId", data.get("run.info.run.id"));
    XMLUtils.addTagValue(doc, root, "RunDate", data.get("run.info.date"));
    XMLUtils.addTagValue(doc, root, "FlowcellId",
        data.get("run.info.flow.cell.id"));
    XMLUtils.addTagValue(doc, root, "InstrumentSN",
        data.get("run.info.instrument"));
    XMLUtils.addTagValue(doc, root, "InstrumentRunNumber",
        data.get("run.info.run.number"));
    XMLUtils.addTagValue(doc, root, "ReportDate",
        DATE_FORMAT.format(new Date()));

    XMLUtils
        .addTagValue(doc, root, "projectName", fastqSample.getProjectName());
    XMLUtils.addTagValue(doc, root, "genomeSample", (genomeSample == null
        ? "no genome" : genomeSample));
    XMLUtils.addTagValue(doc, root, "sampleName", fastqSample.getSampleName());

    final Element report = doc.createElement("Report");
    root.appendChild(report);

    // Table - column
    final Element columns = doc.createElement("Columns");
    report.appendChild(columns);

    final String headerColumns = HEADER_COLUMNS_TEXT.replace('_', ' ');

    for (String header : Splitter.on('\t').split(headerColumns)) {

      final Element columnElement = doc.createElement("Column");
      columnElement.setAttribute("name", header.trim());
      columnElement.setTextContent(header.trim());
      columns.appendChild(columnElement);

    }

    // Values per genome
    final Element genomes = doc.createElement("Genomes");
    report.appendChild(genomes);

    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
      String val = e.getValue().getAllPercentValues();

      final Element genome = doc.createElement("Genome");
      genomes.appendChild(genome);

      boolean first = true;

      for (String value : Splitter.on('\t').split(val)) {
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

    // Bilan report fastqscreen
    final Element unmappedElement = doc.createElement("ReadsUnmapped");
    unmappedElement.setAttribute("name", "reads_unmapped_none_genome");
    unmappedElement.setTextContent(Double.toString(DataPerGenome
        .roundDouble(this.percentUnmappedNoneGenome)));
    report.appendChild(unmappedElement);

    final Element mappedElement = doc.createElement("ReadsMappedOneGenome");
    mappedElement.setAttribute("name", "reads_mapped_at_least_one_genome");
    mappedElement.setTextContent(Double.toString(DataPerGenome
        .roundDouble(this.percentMappedAtLeastOneGenome)));
    report.appendChild(mappedElement);

    final Element mappedExceptGenomeElement =
        doc.createElement("ReadsMappedExceptGenomeSample");
    mappedExceptGenomeElement.setAttribute("name",
        "reads_mapped_at_except_genome_sample");
    mappedExceptGenomeElement.setTextContent(Double.toString(DataPerGenome
        .roundDouble(this.percentMappedExceptGenomeSample)));
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

    if (genome == null)
      return;

    if (!this.resultsPerGenome.containsKey(genome))
      this.resultsPerGenome
          .put(genome, new DataPerGenome(genome, genomeSample));
  }

  /**
   * Count for each read number of hit per reference genome
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

    if (this.resultsPerGenome.isEmpty())
      throw new AozanException(
          "During fastqScreen execusion : no genome receive");
    this.readsMapped = readsMapped;
    this.readsprocessed = readsprocessed;

    double percentMappedOnlyOnGenomeSample = 0.0;

    // Convert value in percentage for all results of each genome
    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
      e.getValue().countPercentValue(readsprocessed);

      percentMappedOnlyOnGenomeSample +=
          e.getValue().getPercentMappedOnlyOnGenomeSample();
    }

    // Convert value in percentage for the global values
    this.percentUnmappedNoneGenome =
        ((double) (readsprocessed - readsMapped)) / readsprocessed;
    this.percentMappedAtLeastOneGenome = 1.0 - this.percentUnmappedNoneGenome;

    this.percentMappedExceptGenomeSample =
        percentMappedAtLeastOneGenome - percentMappedOnlyOnGenomeSample;

    isComputedPercent = true;
  }

  /**
   * Update rundata with results from fastqscreen
   * @param data rundata
   * @param prefix name of sample
   * @throws AozanException if no value.
   */
  public RunData createRundata(final String prefix) throws AozanException {

    if (this.resultsPerGenome.isEmpty())
      throw new AozanException(
          "During fastqScreen execution : no genome receive");

    if (!isComputedPercent)
      throw new AozanException(
          "During fastqScreen execution : no value ​for genome");

    RunData data = new RunData();

    // Add in data all results for each genome
    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
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

  private int lengthMaxGenomeName(final Set<String> genomesName) {
    int length = 0;
    for (String s : genomesName)
      length = s.length() > length ? s.length() : length;

    return length;

  }

  //
  // Internal class
  //

  /**
   * This internal class saves values of fastqscreen from one reference genome.
   * @author Sandrine Perrin
   */
  private static class DataPerGenome {

    private String genome;
    private boolean isGenomeSample;

    // Specific legend : represent key in rundata
    private String unMappedLegend = "unmapped";
    private String oneHitOneLibraryLegend = "one.hit.one.library";
    private String multipleHitsOneLibraryLegend = "multiple.hits.one.library";
    private String oneHitMultipleLibrariesLegend = "one.hit.multiple.libraries";
    private String multipleHitsMultipleLibrariesLegend =
        "multiple.hits.multiple.libraries";
    private String mappedLegend = "mapped";

    private double oneHitOneLibraryPercent = 0.0;
    private double multipleHitsOneLibraryPercent = 0.0;
    private double oneHitMultipleLibrariesPercent = 0.0;
    private double multipleHitsMultipleLibrariesPercent = 0.0;
    private double unMappedPercent = 0.0;
    private double mappedPercent = 0.0;

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
        oneHitOneLibraryCount++;

      } else if (!oneHit && oneGenome) {
        multipleHitsOneLibraryCount++;

      } else if (oneHit && !oneGenome) {
        oneHitMultipleLibrariesCount++;

      } else if (!oneHit && !oneGenome) {
        multipleHitsMultipleLibrariesCount++;
      }
    }

    /**
     * Convert values from fastqscreen in percentage.
     * @param readsprocessed number reads total
     */
    void countPercentValue(int readsprocessed) {
      this.oneHitOneLibraryPercent =
          (double) this.oneHitOneLibraryCount / readsprocessed;
      this.multipleHitsOneLibraryPercent =
          (double) this.multipleHitsOneLibraryCount / readsprocessed;
      this.oneHitMultipleLibrariesPercent =
          (double) this.oneHitMultipleLibrariesCount / readsprocessed;
      this.multipleHitsMultipleLibrariesPercent =
          (double) this.multipleHitsMultipleLibrariesCount / readsprocessed;

      this.mappedPercent =
          (double) (this.oneHitOneLibraryCount
              + this.multipleHitsOneLibraryCount
              + this.oneHitMultipleLibrariesCount + this.multipleHitsMultipleLibrariesCount)
              / readsprocessed;

      this.unMappedPercent = 1.0 - mappedPercent;

    }

    /**
     * Get string with all values rounded.
     * @return string
     */
    String getAllPercentValues(final int lengthName) {
      return writeGenomeName(genome, lengthName)
          + "\t" + roundDouble(this.mappedPercent) + "\t"
          + roundDouble(this.unMappedPercent) + "\t"
          + roundDouble(this.oneHitOneLibraryPercent) + "\t"
          + roundDouble(this.multipleHitsOneLibraryPercent) + "\t"
          + roundDouble(this.oneHitMultipleLibrariesPercent) + "\t"
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
    private static double roundDouble(double n) {
      return Math.rint(n * 10000.0) / 100.0;
    }

    /**
     * Update rundata with result from fastqscreen
     * @param data rundata
     * @param prefix name of sample
     */
    public void updateRundata(final RunData data, final String prefix) {
      // add line in RunData
      data.put(prefix + "." + genome + "." + mappedLegend + ".percent",
          this.mappedPercent);
      data.put(prefix + "." + genome + "." + unMappedLegend + ".percent",
          this.unMappedPercent);
      data.put(prefix
          + "." + genome + "." + oneHitOneLibraryLegend + ".percent",
          this.oneHitOneLibraryPercent);
      data.put(prefix
          + "." + genome + "." + multipleHitsOneLibraryLegend + ".percent",
          this.multipleHitsOneLibraryPercent);
      data.put(prefix
          + "." + genome + "." + oneHitMultipleLibrariesLegend + ".percent",
          this.oneHitMultipleLibrariesPercent);
      data.put(prefix
          + "." + genome + "." + multipleHitsMultipleLibrariesLegend
          + ".percent", this.multipleHitsMultipleLibrariesPercent);

    }

    /**
     * Retrieve the percent of reads which mapped only on genome sample, zero if
     * genome is not the genome sample
     * @param readsprocessed number reads total
     * @return percent
     */
    double getPercentMappedOnlyOnGenomeSample() {

      if (isGenomeSample) {

        double readsMappedOnlyOnGenomeSample =
            oneHitOneLibraryPercent + multipleHitsOneLibraryPercent;

        return readsMappedOnlyOnGenomeSample;
      }
      return 0.0;
    }

    /**
     * Extends the genome name with "_" to the length max for writing in report.
     * @param genomeName name of genome
     * @param lengthFinal final length expected
     * @return string of length equal to lengthFinal
     */
    private String writeGenomeName(final String genomeName,
        final int lengthFinal) {

      if (lengthFinal == -1)
        return genomeName;

      StringBuilder s = new StringBuilder();
      s.append(genomeName);
      int length = genomeName.length();
      while (length < lengthFinal) {
        s.append("_");
        length++;
      }

      return s.toString();
    }

    //
    // Constructor
    //

    /**
     * Constructor for DataPerGenome
     * @param genome genome name
     * @param genomeSample genome reference corresponding to sample
     */
    DataPerGenome(final String genome, final String genomeSample) {
      this.genome = genome;
      this.isGenomeSample = genome.equals(genomeSample);
    }
  }

}
