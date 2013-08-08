package fr.ens.transcriptome.aozan.fastqscreen.light;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.FastqscreenDemo;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenResult;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.IlluminaReadId;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

public class EssaiPartialFastq {

  static final String dir =
      "/home/sperrin/Documents/FastqScreenTest/fqs_light/sources/";

  static String dirRun = "/home/sperrin/Documents/FastqScreenTest/runtest/";

  static final int MAX_SIZE_BLOC = 1200000;
  static final File filePhix = new File(dir, "phix.fastq");
  static final File fileSample = new File(dir + "/imagifngs/okaz-m3/",
      "okaz-m3_TTAGGC_L008_R1_001.fastq");

  /** Parameters test */
  final static double proportion = 0.01;
  final static int nbFastqFileTest = 3;
  final static double[] partFileToRead = new double[] {0.02}; // , 0.05, 0.07,
  // 0.1};
  // ----------------------------------------------------------------------------------------

  static String runId;
  static String sample;
  static String projet;
  static String fileSrc;
  static int lane;
  static int read;
  static String index;
  static int fastqSize; // nb reads
  static String genomeSample;

  static FastqScreen fastqscreen = null;

  public static void main(String[] argv) {

    /** Parameters fastq file */
    /** TEST : ribosomal contamination */
    // runId = "130326_SNL110_0066_AD1GG4ACXX";
    // sample = "2013_0019";
    // projet = "yakafocon3_A2012";
    // fileSrc = "";
    // lane = 1;
    // read = 1;
    // index = "CGATGT";
    // fastqSize = 45298094; // nb reads
    // genomeSample = "C_albicans";
    //
    // launch_test();

    /** TEST : fastq classique run 58 */
    runId = "121116_SNL110_0058_AC11HRACXX";
    sample = "2012_0200";
    projet = "microbrain_A2012";
    fileSrc = "";
    lane = 5;
    read = 1;
    index = "CGATGT";
    fastqSize = 36039833; // nb reads
    genomeSample = "mm10";

    launch_test();

    // runId = "121116_SNL110_0058_AC11HRACXX";
    // sample = "2012_0200";
    // projet = "microbrain_A2012";
    // fileSrc = "";
    // lane = 5;
    // read = 1;
    // index = "CGATGT";
    // fastqSize = 36039833; // nb reads
    // genomeSample = "mm10";
    //
    // launch_test();

    /** TEST : bad tiles and enormous fastq file */
    // runId = "130604_SNL110_0070_AD26NTACXX";
    // sample = "ssDNA";
    // projet = "imagifngs_D2013";
    // fileSrc = "";
    // lane = 8;
    // read = 1;
    // index = "NoIndex";
    // fastqSize = 131791620; // nb reads
    // genomeSample = "";
    //
    // launch_test();
  }

  public static void launch_test() {
    // File fastq =
    // new File(dirRun
    // + "/qc_"
    // + runId
    // + "/"
    // + runId
    // + String.format(
    // "/Project_%s/Sample_%s/%s_%s_L%03d_R%d_001.fastq.bz2", projet,
    // sample, sample, index, lane, read));

    File fastq =
        new File(
            "/home/sperrin/Documents/FastqScreenTest/fqs_light/sources/Project_microbrain_A2012/"
                + String.format("%s_%s_L%03d_R%d_001.fastq.bz2", sample, index,
                    lane, read));

    Stopwatch timer = new Stopwatch().start();
    // Build tmp partial fastq files to execute fastqscreen

    int nbReadsToCopy = 200000;
    int count_pas = 20000;
    int count_end = nbReadsToCopy;
    boolean random = true;

    File dataFile =
        new File(dirRun + "/qc_" + runId + "/" + runId + "_qc_tmp", "data-"
            + runId + ".txt");

    final String key_pf_reads_sample =
        "demux.lane"
            + lane + ".sample." + sample + ".read" + read + ".pf.cluster.count";

    RunData data = null;
    try {
      data = new RunData(dataFile);
    } catch (IOException io) {
      io.printStackTrace();
    }

    int countPFReads = data.getInt(key_pf_reads_sample);

    while (nbReadsToCopy <= count_end) {

      double partial = (double) nbReadsToCopy / countPFReads;

      File tmpFile =
          createPartFilteredFastqFile(fastq, partial, nbReadsToCopy, index
              + "-" + nbReadsToCopy, random);

      // File tmpFile =
      // createPartFastqFile(fastq, partial, index + "-" + nbReadsToCopy,
      // countPFReads, random);

      String timerCreateFile =
          toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS));
      System.out.println("timer create file "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

      timer.reset();
      timer.start();
      String pcrMapped =
          executeFastqScreen(index + "-" + nbReadsToCopy, tmpFile, random);

      System.out.print(index
          + "-" + nbReadsToCopy + "\t part_readed \t" + nbReadsToCopy
          + "\t pcr_mapped \t" + pcrMapped);

      String timerExecuteFqs =
          toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS));

      System.out.println("\ttimer_fqs "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));
      String result =
          "TEST\t"
              + projet + "\t" + sample + "\t" + timerCreateFile + "\t"
              + timerExecuteFqs + "\t" + pcrMapped;
      writeInFile(result);

      System.out.println(result);

      timer.reset();
      timer.start();
      // Delete tmp file
      tmpFile.delete();

      nbReadsToCopy += count_pas;
    }
  }

  static Map<String, File> initListFastqTest(final boolean createFile) {
    Map<String, File> fastqTest = Maps.newHashMap();

    if (createFile) {
      String index = "CENT";
      // Build set fastq files with a specific part of phix read
      for (int i = 0; i < nbFastqFileTest; i++) {
        String f =
            dir
                + "/imagifngs/okaz-m3/"
                + String.format("%s_%s_L%03d_R%d_001.fastq", sample, index
                    + "-" + i, lane, read);

        if (new File(f).exists())
          new File(f).delete();

        buildFastqFile(new File(f));
        fastqTest.put(index + "-" + i, new File(f));
      }

    } else {
      // Used a list of real fastq file

      String f =
          dir
              + String.format(
                  "/Project_%s/Sample_%s/%s_%s_L%03d_R%d_001.fastq", projet,
                  sample, sample, index, lane, read);
      fastqTest.put(index, new File(f));

    }
    return fastqTest;
  }

  /**
   * Create a temporary file with the parse filtered reads. Use the class
   * fastqReader for filtering
   * @param src fastqFile compressed
   * @param part part of file to read
   * @param index for the sample
   * @param random true if selection randomly reads in fastq file
   * @return temporary fastq file
   */
  static File createPartFilteredFastqFile(File src, double part,
      int nbReadsToCopy, String index, boolean random) {

    File tmpFile = new File(dir, "fastq_partiel.fastq");

    try {

      FileWriter tmpOutput = new FileWriter(tmpFile);

      CompressionType zType =
          CompressionType.getCompressionTypeByFilename(src.getName());

      InputStream is = zType.createInputStream(new FileInputStream(src));

      FastqReader fastqReader = new FastqReader(is);

      int comptReadsPF = 1;
      final int step = (int) (1 / part);

      System.out.println("part "
          + part + " nb read to copy " + String.format("%,d", nbReadsToCopy)
          + " pas " + step);

      IlluminaReadId ill = null;

      for (final ReadSequence seq : fastqReader) {

        if (ill == null)
          ill = new IlluminaReadId(seq);
        else
          ill.parse(seq);

        if (!ill.isFiltered()) {
          if (comptReadsPF % step == 0) {
            // Write in tmp fastq file
            tmpOutput.write(seq.toFastQ());
            tmpOutput.flush();

            // System.out.println(nbReadToCopy + " -- " + seq.toFastQ());

            if (--nbReadsToCopy <= 0) {
              System.out.println("get out loop readSeq");
              break;
            }
          }

          comptReadsPF++;
        }

      }

      // Throw an exception if an error has occured while reading data
      fastqReader.throwException();

      fastqReader.close();
      tmpOutput.close();

    } catch (IOException e) {
      e.printStackTrace();
    } catch (EoulsanException ee) {
      ee.printStackTrace();
    } catch (BadBioEntryException bad) {
      bad.printStackTrace();
    }
    return tmpFile;

  }

  static File createPartFastqFile(File src, double part, String index,
      int fastqSize, boolean random) {
    File file =
        new File(dir
            + String.format("/Project_%s/Sample_%s/%s_%s_L%03d_R%d_001.fastq",
                projet, sample, sample, index, lane, read));

    // System.out.println(src.exists() + " src  " + src.getAbsolutePath());
    // System.out.println(file.exists() + " part " + file.getAbsolutePath());

    BufferedReader br = null;
    BufferedWriter bw = null;
    int nbReadToCopy = (int) (fastqSize * part);

    try {
      InputStream is = new FileInputStream(src);

      br =
          new BufferedReader(
              new InputStreamReader(new BZip2CompressorInputStream(is),
                  Charset.forName("ISO-8859-1")));
      // br = new BufferedReader(new FileReader(src));
      bw = new BufferedWriter(new FileWriter(file));

      if (random) {
        int comptLine = 1;
        final int pas = (int) (1 / part) * 4;

        System.out.println("part "
            + part + " nb read to copy " + nbReadToCopy + " pas " + pas);

        // TODO check correct syntaxe start read reading in fastq
        while (nbReadToCopy > 0) {
          if ((comptLine + 3) % pas == 0) {
            // if (comptLine < 2000)
            // System.out.println("copy line " + comptLine);
            // Write a read in file
            bw.write(br.readLine());
            bw.write("\n");
            bw.write(br.readLine());
            bw.write("\n");
            bw.write(br.readLine());
            bw.write("\n");
            bw.write(br.readLine());
            bw.write("\n");
            nbReadToCopy--;
          } else {
            // Skip a read
            br.readLine();
            br.readLine();
            br.readLine();
            br.readLine();
          }
          comptLine += 4;
        }
      } else {

        int nbLineToCopy = nbReadToCopy * 4;

        while (nbLineToCopy > 0) {
          bw.write(br.readLine());
          bw.write("\n");
          nbLineToCopy--;
        }

      }

      br.close();
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        br.close();
        bw.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

    }

    return file;
  }

  /**
   * @param proportion
   * @param fastqSize
   */
  static void buildFastqFile(final File fastq) {

    long nbReads = fastqSize;

    BufferedReader brPhix = null;
    BufferedReader brSample = null;
    BufferedWriter bw = null;
    try {
      brPhix = new BufferedReader(new FileReader(filePhix));
      brSample = new BufferedReader(new FileReader(fileSample));
      bw = new BufferedWriter(new FileWriter(fastq, true));

      while (nbReads > 0) {
        int sizeBloc =
            (int) ((nbReads > MAX_SIZE_BLOC) ? MAX_SIZE_BLOC : nbReads);
        nbReads -= sizeBloc;

        int nbReadPhixBloc = new Double(sizeBloc * proportion).intValue();
        int nbReadSampleBloc =
            new Double(sizeBloc * (1 - proportion)).intValue();

        List<Read> list = newArrayListWithCapacity(sizeBloc);

        addRead(list, brPhix, nbReadPhixBloc);
        addRead(list, brSample, nbReadSampleBloc);

        Collections.shuffle(list);
        // print(list);

        for (Read r : list) {
          bw.write(r.toString());
        }

      }
      brPhix.close();
      brSample.close();
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  /**
   * @param list
   * @param file
   * @param nbRead
   */
  public static void addRead(List<Read> list, final BufferedReader br,
      final int nbRead) throws IOException {
    String l1 = "";
    String l2 = "";
    String l3 = "";
    String l4 = "";

    try {
      for (int i = 0; i < nbRead; i++) {
        l1 = br.readLine();
        l2 = br.readLine();
        l3 = br.readLine();
        l4 = br.readLine();
        list.add(new Read(l1, l2, l3, l4));
      }
    } catch (EOFException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param list
   */
  static void print(List<Read> list) {
    int c = 0;

    for (Read r : list) {
      System.out.print(r.isReadPhix() ? "X" : "-");

      if (++c % 200 == 0)
        System.out.println();
    }
  }

  /**
   * @param lane
   * @param read
   * @param sample
   * @param index
   */
  static String executeFastqScreen(final String index, final File partFile,
      final boolean random) {

    List<String> genomes =
        Lists.newArrayList("phix", "adapters", "lsuref_106", "ssuref_106",
            "lsuref_111", "ssuref_111", genomeSample);

    FastqSample fastqSample = new FastqSample(dir, 1, 1, sample, projet, index);

    StringBuilder s = new StringBuilder();
    try {
      FastqScreenResult result =
          fastqscreen.execute(partFile, fastqSample, genomes, null, false);

      // System.out.println(result.statisticalTableToString(null));
      System.out.println(result.getCountReadsMapped()
          + "  " + result.getCountReadsProcessed());
      s.append(result.getCountReadsMapped());
      s.append("\t");
      s.append(result.getCountReadsProcessed());
      s.append("\t");

      RunData data = result.createRundata("TEST");
      File dataFile =
          new File(dir + "/Project_" + projet, "data_"
              + index + "_mp" + ".data");
      data.createRunDataFile(dataFile);

      for (String g : genomes) {
        if (g.length() > 0) {
          s.append(data.getDouble("TEST" + "." + g + ".mapped.percent"));
          s.append("\t");
        }
      }
      // s.append("\n");

    } catch (Exception e) {
      e.printStackTrace();
    }
    return s.toString();
  }

  static void writeInFile(String result) {
    try {
      BufferedWriter bw =
          new BufferedWriter(new FileWriter(new File(
              dir + "/Project_" + projet, "result_pf_mp.csv"), true));

      bw.write(result);
      bw.write("\n");
      bw.flush();
      bw.close();
    } catch (Exception e) {

    }

  }

  static {
    Properties prop = new Properties();
    // prop.setProperty(FastqScreen.KEY_NUMBER_THREAD, "1");
    prop.setProperty(FastqScreen.KEY_TMP_DIR, FastqscreenDemo.TMP_DIR);

    prop.setProperty(FastqScreen.KEY_GENOMES_DESC_PATH,
        FastqscreenDemo.GENOMES_DESC_PATH);
    prop.setProperty(FastqScreen.KEY_GENOMES_PATH, FastqscreenDemo.GENOMES_PATH);
    prop.setProperty(FastqScreen.KEY_MAPPERS_INDEXES_PATH,
        FastqscreenDemo.MAPPERS_INDEXES_PATH);
    fastqscreen = new FastqScreen(prop);
  }

  //
  // Classe interne
  //

  static class Read {
    String lane1;
    String lane2;
    String lane3;
    String lane4;
    boolean readPhix;

    Read(final String lane1, final String lane2, final String lane3,
        final String lane4) {
      this.lane1 = lane1;
      this.lane2 = lane2;
      this.lane3 = lane3;
      this.lane4 = lane4;

      if (this.lane3 != null)
        readPhix = this.lane3.equals("+phix");
      else
        this.lane3 = "+";
    }

    boolean isReadPhix() {
      return readPhix;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(lane1);
      sb.append("\n");
      sb.append(lane2);
      sb.append("\n");
      sb.append(lane3);
      sb.append("\n");
      sb.append(lane4);
      sb.append("\n");
      return sb.toString();
    }
  }
}
