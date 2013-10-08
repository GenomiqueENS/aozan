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

import static fr.ens.transcriptome.aozan.util.XMLUtilsParser.extractFirstValueToInt;
import static fr.ens.transcriptome.aozan.util.XMLUtilsParser.extractFirstValueToString;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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

import uk.ac.babraham.FastQC.Modules.OverRepresentedSeqs;
import uk.ac.babraham.FastQC.Modules.QCModule;
import uk.ac.babraham.FastQC.Sequence.SequenceFactory;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.Contaminant.ContaminantHit;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.FastqscreenDemo;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

public class OverrepresentedSequencesBlast {

  /** LOGGER */
  private static final Logger LOGGER = Common.getLogger();

  public static final String KEY_STEP_BLAST_REQUIERED =
      "qc.conf.step.blast.enable";
  public static final String KEY_BLAST_PATH = "qc.conf.blast.path";
  public static final String KEY_BLAST_PATH_DB = "qc.conf.blast.path.db";
  public static final String KEY_BLAST_ARGUMENTS = "qc.conf.blast.arguments";
  public static final String KEY_BLAST_VERSION_EXPECTED =
      "qc.conf.blast.version.expected";
  public static final String KEY_STEP_BLAST_ENABLE =
      "qc.conf.step.blast.enable";

  public static String TMP_PATH;

  // Save sequence and result blast for the run
  final Map<String, BlastResultHit> sequencesAlreadyAnalysis;
  // Tag configuration general of blast
  final String tag_queryDef = "Iteration_query-def";
  final String tag_queryLength = "Iteration_query-len";
  final String tag_blastVersion = "BlastOutput_version";

  static AozanException aozanException;

  static boolean firstCall = true;
  static boolean stepEnable = false;

  static List<String> sequencesToIgnore = Lists.newArrayList();
  final static List<String> cmd = Lists.newLinkedList();
  static String blastVersionExpected;

  public static void configure(final Properties properties) {

    stepEnable =
        Boolean.parseBoolean(properties.getProperty(KEY_STEP_BLAST_ENABLE)
            .trim().toLowerCase());

    if (stepEnable) {

      // Check parameters
      blastVersionExpected = properties.getProperty(KEY_BLAST_VERSION_EXPECTED);

      TMP_PATH = properties.getProperty(QC.TMP_DIR);

      String blastPath = properties.getProperty(KEY_BLAST_PATH);
      String blastDBPath = properties.getProperty(KEY_BLAST_PATH_DB);

      // Check path present in configuration aozan
      if (blastPath == null
          || blastPath.trim().length() == 0 || blastDBPath == null
          || blastDBPath.trim().length() == 0)
        stepEnable = false;

      else {

        String blastArguments = properties.getProperty(KEY_BLAST_ARGUMENTS);
        List<String> argBlastList = null;

        try {
          argBlastList = checkArgBlast(blastArguments);
          createCommonArgs(blastPath, blastDBPath, argBlastList);

        } catch (AozanException e) {
          // TODO
          e.printStackTrace();
          stepEnable = false;
        }

        LOGGER.info("Step Blast is enable : command line "
            + Joiner.on(' ').join(cmd));
      }
    }
  }

  public static boolean isStepBlastEnable() {
    return stepEnable;
  }

  private List<String> createCommandLine(final File outputFile)
      throws AozanException {
    List<String> finalCommand = Lists.newLinkedList(cmd);

    finalCommand.add("-o");
    finalCommand.add(outputFile.getAbsolutePath());

    return finalCommand;
  }

  private static void createCommonArgs(final String blastPath,
      final String blastDBPath, final List<String> argBlast)
      throws AozanException {

    try {
      checkExistingFile(new File(blastPath), " path to blast doesn't exist.");
      // Check nt.nal exists
      checkExistingFile(new File(blastDBPath + ".nal"),
          " path to database blast doesn't exist.");

    } catch (IOException e) {
      throw new AozanException(e);
    }

    cmd.clear();

    final List<String> blast = Lists.newLinkedList();
    blast.add(blastPath);
    blast.add("-p");
    blast.add("blastn");
    blast.add("-d");
    blast.add(blastDBPath);
    blast.add("-m");
    blast.add("7");

    // Build line command
    cmd.addAll(blast);

    if (argBlast != null && argBlast.size() > 0)
      cmd.addAll(argBlast);

  }

  public ContaminantHit searchSequenceInBlast(final String sequence) {

    if (checkSequencesNotToIgnore(sequence))
      return null;

    BlastResultHit blastResult = null;
    File resultXML = null;

    // Check if new sequence
    if (sequencesAlreadyAnalysis.containsKey(sequence))
      blastResult = sequencesAlreadyAnalysis.get(sequence);

    else {
      try {

        resultXML = FileUtils.createTempFile("blast_", "_result.xml");
        // resultXML = new File(TMP_PATH + "/blast_" + sequence +
        // "_result.xml");
        // resultXML = new File("/tmp/blast_" + sequence + "_result.xml");

        launchBlastSearch(createCommandLine(resultXML), sequence);

        if (resultXML.length() > 0)
          blastResult = parseDocument(resultXML, sequence);

        // Save result
        sequencesAlreadyAnalysis.put(sequence, blastResult);

      } catch (IOException e) {
        e.printStackTrace();
        aozanException = new AozanException(e);
      } catch (AozanException e) {
        e.printStackTrace();
        aozanException = new AozanException(e);
      }
    }

    if (blastResult == null)
      // No hit found
      return null;

    return blastResult.getContaminantHit();

  }

  private static synchronized void launchBlastSearch(final List<String> cmd,
      final String sequence) throws AozanException {

    final ProcessBuilder builder = new ProcessBuilder(cmd);

    System.out.println(sequence
        + " -> " + Joiner.on(' ').join(builder.command()));
    Process process;
    try {

      // timer.start();

      process = builder.start();

      // Writing on standard input
      OutputStreamWriter os = new OutputStreamWriter(process.getOutputStream());
      os.write(">seq_automatic");
      os.write("\n");
      os.write(sequence);
      os.flush();
      os.close();

      final int exitValue = process.waitFor();

      System.out.println("exit " + exitValue);
      // + " time : " + timer.elapsed(TimeUnit.MILLISECONDS));

    } catch (IOException e) {
      e.printStackTrace();
      throw new AozanException(e);

    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new AozanException(e);
    }

  }

  public BlastResultHit parseDocument(final File resultXML,
      final String sequence) throws AozanException {

    BlastResultHit blastResult = null;

    InputStream is = null;
    try {
      checkExistingFile(resultXML, "Blast : query result xml doesn't exist");

      // Create the input stream
      is = new FileInputStream(resultXML);

      // Read the XML file
      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(is);
      doc.getDocumentElement().normalize();

      // Retrieve results list
      parseHeaderDocument(doc);

      blastResult = parseHit(doc, sequence);

      is.close();
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
        e.printStackTrace();
      }
    }
    return blastResult;
  }

  private void parseHeaderDocument(final Document doc) throws AozanException {

    if (firstCall) {

      final String queryDef = extractFirstValueToString(doc, tag_queryDef);
      final String version = extractFirstValueToString(doc, tag_blastVersion);

      // Check version xml file
      if (!blastVersionExpected.equals(version))
        // throw new AozanException(
        // "FastQC - step blast : version xml not expected "
        // + version + " instead of " + blastVersionExpected);
        LOGGER.info("FastQC - step blast : version xml not expected "
            + version + " instead of " + blastVersionExpected);

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

      System.out.println("\n----------------------\nblast version "
          + version + "\t Query " + queryDef);

      LOGGER.info("Blast version " + version);
      LOGGER.info("Blast parameters " + parameters.toString());

      firstCall = false;
    }
  }

  private BlastResultHit parseHit(final Document doc, final String sequence) {

    List<Element> responses = XMLUtils.getElementsByTagName(doc, "Iteration");
    Element elemIteration = responses.get(0);

    final int queryLength =
        extractFirstValueToInt(elemIteration, tag_queryLength);

    BlastResultHit blastResult = null;

    List<Element> hits = XMLUtils.getElementsByTagName(elemIteration, "Hit");
    int countHits = hits.size();

    // No hit found
    if (countHits == 0)
      return null;

    Element firstHit = hits.get(0);
    if (extractFirstValueToInt(firstHit, "Hit_num") == 1)
      // Parse the document
      blastResult =
          new BlastResultHit(firstHit, countHits, queryLength, sequence);

    return blastResult;
  }

  public Exception throwException() {
    return aozanException;
  }

  private static boolean checkSequencesNotToIgnore(final String sequence) {

    if (sequencesToIgnore.size() == 0) {
      // TODO
      // loadSequencesToIgnoreFile();
      return false;
    }

    for (String seq : sequencesToIgnore) {
      if (seq.equals(sequence))
        return true;
    }

    return false;
  }

  public static List<String> checkArgBlast(final String argBlast)
      throws AozanException {
    // Check coherence with default parameter defined in Aozan

    List<String> paramEvalue = Lists.newLinkedList();
    paramEvalue.add("-e");
    paramEvalue.add("0.0001");

    if (argBlast == null || argBlast.length() == 0)
      return paramEvalue;

    StringTokenizer tokens = new StringTokenizer(argBlast.trim(), " .,");
    List<String> args = Lists.newLinkedList();

    // Set of pair tokens : first must begin with '-'
    while (tokens.hasMoreTokens()) {
      String param = tokens.nextToken();
      String val = tokens.nextToken();

      if (!param.startsWith("-"))
        throw new AozanException("Blast : parameters not conforme " + argBlast);

      // Parameters can not redefine
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
  // Constructor
  //

  public OverrepresentedSequencesBlast() {
    sequencesAlreadyAnalysis = Maps.newHashMap();
  }

  //
  //
  //

  /** Test method */
  public static void main(final String[] argv) {

    // Properties props = FastqscreenDemo.getPropertiesAozanConf();
    // props.put(KEY_BLAST_ARGUMENTS, "-e 10");
    //
    // configure(props);
    // try {
    // RuntimePatchFastQC.runPatchFastQC(isStepBlastEnable());
    // } catch (AozanException e1) {
    // // TODO Auto-generated catch block
    // e1.printStackTrace();
    // }
    //
    // File[] filesXml =
    // (new File("/home/sperrin/Documents/FastqScreenTest/tmp_bis/tmp")
    // .listFiles(new FileFilter() {
    //
    // @Override
    // public boolean accept(final File pathname) {
    // return pathname.length() > 0
    // && pathname.getName().endsWith(".xml");
    // }
    // }));
    //
    // OverrepresentedSequenceBlast ors = new OverrepresentedSequenceBlast();
    // BlastResultHit hit = null;
    // for (File f : filesXml) {
    // System.out.println("file " + f.getName());
    // try {
    // hit = ors.parseDocument(f, "AAAACACTTGTGCATTCTTTG");
    // } catch (AozanException e) {
    // // TODO Auto-generated catch block
    // System.out.println("file " + f.getName());
    // e.printStackTrace();
    // }
    // }
    // }
    //
    // public static void attente() {
    String file =
        "tmp_bis/aozan_fastq_2013_0072_polyA_TTAGGC_L001_R1_001.fastq";
    // String file =
    // "runtest/qc_130904_SNL110_0082_AC2BR0ACXX/"
    // +
    // "130904_SNL110_0082_AC2BR0ACXX/Project_bulbe_D2013/Sample_2013_0209/2013_0209_GTGAAA_L008_R1_001.fastq.bz2";

    final File fastq =
        new File("/home/sperrin/Documents/FastqScreenTest/" + file);

    System.setProperty("fastqc.unzip", "true");

    final String DIR = "/home/sperrin/Documents/FastqScreenTest/blat_fastqc";
    try {

      Properties props = FastqscreenDemo.getPropertiesAozanConf();
      props.put(KEY_BLAST_ARGUMENTS, "-e 10");

      configure(props);

      RuntimePatchFastQC.runPatchFastQC(isStepBlastEnable());
      QCModule[] modules = new QCModule[] {new OverRepresentedSeqs()};

      SequenceFile seqFile =
          SequenceFactory.getSequenceFile(new File[] {fastq});

      while (seqFile.hasNext()) {
        modules[0].processSequence(seqFile.next());
      }

      new HTMLReportArchiveAozan(seqFile, modules, new File(DIR,
          "report-fastqc.zip"));

      // searchSequenceInBlast(seq, null);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
