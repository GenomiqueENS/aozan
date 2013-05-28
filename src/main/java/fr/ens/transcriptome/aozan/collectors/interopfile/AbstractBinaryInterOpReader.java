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

package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;

/**
 * This class define the method necessary for all reader of binary file in
 * InterOp directory.
 * @author Sandrine Perrin
 * @since 1.1
 */
abstract class AbstractBinaryInterOpReader {

  protected int lanes;
  protected int reads;
  protected int tiles;

  /**
   * Define data necessary for all concrete InterOpReader
   * @param data result data object
   */
  public void collect(final RunData data) throws AozanException {

    
    this.lanes = data.getInt("run.info.flow.cell.lane.count");
    this.reads = data.getInt("run.info.read.count");

    if (reads > 3)
      throw new AozanException(
          "Numbers of reads > 3 not accept for reading binary file in InterOp Directory.");

    this.tiles =
        data.getInt("run.info.flow.cell.tile.count")
            * data.getInt("run.info.flow.cell.surface.count")
            * data.getInt("run.info.flow.cell.swath.count");

    // Set global data not specific for one lane
    for (int read = 1; read <= reads; read++) {
      // TODO to define
      data.put("read" + read + ".density.ratio", "0.3472222");
      String s =
          data.getBoolean("run.info.read" + read + ".indexed") ? "(Index)" : "";
      data.put("read" + read + ".type", s);
    }
  }

  /**
   * Build a collection with all records from binary file
   * @param i iterator on element of collection
   * @return collection of records
   */
  public static <T> Collection<T> makeCollection(final Iterator<T> i) {
    final Collection<T> c = new LinkedList<T>();
    while (i.hasNext()) {
      c.add(i.next());
    }
    return c;
  }

}
