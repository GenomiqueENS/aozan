package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.BowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class FastqScreen {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  protected static final String COUNTER_GROUP = "reads_mapping";

  // file path bowtie (local)
  final static String bowtiePath = "/home/sperrin/Programmes/bowtie/bowtie";

  final static File fastqFile = new File(
      "/home/sperrin/Documents/FastqScreenTest/extrait.txt");
  final static File fastqFilePaired = new File(
      "/home/sperrin/Documents/FastqScreenTest/extraitDoublon.txt");

  final static File indexDirAdapters = new File(
      "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/Adapters");
  final static String indexFileAdapters = "Adapters";

  final static File indexDirPhix = new File(
      "/home/sperrin/shares-net/ressources/sequencages/fastq_screen/PhiX");
  final static String indexFilePhix = "PhiX";

  static File outputFile = null;
  static List<File> listOutputFile = new ArrayList<File>();

  final static boolean paired = false;
  final static int readsprocessed = defineReadsprocessed(fastqFile, paired);

  final static long startTime = System.currentTimeMillis();

  public static final void main(final String[] args) throws IOException,
      InterruptedException {

    // eoulsan runtime exception method2();
    method1();

  }

  static void method1() throws IOException, InterruptedException {
    ProcessBuilder pb;

    Map<String, File> listGenome = new HashMap<String, File>();
    listGenome.put(indexFilePhix, indexDirPhix);
    listGenome.put(indexFileAdapters, indexDirAdapters);

    FastqScreenPseudoMapReduce pmr =
        new FastqScreenPseudoMapReduce(readsprocessed);

    Process p = null;

    for (Map.Entry<String, File> e : listGenome.entrySet()) {

      // use option -S to generate SAM output format

      StringBuilder index = new StringBuilder(e.getValue().getAbsolutePath());
      index.append("/");
      index.append(e.getKey());

      String paramFileFastq =
          StringUtils.bashEscaping(fastqFile.getAbsolutePath());
      if (paired) {
        paramFileFastq = paramFileFastq + " "+
            StringUtils.bashEscaping(fastqFilePaired.getAbsolutePath());
      }
      
      String cmd =
          bowtiePath
              + " -l 40 -k 2 --chunkmbs 512 -p 2 " + index.toString() + " "
              + paramFileFastq + " -S";

      StringTokenizer tmp = new StringTokenizer(cmd, " ");
      List<String> cmdBowtie = new ArrayList<String>();
      while (tmp.hasMoreTokens())
        cmdBowtie.add(tmp.nextToken());

      printList(cmdBowtie);
      
      pb = new ProcessBuilder(cmdBowtie);

      // pb =
      // new ProcessBuilder(bowtiePath, "-l", "40", "-k", "2", "--chunkmbs",
      // "512", "-p", "2", index.toString(),
      // StringUtils.bashEscaping(fastqFile.getAbsolutePath()), "-S");

      //LOGGER.info("Command bowtie " + cmdBowtie);

      // pb.directory(e.getValue());
      pb.redirectErrorStream();

      p = pb.start();

      // pmr.setMapReduceTemporaryDirectory(new File("/home/sperrin"));
      pmr.setGenomeReference(e.getKey());
      pmr.doMap(p.getInputStream());
    }

    pmr.doReduce(p.getOutputStream());

    final long endTime = System.currentTimeMillis();

    // System.out.println("count=" + count);
    System.out.println((endTime - startTime));

    System.out.println("\n\n" + pmr.statisticalTableToString());

    // create file screen.txt
    try {
      FileWriter f =
          new FileWriter(StringUtils.bashEscaping(fastqFile.getAbsolutePath())
              + "_screen.txt");
      f.write(pmr.statisticalTableToString());

      //LOGGER.info("Create file result FastqScreen "
      //    + fastqFile.getAbsolutePath() + "_screen.txt");

      f.close();
    } catch (IOException ioe) {
    }

  }

  static void method2() {

    SequenceReadsMapper bowtie = new BowtieReadsMapper();
    Reporter reporter = new Reporter();

    final File[] listArchiveIndexFileDir = {indexDirPhix, indexDirAdapters};
    final File[] listArchiveIndexFile =
        {new File(indexFilePhix), new File(indexFileAdapters)};

    try {
      mapSingleEnd(FastqFormat.FASTQ_SANGER, bowtie, fastqFile,
          listArchiveIndexFile, listArchiveIndexFileDir, reporter);
    } catch (IOException ioe) {

    }
  }

  private static void mapSingleEnd(FastqFormat fastqFormat,
      final SequenceReadsMapper bowtie, final File inFile,
      final File[] archiveIndexFile, final File[] indexDir,
      final Reporter reporter) throws IOException {

    // Init mapper
    bowtie.init(false, fastqFormat, reporter, COUNTER_GROUP);

    // Set mapper arguments
    /*
     * final int mapperThreads = initMapperArguments(mapper,
     * context.getSettings() .getTempDirectoryFile());
     */
    final int mapperThreads = 1;

    // Mapper change defaults arguments
    final int seed = 40;

    final String newArgumentsMapper =
        " -l " + seed + " -k 2 --chunkmbs 512  -p 2 -S";
    bowtie.setMapperArguments(newArgumentsMapper);

    LOGGER
        .info("Map file: "
            + inFile + ", Fastq format: " + fastqFormat + ", use "
            + bowtie.getMapperName() + " with " + mapperThreads
            + " threads option");

    // loop : Process to mapping for each library
    for (int i = 0; i < indexDir.length; i++) {
      bowtie.map(inFile, archiveIndexFile[i], indexDir[i]);
    }

    listOutputFile.add(bowtie.getSAMFile(null));

    for (File f : listOutputFile)
      System.out.println("SAM de bowtie " + f.getName());

  }

  /**
   * TODO : value probably exist in data in fastQC
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
  
  
  static void printList(List<String> l){
    System.out.println("Contenu liste : ");
    for (String s :l){
      System.out.print(s+" ");
    }
    System.out.println();
    }
}