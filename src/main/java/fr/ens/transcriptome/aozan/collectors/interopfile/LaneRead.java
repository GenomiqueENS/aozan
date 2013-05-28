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

package fr.ens.transcriptome.aozan.collectors.interopfile;

/**
 * This class define an object for an unique pair lane-read in a run.
 * @author Sandrine Perrin
 * @since 1.1
 */
class LaneRead implements Comparable<LaneRead> {
  private int lane;
  private int read;

  /**
   * Public constructor
   * @param lane number lane
   * @param read number read
   */
  public LaneRead(final int lane, final int read) {
    this.lane = lane;
    this.read = read;

  }

  /**
   * Get the number lane.
   * @return number lane
   */
  public int getLane() {
    return lane;
  }

  /**
   * Get the number read.
   * @return number read
   */
  public int getRead() {
    return read;
  }

  @Override
  public String toString() {
    return lane + "-" + read;
  }

  @Override
  public boolean equals(Object laneRead) {
    if (this == laneRead)
      return true;

    LaneRead lr = (LaneRead) laneRead;
    return this.lane == lr.getLane() && this.read == lr.getRead();
  }

  @Override
  public int hashCode() {
    return lane * 10 + read;
  }

  @Override
  public int compareTo(LaneRead laneRead) {
    return this.hashCode() - laneRead.hashCode();
  }

}