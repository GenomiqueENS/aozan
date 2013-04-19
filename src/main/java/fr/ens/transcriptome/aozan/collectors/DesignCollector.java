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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.illumina.io.CasavaDesignCSVReader;

/**
 * This class define a Casava design Collector
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DesignCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "design";

  private File casavaDesignFile;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return null;
  }

  @Override
  public void configure(final Properties properties) {

    if (properties == null)
      return;

    this.casavaDesignFile =
        new File(properties.getProperty(QC.CASAVA_DESIGN_PATH));
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null)
      return;

    try {
      final Map<Integer, List<String>> samples = Maps.newHashMap();

      // Read Casava design
      final CasavaDesign design =
          new CasavaDesignCSVReader(casavaDesignFile).read();

      for (CasavaSample s : design) {

        final String prefix =
            "design.lane" + s.getLane() + "." + s.getSampleId();

        data.put(prefix + ".flow.cell.id", s.getFlowCellId());
        data.put(prefix + ".sample.ref", s.getSampleRef());
        data.put(prefix + ".indexed", s.isIndex());
        data.put(prefix + ".index", s.getIndex());
        data.put(prefix + ".description", s.getDescription());
        data.put(prefix + ".control", s.isControl());
        data.put(prefix + ".recipe", s.getRecipe());
        data.put(prefix + ".operator", s.getOperator());
        data.put(prefix + ".sample.project", s.getSampleProject());

        final List<String> samplesInLane;
        if (!samples.containsKey(s.getLane())) {
          samplesInLane = Lists.newArrayList();
          samples.put(s.getLane(), samplesInLane);
        } else
          samplesInLane = samples.get(s.getLane());
        samplesInLane.add(s.getSampleId());
      }

      for (Map.Entry<Integer, List<String>> e : samples.entrySet())
        data.put("design.lane" + e.getKey() + ".samples.names", e.getValue());

    } catch (IOException e) {
      throw new AozanException(e);
    }
  }

  @Override
  public void clear() {
  }
}
