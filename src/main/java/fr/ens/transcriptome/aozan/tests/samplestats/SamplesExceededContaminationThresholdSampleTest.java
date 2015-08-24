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

package fr.ens.transcriptome.aozan.tests.samplestats;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.collectors.stats.SampleStatistics;

/**
 * The class define test on samples count on a project which exceed a contaminant
 * threshold on percent mapped reads on dataset contaminants genomes. The
 * default value of threshold is 0.10, can be redefine in configuration file.
 * @author Sandrine Perrin
 * @since 1.4
 */
public class SamplesExceededContaminationThresholdSampleTest extends
    AbstractSimpleSampleTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(SampleStatistics.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final String sampleName) {

    return SampleStatistics.COLLECTOR_PREFIX
        + sampleName + ".samples.exceeded.contamination.threshold.count";
  }

  @Override
  protected Class<?> getValueType() {

    return Integer.class;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public SamplesExceededContaminationThresholdSampleTest() {
    super("replicatexceededcontaminationthreshold", "",
        "Sample(s) exceeded contamination threshold");
  }

}
