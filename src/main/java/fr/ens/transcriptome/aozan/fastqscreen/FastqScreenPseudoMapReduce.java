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
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.fastqscreen;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.Bowtie2ReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.BowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.generators.GenomeMapperIndexer;
import fr.ens.transcriptome.eoulsan.util.LocalReporter;
import fr.ens.transcriptome.eoulsan.util.PseudoMapReduce;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class account reads that map to each of the reference genome.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenPseudoMapReduce extends PseudoMapReduce {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  private static final String COUNTER_GROUP = "reads_mapping";
  private final Reporter reporter;

  private int mapperThreads = Runtime.getRuntime().availableProcessors();
  private final SequenceReadsMapper mapper;
  private GenomeDescription desc = null;
  private FastqScreenResult fastqScreenResult;
  private final String tmpDir;

  private String newArgumentsMapper;
  private final Pattern pattern = Pattern.compile("\t");

  private int readsprocessed = 0;
  private int readsmapped = 0;
  private final boolean pairedMode;
  private String genomeReference;

  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with reference genome.
   * @param fastqRead fastq file
   * @param genomesForMapping list of reference genome
   * @param genomeSample genome reference corresponding to sample, can be null
   * @param numberThreads number threads used for mapping
   * @throws AozanException if an error occurs while mapping
   */
  public void doMap(final File fastqRead, final List<String> genomesForMapping,
      final String genomeSample, final int numberThreads) throws AozanException {

    this.doMap(fastqRead, null, genomesForMapping, genomeSample, numberThreads);
  }

  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with reference genome
   * @param fastqRead1 fastq file
   * @param fastqRead2 fastq file in mode paired
   * @param genomesForMapping list of genome reference
   * @param genomeSample genome reference corresponding to sample, can be null
   * @param numberThreads number threads used for mapping
   * @throws AozanException if an error occurs while mapping
   */
  public void doMap(final File fastqRead1, final File fastqRead2,
      final List<String> genomesForMapping, final String genomeSample,
      final int numberThreads) throws AozanException {

    if (numberThreads > 0)
      mapperThreads = numberThreads;

    LOGGER.info("FASTQSCREEN mapping sample "
        + fastqRead1.getName() + " on genomes "
        + Joiner.on(",").join(genomesForMapping));

    for (String genome : genomesForMapping) {
      // Timer : for step mapping on genome
      final Stopwatch timer = Stopwatch.createStarted();

      try {
        DataFile genomeFile = new DataFile("genome://" + genome);

        // get index Genome reference exists
        File archiveIndexFile = createIndex(mapper, genomeFile);

        if (archiveIndexFile == null) {
          LOGGER.warning("FASTQSCREEN : archive index file not found for "
              + genome);
          continue;
        }

        FastqScreenSAMParser parser =
            new FastqScreenSAMParser(this.getMapOutputTempFile(), genome,
                this.pairedMode, desc);

        this.setGenomeReference(genome, genomeSample);

        final File indexDir =
            new File(StringUtils.filenameWithoutExtension(archiveIndexFile
                .getPath()));

        // remove default argument
        mapper.setMapperArguments("");

        mapper.init(archiveIndexFile, indexDir, reporter, COUNTER_GROUP);

        // define new argument
        mapper.setMapperArguments(getMapperArguments());
        mapper.setThreadsNumber(mapperThreads);

        if (this.pairedMode) {
          // mode pair-end
          InputStream outputSAM = mapper.mapPE(fastqRead1, fastqRead2, desc);
          parser.parseLine(outputSAM);

          parser.closeMapOutputFile();
          this.readsprocessed = parser.getReadsprocessed();

        } else {

          if (desc == null)
            throw new AozanException(
                "Fastqscreen : genome description is null for bowtie");

          InputStream outputSAM = mapper.mapSE(fastqRead1, desc);
          parser.parseLine(outputSAM);

          parser.closeMapOutputFile();
          this.readsprocessed = parser.getReadsprocessed();
        }

        LOGGER.fine("FASTQSCREEN : "
            + mapper.getMapperName() + " mapping on genome " + genome
            + " in mode " + (this.pairedMode ? "paired" : "single") + ", in "
            + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

        timer.stop();

      } catch (IOException e) {
        throw new AozanException(e);
      }
    }
  }

  /**
   * Create a index with bowtie from the fasta file genome
   * @param bowtie mapper
   * @param genomeDataFile fasta file of genome
   * @return File file of genome index
   * @throws IOException if an error occurs while using file index genome
   * @throws AozanException if an error occurs during call
   *           FastqScreenGenomeMapper instance.
   */
  private File createIndex(final SequenceReadsMapper bowtie,
      final DataFile genomeDataFile) throws IOException, AozanException {

    // Timer :
    final Stopwatch timer = Stopwatch.createStarted();

    final DataFile result =
        new DataFile(this.tmpDir
            + "/aozan-" + bowtie.getMapperName().toLowerCase() + "-index-"
            + genomeDataFile.getName() + ".zip");

    // Create genome description
    try {
      this.desc =
          FastqScreenGenomeMapper.getInstance().createGenomeDescription(
              genomeDataFile);
    } catch (BadBioEntryException e) {
      throw new AozanException(e);
    }

    if (desc == null)
      return null;

    GenomeMapperIndexer indexer = new GenomeMapperIndexer(bowtie);
    indexer.createIndex(genomeDataFile, desc, result);

    LOGGER.info("FASTQSCREEN : create/retrieve index for "
        + genomeDataFile.getName() + " in "
        + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    timer.stop();

    return result.toFile();
  }

  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with reference genome
   * @param value input of the mapper
   * @param output List of output of the mapper
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the mapper
   */
  @Override
  public void map(final String value, final List<String> output,
      final Reporter reporter) throws IOException {

    if (value == null || value.length() == 0 || value.charAt(0) == '@')
      return;

    String[] tokens = pattern.split(value, 3);
    String nameRead = null;

    // flag of SAM format are in case 2 and flag = 4 for read unmapped
    if (Integer.parseInt(tokens[1]) != 4)
      nameRead = tokens[0];

    if (nameRead == null)
      return;
    output.add(nameRead + "\t" + genomeReference);

  }

  /**
   * Reducer Receive for each read list mapped genome Values first character
   * represent the number of hits for a read : 1 or 2 (for several hits) and the
   * end represent the name of reference genome
   * @param key input key of the reducer
   * @param values values for the key
   * @param output list of output values of the reducer : here not use
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the reducer
   */
  public void reduce(final String key, final Iterator<String> values,
      final List<String> output, final Reporter reporter) throws IOException {

    boolean oneHit = true;
    boolean oneGenome = true;
    String currentGenome = null;

    this.readsmapped++;

    // values format : a number 1 or 2 which represent the number of hits for
    // the read on one genome and after name of the genome
    while (values.hasNext()) {
      String s = values.next();
      oneHit = s.charAt(0) == '1';

      currentGenome = s.substring(1);

      // set false if more on genome are done
      if (oneGenome && values.hasNext())
        oneGenome = false;

      fastqScreenResult.countHitPerGenome(currentGenome, oneHit, oneGenome);
    }
  }

  /**
   * Define the genome reference for mapping
   * @param genome name of the new genome
   * @param genomeSample genome reference corresponding to sample
   */
  public void setGenomeReference(final String genome, final String genomeSample) {
    this.genomeReference = genome;

    fastqScreenResult.addGenome(genome, genomeSample);
  }

  /**
   * Compile data of fastqscreen in percentage
   * @return FastqScreenResult result of FastqScreen or null if an error occurs
   *         during mapped: they are more readsmapped than reads processed
   */
  public FastqScreenResult getFastqScreenResult() throws AozanException {

    if (readsmapped > readsprocessed) {
      LOGGER.warning("FASTQSCREEN :  mapped reads count "
          + readsmapped + " must been inferior to processed reads count"
          + readsprocessed);
      return null;
    }
    LOGGER.fine("FASTQSCREEN : result of mappings : nb read mapped "
        + readsmapped + " / nb read " + readsprocessed);

    fastqScreenResult.countPercentValue(readsmapped, readsprocessed);

    return fastqScreenResult;
  }

  /**
   * Return mapper arguments either the defaults or those specified in the
   * configuration
   * @return mapper arguments
   * @throws AozanException for mapper different bowtie or bowtie2, no
   *           parameters are define
   */
  private String getMapperArguments() throws AozanException {

    if (this.newArgumentsMapper == null
        || this.newArgumentsMapper.length() == 0)

      if (mapper.getMapperName()
          .equals(new BowtieReadsMapper().getMapperName())) {

        // Parameter for Bowtie
        return " -l 20 -k 2 --chunkmbs 512"
            + (this.pairedMode ? " --maxins 1000" : "");

      } else if (mapper.getMapperName().equals(
          new Bowtie2ReadsMapper().getMapperName())) {

        // Parameter for Bowtie2
        return " -k 2 --very-fast-local --no-discordant --no-mixed"
            + (this.pairedMode ? " --maxins 1000" : "");
      } else {
        // No parameter define for mapper
        throw new AozanException(
            "FastqScreen fail: no argument defined to the mapper "
                + mapper.getMapperName()
                + ". Only bowtie or bowtie2 are default parameters.");
      }

    // Return parameters setting in configuration Aozan file
    return this.newArgumentsMapper;

  }

  /**
   * Instantiation the mapper used for fastqscreen, if it is not define the
   * mapper per default is called
   * @param mapperName mapper name
   * @return instance of mapper
   * @throws AozanException occurs when the instantiation of mapper fails or if
   *           the mapper name doesn't recognize
   */
  private SequenceReadsMapper createInstanceMapper(final String mapperName,
      final String mapperArguments) throws AozanException {

    // Use default mapper if mapper name or arguments is null
    if (mapperName == null || mapperName.length() == 0) {

      if (mapperArguments != null && mapperArguments.length() > 0)
        this.newArgumentsMapper = mapperArguments;

      return new BowtieReadsMapper();
    }

    // Retrieve instance of mapper or null
    SequenceReadsMapper map =
        SequenceReadsMapperService.getInstance().newService(mapperName);

    if (map == null)
      throw new AozanException("FASTQSCREEN : mapper name "
          + mapperName + " from configuration Aozan doesn't recognized.");

    this.newArgumentsMapper = mapperArguments;
    return map;
  }

  //
  // Constructor
  //

  /**
   * Public construction. Instantiation the mapper, the mapper name and the
   * mapper arguments must be define together else it uses the default mapper
   * @param tmpDir path to temporary directory
   * @param pairedMode true if a pair-end run and option paired mode equals true
   *          else false
   * @param mapperName mapper name name can be null
   * @param mapperArguments mapper arguments can be null
   * @throws AozanException occurs when the instantiation of mapper fails
   */
  public FastqScreenPseudoMapReduce(final String tmpDir,
      final boolean pairedMode, final String mapperName,
      final String mapperArguments) throws AozanException {

    this.pairedMode = pairedMode;
    this.tmpDir = tmpDir;

    this.mapper = createInstanceMapper(mapperName, mapperArguments);

    // Define temporary directory
    this.setMapReduceTemporaryDirectory(new File(this.tmpDir));

    this.reporter = new LocalReporter();
    this.fastqScreenResult = new FastqScreenResult();

    LOGGER.info("FASTQSCREEN : init  mapper "
        + this.mapper.getMapperName() + ", arguments \"" + getMapperArguments()
        + "\" in mode " + (pairedMode ? "paired" : "single"));
  }
}
