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

package fr.ens.biologie.genomique.aozan.collectors;

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

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;

/**
 * This class define a Bcl2fastq samplesheet collector.
 * @since 0.8
 * @author Laurent Jourdren
 */
public class SamplesheetCollector implements Collector {

  /** The collector name. */
  public static final String COLLECTOR_NAME = "samplesheet";

  public static final String SAMPLESHEET_DATA_PREFIX = "samplesheet";

  public static final List<String> FASTQ_COLLECTOR_NAMES =
      Arrays.asList("tmppartialfastq", "undeterminedindexes", "fastqc",
          "globalstats", "fastqscreen", "projectstats");

  private File samplesheetFile;

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
  public void configure(final QC qc, final Properties properties) {

    if (properties == null) {
      return;
    }

    this.samplesheetFile = qc.getSampleSheetFile();
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null) {
      return;
    }

    try {
      final Map<Integer, List<String>> samples = new HashMap<>();
      final Set<String> projectsName = new TreeSet<>();

      // Read Bcl2fastq samplesheet
      final SampleSheet samplesheet = createSampleSheet(data);

      for (final Sample s : samplesheet) {

        final String prefix = SAMPLESHEET_DATA_PREFIX
            + ".lane" + s.getLane() + "." + s.getSampleId();

        data.put(prefix + ".sample.ref", s.getSampleRef());
        data.put(prefix + ".indexed", s.isIndexed());
        data.put(prefix + ".index", s.getIndex1());
        data.put(prefix + ".description", s.getDescription());
        data.put(prefix + ".sample.project", s.getSampleProject());

        if (s.isDualIndexed()) {
          data.put(prefix + ".index2", s.getIndex2());
        }

        // Extract data exist only with first version
        switch (s.getSampleSheet().getVersion()) {

        case 1:

          data.put(prefix + ".flow.cell.id", s.get("FCID"));
          data.put(prefix + ".control", s.get("Control"));
          data.put(prefix + ".recipe", s.get("Recipe"));
          data.put(prefix + ".operator", s.get("Operator"));
          break;

        case 2:

          // Include additional columns
          for (String fieldName : s.getFieldNames()) {

            if (!Sample.isInternalField(fieldName)) {
              data.put(
                  prefix + "." + fieldName.replaceAll(" _-", ".").toLowerCase(),
                  s.get(fieldName));
            }
          }

          break;

        default:

          throw new AozanException(
              "Samplesheet collector bcl2fastq version is invalid: "
                  + s.getSampleSheet().getVersion());
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
        data.put(
            SAMPLESHEET_DATA_PREFIX + ".lane" + e.getKey() + ".samples.names",
            e.getValue());

        // Check homogeneity between sample in lane
        // add in rundata interval for percent sample for each lane
        final double percent = 1.0 / e.getValue().size();
        data.put(SAMPLESHEET_DATA_PREFIX
            + ".lane" + e.getKey() + ".percent.homogeneity", percent);
      }

      // Add all projects name in data
      data.put(SAMPLESHEET_DATA_PREFIX + ".projects.names",
          Joiner.on(",").join(projectsName));

    } catch (final IOException e) {
      throw new AozanException(e);
    }
  }

  private SampleSheet createSampleSheet(final RunData data)
      throws IOException, AozanException {

    Preconditions.checkNotNull(data, "run data instance");

    return new SampleSheetCSVReader(this.samplesheetFile).read();
  }

  @Override
  public void clear() {
  }

}
