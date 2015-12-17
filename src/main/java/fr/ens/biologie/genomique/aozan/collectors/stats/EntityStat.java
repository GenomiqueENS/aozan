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
 *      http://tools.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.collectors.stats;

import static fr.ens.biologie.genomique.aozan.illumina.Bcl2FastqOutput.UNDETERMINED_DIR_NAME;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Globals;
import fr.ens.biologie.genomique.aozan.RunData;
import fr.ens.biologie.genomique.aozan.collectors.StatisticsCollector;
import fr.ens.biologie.genomique.aozan.util.StatisticsUtils;

/**
 * The class define a entity statistics which compute data collected on project,
 * sample or others and update run data.
 * @author Sandrine Perrin
 * @since 1.4
 */
public class EntityStat implements Comparable<EntityStat> {

  /** Default genome value. */
  private static final String DEFAULT_GENOME = "No genome.";

  /** Default read value, manage only read 1. */
  private static final int READ = 1;

  /** Run data. */
  private final RunData data;

  /** Project name. */
  private final String projectName;

  /** The sample name. */
  private final String sampleName;

  /** The entity name. */
  private final String entityName;

  private final File reportDirectory;

  /** Report samples on detection contaminant. */
  private final List<File> fastqscreenReportToCompile;

  /** Genomes. */
  private final Set<String> genomes;

  /** Samples, with technical replicats. */
  private final List<String> samples;

  /** Lanes in run for project. */
  private final Set<Integer> lanes;

  private final StatisticsCollector statisticsCollector;

  // Compile demultiplexing data on all samples
  /** Raw cluster count samples. */
  private List<Integer> rawClusterSamples;

  /** PF cluster count samples. */
  private List<Integer> pfClusterSamples;

  /**
   * Percent mapped contamination samples, value related to mapped read on data
   * set genomes contaminant setting.
   */
  private List<Double> mappedContaminationPercentSamples;

  /** Sample count. */
  private int sampleCount = 0;

  /** Project is indexed. */
  private boolean isIndexed;

  /** Raw cluster recovery sum. */
  private int rawClusterRecoverySum = 0;

  /** Cluster recovery sum. */
  private int pfClusterRecoverySum = 0;

  /** Project directory output. */
  private File projectDir;

  /** Data compile in run data. */
  private boolean compiledData = false;

  private final boolean undeterminedSample;

  @Override
  public int compareTo(final EntityStat that) {

    // Compare on project name
    return that.projectName.toLowerCase(Globals.DEFAULT_LOCALE).compareTo(
        this.projectName.toLowerCase(Globals.DEFAULT_LOCALE));
  }

  /**
   * Creates the run data project.
   * @return the run data.
   * @throws AozanException if run data object has already be create.
   */
  public RunData createRunDataProject() throws AozanException {

    if (compiledData) {
      throw new AozanException("Run data always updated for project "
          + projectName + ".");
    }

    final RunData data = new RunData();
    final String prefix = getPrefixRunData();

    StatisticsUtils stats = null;

    data.put(prefix + ".lanes", Joiner.on(",").join(this.lanes));

    data.put(prefix + ".genomes.ref", (this.genomes.isEmpty() ? "NA" : Joiner
        .on(",").join(getGenomes())));

    data.put(prefix + ".samples.count", samples.size());
    data.put(prefix + ".isindexed", isIndexed);

    // Compile data on raw cluster
    stats = new StatisticsUtils(this.rawClusterSamples);

    data.put(prefix + ".raw.cluster.sum", stats.getSumToInteger());
    data.put(prefix + ".raw.cluster.min", stats.getMin().intValue());
    data.put(prefix + ".raw.cluster.max", stats.getMax().intValue());

    // Compile data on raw cluster
    stats = new StatisticsUtils(this.pfClusterSamples);

    data.put(prefix + ".pf.cluster.sum", stats.getSumToInteger());
    data.put(prefix + ".pf.cluster.min", stats.getMin().intValue());
    data.put(prefix + ".pf.cluster.max", stats.getMax().intValue());

    addConditionalRundata(data, prefix);

    compiledData = true;

    return data;
  }

  /**
   * Adds the conditional rundata.
   * @param data the data
   * @param prefix the prefix
   */
  private void addConditionalRundata(final RunData data, final String prefix) {

    // Check collector is selected
    if (this.statisticsCollector.isUndeterminedIndexesCollectorSelected()) {
      // Compile data on recoverable cluster
      data.put(prefix + ".raw.cluster.recovery.sum", rawClusterRecoverySum);
      data.put(prefix + ".pf.cluster.recovery.sum", pfClusterRecoverySum);
    }

    // Check collector is selected
    if (this.statisticsCollector.isFastqScreenCollectorSelected()) {
      // Compile data on detection contamination
      data.put(prefix + ".samples.exceeded.contamination.threshold.count",
          getSamplesWithContaminationCount());
    }
  }

  /**
   * Adds the sample.
   * @param lane the lane in run
   * @param sample the sample name
   * @throws AozanException if run data object has already be create.
   */
  public void addEntity(final int lane, final String sample)
      throws AozanException {

    if (compiledData) {
      throw new AozanException("Can not add new sample ("
          + sample + ")for projet " + projectName
          + ".Data always compile to updata run data.");
    }

    this.lanes.add(lane);
    this.sampleCount++;

    this.isIndexed = this.data.isLaneIndexed(lane);

    // Extract raw cluster
    this.rawClusterSamples.add(this.data.getSampleRawClusterCount(lane, READ,
        sample));

    // Extract pf cluster
    this.pfClusterSamples.add(this.data.getSamplePFClusterCount(lane, READ,
        sample));

    computeConditionalRundata(lane, sample);

    this.samples.add(sample);

    // Extract from samplesheet file
    if (data.getSampleGenome(lane, sample) != null) {
      this.genomes.add(data.getSampleGenome(lane, sample));
    }
  }

  /**
   * Compute conditional rundata according to collector selected.
   * UndeterminedIndexesCollector and FastqScreenCollector is optional for this
   * collector.
   * @param lane the lane
   * @param sample the sample
   */
  private void computeConditionalRundata(final int lane, final String sample) {

    final String name = (isUndeterminedSample() ? null : sample);

    // Check collector is selected
    if (this.statisticsCollector.isUndeterminedIndexesCollectorSelected()
        && this.data.isUndeterminedInLane(lane) ) {

      // Check if lane is indexed
      if (this.data.isLaneIndexed(lane)) {
        this.rawClusterRecoverySum +=
            this.data.getSampleRawClusterRecoveryCount(lane, name);

        this.pfClusterRecoverySum +=
            this.data.getSamplePFClusterRecoveryCount(lane, name);
      }
    }

    // Check collector is selected
    if (this.statisticsCollector.isFastqScreenCollectorSelected()) {

      this.mappedContaminationPercentSamples.add(this.data
          .getPercentMappedReadOnContaminationSample(lane, name, READ));
    }

  }

  //
  // Getter
  //

  public boolean isUndeterminedSample() {
    return undeterminedSample;
  }

  /**
   * Gets the samples with contamination count.
   * @return the samples with contamination count
   */
  private String getSamplesWithContaminationCount() {
    int count = 0;

    if (isUndeterminedSample()) {
      return "NA";
    }

    for (double percent : this.mappedContaminationPercentSamples) {

      if (percent >= this.statisticsCollector.getContaminationThreshold())
        count++;
    }

    return count + "";
  }

  /**
   * Gets the genomes.
   * @return the genomes
   */
  private Set<String> getGenomes() {

    if (this.genomes.isEmpty())
      return Collections.singleton(DEFAULT_GENOME);

    return this.genomes;
  }

  /**
   * Gets the report html file.
   * @return the report html file
   */
  public File getReportHtmlFile() {

    if (this.statisticsCollector.isSampleStatisticsCollector()) {
      return new File(this.projectDir, this.sampleName + "-fastqscreen.html");
    }

    return new File(this.projectDir, this.projectName + "-fastqscreen.html");
  }

  /**
   * Gets the prefix run data.
   * @return the prefix run data
   */
  private String getPrefixRunData() {

    if (this.statisticsCollector.isSampleStatisticsCollector()) {

      if (isUndeterminedSample())
        return this.statisticsCollector.getCollectorPrefix()
            + SampleStatistics.UNDETERMINED_SAMPLE;

      return this.statisticsCollector.getCollectorPrefix() + samples.get(0);
    }

    return this.statisticsCollector.getCollectorPrefix() + projectName;
  }

  /**
   * Builds the name.
   * @param projectName the project name
   * @param sampleName the sample name
   * @return the string
   */
  private String buildName(final String projectName, final String sampleName) {

    return this.projectName
        + (sampleName == null || sampleName.isEmpty() || isUndeterminedSample()
            ? "" : " " + sampleName);
  }

  /**
   * Gets the samples.
   * @return the samples
   */
  public List<String> getSamples() {
    return samples;
  }

  /**
   * Gets the fastq screen report.
   * @return the fastq screen report
   */
  public List<File> getFastqScreenReport() {

    if (this.fastqscreenReportToCompile == null)
      return Collections.emptyList();

    return Collections.unmodifiableList(this.fastqscreenReportToCompile);
  }

  /**
   * Gets the project name.
   * @return the project name
   */
  public String getProjectName() {
    return this.projectName;
  }

  /**
   * Gets the name.
   * @return the name
   */
  public String getName() {
    return this.entityName;
  }

  /**
   * Gets the project dir.
   * @return the project dir
   */
  public File getProjectDir() {
    return this.projectDir;
  }

  @Override
  public String toString() {
    return "EntityStat [projectName="
        + projectName + ", entityName=" + entityName + ", reportDirectory="
        + reportDirectory + ", fastqscreenReportToCompile="
        + fastqscreenReportToCompile + ", genomes=" + genomes + ", samples="
        + samples + ", lanes=" + lanes + ", statisticsCollector="
        + statisticsCollector + ", rawClusterSamples=" + rawClusterSamples
        + ", pfClusterSamples=" + pfClusterSamples
        + ", mappedContaminationPercentSamples="
        + mappedContaminationPercentSamples + ", sampleCount=" + sampleCount
        + ", isIndexed=" + isIndexed + ", rawClusterRecoverySum="
        + rawClusterRecoverySum + ", pfClusterRecoverySum="
        + pfClusterRecoverySum + ", projectDir=" + projectDir
        + ", compiledData=" + compiledData + "]";
  }

  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((entityName == null) ? 0 : entityName.hashCode());
    result = prime * result + ((genomes == null) ? 0 : genomes.hashCode());
    result = prime * result + (isIndexed ? 1231 : 1237);
    result = prime * result + ((lanes == null) ? 0 : lanes.hashCode());
    result =
        prime * result + ((projectDir == null) ? 0 : projectDir.hashCode());
    result =
        prime * result + ((projectName == null) ? 0 : projectName.hashCode());
    result =
        prime
            * result
            + ((reportDirectory == null) ? 0 : reportDirectory.hashCode());
    result = prime * result + sampleCount;
    result =
        prime * result + ((sampleName == null) ? 0 : sampleName.hashCode());
    result = prime * result + ((samples == null) ? 0 : samples.hashCode());
    result = prime * result + (undeterminedSample ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    EntityStat other = (EntityStat) obj;
    if (entityName == null) {
      if (other.entityName != null)
        return false;
    } else if (!entityName.equals(other.entityName))
      return false;
    if (genomes == null) {
      if (other.genomes != null)
        return false;
    } else if (!genomes.equals(other.genomes))
      return false;
    if (isIndexed != other.isIndexed)
      return false;
    if (lanes == null) {
      if (other.lanes != null)
        return false;
    } else if (!lanes.equals(other.lanes))
      return false;
    if (projectDir == null) {
      if (other.projectDir != null)
        return false;
    } else if (!projectDir.equals(other.projectDir))
      return false;
    if (projectName == null) {
      if (other.projectName != null)
        return false;
    } else if (!projectName.equals(other.projectName))
      return false;
    if (reportDirectory == null) {
      if (other.reportDirectory != null)
        return false;
    } else if (!reportDirectory.equals(other.reportDirectory))
      return false;
    if (sampleCount != other.sampleCount)
      return false;
    if (sampleName == null) {
      if (other.sampleName != null)
        return false;
    } else if (!sampleName.equals(other.sampleName))
      return false;
    if (samples == null) {
      if (other.samples != null)
        return false;
    } else if (!samples.equals(other.samples))
      return false;
    if (undeterminedSample != other.undeterminedSample)
      return false;
    return true;
  }

  //
  // Constructor
  //
  /**
   * Instantiates a new project stat.
   * @param runData the run data
   * @param projectName the project name
   * @param statCollector the stat collector
   * @param fastqscreenReportToCompile the fastqscreen report to compile
   * @throws AozanException if an error occurs when listing source fastqscreen
   *           xml report file.
   */
  public EntityStat(final RunData runData, final String projectName,
      final StatisticsCollector statCollector,
      final List<File> fastqscreenReportToCompile) throws AozanException {

    this(runData, projectName, null, statCollector, fastqscreenReportToCompile);
  }

  /**
   * Instantiates a new entity stat.
   * @param runData the run data
   * @param projectName the project name
   * @param sampleName the sample name
   * @param statCollector the stat collector
   * @param fastqscreenReportToCompile the fastqscreen report to compile
   * @throws AozanException the aozan exception
   */
  public EntityStat(final RunData runData, final String projectName,
      final String sampleName, final StatisticsCollector statCollector,
      final List<File> fastqscreenReportToCompile) throws AozanException {

    this.data = runData;
    this.projectName = projectName;
    this.sampleName = sampleName;

    this.entityName = buildName(this.projectName, sampleName);

    this.genomes = new LinkedHashSet<>();
    this.lanes = new LinkedHashSet<>();
    this.samples = new ArrayList<>();

    this.undeterminedSample =
        Strings.isNullOrEmpty(sampleName) ? false : (sampleName.trim()
            .equals(SampleStatistics.UNDETERMINED_SAMPLE));

    // Compile demultiplexing data
    this.rawClusterSamples = new ArrayList<>();
    this.pfClusterSamples = new ArrayList<>();
    this.mappedContaminationPercentSamples = new ArrayList<>();

    this.reportDirectory = statCollector.getReportDirectory();
    this.projectDir =
        (isUndeterminedSample() ? new File(reportDirectory,
            UNDETERMINED_DIR_NAME) : new File(reportDirectory
            + "/Project_" + this.projectName));

    this.statisticsCollector = statCollector;

    this.fastqscreenReportToCompile = fastqscreenReportToCompile;
  }
}