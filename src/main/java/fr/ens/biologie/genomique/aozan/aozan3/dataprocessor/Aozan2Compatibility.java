package fr.ens.biologie.genomique.aozan.aozan3.dataprocessor;

import static fr.ens.biologie.genomique.aozan.Settings.QC_CONF_THREADS_KEY;

import java.util.HashMap;
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

  /**
   * Convert an Azoan 3 RunConfiguration object to an Aozan 2 Settings object.
   * @param runConfiguration run configuration to convert
   * @return a Settings object
   */
  public static Settings runConfigurationToSettings(
      final RunConfiguration runConfiguration) {

    Objects.requireNonNull(runConfiguration);

    Map<String, String> table = new HashMap<>();

    table.put("qc.conf.fastqc.threads", QC_CONF_THREADS_KEY);

    table.put("qc.conf.blast.arguments",
        Settings.QC_CONF_FASTQC_BLAST_ARGUMENTS_KEY);
    table.put("qc.conf.blast.db.path",
        Settings.QC_CONF_FASTQC_BLAST_DB_PATH_KEY);
    table.put("qc.conf.blast.path", Settings.QC_CONF_FASTQC_BLAST_PATH_KEY);
    table.put("qc.conf.step.blast.enable",
        Settings.QC_CONF_FASTQC_BLAST_ENABLE_KEY);
    table.put("qc.conf.fastqscreen.blast.arguments",
        Settings.QC_CONF_FASTQC_BLAST_ARGUMENTS_KEY);
    table.put("qc.conf.fastqscreen.blast.db.path",
        Settings.QC_CONF_FASTQC_BLAST_DB_PATH_KEY);
    table.put("qc.conf.fastqscreen.blast.path",
        Settings.QC_CONF_FASTQC_BLAST_PATH_KEY);
    table.put("qc.conf.fastqscreen.blast.enable",
        Settings.QC_CONF_FASTQC_BLAST_ENABLE_KEY);

    table.put("qc.conf.fastqscreen.settings.genomes",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_PATH_KEY);
    table.put("qc.conf.fastqscreen.settings.genomes.alias.path",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_ALIAS_PATH_KEY);
    table.put("qc.conf.fastqscreen.settings.genomes.desc.path",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_DESC_PATH_KEY);
    table.put("qc.conf.fastqscreen.settings.mappers.indexes.path",
        Settings.QC_CONF_FASTQSCREEN_MAPPERS_INDEXES_PATH_KEY);
    table.put("qc.conf.fastqscreen.mapper.argument",
        Settings.QC_CONF_FASTQSCREEN_MAPPER_ARGUMENTS_KEY);
    table.put("qc.conf.fastqscreen.mapping.ignore.paired.mode",
        Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_END_MODE_KEY);
    table.put("qc.conf.fastqscreen.percent.contamination.threshold",
        Settings.QC_CONF_FASTQSCREEN_PERCENT_PROJECT_CONTAMINATION_THRESHOLD_KEY);

    table.put("qc.conf.ignore.paired.mode",
        Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_END_MODE_KEY);
    table.put("qc.conf.max.reads.parsed",
        Settings.QC_CONF_FASTQSCREEN_FASTQ_MAX_READS_PARSED_KEY);
    table.put("qc.conf.reads.pf.used",
        Settings.QC_CONF_FASTQSCREEN_FASTQ_READS_PF_USED_KEY);
    table.put("qc.conf.skip.control.lane",
        Settings.QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY);

    table.put("qc.conf.genome.alias.path",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_ALIAS_PATH_KEY);
    table.put("qc.conf.settings.genomes",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_PATH_KEY);
    table.put("qc.conf.settings.genomes.desc.path",
        Settings.QC_CONF_FASTQSCREEN_GENOMES_DESC_PATH_KEY);
    table.put("qc.conf.settings.mappers.indexes.path",
        Settings.QC_CONF_FASTQSCREEN_MAPPERS_INDEXES_PATH_KEY);

    // Converting table between old and new test names
    Map<String, String> testNameTable = new HashMap<>();
    testNameTable.put("globalbasecount", "global.base.count");
    testNameTable.put("globalcyclescount", "global.cycle.count");
    testNameTable.put("globaldensitycluster", "global.cluster.density");
    testNameTable.put("globallanescount", "global.lane.count");
    testNameTable.put("nonindexedglobalbasecount",
        "global.non.indexed.base.count");
    testNameTable.put("globalpercentalignal", "global.phix.align.percent");
    testNameTable.put("globalerrorrate", "global.error.rate");
    testNameTable.put("globalpercentq30", "global.q30.percent");
    testNameTable.put("globalpfclusterscount", "global.pf.cluster.count");
    testNameTable.put("globalpfclustersmean", "global.mean.pf.cluster.count");
    testNameTable.put("globalpfclustersmediane",
        "global.median.pf.cluster.count");
    testNameTable.put("globalprcpfclusters", "global.pf.cluster.percent");
    testNameTable.put("globalpfclusterssd", "global.pf.cluster.sd");
    testNameTable.put("globalprojectscount", "global.project.count");
    testNameTable.put("globalrawclusterscount", "global.raw.cluster.count");
    testNameTable.put("globalrawclustersmean", "global.mean.raw.cluster.count");
    testNameTable.put("globalrawclustersmediane",
        "global.median.raw.cluster.count");
    testNameTable.put("globalrawclustersphix", "global.phix.raw.cluster.count");
    testNameTable.put("globalrawclustersphixmean",
        "global.mean.phix.raw.cluster.count");
    testNameTable.put("globalrawclustersphixmediane",
        "global.median.phix.raw.cluster.count");
    testNameTable.put("globalrawclustersphixsd", "global.phix.raw.cluster.sd");
    testNameTable.put("globalrawclusterssd", "global.raw.cluster.sd");
    testNameTable.put("globalsamplescount", "global.sample.count");
    testNameTable.put("globalprcundeterminedcluster",
        "global.undetermined.cluster.percent");
    testNameTable.put("clusterdensity", "lane.cluster.density");
    testNameTable.put("errorrate100cycle", "lane.100.cycle.error.rate");
    testNameTable.put("errorrate35cycle", "lane.35.cycle.error.rate");
    testNameTable.put("errorrate75cycle", "lane.75.cycle.error.rate");
    testNameTable.put("errorrate", "lane.error.rate");
    testNameTable.put("firstcycleintensity", "lane.first.cycle.intensity");
    testNameTable.put("percentalign", "lane.phix.align.percent");
    testNameTable.put("percentintensitycycle20",
        "lane.cycle.20.intensity.percent");
    testNameTable.put("lanepercentq30", "lane.q30.percent");
    testNameTable.put("pfclusters", "lane.pf.cluster.count");
    testNameTable.put("pfclusterspercent", "lane.pf.cluster.percent");
    testNameTable.put("phasingprephasing", "lane.phasing.prephasing.percent");
    testNameTable.put("rawclusters", "lane.raw.cluster.count");
    testNameTable.put("rawclusterphix", "lane.phix.raw.cluster.count");
    testNameTable.put("genomesproject", "project.genome.names");
    testNameTable.put("isindexedproject", "project.is.indexed");
    testNameTable.put("lanesrunproject", "project.lane.count");
    testNameTable.put("linkprojectreport", "project.fastqscreen.report");
    testNameTable.put("recoverablepfclusterpercent",
        "project.recoverable.pf.cluster.percent");
    testNameTable.put("recoverablerawclusterpercent",
        "project.recoverable.raw.cluster.percent");
    testNameTable.put("pfclustermaxproject", "project.max.pf.cluster.count");
    testNameTable.put("pfclusterminproject", "project.min.pf.cluster.count");
    testNameTable.put("pfclustersumproject", "project.pf.cluster.count");
    testNameTable.put("rawclustermaxproject", "project.max.raw.cluster.count");
    testNameTable.put("rawclusterminproject", "project.min.raw.cluster.count");
    testNameTable.put("rawclustersumproject", "project.raw.cluster.count");
    testNameTable.put("samplecountproject", "project.sample.count");
    testNameTable.put("samplesexceededcontaminationthreshold",
        "project.fastqscreen.sample.overcontamination.count");
    testNameTable.put("adaptercontent", "sample.fastqc.adapter.content");
    testNameTable.put("badtiles", "sample.fastqc.bad.tiles");
    testNameTable.put("basicstats", "sample.fastqc.basic.stats");
    testNameTable.put("duplicationlevel", "sample.fastqc.duplication.level");
    testNameTable.put("fsqmapped", "sample.fastqscreen.mapped.percent");
    testNameTable.put("hitnolibraries",
        "sample.fastqscreen.mapped.except.ref.genome.percent");
    testNameTable.put("kmercontent", "sample.fastqc.kmer.content");
    testNameTable.put("linkreport", "sample.fastqscreen.report");
    testNameTable.put("meanqualityscorepf",
        "sample.base.pf.mean.quality.score");
    testNameTable.put("ncontent", "sample.fastqc.n.content");
    testNameTable.put("overrepresentedseqs",
        "sample.fastqc.overrepresented.sequences");
    testNameTable.put("perbasequalityscores",
        "sample.fastqc.per.base.quality.scores");
    testNameTable.put("perbasesequencecontent",
        "sample.fastqc.per.base.sequence.content");
    testNameTable.put("percentinlanesample", "sample.in.lane.percent");
    testNameTable.put("percentpfsample", "sample.pf.percent");
    testNameTable.put("percentq30", "sample.q30.percent");
    testNameTable.put("persequencegccontent",
        "sample.fastqc.per.sequence.gc.content");
    testNameTable.put("persequencequalityscores",
        "sample.fastqc.per.sequence.quality.scores");
    testNameTable.put("pertilesequencequality",
        "sample.fastqc.per.tile.sequence.quality");
    testNameTable.put("pfclusterssamples", "sample.pf.cluster.count");
    testNameTable.put("rawclusterssamples", "sample.raw.cluster.count");
    testNameTable.put("linkreportrecoverycluster",
        "sample.cluster.recovery.report");
    testNameTable.put("recoverablepfclusterssamplespercent",
        "sample.recoverable.pf.cluster.percent");
    testNameTable.put("recoverablepfclusterssamples",
        "sample.recoverable.pf.cluster.count");
    testNameTable.put("recoverablerawclusterssamplespercent",
        "sample.recoverable.raw.cluster.percent");
    testNameTable.put("recoverablerawclusterssamples",
        "sample.recoverable.raw.cluster.count");
    testNameTable.put("sequencelengthdistribution",
        "sample.fastqc.sequence.length.distribution");
    testNameTable.put("samplestatsfsqmapped",
        "pooledsample.fastqscreen.mapped.percent");
    testNameTable.put("genomessample", "pooledsample.genome.names");
    testNameTable.put("samplestathitnolibrariessum",
        "pooledsample.fastqscreen.mapped.except.ref.percent");
    testNameTable.put("isindexedsample", "pooledsample.is.indexed");
    testNameTable.put("lanesrunsample", "pooledsample.lane.count");
    testNameTable.put("linksamplereport", "pooledsample.fastqscreen.report");
    testNameTable.put("meanqualityscorepfsamplestats",
        "pooledsample.pf.base.mean.quality.score");
    testNameTable.put("samplestatspassingfilterclusterpercent",
        "pooledsample.pf.percent");
    testNameTable.put("samplestatspercentq30basepf",
        "pooledsample.q30.percent");
    testNameTable.put("samplestatsrecoverablepfclusterpercent",
        "pooledsample.recoverable.pf.cluster.percent");
    testNameTable.put("samplestatsrecoverablerawclusterpercent",
        "pooledsample.recoverable.raw.cluster.percent");
    testNameTable.put("percentsampleinproject",
        "pooledsample.in.project.percent");
    testNameTable.put("percentsampleinrun", "pooledsample.in.run.percent");
    testNameTable.put("samplestatspfclustermax",
        "pooledsample.max.pf.cluster.count");
    testNameTable.put("samplestatspfclustermin",
        "pooledsample.min.pf.cluster.count");
    testNameTable.put("samplestatpfclustersum",
        "pooledsample.pf.cluster.count");
    testNameTable.put("samplestatsrawclustermax",
        "pooledsample.max.raw.cluster.count");
    testNameTable.put("samplestatsrawclustermin",
        "pooledsample.min.raw.cluster.count");
    testNameTable.put("samplestatrawclustersum",
        "pooledsample.raw.cluster.count");
    testNameTable.put("samplestatsrecoverypfcluster",
        "pooledsample.pf.cluster.recovery.count");
    testNameTable.put("samplestatsrecoveryrawcluster",
        "pooledsample.raw.cluster.recovery.count");
    testNameTable.put("samplestatcountsample", "pooledsample.sample.count");
    testNameTable.put("samplestatexceededcontaminationthreshold",
        "pooledsample.fastqscreen.sample.overcontamination.count");

    Map<String, String> result = new LinkedHashMap<>();
    String prefix = "qc.test.";

    for (Map.Entry<String, String> e : runConfiguration.toMap().entrySet()) {

      if (table.containsKey(e.getKey())) {

        result.put(table.get(e.getKey()), e.getValue());
      } else if (e.getKey().startsWith(prefix)) {

        String suffix = e.getKey().substring(prefix.length());

        if (testNameTable.containsKey(suffix)) {
          result.put(prefix + testNameTable.get(suffix), e.getValue());
        } else {
          result.put(e.getKey(), e.getValue());
        }
      } else {
        result.put(e.getKey(), e.getValue());
      }
    }

    return new Settings(result);
  }

}
