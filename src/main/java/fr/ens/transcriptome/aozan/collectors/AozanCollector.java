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
package fr.ens.transcriptome.aozan.collectors;

import java.util.List;
import java.util.Properties;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.RunData;

public class AozanCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "aozaninfo";

  /** Prefix for run data */
  public static final String PREFIX = "aozan.info";

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return null;
  }

  @Override
  public void configure(final Properties properties) {
    // Nothing to do
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    // Add main data on sequencing
    data.put(PREFIX + ".version", Globals.APP_VERSION_STRING);
    data.put(PREFIX + ".commit", Globals.APP_BUILD_COMMIT);
    

    // TODO add sequencer type
    // TODO add rta version
    // TODO add bcl2fastq version if demux.step enable

  }

  @Override
  public void clear() {

  }

}
