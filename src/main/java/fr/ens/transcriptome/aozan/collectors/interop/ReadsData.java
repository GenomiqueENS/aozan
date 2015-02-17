/*
 *                 Aozan development code
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

package fr.ens.transcriptome.aozan.collectors.interop;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;

/**
 * The class define all reads setting on run sequencing informations, initial
 * parameters are saving.
 * @author Sandrine Perrin
 * @since 1.4
 */
final class ReadsData {

  /** The run data. */
  private final RunData runData;

  /** The reads count. */
  private final int readsCount;

  /** The reads data on cycles. */
  private final Map<Integer, ReadData> readsDataOnCycles;

  /** The cycles count. */
  private int cyclesCount = -1;

  /**
   * Define the readNumber corresponding to the cycle.
   * @param cycle number cycle in the run
   * @return readNumber
   */
  public int getReadFromCycleNumber(final int cycleNumber)
      throws AozanException {

    if (cycleNumber < 1 && cycleNumber > getCyclesCount()) {
      throw new AozanException("Cycle number invalid "
          + cycleNumber + ", must be < " + getCyclesCount()
          + ". Can not extract read number.");
    }

    // Parse ReadData
    for (ReadData readData : readsDataOnCycles.values()) {

      // Check cycle number included between first and last cycle number
      if (readData.isCycleIncluded(cycleNumber))

        // Return read number
        return readData.getReadNumber();

    }

    // No read number found
    throw new AozanException("Cycle number invalid "
        + cycleNumber + ". No read found.");
  }

  /**
   * Collect
   * @return the map
   */
  private Map<Integer, ReadData> collect() {

    final Map<Integer, ReadData> reads = new HashMap<>(readsCount);

    int counterCycles = 1;

    // Parse reads set in runInfo file.
    for (int read = 1; read <= this.readsCount; read++) {

      final int numberCycles = this.runData.getReadCyclesCount(read);
      final boolean isIndexed = this.runData.isReadIndexed(read);

      // Extract first cycle number for current read
      final int firstCycle = counterCycles;

      // Extract last cycle number for current read
      final int lastCycle = firstCycle + numberCycles - 1;

      // Save new read
      reads.put(read, new ReadData(read, numberCycles, firstCycle, lastCycle,
          isIndexed));

      // Compile count cycles
      counterCycles = lastCycle + 1;
    }

    // Set cycles count final
    this.cyclesCount = counterCycles--;

    return Collections.unmodifiableMap(reads);
  }

  //
  // Getter
  //

  /**
   * Gets the cycles count.
   * @return the cycles count
   */
  int getCyclesCount() {
    return this.cyclesCount;
  }

  /**
   * Gets the reads data on cycles.
   * @return the reads data on cycles
   */
  public Map<Integer, ReadData> getReadsDataOnCycles() {
    return readsDataOnCycles;
  }

  /**
   * Gets the read data related to the read number.
   * @param read the read number
   * @return the read data
   */
  public ReadData getReadData(final int read) {

    checkArgument((read > 0 && read <= this.readsCount), "Read number invalid "
        + read + ". Must be between 1 and " + this.readsCount + ".");

    // Get readData related to the read number
    return readsDataOnCycles.get(read);
  }

  //
  // Constructor
  //

  /**
   * Constructor
   * @param runData the run data
   */
  ReadsData(final RunData runData) {

    checkNotNull(runData, "For extract reads data, runData is null");

    this.runData = runData;
    this.readsCount = runData.getReadCount();

    // Build map with all reads
    this.readsDataOnCycles = collect();
  }

  //
  // Internal class
  //

  /**
   * The class define one read setting on run sequencing informations, initial
   * parameters are saving.
   */
  static final class ReadData {

    /** The read number. */
    private final int readNumber;

    /** The number cycles. */
    private final int numberCycles;

    /** The first cycle number. */
    private final int firstCycleNumber;

    /** The last cycle number. */
    private final int lastCycleNumber;

    /** The indexed read. */
    private final boolean indexedRead;

    /**
     * Gets the read number.
     * @return the read number
     */
    public int getReadNumber() {
      return readNumber;
    }

    /**
     * Check cycle number included between first and last cycle number
     * @param cycleNumber the cycle number
     * @return true, if is cycle included
     */
    public boolean isCycleIncluded(int cycleNumber) {

      return (cycleNumber >= firstCycleNumber && cycleNumber <= lastCycleNumber);
    }

    /**
     * Gets the number cycles.
     * @return the number cycles
     */
    public int getNumberCycles() {
      return numberCycles;
    }

    /**
     * Gets the first cycle number.
     * @return the first cycle number
     */
    public int getFirstCycleNumber() {
      return firstCycleNumber;
    }

    /**
     * Gets the last cycle number.
     * @return the last cycle number
     */
    public int getLastCycleNumber() {
      return lastCycleNumber;
    }

    /**
     * Checks if is indexed read.
     * @return true, if is indexed read
     */
    public boolean isIndexedRead() {
      return indexedRead;
    }

    //
    // Constructor
    //

    /**
     * Instantiates a new read data.
     * @param read the read
     * @param numberCycles the number cycles
     * @param firstCycle the first cycle
     * @param lastCycle the last cycle
     * @param isIndexed the is indexed
     */
    public ReadData(final int read, final int numberCycles,
        final int firstCycle, final int lastCycle, final boolean isIndexed) {

      this.readNumber = read;
      this.numberCycles = numberCycles;
      this.firstCycleNumber = firstCycle;
      this.lastCycleNumber = lastCycle;
      this.indexedRead = isIndexed;

    }
  }

}