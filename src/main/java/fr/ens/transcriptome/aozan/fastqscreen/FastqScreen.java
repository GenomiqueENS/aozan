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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeDebug;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.AbstractBowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.Bowtie2ReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.BowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.GSNAPReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.GsnapStep;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.aozan.fastqscreen.FastsqScreenSAMParser;

public class FastqScreen {

  // name index file
  // /home/sperrin/shares-net/ressources/sequencages/mappers_indexes
  // Drosophile : dmel-all-chromosome-r5

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  protected static final String COUNTER_GROUP = "reads_mapping";

  // file path bowtie (local)
  final static String bowtiePath = "/home/sperrin/Programmes/bowtie/bowtie";

  static File fastqFile = new File(
      "/home/sperrin/Documents/FastqScreenTest/extrait.txt");

  
  final static File indexFileAdapters_method2 =
      new File(
          "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/Adapters/Adapters.1.ebwt");
  final static File indexFilePhix_method2 =
      new File(
          "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/PhiX/PhiX.1.ebwt");

  final static File indexFileAdapters_method1 =
      new File(
          "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/Adapters/Adapters");
  final static File indexFilePhix_method1 = new File(
      "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/PhiX/PhiX");
  final static File indexDirPhix_method1 = new File(
          "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/PhiX");
  final static File genomeDescriptionPhix =  
		  new File ("/home/sperrin/shares-net/ressources/sequencages/mappers_indexes/bowtie-eaa26a56aeaccd77c44ea4168b6f5cdb.zip");
  
  final static File indexDirAdapters_method1 = new File(
          "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/Adapters");
          
  static File outputFile = null;
  static List<File> listOutputFile = new ArrayList<File>();

  final static boolean paired = false;
  final static int readsprocessed = defineReadsprocessed(fastqFile, paired);

  final static long startTime = System.currentTimeMillis();

  public static final void main(final String[] args) throws IOException,
      InterruptedException, EoulsanException {

    EoulsanRuntimeDebug.initDebugEoulsanRuntime();
    // eoulsan runtime exception method2();
    
    method3();
    
  }
  
  static void method3() throws IOException, InterruptedException {

    List<File[]> listGenome = new ArrayList<File[]>();
    listGenome.add(new File[]{indexFilePhix_method2, indexDirPhix_method1});
    listGenome.add(new File[]{indexFileAdapters_method2,indexDirAdapters_method1});

    FastqScreenPseudoMapReduce pmr =
        new FastqScreenPseudoMapReduce(readsprocessed);
    
    pmr.setMapReduceTemporaryDirectory(new File("/home/sperrin"));
    pmr.doMap(fastqFile, listGenome);

    pmr.doReduce(new File("/home/sperrin/outputDoReduce.txt"));
    pmr.getStatisticalTable();

    final long endTime = System.currentTimeMillis();

    // System.out.println("count=" + count);
    System.out.println((endTime - startTime));

    String stat = pmr.statisticalTableToString();
    System.out.println("\n\n" + stat);

    /*
     * create file screen.txt try { FileWriter f = new
     * FileWriter(fastqFile.getAbsolutePath() + "_screen.txt"); f.write(stat);
     * // LOGGER.info("Create file result FastqScreen " // +
     * fastqFile.getAbsolutePath() + "_screen.txt"); f.close(); } catch
     * (IOException ioe) { }
     */
  }

//launch fastq screen with bowtieReadsMapper
 static void method2() throws IOException, InterruptedException {

   AbstractBowtieReadsMapper bowtie = new BowtieReadsMapper();
   Reporter reporter = new Reporter();

   final File[] listArchiveIndexFile =
       {indexFilePhix_method2, indexFileAdapters_method2};
   final File[] listArchiveIndexDir =
       {
           new File(
               "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/PhiX"),
           new File(
               "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/Adapters")};

   mapSingleEnd(FastqFormat.FASTQ_SANGER, bowtie, fastqFile,
       listArchiveIndexFile, listArchiveIndexDir, reporter);
 }

 // change : use table of file instead of one file
 // method from ReadsMapperLocalStep
 private static void mapSingleEnd(FastqFormat fastqFormat,
     final SequenceReadsMapper bowtie, final File inFile,
     final File[] archiveIndexFile, final File[] indexDir,
     final Reporter reporter) throws IOException {

   // Init mapper

   // Set mapper arguments
   /*
    * final int mapperThreads = initMapperArguments(mapper,
    * context.getSettings() .getTempDirectoryFile());
    */
   final int mapperThreads = 1;

   // Mapper change defaults arguments
   final int seed = 40;

   final String newArgumentsMapper = "-l " + seed + " --chunkmbs 512 ";

   // " -l " + seed + " -k 2 --chunkmbs 512 -p 2 -S";

   // LOGGER
   // .info("Map file: "
   // + inFile + ", Fastq format: " + fastqFormat + ", use "
   // + bowtie.getMapperName() + " with " + mapperThreads
   // + " threads option");

   // loop : Process to mapping for each library
   for (int i = 0; i < indexDir.length; i++) {
     bowtie.init(false, fastqFormat, archiveIndexFile[i], indexDir[i],
         reporter, COUNTER_GROUP);
     bowtie.setMapperArguments(newArgumentsMapper);
     
     bowtie.setThreadsNumber(mapperThreads);

     bowtie.map(inFile /*ParseLine()*/);

   }
   
   
//   listOutputFile.add(bowtie.getSAMFile(null));
//   for (File f : listOutputFile) 
//	   System.out.println("SAM de bowtie " + f.getName());
   
 }
  /**
   * TODO : value probably exist in data in RunData
   * @return number of read in fastq file
   */
  private static int defineReadsprocessed(File fastqFile, boolean paired) {
    int countRead = 0;
    String line = "";

    try {
      BufferedReader b = new BufferedReader(new FileReader(fastqFile));
      while ((line = b.readLine()) != null) {
        if (line.charAt(0) == '@') {
          countRead++;
        }
        b.readLine();
        b.readLine();
        b.readLine();
      }// while
      b.close();

    } catch (NullPointerException npe) {
      System.out.println(npe.getMessage());
    } catch (FileNotFoundException fnfe) {
      System.out.println(fnfe.getMessage());
    } catch (IOException ioe) {
      System.out.println(ioe.getMessage());
    }
    return (paired ? countRead * 2 : countRead);
  }// defineReadsprocessed

}