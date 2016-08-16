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

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class read the alias genome file. It make correspondence between genome
 * name in Bcl2fastq samplesheet file and the genome name reference used for
 * identified index of bowtie mapper.
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

  private final Set<String> contaminantGenomes;
  private final Set<String> sampleGenomes;

  /**
   * Set reference genomes for the samples of a run. Retrieve list of genomes
   * sample from Bcl2fastq samplesheet file and filtered them compared to alias
   * genome file. Keep only if it can be create the genome description object.
   * @return collection valid genomes names can be use for mapping
   * @throws AozanException if an error occurs during updating alias genomes
   *           file
   */
  private Set<String> initSampleGenomes(
      final Set<String> genomesReferencesSample, final File aliasGenomesFile)
      throws AozanException {

    // Identify genomes can be use for mapping
    final Set<String> genomes = new HashSet<>();

    final GenomeAliases genomeAliases = GenomeAliases.getInstance();

    for (final String genomeName : genomesReferencesSample) {

      // Retrieve genome description if it exists
      GenomeDescription gdesc = null;
      try {
        gdesc = GenomeDescriptionCreator.getInstance()
            .createGenomeDescription(new DataFile("genome://" + genomeName));
      } catch (final Exception isIgnored) {
        // Do nothing
      }

      // Check if a genome is available for mapping
      if (gdesc != null) {
        // Genome description exist for the genome
        genomes.add(genomeName);

        // Add the genome as Alias if not exists
        if (!genomeAliases.contains(genomeName)) {
          genomeAliases.addAlias(genomeName, genomeName);
        }

      } else {

        if (!genomeAliases.contains(genomeName)) {
          genomeAliases.addUnknownAlias(genomeName);
        } else {
          genomes.add(genomeAliases.get(genomeName));
        }

      }
    }

    // Update new unknown genome aliases
    genomeAliases.saveUnknownAliases();

    // Union genomes contaminants and genomes references
    final Set<String> genomesToMap =
        Sets.newLinkedHashSet(this.contaminantGenomes);
    genomesToMap.addAll(genomes);

    return Collections.unmodifiableSet(genomesToMap);
  }

  /**
   * Initialize collection on genomes reference names from the samples
   * sequencing.
   * @return collection on genomes reference names for the samples
   */
  private Set<String> createSampleRefsFromSamplesheetFile(
      final File samplesheetFile) {

    final Set<String> genomesFromSamplesheet = new HashSet<>();

    if (samplesheetFile.exists() && samplesheetFile.isFile()) {

      final SampleSheetCSVReader samplesheetReader;
      final SampleSheet samplesheet;

      try {
        // Reading Bcl2fastq samplesheet file in format csv
        samplesheetReader = new SampleSheetCSVReader(samplesheetFile);
        samplesheet = samplesheetReader.read();

      } catch (final Exception e) {
        // Return empty list
        return Collections.emptySet();
      }

      // Retrieve all genome sample included in Bcl2fastq samplesheet file
      for (final Sample sample : samplesheet) {
        String genomeSample =
            sample.getSampleRef().replaceAll("\"", "").toLowerCase();

        // Replace all symbols not letters or numbers by space
        genomeSample = PATTERN.matcher(genomeSample).replaceAll(" ");

        genomesFromSamplesheet.add(genomeSample.trim());
      }

      // TODO
      LOGGER
          .warning("FQS-genomeMapper: list genome names found in samplesheet: "
              + Joiner.on(", ").join(genomesFromSamplesheet));

      return genomesFromSamplesheet;

    }
    // TODO
    LOGGER.warning("FQS-genomeMapper: no genome name found in samplesheet file "
        + samplesheetFile.getAbsolutePath());

    // Fail to read the samplesheet file
    return Collections.emptySet();
  }

  /**
   * Initialization genomes or dataset contaminant define in Aozan
   * configuration.
   * @return genomes list
   * @throws AozanException
   */
  private Set<String> initContaminantGenomes(
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
          "FastqScreen : no genome contaminant can be use from configuration file: "
              + contaminantGenomeNames);
    }

    return Collections.unmodifiableSet(genomes);
  }

  /**
   * Get the collection genomes name can be used for the mapping.
   * @return collection genomes name for mapping
   */
  public Set<String> getSampleGenomes() {
    return Collections.unmodifiableSet(this.sampleGenomes);
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
   * @param testConfiguration configuration properties
   * @return a new instance of FastqScreenGenomes
   * @throws AozanException if the initialization of instance fail.
   */
  public static FastqScreenGenomes newInstance(
      final TestConfiguration testConfiguration) throws AozanException {

    checkNotNull(testConfiguration,
        "testConfiguration argument cannot be null");

    final File aliasGenomesFile = new File(testConfiguration
        .get(Settings.QC_CONF_FASTQSCREEN_GENOMES_ALIAS_PATH_KEY));
    final File sampleshettFile =
        new File(testConfiguration.get(QC.BCL2FASTQ_SAMPLESHEET_PATH));
    final String contaminantGenomeNames =
        testConfiguration.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_KEY);

    return new FastqScreenGenomes(aliasGenomesFile, sampleshettFile,
        contaminantGenomeNames);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param aliasGenomesFile alias genomes file
   * @param samplesheetFile samplesheet file
   * @param contaminantGenomeNames a string with the list of the contaminant
   *          genomes
   * @throws AozanException if an error occurs while creating the object
   */
  public FastqScreenGenomes(final File aliasGenomesFile,
      final File samplesheetFile, final String contaminantGenomeNames)
      throws AozanException {

    checkNotNull(aliasGenomesFile, "aliasGenomesFile argument cannot be null");
    checkNotNull(samplesheetFile, "samplesheetFile argument cannot be null");
    checkNotNull(contaminantGenomeNames,
        "contaminantGenomeNames argument cannot be null");

    // Collect genomes contaminant list
    this.contaminantGenomes = initContaminantGenomes(contaminantGenomeNames);

    // Collect genomes useful to contaminant detection
    this.sampleGenomes = initSampleGenomes(
        createSampleRefsFromSamplesheetFile(samplesheetFile), aliasGenomesFile);
  }

}
