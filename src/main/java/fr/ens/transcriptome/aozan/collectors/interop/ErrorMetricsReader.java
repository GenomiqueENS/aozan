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

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.List;

import fr.ens.transcriptome.aozan.AozanException;

/**
 * This class define a specified iterator for reading the binary file :
 * ErrorMetricsOut.bin.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ErrorMetricsReader extends
    AbstractBinaryFileReader<IlluminaErrorMetrics> {

  public static final String NAME = "ErrorMetricsOut";

  public static final String ERROR_METRICS_FILE = "ErrorMetricsOut.bin";
  private static final int EXPECTED_RECORD_SIZE = 30;
  private static final int EXPECTED_VERSION = 3;

  /**
   * Get the file name treated
   * @return file name
   */
  @Override
  public String getName() {
    return NAME;
  }

  @Override
  protected File getMetricsFile() {
    return new File(dirInterOpPath + ERROR_METRICS_FILE);
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
      final List<IlluminaErrorMetrics> collection, final ByteBuffer bb) {

    collection.add(new IlluminaErrorMetrics(bb));
  }

  //
  // Constructor
  //

  /**
   * Constructor
   * @throws AozanException it occurs if size record or version aren't the same
   *           that expected.
   */

  ErrorMetricsReader(final String dirPath) throws FileNotFoundException,
      AozanException {
    super(dirPath);

    if (!new File(dirInterOpPath + ERROR_METRICS_FILE).exists())
      throw new FileNotFoundException();
  }

}
