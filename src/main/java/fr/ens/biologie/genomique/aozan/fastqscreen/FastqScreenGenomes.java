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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.aozan.Aozan2Logger;
import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.Storages;
import fr.ens.biologie.genomique.aozan.tests.TestConfiguration;
import fr.ens.biologie.genomique.kenetre.bio.BadBioEntryException;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;

/**
 * This class read the alias genome file. It make correspondence between genome
 * name in Bcl2fastq samplesheet file and the genome name reference used for
 * identified index of bowtie mapper.
 * @since 1.3
 * @author Sandrine Perrin
 */
public class FastqScreenGenomes {

  /** Logger. */
  private static final Logger LOGGER = Aozan2Logger.getLogger();

  /** Pattern. */
  private static final Pattern PATTERN = Pattern.compile(".,;:/-_'");

  private final Set<String> contaminantGenomes;
  private final Set<String> sampleGenomes;

  /**
   * Set reference genomes for the samples of a run.
   * @return collection valid genomes names can be use for mapping
   * @throws AozanException if an error occurs during updating alias genomes
   *           file
   */
  private static Set<String> initGenomes(final Collection<String> genomeNames)
      throws AozanException {

    Objects.requireNonNull(genomeNames);

    final Set<String> result = new LinkedHashSet<>();

    final GenomeAliases genomeAliases = GenomeAliases.getInstance();

    for (final String genomeName : genomeNames) {

      if (genomeName == null) {
        continue;
      }

      final String newGenomeName =
          genomeAliases.contains(genomeName.toLowerCase())
              ? genomeAliases.get(genomeName.toLowerCase()) : genomeName;

      // Check if a genome is available for mapping
      if (genomeExists(newGenomeName)) {

        // Genome description exist for the genome
        result.add(newGenomeName);

        // Add the genome as Alias if not exists
        if (!genomeAliases.contains(genomeName)) {
          genomeAliases.addAlias(genomeName, genomeName);
        }

      } else {

        if (!genomeAliases.contains(genomeName)) {
          genomeAliases.addUnknownAlias(genomeName);
        } else {
          result.add(genomeAliases.get(genomeName));
        }

      }
    }

    // Update new unknown genome aliases
    genomeAliases.saveUnknownAliases();

    return result;
  }

  /**
   * Check if a genome name exists.
   * @param genomeName name of the genome to check
   * @return true if the genome exists
   */
  private static boolean genomeExists(String genomeName) {

    Storages storages = Storages.getInstance();

    if (!storages.isGenomeStorage()) {
      return false;
    }

    try {

      File genomeFile = storages.getGenomeStorage().getFile(genomeName);

      if (genomeFile == null) {
        return false;
      }

      GenomeDescription gdesc = storages.createGenomeDescription(genomeFile);

      return gdesc != null;

    } catch (BadBioEntryException | IOException e) {
      return false;
    }
  }

  /**
   * Initialize collection on genomes reference names from the samples
   * sequencing.
   * @return collection on genomes reference names for the samples
   */
  private Set<String> createSampleRefsFromSamplesheetFile(
      final SampleSheet samplesheet) {

    final Set<String> genomesFromSamplesheet = new HashSet<>();

    // Retrieve all genome sample included in Bcl2fastq samplesheet file
    for (final Sample sample : samplesheet) {

      if (sample.isSampleRefField()) {

        String genomeSample = sample.getSampleRef().replaceAll("\"", "");

        // Replace all symbols not letters or numbers by space
        genomeSample = PATTERN.matcher(genomeSample).replaceAll(" ");

        genomesFromSamplesheet.add(genomeSample.trim());
      }
    }

    // TODO
    LOGGER.warning("FQS-genomeMapper: list genome names found in samplesheet: "
        + Joiner.on(", ").join(genomesFromSamplesheet));

    return genomesFromSamplesheet;
  }

  private static List<String> splitComtaminantString(String s) {
    return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(s);
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

    requireNonNull(testConfiguration,
        "testConfiguration argument cannot be null");

    final SampleSheet samplesheet =
        testConfiguration.getSampleSheet(QC.SAMPLESHEET);
    final String contaminantGenomeNames =
        testConfiguration.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_KEY);

    return new FastqScreenGenomes(samplesheet, contaminantGenomeNames);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param samplesheet sampleSheet file
   * @param contaminantGenomeNames a string with the list of the contaminant
   *          genomes
   * @throws AozanException if an error occurs while creating the object
   */
  public FastqScreenGenomes(final SampleSheet samplesheet,
      final String contaminantGenomeNames) throws AozanException {

    requireNonNull(samplesheet, "sampleSheet argument cannot be null");
    requireNonNull(contaminantGenomeNames,
        "contaminantGenomeNames argument cannot be null");

    // Collect genomes contaminant list
    this.contaminantGenomes =
        initGenomes(splitComtaminantString(contaminantGenomeNames));

    if (contaminantGenomeNames.isEmpty()) {
      throw new AozanException("FastqScreen : no contaminant genome defined.");
    }

    // Collect genomes useful to contaminant detection
    this.sampleGenomes = new LinkedHashSet<>(this.contaminantGenomes);
    this.sampleGenomes
        .addAll(initGenomes(createSampleRefsFromSamplesheetFile(samplesheet)));
  }

}
