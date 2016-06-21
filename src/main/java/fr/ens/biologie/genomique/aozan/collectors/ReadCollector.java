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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.ImmutableList;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.collectors.interop.ErrorMetricsCollector;
import fr.ens.biologie.genomique.aozan.collectors.interop.ExtractionMetricsCollector;
import fr.ens.biologie.genomique.aozan.collectors.interop.TileMetricsCollector;

/**
 * This class define a Collector for read?.xml files.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class ReadCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "read";

  public static final String READ_DATA_PREFIX = "reads";

  private final List<Collector> subCollectionList = new ArrayList<>();

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public boolean isStatisticCollector() {
    return false;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(RunInfoCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final QC qc, final Properties properties) {

    if (properties == null) {
      return;
    }

    // Use ReadXMLCollector, if specified in aozan.conf
    final String readXMLCollectorUsed =
        properties.getProperty(Settings.QC_CONF_READ_XML_COLLECTOR_USED_KEY)
            .trim().toLowerCase();

    // Build the list of sub-collector
    if (readXMLCollectorUsed.equals("true")) {

      // file read.xml exists
      this.subCollectionList.add(new ReadXMLCollector());

    } else {
      // Reading interOpFile, per default
      this.subCollectionList.add(new TileMetricsCollector());
      this.subCollectionList.add(new ExtractionMetricsCollector());
      this.subCollectionList.add(new ErrorMetricsCollector());
    }

    // Configure sub-collector
    for (final Collector collector : this.subCollectionList) {
      collector.configure(qc, properties);
    }
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    // Collect sub-collector
    for (final Collector collector : this.subCollectionList) {
      collector.collect(data);
    }

  }

  @Override
  public void clear() {

    for (final Collector collector : this.subCollectionList) {
      collector.clear();
    }
  }

}
