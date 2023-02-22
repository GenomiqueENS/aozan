package fr.ens.biologie.genomique.aozan;

import static fr.ens.biologie.genomique.kenetre.io.CompressionType.open;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import fr.ens.biologie.genomique.kenetre.bio.BadBioEntryException;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;
import fr.ens.biologie.genomique.kenetre.storage.FileGenomeDescStorage;
import fr.ens.biologie.genomique.kenetre.storage.FileGenomeIndexStorage;
import fr.ens.biologie.genomique.kenetre.storage.FileStorage;
import fr.ens.biologie.genomique.kenetre.storage.GenomeDescStorage;
import fr.ens.biologie.genomique.kenetre.storage.GenomeIndexStorage;

public class Storages {

  private static Storages instance = null;

  private final GenomeDescStorage genomeDescStorage;
  private final GenomeIndexStorage genomeIndexStorage;
  private final FileStorage genomeStorage;

  public boolean isGenomeDescStorage() {

    return this.genomeDescStorage != null;
  }

  public boolean isGenomeIndexStorage() {

    return this.genomeIndexStorage != null;
  }

  public boolean isGenomeStorage() {

    return this.genomeStorage != null;
  }

  /**
   * Get the genome description storage.
   * @return the genome description storage object
   */
  public GenomeDescStorage getGenomeDescStorage() {
    return this.genomeDescStorage;
  }

  /**
   * Get the genome index storage.
   * @return the genome index storage object
   */
  public GenomeIndexStorage getGenomeIndexStorage() {
    return this.genomeIndexStorage;
  }

  /**
   * Get the genome storage.
   * @return the genome index object
   */
  public FileStorage getGenomeStorage() {
    return this.genomeStorage;
  }

  /**
   * Create a GenomeDescription object from a Fasta file.
   * @param genomeFile file used for create index
   * @return genomeDescription description of the genome
   * @throws BadBioEntryException if an error occurs during create genome
   *           description object
   * @throws IOException if an error occurs during access genome file
   */
  public GenomeDescription createGenomeDescription(final File genomePath)
      throws BadBioEntryException, IOException {

    requireNonNull(genomePath, "genomePath argument cannot be null");

    GenomeDescription desc = null;

    if (this.genomeDescStorage != null) {
      desc = this.genomeDescStorage.get(genomePath.getPath());
    }

    // Compute the genome description
    if (desc == null) {
      desc = GenomeDescription.createGenomeDescFromFasta(open(genomePath),
          genomePath.getName());

      if (this.genomeDescStorage != null) {
        this.genomeDescStorage.put(genomePath.getAbsolutePath(), desc);
      }
    }

    return desc;
  }

  //
  // Static methods
  //

  public static synchronized void init(String genomeStoragePath,
      String genomeDescStoragePath, String genomeMapperIndexStoragePath,
      GenericLogger logger) {

    if (instance != null) {
      throw new IllegalStateException("Storages has been already initialized");
    }

    File genomeStorageFile =
        genomeStoragePath != null ? new File(genomeStoragePath) : null;

    File genomeDescStorageFile =
        genomeDescStoragePath != null ? new File(genomeDescStoragePath) : null;

    File genomeMapperIndexStorageFile = genomeMapperIndexStoragePath != null
        ? new File(genomeMapperIndexStoragePath) : null;

    instance = new Storages(genomeStorageFile, genomeDescStorageFile,
        genomeMapperIndexStorageFile, logger);
  }

  public static synchronized Storages getInstance() {

    if (instance == null) {
      throw new IllegalStateException("Storages has not been initialized");
    }

    return instance;
  }

  //
  // Constructor
  //

  private Storages(File genomeStorageFile, File genomeDescStorageFile,
      File genomeMapperIndexStorageFile, GenericLogger logger) {

    this.genomeStorage = genomeStorageFile != null
        ? new FileStorage(genomeStorageFile.getAbsolutePath(),
            Arrays.asList(".fasta", ".fa", ".fna"))
        : null;

    this.genomeDescStorage = genomeDescStorageFile != null
        ? FileGenomeDescStorage.getInstance(
            genomeDescStorageFile.getAbsolutePath(), logger)
        : null;

    this.genomeIndexStorage = genomeMapperIndexStorageFile != null
        ? FileGenomeIndexStorage.getInstance(
            genomeMapperIndexStorageFile.getAbsolutePath(), logger)
        : null;
  }

}
