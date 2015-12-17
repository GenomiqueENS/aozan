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
 *      http://tools.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.fastqscreen;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.io.Files;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.Settings;

/**
 * This class define a storage for genome name aliases.
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
public class GenomeAliases {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private static GenomeAliases singleton;

  // Associated genome name from design file with valid genome call for mapping
  private final Map<String, String> genomesAliases = new HashMap<>();

  /**
   * Return the reference genome corresponding to the genome sample if it is
   * present in alias genomes file.
   * @param genomeName name of genome sample
   * @return reference genome corresponding to genome if it exists or empty
   *         string or null if no genome exist.
   */
  public String getGenomeNameFromAlias(final String genomeName) {

    checkNotNull(genomeName, "genome argument cannot be null");

    final String genomeTrimmed =
        genomeName.replaceAll("\"", "").trim().toLowerCase();

    return this.genomesAliases.get(genomeTrimmed);

  }

  /**
   * Create a map which does correspondence between genome of sample and
   * reference genome from a file, the path is in aozan configuration.
   * @param map to correspondence between common genome name and valid call name
   *          for mapping
   * @throws AozanException if alias genomes file doesn't exist.
   */
  private static Map<String, String> loadAliasGenomesFile(
      final String aliasFilePath) throws AozanException {

    if (aliasFilePath == null || aliasFilePath.trim().length() == 0) {
      LOGGER.fine("FastqScreen no alias genome file parameter define.");
      return Collections.emptyMap();
    }

    final File aliasGenomesFile = new File(aliasFilePath.trim());

    final Map<String, String> genomes = new HashMap<>();

    // Not found alias genomes file
    if (!aliasGenomesFile.exists()) {
      throw new AozanException("FastqScreen alias genome file doesn't exists "
          + aliasGenomesFile.getAbsolutePath());
    }

    try {
      // Read alias genomes files
      final BufferedReader br =
          Files.newReader(aliasGenomesFile, Globals.DEFAULT_FILE_ENCODING);

      String line = null;

      while ((line = br.readLine()) != null) {

        final int pos = line.indexOf('=');
        if (pos == -1) {
          continue;
        }

        final String key = line.substring(0, pos);
        final String value = line.substring(pos + 1);

        // Retrieve genomes identified in Casava design file
        // Certain have not genome name reference
        if (!(value == null || value.isEmpty())) {
          genomes.put(key, value);
        }
      }
      br.close();

    } catch (final IOException ignored) {
      LOGGER.warning("Reading alias genomes file failed: "
          + "none genome sample can be used for detection contamination.");
      return Collections.emptyMap();
    }

    return Collections.unmodifiableMap(genomes);
  }

  /**
   * Add an alias.
   * @param genomeName alias
   * @param alias the value of the alias
   */
  public void addAlias(final String genomeName, final String alias) {

    checkNotNull(genomeName, "genomeName parameter cannot be null");
    checkNotNull(alias, "alias parameter cannot be null");

    this.genomesAliases.put(genomeName, alias);
  }

  //
  // Static methods
  //

  /**
   * Create a instance of GenomeAliases or if it exists return instance.
   * @param props the props
   * @return instance of GenomeAliases
   * @throws AozanException if the initialization of instance fail.
   */
  public static void initialize(final Map<String, String> props)
      throws AozanException {

    checkNotNull(props, "props argument cannot be null");

    if (singleton == null) {
      singleton = new GenomeAliases(props);
    }
  }

  /**
   * Create a instance of GenomeAliases or if it exists return instance.
   * @return instance of GenomeAliases
   * @throws AozanException if the instance doesn't exist
   */
  public static GenomeAliases getInstance() throws AozanException {

    if (singleton == null) {
      throw new IllegalStateException(
          "GenomeAliases instance doesn't exist. It should be initialize with congfiguration Aozan properties.");
    }
    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor of GenomeAliases.
   * @throws AozanException if the initialization of instance fail.
   */
  private GenomeAliases(final Map<String, String> props) throws AozanException {

    this.genomesAliases.putAll(loadAliasGenomesFile(props
        .get(Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY)));
  }

}
