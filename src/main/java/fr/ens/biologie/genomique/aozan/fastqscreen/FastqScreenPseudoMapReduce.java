/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 3 or
 * later and CeCILL. This should be distributed with the code.
 * If you do not have a copy, see:
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
 * or to join the Aozan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.fastqscreen;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.toTimeHumanReadable;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.Bowtie2MapperProvider;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.BowtieMapperProvider;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.FileMapping;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.Mapper;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperIndex;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperInstance;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperProcess;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.modules.generators.GenomeMapperIndexer;
import fr.ens.biologie.genomique.eoulsan.util.LocalReporter;
import fr.ens.biologie.genomique.eoulsan.util.PseudoMapReduce;
import fr.ens.biologie.genomique.eoulsan.util.Reporter;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class account reads that map to each of the reference genome.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenPseudoMapReduce extends PseudoMapReduce {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  // Boolean use to update logger with parameter mapper only at the first
  // execution
  private static boolean firstDoMapRun = true;

  private static final String COUNTER_GROUP = "reads_mapping";
  private final Reporter reporter;

  private GenomeDescription desc = null;
  private final FastqScreenResult fastqScreenResult;
  private final File tmpDir;

  private final Pattern pattern = Pattern.compile("\t");

  private int readsProcessed = 0;
  private int readsMapped = 0;
  private final boolean pairedMode;
  private String genomeReference;

  private final String mapperArguments;

  private final String mapperName;

  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with reference genome.
   * @param fastqRead fastq file
   * @param genomes list of reference genome
   * @param sampleGenome genome reference corresponding to sample, can be null
   * @param threadNumber number threads used for mapping
   * @throws AozanException if an error occurs while mapping
   */
  public void doMap(final File fastqRead, final List<String> genomes,
      final String sampleGenome, final int threadNumber) throws AozanException {

    this.doMap(fastqRead, null, genomes, sampleGenome, threadNumber);
  }

  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with reference genome.
   * @param fastqRead1 fastq file
   * @param fastqRead2 fastq file in mode paired
   * @param genomes list of genome reference
   * @param sampleGenome genome reference corresponding to sample, can be null
   * @param threadNumber number threads used for mapping
   * @throws AozanException if an error occurs while mapping
   */
  public void doMap(final File fastqRead1, final File fastqRead2,
      final List<String> genomes, final String sampleGenome,
      final int threadNumber) throws AozanException {

    requireNonNull(fastqRead1, "fastqRead1 argument cannot be null");
    requireNonNull(genomes, "genomesForMapping argument cannot be null");

    if (this.pairedMode) {
      requireNonNull(fastqRead2, "fastqRead2 argument cannot be null");
    }

    // Define the number of thread to use
    final int mapperThreads = threadNumber > 0
        ? threadNumber : Runtime.getRuntime().availableProcessors();

    if (firstDoMapRun) {
      // Update logger at the first execution
      LOGGER.info("FASTQSCREEN: map "
          + fastqRead1.getName() + " on genomes "
          + Joiner.on(",").join(genomes));

      LOGGER.info("FASTQSCREEN: init "
          + this.mapperName + " mapper, arguments: \"" + this.mapperArguments
          + "\", mode: " + (pairedMode ? "paired" : "single") + ", threads: "
          + mapperThreads);
    }

    for (final String genome : genomes) {
      // Timer : for step mapping on genome
      final Stopwatch timer = Stopwatch.createStarted();

      LOGGER.info("FASTQSCREEN: map "
          + fastqRead1.getName()
          + (this.pairedMode ? ", " + fastqRead2.getName() : "") + " on "
          + genome);

      // Get the mapper object
      final Mapper mapper = Mapper.newMapper(this.mapperName);

      // Set mapper temporary directory
      mapper.setTempDirectory(this.tmpDir);

      try {

        // Create the mapper instance
        final MapperInstance mapperInstance =
            mapper.newMapperInstance("", "", true, null);

        final DataFile genomeFile = new DataFile("genome://" + genome);

        // get index Genome reference exists
        final File archiveIndexFile = createIndex(mapperInstance, genomeFile);

        if (archiveIndexFile == null) {
          LOGGER.warning(
              "FASTQSCREEN: archive index file not found for " + genome);
          continue;
        }

        final File indexDir = new File(
            StringUtils.filenameWithoutExtension(archiveIndexFile.getPath()));

        final FastqScreenSAMParser parser = new FastqScreenSAMParser(
            this.getMapOutputTempFile(), genome, this.pairedMode, this.desc);

        this.genomeReference = genome;
        this.fastqScreenResult.addGenome(genome, sampleGenome);

        // Do nothing if the file is empty
        if (fastqRead1.length() == 0) {
          parser.closeMapOutputFile();
        } else {

          // Create the MapperIndex object
          final MapperIndex mapperIndex =
              mapperInstance.newMapperIndex(archiveIndexFile, indexDir);

          // Create the mapping object
          FileMapping mapping = mapperIndex.newFileMapping(
              FastqFormat.FASTQ_SANGER, this.mapperArguments, mapperThreads,
              false, this.reporter, COUNTER_GROUP);

          // Create the MapperProcess
          final MapperProcess process = this.pairedMode
              ? mapping.mapPE(fastqRead1, fastqRead2, null, null)
              : mapping.mapSE(fastqRead1, null, null);

          // Parse SAM output
          parser.parseLines(process.getStout());

          // Wait the end of the process and do cleanup
          process.waitFor();

          // Throw an exception if an exception has occurred while mapping
          mapping.throwMappingException();
        }

        this.readsProcessed = parser.getReadsprocessed();

        LOGGER.fine("FASTQSCREEN: "
            + mapper.getName() + " mapping on genome " + genome + " in mode "
            + (this.pairedMode ? "paired" : "single") + ", in "
            + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

        timer.stop();

        firstDoMapRun = false;

      } catch (final IOException e) {
        throw new AozanException(e);
      }
    }
  }

  /**
   * Create a index with bowtie from the fasta file genome.
   * @param bowtie mapper
   * @param genomeDataFile fasta file of genome
   * @return File file of genome index
   * @throws IOException if an error occurs while using file index genome
   * @throws AozanException if an error occurs during call
   *           FastqScreenGenomeMapper instance.
   */
  private File createIndex(final MapperInstance bowtie,
      final DataFile genomeDataFile) throws IOException, AozanException {

    // Timer :
    final Stopwatch timer = Stopwatch.createStarted();

    final DataFile tempDir = new DataFile(this.tmpDir);
    final DataFile result = new DataFile(tempDir,
        "aozan-"
            + bowtie.getName().toLowerCase() + "-index-"
            + genomeDataFile.getName() + ".zip");

    // Create genome description
    try {
      this.desc = GenomeDescriptionCreator.getInstance()
          .createGenomeDescription(genomeDataFile);
    } catch (final BadBioEntryException e) {
      throw new AozanException(e);
    }

    if (this.desc == null) {
      return null;
    }

    // Check if the index has already been created/retrieved
    if (result.exists()) {
      return result.toFile();
    }

    final Map<String, String> additionnalArgument = Collections.emptyMap();

    final GenomeMapperIndexer indexer =
        new GenomeMapperIndexer(bowtie, "", additionnalArgument, 1);

    indexer.createIndex(genomeDataFile, this.desc, result);

    LOGGER.fine("FASTQSCREEN: create/retrieve index for "
        + genomeDataFile.getName() + " in "
        + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    timer.stop();

    return result.toFile();
  }

  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with reference genome.
   * @param value input of the mapper
   * @param output List of output of the mapper
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the mapper
   */
  @Override
  public void map(final String value, final List<String> output,
      final Reporter reporter) throws IOException {

    requireNonNull(output, "output argument cannot be null");

    if (value == null || value.length() == 0 || value.charAt(0) == '@') {
      return;
    }

    final String[] tokens = this.pattern.split(value, 3);
    String nameRead = null;

    // flag of SAM format are in case 2 and flag = 4 for read unmapped
    if (Integer.parseInt(tokens[1]) != 4) {
      nameRead = tokens[0];
    }

    if (nameRead == null) {
      return;
    }
    output.add(nameRead + "\t" + this.genomeReference);

  }

  /**
   * Reducer Receive for each read list mapped genome Values first character
   * represent the number of hits for a read : 1 or 2 (for several hits) and the
   * end represent the name of reference genome.
   * @param key input key of the reducer
   * @param values values for the key
   * @param output list of output values of the reducer : here not use
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the reducer
   */
  @Override
  public void reduce(final String key, final Iterator<String> values,
      final List<String> output, final Reporter reporter) throws IOException {

    requireNonNull(values, "values argument cannot be null");

    // Do not process null keys
    if (key == null) {
      return;
    }

    boolean oneHit = true;
    boolean oneGenome = true;
    String currentGenome = null;

    this.readsMapped++;

    // values format : a number 1 or 2 which represent the number of hits for
    // the read on one genome and after name of the genome
    while (values.hasNext()) {
      final String s = values.next();
      oneHit = s.charAt(0) == '1';

      currentGenome = s.substring(1);

      // set false if more on genome are done
      if (oneGenome && values.hasNext()) {
        oneGenome = false;
      }

      this.fastqScreenResult.countHitPerGenome(currentGenome, oneHit,
          oneGenome);
    }
  }

  /**
   * Compile data of fastqscreen in percentage.
   * @return FastqScreenResult result of FastqScreen or null if an error occurs
   *         during mapped: they are more readsmapped than reads processed
   */
  public FastqScreenResult getFastqScreenResult() throws AozanException {

    if (this.readsMapped > this.readsProcessed) {
      LOGGER.warning("FASTQSCREEN: mapped reads count ("
          + this.readsMapped + ") must been inferior to processed reads count ("
          + this.readsProcessed + ")");
      return null;
    }

    LOGGER.fine("FASTQSCREEN: result of mappings : nb read mapped "
        + this.readsMapped + " / nb read " + this.readsProcessed);

    this.fastqScreenResult.countPercentValue(this.readsMapped,
        this.readsProcessed);

    return this.fastqScreenResult;
  }

  /**
   * Return mapper arguments either the defaults or those specified in the
   * configuration.
   * @return mapper arguments
   * @throws AozanException for mapper different bowtie or bowtie2, no
   *           parameters are define
   */
  private static String getMapperArguments(final String mapperName,
      final String mapperArguments, final boolean pairedMode)
      throws AozanException {

    if (mapperArguments != null && !mapperArguments.isEmpty()) {
      return mapperArguments;
    }

    // Use default argument mapper
    final String mapperNameLower =
        mapperName.toLowerCase(Globals.DEFAULT_LOCALE);

    if (BowtieMapperProvider.MAPPER_NAME.toLowerCase(Globals.DEFAULT_LOCALE)
        .equals(mapperNameLower)) {
      return " -l 20 -k 2 --chunkmbs 512"
          + (pairedMode ? " --maxins 1000" : "");
    }

    if (Bowtie2MapperProvider.MAPPER_NAME.toLowerCase(Globals.DEFAULT_LOCALE)
        .equals(mapperNameLower)) {
      return " -k 2 --very-fast-local --no-discordant --no-mixed"
          + (pairedMode ? " --maxins 1000" : "");
    }

    throw new AozanException(
        "FastqScreen fail: no argument defined for the mapper " + mapperName);
  }

  //
  // Constructor
  //

  /**
   * Public construction. Instantiation the mapper, the mapper name and the
   * mapper arguments must be define together else it uses the default mapper.
   * @param tmpDir path to temporary directory
   * @param pairedMode true if a pair-end run and option paired mode equals true
   *          else false
   * @param mapperName mapper name name can be null
   * @param mapperArguments mapper arguments can be null
   * @throws AozanException occurs when the instantiation of mapper fails
   */
  public FastqScreenPseudoMapReduce(final File tmpDir, final boolean pairedMode,
      final String mapperName, final String mapperArguments)
      throws AozanException {

    requireNonNull(tmpDir, "tmpDir argument cannot be null");
    requireNonNull(mapperName, "mapperName argument cannot be null");

    checkArgument(tmpDir.isDirectory(),
        "temporary directory does not exists or is not a directory: " + tmpDir);

    this.pairedMode = pairedMode;
    this.tmpDir = tmpDir;

    // Use default mapper if mapper name or arguments is null
    if (mapperName == null || mapperName.isEmpty()) {
      this.mapperName = new BowtieMapperProvider().getName();
    } else {
      this.mapperName = mapperName;
    }

    this.mapperArguments =
        getMapperArguments(mapperName, mapperArguments, pairedMode);

    // Define temporary directory
    this.setMapReduceTemporaryDirectory(this.tmpDir);

    this.reporter = new LocalReporter();
    this.fastqScreenResult = new FastqScreenResult(this.mapperName);
  }
}
