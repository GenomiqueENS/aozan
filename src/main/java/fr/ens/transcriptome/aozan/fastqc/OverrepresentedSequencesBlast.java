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

package fr.ens.transcriptome.aozan.fastqc;

import static com.google.common.collect.Lists.newArrayList;
import static fr.ens.transcriptome.aozan.util.XMLUtilsParser.extractFirstValueToInt;
import static fr.ens.transcriptome.aozan.util.XMLUtilsParser.extractFirstValueToString;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
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

import uk.ac.babraham.FastQC.Sequence.Contaminant.ContaminantHit;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.AozanRuntimeException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class launches blastn on one sequence query and parses xml result file
 * to identify the best hit.
 * @since 1.2
 * @author Sandrine Perrin
 */
public class OverrepresentedSequencesBlast {

  /** LOGGER */
  private static final Logger LOGGER = Common.getLogger();

  private static final String SEQUENCES_NOT_USE_FILE =
      "/sequences_nohit_with_blastn.txt";

  // Key for parameters in Aozan configuration
  public static final String KEY_BLAST_PATH = "qc.conf.blast.path";
  public static final String KEY_BLAST_PATH_DB = "qc.conf.blast.path.db";
  public static final String KEY_BLAST_ARGUMENTS = "qc.conf.blast.arguments";
  public static final String KEY_BLAST_VERSION_EXPECTED =
      "qc.conf.blast.version.expected";
  public static final String KEY_STEP_BLAST_ENABLE =
      "qc.conf.step.blast.enable";

  // Tag configuration general of blast
  private static final String tag_queryLength = "Iteration_query-len";
  private static final String tag_blastVersion = "BlastOutput_version";

  private static OverrepresentedSequencesBlast singleton;

  // Save sequence and result blast for the run
  private final Map<String, BlastResultHit> sequencesAlreadyAnalysis = Maps
      .newHashMap();

  private boolean configured;
  private String tmpPath;

  private boolean firstCall = true;
  private boolean stepEnabled = false;

  private List<String> blastCommonCommandLine;
  private String blastVersionExpected;

  //
  // Configuration
  //

  /**
   * Configure and check that blastall can be launched
   * @param properties object with the collector configuration
   */
  public void configure(final Properties properties) {

    if (this.configured)
      throw new AozanRuntimeException(
          "OverrepresentedSequencesBlast has been already configured");

    this.stepEnabled =
        Boolean.parseBoolean(properties.getProperty(KEY_STEP_BLAST_ENABLE)
            .trim().toLowerCase());

    if (this.stepEnabled) {

      // Check parameters
      this.blastVersionExpected =
          properties.getProperty(KEY_BLAST_VERSION_EXPECTED);

      this.tmpPath = properties.getProperty(QC.TMP_DIR);

      String blastPath = properties.getProperty(KEY_BLAST_PATH).trim();
      String blastDBPath = properties.getProperty(KEY_BLAST_PATH_DB).trim();

      // Check paths needed in configuration aozan
      if (blastPath == null
          || blastPath.trim().length() == 0 || blastDBPath == null
          || blastDBPath.trim().length() == 0) {
        LOGGER.warning("Empty or incompleted configuration for Blast detected."
            + " Blast will not be used");
        this.stepEnabled = false;
      }

      // Check if blast is installed
      if (!new File(blastPath).exists()) {
        LOGGER
            .warning("Blast executable is not installed at the following path: "
                + blastPath);
        this.stepEnabled = false;
      }

      if (this.stepEnabled) {

        try {
          // Add arguments from configuration Aozan
          String blastArguments = properties.getProperty(KEY_BLAST_ARGUMENTS);

          this.blastCommonCommandLine =
              createCommonBlastCommandLine(blastPath, blastDBPath,
                  checkArgBlast(blastArguments));

          // Add in map all sequences to do not analysis, return a resultBlast
          // with no hit
          loadSequencesToIgnore();

        } catch (IOException e) {
          LOGGER.severe(StringUtils.join(e.getStackTrace(), "\n\t"));
          this.stepEnabled = false;
        } catch (AozanException e) {
          LOGGER.info(StringUtils.join(e.getStackTrace(), "\n\t"));
          this.stepEnabled = false;
        }

        LOGGER.info("FastQC-step blast : step is enable, command line = "
            + Joiner.on(' ').join(this.blastCommonCommandLine));
      }
    }

    this.configured = true;
  }

  /**
   * Test if blastall can be launched
   * @return true if blastall can be launched else false
   */
  public boolean isStepBlastEnabled() {

    return this.stepEnabled;
  }

  /**
   * Add in hashMap all sequences identified like to fail blastn analysis for
   * skipping them
   */
  private void loadSequencesToIgnore() {

    BufferedReader br = null;
    try {
      br =
          new BufferedReader(new InputStreamReader(this.getClass()
              .getResourceAsStream(SEQUENCES_NOT_USE_FILE),
              Globals.DEFAULT_FILE_ENCODING));

      String sequence = "";

      while ((sequence = br.readLine()) != null) {

        if (!sequence.startsWith("#"))
          if (sequence.trim().length() > 0)
            this.sequencesAlreadyAnalysis.put(sequence, new BlastResultHit(
                sequence));

      }
      br.close();

    } catch (IOException e) {

    } finally {
      if (br != null)
        try {
          br.close();
        } catch (IOException e) {
        }
    }

  }

  //
  // Methods to build command line
  //

  /**
   * Build the command line, part common to all sequences
   * @param blastPath path to blastall
   * @param blastDBPath path to blastn database
   * @param argBlast argument for blastn added in configuration aozan, optional
   * @throws IOException
   */
  private static List<String> createCommonBlastCommandLine(
      final String blastPath, final String blastDBPath,
      final List<String> argBlast) throws IOException {

    checkExistingFile(new File(blastPath),
        "FastQC-step blast : path to blast doesn't exist.");
    // Check nt.nal file exists
    checkExistingFile(new File(blastDBPath + ".nal"),
        " FastQC-step blast : path to database blast doesn't exist.");

    final List<String> result = Lists.newArrayList();

    result.add(blastPath);
    result.add("-p");
    result.add("blastn");
    result.add("-d");
    result.add(blastDBPath);
    result.add("-m");
    result.add("7");

    // Add arguments from configuration aozan
    if (argBlast != null && argBlast.size() > 0)
      result.addAll(argBlast);

    return result;
  }

  /**
   * Check argument for blastn from configuration aozan. This parameters can't
   * been modified : -d, -m, -a. The parameters are returned in a list.
   * @param argBlast parameters for blastn
   * @return parameters for blastn in a list
   * @throws AozanException occurs if the parameters syntax not conforms.
   */
  public static List<String> checkArgBlast(final String argBlast)
      throws AozanException {

    // Check coherence with default parameter defined in Aozan
    List<String> paramEvalue = Lists.newArrayList();
    paramEvalue.add("-e");
    paramEvalue.add("0.0001");

    if (argBlast == null || argBlast.length() == 0)
      return paramEvalue;

    StringTokenizer tokens = new StringTokenizer(argBlast.trim(), " .,");
    List<String> args = Lists.newArrayList();

    // Set of pair tokens : first must begin with '-'
    while (tokens.hasMoreTokens()) {
      String param = tokens.nextToken();
      String val = tokens.nextToken();

      if (!param.startsWith("-"))
        throw new AozanException("FastQC-step blast : parameters not conforme "
            + argBlast);

      // Parameters can not redefined
      if (param.equals("-d")
          || param.equals("-p") || param.equals("-m") || param.equals("-a"))
        continue;

      if (param.equals("-e")) {
        // Replace initial value
        paramEvalue.clear();
        paramEvalue.add("-e");
        paramEvalue.add(val);
        continue;
      }

      args.add(param);
      args.add(val);
    }

    args.addAll(paramEvalue);

    return args;
  }

  //
  // Methods to analysis sequences
  //

  /**
   * Launch blastn and parse result xml file for identify the best hit.
   * @param sequence query blastn
   * @return a ContaminantHit for the best hit return by blastn or null
   */
  @SuppressWarnings("static-access")
  public synchronized ContaminantHit blastSequence(final String sequence)
      throws IOException, AozanException {

    // Test if the instance has been configured
    if (!this.configured)
      throw new AozanRuntimeException(
          "OverrepresentedSequencesBlast is not configured");

    final BlastResultHit blastResult;

    // Check if new sequence
    if (this.sequencesAlreadyAnalysis.containsKey(sequence))

      // The sequence has been already blasted
      blastResult = this.sequencesAlreadyAnalysis.get(sequence);

    else {

      // The sequence has not been blasted
      blastResult = blastNewSequence(sequence);

      // Save sequence in multithreading context
      this.sequencesAlreadyAnalysis.put(sequence, blastResult);

    }

    if (blastResult == null || blastResult.isNull())
      // No hit found
      return null;

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

    File resultXMLFile = null;
    BlastResultHit blastResult = null;

    // Create temporary file
    resultXMLFile =
        FileUtils.createTempFile(new File(this.tmpPath), "blast_",
            "_result.xml");

    // Check if the temporary file already exists
    checkExistingFile(resultXMLFile,
        "FastQC-step blast : path to output file doesn't exist for the sequence "
            + sequence);

    // Clone command line
    final List<String> finalCommand = newArrayList(this.blastCommonCommandLine);

    // Add output file result
    finalCommand.add("-o");
    finalCommand.add(resultXMLFile.getAbsolutePath());

    // Launch blast
    launchBlastSearch(finalCommand, sequence);

    // Wait writing xml file
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // Do nothing
    }

    // Parse result file if not empty
    if (resultXMLFile.length() > 0)
      blastResult = parseDocument(resultXMLFile, sequence);

    // Remove XML file
    if (resultXMLFile.exists())
      if (!resultXMLFile.delete())
        LOGGER
            .warning("FastQC-step blast : Can not delete xml file with result from blast : "
                + resultXMLFile.getAbsolutePath());
    return blastResult;

  }

  /**
   * Process blastn, one instance at the same time
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
      Writer os =
          new OutputStreamWriter(process.getOutputStream(),
              Globals.DEFAULT_FILE_ENCODING);
      os.write(sequence);
      os.flush();
      os.close();

      final int exitValue = process.waitFor();
      if (exitValue > 0)
        LOGGER
            .warning("FastQC-step blast : fail of process to launch blastn with sequence "
                + sequence + ", exist value is : " + exitValue);

    } catch (IOException e) {
      // e.printStackTrace();
      throw new AozanException(e);

    } catch (InterruptedException e) {
      // e.printStackTrace();
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
      checkExistingFile(resultXML,
          "FastQC-step blast : query result xml doesn't exist");

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

    } catch (IOException e) {
      throw new AozanException(e);
    } catch (SAXException e) {
      throw new AozanException(e);
    } catch (ParserConfigurationException e) {
      throw new AozanException(e);

    } finally {
      try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  /**
   * Retrieve general informations of blastn, call once.
   * @param doc root of xml file result for blastn
   */
  private void parseHeaderDocument(final Document doc)  {

    if (firstCall) {

      final String version = extractFirstValueToString(doc, tag_blastVersion);

      // Check version xml file
      if (!this.blastVersionExpected.equals(version))
        LOGGER.warning("FastQC - step blast : version xml not expected "
            + version + " instead of " + this.blastVersionExpected);
      else
        LOGGER.info("FastQC-step blast : blastall version " + version);

      StringBuilder parameters = new StringBuilder();

      parameters.append("Parameters_expect=");
      parameters.append(extractFirstValueToString(doc, "Parameters_expect"));
      parameters.append(", Parameters_sc-match=");
      parameters.append(extractFirstValueToString(doc, "Parameters_sc-match"));
      parameters.append(", Parameters_sc-mismatch=");
      parameters
          .append(extractFirstValueToString(doc, "Parameters_sc-mismatch"));
      parameters.append(", Parameters_gap-open=");
      parameters.append(extractFirstValueToString(doc, "Parameters_gap-open"));
      parameters.append(", Parameters_gap-extend=");
      parameters
          .append(extractFirstValueToString(doc, "Parameters_gap-extend"));
      parameters.append(", Parameters_filter=");
      parameters.append(extractFirstValueToString(doc, "Parameters_filter"));

      LOGGER.info("FastQC-step blast : blastall parameters "
          + parameters.toString());

      firstCall = false;
    }
  }

  /**
   * Search hit in xml result file.
   * @param doc root of the xml file
   * @param sequence query blastn
   * @return best hit contained in xml file or null
   */
  private BlastResultHit parseHit(final Document doc, final String sequence) {

    List<Element> responses = XMLUtils.getElementsByTagName(doc, "Iteration");
    Element elemIteration = responses.get(0);

    final int queryLength =
        extractFirstValueToInt(elemIteration, tag_queryLength);

    List<Element> hits = XMLUtils.getElementsByTagName(elemIteration, "Hit");
    int countHits = hits.size();

    // No hit found
    if (countHits == 0)
      return null;

    // Parse the first hit to build result
    return new BlastResultHit(hits.get(0), countHits, queryLength, sequence);

  }

  //
  // Singleton method
  //

  /**
   * Get the singleton instance.
   * @return the unique instance of the class
   */
  public static synchronized final OverrepresentedSequencesBlast getInstance() {

    if (singleton == null)
      singleton = new OverrepresentedSequencesBlast();

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

}
