/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.math.DoubleMath;

public class FastqScreenResult {

  private static final String[] HEADER_COLUMNS_TAB = {"library", "unmapped",
      "one_hit_one_library", "multiple_hits_one_library",
      "one_hit_multiple_libraries", "multiple_hits_multiple_libraries"};

  // specific legend : represent key in rundata
  private static final String[] LEGEND_RUNDATA = {"unmapped",
      "one.hit.one.library", "multiple.hits.one.library",
      "one.hit.multiple.libraries", "multiple.hits.multiple.libraries"};
  private static final String FINAL_LINE_RUNDATA = "hit.no.libraries";

  // legend for file result
  private static final String HEADER_COLUMNS_TEXT =
      "Library \t %Unmapped \t %One_hit_one_library"
          + "\t %Multiple_hits_one_library \t %One_hit_multiple_libraries \t "
          + "%Multiple_hits_multiple_libraries";
  private static final String FINAL_TEXT = "Hit_no_libraries";

  private Map<String, double[]> resultsPerGenome;
  private double percentHitNoLibraries = 0.0;
  private boolean paired = false;
  private int readsprocessed;
  private int readsMapped;

  /**
   * Print table percent in format use by fastqscreen program with rounding
   * @return
   */
  public String statisticalTableToString() {

    StringBuilder s = new StringBuilder().append(HEADER_COLUMNS_TEXT);

    for (Map.Entry<String, double[]> e : this.resultsPerGenome.entrySet()) {
      double[] tab = e.getValue();
      s.append("\n" + e.getKey());

      for (double n : tab) {
        n = DoubleMath.roundToInt((n * 100.0), RoundingMode.HALF_DOWN) / 100.0;
        s.append("\t" + n);
      }
    }
    double percentHitNoLibrariesRounding =
        DoubleMath.roundToInt((this.percentHitNoLibraries * 100.0),
            RoundingMode.HALF_DOWN) / 100.0;
    s.append("\n\n% Hit_no_libraries : " + percentHitNoLibrariesRounding + "\n");
    return s.toString();
  }

  /**
   * @return list with name of reference genome using for the mapping
   */
  public List<String> nameGenomeList() {
    Set<String> set = this.resultsPerGenome.keySet();

    return new ArrayList<String>(set);
  }

  public File createFileResultFastqScreen(final String outputFilePath) {
    String result = outputFilePath + "_screen.txt";
    try {
      FileWriter fr = new FileWriter(result);
      BufferedWriter br = new BufferedWriter(fr);

      br.append(statisticalTableToString());

      br.close();
      fr.close();
    } catch (IOException io) {
      io.printStackTrace();
    }

    return new File(result);
  }

  //
  // Getter
  //

  public Map<String, double[]> getResultsPerGenome() {
    return this.resultsPerGenome;
  }

  public boolean isPaired() {
    return this.paired;
  }

  public String[] getLegendRunData() {
    return this.LEGEND_RUNDATA;
  }

  public String getFinalLineRunData() {
    return this.FINAL_LINE_RUNDATA;
  }

  public String getHeaderColumnsText() {
    return this.HEADER_COLUMNS_TEXT;
  }

  public String getFinalText() {
    return this.FINAL_TEXT;
  }

  public double getReadsprocessed() {
    return this.readsprocessed;
  }

  public double getPercentHitNoLibraries() {
    return this.percentHitNoLibraries;
  }

  //
  // SETTER
  //

  public void setPercentHitNoLibraries() {
    this.percentHitNoLibraries =
        ((double) (readsprocessed - readsMapped)) / readsprocessed * 100.0;
  }

  public void setPaired(final boolean paired) {
    this.paired = paired;
  }

  public void setReadsprocessed(final int readsprocessed) {
    this.readsprocessed = readsprocessed;
  }

  //
  // CONSTRUCTOR
  //

  public FastqScreenResult(final Map<String, double[]> result,
      final int readsMapped, final int readsprocessed) {
    this.resultsPerGenome = result;
    this.readsMapped = readsMapped;
    this.readsprocessed = readsprocessed;

    this.setPercentHitNoLibraries();
  }

}
