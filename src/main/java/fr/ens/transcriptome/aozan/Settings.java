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

package fr.ens.transcriptome.aozan;

/**
 * This class define constants to keys of configuration Aozan file.
 * @since 1.2.1
 * @author Sandrine Perrin
 */
public final class Settings {

  /** */
  public static final String AOZAN_DEBUG_KEY = "aozan.debug";
  /** */
  public static final String AOZAN_ENABLE_KEY = "aozan.enable";
  /** */
  public static final String AOZAN_LOG_LEVEL_KEY = "aozan.log.level";
  /** */
  public static final String AOZAN_LOG_PATH_KEY = "aozan.log.path";
  /** */
  public static final String AOZAN_VAR_PATH_KEY = "aozan.var.path";
  /** */
  public static final String BCL_DATA_PATH_KEY = "bcl.data.path";
  /** */
  public static final String BCL_SPACE_FACTOR_KEY = "bcl.space.factor";
  /** */
  public static final String CASAVA_ADAPTER_FASTA_FILE_PATH_KEY =
      "casava.adapter.fasta.file.path";
  /** */
  public static final String CASAVA_ADDITIONNAL_ARGUMENTS_KEY =
      "casava.additionnal.arguments";
  /** */
  public static final String CASAVA_COMPRESSION_KEY = "casava.compression";
  /** */
  public static final String CASAVA_COMPRESSION_LEVEL_KEY =
      "casava.compression.level";
  /** */
  public static final String CASAVA_FASTQ_CLUSTER_COUNT_KEY =
      "casava.fastq.cluster.count";
  /** */
  public static final String CASAVA_MISMATCHES_KEY = "casava.mismatches";
  /** */
  public static final String CASAVA_PATH_KEY = "casava.path";
  /** */
  public static final String CASAVA_SAMPLESHEET_FORMAT_KEY =
      "casava.samplesheet.format";
  /** */
  public static final String CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY =
      "casava.samplesheet.prefix.filename";
  /** */
  public static final String CASAVA_SAMPLESHEETS_PATH_KEY =
      "casava.samplesheets.path";
  /** */
  public static final String CASAVA_THREADS_KEY = "casava.threads";
  /** */
  public static final String CASAVA_WITH_FAILED_READS_KEY =
      "casava.with.failed.reads";
  /** */
  public static final String DEMUX_SPACE_FACTOR_KEY = "demux.space.factor";
  /** */
  public static final String DEMUX_STEP_KEY = "demux.step";
  /** */
  public static final String DEMUX_USE_HISEQ_OUTPUT_KEY =
      "demux.use.hiseq.output";
  /** */
  public static final String FASTQ_DATA_PATH_KEY = "fastq.data.path";
  /** */
  public static final String FASTQ_SPACE_FACTOR_KEY = "fastq.space.factor";
  /** */
  public static final String FIRST_BASE_REPORT_STEP_KEY =
      "first.base.report.step";
  /** */
  public static final String HISEQ_CRITICAL_MIN_SPACE_KEY =
      "hiseq.critical.min.space";
  /** */
  public static final String HISEQ_DATA_PATH_KEY = "hiseq.data.path";
  /** */
  public static final String HISEQ_SN_KEY = "hiseq.sn";
  /** */
  public static final String HISEQ_SPACE_FACTOR_KEY = "hiseq.space.factor";
  /** */
  public static final String HISEQ_STEP_KEY = "hiseq.step";
  /** */
  public static final String HISEQ_WARNING_MIN_SPACE_KEY =
      "hiseq.warning.min.space";
  /** */
  public static final String INDEX_HTML_TEMPLATE_KEY = "index.html.template";
  /** */
  public static final String INDEX_SEQUENCES_KEY = "index.sequences";
  /** */
  public static final String LOCK_FILE_KEY = "lock.file";
  /** */
  public static final String MAIL_ERROR_TO_KEY = "mail.error.to";
  /** */
  public static final String MAIL_FOOTER_KEY = "mail.footer";
  /** */
  public static final String MAIL_FROM_KEY = "mail.from";
  /** */
  public static final String MAIL_HEADER_KEY = "mail.header";
  /** */
  public static final String MAIL_TO_KEY = "mail.to";

  /** */
  public static final String QC_CONF_FASTQC_CONTAMINANT_FILE_KEY =
      "qc.conf.fastqc.contaminant.file";

  /** */
  public static final String QC_CONF_FASTQC_KMER_SIZE_KEY =
      "qc.conf.fastqc.kmer.size";

  /** */
  public static final String QC_CONF_FASTQC_NOGROUP_KEY =
      "qc.conf.fastqc.nogroup";
  /** */
  public static final String QC_CONF_CLUSTER_DENSITY_RATIO_KEY =
      "qc.conf.cluster.density.ratio";
  /** */
  public static final String QC_CONF_FASTQSCREEN_BLAST_ARGUMENTS_KEY =
      "qc.conf.fastqscreen.blast.arguments";
  /** */
  public static final String QC_CONF_FASTQSCREEN_BLAST_DB_PATH_KEY =
      "qc.conf.fastqscreen.blast.db.path";
  /** */
  public static final String QC_CONF_FASTQSCREEN_BLAST_ENABLE_KEY =
      "qc.conf.fastqscreen.blast.enable";
  /** */
  public static final String QC_CONF_FASTQSCREEN_BLAST_PATH_KEY =
      "qc.conf.fastqscreen.blast.path";
  /** */
  public static final String QC_CONF_FASTQSCREEN_BLAST_VERSION_EXPECTED_KEY =
      "qc.conf.fastqscreen.blast.version.expected";
  /** */
  public static final String QC_CONF_FASTQSCREEN_FASTQ_MAX_READS_PARSED_KEY =
      "qc.conf.fastqscreen.fastq.max.reads.parsed";
  /** */
  public static final String QC_CONF_FASTQSCREEN_FASTQ_READS_PF_USED_KEY =
      "qc.conf.fastqscreen.fastq.reads.pf.used";
  /** */
  public static final String QC_CONF_FASTQSCREEN_GENOMES_KEY =
      "qc.conf.fastqscreen.genomes";
  /** */
  public static final String QC_CONF_FASTQSCREEN_MAPPER_KEY =
      "qc.conf.fastqscreen.mapper";
  /** */
  public static final String QC_CONF_FASTQSCREEN_MAPPER_ARGUMENT_KEY =
      "qc.conf.fastqscreen.mapper.argument";
  /** */
  public static final String QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_MODE_KEY =
      "qc.conf.fastqscreen.mapping.ignore.paired.mode";
  /** */
  public static final String QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY =
      "qc.conf.fastqscreen.mapping.skip.control.lane";
  /** */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_KEY =
      "qc.conf.fastqscreen.settings.genomes";
  /** */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY =
      "qc.conf.fastqscreen.settings.genomes.alias.path";
  /** */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_DESC_PATH_KEY =
      "qc.conf.fastqscreen.settings.genomes.desc.path";
  /** */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_MAPPERS_INDEXES_PATH_KEY =
      "qc.conf.fastqscreen.settings.mappers.indexes.path";
  /** */
  public static final String QC_CONF_FASTQSCREEN_XSL_FILE_KEY =
      "qc.conf.fastqscreen.xsl.file";
  /** */
  public static final String QC_CONF_READ_XML_COLLECTOR_USED_KEY =
      "qc.conf.read.xml.collector.used";
  /** */
  public static final String QC_CONF_THREADS_KEY = "qc.conf.threads";
  /** */
  public static final String QC_REPORT_SAVE_RAW_DATA_KEY =
      "qc.report.save.raw.data";
  /** */
  public static final String QC_REPORT_SAVE_REPORT_DATA_KEY =
      "qc.report.save.report.data";
  /** */
  public static final String QC_REPORT_STYLESHEET_KEY = "qc.report.stylesheet";
  /** */
  public static final String QC_STEP_KEY = "qc.step";

  /** */
  public static final String REPORTS_DATA_PATH_KEY = "reports.data.path";
  /** */
  public static final String REPORTS_URL_KEY = "reports.url";
  /** */
  public static final String SEND_MAIL_KEY = "send.mail";
  /** */
  public static final String SMTP_SERVER_KEY = "smtp.server";
  /** */
  public static final String SYNC_CONTINUOUS_SYNC_KEY = "sync.continuous.sync";
  /** */
  public static final String SYNC_CONTINUOUS_SYNC_MIN_AGE_FILES_KEY =
      "sync.continuous.sync.min.age.files";
  /** */
  public static final String SYNC_EXCLUDE_CIF_KEY = "sync.exclude.cif";
  /** */
  public static final String SYNC_SPACE_FACTOR_KEY = "sync.space.factor";
  /** */
  public static final String SYNC_STEP_KEY = "sync.step";
  /** */
  public static final String TMP_PATH_KEY = "tmp.path";

  /** */
  public static final String QC_TEST_BADTILES_ENABLE_KEY =
      "qc.test.badtiles.enable";
  /** */
  public static final String QC_TEST_BASICSTATS_ENABLE_KEY =
      "qc.test.basicstats.enable";
  /** */
  public static final String QC_TEST_CLUSTERDENSITY_ENABLE_KEY =
      "qc.test.clusterdensity.enable";
  /** */
  public static final String QC_TEST_DUPLICATIONLEVEL_ENABLE_KEY =
      "qc.test.duplicationlevel.enable";
  /** */
  public static final String QC_TEST_ERRORRATE100CYCLE_ENABLE_KEY =
      "qc.test.errorrate100cycle.enable";
  /** */
  public static final String QC_TEST_ERRORRATE100CYCLE_INTERVAL_KEY =
      "qc.test.errorrate100cycle.interval";
  /** */
  public static final String QC_TEST_ERRORRATE35CYCLE_ENABLE_KEY =
      "qc.test.errorrate35cycle.enable";
  /** */
  public static final String QC_TEST_ERRORRATE35CYCLE_INTERVAL_KEY =
      "qc.test.errorrate35cycle.interval";
  /** */
  public static final String QC_TEST_ERRORRATE75CYCLE_ENABLE_KEY =
      "qc.test.errorrate75cycle.enable";
  /** */
  public static final String QC_TEST_ERRORRATE75CYCLE_INTERVAL_KEY =
      "qc.test.errorrate75cycle.interval";
  /** */
  public static final String QC_TEST_ERRORRATE_ENABLE_KEY =
      "qc.test.errorrate.enable";
  /** */
  public static final String QC_TEST_FIRSTCYCLEINTENSITY_ENABLE_KEY =
      "qc.test.firstcycleintensity.enable";
  /** */
  public static final String QC_TEST_FSQMAPPED_ENABLE_KEY =
      "qc.test.fsqmapped.enable";
  /** */
  public static final String QC_TEST_FSQMAPPED_INTERVAL_KEY =
      "qc.test.fsqmapped.interval";
  /** */
  public static final String QC_TEST_HITNOLIBRARIES_ENABLE_KEY =
      "qc.test.hitnolibraries.enable";
  /** */
  public static final String QC_TEST_HITNOLIBRARIES_INTERVAL_KEY =
      "qc.test.hitnolibraries.interval";
  /** */
  public static final String QC_TEST_KMERCONTENT_ENABLE_KEY =
      "qc.test.kmercontent.enable";
  /** */
  public static final String QC_TEST_LINKREPORT_ENABLE_KEY =
      "qc.test.linkreport.enable";
  /** */
  public static final String QC_TEST_MEANQUALITYSCOREPF_ENABLE_KEY =
      "qc.test.meanqualityscorepf.enable";
  /** */
  public static final String QC_TEST_NCONTENT_ENABLE_KEY =
      "qc.test.ncontent.enable";
  /** */
  public static final String QC_TEST_OVERREPRESENTEDSEQS_ENABLE_KEY =
      "qc.test.overrepresentedseqs.enable";
  /** */
  public static final String QC_TEST_PERBASEGCCONTENT_ENABLE_KEY =
      "qc.test.perbasegccontent.enable";
  /** */
  public static final String QC_TEST_PERBASEQUALITYSCORES_ENABLE_KEY =
      "qc.test.perbasequalityscores.enable";
  /** */
  public static final String QC_TEST_PERBASESEQUENCECONTENT_ENABLE_KEY =
      "qc.test.perbasesequencecontent.enable";
  /** */
  public static final String QC_TEST_PERCENTALIGN_ENABLE_KEY =
      "qc.test.percentalign.enable";
  /** */
  public static final String QC_TEST_PERCENTALIGN_INTERVAL_KEY =
      "qc.test.percentalign.interval";
  /** */
  public static final String QC_TEST_PERCENTINLANESAMPLE_ENABLE_KEY =
      "qc.test.percentinlanesample.enable";
  /** */
  public static final String QC_TEST_PERCENTINLANESAMPLE_DISTANCE_KEY =
      "qc.test.percentinlanesample.distance";

  /** */
  public static final String QC_TEST_PERCENTINTENSITYCYCLE20_ENABLE_KEY =
      "qc.test.percentintensitycycle20.enable";
  /** */
  public static final String QC_TEST_PERCENTINTENSITYCYCLE20_INTERVAL_KEY =
      "qc.test.percentintensitycycle20.interval";
  /** */
  public static final String QC_TEST_PERCENTPFSAMPLE_ENABLE_KEY =
      "qc.test.percentpfsample.enable";
  /** */
  public static final String QC_TEST_PERCENTPFSAMPLE_INTERVAL_KEY =
      "qc.test.percentpfsample.interval";
  /** */
  public static final String QC_TEST_PERCENTQ30_ENABLE_KEY =
      "qc.test.percentq30.enable";
  /** */
  public static final String QC_TEST_PERCENTQ30_INTERVAL_KEY =
      "qc.test.percentq30.interval";
  /** */
  public static final String QC_TEST_PERSEQUENCEGCCONTENT_ENABLE_KEY =
      "qc.test.persequencegccontent.enable";
  /** */
  public static final String QC_TEST_PERSEQUENCEQUALITYSCORES_ENABLE_KEY =
      "qc.test.persequencequalityscores.enable";
  /** */
  public static final String QC_TEST_PFCLUSTERS_ENABLE_KEY =
      "qc.test.pfclusters.enable";
  /** */
  public static final String QC_TEST_PFCLUSTERSPERCENT_ENABLE_KEY =
      "qc.test.pfclusterspercent.enable";
  /** */
  public static final String QC_TEST_PFCLUSTERSPERCENT_INTERVAL_KEY =
      "qc.test.pfclusterspercent.interval";
  /** */
  public static final String QC_TEST_PFCLUSTERSSAMPLES_ENABLE_KEY =
      "qc.test.pfclusterssamples.enable";
  /** */
  public static final String QC_TEST_PFCLUSTERSSAMPLES_INTERVAL_KEY =
      "qc.test.pfclusterssamples.interval";
  /** */
  public static final String QC_TEST_PHASINGPREPHASING_ENABLE_KEY =
      "qc.test.phasingprephasing.enable";
  /** */
  public static final String QC_TEST_PHASINGPREPHASING_PHASING_INTERVAL_KEY =
      "qc.test.phasingprephasing.phasing.interval";
  /** */
  public static final String QC_TEST_PHASINGPREPHASING_PREPHASING_INTERVAL_KEY =
      "qc.test.phasingprephasing.prephasing.interval";
  /** */
  public static final String QC_TEST_RAWCLUSTERS_ENABLE_KEY =
      "qc.test.rawclusters.enable";
  /** */
  public static final String QC_TEST_RAWCLUSTERSSAMPLES_ENABLE_KEY =
      "qc.test.rawclusterssamples.enable";
  /** */
  public static final String QC_TEST_SEQUENCELENGTHDISTRIBUTION_ENABLE_KEY =
      "qc.test.sequencelengthdistribution.enable";

}
