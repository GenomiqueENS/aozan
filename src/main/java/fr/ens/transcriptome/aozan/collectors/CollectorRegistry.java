/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.aozan.collectors;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanRuntimeException;

/**
 * This class define a registry for Collectors.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class CollectorRegistry {

  private static CollectorRegistry instance;
  private Map<String, Collector> tests = Maps.newHashMap();

  private void register(final Collector test) {

    if (test == null)
      throw new NullPointerException("The AozanTest to register is null");

    if (test.getName() == null)
      throw new AozanRuntimeException("The name of the AozanTest ("
          + test.getClass().getName() + ") to register is null");

    if (this.tests.containsKey(test.getName()))
      throw new AozanRuntimeException("The AozanTest ("
          + test.getName() + ") is already registred");

    this.tests.put(test.getName(), test);
  }

  //
  // Static method
  //

  /**
   * Get the singleton instance of DataFormatRegistry
   * @return the DataFormatRegistry singleton
   */
  public static CollectorRegistry getInstance() {

    if (instance == null)
      instance = new CollectorRegistry();

    return instance;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private CollectorRegistry() {

    final Iterator<Collector> it =
        ServiceLoader.load(Collector.class).iterator();

    while (it.hasNext()) {

      register(it.next());

    }
  }

}
