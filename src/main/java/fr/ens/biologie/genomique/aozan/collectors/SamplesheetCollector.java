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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.QC;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.fastqscreen.GenomeAliases;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheetUtils;
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

  private File samplesheetFile;

  /**
   * This private class define a pooled sample.
   */
  private static class PooledSample {

    private final String project;
    private final String name;
    private final String index1;
    private final String index2;
    private final String ref;
    private final String normalizedRef;

    @Override
    public int hashCode() {
      return Objects.hash(this.project, this.name, this.index1, this.index2,
          this.ref, this.normalizedRef);
    }

    @Override
    public boolean equals(final Object obj) {

      if (!(obj instanceof PooledSample)) {
        return false;
      }

      final PooledSample that = (PooledSample) obj;

      return Objects.equals(this.project, that.project)
          && Objects.equals(this.name, that.name)
          && Objects.equals(this.index1, that.index1)
          && Objects.equals(this.index2, that.index2)
          && Objects.equals(this.ref, that.ref)
          && Objects.equals(this.normalizedRef, that.normalizedRef);
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param project project name
     * @param name sample name
     * @param index1 first index
     * @param index2 second index
     * @param ref the genome reference
     * @param normalizedRef the genome reference
     */
    public PooledSample(final String project, final String name,
        final String index1, final String index2, final String ref,
        final String normalizedRef) {

      this.project = project;
      this.name = name;
      this.index1 = index1;
      this.index2 = index2;
      this.ref = ref;
      this.normalizedRef = normalizedRef;
    }
  }

  @Override
  public String getName() {

    return COLLECTOR_NAME;
  }

  @Override
  public List<String> getCollectorsNamesRequiered() {

    return Lists.newArrayList(RunInfoCollector.COLLECTOR_NAME);
  }

  @Override
  public void configure(final QC qc, final CollectorConfiguration conf) {

    if (conf == null) {
      return;
    }

    // Initialize Genome aliases
    //GenomeAliases.initialize(settings);

    this.samplesheetFile = qc.getSampleSheetFile();
  }

  @Override
  public void collect(final RunData data) throws AozanException {

    if (data == null) {
      return;
    }

    try {
      final Multimap<Integer, Integer> samplesInLane =
          ArrayListMultimap.create();
      final Set<String> projectNames = new LinkedHashSet<>();
      final Multimap<String, Integer> projectSamples =
          ArrayListMultimap.create();
      final Multimap<PooledSample, Integer> pooledSamples =
          ArrayListMultimap.create();

      // Read Bcl2fastq samplesheet
      final SampleSheet samplesheet =
          new SampleSheetCSVReader(this.samplesheetFile).read();

      int sampleNumber = 0;
      final Set<Integer> lanes = new HashSet<>();
      final Set<Integer> indexedLanes = new HashSet<>();

      // TODO handle empty samplesheet where a sample is create by lane
      // TODO save pooled samples

      for (final Sample s : SampleSheetUtils
          .getCheckedDemuxTableSection(samplesheet)) {

        sampleNumber++;

        final String prefix =
            SAMPLESHEET_DATA_PREFIX + ".sample" + sampleNumber;

        final String id = s.getSampleId();
        final String name = s.getSampleName();
        final String demuxName = s.getDemultiplexingName();
        final int lane = s.getLane();
        final String project = Strings.nullToEmpty(s.getSampleProject());
        final String index1 = Strings.nullToEmpty(s.getIndex1());
        final String index2 = Strings.nullToEmpty(s.getIndex2());
        final String ref = Strings.nullToEmpty(s.getSampleRef());
        final String normalizedRef = GenomeAliases.getInstance().get(ref);

        data.put(prefix + ".id", id);
        data.put(prefix + ".name", name);
        data.put(prefix + ".demux.name", demuxName);
        data.put(prefix + ".lane", lane);
        data.put(prefix + ".undetermined", false);
        data.put(prefix + ".ref", ref);
        data.put(prefix + ".normalized.ref", normalizedRef);
        data.put(prefix + ".indexed", s.isIndexed());
        data.put(prefix + ".index", index1);
        data.put(prefix + ".description", s.getDescription());
        data.put(prefix + ".project", project);

        lanes.add(s.getLane());
        if (s.isIndexed()) {
          indexedLanes.add(s.getLane());
        }

        if (s.isDualIndexed()) {
          data.put(prefix + ".index2", index2);
        }

        projectSamples.put(project, sampleNumber);
        pooledSamples.put(new PooledSample(project, demuxName, index1, index2,
            ref, normalizedRef),
            sampleNumber);
        samplesInLane.put(lane, sampleNumber);

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

        // List projects in run
        projectNames.add(s.getSampleProject());
      }

      // Add undetermined samples
      final List<String> undeterminedSamples = new ArrayList<>();
      for (int lane : lanes) {

        if (indexedLanes.contains(lane)) {

          sampleNumber++;

          final String prefix =
              SAMPLESHEET_DATA_PREFIX + ".sample" + sampleNumber;
          data.put(prefix + ".id", "undetermined");
          data.put(prefix + ".lane", lane);
          data.put(prefix + ".undetermined", true);
          data.put(prefix + ".ref", "");
          data.put(prefix + ".normalized.ref", "");
          data.put(prefix + ".indexed", false);
          data.put(prefix + ".index", "");
          data.put(prefix + ".description", "");
          data.put(prefix + ".project", "");

          data.put(
              SAMPLESHEET_DATA_PREFIX + ".lane" + lane + ".undetermined.sample",
              sampleNumber);
          undeterminedSamples.add(Integer.toString(sampleNumber));
          samplesInLane.put(lane, sampleNumber);
        }
      }

      data.put(SAMPLESHEET_DATA_PREFIX + ".undetermined.samples",
          Joiner.on(",").join(undeterminedSamples));

      // List samples by lane
      for (final Map.Entry<Integer, Collection<Integer>> e : samplesInLane
          .asMap().entrySet()) {
        data.put(SAMPLESHEET_DATA_PREFIX + ".lane" + e.getKey() + ".samples",
            Joiner.on(",").join(e.getValue()));
      }

      // List indexed lanes
      for (int lane : lanes) {
        data.put(SAMPLESHEET_DATA_PREFIX + ".lane" + lane + ".indexed",
            indexedLanes.contains(lane));
      }

      data.put(SAMPLESHEET_DATA_PREFIX + ".sample.count", sampleNumber);

      // Add all projects name in data
      data.put(SAMPLESHEET_DATA_PREFIX + ".projects.names",
          Joiner.on(",").join(projectNames));

      // Add the projects
      addProjects(data, projectNames, projectSamples);

      // Add pooled samples
      addPooledSamplesInRunData(data, pooledSamples, undeterminedSamples);

    } catch (final IOException e) {
      throw new AozanException(e);
    }
  }

  /**
   * Add pooled samples keys in RunData object.
   * @param data the RunData object
   * @param pooledSamples the pooled samples
   * @param undeterminedSamples the undetermined samples
   */
  private static void addPooledSamplesInRunData(final RunData data,
      final Multimap<PooledSample, Integer> pooledSamples,
      final List<String> undeterminedSamples) {

    int pooledSampleNumber = 0;

    for (Map.Entry<PooledSample, Collection<Integer>> e : pooledSamples.asMap()
        .entrySet()) {

      pooledSampleNumber++;

      final PooledSample ps = e.getKey();
      final String prefix =
          SAMPLESHEET_DATA_PREFIX + ".pooledsample" + pooledSampleNumber;

      data.put(prefix + ".demux.name", ps.name);
      data.put(prefix + ".description",
          data.getSampleDescription(e.getValue().iterator().next()));
      data.put(prefix + ".undetermined", false);
      data.put(prefix + ".index", ps.index1);

      if (!ps.index2.isEmpty()) {
        data.put(prefix + ".index", ps.index2);
      }
      data.put(prefix + ".project.name", ps.project);
      data.put(prefix + ".project", data.getProjectId(ps.project));
      data.put(prefix + ".ref", ps.ref);
      data.put(prefix + ".normalized.ref", ps.normalizedRef);
      data.put(prefix + ".samples", Joiner.on(",").join(e.getValue()));
    }

    if (!undeterminedSamples.isEmpty()) {

      pooledSampleNumber++;
      final String prefix =
          SAMPLESHEET_DATA_PREFIX + ".pooledsample" + pooledSampleNumber;
      data.put(prefix + ".demux.name", "undetermined");
      data.put(prefix + ".description", "");
      data.put(prefix + ".undetermined", true);
      data.put(prefix + ".index", "");
      data.put(prefix + ".project.name", "");
      data.put(prefix + ".ref", "");
      data.put(prefix + ".normalized.ref", "");
      data.put(prefix + ".samples", Joiner.on(",").join(undeterminedSamples));
    }

    data.put(SAMPLESHEET_DATA_PREFIX + ".pooledsample.count",
        pooledSampleNumber);
  }

  /**
   * Add private keys in RunData object.
   * @param data the RunData object
   * @param projectNames the project names
   * @param undeterminedSamples the project samples
   */
  private static void addProjects(final RunData data,
      final Set<String> projectNames,
      final Multimap<String, Integer> projectSamples) {

    int projectNumber = 0;

    for (String projectName : projectNames) {

      projectNumber++;

      final String prefix =
          SAMPLESHEET_DATA_PREFIX + ".project" + projectNumber;

      data.put(prefix + ".name", projectName);
      data.put(prefix + ".samples",
          Joiner.on(",").join(projectSamples.get(projectName)));
    }
    data.put(SAMPLESHEET_DATA_PREFIX + ".project.count", projectNumber);
  }

  @Override
  public void clear() {
  }

  @Override
  public boolean isSummaryCollector() {
    return false;
  }

}
