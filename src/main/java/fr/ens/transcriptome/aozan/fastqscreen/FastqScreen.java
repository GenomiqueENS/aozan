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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;

/**
 * This class execute fastqscreen pair-end mode or single-end.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreen {

  /** Logger. */
  private static final Logger LOGGER = Common.getLogger();

  private final File tmpDir;
  private int confThreads;
  private final String mapperName;
  private final String mapperArgument;

  /**
   * Mode pair-end : execute fastqscreen.
   * @param fastqRead fastq file input for mapper
   * @param fastqSample instance to describe fastq sample
   * @param genomes list or reference genome, used by mapper
   * @param genomeSample genome reference corresponding to sample
   * @return FastqScreenResult object contains results for each reference genome
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead,
      final FastqSample fastqSample, final List<String> genomes,
      final String genomeSample, final boolean paired) throws AozanException {

    return this.execute(fastqRead, null, fastqSample, genomes, genomeSample,
        paired);

  }

  /**
   * Mode single-end : execute fastqscreen.
   * @param fastqRead1 fastq read1 file input for mapper
   * @param fastqRead2 fastq read2 file input for mapper
   * @param fastqSample instance to describe fastq sample
   * @param genomes list or reference genome, used by mapper
   * @param genomeSample genome reference corresponding to sample
   * @param isPairedMode true if a pair-end run and option paired mode equals
   *          true else false
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead1, final File fastqRead2,
      final FastqSample fastqSample, final List<String> genomes,
      final String genomeSample, final boolean isPairedMode)
          throws AozanException {

    checkNotNull(fastqRead1, "fastqRead1 argument cannot be null");
    checkNotNull(genomes, "genomes argument cannot be null");
    checkNotNull(genomeSample, "genomeSample argument cannot be null");

    if (isPairedMode) {
      checkNotNull(fastqRead2, "fastqRead2 argument cannot be null");
    }

    // Timer
    final Stopwatch timer = Stopwatch.createStarted();

    final FastqScreenPseudoMapReduce pmr = new FastqScreenPseudoMapReduce(
        this.tmpDir, isPairedMode, this.mapperName, this.mapperArgument);

    try {

      if (isPairedMode) {
        pmr.doMap(fastqRead1, fastqRead2, genomes, genomeSample,
            this.confThreads);
      } else {
        pmr.doMap(fastqRead1, genomes, genomeSample, this.confThreads);
      }

      LOGGER.fine("FASTQSCREEN : step map for "
          + fastqSample.getKeyFastqSample() + " in mode "
          + (isPairedMode ? "paired" : "single") + " on genome(s) " + genomes
          + " in " + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

      timer.reset();
      timer.start();

      final File outputDoReduceFile =
          new File(this.tmpDir, "outputDoReduce.txt");

      pmr.doReduce(outputDoReduceFile);

      LOGGER.fine("FASTQSCREEN : step reduce for "
          + fastqSample.getKeyFastqSample() + " in mode "
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
   * @param properties properties defines in configuration of aozan
   */
  public FastqScreen(final Properties properties) {

    checkNotNull(properties, "properties argument cannot be null");

    this.tmpDir = new File(properties.getProperty(QC.TMP_DIR));

    if (properties.containsKey(Settings.QC_CONF_THREADS_KEY)) {
      try {
        this.confThreads = Integer
            .parseInt(properties.getProperty(Settings.QC_CONF_THREADS_KEY));
      } catch (final Exception e) {
      }
    }

    // Parameter mapper instead of default value
    this.mapperName =
        properties.getProperty(Settings.QC_CONF_FASTQSCREEN_MAPPER_KEY);
    this.mapperArgument = properties
        .getProperty(Settings.QC_CONF_FASTQSCREEN_MAPPER_ARGUMENT_KEY);

    final fr.ens.transcriptome.eoulsan.Settings settings =
        EoulsanRuntime.getSettings();

    settings.setGenomeDescStoragePath(properties.getProperty(
        Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_DESC_PATH_KEY));
    settings.setGenomeMapperIndexStoragePath(properties.getProperty(
        Settings.QC_CONF_FASTQSCREEN_SETTINGS_MAPPERS_INDEXES_PATH_KEY));
    settings.setGenomeStoragePath(properties
        .getProperty(Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_KEY));

  }
}