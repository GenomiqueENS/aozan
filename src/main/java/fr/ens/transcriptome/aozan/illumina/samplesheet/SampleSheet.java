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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import fr.ens.transcriptome.aozan.illumina.sampleentry.Sample;

/**
 * This class handle a Casava design object.
 * @since 1.1
 * @author Laurent Jourdren
 */
public abstract class SampleSheet implements Iterable<Sample> {

  private final String sampleSheetVersion;
  private final List<Sample> samples = new ArrayList<>();

  /**
   * Adds the sample.
   * @param sample the sample
   */
  public void addSample(final Sample sample) {

    this.samples.add(sample);
  }

  /**
   * Checks if is version1.
   * @return true, if is version1
   */
  public boolean isVersion1() {

    return SampleSheetUtils.isBcl2fastqVersion1(getSampleSheetVersion());
  }

  /**
   * Checks if is version2.
   * @return true, if is version2
   */
  public boolean isVersion2() {

    return SampleSheetUtils.isBcl2fastqVersion2(getSampleSheetVersion());
  }

  /**
   * To CSV.
   * @return the string
   */
  public abstract String toCSV();

  @Override
  public Iterator<Sample> iterator() {

    return Collections.unmodifiableList(this.samples).iterator();
  }

  /**
   * Get all the samples of a lane.
   * @param laneId the lane of the samples
   * @return a list of the samples in the lane in the same order as the Casava
   *         design. Return null if the laneId < 1.
   */
  public List<Sample> getSampleInLane(final int laneId) {

    if (laneId < 1) {
      return null;
    }

    final List<Sample> result = new ArrayList<>();

    for (Sample s : this.samples) {
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

    return Sample.class.getName()
        + "{samples=" + Joiner.on("\t").join(this.samples) + "}";
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

    Preconditions.checkArgument(!Strings.isNullOrEmpty(sampleSheetVersion),
        "sample sheet version not define");

    this.sampleSheetVersion = sampleSheetVersion;
  }

}
