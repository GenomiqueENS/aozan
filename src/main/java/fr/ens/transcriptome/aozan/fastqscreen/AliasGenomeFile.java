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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.Globals;

/**
 * This class read the alias genome file. It make correspondence between genome
 * name in casava design file and the genome name reference used for identified
 * index of bowtie mapper.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class AliasGenomeFile {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  private static AliasGenomeFile singleton;

  // Correspondence between genome name in casava design file
  private final Map<String, String> aliasGenomes = Maps.newHashMap();
  // Correspondence between genome sample in run and genome name reference
  private final Map<String, String> aliasGenomesForRun = Maps.newHashMap();

  /**
   * Make the correspondence between genome sample and the reference genomes
   * used by bowtie according to alias genomes file.
   * @param genomeAliasFile absolute path from alias genomes file
   * @param genomes set of genomes sample to convert
   * @return set of reference genomes
   */
  public Set<String> convertListToGenomeReferenceName(
      final String genomeAliasFile, final Set<String> genomes) {

    Set<String> genomesNameReference = Sets.newHashSet();
    Set<String> genomesToAddInAliasGenomeFile = Sets.newHashSet();

    File aliasGenomeFile = new File(genomeAliasFile);

    // Initialize map of alias genomes
    if (aliasGenomeFile.exists())
      createMapAliasGenome(aliasGenomeFile);

    if (aliasGenomes.isEmpty())
      // Return a empty set
      return Collections.emptySet();

    for (String sampleGenomes : genomes) {

      // Check if it exists a name reference for this genome
      if (aliasGenomes.containsKey(sampleGenomes)) {
        String genomeNameReference = aliasGenomes.get(sampleGenomes);
        if (genomeNameReference.length() > 0) {
          genomesNameReference.add(genomeNameReference);

          // Add in map for fastqscreen collector
          aliasGenomesForRun.put(sampleGenomes, genomeNameReference);
        }

      } else {
        // Genome not present in alias genome file
        genomesToAddInAliasGenomeFile.add(sampleGenomes);
      }
    }

    // Update alias genomes file
    updateAliasGenomeFile(aliasGenomeFile, genomesToAddInAliasGenomeFile);

    return genomesNameReference;
  }

  /**
   * Create a map which does correspondence between genome of sample and
   * reference genome from a file, the path is in aozan configuration
   * @param aliasGenomeFile file
   */
  private void createMapAliasGenome(File aliasGenomeFile) {
    try {

      if (aliasGenomeFile.exists()) {

        final BufferedReader br =
            Files.newReader(aliasGenomeFile, Globals.DEFAULT_FILE_ENCODING);

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

    } catch (IOException ignored) {
      LOGGER
          .warning("Reading alias genomes file failed : none genome sample can be used for detection contamination.");
    }

  }

  /**
   * Add the genome of the sample in the file which does correspondence with
   * reference genome
   * @param aliasGenomeFile file of alias genomes name
   * @param genomesToAdd genomes must be added in alias genomes file
   */
  private void updateAliasGenomeFile(File aliasGenomeFile,
      Set<String> genomesToAdd) {

    // None genome to add
    if (genomesToAdd.isEmpty())
      return;

    try {
      if (aliasGenomeFile.exists()) {

        final Writer fw =
            Files.asCharSink(aliasGenomeFile, Globals.DEFAULT_FILE_ENCODING,
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
   * Return the reference genome corresponding to the genome sample if it is
   * present in alias genomes file.
   * @param genome name of genome sample
   * @return reference genome corresponding to genome if it exists or empty
   *         string
   */
  public String getGenomeReferenceCorresponding(String genome) {

    genome = genome.replaceAll("\"", "").trim().toLowerCase();

    if (aliasGenomesForRun.containsKey(genome))
      return aliasGenomesForRun.get(genome);

    return null;
  }

  /**
   * Create a instance of AliasGenomeFile or if it exists return instance
   * @return instance of AliasGenomeFile
   */
  public static AliasGenomeFile getInstance() {

    if (singleton == null) {
      singleton = new AliasGenomeFile();
    }
    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor of AliasGenomeFile
   */
  private AliasGenomeFile() {
  }
}