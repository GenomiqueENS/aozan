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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.collectors.interop.ErrorMetricsCollector;
import fr.ens.transcriptome.aozan.collectors.interop.ExtractionMetricsCollector;
import fr.ens.transcriptome.aozan.collectors.interop.TileMetricsCollector;

/**
 * This class define a Collector for read?.xml files.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class ReadCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "read";

  private final List<Collector> subCollectionList = Lists.newArrayList();

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(RunInfoCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final Properties properties) {

    if (properties == null)
      return;

    // Use ReadXMLCollector, if specified in aozan.conf
    String readXMLCollectorUsed =
        properties.getProperty(Settings.QC_CONF_READ_XML_COLLECTOR_USED_KEY)
            .trim().toLowerCase();

    // Build the list of sub-collector
    if (readXMLCollectorUsed.equals("true")) {

      // file read.xml exists
      subCollectionList.add(new ReadXMLCollector());

    } else {
      // Reading interOpFile, per default
      subCollectionList.add(new TileMetricsCollector());
      subCollectionList.add(new ExtractionMetricsCollector());
      subCollectionList.add(new ErrorMetricsCollector());
    }

    // Configure sub-collector
    for (Collector collector : subCollectionList)
      collector.configure(properties);
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    // Collect sub-collector
    for (Collector collector : subCollectionList)
      collector.collect(data);

  }

  @Override
  public void clear() {

    for (Collector collector : subCollectionList)
      collector.clear();
  }

}
