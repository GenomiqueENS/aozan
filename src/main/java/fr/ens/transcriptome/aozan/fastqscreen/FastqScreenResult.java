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

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;

public class FastqScreenResult {

  private static final String FINAL_LINE_RUNDATA = "hitnolibraries";
  private static final String FINAL_TEXT = "Hit_no_libraries";
  private static final String HEADER_COLUMNS_TAB =
      "library \t unmapped \t one_hit_one_library \t multiple_hits_one_library "
          + "\t one_hit_multiple_libraries \t multiple_hits_multiple_libraries";

  private static final String HEADER_COLUMNS_TEXT =
      "Library \t %Unmapped \t %One_hit_one_library"
          + "\t %Multiple_hits_one_library \t %One_hit_multiple_libraries \t "
          + "%Multiple_hits_multiple_libraries";

  private Map<String, DataPerGenome> resultsPerGenome;
  private double percentHitNoLibraries = 0.0;
  private boolean countPercentOk = false;

  /**
   * Print table percent in format use by fastqscreen program with rounding
   * @return
   */
  public String statisticalTableToString() {

    StringBuilder s =
        new StringBuilder().append("\n" + HEADER_COLUMNS_TEXT + "\n");

    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
      s.append(e.getValue().getAllPercentValues() + "\n");
    }

    double percentHitNoLibrariesRounding =
        DoubleMath.roundToInt((this.percentHitNoLibraries * 100.0),
            RoundingMode.HALF_DOWN) / 100.0;
    s.append("\n% Hit_no_libraries : " + percentHitNoLibrariesRounding + "\n");

    return s.toString();
  }

  /**
   * @param outputFilePath
   * @return
   */
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

  public void addGenome(final String genome) {
    this.resultsPerGenome.put(genome, new DataPerGenome(genome));
  }

  public void countHitPerGenome(final String genome, final boolean oneHit,
      final boolean oneGenome) {
    this.resultsPerGenome.get(genome).countHitPerGenome(oneHit, oneGenome);
  }

  public void countPercentValue(final int readsMapped, final int readsprocessed)
      throws AozanException {

    if (this.resultsPerGenome.isEmpty())
      throw new AozanException(
          "During fastqScreen execusion : no genome receive");

    countPercentOk = true;

    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
      e.getValue().countPercentValue(readsprocessed);
    }
    this.percentHitNoLibraries =
        ((double) (readsprocessed - readsMapped)) / readsprocessed * 100.0;
  }

  public void rundata(final RunData data, final String prefix)
      throws AozanException {

    if (this.resultsPerGenome.isEmpty())
      throw new AozanException(
          "During fastqScreen execusion : no genome receive");

    if (!countPercentOk)
      throw new AozanException(
          "During fastqScreen execusion : no value ​for each genome");

    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
      e.getValue().rundata(data, prefix);
    }

    // print last line of report FastqScreen
    data.put(prefix + "." + FINAL_LINE_RUNDATA, this.percentHitNoLibraries);
    // TODO to remove
    System.out.println("add in runData \t"
        + prefix + "." + FINAL_LINE_RUNDATA + "=" + percentHitNoLibraries);
  }

  //
  // CONSTRUCTOR
  //

  public FastqScreenResult() {
    this.resultsPerGenome = new HashMap<String, DataPerGenome>();
  }

  //
  // Internal class
  //

  public class DataPerGenome {

    private String genome;

    // specific legend : represent key in rundata
    private String unMappedLegend = "unmapped";
    private String oneHitOneLibraryLegend = "one.hit.one.library";
    private String multipleHitsOneLibraryLegend = "multiple.hits.one.library";
    private String oneHitMultipleLibrariesLegend = "one.hit.multiple.libraries";
    private String multipleHitsMultipleLibrariesLegend =
        "multiple.hits.multiple.libraries";
    private String mappedLegend = "mapped";

    private double oneHitOneLibraryPercent = 0.0;
    private double multipleHitsOneLibraryPercent = 0.0;
    private double oneHitMultipleLibrariesPercent = 0.0;
    private double multipleHitsMultipleLibrariesPercent = 0.0;
    private double unMappedPercent = 0.0;
    private double mappedPercent = 0.0;

    private int oneHitOneLibraryCount = 0;
    private int multipleHitsOneLibraryCount = 0;
    private int oneHitMultipleLibrariesCount = 0;
    private int multipleHitsMultipleLibrariesCount = 0;

    /**
     * Called by the reduce method for each read mapped and filled intermediate
     * table
     * @param oneHit
     * @param oneGenome
     */
    void countHitPerGenome(final boolean oneHit, final boolean oneGenome) {

      if (oneHit && oneGenome) {
        oneHitOneLibraryCount++;

      } else if (!oneHit && oneGenome) {
        multipleHitsOneLibraryCount++;

      } else if (oneHit && !oneGenome) {
        oneHitMultipleLibrariesCount++;

      } else if (!oneHit && !oneGenome) {
        multipleHitsMultipleLibrariesCount++;
      }
    }

    /**
     * @param readsprocessed
     */
    void countPercentValue(int readsprocessed) {
      this.oneHitOneLibraryPercent =
          (double) this.oneHitOneLibraryCount * 100.0 / readsprocessed;
      this.multipleHitsOneLibraryPercent =
          (double) this.multipleHitsOneLibraryCount * 100.0 / readsprocessed;
      this.oneHitMultipleLibrariesPercent =
          (double) this.oneHitMultipleLibrariesCount * 100.0 / readsprocessed;
      this.multipleHitsMultipleLibrariesPercent =
          (double) this.multipleHitsMultipleLibrariesCount
              * 100.0 / readsprocessed;

      this.mappedPercent =
          (double) (this.oneHitOneLibraryCount
              + this.multipleHitsOneLibraryCount
              + this.oneHitMultipleLibrariesCount + this.multipleHitsMultipleLibrariesCount)
              * 100.0 / readsprocessed;

      this.unMappedPercent = 100.0 - mappedPercent;
    }

    /**
     * @return
     */
    String getAllPercentValues() {
      return genome
          + "\t" + roundDouble(this.unMappedPercent) + " \t"
          + roundDouble(this.oneHitOneLibraryPercent) + " \t"
          + roundDouble(this.multipleHitsOneLibraryPercent) + " \t"
          + roundDouble(this.oneHitMultipleLibrariesPercent) + " \t"
          + roundDouble(this.multipleHitsMultipleLibrariesPercent);
    }

    /**
     * @param n
     * @return
     */
    private double roundDouble(double n) {
      return DoubleMath.roundToInt((n * 100.0), RoundingMode.HALF_DOWN) / 100.0;
    }

    /**
     * @param data
     * @param prefix
     */
    public void rundata(final RunData data, final String prefix) {
      // add line in RunData
      data.put(prefix + "." + genome + "." + unMappedLegend + ".percent",
          this.unMappedPercent);
      data.put(prefix
          + "." + genome + "." + oneHitOneLibraryPercent + ".percent",
          this.oneHitOneLibraryPercent);
      data.put(prefix
          + "." + genome + "." + multipleHitsOneLibraryLegend + ".percent",
          this.multipleHitsOneLibraryPercent);
      data.put(prefix
          + "." + genome + "." + oneHitMultipleLibrariesLegend + ".percent",
          this.oneHitMultipleLibrariesPercent);
      data.put(prefix
          + "." + genome + "." + multipleHitsMultipleLibrariesLegend
          + ".percent", this.multipleHitsMultipleLibrariesPercent);

      // TODO remove print
      System.out.println("add in runData \t"
          + prefix + genome + "." + unMappedLegend + ".percent" + "="
          + this.unMappedPercent);
      System.out.println("add in runData \t"
          + prefix + genome + "." + oneHitOneLibraryLegend + ".percent" + "="
          + this.oneHitOneLibraryPercent);
      System.out.println("add in runData \t"
          + prefix + genome + "." + multipleHitsOneLibraryLegend + ".percent"
          + "=" + this.multipleHitsOneLibraryPercent);
      System.out.println("add in runData \t"
          + prefix + genome + "." + oneHitMultipleLibrariesLegend + ".percent"
          + "=" + this.oneHitMultipleLibrariesPercent);
      System.out.println("add in runData \t"
          + prefix + genome + "." + multipleHitsMultipleLibrariesLegend
          + ".percent" + "=" + this.multipleHitsMultipleLibrariesPercent);

    }

    //
    // Constructor
    //

    DataPerGenome(final String genome) {
      this.genome = genome;
    }
  }

}
