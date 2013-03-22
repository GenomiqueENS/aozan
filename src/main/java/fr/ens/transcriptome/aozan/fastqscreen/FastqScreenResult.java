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

  // Legend for global value in fastqscreen
  private static final String PERCENT_MAPPED_NONE_GENOME =
      "percentMappedNoneGenome";
  private static final String PERCENT_MAPPED_AT_LEAST_ONE_GENOME =
      "percentMappedAtLeastOneGenome";
  private static final String PERCENT_MAPPED_EXCEPT_GENOME_SAMPLE =
      "mappedexceptgenomesample";

  private static final String HEADER_COLUMNS_TEXT =
      "Library \t %Mapped \t %Unmapped \t %One_hit_one_library"
          + "\t %Multiple_hits_one_library \t %One_hit_multiple_libraries \t "
          + "%Multiple_hits_multiple_libraries";

  private Map<String, DataPerGenome> resultsPerGenome;
  private double percentUnmappedNoneGenome = 0.0;
  private double percentMappedAtLeastOneGenome = 0.0;
  private double percentMappedExceptGenomeSample = 0.0;
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

    // add last lines for percentage of reads
    s.append("\n% reads_mapped_none_genome : "
        + DataPerGenome.roundDouble(this.percentUnmappedNoneGenome) + "\n");
    s.append("% reads_mapped_at_least_one_genome : "
        + DataPerGenome.roundDouble(this.percentMappedAtLeastOneGenome) + "\n");
    s.append("% mapped_except_genome_sample : "
        + DataPerGenome.roundDouble(this.percentMappedExceptGenomeSample)
        + "\n");

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
   * @param genomeSample genome reference corresponding to sample
   */
  public void addGenome(final String genome, final String genomeSample) {

    if (genome == null)
      return;

    if (!this.resultsPerGenome.containsKey(genome))
      this.resultsPerGenome
          .put(genome, new DataPerGenome(genome, genomeSample));
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

    double percentMappedOnlyOnGenomeSample = 0.0;

    for (Map.Entry<String, DataPerGenome> e : this.resultsPerGenome.entrySet()) {
      e.getValue().countPercentValue(readsprocessed);

      percentMappedOnlyOnGenomeSample +=
          e.getValue().getPercentMappedOnlyOnGenomeSample(readsprocessed);
    }

    this.percentUnmappedNoneGenome =
        ((double) (readsprocessed - readsMapped)) / readsprocessed;
    this.percentMappedAtLeastOneGenome = 1.0 - this.percentUnmappedNoneGenome;

    this.percentMappedExceptGenomeSample =
        percentMappedAtLeastOneGenome - percentMappedOnlyOnGenomeSample;

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
    data.put(prefix + "." + PERCENT_MAPPED_NONE_GENOME,
        this.percentUnmappedNoneGenome);
    data.put(prefix + "." + PERCENT_MAPPED_AT_LEAST_ONE_GENOME,
        this.percentMappedAtLeastOneGenome);

    //
    data.put(prefix + "." + PERCENT_MAPPED_EXCEPT_GENOME_SAMPLE,
        this.percentMappedExceptGenomeSample);

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
    private boolean isGenomeSample;

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
          + "\t" + roundDouble(this.mappedPercent) + " \t"
          + roundDouble(this.unMappedPercent) + " \t"
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

    /**
     * Retrieve the percent of reads which mapped only on genome sample, zero if
     * genome is not the genome sample
     * @param readsprocessed number reads total
     * @return percent
     */
    double getPercentMappedOnlyOnGenomeSample(int readsprocessed) {
      double readsMappedOnlyOnGenomeSample = 0;

      if (isGenomeSample) {

        readsMappedOnlyOnGenomeSample =
            oneHitOneLibraryPercent + multipleHitsOneLibraryPercent;
        
        return readsMappedOnlyOnGenomeSample / readsprocessed;

      }

      return 0.0;
    }

    //
    // Constructor
    //

    /**
     * Constructor for DataPerGenome
     * @param genome genome name
     * @param genomeSample genome reference corresponding to sample
     */
    DataPerGenome(final String genome, final String genomeSample) {
      this.genome = genome;
      this.isGenomeSample = genome.equals(genomeSample);
    }
  }

}
