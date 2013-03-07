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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;

/**
 * This class manages results from fastqscreen
 * @author Sandrine Perrin
 */
public class FastqScreenResult {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String HIT_NO_LIBRAIRIES_LEGEND = "hitnolibraries";
  private static final String HIT_LEGEND = "hit";
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
  private double percentHit = 0.0;
  private boolean countPercentOk = false;

  /**
   * Print table percent in format use by fastqscreen program with rounding
   * value
   * @return string with results from fastqscreen
   */
  public String statisticalTableToString() {

    StringBuilder s =
        new StringBuilder().append("\n" + HEADER_COLUMNS_TEXT + "\n");

    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
      s.append(e.getValue().getAllPercentValues() + "\n");
    }

    // add last line for percentage of reads unmapped
    double percentHitNoLibrariesRounding =
        DataPerGenome.roundDouble(this.percentHitNoLibraries);
    s.append("\n% Hit_no_libraries : " + percentHitNoLibrariesRounding + "\n");

    return s.toString();
  }

  /**
   * Save result from fastqscreen in file.
   * @param dirPath directory who save file of result from fastqscreen
   */
  public void createFileResultFastqScreen_OLD(final String pathDir) {

    String result = pathDir + "/fastqscreen.txt";
    try {
      FileWriter fr = new FileWriter(result);
      BufferedWriter br = new BufferedWriter(fr);

      br.append(statisticalTableToString());

      br.close();
      fr.close();
    } catch (IOException io) {
      io.printStackTrace();
    }

    System.out.println("Save result fastqscreen in file : " + result);

    LOGGER.fine("Save result fastqscreen in file : " + result);

  }

  /**
   * Update the list of reference genome used by fastqscreen.
   * @param genome name of new reference genome
   */
  public void addGenome(final String genome) {

    if (!this.resultsPerGenome.containsKey(genome))
      this.resultsPerGenome.put(genome, new DataPerGenome(genome));
  }

  /**
   * Count for each read number of hit per reference genome
   * @param genome genome name
   * @param oneHit true if read mapped one time on genome else false
   * @param oneGenome true if read mapped on several genome else false
   */
  public void countHitPerGenome(final String genome, final boolean oneHit,
      final boolean oneGenome) {
    this.resultsPerGenome.get(genome).countHitPerGenome(oneHit, oneGenome);
  }

  /**
   * Convert values from fastqscreen in percentage.
   * @param readsMapped number reads mapped
   * @param readsprocessed number reads total
   * @throws AozanException if no value
   */
  public void countPercentValue(final int readsMapped, final int readsprocessed)
      throws AozanException {

    if (this.resultsPerGenome.isEmpty())
      throw new AozanException(
          "During fastqScreen execusion : no genome receive");

    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
      e.getValue().countPercentValue(readsprocessed);
    }

    this.percentHitNoLibraries =
        ((double) (readsprocessed - readsMapped)) / readsprocessed;
    this.percentHit = 1.0 - this.percentHitNoLibraries;

    countPercentOk = true;
  }

  /**
   * Update rundata with result from fastqscreen
   * @param data rundata
   * @param prefix name of sample
   * @throws AozanException if no value.
   */
  public RunData createRundata(final String prefix) throws AozanException {

    if (this.resultsPerGenome.isEmpty())
      throw new AozanException(
          "During fastqScreen execusion : no genome receive");

    if (!countPercentOk)
      throw new AozanException(
          "During fastqScreen execusion : no value ​for each genome");

    RunData data = new RunData();

    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
      e.getValue().updateRundata(data, prefix);
    }

    // print last line of report FastqScreen
    data.put(prefix + "." + HIT_NO_LIBRAIRIES_LEGEND,
        this.percentHitNoLibraries);
    data.put(prefix + "." + HIT_LEGEND, this.percentHit);

    return data;
  }

  //
  // CONSTRUCTOR
  //

  /**
   * Public constructor of FastqScreenResult
   */
  public FastqScreenResult() {
    this.resultsPerGenome = new HashMap<String, DataPerGenome>();
  }

  //
  // Internal class
  //

  /**
   * This internal class saves values of fastqscreen from one reference genome.
   * @author Sandrine Perrin
   */
  public static class DataPerGenome {

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
     * Count for each read number of hit per reference genome
     * @param genome genome name
     * @param oneHit true if read mapped one time on genome else false
     * @param oneGenome true if read mapped on several genome else false
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
     * Convert values from fastqscreen in percentage.
     * @param readsprocessed number reads total
     */
    void countPercentValue(int readsprocessed) {
      this.oneHitOneLibraryPercent =
          (double) this.oneHitOneLibraryCount / readsprocessed;
      this.multipleHitsOneLibraryPercent =
          (double) this.multipleHitsOneLibraryCount / readsprocessed;
      this.oneHitMultipleLibrariesPercent =
          (double) this.oneHitMultipleLibrariesCount / readsprocessed;
      this.multipleHitsMultipleLibrariesPercent =
          (double) this.multipleHitsMultipleLibrariesCount / readsprocessed;

      this.mappedPercent =
          (double) (this.oneHitOneLibraryCount
              + this.multipleHitsOneLibraryCount
              + this.oneHitMultipleLibrariesCount + this.multipleHitsMultipleLibrariesCount)
              / readsprocessed;

      this.unMappedPercent = 1.0 - mappedPercent;

    }

    /**
     * Get string with all values rounded.
     * @return string
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
     * Rounding a double
     * @param n double
     * @return double value rounded
     */
    private static double roundDouble(double n) {
      return Math.rint(n * 10000.0) / 100.0;
    }

    /**
     * Update rundata with result from fastqscreen
     * @param data rundata
     * @param prefix name of sample
     */
    public void updateRundata(final RunData data, final String prefix) {
      // add line in RunData
      data.put(prefix + "." + genome + "." + mappedLegend + ".percent",
          this.mappedPercent);
      data.put(prefix + "." + genome + "." + unMappedLegend + ".percent",
          this.unMappedPercent);
      data.put(prefix
          + "." + genome + "." + oneHitOneLibraryLegend + ".percent",
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

    }

    //
    // Constructor
    //

    /**
     * Constructor for DataPerGenome
     * @param genome genome name
     */
    DataPerGenome(final String genome) {
      this.genome = genome;
    }
  }

}
