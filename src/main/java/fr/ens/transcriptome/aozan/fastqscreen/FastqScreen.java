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

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Globals;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.LocalEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;

/**
 * This class execute fastqscreen pair-end mode or single-end
 * @author Sandrine Perrin
 */
public class FastqScreen {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  protected static final String COUNTER_GROUP = "fastqscreen";

  private static final String KEY_TMP_DIR = "tmp.dir";
  private static final String KEY_GENOMES_DESC_PATH =
      "qc.conf.settings.genomes.desc.path";
  private static final String KEY_MAPPERS_INDEXES_PATH =
      "qc.conf.settings.mappers.indexes.path";
  private static final String KEY_GENOMES_PATH = "qc.conf.settings.genomes";

  // map which does correspondance between genome of sample and reference genome
  private static final Map<String, String> aliasGenome =
      new HashMap<String, String>();

  // path for file alias genome
  private static final String KEY_ALIAS_GENOME_PATH =
      "qc.conf.genome.alias.path";

  private Properties properties;
  private String aliasGenomePath;
  private final List<String> genomes;

  private static final List<String> genomesForAozanTest =
      new ArrayList<String>();

  /**
   * Mode pair-end : execute fastqscreen calcul
   * @param fastqRead1 fastq file input for mapper
   * @param fastqRead2 fastq file input for mapper
   * @param listGenomes list or reference genome, used by mapper
   * @return FastqScreenResult object contains results for each reference genome
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead,
      final List<String> listGenomes, final String projectName,
      final String sampleName, final String genomeSample) throws AozanException {

    return this.execute(fastqRead, null, listGenomes, projectName, sampleName,
        genomeSample);

  }

  /**
   * Mode single-end : execute fastqscreen calcul
   * @param fastqFile fastq file input for mapper
   * @param listGenomes list or reference genome, used by mapper
   * @return FastqScreenResult object contains results for each reference genome
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead1,
      final File fastqRead2, final List<String> listGenomes,
      final String projectName, final String sampleName, String genomeSample)
      throws AozanException {

    final long startTime = System.currentTimeMillis();
    List<String> genomes = new ArrayList<String>();
    genomes.addAll(listGenomes);

    LOGGER.fine("Start fastqscreen on project "
        + projectName + " " + sampleName);

    String tmpDir = properties.getProperty(KEY_TMP_DIR);

    FastqScreenPseudoMapReduce pmr = new FastqScreenPseudoMapReduce();
    pmr.setMapReduceTemporaryDirectory(new File(tmpDir));

    // if genomeSample is present in mapAliasGenome, it add in list of genomes
    // reference for the mapping
    genomeSample = genomeSample.trim().toLowerCase();
    genomeSample = genomeSample.replace('"', '\0');

    if (aliasGenome.containsKey(genomeSample)) {

      String alias = aliasGenome.get(genomeSample);
      if (!genomes.contains(alias))
        genomes.add(alias);

      if (!genomesForAozanTest.contains(alias))
        genomesForAozanTest.add(alias);

      System.out.println("list generic for a run " + genomesForAozanTest);

    } else
      updateAliasGenomeFile(genomeSample);

    System.out.println(" genome of sample "
        + genomeSample + " is present " + aliasGenome.containsKey(genomeSample)
        + " alias " + aliasGenome.get(genomeSample));

    try {

      if (fastqRead2 == null)
        pmr.doMap(fastqRead1, genomes, properties);
      else
        pmr.doMap(fastqRead1, fastqRead2, genomes, properties);

      pmr.doReduce(new File(tmpDir + "/outputDoReduce.txt"));

      // remove temporary output file use in map-reduce step
      new File(tmpDir + "/outputDoReduce.txt").delete();

    } catch (IOException e) {
      throw new AozanException(e.getMessage());

    } catch (BadBioEntryException bad) {
      throw new AozanException(bad.getMessage());

    }

    LOGGER.fine("End fastqscreen on project "
        + projectName + " " + sampleName + " for genome(s) "
        + listGenomes.toString() + " in mode "
        + (fastqRead2 == null ? "single " : "paired ") + " in "
        + toTimeHumanReadable(System.currentTimeMillis() - startTime));

    return pmr.getFastqScreenResult();
  }

  /**
   * Create a map which does correspondence between genome of sample and
   * reference genome from a file, the path is in aozan.conf
   */
  private void createMapAliasGenome() {
    try {
      System.out.println("path file " + this.aliasGenomePath);

      if (this.aliasGenomePath != null) {

        final BufferedReader br =
            new BufferedReader(new FileReader(new File(this.aliasGenomePath)));
        String line = null;

        while ((line = br.readLine()) != null) {

          final int pos = line.indexOf('=');
          if (pos == -1)
            continue;

          final String key = line.substring(0, pos);
          final String value = line.substring(pos + 1);

          // Retrieve only genomes identified in Aozan
          if (value.length() > 0)
            aliasGenome.put(key, value);
        }
        br.close();
      }
    } catch (IOException io) {
    }

    System.out.println("map alias " + aliasGenome);
  }

  /**
   * Add the genome of the sample in the file which does correspondence with
   * reference genome
   * @param genomeSample name genome
   */
  private void updateAliasGenomeFile(final String genomeSample) {

    try {
      if (this.aliasGenomePath != null) {

        final FileWriter fw = new FileWriter(this.aliasGenomePath, true);
        fw.write(genomeSample + "=\n");
        fw.flush();
        fw.close();
      }
    } catch (IOException io) {
      System.out.println(io.getMessage());
    }
  }

  //
  // Getter
  //

  public static List<String> getListGenomeReferenceSample() {
    return genomesForAozanTest;
  }

  //
  // Constructor
  //

  /**
   * Public constructor of fastqscreen. Initialization of settings of Eoulsan
   * necessary for use of mapper index.
   * @param properties properties defines in configuration of aozan
   */
  public FastqScreen(final Properties properties, final List<String> genomes) {

    this.properties = properties;
    this.aliasGenomePath = properties.getProperty(KEY_ALIAS_GENOME_PATH);
    this.genomes = genomes;

    genomesForAozanTest.addAll(genomes);

    createMapAliasGenome();

    try {
      // init EoulsanRuntime, it is necessary to use the implementation of
      // bowtie in Eoulsan
      LocalEoulsanRuntime.initEoulsanRuntimeForExternalApp();
      Settings settings = EoulsanRuntime.getSettings();

      settings.setGenomeDescStoragePath(properties
          .getProperty(KEY_GENOMES_DESC_PATH));
      settings.setGenomeMapperIndexStoragePath(properties
          .getProperty(KEY_MAPPERS_INDEXES_PATH));
      settings.setGenomeStoragePath(properties.getProperty(KEY_GENOMES_PATH));

    } catch (IOException e) {
      e.printStackTrace();

    } catch (EoulsanException ee) {
      ee.printStackTrace();
    }

  }
}