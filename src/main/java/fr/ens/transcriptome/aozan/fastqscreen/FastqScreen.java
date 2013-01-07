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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeDebug;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;

public class FastqScreen {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  protected static final String COUNTER_GROUP = "fastqscreen";
  private static final String KEY_TMP_DIR = "tmp.dir";
  private static final String KEY_GENOMES_DESC_PATH =
      "conf.settings.genomes.desc.path";
  private static final String KEY_MAPPERS_INDEXES_PATH =
      "conf.settings.mappers.indexes.path";
  private static final String KEY_GENOMES_PATH = "conf.settings.genomes";

  private Properties properties;

  /**
   * mode pair-end : execute fastqscreen calcul
   * @param fastqRead1 fastq file input for mapper
   * @param fastqRead2 fastq file input for mapper
   * @param listGenomes list or reference genome, used by mapper
   * @return FastqScreenResult object contains results for each reference genome
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead,
      final List<String> listGenomes) throws AozanException {

    return this.execute(fastqRead, null, listGenomes);

  }

  /**
   * mode single-end : execute fastqscreen calcul
   * @param fastqFile fastq file input for mapper
   * @param listGenomes list or reference genome, used by mapper
   * @return FastqScreenResult object contains results for each reference genome
   * @throws AozanException
   */
  public FastqScreenResult execute(final File fastqRead1,
      final File fastqRead2, final List<String> listGenomes)
      throws AozanException {

    final long startTime = System.currentTimeMillis();

    String tmpDir = properties.getProperty(KEY_TMP_DIR);
    FastqScreenPseudoMapReduce pmr = new FastqScreenPseudoMapReduce();
    pmr.setMapReduceTemporaryDirectory(new File(tmpDir));

    try {

      if (fastqRead2 == null)
        pmr.doMap(fastqRead1, listGenomes, properties);
      else
        pmr.doMap(fastqRead1, fastqRead2, listGenomes, properties);

      pmr.doReduce(new File(tmpDir + "/outputDoReduce.txt"));

    } catch (IOException e) {
      e.printStackTrace();
      throw new AozanException(e.getMessage());

    } catch (BadBioEntryException bad) {
      bad.printStackTrace();
      throw new AozanException(bad.getMessage());

    }

    final long endTime = System.currentTimeMillis();
    LOGGER.info("Execute fastqscreen for genome "
        + listGenomes.toString() + " in mode "
        + (fastqRead2 == null ? "single" : "paired")
        + toTimeHumanReadable(endTime - startTime));

    System.out.println("Execute fastqscreen for genome "
        + listGenomes.toString() + " in mode "
        + (fastqRead2 == null ? "single in " : "paired in ")
        + toTimeHumanReadable(endTime - startTime));

    return pmr.getFastqScreenResult();
  }

  //
  // CONSTRUCTOR
  //

  /**
   * @param properties properties defines in configuration of aozan
   * @throws EoulsanException
   * @throws IOException
   */
  public FastqScreen(final Properties properties) {
    this.properties = properties;

    try {
      EoulsanRuntimeDebug.initDebugEoulsanRuntime();
      Settings settings = EoulsanRuntime.getSettings();

      settings.setGenomeDescStoragePath(properties.getProperty(KEY_GENOMES_DESC_PATH));
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