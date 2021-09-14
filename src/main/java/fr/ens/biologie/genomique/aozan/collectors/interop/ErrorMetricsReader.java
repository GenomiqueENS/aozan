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
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.aozan.AozanException;

/**
 * This class define a specified iterator for reading the binary file :
 * ErrorMetricsOut.bin.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class ErrorMetricsReader extends AbstractBinaryFileReader<ErrorMetrics> {

  public static final String NAME = "ErrorMetricsOut";

  public static final String ERROR_METRICS_FILE = "ErrorMetricsOut.bin";

  /**
   * Get the file name treated.
   * @return file name
   */
  @Override
  public String getName() {
    return NAME;
  }

  @Override
  protected File getMetricsFile() {
    return new File(getDirPathInterOP(), ERROR_METRICS_FILE);
  }

  @Override
  protected int getExpectedRecordSize(int version) {

    switch (version) {
    case 3:
      return 30;

    case 4:
      return 12;

    case 5:
      return 16;

    default:
      throw new IllegalArgumentException();
    }

  }

  @Override
  protected Set<Integer> getExpectedVersions() {
    return new HashSet<Integer>(Arrays.asList(3, 4, 5));
  }

  @Override
  protected void readMetricRecord(final List<ErrorMetrics> collection,
      final ByteBuffer bb, final int version) {

    collection.add(new ErrorMetrics(version, bb));
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @throws AozanException it occurs if size record or version aren't the same
   *           that expected.
   */

  ErrorMetricsReader(final File dirPath)
      throws FileNotFoundException, AozanException {

    super(dirPath);

    if (!new File(getDirPathInterOP(), ERROR_METRICS_FILE).exists()) {
      throw new FileNotFoundException();
    }
  }

}
