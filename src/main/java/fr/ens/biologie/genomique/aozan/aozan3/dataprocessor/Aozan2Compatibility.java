package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.Settings.QC_CONF_THREADS_KEY;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import fr.ens.biologie.genomique.aozan.Settings;
import fr.ens.biologie.genomique.aozan.aozan3.RunConfiguration;

/**
 * This utility class contains methods for Aozan 2 code source compatibility.
 * @author Laurent Jourdren
 * @since 3.0
 */
public class Aozan2Compatibility {

  private static final String TEST_PREFIX = "qc.test.";

  /**
   * Convert an Azoan 3 RunConfiguration object to an Aozan 2 Settings object.
   * @param runConfiguration run configuration to convert
   * @return a Settings object
   */
  public static Settings runConfigurationToSettings(
      final RunConfiguration runConfiguration) {

    Objects.requireNonNull(runConfiguration);

    Map<String, String> result = new LinkedHashMap<>();
    Map<String, String> table = createCompatibilityTable();
    Map<String, String> testTable = createTestCompatibilityTable();

    for (Map.Entry<String, String> e : runConfiguration.toMap().entrySet()) {

      String key = e.getKey();

      if (table.containsKey(key)) {

        key = table.get(key);
      } else if (key.startsWith(TEST_PREFIX)) {

        for (String prefix : testTable.keySet()) {

          if (key.toLowerCase().startsWith(prefix)) {
            key = testTable.get(prefix) + key.substring(prefix.length());
            break;
          }
        }
      }

      result.put(key, e.getValue());
    }

    return new Settings(result);
  }

  private static Map<String, String> createCompatibilityTable() {

    Map<String, String> t = new LinkedHashMap<>();

    t.put("qc.conf.fastqc.threads", QC_CONF_THREADS_KEY);

    t.put("qc.conf.blast.arguments",
        Settings.QC_CONF_FASTQC_BLAST_ARGUMENTS_KEY);
    t.put("qc.conf.blast.db.path", Settings.QC_CONF_FASTQC_BLAST_DB_PATH_KEY);
    t.put("qc.conf.blast.path", Settings.QC_CONF_FASTQC_BLAST_PATH_KEY);
    t.put("qc.conf.step.blast.enable",
        Settings.QC_CONF_FASTQC_BLAST_ENABLE_KEY);
    t.put("qc.conf.fastqscreen.blast.arguments",
        Settings.QC_CONF_FASTQC_BLAST_ARGUMENTS_KEY);
    t.put("qc.conf.fastqscreen.blast.db.path",
        Settings.QC_CONF_FASTQC_BLAST_DB_PATH_KEY);
    t.put("qc.conf.fastqscreen.blast.path",
        Settings.QC_CONF_FASTQC_BLAST_PATH_KEY);
    t.put("qc.conf.fastqscreen.blast.enable",
        Settings.QC_CONF_FASTQC_BLAST_ENABLE_KEY);

    t.put("qc.conf.fastqscreen.settings.genomes",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_PATH_KEY);
    t.put("qc.conf.fastqscreen.settings.genomes.alias.path",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_ALIAS_PATH_KEY);
    t.put("qc.conf.fastqscreen.settings.genomes.desc.path",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_DESC_PATH_KEY);
    t.put("qc.conf.fastqscreen.settings.mappers.indexes.path",
        Settings.QC_CONF_FASTQSCREEN_MAPPERS_INDEXES_PATH_KEY);
    t.put("qc.conf.fastqscreen.mapper.argument",
        Settings.QC_CONF_FASTQSCREEN_MAPPER_ARGUMENTS_KEY);
    t.put("qc.conf.fastqscreen.mapping.ignore.paired.mode",
        Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_END_MODE_KEY);
    t.put("qc.conf.fastqscreen.percent.contamination.threshold",
        Settings.QC_CONF_FASTQSCREEN_PERCENT_PROJECT_CONTAMINATION_THRESHOLD_KEY);

    t.put("qc.conf.ignore.paired.mode",
        Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_END_MODE_KEY);
    t.put("qc.conf.max.reads.parsed",
        Settings.QC_CONF_FASTQSCREEN_FASTQ_MAX_READS_PARSED_KEY);
    t.put("qc.conf.reads.pf.used",
        Settings.QC_CONF_FASTQSCREEN_FASTQ_READS_PF_USED_KEY);
    t.put("qc.conf.skip.control.lane",
        Settings.QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY);

    t.put("qc.conf.genome.alias.path",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_ALIAS_PATH_KEY);
    t.put("qc.conf.settings.genomes",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_PATH_KEY);
    t.put("qc.conf.settings.genomes.desc.path",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_DESC_PATH_KEY);
    t.put("qc.conf.settings.mappers.indexes.path",
        Settings.QC_CONF_FASTQSCREEN_MAPPERS_INDEXES_PATH_KEY);

    return t;
  }

  private static Map<String, String> createTestCompatibilityTable() {

    Map<String, String> m = new LinkedHashMap<>();
    m.put("globalbasecount", "global.base.count");
    m.put("globalcyclescount", "global.cycle.count");
    m.put("globaldensitycluster", "global.cluster.density");
    m.put("globallanescount", "global.lane.count");
    m.put("nonindexedglobalbasecount", "global.non.indexed.base.count");
    m.put("globalpercentalignal", "global.phix.align.percent");
    m.put("globalerrorrate", "global.error.rate");
    m.put("globalpercentq30", "global.q30.percent");
    m.put("globalpfclusterscount", "global.pf.cluster.count");
    m.put("globalpfclustersmean", "global.mean.pf.cluster.count");
    m.put("globalpfclustersmediane", "global.median.pf.cluster.count");
    m.put("globalprcpfclusters", "global.pf.cluster.percent");
    m.put("globalpfclusterssd", "global.pf.cluster.sd");
    m.put("globalprojectscount", "global.project.count");
    m.put("globalrawclusterscount", "global.raw.cluster.count");
    m.put("globalrawclustersmean", "global.mean.raw.cluster.count");
    m.put("globalrawclustersmediane", "global.median.raw.cluster.count");
    m.put("globalrawclustersphix", "global.phix.raw.cluster.count");
    m.put("globalrawclustersphixmean", "global.mean.phix.raw.cluster.count");
    m.put("globalrawclustersphixmediane",
        "global.median.phix.raw.cluster.count");
    m.put("globalrawclustersphixsd", "global.phix.raw.cluster.sd");
    m.put("globalrawclusterssd", "global.raw.cluster.sd");
    m.put("globalsamplescount", "global.sample.count");
    m.put("globalprcundeterminedcluster",
        "global.undetermined.cluster.percent");
    m.put("clusterdensity", "lane.cluster.density");
    m.put("errorrate100cycle", "lane.100.cycle.error.rate");
    m.put("errorrate35cycle", "lane.35.cycle.error.rate");
    m.put("errorrate75cycle", "lane.75.cycle.error.rate");
    m.put("errorrate", "lane.error.rate");
    m.put("firstcycleintensity", "lane.first.cycle.intensity");
    m.put("percentalign", "lane.phix.align.percent");
    m.put("percentintensitycycle20", "lane.cycle.20.intensity.percent");
    m.put("lanepercentq30", "lane.q30.percent");
    m.put("pfclusters", "lane.pf.cluster.count");
    m.put("pfclusterspercent", "lane.pf.cluster.percent");
    m.put("phasingprephasing", "lane.phasing.prephasing.percent");
    m.put("rawclusters", "lane.raw.cluster.count");
    m.put("rawclusterphix", "lane.phix.raw.cluster.count");
    m.put("genomesproject", "project.genome.names");
    m.put("isindexedproject", "project.is.indexed");
    m.put("lanesrunproject", "project.lane.count");
    m.put("linkprojectreport", "project.fastqscreen.report");
    m.put("recoverablepfclusterpercent",
        "project.recoverable.pf.cluster.percent");
    m.put("recoverablerawclusterpercent",
        "project.recoverable.raw.cluster.percent");
    m.put("pfclustermaxproject", "project.max.pf.cluster.count");
    m.put("pfclusterminproject", "project.min.pf.cluster.count");
    m.put("pfclustersumproject", "project.pf.cluster.count");
    m.put("rawclustermaxproject", "project.max.raw.cluster.count");
    m.put("rawclusterminproject", "project.min.raw.cluster.count");
    m.put("rawclustersumproject", "project.raw.cluster.count");
    m.put("samplecountproject", "project.sample.count");
    m.put("samplesexceededcontaminationthreshold",
        "project.fastqscreen.sample.overcontamination.count");
    m.put("adaptercontent", "sample.fastqc.adapter.content");
    m.put("badtiles", "sample.fastqc.bad.tiles");
    m.put("basicstats", "sample.fastqc.basic.stats");
    m.put("duplicationlevel", "sample.fastqc.duplication.level");
    m.put("fsqmapped", "sample.fastqscreen.mapped.percent");
    m.put("hitnolibraries",
        "sample.fastqscreen.mapped.except.ref.genome.percent");
    m.put("kmercontent", "sample.fastqc.kmer.content");
    m.put("linkreport", "sample.fastqscreen.report");
    m.put("meanqualityscorepf", "sample.base.pf.mean.quality.score");
    m.put("ncontent", "sample.fastqc.n.content");
    m.put("overrepresentedseqs", "sample.fastqc.overrepresented.sequences");
    m.put("perbasequalityscores", "sample.fastqc.per.base.quality.scores");
    m.put("perbasesequencecontent", "sample.fastqc.per.base.sequence.content");
    m.put("percentinlanesample", "sample.in.lane.percent");
    m.put("percentpfsample", "sample.pf.percent");
    m.put("percentq30", "sample.q30.percent");
    m.put("persequencegccontent", "sample.fastqc.per.sequence.gc.content");
    m.put("persequencequalityscores",
        "sample.fastqc.per.sequence.quality.scores");
    m.put("pertilesequencequality", "sample.fastqc.per.tile.sequence.quality");
    m.put("pfclusterssamples", "sample.pf.cluster.count");
    m.put("rawclusterssamples", "sample.raw.cluster.count");
    m.put("linkreportrecoverycluster", "sample.cluster.recovery.report");
    m.put("recoverablepfclusterssamplespercent",
        "sample.recoverable.pf.cluster.percent");
    m.put("recoverablepfclusterssamples",
        "sample.recoverable.pf.cluster.count");
    m.put("recoverablerawclusterssamplespercent",
        "sample.recoverable.raw.cluster.percent");
    m.put("recoverablerawclusterssamples",
        "sample.recoverable.raw.cluster.count");
    m.put("sequencelengthdistribution",
        "sample.fastqc.sequence.length.distribution");
    m.put("samplestatsfsqmapped", "pooledsample.fastqscreen.mapped.percent");
    m.put("genomessample", "pooledsample.genome.names");
    m.put("samplestathitnolibrariessum",
        "pooledsample.fastqscreen.mapped.except.ref.percent");
    m.put("isindexedsample", "pooledsample.is.indexed");
    m.put("lanesrunsample", "pooledsample.lane.count");
    m.put("linksamplereport", "pooledsample.fastqscreen.report");
    m.put("meanqualityscorepfsamplestats",
        "pooledsample.pf.base.mean.quality.score");
    m.put("samplestatspassingfilterclusterpercent", "pooledsample.pf.percent");
    m.put("samplestatspercentq30basepf", "pooledsample.q30.percent");
    m.put("samplestatsrecoverablepfclusterpercent",
        "pooledsample.recoverable.pf.cluster.percent");
    m.put("samplestatsrecoverablerawclusterpercent",
        "pooledsample.recoverable.raw.cluster.percent");
    m.put("percentsampleinproject", "pooledsample.in.project.percent");
    m.put("percentsampleinrun", "pooledsample.in.run.percent");
    m.put("samplestatspfclustermax", "pooledsample.max.pf.cluster.count");
    m.put("samplestatspfclustermin", "pooledsample.min.pf.cluster.count");
    m.put("samplestatpfclustersum", "pooledsample.pf.cluster.count");
    m.put("samplestatsrawclustermax", "pooledsample.max.raw.cluster.count");
    m.put("samplestatsrawclustermin", "pooledsample.min.raw.cluster.count");
    m.put("samplestatrawclustersum", "pooledsample.raw.cluster.count");
    m.put("samplestatsrecoverypfcluster",
        "pooledsample.pf.cluster.recovery.count");
    m.put("samplestatsrecoveryrawcluster",
        "pooledsample.raw.cluster.recovery.count");
    m.put("samplestatcountsample", "pooledsample.sample.count");
    m.put("samplestatexceededcontaminationthreshold",
        "pooledsample.fastqscreen.sample.overcontamination.count");

    Map<String, String> result = new LinkedHashMap<>();

    for (Map.Entry<String, String> e : m.entrySet()) {
      result.put(TEST_PREFIX + e.getKey() + '.',
          TEST_PREFIX + e.getValue() + '.');
    }

    return result;
  }

}
