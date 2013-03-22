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

package fr.ens.transcriptome.aozan.collectors;

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

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreen;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * This class manages the execution of Fastq Screen for a full run according to
 * the properties defined in the configuration file Aozan, which define the list
 * of references genomes. Each sample are mapped on list of references genomes
 * and the genome of sample if it is available for Aozan.
 * @author Sandrine Perrin
 */
public class FastqScreenCollector extends AbstractFastqCollector {

  public static final String COLLECTOR_NAME = "fastqscreen";
  public static final String KEY_GENOMES = "qc.conf.fastqscreen.genomes";

  private FastqScreen fastqscreen;

  // path for file alias genome which which does correspondance between genome
  // of sample and reference genome
  private String aliasGenomePath;
  // Key in rundata for retrieve the path of alias file
  private static final String KEY_ALIAS_GENOME_PATH =
      "qc.conf.genome.alias.path";

  // Correspondance between genome name in casava design file and the reference
  // genome, if it exists.
  private static Map<String, String> aliasGenomes;
  // List with all reference genome identified for a run used by the Aozan test
  // for the qc report in html
  private static List<String> genomesToAozanTest;
  // Genome name to add in alias file
  private static List<String> genomesToAddInAliasGenomesFile;
  // List of genome for fastqscreen specific of a sample
  private static List<String> genomesToFastqscreenSample;

  @Override
  public String getName() {
    return COLLECTOR_NAME;
  }

  /**
   * Collectors to execute before fastqscreen Collector
   * @return list of names collector
   */
  @Override
  public String[] getCollectorsNamesRequiered() {

    List<String> result =
        Lists.newArrayList(super.getCollectorsNamesRequiered());
    result.add(UncompressFastqCollector.COLLECTOR_NAME);

    return result.toArray(new String[] {});

  }

  /**
   * Configure fastqScreen with properties from file aozan.conf.
   * @param properties
   */
  @Override
  public void configure(final Properties properties) {

    super.configure(properties);

    this.fastqscreen = new FastqScreen(properties);
    this.aliasGenomePath = properties.getProperty(KEY_ALIAS_GENOME_PATH);

    // Retrieve contents of alias file
    createMapAliasGenome();

    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();

    // Set list of reference genome for fastqscreen and aozan test
    for (String g : s.split(properties.getProperty(KEY_GENOMES))) {
      genomesToFastqscreenSample.add(g);
      genomesToAozanTest.add(g);
    }

    // List genomesSample contains genomes for all samples to treate
    // for each, retrieve the reference genome corresponding
    for (String genomeSample : genomesSample) {

      String alias = aliasGenomes.get(genomeSample);
      if (aliasGenomes.containsKey(genomeSample)) {

        if (alias.length() > 0) {
          if (!genomesToAozanTest.contains(alias)) {
            genomesToAozanTest.add(alias);
          }
        }

      } else {
        // add new genomes in list for update alias genomes file
        if (!genomesToAddInAliasGenomesFile.contains(genomeSample)) {
          genomesToAddInAliasGenomesFile.add(genomeSample);
        }
      }

    }

    // update alias genomes file
    updateAliasGenomeFile();
  }

  public AbstractFastqProcessThread collectSample(RunData data,
      final FastqSample fastqSample, final File reportDir)
      throws AozanException {

    // TODO fix after test : throw an AozanException
    if (fastqSample.getFastqFiles() == null
        || fastqSample.getFastqFiles().isEmpty()) {
      return null;
    }

    // Create the thread object only if the fastq sample correspond to a R1
    if (fastqSample.getRead() == 2)
      return null;

    // Retrieve genome sample
    String genomeSample =
        data.get("design.lane"
            + fastqSample.getLane() + "." + fastqSample.getSampleName()
            + ".sample.ref");

    // if genomeSample is present in mapAliasGenome, it add in list of
    // genomes reference for the mapping
    genomeSample = genomeSample.trim().toLowerCase();
    genomeSample = genomeSample.replace('"', '\0');
    String aliasGenome = "";

    if (aliasGenomes.containsKey(genomeSample)) {

      aliasGenome = aliasGenomes.get(genomeSample);
      if (aliasGenome.length() > 0) {
        if (!genomesToFastqscreenSample.contains(aliasGenome))
          genomesToFastqscreenSample.add(aliasGenome);
      }
    }

    if (paired) {
      // in mode paired FastqScreen should be launched with R1 and R2 together.

      // Search fasqtSample which correspond to fastqSample R1
      String prefixRead2 = fastqSample.getPrefixRead2();

      for (FastqSample fs : fastqSamples) {
        if (fs.getKeyFastqSample().equals(prefixRead2)) {

          return new FastqScreenProcessThread(fastqSample, fs, fastqscreen,
              genomesToFastqscreenSample, aliasGenome, reportDir, paired);
        }
      }
    }
    return new FastqScreenProcessThread(fastqSample, fastqscreen,
        genomesToFastqscreenSample, aliasGenome, reportDir, paired);
  }

  /**
   * Create a map which does correspondence between genome of sample and
   * reference genome from a file, the path is in aozan.conf
   */
  private void createMapAliasGenome() {
    try {

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
          // if (value.length() > 0)
          aliasGenomes.put(key, value);
        }
        br.close();
      }

    } catch (IOException io) {
    }

  }

  /**
   * Add the genome of the sample in the file which does correspondence with
   * reference genome
   */
  private void updateAliasGenomeFile() {

    if (genomesToAddInAliasGenomesFile.isEmpty())
      return;

    try {
      if (this.aliasGenomePath != null) {

        final FileWriter fw = new FileWriter(this.aliasGenomePath, true);

        for (String genomeSample : genomesToAddInAliasGenomesFile)
          fw.write(genomeSample + "=\n");

        fw.flush();
        fw.close();
      }
    } catch (IOException io) {
      System.out.println(io.getMessage());
    }
  }

  //
  // Getters & Setters
  //

  /**
   * Get the list with reference genome and all genome from sample used for
   * fastqscreeen
   * @return list genome name
   */
  public static List<String> getGenomesReferenceSample() {
    return genomesToAozanTest;
  }

  /**
   * Get map with correspondance between genome name et genome reference
   * @return map genome
   */
  public static Map<String, String> getAliasGenomes() {
    return aliasGenomes;
  }

  @Override
  /**
   * Get number of thread
   * @return number of thread
   */
  public int getThreadsNumber() {
    return 1;
  }

  //
  // Constructor
  //

  /**
   * Public constructor for FastqScreenCollector
   */
  public FastqScreenCollector() {
    aliasGenomes = new HashMap<String, String>();
    genomesToFastqscreenSample = new ArrayList<String>();
    genomesToAozanTest = new ArrayList<String>();
    genomesToAddInAliasGenomesFile = new ArrayList<String>();
  }

}
