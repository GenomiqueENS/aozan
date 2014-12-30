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

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import fr.ens.transcriptome.aozan.AozanRuntimeException;

/**
 * This class define a registry for Collectors.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class CollectorRegistry {

  private static CollectorRegistry instance;
  private final Map<String, Collector> collectors = new HashMap<>();

  /**
   * Get a Collector.
   * @param collectorName name of the Collector.
   * @return an AozanTest or null is the test does not exists
   */
  public Collector get(final String collectorName) {

    if (collectorName == null) {
      return null;
    }

    return this.collectors.get(collectorName.trim().toLowerCase());
  }

  private void register(final Collector test) {

    if (test == null) {
      throw new NullPointerException("The Collector to register is null");
    }

    if (test.getName() == null) {
      throw new AozanRuntimeException("The name of the Collector ("
          + test.getClass().getName() + ") to register is null");
    }

    if (this.collectors.containsKey(test.getName())) {
      throw new AozanRuntimeException("The Collector ("
          + test.getName() + ") is already registred");
    }

    this.collectors.put(test.getName(), test);
  }

  //
  // Static method
  //

  /**
   * Get the singleton instance of CollectorRegistry.
   * @return the CollectorRegistry singleton
   */
  public static CollectorRegistry getInstance() {

    if (instance == null) {
      instance = new CollectorRegistry();
    }

    return instance;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private CollectorRegistry() {

    for (final Collector collector : ServiceLoader.load(Collector.class)) {

      register(collector);

    }
  }

}
