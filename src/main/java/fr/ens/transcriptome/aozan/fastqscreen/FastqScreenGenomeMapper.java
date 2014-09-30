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

import static fr.ens.transcriptome.eoulsan.EoulsanRuntime.getSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.testng.collections.Lists;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.storages.GenomeDescStorage;
import fr.ens.transcriptome.eoulsan.data.storages.SimpleGenomeDescStorage;
import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.illumina.io.CasavaDesignCSVReader;

/**
 * This class read the alias genome file. It make correspondence between genome
 * name in casava design file and the genome name reference used for identified
 * index of bowtie mapper.
 * @since 1.3
 * @author Sandrine Perrin
 */
public class FastqScreenGenomeMapper {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  /** Spliter */
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults()
      .omitEmptyStrings();

  /** Pattern */
  private static final Pattern PATTERN = Pattern.compile(".,;:/-_'");

  private static FastqScreenGenomeMapper singleton;
  private final Map<String, String> properties;

  // Correspondence between genome name in casava design file
  private final Set<String> genomesReferencesSample;
  private final Set<String> genomesContaminants;

  // Correspondence between genome sample in run and genome name reference
  private final Map<String, String> genomesNamesConvertor;

  // Associated genome name from design file with valid genome call for mapping
  private final Map<String, String> genomesReferencesSampleRenamed;

  private final Set<String> genomesToMapping;

  private GenomeDescStorage storage;

  // ---------------------------------------------------------------------------------------------------------------------------------------------------

  // public void fromFQSTest() {
  // // Set reference genomes defined in configuration aozan file
  // String genomesPerDefault =
  // properties.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_KEY);
  //
  // if (genomesPerDefault == null || genomesPerDefault.length() == 0)
  // throw new AozanException(
  // "AozanTest FastqScreen : none default genome reference for tests define");
  //
  // final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();
  // Set<String> genomes = Sets.newLinkedHashSet(s.split(genomesPerDefault));
  //
  // // Set list of genome from samples to use in fastqscreen
  // Set<String> genomesSamples =
  // setGenomesNameReferenceSample(,
  // properties
  // .get(Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY));
  //
  // // Set a global list of the run
  // genomes.addAll(genomesSamples);
  // }

  // private void createMapAliasGenome(File aliasGenomeFile) {
  // try {
  //
  // if (aliasGenomeFile.exists()) {
  //
  // final BufferedReader br =
  // Files.newReader(aliasGenomeFile, Globals.DEFAULT_FILE_ENCODING);
  //
  // String line = null;
  //
  // while ((line = br.readLine()) != null) {
  //
  // final int pos = line.indexOf('=');
  // if (pos == -1)
  // continue;
  //
  // final String key = line.substring(0, pos);
  // final String value = line.substring(pos + 1);
  //
  // // Retrieve genomes identified in Casava design file
  // // Certain have not genome name reference
  // aliasGenomes.put(key, value);
  // }
  // br.close();
  // }
  //
  // } catch (IOException ignored) {
  // LOGGER
  // .warning("Reading alias genomes file failed : none genome sample can be used for detection contamination.");
  // }
  //
  // }

  /**
   * Make the correspondence between genome sample and the reference genomes
   * used by bowtie according to alias genomes file.
   * @param genomeAliasFile absolute path from alias genomes file
   * @param genomes set of genomes sample to convert
   * @return set of reference genomes
   */
  // private Set<String> convertListToGenomeReferenceName(
  // final String genomeAliasFile, final Set<String> genomes) {
  //
  // Set<String> genomesNameReference = Sets.newHashSet();
  // Set<String> genomesToAddInAliasGenomeFile = Sets.newHashSet();
  //
  // File aliasGenomeFile = new File(genomeAliasFile);
  //
  // // Initialize map of alias genomes
  // if (aliasGenomeFile.exists())
  // createMapAliasGenome(aliasGenomeFile);
  //
  // if (aliasGenomes.isEmpty())
  // // Return a empty set
  // return Collections.emptySet();
  //
  // for (String sampleGenomes : genomes) {
  //
  // // Check if it exists a name reference for this genome
  // if (aliasGenomes.containsKey(sampleGenomes)) {
  // String genomeNameReference = aliasGenomes.get(sampleGenomes);
  // if (genomeNameReference.length() > 0) {
  // genomesNameReference.add(genomeNameReference);
  //
  // // Add in map for fastqscreen collector
  // aliasGenomesForRun.put(sampleGenomes, genomeNameReference);
  // }
  //
  // } else {
  // // Genome not present in alias genome file
  // genomesToAddInAliasGenomeFile.add(sampleGenomes);
  // }
  // }
  //
  // // Update alias genomes file
  // updateAliasGenomeFile(aliasGenomeFile, genomesToAddInAliasGenomeFile);
  //
  // return genomesNameReference;
  // }

  /**
   * Set reference genomes for the samples of a run. Retrieve list of genomes
   * sample from casava design file and filtered them compared to alias genome
   * file.
   * @param casavaDesignPath absolute path of the casava design file
   * @param genomeAliasFile absolute path of the alias genomes file
   * @return list genomes references used in FastqScreenCollector
   */
  // private Set<String> extractGenomesNameFromDesign(final String
  // casavaDesignPath) {

  //
  // // Retrieve list of corresponding reference genome from casava design
  // file
  // return AliasGenomeFile.getInstance().convertListToGenomeReferenceName(
  // genomeAliasFile, genomesFromCasavaDesign);
  // }

  /**
   * @throws IOException
   * @throws AozanException
   */
  private Set<String> collectGenomesForMapping() throws AozanException {

    // Identify genomes can be use for mapping
    final Set<String> genomes = Sets.newHashSet();
    final Set<String> newGenomes = Sets.newHashSet();

    for (String genome : this.genomesReferencesSample) {
      final DataFile genomeFile = new DataFile("genome://" + genome);

      // Retrieve genome description if it exists
      GenomeDescription gdesc = null;
      try {
        gdesc = createGenomeDescription(genomeFile);
      } catch (Exception isIgnored) {
        // Do nothing
      }

      // Check if a genome is available for mapping
      if (gdesc != null) {
        // Genome description exist for the genome
        genomes.add(genome);

      } else {
        // Parse alias file to find a valid genome name
        final String aliasGenomeName = this.genomesNamesConvertor.get(genome);

        if (aliasGenomeName == null) {
          // No genome name found, add entry in alias genomes file
          newGenomes.add(genome);

        } else {
          // Replace genome name from design file by valid name
          genomes.add(aliasGenomeName);
          this.genomesReferencesSampleRenamed.put(genome, aliasGenomeName);
        }
      }
    }

    // Update alias genomes file
    updateAliasGenomeFile(Collections.unmodifiableSet(newGenomes));

    // Union genomes contaminants and genomes references
    Set<String> genomesToMapping =
        Sets.newLinkedHashSet(this.genomesContaminants);
    genomesToMapping.addAll(genomes);

    return Collections.unmodifiableSet(genomesToMapping);
  }

  /**
   * Create a GenomeDescription object from a Fasta file
   * @param genomeFile file used for create index
   * @return genomeDescription description of the genome
   * @throws AozanException if an error occurs while create genome description
   *           file
   * @throws IOException
   * @throws BadBioEntryException
   */
  public GenomeDescription createGenomeDescription(final DataFile genomeFile)
      throws BadBioEntryException, IOException {

    if (!genomeFile.exists())
      LOGGER.warning("Fastqscreen "
          + genomeFile.getBasename()
          + " not exists, Index mapper can't be created.");

    GenomeDescription desc = null;

    if (storage != null) {
      desc = storage.get(genomeFile);
    }

    // Compute the genome description
    if (desc == null) {
      desc =
          GenomeDescription.createGenomeDescFromFasta(genomeFile.open(),
              genomeFile.getName());

      if (storage != null)
        storage.put(genomeFile, desc);
    }

    return desc;
  }

  /**
   * Add the genome of the sample in the file which does correspondence with
   * reference genome
   * @param genomesToAdd genomes must be added in alias genomes file
   */
  private void updateAliasGenomeFile(final Set<String> genomesToAdd) {

    // None genome to add
    if (genomesToAdd.isEmpty())
      return;

    final File aliasGenomesFile =
        new File(
            this.properties
                .get(Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY));

    try {
      if (aliasGenomesFile.exists()) {

        final Writer fw =
            Files.asCharSink(aliasGenomesFile, Globals.DEFAULT_FILE_ENCODING,
                FileWriteMode.APPEND).openStream();

        for (String genomeSample : genomesToAdd)
          fw.write(genomeSample + "=\n");

        fw.flush();
        fw.close();
      }
    } catch (IOException ignored) {
      LOGGER
          .warning("Writing alias genomes file failed : file can not be updated.");
    }
  }

  /**
   * @return
   */
  private Set<String> initGenomesReferencesSample() {

    // Samplesheet file for demultiplexing
    final File designFile =
        new File(this.properties.get(QC.CASAVA_DESIGN_PATH));

    final Set<String> genomesFromDesign = Sets.newHashSet();

    if (designFile.exists() && designFile.isFile()) {

      final CasavaDesignCSVReader casavaReader;
      final CasavaDesign casavaDesign;

      try {
        // Reading casava design file in format csv
        casavaReader = new CasavaDesignCSVReader(designFile);
        casavaDesign = casavaReader.read();

      } catch (Exception e) {
        // Return empty list
        return Collections.emptySet();
      }

      // Retrieve all genome sample included in casava design file
      for (CasavaSample casavaSample : casavaDesign) {
        final String genomeSample =
            casavaSample.getSampleRef().replaceAll("\"", "").toLowerCase();

        // Replace all symbols not letters or numbers by space
        PATTERN.matcher(genomeSample).replaceAll(" ");

        genomesFromDesign.add(genomeSample.trim());
      }
      return genomesFromDesign;
    }

    return Collections.emptySet();
  }

  /**
   * Initialization genomes or dataset contaminant define in Aozan
   * configuration.
   * @return genomes list
   * @throws AozanException
   */
  private Set<String> initGenomesContaminant() throws AozanException {

    final String val =
        this.properties.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_KEY);

    final Set<String> genomes = Sets.newHashSet();

    if (val == null || val.trim().length() == 0)
      throw new AozanException("FastqScreen : none genomes contaminant define.");

    for (String genome : COMMA_SPLITTER.split(val)) {
      final DataFile genomeFile = new DataFile("genome://" + genome);

      // Retrieve genome description if it exists
      GenomeDescription gdesc = null;
      try {
        gdesc = createGenomeDescription(genomeFile);
      } catch (Exception isIgnored) {
        // Do nothing
      }
      // Check genomes can be use for mapping
      if (gdesc != null) {
        genomes.add(genome);
      }
    }

    return Collections.unmodifiableSet(genomes);
  }

  /**
   * Create a map which does correspondence between genome of sample and
   * reference genome from a file, the path is in aozan configuration
   */
  private Map<String, String> loadAliasGenomesFile() {
    final File aliasGenomesFile =
        new File(
            this.properties
                .get(Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY));

    final Map<String, String> genomes = Maps.newHashMap();

    // No found alias genomes file
    if (!aliasGenomesFile.exists())
      return Collections.emptyMap();

    try {
      // Read alias genomes files
      final BufferedReader br =
          Files.newReader(aliasGenomesFile, Globals.DEFAULT_FILE_ENCODING);

      String line = null;

      while ((line = br.readLine()) != null) {

        final int pos = line.indexOf('=');
        if (pos == -1)
          continue;

        final String key = line.substring(0, pos);
        final String value = line.substring(pos + 1);

        // Retrieve genomes identified in Casava design file
        // Certain have not genome name reference
        genomes.put(key, value);
      }
      br.close();

    } catch (IOException ignored) {
      LOGGER
          .warning("Reading alias genomes file failed : none genome sample can be used for detection contamination.");
      return Collections.emptyMap();
    }

    return Collections.unmodifiableMap(genomes);
  }

  /**
   * Return the reference genome corresponding to the genome sample if it is
   * present in alias genomes file.
   * @param genome name of genome sample
   * @return reference genome corresponding to genome if it exists or empty
   *         string or null if no genome exist.
   */
  public String getGenomeReferenceCorresponding(final String genome) {

    final String genomeTrimmed =
        genome.replaceAll("\"", "").trim().toLowerCase();

    return this.genomesReferencesSampleRenamed.get(genomeTrimmed);

  }

  public Set<String> getGenomesToMapping() {
    return this.genomesToMapping;
  }

  public boolean isGenomeContamination(final String genome) {
    return this.genomesContaminants.contains(genome);
  }

  public Set<String> getGenomesContaminants() {
    return this.genomesContaminants;
  }

  /**
   * Create a instance of FastqScreenGenomeMapper or if it exists return
   * instance
   * @param properties map from Aozan configuration
   * @return instance of FastqScreenGenomeMapper
   * @throws AozanException if the initialization of instance fail.
   */
  public static FastqScreenGenomeMapper getInstance(
      final Map<String, String> props) throws AozanException {

    if (singleton == null) {
      singleton = new FastqScreenGenomeMapper(props);

    }
    return singleton;
  }

  /**
   * Create a instance of FastqScreenGenomeMapper or if it exists return
   * instance
   * @return instance of FastqScreenGenomeMapper
   * @throws AozanException if the instance doesn't exist
   */
  public static FastqScreenGenomeMapper getInstance() throws AozanException {

    if (singleton == null) {
      // singleton = new FastqScreenGenomeMapper(null);
      throw new AozanException(
          "FastqScreenGenomeMapper instance doesn't exist. It should be initialize with congfiguration Aozan properties.");
    }
    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor of FastqScreenGenomeMapper
   * @throws AozanException if the initialization of instance fail.
   */
  private FastqScreenGenomeMapper(final Map<String, String> props)
      throws AozanException {
    this.properties = props;

    fr.ens.transcriptome.eoulsan.Settings settings = getSettings();

    settings.setGenomeDescStoragePath(properties
        .get(Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_DESC_PATH_KEY));
    settings.setGenomeMapperIndexStoragePath(properties
        .get(Settings.QC_CONF_FASTQSCREEN_SETTINGS_MAPPERS_INDEXES_PATH_KEY));
    settings.setGenomeStoragePath(properties
        .get(Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_KEY));

    DataFile genomeDescStoragePath =
        new DataFile(settings.getGenomeDescStoragePath());
    this.storage = SimpleGenomeDescStorage.getInstance(genomeDescStoragePath);

    this.genomesReferencesSampleRenamed = Maps.newHashMap();

    // Load alias genomes file
    this.genomesNamesConvertor = loadAliasGenomesFile();

    // Load genomes references sample from design file
    this.genomesReferencesSample = initGenomesReferencesSample();

    // Load genomes contaminant
    this.genomesContaminants = initGenomesContaminant();

    // Collect genomes useful to contaminant detection
    this.genomesToMapping = collectGenomesForMapping();

  }

}
