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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.storages.GenomeDescStorage;
import fr.ens.transcriptome.eoulsan.data.storages.SimpleGenomeDescStorage;

/**
 * This class allow to create genome descriptions.
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 * @since 2.0
 */
public class GenomeDescriptionCreator {

  private static GenomeDescriptionCreator singleton;

  private final GenomeDescStorage storage;

  /**
   * Create a GenomeDescription object from a Fasta file.
   * @param genomeFile file used for create index
   * @return genomeDescription description of the genome
   * @throws BadBioEntryException if an error occurs during create genome
   *           description object
   * @throws IOException if an error occurs during access genome file
   */
  public GenomeDescription createGenomeDescription(final DataFile genomeFile)
      throws BadBioEntryException, IOException {

    checkNotNull(genomeFile, "genomeFile argument cannot be null");

    GenomeDescription desc = null;

    if (this.storage != null) {
      desc = this.storage.get(genomeFile);
    }

    // Compute the genome description
    if (desc == null) {
      desc = GenomeDescription.createGenomeDescFromFasta(genomeFile.open(),
          genomeFile.getName());

      if (this.storage != null) {
        this.storage.put(genomeFile, desc);
      }
    }

    return desc;
  }

  //
  // Static methods
  //

  /**
   * Initialize the singleton.
   */
  public static void initialize(final String genomeDescStoragePath) {

    if (singleton == null) {

      singleton = new GenomeDescriptionCreator(genomeDescStoragePath);
    }
  }

  /**
   * Get the instance of the singleton.
   * @return the singleton instance of GenomeDescriptionCreator
   */
  public static GenomeDescriptionCreator getInstance() {

    if (singleton == null) {
      throw new IllegalStateException(
          "The instance of the singleton of GenomeDescriptionCreator has not been created");
    }

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   * @param genomeDescStoragePath path to genome description storage
   */
  private GenomeDescriptionCreator(final String genomeDescStoragePath) {

    this.storage = genomeDescStoragePath == null
        ? null : SimpleGenomeDescStorage
            .getInstance(new DataFile(genomeDescStoragePath));
  }

}
