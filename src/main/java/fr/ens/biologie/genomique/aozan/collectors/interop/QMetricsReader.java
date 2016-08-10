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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors.interop;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import fr.ens.biologie.genomique.aozan.AozanException;

/**
 * This class define a specified iterator for reading the binary file :
 * QMetricsOut.bin.
 * @author Cyril Firmo
 * @since 2.0
 */
public class QMetricsReader extends QualityMetricsReader {

  public static final String NAME = "QualityMetricsOut";

  public static final String QUALITY_METRICS_FILE = "QMetricsOut.bin";

  private static final int EXPECTED_RECORD_SIZE = 206;
  private static final int EXPECTED_VERSION = 5;

  private int qualityScoreBinningEnabled = -1;
  private int qualityScoreBinNumber = -1;

  private int[] lowerBoundary;
  private int[] upperBoundary;
  private int[] remappedScoreQuality;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  protected void readOptionalFlag(ByteBuffer bb) {
    this.qualityScoreBinningEnabled = uByteToInt(bb.get());
    if (this.qualityScoreBinningEnabled == 1) {
      this.qualityScoreBinNumber = uByteToInt(bb.get());
      this.lowerBoundary = new int[this.qualityScoreBinNumber];
      this.upperBoundary = new int[this.qualityScoreBinNumber];
      this.remappedScoreQuality = new int[this.qualityScoreBinNumber];

      for (int i = 0; i < qualityScoreBinNumber; i++) {
        this.lowerBoundary[i] = uByteToInt(bb.get());
      }

      for (int i = 0; i < qualityScoreBinNumber; i++) {
        this.upperBoundary[i] = uByteToInt(bb.get());
      }

      for (int i = 0; i < qualityScoreBinNumber; i++) {
        this.remappedScoreQuality[i] = uByteToInt(bb.get());
      }

    }

  }

  @Override
  protected File getMetricsFile() {
    return new File(getDirPathInterOP(), QUALITY_METRICS_FILE);
  }

  @Override
  protected int getExpectedRecordSize() {
    return EXPECTED_RECORD_SIZE;
  }

  @Override
  protected int getExpectedVersion() {
    return EXPECTED_VERSION;
  }

  @Override
  protected void addIlluminaMetricsInCollection(
      final List<QualityMetrics> collection, final ByteBuffer bb) {

    collection.add(new QualityMetrics(bb));
  }

  /**
   * Get an integer telling if Qscore binning is enabled
   * @return if Qscore binning is enabled.
   */
  public int getQualityScoreBinningEnabled() {
    return qualityScoreBinningEnabled;
  }

  /**
   * Get the number of bin
   * @return int number of bin
   */
  public int getQualityScoreBinNumber() {
    return qualityScoreBinNumber;
  }

  /**
   * Get an array of int with the lower boundaries of each bin
   * @return int[] the lower boundaries of each bin
   */
  public int[] getLowerBoundary() {
    return lowerBoundary;
  }

  /**
   * Get an array of int with the upper boundaries of each bin
   * @return int[] the upper boundaries of each bin
   */
  public int[] getUpperBoundary() {
    return upperBoundary;
  }

  /**
   * Get an array of int with the value on which quality score of a bin is
   * remapped
   * @return int[] remapped score quality
   */
  public int[] getRemappedScoreQuality() {
    return remappedScoreQuality;
  }

  /**
   * Set an integer telling if Qscore binning is enabled
   * @param value integer if Qscore binning is enabled.
   */
  public void setQualityScoreBinningEnabled(int value) {
    this.qualityScoreBinningEnabled = value;
  }

  /**
   * Set the number of bin
   * @param value int number of bin
   */
  public void setQualityScoreBinNumber(int value) {
    this.qualityScoreBinNumber = value;
  }

  /**
   * Set an array of int with the lower boundaries of each bin
   * @param value int[] the lower boundaries of each bin
   */
  public void setLowerBoundary(int[] value) {
    this.lowerBoundary = value;
  }

  /**
   * Set an array of int with the upper boundaries of each bin
   * @param value int[] the upper boundaries of each bin
   */
  public void setUpperBoundary(int[] value) {
    this.upperBoundary = value;
  }

  /**
   * Set an array of int with the value on which quality score of a bin is
   * remapped
   * @param value int[] remapped score quality
   */
  public void setRemappedScoreQuality(int[] value) {
    this.remappedScoreQuality = value;
  }
  //
  // Constructor
  //

  QMetricsReader(final File dirPath) throws AozanException {

    super(dirPath);

  }

}
