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

package fr.ens.biologie.genomique.aozan.fastqscreen;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.Strings;
import com.google.common.io.FileWriteMode;
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

  // Associated genome name from samplesheet file with valid genome call for
  // mapping
  private final File genomeAliasesFile;
  private final Map<String, String> genomeAliases = new HashMap<>();
  private final Set<String> unknownAliases = new HashSet<>();
  private final Set<String> newUnknownAliases = new HashSet<>();

  /**
   * Return the reference genome corresponding to the genome sample if it is
   * present in alias genomes file.
   * @param genomeName name of genome sample
   * @return reference genome corresponding to genome if it exists or empty
   *         string or null if no genome exist.
   */
  public String get(final String genomeName) {

    checkNotNull(genomeName, "genome argument cannot be null");

    final String key = createKey(genomeName);

    if (!this.genomeAliases.containsKey(key)) {
      return null;
    }

    return this.genomeAliases.get(key).trim();
  }

  public boolean contains(final String genomeName) {

    checkNotNull(genomeName, "genome argument cannot be null");

    final String key = createKey(genomeName);

    return this.genomeAliases.containsKey(key);
  }

  /**
   * Create a map which does correspondence between genome of sample and
   * reference genome from a file, the path is in aozan configuration.
   * @param aliasFile alias file
   * @throws AozanException if genome aliases file doesn't exist.
   */
  private void loadGenomeAliasFile(final File aliasFile) throws AozanException {

    if (aliasFile == null) {
      LOGGER.fine("FASTQSCREEN: "
          + "No genome alias file defined in Aozan configuration");
      return;
    }

    // Not found alias genomes file
    if (!aliasFile.exists()) {
      throw new AozanException("FastqScreen alias genome file doesn't exists "
          + aliasFile.getAbsolutePath());
    }

    try {
      // Read alias genomes files
      final BufferedReader br =
          Files.newReader(aliasFile, Globals.DEFAULT_FILE_ENCODING);

      String line = null;

      while ((line = br.readLine()) != null) {

        line = line.trim();

        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        final int pos = line.indexOf('=');
        if (pos == -1) {
          continue;
        }

        final String key = line.substring(0, pos);
        final String value = line.substring(pos + 1);

        if (!(value == null || value.isEmpty())) {
          this.genomeAliases.put(createKey(key), value);
        } else {
          this.unknownAliases.add(key);
        }

      }
      br.close();

    } catch (final IOException ignored) {
      LOGGER.warning("Reading alias genomes file failed: "
          + "none genome sample can be used for detection contamination.");
    }
  }

  /**
   * Add an alias.
   * @param genomeName alias
   * @param alias the value of the alias
   */
  public void addAlias(final String genomeName, final String alias) {

    checkNotNull(genomeName, "genomeName parameter cannot be null");
    checkNotNull(alias, "alias parameter cannot be null");

    this.genomeAliases.put(genomeName, alias);
  }

  /**
   * Add an alias.
   * @param genomeName the name of the unknown alias
   */
  public void addUnknownAlias(final String genomeName) {

    checkNotNull(genomeName, "genomeName parameter cannot be null");

    if (this.unknownAliases.contains(createKey(genomeName))) {
      this.newUnknownAliases.add(genomeName);
    }
  }

  /**
   * Add the genome of the sample in the file which does correspondence with
   * reference genome.
   */
  public void saveUnknownAliases() {

    // None genome to add
    if (this.newUnknownAliases.isEmpty()) {
      return;
    }

    try {
      if (this.genomeAliasesFile.exists()) {

        final Writer fw =
            Files
                .asCharSink(this.genomeAliasesFile,
                    Globals.DEFAULT_FILE_ENCODING, FileWriteMode.APPEND)
                .openStream();

        for (final String genomeSample : this.newUnknownAliases) {
          fw.write(genomeSample + "=\n");
        }

        fw.flush();
        fw.close();
      }
    } catch (final IOException ignored) {
      LOGGER.warning(
          "Writing alias genomes file failed : file can not be updated.");
    }
  }

  //
  // Static methods
  //

  /**
   * Create a instance of GenomeAliases or if it exists return instance.
   * @param settings Aozan settings
   * @throws AozanException if the initialization of instance fail.
   */
  public static void initialize(final Settings settings) throws AozanException {

    checkNotNull(settings, "conf argument cannot be null");

    if (singleton == null) {
      singleton = new GenomeAliases(settings);
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

  private String createKey(final String genomeName) {

    return genomeName.replaceAll("\"", "").replaceAll(" ", "").trim()
        .toLowerCase();
  }

  //
  // Constructor
  //

  /**
   * Private constructor of GenomeAliases.
   * @param settings Aozan settings
   * @throws AozanException if the initialization of instance fail.
   */
  private GenomeAliases(final Settings settings) throws AozanException {

    final String genomeAliasesFilename = settings
        .get(Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY);

    this.genomeAliasesFile = Strings.emptyToNull(genomeAliasesFilename) == null
        ? null : new File(genomeAliasesFilename.trim());

    loadGenomeAliasFile(this.genomeAliasesFile);

    LOGGER.info("FASTQSCREEN: "
        + this.genomeAliases.size() + " genome alias(es) loaded");
  }

}
