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

package fr.ens.transcriptome.aozan.fastqscreen;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.illumina.samplesheet.Sample;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class read the alias genome file. It make correspondence between genome
 * name in casava design file and the genome name reference used for identified
 * index of bowtie mapper.
 * @since 1.3
 * @author Sandrine Perrin
 */
public class FastqScreenGenomeMapper {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  /** Splitter. */
  private static final Splitter COMMA_SPLITTER =
      Splitter.on(',').trimResults().omitEmptyStrings();

  /** Pattern. */
  private static final Pattern PATTERN = Pattern.compile(".,;:/-_'");

  private static FastqScreenGenomeMapper singleton;
  private final Map<String, String> properties;

  // Correspondence between genome name in casava design file
  private final Set<String> genomesReferencesSample;
  private final Set<String> genomesContaminants;

  // Correspondence between genome sample in run and genome name reference
  // private final Map<String, String> genomesNamesConvertor;

  private final Set<String> genomesToMapping;

  /**
   * Set reference genomes for the samples of a run. Retrieve list of genomes
   * sample from casava design file and filtered them compared to alias genome
   * file. Keep only if it can be create the genome description object.
   * @return collection valid genomes names can be use for mapping
   * @throws AozanException if an error occurs during updating alias genomes
   *           file
   */
  private Set<String> collectGenomesForMapping() throws AozanException {

    // Identify genomes can be use for mapping
    final Set<String> genomes = new HashSet<>();
    final Set<String> newGenomes = new HashSet<>();

    final GenomeAliases genomesAliases = GenomeAliases.getInstance();

    for (final String genome : this.genomesReferencesSample) {
      final DataFile genomeFile = new DataFile("genome://" + genome);

      // Retrieve genome description if it exists
      GenomeDescription gdesc = null;
      try {
        gdesc = GenomeDescriptionCreator.getInstance()
            .createGenomeDescription(genomeFile);
      } catch (final Exception isIgnored) {
        // Do nothing
      }

      // Check if a genome is available for mapping
      if (gdesc != null) {
        // Genome description exist for the genome
        genomes.add(genome);
        genomesAliases.addAlias(genome, genome);

      } else {
        // Parse alias file to find a valid genome name
        final String aliasGenomeName =
            genomesAliases.getGenomeNameFromAlias(genome);

        if (aliasGenomeName == null || aliasGenomeName.isEmpty()) {
          // No genome name found, add entry in alias genomes file
          newGenomes.add(genome);

        } else {
          // Replace genome name from design file by valid name
          genomes.add(aliasGenomeName);
          genomesAliases.addAlias(genome, aliasGenomeName);
        }
      }
    }

    // Update alias genomes file
    this.updateAliasGenomeFile(Collections.unmodifiableSet(newGenomes));

    // Union genomes contaminants and genomes references
    final Set<String> genomesToMapping =
        Sets.newLinkedHashSet(this.genomesContaminants);
    genomesToMapping.addAll(genomes);

    return Collections.unmodifiableSet(genomesToMapping);
  }

  /**
   * Add the genome of the sample in the file which does correspondence with
   * reference genome.
   * @param genomesToAdd genomes must be added in alias genomes file
   */
  private void updateAliasGenomeFile(final Set<String> genomesToAdd) {

    // None genome to add
    if (genomesToAdd.isEmpty()) {
      return;
    }

    final File aliasGenomesFile = new File(this.properties
        .get(Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY));

    try {
      if (aliasGenomesFile.exists()) {

        final Writer fw = Files.asCharSink(aliasGenomesFile,
            Globals.DEFAULT_FILE_ENCODING, FileWriteMode.APPEND).openStream();

        for (final String genomeSample : genomesToAdd) {
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

  /**
   * Initialize collection on genomes reference names from the samples
   * sequencing.
   * @return collection on genomes reference names for the samples
   */
  private Set<String> initGenomesReferencesSample() {

    // Samplesheet file for demultiplexing
    final File designFile =
        new File(this.properties.get(QC.CASAVA_DESIGN_PATH));

    final Set<String> genomesFromDesign = new HashSet<>();

    if (designFile.exists() && designFile.isFile()) {

      final SampleSheetCSVReader samplesheetReader;
      final SampleSheet samplesheet;

      try {
        // Reading casava design file in format csv
        samplesheetReader = new SampleSheetCSVReader(designFile);
        samplesheet = samplesheetReader.read();

      } catch (final Exception e) {
        // Return empty list
        return Collections.emptySet();
      }

      // Retrieve all genome sample included in casava design file
      for (final Sample casavaSample : samplesheet) {
        final String genomeSample =
            casavaSample.getSampleRef().replaceAll("\"", "").toLowerCase();

        // Replace all symbols not letters or numbers by space
        PATTERN.matcher(genomeSample).replaceAll(" ");

        genomesFromDesign.add(genomeSample.trim());
      }

      // TODO
      LOGGER.warning("FQS-genomeMapper: list genomes name find in design "
          + Joiner.on(", ").join(genomesFromDesign));

      return genomesFromDesign;

    }
    // TODO
    LOGGER.warning("FQS-genomeMapper: no genomes name found in design file "
        + designFile.getAbsolutePath());

    // Fail to read design file
    return Collections.emptySet();
  }

  /**
   * Initialization genomes or dataset contaminant define in Aozan
   * configuration.
   * @return genomes list
   * @throws AozanException
   */
  private Set<String> initGenomesContaminant() throws AozanException {

    // Set genomes in configuration file
    final String val =
        this.properties.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_KEY);

    final Set<String> genomes = new HashSet<>();

    if (val == null || val.isEmpty()) {
      throw new AozanException(
          "FastqScreen : none genomes contaminant define.");
    }

    for (final String genome : COMMA_SPLITTER.split(val)) {
      final DataFile genomeFile = new DataFile("genome://" + genome);

      // Retrieve genome description if it exists
      GenomeDescription gdesc = null;
      try {
        gdesc = GenomeDescriptionCreator.getInstance()
            .createGenomeDescription(genomeFile);
      } catch (final Exception isIgnored) {
        // Do nothing
      }
      // Check genomes can be use for mapping
      if (gdesc != null) {
        genomes.add(genome);
      }
    }

    // No genome can be use for mapping
    if (genomes.isEmpty()) {
      throw new AozanException(
          "FastqScreen : none genomes contaminant can be use from configuration file: "
              + val + " found: " + Joiner.on("-").join(genomes));
    }

    return Collections.unmodifiableSet(genomes);
  }

  /**
   * Get the collection genomes name can be used for the mapping.
   * @return collection genomes name for mapping
   */
  public Set<String> getGenomesToMapping() {
    return this.genomesToMapping;
  }

  /**
   * Check genomes name included in collection genomes contaminants.
   * @param genome genome name
   * @return true if is a genome contaminant otherwise false
   */
  public boolean isGenomeContamination(final String genome) {

    checkNotNull(genome, "genome argument cannot be null");

    return this.genomesContaminants.contains(genome);
  }

  /**
   * Get the collection of genomes contaminants can be used for mapping.
   * @return collection of genomes contaminans
   */
  public Set<String> getGenomesContaminants() {
    return this.genomesContaminants;
  }

  /**
   * Create a instance of FastqScreenGenomeMapper or if it exists return
   * instance.
   * @param props the props
   * @return instance of FastqScreenGenomeMapper
   * @throws AozanException if the initialization of instance fail.
   */
  public static FastqScreenGenomeMapper getInstance(
      final Map<String, String> props) throws AozanException {

    checkNotNull(props, "props argument cannot be null");

    if (singleton == null) {
      singleton = new FastqScreenGenomeMapper(props);

    }
    return singleton;
  }

  /**
   * Create a instance of FastqScreenGenomeMapper or if it exists return
   * instance.
   * @return instance of FastqScreenGenomeMapper
   * @throws AozanException if the instance doesn't exist
   */
  public static FastqScreenGenomeMapper getInstance() throws AozanException {

    if (singleton == null) {
      throw new AozanException(
          "FastqScreenGenomeMapper instance doesn't exist. "
              + "It should be initialize with congfiguration Aozan properties.");
    }
    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor of FastqScreenGenomeMapper.
   * @throws AozanException if the initialization of instance fail.
   */
  private FastqScreenGenomeMapper(final Map<String, String> props)
      throws AozanException {

    checkNotNull(props, "props argument cannot be null");

    this.properties = new HashMap<>(props);

    // Collect genomes references list sample from design file
    this.genomesReferencesSample = this.initGenomesReferencesSample();

    // Collect genomes contaminant list
    this.genomesContaminants = this.initGenomesContaminant();

    // Collect genomes useful to contaminant detection
    this.genomesToMapping = this.collectGenomesForMapping();
  }

}
