/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeDebug;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.AbstractBowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.Bowtie2ReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.BowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.GSNAPReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.GsnapStep;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QCReport;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.RunDataGenerator;
import fr.ens.transcriptome.aozan.collectors.FastQScreenCollector;
import fr.ens.transcriptome.aozan.fastqscreen.FastsqScreenSAMParser;

public class FastqScreen {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  protected static final String COUNTER_GROUP = "fastqscreen";
  private Map<String, String> properties;

  private static String indexDir;

  static File fastqFile = new File(
      "/home/sperrin/Documents/FastqScreenTest/fastq/phixmille.fq");

  // "/home/sperrin/Documents/FastqScreenTest/extrait.txt");
  // "/home/sperrin/Documents/FastqScreenTest/extrait_zip.txt.bz2");
  // "/home/sperrin/Documents/FastqScreenTest/extrait.txt");
  // "/home/sperrin/Documents/FastqScreenTest/fastq_phix.fq");
  // "/home/sperrin/Documents/FastqScreenTest/fastq/phixcentmille.fq");
  // "/home/sperrin/Documents/FastqScreenTest/fastq/phixcinquantemille.fq");

  // 3 REFERENCE GENOME
  // ADAPTER - size file Adapters.1.ebwt : 4,1M
  final static File indexFileAdapters =
      new File(
          "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/Adapters/Adapters.1.ebwt");
  final static File indexDirAdapters = new File(
      "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/Adapters");

  // PHIX - size file PhiX.1.ebwt : 4,1M
  final static File indexFilePhix =
      new File(
          "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/PhiX/PhiX.1.ebwt");
  final static File indexDirPhix = new File(
      "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/PhiX");

  // RIBOSOME - size file LSURef.1.ebwt 24M
  // shares-net/ressources/sequencages/fastq_screen/Silva_ribosomes/LSURef/LSURef
  final static File indexFileRibosome =
      new File(
          "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/Silva_ribosomes/LSURef/LSURef.1.ebwt");
  final static File indexDirRibosome =
      new File(
          "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/Silva_ribosomes/LSURef");

  static File outputFile = null;
  static List<File> listOutputFile = new ArrayList<File>();

  final static boolean paired = false;
  final static float percentHitNoLibraries = 0.f;
  final static long startTime = System.currentTimeMillis();
  Map<String, float[]> resultsFastqscreen;

  // ===================================================================================================

  

  // paired-end
  public Map<String, float[]> execute(String fastqRead1, String fastqRead2, List<String> listGenome) {
    return null;
  }

  // single-end
  public Map<String, float[]> execute(String fastqFile, List<File> listGenome) {

    FastqScreenPseudoMapReduce pmr = new FastqScreenPseudoMapReduce();

    try {
      pmr.setMapReduceTemporaryDirectory(new File("/home/sperrin"));

      pmr.doMap(new File(fastqFile), listGenome);
      pmr.doReduce(new File("/home/sperrin/outputDoReduce.txt"));
      pmr.createStatisticalTable();
      
    } catch (IOException ioe) {
      new AozanException(ioe.getMessage());
    } catch (EoulsanException ee) {
      new AozanException(ee.getMessage());
    } catch (BadBioEntryException bbe) {
      new AozanException(bbe.getMessage());

    }

    final long endTime = System.currentTimeMillis();

    // System.out.println("count=" + count);

    String stat = pmr.statisticalTableToString();
    System.out.println("\n\n" + stat);
    System.out.println((endTime - startTime)
        + " -- " + new SimpleDateFormat("h:m a").format(new Date()));

    System.out.println("size map " + pmr.getStatisticalTable().size());

    return pmr.getStatisticalTable();

  }

  public Map<String, float[]> getResultsFastqscreen() {
    return this.resultsFastqscreen;
  }

  
  public float getPercentHitNoLibraries() {
    return this.percentHitNoLibraries;
  }

  //
  // CONSTRUCTOR
  //
  public FastqScreen() {

  }

  public FastqScreen(Map<String, String> properties, String indexDir) {

    this.indexDir = indexDir;
    this.properties = properties;

  }

}