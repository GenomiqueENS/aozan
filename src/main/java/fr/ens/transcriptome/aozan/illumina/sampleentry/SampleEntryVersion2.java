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

package fr.ens.transcriptome.aozan.illumina.sampleentry;

import java.util.HashMap;
import java.util.Map;

public class SampleEntryVersion2 extends AbstractSampleEntry implements
    SampleV2 {

  private int orderNumber;
  private Map<String, String> optionalColumns = new HashMap<>();

  @Override
  public int getOrderNumber() {
    return orderNumber;
  }

  @Override
  public void setOrderNumber(int orderNumber) {
    this.orderNumber = orderNumber;
  }

  @Override
  public void setOptionalColumns(final String key, final String value) {
    this.optionalColumns.put(key, value);
  }

  @Override
  public String getDemultiplexedFilenamePrefix(final int readNumber) {

    final StringBuilder sb = new StringBuilder();

    sb.append(getSampleId().replaceAll("_", "-"));
    sb.append('_');
    sb.append("S" + getOrderNumber());
    sb.append('_');

    sb.append(getDemultiplexedFilenameSuffix(readNumber));

    return sb.toString();
  }

  @Override
  public String getNotDemultiplexedFilenamePrefix(final int readNumber) {

    final StringBuilder sb = new StringBuilder();

    sb.append("Undetermined_S0_");

    sb.append(getDemultiplexedFilenameSuffix(readNumber));

    return sb.toString();
  }

  @Override
  public String toString() {
    return "SampleEntryVersion2 [orderNumber="
        + orderNumber + ", getLane()=" + getLane() + ", getSampleId()="
        + getSampleId() + ", getSampleRef()=" + getSampleRef()
        + ", getIndex()=" + getIndex() + ", getIndex2()=" + getIndex2()
        + ", isIndex()=" + isIndex() + ", isDualIndex()=" + isDualIndex()
        + ", getDescription()=" + getDescription() + ", getSampleProject()="
        + getSampleProject() + "]";
  }

}
