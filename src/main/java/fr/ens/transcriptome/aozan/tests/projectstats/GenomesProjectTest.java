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
package fr.ens.transcriptome.aozan.tests.projectstats;

import static fr.ens.transcriptome.aozan.collectors.stats.ProjectStatistics.COLLECTOR_PREFIX;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.collectors.stats.ProjectStatistics;

/**
 * The Class GenomesProjectTest.
 * @author Sandrine Perrin
 * @since 2.4
 */
public class GenomesProjectTest extends AbstractSimpleProjectTest {

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return ImmutableList.of(ProjectStatistics.COLLECTOR_NAME);
  }

  @Override
  protected String getKey(final String projectName) {

    return COLLECTOR_PREFIX + projectName + ".genomes.ref";
  }

  @Override
  protected Class<?> getValueType() {

    return String.class;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public GenomesProjectTest() {
    super("genomesproject", "", "Genome(s)");
  }

}
