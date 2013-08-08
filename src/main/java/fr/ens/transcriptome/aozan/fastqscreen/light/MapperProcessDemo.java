package fr.ens.transcriptome.aozan.fastqscreen.light;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Charsets;

import fr.ens.transcriptome.aozan.FastqscreenDemo;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.AbstractSequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.BowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.MapperProcess;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class MapperProcessDemo {
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  private static final String EXEC = "/tmp/eoulsan/UNKNOWN_VERSION/bowtie";
  private static final String INDEX_PATH =
      "/home/sperrin/Documents/FastqScreenTest/resources/fastqscreen/mm10/genome";
  private static final int NB_THREAD = 1;
  private static final String TMP =
      "/home/sperrin/Documents/FastqScreenTest/tmp";
  private static final File fastq =
      new File(
          "/home/sperrin/Documents/FastqScreenTest/tmp/aozan_fastq_2013_0143_AGTTCC_L008_R1_001.fastq");

  // "/home/sperrin/Documents/FastqScreenTest/runtest/qc_121116_SNL110_0058_AC11HRACXX/121116_SNL110_0058_AC11HRACXX/Project_microbrain_A2012/Sample_2012_0200/2012_0200_CGATGT_L005_R1_001.millefq");

  public static final void main(String[] args) throws IOException,
      InterruptedException {

    new MapperProcessDemo().testMapperProcessStdinStdoutPartial();
  }

  public void testMapperProcessStdinStdoutPartial() throws IOException,
      InterruptedException {
    // STDIN and STDOUT
    MapperProcess mp =
        new MapperProcess(new BowtieReadsMapper(), false, true, false) {

          protected List<List<String>> createCommandLines() {
            final List<String> cmd = new ArrayList<String>();
            cmd.add(EXEC);
            cmd.add("--phred33-quals");
            cmd.add("-p");
            cmd.add("" + NB_THREAD);
            cmd.add(INDEX_PATH);
            cmd.add("-q");
            // standard input
            cmd.add("-");
            cmd.add("-S");

            System.out.println("cmd " + cmd.toString().replace(',', ' '));
            return Collections.singletonList(cmd);
          }
        };

    // equivalent FileUtils but in the specified thread
    // copy the standard output of mapper process in the file
    parseStdoutStream(mp.getStout(), new File(TMP
        + "/test_mapper_stdinstdout2.sam"));
    parseStdinStream(mp.getStdin());

    // copy in standard input of mapper process ("-") the fastq file
    // long n = FileUtils.copy(new FileInputStream(fastq), mp.getStdin());

    mp.waitFor();
  }

  public void parseStdinStream(final OutputStream os) throws IOException {

    // Thread local = new Thread(new Runnable() {
    //
    // // test : write on OutputStream from MapperProcess one read on two
    // public void run() {
    int compt = 0;
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    int n = 0;
    try {
      BufferedInputStream bis =
          new BufferedInputStream(new FileInputStream(fastq));
      System.out.println("read fastq " + fastq.getAbsolutePath());
      
      while ((n = bis.read(buffer)) != -1) {
        os.write(buffer, 0, n);
        compt += n;
      }

      bis.close();
      os.close();
    } catch (Exception e) {
    }
    // }
    //
    // }, "sdtin");
    //
    // local.start();

    System.out.println("nb bytes in fastq " + compt);
  }

  public void parseStdoutStream(final InputStream is, final File samFile)
      throws IOException {

    Thread local = new Thread(new Runnable() {

      public void run() {
        BufferedReader br =
            new BufferedReader(new InputStreamReader(is, Charsets.ISO_8859_1));
        String line = null;

        try {
          BufferedWriter bw = new BufferedWriter(new FileWriter(samFile));
          while ((line = br.readLine()) != null) {
            bw.write(line);
            bw.write("\n");
          }

          bw.close();
          br.close();
        } catch (Exception e) {
        }
      }

    }, "stdout");

    local.start();

    // System.out.println("sam size " + samFile.length());
  }

  public void testMapperProcessStdinStdout() throws IOException,
      InterruptedException {
    // STDIN and STDOUT
    MapperProcess mp =
        new MapperProcess(new BowtieReadsMapper(), false, true, false) {

          protected List<List<String>> createCommandLines() {
            final List<String> cmd = new ArrayList<String>();
            cmd.add(EXEC);
            cmd.add("--phred33-quals");
            cmd.add("-p");
            cmd.add("" + NB_THREAD);
            cmd.add(INDEX_PATH);
            cmd.add("-q");
            // standard input
            cmd.add("-");
            cmd.add("-S");

            System.out.println("cmd " + cmd.toString().replace(',', ' '));
            return Collections.singletonList(cmd);
          }
        };

    // equivalent FileUtils but in the specified thread
    // copy the standard output of mapper process in the file
    parseStdoutStream(mp.getStout(), new File(TMP
        + "/test_mapper_stdinstdout.sam"));

    // copy in standard input of mapper process ("-") the fastq file
    long n = FileUtils.copy(new FileInputStream(fastq), mp.getStdin());

    mp.waitFor();
    System.out.println("bytes size fastq " + n + "  end");
  }

  public static void testMapperProcessInputFile() throws IOException,
      InterruptedException {

    // mapper
    AbstractSequenceReadsMapper bowtie = new BowtieReadsMapper();
    bowtie.setTempDirectory(new File(TMP));

    // tmp input file
    MapperProcess mp = new MapperProcess(bowtie, false, false, false) {

      protected List<List<String>> createCommandLines() {
        final List<String> cmd = new ArrayList<String>();
        cmd.add(EXEC);
        cmd.add("--phred33-quals");
        cmd.add("-p");
        cmd.add("" + NB_THREAD);
        cmd.add(INDEX_PATH);
        cmd.add("-q");
        // use tmpfile create in constructor of mapperProcess
        cmd.add(getTmpInputFile1().getAbsolutePath());
        cmd.add("-S");

        System.out.println("cmd " + cmd.toString().replace(',', ' '));
        return Collections.singletonList(cmd);
      }
    };

    // copy fastq in standard input of mapper process
    long n = FileUtils.copy(new FileInputStream(fastq), mp.getStdin());

    // copy standard output of mapper process in sam file in specified thread
    File samFile = new File(TMP + "/test_mapper_tmpfile.sam");
    mp.toFile(samFile);

    mp.waitFor();

    System.out.println("bytes size fastq "
        + n + "   sam size " + samFile.length());

  }

  public static void testMapperProcessStdin() throws IOException,
      InterruptedException {
    // STDIN
    MapperProcess mp =
        new MapperProcess(new BowtieReadsMapper(), false, true, false) {

          protected List<List<String>> createCommandLines() {
            final List<String> cmd = new ArrayList<String>();
            cmd.add(EXEC);
            cmd.add("--phred33-quals");
            cmd.add("-p");
            cmd.add("" + NB_THREAD);
            cmd.add(INDEX_PATH);
            cmd.add("-q");
            // standard input
            cmd.add("-");
            cmd.add("-S");

            System.out.println("cmd " + cmd.toString().replace(',', ' '));
            return Collections.singletonList(cmd);
          }
        };

    // equivalent FileUtils but in the specified thread
    // copy the standard output of mapper process in the file
    File samFile = new File(TMP + "/test_mapper_stdin.sam");
    mp.toFile(samFile);

    // copy in standard input of mapper process ("-") the fastq file
    long n = FileUtils.copy(new FileInputStream(fastq), mp.getStdin());

    mp.waitFor();

    System.out.println("bytes size fastq "
        + n + "  sam size " + samFile.length());
  }

  public static void testMapperProcessFile() throws IOException,
      InterruptedException {
    // FILE
    MapperProcess mp =
        new MapperProcess(new BowtieReadsMapper(), true, false, false) {

          protected List<List<String>> createCommandLines() {
            final List<String> cmd = new ArrayList<String>();
            cmd.add(EXEC);
            cmd.add("--phred33-quals");
            cmd.add("-p");
            cmd.add("" + NB_THREAD);
            cmd.add(INDEX_PATH);
            cmd.add("-q");
            // input file : path to the fastq
            cmd.add(fastq.getAbsolutePath());
            cmd.add("-S");

            return Collections.singletonList(cmd);
          }

        };

    File samFile = new File(TMP + "/test_mapper_file.sam");

    // standard output from mapper process, ie data in format sam -> copy in sam
    // file
    long n = FileUtils.copy(mp.getStout(), new FileOutputStream(samFile));

    mp.waitFor();

    System.out.println("bytes size fastq "
        + n + "  sam size " + samFile.length());
  }

  public void parseStdinStream2(final OutputStream os) throws IOException {
    BufferedInputStream bis =
        new BufferedInputStream(new FileInputStream(fastq));
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = bis.read(buffer))) {
      os.write(buffer, 0, n);
      count += n;
    }

    bis.close();
    os.close();

    System.out.println("nb lines in fastq " + count);
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
    new FastqScreen(prop);
  }
}
