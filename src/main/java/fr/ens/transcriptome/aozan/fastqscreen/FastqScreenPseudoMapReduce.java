/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.AbstractBowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.BowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.generators.GenomeDescriptionCreator;
import fr.ens.transcriptome.eoulsan.steps.generators.GenomeMapperIndexer;
import fr.ens.transcriptome.eoulsan.util.PseudoMapReduce;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class FastqScreenPseudoMapReduce extends PseudoMapReduce {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  protected static final String COUNTER_GROUP = "reads_mapping";

  private final int NB_STAT_VALUES = 5;
  private final AbstractBowtieReadsMapper bowtie;
  private final Reporter reporter;
  private final FastqScreenResult fastqScreenResult;

  private Map<String, float[]> percentHitsPerGenome =
      new HashMap<String, float[]>();
  private int readsprocessed;
  private int readsMapped = 0;

  private String genomeReference;
  private boolean succesMapping = false;

  private Pattern pattern = Pattern.compile("\t");

  // TODO : override method doMap() of PseudoMapReduce
  /**
   * @param readsFile fastq file
   * @param listGenomes
   * @throws IOException
   * @throws EoulsanException
   * @throws BadBioEntryException
   */
  public void doMap(File readsFile, List<String> listGenomes)
      throws AozanException, BadBioEntryException {

    final int mapperThreads = Runtime.getRuntime().availableProcessors();

    // Mapper change defaults arguments
    // seed use by bowtie already define to 40
    final String newArgumentsMapper = "-l 40 --chunkmbs 512 ";
    for (String genome : listGenomes) {

      try {
        DataFile genomeFile = new DataFile("genome://" + genome);

        // test if index Genome reference exists
        File archiveIndexFile = createIndex(bowtie, genomeFile);

        FastsqScreenSAMParser parser =
            new FastsqScreenSAMParser(this.getMapOutputTempFile(), genome);

        this.setGenomeReference(genome);

        // System.out.println(new SimpleDateFormat("h:m a").format(new Date())
        // + " name genome : " + nameGenome + " path "
        // + genome.getAbsolutePath());

        bowtie.setThreadsNumber(mapperThreads);
        bowtie.setMapperArguments(newArgumentsMapper);

        final File indexDir =
            new File(StringUtils.filenameWithoutExtension(archiveIndexFile
                .getPath()));

        bowtie.init(false, FastqFormat.FASTQ_SANGER, archiveIndexFile,
            indexDir, reporter, COUNTER_GROUP);

        bowtie.map(readsFile, parser);
        this.readsprocessed = parser.getReadsprocessed();
        succesMapping = (readsprocessed > 0);

        parser.closeMapOutpoutFile();

      } catch (IOException e) {
        e.printStackTrace();
        throw new AozanException(e.getMessage());
      }

    } // for
  } // doMap

  private File createIndex(AbstractBowtieReadsMapper bowtie,
      DataFile genomeDataFile) throws BadBioEntryException, IOException {

    final DataFile result =
        new DataFile("/tmp/aozan-bowtie-index-"
            + genomeDataFile.getName() + ".zip");

    // Create genome description
    GenomeDescriptionCreator descCreator = new GenomeDescriptionCreator();

    GenomeDescription desc =
        descCreator.createGenomeDescription(genomeDataFile);

    GenomeMapperIndexer indexer = new GenomeMapperIndexer(bowtie);
    indexer.createIndex(genomeDataFile, desc, result);

    return result.toFile();
  }

  @Override
  /**
   * Mapper Receive value in SAM format, only the read mapped are added in
   * output with genome reference used
   * @param value input of the mapper
   * @param output List of output of the mapper
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the mapper
   */
  public void map(final String value, final List<String> output,
      final Reporter reporter) throws IOException {

    if (value == null || value.length() == 0 || value.charAt(0) == '@')
      return;

    succesMapping = true;
    String[] tokens = pattern.split(value, 3);
    String nameRead = null;

    // flag of SAM format are in case 2 and flag = 4 for read unmapped
    if (Integer.parseInt(tokens[1]) != 4)
      nameRead = tokens[0];

    if (nameRead == null)
      return;
    output.add(nameRead + "\t" + genomeReference);
    // System.out.println("output map " + output);

  }// map

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
  public void reduce(final String key, Iterator<String> values,
      final List<String> output, final Reporter reporter) throws IOException {

    boolean oneHit = true;
    boolean oneGenome = true;
    String currentGenome = null;
    String nextGenome = null;
    readsMapped++;

    // System.out.println("reduce : value of key "+ key);

    // values format : a number 1 or 2 which represent the number of hits for
    // the read on one genome and after name of the genome
    while (values.hasNext()) {
      String s = values.next();
      oneHit = s.charAt(0) == '1' ? true : false;
      nextGenome = s.substring(1);
      // System.out.println("read"+key+"\t genome "+genome+"\t onegenome "+oneGenome+"\t hits \t"+oneHit);

      this.fastqScreenResult.countHitPerGenome(nextGenome, oneHit, oneGenome);
      oneGenome = !nextGenome.equals(currentGenome);
      currentGenome = nextGenome;
    }// while

  } // reduce

  public FastqScreenResult getFastqScreenResult() {
    return this.fastqScreenResult.getFastqScreenResult();
  }

  //
  // SETTERS
  //

  public void setGenomeReference(String genome) {
    this.genomeReference = genome;

    // update list genomeReference : create a new entry for the new reference
    // genome
    percentHitsPerGenome.put(genome, new float[NB_STAT_VALUES]);
  }

  //
  // CONSTRUCTOR
  //

  public FastqScreenPseudoMapReduce() {
    this.bowtie = new BowtieReadsMapper();
    this.reporter = new Reporter();
    this.fastqScreenResult = new FastqScreenResult();
  }

  public FastqScreenPseudoMapReduce(int readsprocessed) {
    this.bowtie = new BowtieReadsMapper();
    this.reporter = new Reporter();
    this.fastqScreenResult = new FastqScreenResult();
    this.readsprocessed = readsprocessed;
  }
}
