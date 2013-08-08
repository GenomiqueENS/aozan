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

import java.io.File;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
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
  private static final String READ_XML_COLLECTOR_SPECIFIED = "";

  private String RTAOutputDirPath;
  private Properties properties;

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

    this.properties = properties;
    RTAOutputDirPath = properties.getProperty(QC.RTA_OUTPUT_DIR);
    String readXMLCollectorUsed =
        properties.getProperty("readXMLCollector.used").trim().toLowerCase();

    // Build the list of subcollector
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

  // <<<<<<< HEAD
  // =======
  // /**
  // * This class define a Collector for several binary files from InterOp
  // * directory.
  // * @since 1.1
  // * @author Sandrine Perrin
  // */
  // class ReadCollectorBinaryFile extends ReadCollector {
  //
  // private void collectRead(final RunData data, final String readInfoFilePath)
  // throws AozanException {
  //
  // AbstractBinaryIteratorReader.setDirectory(readInfoFilePath);
  //
  // // Collect metrics on number cluster
  // new TileMetricsOutReader().collect(data);
  // // Collect metrics on error rate
  // new ErrorMetricsOutReader().collect(data);
  // // Collect metrics on intensity rate
  // new ExtractionMetricsOutReader().collect(data);
  // }
  //
  // /**
  // * Public constructor
  // * @param data
  // */
  // ReadCollectorBinaryFile(final RunData data) throws AozanException {
  //
  // String readInfoFilePath = RTAOutputDirPath + "/InterOp/";
  // try {
  //
  // FileUtils.checkExistingDirectoryFile(new File(readInfoFilePath),
  // "Directory interOp not find here !" + readInfoFilePath);
  // collectRead(data, readInfoFilePath);
  //
  // } catch (IOException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // }
  //
  // }
  // }
  // >>>>>>> refs/heads/fastqscreen_light
}
