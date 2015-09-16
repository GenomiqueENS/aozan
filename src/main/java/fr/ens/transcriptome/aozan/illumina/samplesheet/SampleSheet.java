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

package fr.ens.transcriptome.aozan.illumina.samplesheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntry;

/**
 * This class handle a Casava design object.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class SampleSheet implements Iterable<SampleEntry> {

  private final String sampleSheetVersion;
  private final List<SampleEntry> samples = new ArrayList<>();

  public void addSample(final SampleEntry sample) {

    this.samples.add(sample);
  }

  public boolean isVersion1() {

    return SampleSheetUtils.isBcl2fastqVersion1(getSampleSheetVersion());
  }

  public boolean isVersion2() {

    return SampleSheetUtils.isBcl2fastqVersion2(getSampleSheetVersion());
  }

  @Override
  public Iterator<SampleEntry> iterator() {

    return Collections.unmodifiableList(this.samples).iterator();
  }

  /**
   * Get all the samples of a lane.
   * @param laneId the lane of the samples
   * @return a list of the samples in the lane in the same order as the Casava
   *         design. Return null if the laneId < 1.
   */
  public List<SampleEntry> getSampleInLane(final int laneId) {

    if (laneId < 1) {
      return null;
    }

    final List<SampleEntry> result = new ArrayList<>();

    for (SampleEntry s : this.samples) {
      if (s.getLane() == laneId) {
        result.add(s);
      }
    }

    return result;
  }

  /**
   * Get the number of samples in the design.
   * @return the number of samples in the design
   */
  public int size() {

    return this.samples.size();
  }

  @Override
  public String toString() {

    return SampleEntry.class.getName() + "{samples=" + this.samples + "}";
  }

  //
  // Getter
  //

  /**
   * Gets the sample sheet version.
   * @return the sampleSheetVersion
   */
  public String getSampleSheetVersion() {
    return sampleSheetVersion;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param sampleSheetVersion the sample sheet version
   */
  public SampleSheet(final String sampleSheetVersion) {
    this.sampleSheetVersion = sampleSheetVersion;
  }

}
