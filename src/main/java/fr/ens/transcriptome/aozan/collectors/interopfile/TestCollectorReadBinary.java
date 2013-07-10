package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.RunInfoCollector;

public class TestCollectorReadBinary {

  private static final String DIR = "/home/sperrin/shares-net/sequencages/runs";
  private static final String DIR_TMP =
      "/home/sperrin/Documents/FastqScreenTest/tmp/interOp";

  public static final boolean PRINT_DETAIL = false;
  static File file = null;
  static BufferedWriter bw = null;

  public static void main(String[] argv) {
    file = new File(DIR_TMP, "comparaison_all_run_20130704.data");

    try {
      System.setOut(new PrintStream(new FileOutputStream(new File(DIR_TMP,
          "redir_consoleoutput_all_run_20130704.data"))));

    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    }

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
    // "/hiseq_log_130326_SNL110_0066_AD1GG4ACXX/"));
    // collectRun(new File(DIR_TMP +
    // "/hiseq_log_130621_SNL110_0073_AD1Y8WACXX/"));
    // collectRun(new File(DIR_TMP +
    // "/hiseq_log_120830_SNL110_0055_AD16D9ACXX/"));
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
    Properties props = new Properties();
    props.put(QC.RTA_OUTPUT_DIR, DIR_TMP + "/hiseq_log_" + runId);
    runInfoColl.configure(props);

    runInfoColl.collect(data);

    System.out.println(runId
        + ": " + interOpPath + "- - - - - - - - - - - - - - - - - - - - - - -");

    AbstractBinaryIteratorReader.setDirectory(interOpPath);

    new TileMetricsOutReader().collect(data);
    new ErrorMetricsOutReader().collect(data);
    new ExtractionMetricsOutReader().collect(data);

  }

  private List<Critere> getListCritere(int read, int lane) {
    List<Critere> keys = Lists.newArrayList();
    keys.add(new Critere("read" + read + ".density.ratio", Double.class,
        Epsilon.DOUBLE_3));
    // keys.add(new Critere("read" + read + ".type", String.class,
    // Epsilon.IDEM));
    keys.add(new Critere("read" + read + ".lane" + lane + ".called.cycles.max",
        Integer.class, Epsilon.IDEM));
    keys.add(new Critere("read" + read + ".lane" + lane + ".called.cycles.min",
        Integer.class, Epsilon.IDEM));
    keys.add(new Critere("read" + read + ".lane" + lane + ".clusters.pf",
        Double.class, Epsilon.CENTAINE));
    keys.add(new Critere("read" + read + ".lane" + lane + ".clusters.pf.sd",
        Double.class, Epsilon.CENTAINE));
    keys.add(new Critere("read" + read + ".lane" + lane + ".clusters.raw",
        Double.class, Epsilon.CENTAINE));
    keys.add(new Critere("read" + read + ".lane" + lane + ".clusters.raw.sd",
        Double.class, Epsilon.CENTAINE));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.100",
        Double.class, Epsilon.DOUBLE_2));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.100.sd",
        Double.class, Epsilon.DOUBLE_3));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.35",
        Double.class, Epsilon.DOUBLE_2));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.35.sd",
        Double.class, Epsilon.DOUBLE_3));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.75",
        Double.class, Epsilon.DOUBLE_2));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.75.sd",
        Double.class, Epsilon.DOUBLE_3));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.phix",
        Double.class, Epsilon.DOUBLE_2));
    keys.add(new Critere("read" + read + ".lane" + lane + ".err.rate.phix.sd",
        Double.class, Epsilon.DOUBLE_3));
    keys.add(new Critere(
        "read" + read + ".lane" + lane + ".first.cycle.int.pf", Integer.class,
        Epsilon.DIZAINE));
    keys.add(new Critere("read"
        + read + ".lane" + lane + ".first.cycle.int.pf.sd", Double.class,
        Epsilon.UNITE));
    keys.add(new Critere("read" + read + ".lane" + lane + ".phasing",
        Double.class, Epsilon.DOUBLE_3));
    keys.add(new Critere("read" + read + ".lane" + lane + ".prc.align",
        Double.class, Epsilon.DOUBLE_2));
    keys.add(new Critere("read" + read + ".lane" + lane + ".prc.align.sd",
        Double.class, Epsilon.DOUBLE_3));
    keys.add(new Critere("read"
        + read + ".lane" + lane + ".prc.intensity.after.20.cycles.pf",
        Double.class, Epsilon.DOUBLE_1));
    keys.add(new Critere("read"
        + read + ".lane" + lane + ".prc.intensity.after.20.cycles.pf.sd",
        Double.class, Epsilon.DOUBLE_2));
    keys.add(new Critere("read" + read + ".lane" + lane + ".prc.pf.clusters",
        Double.class, Epsilon.DOUBLE_1));
    keys.add(new Critere(
        "read" + read + ".lane" + lane + ".prc.pf.clusters.sd", Double.class,
        Epsilon.DOUBLE_1));
    keys.add(new Critere("read" + read + ".lane" + lane + ".prephasing",
        Double.class, Epsilon.DOUBLE_3));
    keys.add(new Critere("read" + read + ".lane" + lane + ".tile.count",
        Integer.class, Epsilon.CENTAINE));
    return keys;
  }

  private class Critere {

    String key;
    Class<?> clazz;
    Epsilon eps;

    Critere(final String key, final Class<?> clazz, final Epsilon epsilon) {
      this.key = key;
      this.clazz = clazz;
      this.eps = epsilon;
    }

    public String compare(RunData ori, RunData test, String runId) {

      String s = "";

      if (this.clazz == Integer.class) {
        int val_ori = ori.getInt(key);
        int val_test = test.getInt(key);
        int diff = Math.abs(val_test - val_ori);

        boolean res = diff <= eps.getValue();
        s =
            String.format("%s \tdiff %s \tori %s \ttest %s", res, diff,
                val_ori, val_test);

      } else if (this.clazz == Double.class) {

        double val_ori = ori.getDouble(key);
        double val_test = test.getDouble(key);
        double diff = Math.abs(val_test - val_ori);

        boolean res = diff <= eps.getValue();
        s =
            String.format("%s \tdiff %.4f \tori %.3f \ttest %.3f", res, diff,
                val_ori, val_test);

      } else if (this.clazz == String.class) {

        String val_ori = ori.get(key);
        String val_test = test.get(key);

        boolean res = false;

        if (val_ori == null)
          res = val_test == null;
        else
          res = val_ori.equals(val_test);

        s = String.format("%s \tori %.3f \ttest %.3f", res, val_ori, val_test);
      }

      return s + "\t " + runId + "\t" + key;
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
