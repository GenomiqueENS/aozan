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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.Common;
import fr.ens.transcriptome.aozan.io.FastqSample;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;

/**
 * This class execute fastqscreen pair-end mode or single-end
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreen {

  /** Logger */
  private static final Logger LOGGER = Common.getLogger();

  protected static final String COUNTER_GROUP = "fastqscreen";

  public static final String KEY_NUMBER_THREAD = "qc.conf.fastqc.threads";
  public static final String KEY_TMP_DIR = "tmp.dir";

  public static final String KEY_GENOMES_DESC_PATH =
      "qc.conf.settings.genomes.desc.path";
  public static final String KEY_MAPPERS_INDEXES_PATH =
      "qc.conf.settings.mappers.indexes.path";
  public static final String KEY_GENOMES_PATH = "qc.conf.settings.genomes";

  // Configuration mapper for fastqscreen, optional
  public static final String KEY_MAPPER_NAME = "qc.conf.fastqscreen.mapper";
  public static final String KEY_MAPPER_ARGUMENT =
      "qc.conf.fastqscreen.mapper.argument";

  private String tmpDir;
  private int confThreads;
  private String mapperName;
  private String mapperArgument;

  /**
   * Mode pair-end : execute fastqscreen
   * @param fastqRead1 fastq file input for mapper
   * @param fastqRead2 fastq file input for mapper
   * @param listGenomes list or reference genome, used by mapper
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
   * Mode single-end : execute fastqscreen
   * @param fastqFile fastq file input for mapper
   * @param listGenomes list or reference genome, used by mapper
   * @param genomeSample genome reference corresponding to sample
   * @param paired true if a pair-end run and option paired mode equals true
   *          else false
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead1,
      final File fastqRead2, final FastqSample fastqSample,
      final List<String> genomes, final String genomeSample,
      final boolean pairedMode) throws AozanException {

    // Timer
    final Stopwatch timer = new Stopwatch().start();

    FastqScreenPseudoMapReduce pmr =
        new FastqScreenPseudoMapReduce(tmpDir, pairedMode, mapperName,
            mapperArgument);

    try {

      if (pairedMode)
        pmr.doMap(fastqRead1, fastqRead2, genomes, genomeSample, confThreads);
      else
        pmr.doMap(fastqRead1, genomes, genomeSample, confThreads);

      LOGGER.fine("FASTQSCREEN : step map for "
          + fastqSample.getKeyFastqSample() + " in mode "
          + (pairedMode ? "paired" : "single") + " on genome(s) " + genomes
          + " in " + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

      timer.reset();
      timer.start();

      pmr.doReduce(new File(tmpDir + "/outputDoReduce.txt"));

      LOGGER.fine("FASTQSCREEN : step reduce for "
          + fastqSample.getKeyFastqSample() + " in mode "
          + (pairedMode ? "paired" : "single") + " in "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

      // Remove temporary output file use in map-reduce step
      File f = new File(tmpDir + "/outputDoReduce.txt");
      if (!f.delete())
        LOGGER.warning("Fastqscreen : fail to delete file "
            + f.getAbsolutePath());

    } catch (IOException e) {
      throw new AozanException(e);

    } catch (BadBioEntryException bad) {
      throw new AozanException(bad);

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

    this.tmpDir = properties.getProperty(KEY_TMP_DIR);
    
    if (properties.containsKey(KEY_TMP_DIR)) {
      try {
        confThreads =
            Integer.parseInt(properties.getProperty(KEY_NUMBER_THREAD));
      } catch (Exception e) {
      }
    }

    // Parameter mapper instead of default value
    this.mapperName = properties.getProperty(KEY_MAPPER_NAME);
    this.mapperArgument = properties.getProperty(KEY_MAPPER_ARGUMENT);

    Settings settings = EoulsanRuntime.getSettings();

    settings.setGenomeDescStoragePath(properties
        .getProperty(KEY_GENOMES_DESC_PATH));
    settings.setGenomeMapperIndexStoragePath(properties
        .getProperty(KEY_MAPPERS_INDEXES_PATH));
    settings.setGenomeStoragePath(properties.getProperty(KEY_GENOMES_PATH));

  }
}