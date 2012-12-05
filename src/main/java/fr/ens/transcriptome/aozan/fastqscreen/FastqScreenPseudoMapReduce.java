/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.math.DoubleMath;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.AbstractBowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.BowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.generators.GenomeMapperIndexer;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep;
import fr.ens.transcriptome.eoulsan.util.PseudoMapReduce;
import fr.ens.transcriptome.eoulsan.util.Reporter;

public class FastqScreenPseudoMapReduce extends PseudoMapReduce {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  protected static final String COUNTER_GROUP = "reads_mapping";

  private final int NB_STAT_VALUES = 5;
  private final AbstractBowtieReadsMapper bowtie;;
  private final Reporter reporter;

  private Map<String, float[]> percentHitsPerGenome =
      new HashMap<String, float[]>();
  private float readsprocessed;

  private final String[] headerColumns = {"library", "unmapped",
      "one_hit_one_library", "multiple_hits_one_library",
      "one_hit_multiple_libraries", "multiple_hits_multiple_libraries"};

  private String genomeReference;
  private float percentHitNoLibraries = 0.f;
  private boolean succesMapping = false;
  private boolean tableStatisticExist = false;
  private int nbReadMapped = 0;

  private Pattern pattern = Pattern.compile("\t");

  // TODO : override method doMap() of PseudoMapReduce
  /**
   * @param readsFile fastq file
   * @param listGenome
   * @throws IOException
   * @throws EoulsanException
   * @throws BadBioEntryException
   */
  public void doMap(File readsFile, List<File> listGenome) throws IOException,
      EoulsanException, BadBioEntryException {

    final int mapperThreads = Runtime.getRuntime().availableProcessors();

    // Mapper change defaults arguments
    // seed use by bowtie already define to 40
    final String newArgumentsMapper = "-l 40 --chunkmbs 512 ";

    for (File genome : listGenome) {

      // test if index Genome reference exists
      //createIndex(bowtie, genome);
      String nameGenome = genome.getParentFile().getName();
      
      FastsqScreenSAMParser parser =
          new FastsqScreenSAMParser(this.getMapOutputTempFile(),
              nameGenome);
      this.setGenomeReference(nameGenome);

      System.out.println(new SimpleDateFormat("h:m a").format(new Date())
          + " nom genome : " + nameGenome + " chemin genome "
          + genome.getAbsolutePath());

      bowtie.setThreadsNumber(mapperThreads);
      bowtie.setMapperArguments(newArgumentsMapper);

      bowtie.init(false, FastqFormat.FASTQ_SANGER, genome,
          genome.getParentFile(), reporter, COUNTER_GROUP);
      bowtie.map(readsFile, parser);

      this.readsprocessed = (float) parser.getReadsprocessed();
      succesMapping = (readsprocessed > 0);

      parser.cleanup();

    } // for
  } // doMap

  private void createIndex(AbstractBowtieReadsMapper bowtie, File genome)
      throws BadBioEntryException, IOException {

    // ===== src mapFilteredFastq() in ReadsLaneStatsGenerator
    // File genomeName = genome[0].getAbsolutePath();

    System.out.println("in method createIndex");

    String nameGenome =
        "/home/sperrin/Documents/FastqScreenTest/index/saccharomyces";
    final File archiveFile = new File(nameGenome + ".zip");
    final File archiveDir =
        new File("/home/sperrin/Documents/FastqScreenTest/indexs");

    if (!archiveDir.exists()) {
      // Get the genome file
      final DataFile genomeFile = new DataFile("genome:/" + nameGenome);

      System.out.println("index create");

      // Create genome description
      final GenomeDescription desc =
          GenomeDescription.createGenomeDescFromFasta(genomeFile.open(),
              nameGenome + ".fasta");

      GenomeMapperIndexer indexer = new GenomeMapperIndexer(bowtie);
      indexer.createIndex(genomeFile, desc, new DataFile(archiveFile));

      // bowtie.makeArchiveIndex(genomeFile, archiveFile);
    }

  }

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
   * represent the number of hits for a read : 1 or 2 (for several hits) the end
   * represent the name of referenced genome
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
    nbReadMapped++;

    // System.out.println("reduce : value of key "+ key);

    // values format : a number 1 or 2 which represent the number of hits for the read on one genome
    // and name of the genome
    while (values.hasNext()) {
      String s = values.next();
      oneHit = s.charAt(0) == '1' ? true : false;
      nextGenome = s.substring(1);
      // System.out.println("read"+key+"\t genome "+genome+"\t onegenome "+oneGenome+"\t hits \t"+oneHit);

      countHitPerGenome(nextGenome, oneHit, oneGenome);
      oneGenome = ! nextGenome.equals(currentGenome);
      currentGenome = nextGenome;
    }// while

  } // reduce

  /**
   * Called by method reduce for each read and filled intermediate table
   * @param genome
   * @param oneHit
   * @param oneGenome
   */
  void countHitPerGenome(String genome, boolean oneHit, boolean oneGenome) {
    // indices for table tabHitsPerLibraries
    // position 0 of the table for UNMAPPED ;

    // System.out.println("genome : "
    // + genome + " hit " + oneHit + " gen " + oneGenome);

    final int ONE_HIT_ONE_LIBRARY = 1;
    final int MULTIPLE_HITS_ONE_LIBRARY = 2;
    final int ONE_HIT_MULTIPLE_LIBRARIES = 3;
    final int MUTILPLE_HITS_MULTIPLE_LIBRARIES = 4;
    float[] tab;
    // genome must be contained in map
    if (!(percentHitsPerGenome.containsKey(genome)))
      return;

    if (oneHit && oneGenome) {
      tab = percentHitsPerGenome.get(genome);
      tab[ONE_HIT_ONE_LIBRARY] += 1.0;

    } else if (!oneHit && oneGenome) {
      tab = percentHitsPerGenome.get(genome);
      tab[MULTIPLE_HITS_ONE_LIBRARY] += 1.0;

    } else if (oneHit && !oneGenome) {
      tab = percentHitsPerGenome.get(genome);
      tab[ONE_HIT_MULTIPLE_LIBRARIES] += 1.0;

    } else if (!oneHit && !oneGenome) {
      tab = percentHitsPerGenome.get(genome);
      tab[MUTILPLE_HITS_MULTIPLE_LIBRARIES] += 1.0;
    }
  }// countHitPerGenome

  /**
   * calculating as a percentage, without rounding
   */
  public void createStatisticalTable() {
    System.out.println("nb mapped "
        + nbReadMapped + " nb read " + readsprocessed);

    if (nbReadMapped > readsprocessed)
      return;

    tableStatisticExist = true;

    for (Map.Entry<String, float[]> e : percentHitsPerGenome.entrySet()) {
      float unmapped = 100.f;
      float[] tab = e.getValue();

      for (int i = 1; i < tab.length; i++) {
        float n = tab[i] * 100.f / readsprocessed;
        tab[i] = n;
        unmapped -= n;
        // System.out.println("genome "
        // + e.getKey() + " i : " + i + " val : " + n + " unmap " +
        // unmapped);
      }
      tab[0] = unmapped;
    }
  }

  public Map<String, float[]> getStatisticalTable() {
    return this.percentHitsPerGenome;
  }

  public String[] getHeaderColumns() {
    return this.headerColumns;
  }

  /**
   * print table percent in format use by fastqscreen program
   * @return
   */
  public String statisticalTableToString() {

    if (!succesMapping) {
      return "ERROR mapping : no value receive ! (in method statisticalTableToString)";
    }

    if (!tableStatisticExist)
      createStatisticalTable();

    StringBuilder s =
        new StringBuilder(
            "Library \t %Unmapped \t %One_hit_one_library"
                + "\t %Multiple_hits_one_library \t %One_hit_multiple_libraries \t "
                + "%Multiple_hits_multiple_libraries");

    percentHitNoLibraries =
        (readsprocessed - nbReadMapped) / readsprocessed * 100.f;
    percentHitNoLibraries = ((int) (percentHitNoLibraries * 100)) / 100.f;
    for (Map.Entry<String, float[]> e : percentHitsPerGenome.entrySet()) {
      float[] tab = e.getValue();
      s.append("\n" + e.getKey());

      for (float n : tab) {
        // n = ((int) (n * 100.0)) / 100.0;
        // n = Math.ceil(n);
        n = DoubleMath.roundToInt((n * 100.f), RoundingMode.HALF_DOWN) / 100.f;
        s.append("\t" + n);
      }
    }

    s.append("\n\n% Hit_no_libraries : " + percentHitNoLibraries + "\n");
    return s.toString();
  }

  //
  // GETTERS
  //

  public float getReadsprocessed() {
    return (float) this.readsprocessed;
  }

  public float getPercentHitNoLibraries() {
    return this.percentHitNoLibraries;
  }

  //
  // SETTERS
  //

  public void setGenomeReference(String genome) {
    this.genomeReference = genome;

    // update list genomeReference
    percentHitsPerGenome.put(genome, new float[NB_STAT_VALUES]);
  }

  //
  // CONSTRUCTOR
  //

  public FastqScreenPseudoMapReduce() {
    this.bowtie = new BowtieReadsMapper();
    this.reporter = new Reporter();
  }

  public FastqScreenPseudoMapReduce(int readsprocessed) {
    this.bowtie = new BowtieReadsMapper();
    this.reporter = new Reporter();

    this.readsprocessed = (float) readsprocessed;
  }
}
