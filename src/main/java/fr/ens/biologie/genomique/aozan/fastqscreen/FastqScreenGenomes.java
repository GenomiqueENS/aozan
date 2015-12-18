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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class read the alias genome file. It make correspondence between genome
 * name in casava design file and the genome name reference used for identified
 * index of bowtie mapper.
 * @since 1.3
 * @author Sandrine Perrin
 */
public class FastqScreenGenomes {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  /** Splitter. */
  private static final Splitter COMMA_SPLITTER =
      Splitter.on(',').trimResults().omitEmptyStrings();

  /** Pattern. */
  private static final Pattern PATTERN = Pattern.compile(".,;:/-_'");

  // Map between genome name in bcl2fastq design file
  private final Set<String> genomesReferencesSample;
  private final Set<String> contaminantGenomes;

  // Correspondence between genome sample in run and genome name reference
  // private final Map<String, String> genomesNamesConvertor;

  private final Set<String> genomesToMap;

  /**
   * Set reference genomes for the samples of a run. Retrieve list of genomes
   * sample from casava design file and filtered them compared to alias genome
   * file. Keep only if it can be create the genome description object.
   * @return collection valid genomes names can be use for mapping
   * @throws AozanException if an error occurs during updating alias genomes
   *           file
   */
  private Set<String> collectGenomesForMapping(final File aliasGenomesFile)
      throws AozanException {

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
    this.updateAliasGenomeFile(aliasGenomesFile,
        Collections.unmodifiableSet(newGenomes));

    // Union genomes contaminants and genomes references
    final Set<String> genomesToMapping =
        Sets.newLinkedHashSet(this.contaminantGenomes);
    genomesToMapping.addAll(genomes);

    return Collections.unmodifiableSet(genomesToMapping);
  }

  /**
   * Add the genome of the sample in the file which does correspondence with
   * reference genome.
   * @param genomesToAdd genomes must be added in alias genomes file
   */
  private void updateAliasGenomeFile(final File aliasGenomesFile,
      final Set<String> genomesToAdd) {

    // None genome to add
    if (genomesToAdd.isEmpty()) {
      return;
    }

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
  private Set<String> initGenomesReferencesSample(final File designFile) {

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
        String genomeSample =
            casavaSample.getSampleRef().replaceAll("\"", "").toLowerCase();

        // Replace all symbols not letters or numbers by space
        genomeSample = PATTERN.matcher(genomeSample).replaceAll(" ");

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
  private Set<String> initGenomesContaminant(
      final String contaminantGenomeNames) throws AozanException {

    // Set genomes in configuration file
    final Set<String> genomes = new HashSet<>();

    if (contaminantGenomeNames == null || contaminantGenomeNames.isEmpty()) {
      throw new AozanException("FastqScreen : no contaminant genome defined.");
    }

    for (final String genome : COMMA_SPLITTER.split(contaminantGenomeNames)) {
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
              + contaminantGenomeNames + " found: "
              + Joiner.on("-").join(genomes));
    }

    return Collections.unmodifiableSet(genomes);
  }

  /**
   * Get the collection genomes name can be used for the mapping.
   * @return collection genomes name for mapping
   */
  public Set<String> getGenomesToMap() {
    return Collections.unmodifiableSet(this.genomesToMap);
  }

  /**
   * Check genomes name included in collection genomes contaminants.
   * @param genome genome name
   * @return true if is a genome contaminant otherwise false
   */
  public boolean isContaminantGenome(final String genome) {

    checkNotNull(genome, "genome argument cannot be null");

    return this.contaminantGenomes.contains(genome);
  }

  /**
   * Get the collection of genomes contaminants can be used for mapping.
   * @return collection of genomes contaminans
   */
  public Set<String> getContaminantGenomes() {
    return Collections.unmodifiableSet(this.contaminantGenomes);
  }

  //
  // Static constructors
  //

  /**
   * Private constructor of FastqScreenGenomeMapper.
   * @param properties configuration properties
   * @return a new instance of FastqScreenGenomes
   * @throws AozanException if the initialization of instance fail.
   */
  public static FastqScreenGenomes newInstance(
      final Map<String, String> properties) throws AozanException {

    checkNotNull(properties, "props argument cannot be null");

    final File aliasGenomesFile = new File(properties
        .get(Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY));
    final File designFile = new File(properties.get(QC.CASAVA_DESIGN_PATH));
    final String contaminantGenomeNames =
        properties.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_KEY);

    return new FastqScreenGenomes(aliasGenomesFile, designFile,
        contaminantGenomeNames);
  }

  /**
   * Private constructor of FastqScreenGenomeMapper.
   * @param properties configuration properties
   * @return a new instance of FastqScreenGenomes
   * @throws AozanException if the initialization of instance fail.
   */
  public static FastqScreenGenomes newInstance(final Properties properties)
      throws AozanException {

    checkNotNull(properties, "properties argument cannot be null");

    final File aliasGenomesFile = new File(properties.getProperty(
        Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY));
    final File designFile =
        new File(properties.getProperty(QC.CASAVA_DESIGN_PATH));
    final String contaminantGenomeNames =
        properties.getProperty(Settings.QC_CONF_FASTQSCREEN_GENOMES_KEY);

    return new FastqScreenGenomes(aliasGenomesFile, designFile,
        contaminantGenomeNames);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param aliasGenomesFile alias genomes file
   * @param designFile design file
   * @param contaminantGenomeNames a string with the list of the contaminant
   *          genomes
   * @throws AozanException if an error occurs while creating the object
   */
  public FastqScreenGenomes(final File aliasGenomesFile, final File designFile,
      final String contaminantGenomeNames) throws AozanException {

    checkNotNull(aliasGenomesFile, "aliasGenomesFile argument cannot be null");
    checkNotNull(designFile, "designFile argument cannot be null");
    checkNotNull(contaminantGenomeNames,
        "contaminantGenomeNames argument cannot be null");

    // Collect genomes references list sample from design file
    this.genomesReferencesSample = initGenomesReferencesSample(designFile);

    // Collect genomes contaminant list
    this.contaminantGenomes = initGenomesContaminant(contaminantGenomeNames);

    // Collect genomes useful to contaminant detection
    this.genomesToMap = collectGenomesForMapping(aliasGenomesFile);
  }

}
