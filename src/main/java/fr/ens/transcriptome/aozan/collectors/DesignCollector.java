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

import static fr.ens.transcriptome.aozan.util.StringUtils.COMMA_SPLITTER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.QC;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.illumina.io.CasavaDesignCSVReader;
import fr.ens.transcriptome.aozan.illumina.sampleentry.SampleEntry;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.transcriptome.aozan.illumina.samplesheet.SampleSheetUtils;

/**
 * This class define a Casava design Collector.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class DesignCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "design";

  public static final List<String> FASTQ_COLLECTOR_NAMES = Arrays.asList(
      "tmppartialfastq", "undeterminedindexes", "fastqc", "globalstats",
      "fastqscreen", "projectstats");

  private File casavaDesignFile;

  private String bcl2fastqVersion;

  private boolean callAtLeastOneFastqCollector;

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public boolean isStatisticCollector() {
    return false;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return Lists.newArrayList(RunInfoCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final Properties properties) {

    if (properties == null) {
      return;
    }

    this.casavaDesignFile =
        new File(properties.getProperty(QC.CASAVA_DESIGN_PATH));
    this.bcl2fastqVersion = properties.getProperty(QC.BCL2FASTQ_VERSION);

    this.callAtLeastOneFastqCollector =
        checkCallFastqCollector(properties.getProperty(QC.QC_COLLECTOR_NAMES));

  }

  private boolean checkCallFastqCollector(final String collectorsNames) {

    // Split names
    for (String name : COMMA_SPLITTER.splitToList(collectorsNames)) {

      if (FASTQ_COLLECTOR_NAMES.contains(name))
        return true;
    }

    return false;
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null) {
      return;
    }

    try {
      final Map<Integer, List<String>> samples = new HashMap<>();
      final Set<String> projectsName = new TreeSet<>();

      // Read Casava design
      final SampleSheet design = createSampleSheet(data);

      for (final SampleEntry s : design) {

        final String prefix =
            "design.lane" + s.getLane() + "." + s.getSampleId();

        data.put(prefix + ".sample.ref", s.getSampleRef());
        data.put(prefix + ".indexed", s.isIndex());
        data.put(prefix + ".index", s.getIndex());
        data.put(prefix + ".description", s.getDescription());
        data.put(prefix + ".sample.project", s.getSampleProject());

        // Extract data exist only with first version
        if (SampleSheetUtils.isBcl2fastqVersion1(this.bcl2fastqVersion)) {

          data.put(prefix + ".flow.cell.id", s.getFlowCellId());
          data.put(prefix + ".control", s.isControl());
          data.put(prefix + ".recipe", s.getRecipe());
          data.put(prefix + ".operator", s.getOperator());

        } else if (SampleSheetUtils
            .isBcl2fastqVersion2(this.bcl2fastqVersion)) {
          // TODO add extract columns, retrieve map with key-value

        } else {

          throw new AozanException(
              "Design collector bcl2fastq version is invalid "
                  + this.bcl2fastqVersion);
        }

        final List<String> samplesInLane;
        if (!samples.containsKey(s.getLane())) {
          samplesInLane = new ArrayList<>();
          samples.put(s.getLane(), samplesInLane);
        } else {
          samplesInLane = samples.get(s.getLane());
        }
        samplesInLane.add(s.getSampleId());

        // List projects in run
        projectsName.add(s.getSampleProject());
      }

      // List samples by lane
      for (final Map.Entry<Integer, List<String>> e : samples.entrySet()) {
        data.put("design.lane" + e.getKey() + ".samples.names", e.getValue());

        // Check homogeneity between sample in lane
        // add in rundata interval for percent sample for each lane
        final double percent = 1.0 / e.getValue().size();
        data.put("design.lane" + e.getKey() + ".percent.homogeneity", percent);
      }

      // Add all projects name in data
      data.put("design.projects.names", Joiner.on(",").join(projectsName));

    } catch (final IOException e) {
      throw new AozanException(e);
    }
  }

  private SampleSheet createSampleSheet(final RunData data) throws IOException,
      AozanException {

    if (this.callAtLeastOneFastqCollector) {

      Preconditions.checkNotNull(data, "run data instance");

      return new CasavaDesignCSVReader(this.casavaDesignFile).readForQCReport(
          this.bcl2fastqVersion, data.getLaneCount());
    }

    return new CasavaDesignCSVReader(this.casavaDesignFile)
        .read(this.bcl2fastqVersion);
  }

  @Override
  public void clear() {
  }

}
