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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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

  // List of genome for fastqscreen specific of a sample
  private static List<String> genomesConfiguration;

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
    result.add(FastQCCollector.COLLECTOR_NAME);
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

    final Splitter s = Splitter.on(',').trimResults().omitEmptyStrings();

    // Set list of reference genome for fastqscreen
    for (String g : s.split(properties.getProperty(KEY_GENOMES))) {
      genomesConfiguration.add(g);
    }

  }

  @Override
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

    String genomeReferenceSample =
        AliasGenomeFile.getGenomeReferenceCorresponding(genomeSample);

    if (paired) {
      // in mode paired FastqScreen should be launched with R1 and R2 together.
      // Search fasqtSample which correspond to fastqSample R1
      String prefixRead2 = fastqSample.getPrefixRead2();

      for (FastqSample fastqSampleR2 : fastqSamples) {
        if (fastqSampleR2.getKeyFastqSample().equals(prefixRead2)) {

          return new FastqScreenProcessThread(fastqSample, fastqSampleR2,
              fastqscreen, genomesConfiguration, genomeReferenceSample,
              reportDir, paired);
        }
      }
    }

    // Call in mode single-end
    return new FastqScreenProcessThread(fastqSample, fastqscreen,
        genomesConfiguration, genomeReferenceSample, reportDir, paired);
  }

  //
  // Getters & Setters
  //

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
    genomesConfiguration = new ArrayList<String>();
  }

  //
  // Internal class
  //

  /**
   * The internal class read the alias genome file. It make correspondence
   * between genome name in casava design file and the genome name reference
   * used for identified index of bowtie mapper. 
   * @author Sandrine Perrin
   */
  public static class AliasGenomeFile {

    // Correspondence between genome name in casava design file
    private static Map<String, String> aliasGenomes = Maps.newHashMap();
    // Correspondence between genome sample in run and genome name reference
    private static Map<String, String> aliasGenomesForRun = Maps.newHashMap();

    // Key in rundata for retrieve the path of alias file
    private static final String KEY_ALIAS_GENOME_PATH =
        "qc.conf.genome.alias.path";

    public static Set<String> convertListToGenomeReferenceName(final Map<String, String> properties,
        final Set<String> genomes) {

      Set<String> genomesNameReference = Sets.newHashSet();
      Set<String> genomesToAddInAliasGenomeFile = Sets.newHashSet();

      File aliasGenomeFile = new File(properties.get(KEY_ALIAS_GENOME_PATH));

      // Initialize map of alias genomes
      if (aliasGenomeFile.exists())
        createMapAliasGenome(aliasGenomeFile);

      if (aliasGenomes.isEmpty())
        // Return a empty set
        return genomesNameReference;

      for (String genome : genomes) {

        // Check if it exists a name reference for this genome
        if (aliasGenomes.containsKey(genome)) {
          String genomeNameReference = aliasGenomes.get(genome);
          if (genomeNameReference.length() > 0) {
            genomesNameReference.add(genomeNameReference);

            // Add in map for fastqscreen collector
            aliasGenomesForRun.put(genome, genomeNameReference);
          }

        } else {
          // Genome not present in alias genome file
          genomesToAddInAliasGenomeFile.add(genome);
        }
      }

      // Update alias genomes file
      updateAliasGenomeFile(aliasGenomeFile, genomesToAddInAliasGenomeFile);
      return genomesNameReference;
    }

    /**
     * Create a map which does correspondence between genome of sample and
     * reference genome from a file, the path is in aozan.conf
     * @param aliasGenomeFile file
     */
    private static void createMapAliasGenome(File aliasGenomeFile) {
      try {

        if (aliasGenomeFile.exists()) {

          final BufferedReader br =
              new BufferedReader(new FileReader(aliasGenomeFile));
          String line = null;

          while ((line = br.readLine()) != null) {

            final int pos = line.indexOf('=');
            if (pos == -1)
              continue;

            final String key = line.substring(0, pos);
            final String value = line.substring(pos + 1);

            // Retrieve genomes identified in Casava design file
            // Certain have not genome name reference
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
     * @param aliasGenomeFile file of alias genomes name
     * @param genomesToAdd genomes must be added in alias genomes file
     */
    private static void updateAliasGenomeFile(File aliasGenomeFile,
        Set<String> genomesToAdd) {

      // None genome identified
      if (genomesToAdd.isEmpty())
        return;

      try {
        if (aliasGenomeFile.exists()) {

          final FileWriter fw = new FileWriter(aliasGenomeFile, true);

          for (String genomeSample : genomesToAdd)
            fw.write(genomeSample + "=\n");

          fw.flush();
          fw.close();
        }
      } catch (IOException io) {
        System.out.println(io.getMessage());
      }
    }

    public static String getGenomeReferenceCorresponding(String genome) {

      genome = genome.replaceAll("\"", "").trim().toLowerCase();

      if (aliasGenomesForRun.containsKey(genome))
        return aliasGenomesForRun.get(genome);

      return "";
    }
  }
}
