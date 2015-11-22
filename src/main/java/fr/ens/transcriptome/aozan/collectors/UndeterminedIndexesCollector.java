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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.Settings;
import fr.ens.transcriptome.aozan.io.FastqSample;

/**
 * This class allow to collect information about the undetermined indices that
 * can be recovered.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class UndeterminedIndexesCollector extends AbstractFastqCollector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "undeterminedindexes";

  private int numberThreads = Runtime.getRuntime().availableProcessors();
  private File undeterminedIndexedXSLFile;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  protected boolean isProcessStandardSamples() {

    return false;
  }

  @Override
  protected boolean isProcessUndeterminedIndicesSamples() {

    return true;
  }

  @Override
  protected boolean isProcessAllReads() {

    return false;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    final List<String> result =
        Lists.newArrayList(super.getCollectorsNamesRequiered());
    result.add(FlowcellDemuxSummaryCollector.COLLECTOR_NAME);

    return Collections.unmodifiableList(result);
  }

  @Override
  public void configure(final QC qc, final Properties properties) {

    super.configure(qc, properties);

    // Set the number of threads
    if (properties.containsKey(Settings.QC_CONF_THREADS_KEY)) {

      try {
        int confThreads = Integer.parseInt(
            properties.getProperty(Settings.QC_CONF_THREADS_KEY).trim());
        if (confThreads > 0)
          this.numberThreads = confThreads;

      } catch (NumberFormatException ignored) {
      }
    }

    // Set external xsl file to write report html instead of default version
    try {
      String filename = properties
          .getProperty(Settings.QC_CONF_UNDETERMINED_INDEXED_XSL_FILE_KEY);
      if (new File(filename).exists())
        this.undeterminedIndexedXSLFile = new File(filename);
    } catch (Exception e) {
      // Call default xsl file
      this.undeterminedIndexedXSLFile = null;
    }
  }

  @Override
  protected int getThreadsNumber() {

    return this.numberThreads;
  }

  @Override
  protected AbstractFastqProcessThread collectSample(final RunData data,
      final FastqSample fastqSample, final File reportDir, boolean runPE)
          throws AozanException {

    if (fastqSample.getFastqFiles().isEmpty()) {
      return null;
    }

    return new UndeterminedIndexesProcessThreads(data, fastqSample, reportDir,
        this.undeterminedIndexedXSLFile);
  }

}
