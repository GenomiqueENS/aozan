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
 * TileMetricsOut.bin.
 * @author Sandrine Perrin
 * @since 1.1
 */
public class TileMetricsReader extends
    AbstractBinaryFileReader<IlluminaTileMetrics> {

  public static final String NAME = "TileMetricsOut";

  public static final String TILE_METRICS_FILE = "TileMetricsOut.bin";

  private static final int EXPECTED_RECORD_SIZE = 10;
  private static final int EXPECTED_VERSION = 2;

  @Override
  protected File getMetricsFile() {
    return new File(getDirPathInterOP(), TILE_METRICS_FILE);
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
  public String getName() {
    return NAME;
  }

  @Override
  protected void addIlluminaMetricsInCollection(
      final List<IlluminaTileMetrics> collection, final ByteBuffer bb) {

    collection.add(new IlluminaTileMetrics(bb));
  }

  //
  // Constructor
  //

  TileMetricsReader(final File dirPath) throws AozanException {
    super(dirPath);
  }

}
