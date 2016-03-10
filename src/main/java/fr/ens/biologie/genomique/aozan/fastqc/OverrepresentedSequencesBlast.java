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

package fr.ens.biologie.genomique.aozan.fastqc;

import static fr.ens.biologie.genomique.aozan.util.StringUtils.stackTraceToString;
import static fr.ens.biologie.genomique.aozan.util.XMLUtilsParser.extractFirstValueToInt;
import static fr.ens.biologie.genomique.aozan.util.XMLUtilsParser.extractFirstValueToString;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;
import uk.ac.babraham.FastQC.Sequence.Contaminant.ContaminantHit;

/**
 * This class launches blastn on one sequence query and parses xml result file
 * to identify the best hit.
 * @since 1.2
 * @author Sandrine Perrin
 */
public class OverrepresentedSequencesBlast {

  /** LOGGER. */
  private static final Logger LOGGER = Common.getLogger();

  private static final String SEQUENCES_NOT_USE_FILE =
      "/sequences_nohit_with_blastn.txt";
  private static final String PREFIX_FILENAME_DATABASE = "/nt";

  private static final boolean BLAST_RESULT_HTML_TYPE = true;

  private static final Object LOCK = new Object();

  // Tag configuration general of blast
  private static final String tag_queryLength = "Iteration_query-len";
  private static final String tag_blastVersion = "BlastOutput_version";

  private static volatile OverrepresentedSequencesBlast singleton;

  // Save sequence and result blast for the run
  private final Map<String, BlastResultHit> sequencesAlreadyAnalysis =
      new HashMap<>();

  private boolean configured;
  private String tmpPath;

  private boolean firstCall = true;

  private CommandLine blastCommonCommandLine;
  private String blastVersionExpected;

  //
  // Configuration
  //

  /**
   * Configure and check that Blastn can be launched.
   * @param properties object with the collector configuration
   */
  public void configure(final Properties properties) {

    checkNotNull(properties, "properties argument cannot be null");

    if (this.configured) {
      return;
    }

    boolean stepEnabled = Boolean.parseBoolean(
            properties.getProperty(Settings.QC_CONF_FASTQSCREEN_BLAST_ENABLE_KEY)
                    .trim().toLowerCase());

    if (stepEnabled) {

      // Check parameters
      this.blastVersionExpected = properties
          .getProperty(Settings.QC_CONF_FASTQSCREEN_BLAST_VERSION_EXPECTED_KEY);

      this.tmpPath = properties.getProperty(QC.TMP_DIR);

      // Path to blast executable
      final String blastPath = properties
          .getProperty(Settings.QC_CONF_FASTQSCREEN_BLAST_PATH_KEY).trim();

      // Path to database for blast, need to add prefix filename used "nt"
      final String blastDBPath =
          properties.getProperty(Settings.QC_CONF_FASTQSCREEN_BLAST_DB_PATH_KEY)
              .trim() + PREFIX_FILENAME_DATABASE;

      // Check paths needed in configuration aozan
      if (blastPath == null
          || blastPath.isEmpty() || blastDBPath == null
          || blastDBPath.isEmpty()) {
        LOGGER.warning("Empty or incompleted configuration for Blast detected."
            + " Blast will not be used");
        stepEnabled = false;
      }

      // Check if blast is installed
      if (!new File(blastPath).exists()) {
        LOGGER
            .warning("Blast executable is not installed at the following path: "
                + blastPath);
        stepEnabled = false;
      }

      if (stepEnabled) {

        try {
          // Add arguments from configuration Aozan
          final String blastArguments = properties
              .getProperty(Settings.QC_CONF_FASTQSCREEN_BLAST_ARGUMENTS_KEY);

          this.blastCommonCommandLine =
              createBlastCommandLine(blastPath, blastDBPath, blastArguments);

          // Add in map all sequences to do not analysis, return a resultBlast
          // with no hit
          loadSequencesToIgnore();

          LOGGER.info("FastQC: blast is enabled, command line = "
              + this.blastCommonCommandLine);

        } catch (final IOException | AozanException e) {
          LOGGER.warning(e.getMessage() + '\n' + stackTraceToString(e));
          stepEnabled = false;

        }
      }
    }

    this.configured = true;
  }

  /**
   * Add in hashMap all sequences identified like to fail blastn analysis for
   * skipping them.
   */
  private void loadSequencesToIgnore() {

    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(
          this.getClass().getResourceAsStream(SEQUENCES_NOT_USE_FILE),
          Globals.DEFAULT_FILE_ENCODING));

      String sequence = "";

      while ((sequence = br.readLine()) != null) {

        if (!sequence.startsWith("#")) {
          if (!sequence.isEmpty()) {
            this.sequencesAlreadyAnalysis.put(sequence,
                new BlastResultHit(sequence));
          }
        }

      }
      br.close();

    } catch (final IOException e) {

    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (final IOException e) {
        }
      }
    }

  }

  //
  // Methods to build command line
  //

  private CommandLine createBlastCommandLine(final String blastPath,
      final String blastDBPath, final String blastArguments)
          throws IOException, AozanException {

    if (blastPath.endsWith("blastall")) {
      return new BlastallCommandLine(blastPath, blastDBPath, blastArguments);
    }

    if (blastPath.endsWith("blastn")) {
      return new NbciBlastnCommandLine(blastPath, blastDBPath, blastArguments);
    }

    throw new AozanException(
        "Fail to build command line, only blastn (from ncbi-blast+) or blastall application are supported.");
  }

  //
  //
  // Methods to analysis sequences
  //

  /**
   * Launch blastn and parse result xml file for identify the best hit.
   * @param sequence query blastn
   * @return a ContaminantHit for the best hit return by blastn or null
   */
  public synchronized ContaminantHit blastSequence(final String sequence)
      throws IOException, AozanException {

    checkNotNull(sequence, "sequence argument cannot be null");

    // Test if the instance has been configured
    if (!this.configured) {
      throw new AozanRuntimeException(
          "OverrepresentedSequencesBlast is not configured");
    }

    final BlastResultHit blastResult;

    // Check if new sequence
    if (this.sequencesAlreadyAnalysis.containsKey(sequence)) {
      // The sequence has been already blasted
      blastResult = this.sequencesAlreadyAnalysis.get(sequence);
    } else {

      // The sequence has not been blasted
      blastResult = blastNewSequence(sequence);

      // Save sequence in multithreading context
      this.sequencesAlreadyAnalysis.put(sequence, blastResult);

    }

    if (blastResult == null || blastResult.isNull()) {
      // No hit found
      return null;
    }

    return blastResult.toContaminantHit();
  }

  /**
   * Blast a sequence that has not been already blasted.
   * @param sequence sequence to blast
   * @return a BlastResultHit object
   * @throws IOException if an error occurs while blasting
   * @throws AozanException if an error occurs while blasting
   */
  private BlastResultHit blastNewSequence(final String sequence)
      throws IOException, AozanException {

    LOGGER.fine("FastQC: Blast sequence: " + sequence);

    File resultXMLFile = null;
    BlastResultHit blastResult = null;

    // Create temporary file
    resultXMLFile = FileUtils.createTempFile(new File(this.tmpPath), "blast_",
        "_result.xml");

    // Check if the temporary file already exists
    checkExistingFile(resultXMLFile,
        "FastQC: path of the Blast output file for the sequence " + sequence);

    // Set output file for this sequence
    this.blastCommonCommandLine.setOutputFile(resultXMLFile);

    // Launch blast
    launchBlastSearch(this.blastCommonCommandLine.getComandLine(), sequence);

    // Wait writing xml file
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e) {
      // Do nothing
    }

    // Parse result file if not empty
    if (resultXMLFile.length() > 0) {
      blastResult = parseDocument(resultXMLFile, sequence);
    }

    // Remove XML file
    if (resultXMLFile.exists()) {
      if (!resultXMLFile.delete()) {
        LOGGER.warning("FastQC-step: Cannot delete the Blast xml output file "
            + resultXMLFile.getAbsolutePath());
      }
    }
    return blastResult;

  }

  /**
   * Process blastn, one instance at the same time.
   * @param cmd command line
   * @param sequence query blastn
   * @throws AozanException occurs if the process fails
   */
  private static void launchBlastSearch(final List<String> cmd,
      final String sequence) throws AozanException {

    final ProcessBuilder builder = new ProcessBuilder(cmd);

    Process process;
    try {

      process = builder.start();

      // Writing on standard input
      final Writer os = new OutputStreamWriter(process.getOutputStream(),
          Globals.DEFAULT_FILE_ENCODING);
      os.write(sequence);
      os.flush();
      os.close();

      final int exitValue = process.waitFor();
      if (exitValue > 0) {
        LOGGER.warning("FastQC: fail of blastn process for sequence "
            + sequence + ", exit value is : " + exitValue);
      }

    } catch (final IOException | InterruptedException e) {
      throw new AozanException(e);
    }
  }

  //
  // Methods to parsing xml file.
  //

  /**
   * Parse xml file result to identify the best hit.
   * @param resultXML result file from blastn
   * @param sequence query blastn
   * @return the best hit or null
   * @throws AozanException occurs if the parsing fails.
   */
  private BlastResultHit parseDocument(final File resultXML,
      final String sequence) throws AozanException {

    InputStream is = null;
    try {
      checkExistingFile(resultXML, "FastQC: Blast xml query result");

      // Create the input stream
      is = new FileInputStream(resultXML);

      // Read the XML file
      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      // Parse general information on blastn
      parseHeaderDocument(doc);

      is.close();
      // Search the best hit
      return parseHit(doc, sequence);

    } catch (final IOException | SAXException | ParserConfigurationException e) {
      throw new AozanException(e);
    } finally {
      try {
        is.close();
      } catch (final IOException e) {
      }
    }
  }

  /**
   * Retrieve general informations of blastn, call once.
   * @param doc root of xml file result for blastn
   */
  private void parseHeaderDocument(final Document doc) {

    if (this.firstCall) {

      final String version = extractFirstValueToString(doc, tag_blastVersion);

      // Check version xml file
      if (!this.blastVersionExpected.equals(version)) {
        LOGGER.warning("FastQC: the blast  version in the xml output file ("
            + version + ") is not the expected version ("
            + this.blastVersionExpected + ")");
      } else {
        LOGGER.info("FastQC-step blastn : blast version " + version);
      }

      final String parameters = "Parameters_expect=" +
              extractFirstValueToString(doc, "Parameters_expect") +
              ", Parameters_sc-match=" +
              extractFirstValueToString(doc, "Parameters_sc-match") +
              ", Parameters_sc-mismatch="
              +
              extractFirstValueToString(doc, "Parameters_sc-mismatch") +
              ", Parameters_gap-open=" +
              extractFirstValueToString(doc, "Parameters_gap-open") +
              ", Parameters_gap-extend="
              +
              extractFirstValueToString(doc, "Parameters_gap-extend") +
              ", Parameters_filter=" +
              extractFirstValueToString(doc, "Parameters_filter");

      LOGGER.info("FastQC-step blast parameters : " + parameters);

      this.firstCall = false;
    }
  }

  /**
   * Search hit in xml result file.
   * @param doc root of the xml file
   * @param sequence query blastn
   * @return best hit contained in xml file or null
   */
  private BlastResultHit parseHit(final Document doc, final String sequence) {

    final List<Element> responses =
        XMLUtils.getElementsByTagName(doc, "Iteration");
    final Element elemIteration = responses.get(0);

    final int queryLength =
        extractFirstValueToInt(elemIteration, tag_queryLength);

    final List<Element> hits =
        XMLUtils.getElementsByTagName(elemIteration, "Hit");
    final int countHits = hits.size();

    // No hit found
    if (countHits == 0) {
      return null;
    }

    // Parse the first hit to build result
    return new BlastResultHit(hits.get(0), countHits, queryLength, sequence,
        BLAST_RESULT_HTML_TYPE);

  }

  //
  // Singleton method
  //

  /**
   * Get the singleton instance.
   * @return the unique instance of the class
   */
  public static final synchronized OverrepresentedSequencesBlast getInstance() {

    if (singleton == null) {
      synchronized (LOCK) {
        singleton = new OverrepresentedSequencesBlast();
      }
    }

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private OverrepresentedSequencesBlast() {
  }

  //
  // Internal classes
  //

  /**
   * This abstract class store a command line to run blastn.
   */
  private abstract static class CommandLine {

    /** Result file must be XML. */
    private static final String EVALUE_ARGUMENT = "0.0001";

    private final String blastPath;
    private final String blastDBPath;
    private final List<String> argBlast;
    private File outputFile = null;

    //
    // Abstract methods
    //

    abstract String getProgramNameArgumentName();

    abstract String getDatabaseArgumentName();

    abstract String getAlignementViewOptionsArgumentName();

    abstract String getAlignementViewOptionsArgumentValue();

    abstract String getEvalueArgumentName();

    abstract String getNumberThreadsArgumentName();

    abstract String getOutputFileArgumentName();

    File getOutputFile() {
      return this.outputFile;
    }

    void setOutputFile(final File resultXMLFile) {
      this.outputFile = resultXMLFile;
    }

    /**
     * Build the command line, part common to all sequences.
     */
    List<String> getComandLine() {

      final List<String> result = new ArrayList<>();

      result.add(this.blastPath);

      result.add(getProgramNameArgumentName());
      result.add("blastn");

      result.add(getDatabaseArgumentName());
      result.add(this.blastDBPath);

      result.add(getAlignementViewOptionsArgumentName());
      result.add(getAlignementViewOptionsArgumentValue());

      if (this.outputFile != null) {
        result.add(getOutputFileArgumentName());
        result.add(getOutputFile().getAbsolutePath());
      }

      // Add arguments from configuration Aozan
      if (this.argBlast != null && this.argBlast.size() > 0) {
        result.addAll(this.argBlast);
      }

      return Collections.unmodifiableList(result);

    }

    /**
     * Check argument for blastn from configuration aozan. This parameters can't
     * been modified : -d, -m, -a. The parameters are returned in a list.
     * @param blastArguments parameters for blastn
     * @return parameters for blastn in a list
     * @throws AozanException occurs if the parameters syntax is invalid.
     */
    private List<String> checkBlastArguments(final String blastArguments)
        throws AozanException {

      // Check coherence with default parameter defined in Aozan
      final List<String> paramEvalue = new ArrayList<>();
      paramEvalue.add(getEvalueArgumentName());
      paramEvalue.add(EVALUE_ARGUMENT);

      if (blastArguments == null || blastArguments.length() == 0) {
        return paramEvalue;
      }

      final StringTokenizer tokens =
          new StringTokenizer(blastArguments.trim(), " .,");
      final List<String> args = new ArrayList<>();

      // Set of pair tokens : first must begin with '-'
      while (tokens.hasMoreTokens()) {
        final String param = tokens.nextToken();
        final String val = tokens.nextToken();

        if (!param.startsWith("-")) {
          throw new AozanException(
              "FastQC: invalid Blast argument: " + this.argBlast);
        }

        // Parameters can not redefined
        if (param.equals(getProgramNameArgumentName())
            || param.equals(getDatabaseArgumentName())
            || param.equals(getNumberThreadsArgumentName())
            || param.equals(getAlignementViewOptionsArgumentName())) {
          continue;
        }

        if (param.equals(getEvalueArgumentName())) {
          // Replace initial value
          paramEvalue.clear();
          paramEvalue.add(getEvalueArgumentName());
          paramEvalue.add(val);
          continue;
        }

        args.add(param);
        args.add(val);
      }

      args.addAll(paramEvalue);

      return Collections.unmodifiableList(args);
    }

    @Override
    public String toString() {
      return Joiner.on(' ').join(getComandLine());
    }

    //
    // Constructor
    //
    /**
     * Constructor.
     * @param blastPath path to application blast
     * @param blastDBPath path to blastn database
     * @param blastArguments argument for blastn added in configuration Aozan,
     *          optional
     * @throws IOException an error occurs if application or database doesn't
     *           exist
     * @throws AozanException occurs if the parameters syntax is invalid.
     */
    CommandLine(final String blastPath, final String blastDBPath,
        final String blastArguments) throws IOException, AozanException {

      this.blastPath = blastPath;
      this.blastDBPath = blastDBPath;

      checkExistingFile(new File(blastPath), "FastQC: Blast path");
      // Check nt.nal file exists
      checkExistingFile(new File(blastDBPath + ".nal"),
          " FastQC: Blast database path");

      this.argBlast = checkBlastArguments(blastArguments);
    }
  }

  /**
   * This class store a command line to run blastn from ncbi-blast+ package.
   */
  private static final class NbciBlastnCommandLine extends CommandLine {

    @Override
    String getProgramNameArgumentName() {
      return "-task";
    }

    @Override
    String getDatabaseArgumentName() {
      return "-db";
    }

    @Override
    String getAlignementViewOptionsArgumentName() {
      return "-outfmt";
    }

    @Override
    String getAlignementViewOptionsArgumentValue() {
      // Output xml format
      return "5";
    }

    @Override
    String getEvalueArgumentName() {
      return "-evalue";
    }

    @Override
    String getNumberThreadsArgumentName() {
      return "-num_threads";
    }

    @Override
    String getOutputFileArgumentName() {
      return "-out";
    }

    /**
     * Constructor.
     */
    NbciBlastnCommandLine(final String blastPath, final String blastDBPath,
        final String blastArguments) throws IOException, AozanException {
      super(blastPath, blastDBPath, blastArguments);
    }

  }

  /**
   * This class store a command line to run blastn from blastall package.
   */
  private static final class BlastallCommandLine extends CommandLine {

    @Override
    String getProgramNameArgumentName() {
      return "-p";
    }

    @Override
    String getDatabaseArgumentName() {
      return "-d";
    }

    @Override
    String getAlignementViewOptionsArgumentName() {
      return "-m";
    }

    @Override
    String getAlignementViewOptionsArgumentValue() {
      // Output xml format
      return "7";
    }

    @Override
    String getEvalueArgumentName() {
      return "-e";
    }

    @Override
    String getNumberThreadsArgumentName() {
      return "-a";
    }

    @Override
    String getOutputFileArgumentName() {
      return "-o";
    }

    /**
     * Constructor.
     */
    BlastallCommandLine(final String blastPath, final String blastDBPath,
        final String blastArguments) throws IOException, AozanException {
      super(blastPath, blastDBPath, blastArguments);
    }
  }

}
