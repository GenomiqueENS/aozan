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

import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.toTimeHumanReadable;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.collectors.CollectorConfiguration;

/**
 * This class execute fastqscreen pair-end mode or single-end.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreen {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private final File tmpDir;
  private final int confThreads;
  private final String mapperName;
  private final String mapperArgument;

  // Fields for delayed initialization of fastqScreenGenomes
  private FastqScreenGenomes fastqScreenGenomes;
  private final File samplesheetFile;
  private final String contaminantGenomeNames;

  public FastqScreenGenomes getFastqScreenGenomes() throws AozanException {

    if (this.fastqScreenGenomes == null) {
      this.fastqScreenGenomes = new FastqScreenGenomes(this.samplesheetFile,
          this.contaminantGenomeNames);
    }

    return this.fastqScreenGenomes;
  }

  /**
   * Mode pair-end : execute fastqscreen.
   * @param fastqRead fastq file input for mapper
   * @param sampleDescription sample description
   * @param genomes list or reference genome, used by mapper
   * @param genomeSample genome reference corresponding to sample
   * @return FastqScreenResult object contains results for each reference genome
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead,
      final String sampleDescription, final List<String> genomes,
      final String genomeSample, final boolean paired) throws AozanException {

    return this.execute(fastqRead, null, sampleDescription, genomes,
        genomeSample, paired);

  }

  /**
   * Mode single-end : execute fastqscreen.
   * @param fastqRead1 fastq read1 file input for mapper
   * @param fastqRead2 fastq read2 file input for mapper
   * @param sampleDescription sample description
   * @param genomes list or reference genome, used by mapper
   * @param sampleGenome genome reference corresponding to sample
   * @param isPairedMode true if a pair-end run and option paired mode equals
   *          true else false
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead1, final File fastqRead2,
      final String sampleDescription, final List<String> genomes,
      final String sampleGenome, final boolean isPairedMode)
      throws AozanException {

    requireNonNull(fastqRead1, "fastqRead1 argument cannot be null");
    requireNonNull(genomes, "genomes argument cannot be null");
    requireNonNull(sampleDescription,
        "sampleDescription argument cannot be null");

    if (isPairedMode) {
      requireNonNull(fastqRead2, "fastqRead2 argument cannot be null");
    }

    // Timer
    final Stopwatch timer = Stopwatch.createStarted();

    final FastqScreenPseudoMapReduce pmr = new FastqScreenPseudoMapReduce(
        this.tmpDir, isPairedMode, this.mapperName, this.mapperArgument);

    try {

      if (isPairedMode) {
        pmr.doMap(fastqRead1, fastqRead2, genomes, sampleGenome,
            this.confThreads);
      } else {
        pmr.doMap(fastqRead1, genomes, sampleGenome, this.confThreads);
      }

      LOGGER.fine("FASTQSCREEN: step map for "
          + sampleDescription + " in mode "
          + (isPairedMode ? "paired" : "single") + " on genome(s) " + genomes
          + " in " + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

      timer.reset();
      timer.start();

      final File outputDoReduceFile =
          new File(this.tmpDir, "outputDoReduce.txt");

      pmr.doReduce(outputDoReduceFile);

      LOGGER.fine("FASTQSCREEN: step reduce for "
          + sampleDescription + " in mode "
          + (isPairedMode ? "paired" : "single") + " in "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

      // Remove temporary output file use in map-reduce step
      if (!outputDoReduceFile.delete()) {
        LOGGER.warning("Fastqscreen : fail to delete file "
            + outputDoReduceFile.getAbsolutePath());
      }

    } catch (final IOException e) {
      throw new AozanException(e);

    } finally {
      timer.stop();
    }

    return pmr.getFastqScreenResult();
  }

  //
  // Constructor
  //

  /**
   * Public constructor of fastqscreen. Initialization of settings of Eoulsan
   * necessary for use of mapper index.
   * @param conf Collector configuration
   */
  public FastqScreen(final CollectorConfiguration conf) {

    requireNonNull(conf, "properties argument cannot be null");

    this.tmpDir = conf.getFile(QC.TMP_DIR);

    this.confThreads = conf.getInt(Settings.QC_CONF_THREADS_KEY, -1);

    // Fields required to initialize fastqScreenGenomes
    this.samplesheetFile = conf.getFile(QC.BCL2FASTQ_SAMPLESHEET_PATH);
    this.contaminantGenomeNames =
        conf.get(Settings.QC_CONF_FASTQSCREEN_GENOMES_KEY);

    // Parameter mapper instead of default value
    this.mapperName = conf.get(Settings.QC_CONF_FASTQSCREEN_MAPPER_KEY);
    this.mapperArgument =
        conf.get(Settings.QC_CONF_FASTQSCREEN_MAPPER_ARGUMENTS_KEY);

  }
}