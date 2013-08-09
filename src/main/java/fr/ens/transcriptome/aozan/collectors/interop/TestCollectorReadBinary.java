package fr.ens.transcriptome.aozan.collectors.interop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.Collector;
import fr.ens.transcriptome.aozan.collectors.PhasingCollector;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;

public class TestCollectorReadBinary {

  private static final String DIR = "/home/sperrin/shares-net/sequencages/runs";
  private static final String DIR_TMP =
      "/home/sperrin/Documents/FastqScreenTest/tmp/interOp";
  private static final String DIR_RESULT =
      "/home/sperrin/Documents/FastqScreenTest/reading_interop";

  public static final boolean PRINT_DETAIL = false;
  static File file = null;
  static BufferedWriter bw = null;

  public static void main(String[] argv) {
    String v = "78";
    file = new File(DIR_RESULT, "comparaison_all_run_" + v + ".data");

    // Copy console output in a file
    // try {
    // System.setOut(new PrintStream(new FileOutputStream(new File(DIR_RESULT,
    // "redir_consoleoutput_all_run_"+ v +".data"))));
    //
    // } catch (FileNotFoundException e1) {
    // e1.printStackTrace();
    // }

    try {
      bw = new BufferedWriter(new FileWriter(file));
      new TestCollectorReadBinary().testCollectorAllRun();
      bw.close();
    } catch (Exception e) {
      if (file.exists())
        file.delete();
    }
  }

  public void testCollectorAllRun() {
    List<File> runs =
        Arrays.asList(new File(DIR_TMP).listFiles(new FileFilter() {

          @Override
          public boolean accept(final File pathname) {
            return !pathname.getName().endsWith("tar.bz2")
                && pathname.getName().contains("_SN");
          }
        }));

    for (File f : runs) {
      collectRun(f);
    }

    // collectRun(new File(DIR_TMP +
    // "/hiseq_log_120830_SNL110_0055_AD16D9ACXX/"));
    // collectRun(new File(DIR_TMP +
    // "/hiseq_log_130621_SNL110_0073_AD1Y8WACXX/"));
    // collectRun(new File(DIR_TMP +
    // "/hiseq_log_130507_SNL110_0068_AD20WUACXX/"));
  }

  private void collectRun(final File dirRun) {
    String runId = dirRun.getName();
    int pos = "hiseq_log_".length();
    runId = runId.substring(pos);

    String interOpPath = dirRun.getAbsolutePath() + "/InterOp/";
    String dataPath =
        DIR + "/" + runId + "/qc_" + runId + "/data-" + runId + ".txt";

    if (!new File(dataPath).exists())
      return;

    RunData dataOriginal = null;
    try {
      dataOriginal = new RunData(new File(dataPath));
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    RunData dataTest = new RunData();
    try {
      collect(dataTest, interOpPath, runId);

      compareRunData(dataOriginal, dataTest, runId);

    } catch (AozanException ae) {
      ae.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private void compareRunData(final RunData ori, final RunData test,
      final String runId) throws Exception {
    int reads = ori.getInt("run.info.read.count");
    int lanes = ori.getInt("run.info.flow.cell.lane.count");

    System.out.println(runId + "- - - - - - - - - - - - - - - - - - - - - - -");
    bw.write(runId + "- - - - - - - - - - - - - - - - - - - - - - -\n");

    for (int lane = 1; lane <= lanes; lane++) {
      int comptFalse = 0;
      int count = 0;

      for (int read = 1; read <= reads; read++) {
        List<Critere> keys = getListCritere(read, lane);
        count = keys.size() * reads;

        for (Critere c : keys) {
          String s = c.compare(ori, test, runId);
          bw.write(s + "\n");
          if (PRINT_DETAIL)
            System.out.println(s);

          // LOGGER.info(s);
          comptFalse += (s.startsWith("false") ? 1 : 0);
        }
      }
      bw.write("BILAN "
          + runId + ":" + lane + "  " + comptFalse + " false on " + count
          + "\n");
      System.out.println("BILAN "
          + runId + ":" + lane + "  " + comptFalse + " false on " + count);
    }
  }

  private void collect(final RunData data, final String interOpPath,
      final String runId) throws AozanException, IOException {
    // Call RunInfoCollector

    RunInfoCollector runInfoColl = new RunInfoCollector();
    PhasingCollector phasingColl = new PhasingCollector();

    Properties props = new Properties();
    props.put(QC.RTA_OUTPUT_DIR, DIR_TMP + "/hiseq_log_" + runId);
    props.put(QC.CASAVA_OUTPUT_DIR,
        "/home/sperrin/shares-net/sequencages/fastq/" + runId);

    // Collector configuration
    props.put("readXMLCollector.used", "false");
    props.put("cluster.density.ratio", "0.3472222");

    runInfoColl.configure(props);
    phasingColl.configure(props);

    runInfoColl.collect(data);
    phasingColl.collect(data);

    System.out.println(runId
        + ": " + interOpPath + "- - - - - - - - - - - - - - - - - - - - - - -");

    // AbstractBinaryFileReader.setDirectory(interOpPath);
    List<Collector> collectors = Lists.newArrayList();
    collectors.add(new TileMetricsCollector());
    collectors.add(new ErrorMetricsCollector());
    collectors.add(new ExtractionMetricsCollector());

    // configure collector
    for (Collector c : collectors) {
      c.configure(props);
      c.collect(data);
    }
  }

  private List<Critere> getListCritere(int read, int lane) {
    List<Critere> keys = Lists.newArrayList();
    keys.add(new Critere("read" + read + ".density.ratio", Double.class,
        Epsilon.DOUBLE_3, "tile"));
    // keys.add(new Critere("read" + read + ".type", String.class,
    // Epsilon.IDEM));
    keys.add(new Critere("read" + read + ".lane" + lane + ".called.cycles.max",
        Integer.class, Epsilon.IDEM, "tile"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".called.cycles.min",
        Integer.class, Epsilon.IDEM, "tile"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".clusters.pf",
        Double.class, Epsilon.CENTAINE, "tile"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".clusters.pf.sd",
        Double.class, Epsilon.CENTAINE, "tile"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".clusters.raw",
        Double.class, Epsilon.CENTAINE, "tile"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".clusters.raw.sd",
        Double.class, Epsilon.CENTAINE, "tile"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.100",
        Double.class, Epsilon.DOUBLE_2, "err"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.100.sd",
        Double.class, Epsilon.DOUBLE_3, "err"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.35",
        Double.class, Epsilon.DOUBLE_2, "err"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.35.sd",
        Double.class, Epsilon.DOUBLE_3, "err"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.75",
        Double.class, Epsilon.DOUBLE_2, "err"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.75.sd",
        Double.class, Epsilon.DOUBLE_3, "err"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.phix",
        Double.class, Epsilon.DOUBLE_2, "err"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.phix.sd",
        Double.class, Epsilon.DOUBLE_3, "err"));
    keys.add(new Critere(
        "read" + read + ".lane" + lane + ".first.cycle.int.pf", Integer.class,
        Epsilon.DIZAINE, "int"));
    keys.add(new Critere("read"
        + read + ".lane" + lane + ".first.cycle.int.pf.sd", Double.class,
        Epsilon.UNITE, "int"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".prc.align",
        Double.class, Epsilon.DOUBLE_2, "tile"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".prc.align.sd",
        Double.class, Epsilon.DOUBLE_3, "tile"));
    keys.add(new Critere("read"
        + read + ".lane" + lane + ".prc.intensity.after.20.cycles.pf",
        Double.class, Epsilon.DOUBLE_1, "int"));
    keys.add(new Critere("read"
        + read + ".lane" + lane + ".prc.intensity.after.20.cycles.pf.sd",
        Double.class, Epsilon.DOUBLE_2, "int"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".prc.pf.clusters",
        Double.class, Epsilon.DOUBLE_1, "tile"));
    keys.add(new Critere(
        "read" + read + ".lane" + lane + ".prc.pf.clusters.sd", Double.class,
        Epsilon.DOUBLE_1, "tile"));
    keys.add(new Critere("read" + read + ".lane" + lane + ".tile.count",
        Integer.class, Epsilon.CENTAINE, "tile"));

    // keys.add(new Critere("read" + read + ".lane" + lane + ".phasing",
    // Double.class, Epsilon.DOUBLE_3, "tile"));
    // keys.add(new Critere("read" + read + ".lane" + lane + ".prephasing",
    // Double.class, Epsilon.DOUBLE_3, "tile"));

    // use Phasing Collector
    keys.add(new Critere("phasing.read" + read + ".lane" + lane + ".phasing",
        "read" + read + ".lane" + lane + ".phasing", Double.class,
        Epsilon.DOUBLE_3, "phas"));
    keys.add(new Critere(
        "phasing.read" + read + ".lane" + lane + ".prephasing", "read"
            + read + ".lane" + lane + ".prephasing", Double.class,
        Epsilon.DOUBLE_3, "phas"));
    return keys;
  }

  private static class Critere {

    String keySource = null;
    String key;
    Class<?> clazz;
    Epsilon eps;
    String fileInterOpSource;

    Critere(final String key, final Class<?> clazz, final Epsilon epsilon,
        final String fileName) {
      this.key = key;
      this.clazz = clazz;
      this.eps = epsilon;
      fileInterOpSource = fileName;
    }

    Critere(final String keySrc, final String keyTest, final Class<?> clazz,
        final Epsilon epsilon, final String fileName) {
      this(keyTest, clazz, epsilon, fileName);
      keySource = keySrc;
    }

    public String compare(RunData ori, RunData test, String runId) {
      keySource = keySource == null ? key : keySource;

      String s = "bool\tdiff \tdprc \tori \ttest \trund_id";

      // try {

      if (this.clazz == Integer.class) {
        int val_ori = ori.getInt(keySource);

        int val_test = test.getInt(key);
        int diff = Math.abs(val_test - val_ori);

        boolean res = diff <= eps.getValue();
        s =
            String.format("%s \t %s \t     \t %s \t %s", res, diff, val_ori,
                val_test);

      } else if (this.clazz == Double.class) {

        double val_ori = ori.getDouble(keySource);
        val_ori *= (fileInterOpSource.equals("phas") ? 100 : 1);

        double val_test = test.getDouble(key);
        double diff = Math.abs(val_test - val_ori);

        boolean res = diff <= eps.getValue();
        s =
            String.format("%s \t %.4f \t %.2f \t %.4f \t %.4f", res, diff,
                (val_ori == 0.0 ? 0.0 : ((diff / val_ori) * 100)), val_ori,
                val_test);

      } else if (this.clazz == String.class) {

        String val_ori = ori.get(keySource);
        String val_test = test.get(key);

        boolean res = false;

        if (val_ori == null)
          res = val_test == null;
        else
          res = val_ori.equals(val_test);

        s = String.format("%s \t     \t %sf \t %sf", res, val_ori, val_test);
      }

      // } catch (NullPointerException e) {
      // System.out.println(runId + "\tkey pbl " + key);
      // }

      return s
          + "\t " + runId + "\t" + keySource + "\t"
          + fileInterOpSource.toUpperCase();
    }
  }

  private enum Epsilon {

    IDEM(0.0), UNITE(1.), DIZAINE(10.), CENTAINE(100.), DOUBLE_1(0.1),
    DOUBLE_2(0.01), DOUBLE_3(0.001);

    final double value;

    double getValue() {
      return value;
    }

    Epsilon(final double val) {
      this.value = val;
    }
  }
}
