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

import java.util.List;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;

/**
 * This interface define a Collector.
 * @since 0.8
 * @author Laurent Jourdren
 */
public interface Collector {

  /**
   * Get the name of the collector.
   * @return the name of the collector
   */
  String getName();

  /**
   * Get the name of the collectors required to run this collector.
   * @return a list of String with the name of the required collectors
   */
  List<String> getCollectorsNamesRequiered();

  /**
   * Configure the collector with the path of the run data.
   * @param qc QC object for the run
   * @param conf the collector configuration
   */
  void configure(QC qc, CollectorConfiguration conf);

  /**
   * Collect data.
   * @param data result data object
   * @throws AozanException if an error occurs while collecting data
   */
  void collect(RunData data) throws AozanException;

  /**
   * Remove temporary files.
   */
  void clear();

  /**
   * Checks if is statistic collector, if it's true, should be executed at the
   * end.
   * @return true, if is statistic collector
   */
  boolean isStatisticCollector();
}
