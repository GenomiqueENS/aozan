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
import static fr.ens.biologie.genomique.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.biologie.genomique.eoulsan.util.FileUtils.createTempFile;
import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.AozanRuntimeException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.collectors.CollectorConfiguration;
import fr.ens.biologie.genomique.aozan.util.DockerManager;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerImageInstance;
import fr.ens.biologie.genomique.eoulsan.util.process.SimpleProcess;
import fr.ens.biologie.genomique.eoulsan.util.process.SystemSimpleProcess;

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

  private static final String BLAST_EXECUTABLE_DOCKER = "blastall";
  private static final String BLAST_DOCKER_IMAGE = "genomicpariscentre/blast2";
  private static final String BLAST_VERSION_DOCKER = "2.2.26";

  // Tag configuration general of blast
  private static final String ITERATION_TAG = "Iteration";
  private static final String HIT_TAG = "Hit";
  private static final String QUERY_LENGTH_TAG = "Iteration_query-len";
  private static final String QUERY_DEF_TAG = "Iteration_query-def";

  private static final String HIT_NUM_TAG = "Hit_num";
  private static final String HIT_DEF_TAG = "Hit_def";
  private static final String HSP_EVALUE_TAG = "Hsp_evalue";
  private static final String HSP_IDENTITY_TAG = "Hsp_identity";
  private static final String HSP_ALIGN_LEN_TAG = "Hsp_align-len";

  private static volatile OverrepresentedSequencesBlast singleton;

  // Save sequence and result blast for the run
  private final Map<String, BlastResultHit> sequencesAlreadyAnalysis =
      new HashMap<>();

  private boolean useDocker;
  private boolean configured;
  private boolean enabled;
  private File tmpDir;

  private CommandLine blastCommonCommandLine;
  private Set<String> submittedSequences = new HashSet<>();
  private String dockerConnectionString;

  /**
   * This class with parse Blast XML output using SAX API.
   */
  private static class IterationHandler extends DefaultHandler {

    private final Map<String, String> sequences;
    private final Map<String, BlastResultHit> result = new HashMap<>();

    private String currentElement;

    private Integer queryLength;
    private String seqId;

    private Integer hitNum;
    private String hitResult;
    private String hspEValue;
    private Integer hspIdentity;
    private Integer hspAlignLen;
    private int hitCount;
    private int iterationCount;

    @Override
    public void startElement(final String uri, final String localName,
        final String qName, final Attributes attributes) throws SAXException {

      switch (qName) {

      case QUERY_LENGTH_TAG:
      case QUERY_DEF_TAG:
      case HIT_NUM_TAG:
      case HIT_DEF_TAG:
      case HSP_EVALUE_TAG:
      case HSP_IDENTITY_TAG:
      case HSP_ALIGN_LEN_TAG:
        this.currentElement = qName;
        break;

      case ITERATION_TAG:
        this.iterationCount++;
        this.currentElement = null;
        break;

      case HIT_TAG:
        this.hitCount++;
        this.currentElement = null;
        break;

      default:
        this.currentElement = null;
        break;
      }
    }

    @Override
    public void endElement(final String uri, final String localName,
        final String qName) throws SAXException {

      if (ITERATION_TAG.equals(qName)) {

        if (this.hitCount > 0) {

          BlastResultHit blastResultHit =
              new BlastResultHit(this.hitNum, this.hitResult, this.hspEValue,
                  this.hspIdentity, this.hspAlignLen, this.hitCount,
                  this.queryLength, this.seqId, BLAST_RESULT_HTML_TYPE);

          this.result.put(seqId, blastResultHit);
        } else {
          this.result.put(seqId, null);
        }

        this.seqId = null;
        this.queryLength = null;

        this.hitNum = null;
        this.hitResult = null;
        this.hspAlignLen = null;
        this.hspIdentity = null;
        this.hspEValue = null;
        this.hitCount = 0;
      }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length)
        throws SAXException {

      if (this.currentElement == null) {
        return;
      }

      switch (currentElement) {

      case QUERY_LENGTH_TAG:
        this.queryLength = Integer.decode(new String(ch, start, length));
        break;

      case QUERY_DEF_TAG:
        this.seqId = new String(ch, start, length);
        break;

      case HIT_DEF_TAG:
        if (hitCount == 1) {
          this.hitResult = new String(ch, start, length);
        }
        break;

      case HIT_NUM_TAG:
        if (hitCount == 1) {
          this.hitNum = Integer.decode(new String(ch, start, length));
        }
        break;

      case HSP_EVALUE_TAG:
        if (hitCount == 1) {
          this.hspEValue = new String(ch, start, length);
        }
        break;

      case HSP_IDENTITY_TAG:
        if (hitCount == 1) {
          this.hspIdentity = Integer.decode(new String(ch, start, length));
        }
        break;

      case HSP_ALIGN_LEN_TAG:
        if (hitCount == 1) {
          this.hspAlignLen = Integer.decode(new String(ch, start, length));
        }
        break;

      default:
        break;
      }

    }

    @Override
    public void endDocument() throws SAXException {

      // Empty responses
      if (this.iterationCount == 0) {
        for (String seqId : this.sequences.keySet()) {
          this.result.put(seqId, null);
        }
      }
    }

    /**
     * Get the result of the parsing.
     * @return a map with the results of the parsing
     */
    public Map<String, BlastResultHit> getResult() {
      return this.result;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param sequences submitted sequences
     */
    IterationHandler(final Map<String, String> sequences) {

      this.sequences = sequences;
    }

  }

  //
  // Configuration
  //

  /**
   * Configure and check that Blastn can be launched.
   * @param conf object with the collector configuration
   * @param dockerConnectionString Docker connection URI
   */
  public void configure(final CollectorConfiguration conf,
      final String dockerConnectionString) {

    requireNonNull(conf, "conf argument cannot be null");

    if (this.configured) {
      return;
    }

    boolean stepEnabled =
        conf.getBoolean(Settings.QC_CONF_FASTQC_BLAST_ENABLE_KEY);

    if (!stepEnabled) {
      this.configured = true;
      return;
    }

    this.tmpDir = conf.getFile(QC.TMP_DIR);

    // Test Docker must be used for launching Blast
    this.useDocker =
        conf.getBoolean(Settings.QC_CONF_FASTQC_BLAST_USE_DOCKER_KEY);

    // Add arguments from configuration Aozan
    final String blastArguments =
        conf.get(Settings.QC_CONF_FASTQC_BLAST_ARGUMENTS_KEY);

    // Path to database for blast, need to add prefix filename used "nt"
    final String blastDBPath =
        conf.get(Settings.QC_CONF_FASTQC_BLAST_DB_PATH_KEY)
            + PREFIX_FILENAME_DATABASE;

    final String blastPath;

    if (this.useDocker) {
      blastPath = BLAST_EXECUTABLE_DOCKER;
      this.dockerConnectionString = conf.get(Settings.DOCKER_URI_KEY);
    } else {

      // Path to blast executable
      blastPath = conf.get(Settings.QC_CONF_FASTQC_BLAST_PATH_KEY);

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
    }

    if (stepEnabled) {

      try {

        this.blastCommonCommandLine =
            createBlastCommandLine(new File(blastPath), new File(blastDBPath),
                blastArguments, useDocker);

        // Add in map all sequences to do not analysis, return a resultBlast
        // with no hit
        loadSequencesToIgnore();

        LOGGER.info("FASTQC: blast is enabled, command line = "
            + this.blastCommonCommandLine);
        this.enabled = true;

      } catch (final IOException | AozanException e) {
        LOGGER.warning(e.getMessage() + '\n' + stackTraceToString(e));
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
  // Public methods
  //

  /**
   * Submit a sequence to blast
   * @param sequence the sequence to blast
   */
  public void submitSequence(final String sequence) {

    requireNonNull(sequence, "sequence argument cannot be null");

    if (!this.sequencesAlreadyAnalysis.containsKey(sequence)
        && !this.submittedSequences.contains(sequence)) {
      synchronized (this.submittedSequences) {
        this.submittedSequences.add(sequence);
      }
    }
  }

  /**
   * Get the blast result of a sequence
   * @param sequence the sequence to blast
   * @return the result as BlastResultHit object
   * @throws IOException if an error occurs while blasting sequences
   * @throws AozanException if an error occurs while blasting sequences
   */
  public BlastResultHit getResult(final String sequence)
      throws IOException, AozanException {

    requireNonNull(sequence, "sequence argument cannot be null");

    // Test if the instance has been configured
    if (!this.configured) {
      throw new AozanRuntimeException(
          "OverrepresentedSequencesBlast is not configured");
    }

    // Return nothing if blast if disabled
    if (!this.enabled) {
      return null;
    }

    // Return the result if it already been computed
    if (this.sequencesAlreadyAnalysis.containsKey(sequence)) {
      return this.sequencesAlreadyAnalysis.get(sequence);
    }

    synchronized (this.submittedSequences) {

      // Return the result if it already been computed since the end of the lock
      if (this.sequencesAlreadyAnalysis.containsKey(sequence)) {
        return this.sequencesAlreadyAnalysis.get(sequence);
      }

      // Add the sequence to the submitted list
      this.submittedSequences.add(sequence);

      // Blast all the submitted sequences
      blast();

      // Clear submitted sequences
      this.submittedSequences.clear();

      // Return the result
      return this.sequencesAlreadyAnalysis.get(sequence);
    }
  }

  //
  // Methods to build command line
  //

  private CommandLine createBlastCommandLine(final File blastPath,
      final File blastDBPath, final String blastArguments,
      final boolean useDocker) throws IOException, AozanException {

    if ("blastall".equals(blastPath.getName())) {
      return new BlastallCommandLine(blastPath, blastDBPath, blastArguments,
          useDocker);
    }

    if ("blastn".equals(blastPath.getName())) {
      return new NbciBlastnCommandLine(blastPath, blastDBPath, blastArguments,
          useDocker);
    }

    throw new AozanException(
        "Fail to build command line, only blastn (from ncbi-blast+) or "
            + "blastall application are supported.");
  }

  //
  //
  // Methods to analysis sequences
  //

  private void blast() throws IOException, AozanException {

    // Create temporary files
    File inputFastaFile = createTempFile(this.tmpDir, "blast_", "_input.fast");
    File resultXMLFile = createTempFile(this.tmpDir, "blast_", "_output.xml");

    // Create input file
    final Map<String, String> mapIds = new HashMap<>();
    try (FileWriter writer = new FileWriter(inputFastaFile)) {

      int count = 0;
      for (String sequence : this.submittedSequences) {

        final String seqId = "seq" + ++count;

        writer.write(">" + seqId + "\n" + sequence + "\n");
        mapIds.put(seqId, sequence);
      }
      writer.flush();
    }

    LOGGER
        .info("FASTQC: Launch " + this.submittedSequences.size() + " blast(s)");

    // Launch blast
    launchBlast(useDocker, this.dockerConnectionString,
        this.blastCommonCommandLine, inputFastaFile, resultXMLFile);

    // Wait writing xml file
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e) {
      // Do nothing
    }

    // Parse result file if not empty
    if (resultXMLFile.length() > 0) {
      parseDocument(resultXMLFile, mapIds);
    }

    // Remove temporary files
    if (inputFastaFile.exists()) {
      if (!inputFastaFile.delete()) {
        LOGGER.warning("FASTQC: Cannot delete the Blast xml output file "
            + inputFastaFile.getAbsolutePath());
      }
    }
    if (resultXMLFile.exists()) {
      if (!resultXMLFile.delete()) {
        LOGGER.warning("FASTQC: Cannot delete the Blast xml output file "
            + resultXMLFile.getAbsolutePath());
      }
    }
  }

  /**
   * Launch blast.
   * @param dockerMode launch Blast in Docker mode if true
   * @param commandLine the command line
   * @param inputFile input FASTA file
   * @param outputFile output XML file
   * @throws AozanException occurs if the process fails
   */
  private static void launchBlast(final boolean dockerMode,
      final String dockerConnectionString, final CommandLine commandLine,
      final File inputFile, final File outputFile) throws AozanException {

    final List<String> cmd = commandLine.getComandLine(inputFile, outputFile);

    SimpleProcess process;

    try {

      if (dockerMode) {

        DockerImageInstance instance =
            DockerManager.getInstance(DockerManager.ClientType.FALLBACK,
                new URI(dockerConnectionString)).createImageInstance(
                    BLAST_DOCKER_IMAGE + ':' + BLAST_VERSION_DOCKER);

        instance.pullImageIfNotExists();
        process = instance;

      } else {
        process = new SystemSimpleProcess();
      }

      File workingDir = outputFile.getParentFile();
      File tmpDir = outputFile.getParentFile();
      File stderrFile = new File(tmpDir, "STDERR");
      File stdoutFile = new File(tmpDir, "STDOUT");

      LOGGER.fine("FASTQC: Blast command line: " + cmd);

      int exitValue = process.execute(cmd, workingDir,
          outputFile.getParentFile(), stdoutFile, stderrFile,
          inputFile.getParentFile(), outputFile.getParentFile(),
          commandLine.blastDBPath.getParentFile());

      if (exitValue > 0) {
        LOGGER.warning(
            "FastQC: fail of blastn process, exit value is : " + exitValue);
      }

    } catch (IOException e) {
      throw new AozanException(e);
    } catch (URISyntaxException e) {
      throw new AozanException(
          "Invalid Docker connection URI: " + dockerConnectionString, e);
    }
  }

  //
  // XML Parsing methods
  //

  /**
   * Parse xml file result to identify the best hit.
   * @param resultXML result file from blastn
   * @param sequence query blastn
   * @throws AozanException occurs if the parsing fails.
   */
  private void parseDocument(final File resultXML,
      final Map<String, String> sequences) throws AozanException {

    try {
      checkExistingFile(resultXML, "FastQC: Blast xml query result");

      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      IterationHandler iterationHandler = new IterationHandler(sequences);
      saxParser.parse(resultXML, iterationHandler);

      // Search the best hit
      this.sequencesAlreadyAnalysis.putAll(iterationHandler.getResult());

    } catch (final IOException | SAXException
        | ParserConfigurationException e) {
      throw new AozanException(e);
    }
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

    private final File blastPath;
    private final File blastDBPath;
    private final List<String> argBlast;

    //
    // Abstract methods
    //

    abstract String getProgramNameArgumentName();

    abstract String getDatabaseArgumentName();

    abstract String getAlignementViewOptionsArgumentName();

    abstract String getAlignementViewOptionsArgumentValue();

    abstract String getEvalueArgumentName();

    abstract String getNumberThreadsArgumentName();

    abstract String getInputFileArgumentName();

    abstract String getOutputFileArgumentName();

    /**
     * Build the command line, part common to all sequences.
     */
    List<String> getComandLine(final File inputFile, final File outputFile) {

      final List<String> result = new ArrayList<>();

      result.add(this.blastPath.getPath());

      result.add(getProgramNameArgumentName());
      result.add("blastn");

      result.add(getDatabaseArgumentName());
      result.add(this.blastDBPath.getPath());

      result.add(getAlignementViewOptionsArgumentName());
      result.add(getAlignementViewOptionsArgumentValue());

      if (inputFile != null) {
        result.add(getInputFileArgumentName());
        result.add(inputFile.getAbsolutePath());
      }

      if (outputFile != null) {
        result.add(getOutputFileArgumentName());
        result.add(outputFile.getAbsolutePath());
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
      return Joiner.on(' ').join(getComandLine(null, null));
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
     * @param useDocker true if Docker must be used
     * @throws IOException an error occurs if application or database doesn't
     *           exist
     * @throws AozanException occurs if the parameters syntax is invalid.
     */
    CommandLine(final File blastPath, final File blastDBPath,
        final String blastArguments, final boolean useDocker)
        throws IOException, AozanException {

      this.blastPath = blastPath;
      this.blastDBPath = blastDBPath;

      if (!useDocker) {
        checkExistingFile(blastPath, "FastQC: Blast path");
      }

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
    String getInputFileArgumentName() {
      return "-query";
    }

    @Override
    String getOutputFileArgumentName() {
      return "-out";
    }

    /**
     * Constructor.
     */
    NbciBlastnCommandLine(final File blastPath, final File blastDBPath,
        final String blastArguments, final boolean useDocker)
        throws IOException, AozanException {
      super(blastPath, blastDBPath, blastArguments, useDocker);
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
    String getInputFileArgumentName() {
      return "-i";
    }

    @Override
    String getOutputFileArgumentName() {
      return "-o";
    }

    /**
     * Constructor.
     */
    BlastallCommandLine(final File blastPath, final File blastDBPath,
        final String blastArguments, final boolean useDocker)
        throws IOException, AozanException {
      super(blastPath, blastDBPath, blastArguments, useDocker);
    }
  }

}