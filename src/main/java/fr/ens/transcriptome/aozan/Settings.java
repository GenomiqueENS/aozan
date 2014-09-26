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

  /** Mode Aozan debug */
  public static final String AOZAN_DEBUG_KEY = "aozan.debug";
  /** Aozan enable */
  public static final String AOZAN_ENABLE_KEY = "aozan.enable";
  /** Aozan log level */
  public static final String AOZAN_LOG_LEVEL_KEY = "aozan.log.level";
  /** Aozan log path */
  public static final String AOZAN_LOG_PATH_KEY = "aozan.log.path";
  /** Aozan directory var path */
  public static final String AOZAN_VAR_PATH_KEY = "aozan.var.path";
  /** Aozan directory bcl path */
  public static final String BCL_DATA_PATH_KEY = "bcl.data.path";
  /** Aozan bcl space factor */
  public static final String BCL_SPACE_FACTOR_KEY = "bcl.space.factor";
  /** Hiseq critical minimum space */
  public static final String HISEQ_CRITICAL_MIN_SPACE_KEY =
      "hiseq.critical.min.space";
  /** hiseq directory data path */
  public static final String HISEQ_DATA_PATH_KEY = "hiseq.data.path";
  /** Hiseq SN */
  public static final String HISEQ_SN_KEY = "hiseq.sn";
  /** Hiseq space factor */
  public static final String HISEQ_SPACE_FACTOR_KEY = "hiseq.space.factor";
  /** Hiseq step */
  public static final String HISEQ_STEP_KEY = "hiseq.step";
  /** Hiseq warning minimum space */
  public static final String HISEQ_WARNING_MIN_SPACE_KEY =
      "hiseq.warning.min.space";
  /** Casava adapter fasta file path */
  public static final String CASAVA_ADAPTER_FASTA_FILE_PATH_KEY =
      "casava.adapter.fasta.file.path";
  /** Casava additionnal arguments */
  public static final String CASAVA_ADDITIONNAL_ARGUMENTS_KEY =
      "casava.additionnal.arguments";
  /** Casava compression fastq files */
  public static final String CASAVA_COMPRESSION_KEY = "casava.compression";
  /** Casava compression level */
  public static final String CASAVA_COMPRESSION_LEVEL_KEY =
      "casava.compression.level";
  /** Casava fastq cluster count */
  public static final String CASAVA_FASTQ_CLUSTER_COUNT_KEY =
      "casava.fastq.cluster.count";
  /** Casava mismatches */
  public static final String CASAVA_MISMATCHES_KEY = "casava.mismatches";
  /** Casava path */
  public static final String CASAVA_PATH_KEY = "casava.path";
  /** Casava samplesheet format */
  public static final String CASAVA_SAMPLESHEET_FORMAT_KEY =
      "casava.samplesheet.format";
  /** Casava samplesheet prefix filename */
  public static final String CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY =
      "casava.samplesheet.prefix.filename";
  /** Casava samplesheet path */
  public static final String CASAVA_SAMPLESHEETS_PATH_KEY =
      "casava.samplesheets.path";
  /** Casava threads */
  public static final String CASAVA_THREADS_KEY = "casava.threads";
  /** Casava with failed reads */
  public static final String CASAVA_WITH_FAILED_READS_KEY =
      "casava.with.failed.reads";
  /** Casava design generator command */
  public static final String CASAVA_DESIGN_GENERATOR_COMMAND_KEY =
      "casava.design.generator.command";

  /** Demultiplex space factor */
  public static final String DEMUX_SPACE_FACTOR_KEY = "demux.space.factor";
  /** Demultiplex step */
  public static final String DEMUX_STEP_KEY = "demux.step";
  /** Demultiplex use Hiseq output */
  public static final String DEMUX_USE_HISEQ_OUTPUT_KEY =
      "demux.use.hiseq.output";
  /** Fastq directory data path */
  public static final String FASTQ_DATA_PATH_KEY = "fastq.data.path";
  /** Fastq space factor */
  public static final String FASTQ_SPACE_FACTOR_KEY = "fastq.space.factor";

  /** First base report step */
  public static final String FIRST_BASE_REPORT_STEP_KEY =
      "first.base.report.step";

  /** Index html template path */
  public static final String INDEX_HTML_TEMPLATE_KEY = "index.html.template";
  /** Index sequences path */
  public static final String INDEX_SEQUENCES_KEY = "index.sequences";
  /** Lock file */
  public static final String LOCK_FILE_KEY = "lock.file";
  /** Mail error to */
  public static final String MAIL_ERROR_TO_KEY = "mail.error.to";
  /** Mail footer */
  public static final String MAIL_FOOTER_KEY = "mail.footer";
  /** Mail from */
  public static final String MAIL_FROM_KEY = "mail.from";
  /** Mail header */
  public static final String MAIL_HEADER_KEY = "mail.header";
  /** Mail to */
  public static final String MAIL_TO_KEY = "mail.to";
  /** QC report save raw data */
  public static final String QC_REPORT_SAVE_RAW_DATA_KEY =
      "qc.report.save.raw.data";
  /** QC report save report data */
  public static final String QC_REPORT_SAVE_REPORT_DATA_KEY =
      "qc.report.save.report.data";
  /** QC report stylesheet path */
  public static final String QC_REPORT_STYLESHEET_KEY = "qc.report.stylesheet";
  /** QC step */
  public static final String QC_STEP_KEY = "qc.step";
  /** Report directory data path */
  public static final String REPORTS_DATA_PATH_KEY = "reports.data.path";
  /** Report url */
  public static final String REPORTS_URL_KEY = "reports.url";
  /** Send mail */
  public static final String SEND_MAIL_KEY = "send.mail";
  /** SMTP server */
  public static final String SMTP_SERVER_KEY = "smtp.server";
  /** Synchronization continuous */
  public static final String SYNC_CONTINUOUS_SYNC_KEY = "sync.continuous.sync";
  /** Synchronization continuous minimum ages files */
  public static final String SYNC_CONTINUOUS_SYNC_MIN_AGE_FILES_KEY =
      "sync.continuous.sync.min.age.files";
  /** Synchronization exclude cif files */
  public static final String SYNC_EXCLUDE_CIF_KEY = "sync.exclude.cif";
  /** Synchronization space factor */
  public static final String SYNC_SPACE_FACTOR_KEY = "sync.space.factor";
  /** Synchronization step */
  public static final String SYNC_STEP_KEY = "sync.step";
  /** Directory tmp path */
  public static final String TMP_PATH_KEY = "tmp.path";

  /** Configuration collector */
  /** Collector FastQC contaminant file */
  public static final String QC_CONF_FASTQC_CONTAMINANT_FILE_KEY =
      "qc.conf.fastqc.contaminant.file";
  /** Collector FastQC kmer size */
  public static final String QC_CONF_FASTQC_KMER_SIZE_KEY =
      "qc.conf.fastqc.kmer.size";
  /** Collector FastQC nogroup */
  public static final String QC_CONF_FASTQC_NOGROUP_KEY =
      "qc.conf.fastqc.nogroup";
  /** Collector cluster density ratio */
  public static final String QC_CONF_CLUSTER_DENSITY_RATIO_KEY =
      "qc.conf.cluster.density.ratio";
  /** Collector fastqscreen blast arguments */
  public static final String QC_CONF_FASTQSCREEN_BLAST_ARGUMENTS_KEY =
      "qc.conf.fastqscreen.blast.arguments";
  /** Collector fastqscreen blast database path */
  public static final String QC_CONF_FASTQSCREEN_BLAST_DB_PATH_KEY =
      "qc.conf.fastqscreen.blast.db.path";
  /** Collector fastqscreen blast enable */
  public static final String QC_CONF_FASTQSCREEN_BLAST_ENABLE_KEY =
      "qc.conf.fastqscreen.blast.enable";
  /** Collector fastqscreen blast path */
  public static final String QC_CONF_FASTQSCREEN_BLAST_PATH_KEY =
      "qc.conf.fastqscreen.blast.path";
  /** Collector fastqscreen blast version expected */
  public static final String QC_CONF_FASTQSCREEN_BLAST_VERSION_EXPECTED_KEY =
      "qc.conf.fastqscreen.blast.version.expected";
  /** Collector fastqscreen fastq max reads parsed */
  public static final String QC_CONF_FASTQSCREEN_FASTQ_MAX_READS_PARSED_KEY =
      "qc.conf.fastqscreen.fastq.max.reads.parsed";
  /** Collector fastqscreen fastq reads pf used */
  public static final String QC_CONF_FASTQSCREEN_FASTQ_READS_PF_USED_KEY =
      "qc.conf.fastqscreen.fastq.reads.pf.used";
  /** Collector fastqscreen genomes */
  public static final String QC_CONF_FASTQSCREEN_GENOMES_KEY =
      "qc.conf.fastqscreen.genomes";
  /** Collector fastqscreen mapper */
  public static final String QC_CONF_FASTQSCREEN_MAPPER_KEY =
      "qc.conf.fastqscreen.mapper";
  /** Collector fastqscreen mapper argument */
  public static final String QC_CONF_FASTQSCREEN_MAPPER_ARGUMENT_KEY =
      "qc.conf.fastqscreen.mapper.argument";
  /** Collector fastqscreen mapping ignore paired mode */
  public static final String QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_MODE_KEY =
      "qc.conf.fastqscreen.mapping.ignore.paired.mode";
  /** Collector fastqscreen mapping skip control lane */
  public static final String QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY =
      "qc.conf.fastqscreen.mapping.skip.control.lane";
  /** Collector fastqscreen settings genomes */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_KEY =
      "qc.conf.fastqscreen.settings.genomes";
  /** Collector fastqscreen settings genomes alias path */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY =
      "qc.conf.fastqscreen.settings.genomes.alias.path";
  /** Collector fastqscreen settings genomes description path */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_DESC_PATH_KEY =
      "qc.conf.fastqscreen.settings.genomes.desc.path";
  /** Collector fastqscreen settings mappers indexes path */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_MAPPERS_INDEXES_PATH_KEY =
      "qc.conf.fastqscreen.settings.mappers.indexes.path";
  /** Collector fastqscreen xsl file */
  public static final String QC_CONF_FASTQSCREEN_XSL_FILE_KEY =
      "qc.conf.fastqscreen.xsl.file";
  /** Collector read xml collector used */
  public static final String QC_CONF_READ_XML_COLLECTOR_USED_KEY =
      "qc.conf.read.xml.collector.used";
  /** Collector threads */
  public static final String QC_CONF_THREADS_KEY = "qc.conf.threads";

  /** Collector undetermined indexed xsl file */
  public static final String QC_CONF_UNDETERMINED_INDEXED_XSL_FILE_KEY =
      "qc.conf.undetermined.indexed.xsl.file";

  /** Configuration test */
  /** Test bad tiles enable */
  public static final String QC_TEST_BADTILES_ENABLE_KEY =
      "qc.test.badtiles.enable";
  /** Test basic stats enable */
  public static final String QC_TEST_BASICSTATS_ENABLE_KEY =
      "qc.test.basicstats.enable";
  /** Test cluster density enable */
  public static final String QC_TEST_CLUSTERDENSITY_ENABLE_KEY =
      "qc.test.clusterdensity.enable";
  /** Test duplication level enable */
  public static final String QC_TEST_DUPLICATIONLEVEL_ENABLE_KEY =
      "qc.test.duplicationlevel.enable";
  /** Test error rate 100 cycle enable */
  public static final String QC_TEST_ERRORRATE100CYCLE_ENABLE_KEY =
      "qc.test.errorrate100cycle.enable";
  /** Test error rate 100 cycle interval */
  public static final String QC_TEST_ERRORRATE100CYCLE_INTERVAL_KEY =
      "qc.test.errorrate100cycle.interval";
  /** Test error rate 35 cycle enable */
  public static final String QC_TEST_ERRORRATE35CYCLE_ENABLE_KEY =
      "qc.test.errorrate35cycle.enable";
  /** Test error rate 35 cycle interval */
  public static final String QC_TEST_ERRORRATE35CYCLE_INTERVAL_KEY =
      "qc.test.errorrate35cycle.interval";
  /** Test error rate 75 cycle enable */
  public static final String QC_TEST_ERRORRATE75CYCLE_ENABLE_KEY =
      "qc.test.errorrate75cycle.enable";
  /** Test error rate 75 cycle interval */
  public static final String QC_TEST_ERRORRATE75CYCLE_INTERVAL_KEY =
      "qc.test.errorrate75cycle.interval";
  /** Test error rate enable */
  public static final String QC_TEST_ERRORRATE_ENABLE_KEY =
      "qc.test.errorrate.enable";
  /** Test first cycle intensity enable */
  public static final String QC_TEST_FIRSTCYCLEINTENSITY_ENABLE_KEY =
      "qc.test.firstcycleintensity.enable";
  /** Test fastqscreen mapped enable */
  public static final String QC_TEST_FSQMAPPED_ENABLE_KEY =
      "qc.test.fsqmapped.enable";
  /** Test fastqscreen mapped interval */
  public static final String QC_TEST_FSQMAPPED_INTERVAL_KEY =
      "qc.test.fsqmapped.interval";
  /** Test hit no libraries enable */
  public static final String QC_TEST_HITNOLIBRARIES_ENABLE_KEY =
      "qc.test.hitnolibraries.enable";
  /** Test hit no libraries interval */
  public static final String QC_TEST_HITNOLIBRARIES_INTERVAL_KEY =
      "qc.test.hitnolibraries.interval";
  /** Test kmer content enable */
  public static final String QC_TEST_KMERCONTENT_ENABLE_KEY =
      "qc.test.kmercontent.enable";
  /** Test link report enable */
  public static final String QC_TEST_LINKREPORT_ENABLE_KEY =
      "qc.test.linkreport.enable";
  /** Test mean quality score passing filter enable */
  public static final String QC_TEST_MEANQUALITYSCOREPF_ENABLE_KEY =
      "qc.test.meanqualityscorepf.enable";
  /** Test 'n' content enable */
  public static final String QC_TEST_NCONTENT_ENABLE_KEY =
      "qc.test.ncontent.enable";
  /** Test over represented sequences enable */
  public static final String QC_TEST_OVERREPRESENTEDSEQS_ENABLE_KEY =
      "qc.test.overrepresentedseqs.enable";
  /** Test per base GC content enable */
  public static final String QC_TEST_PERBASEGCCONTENT_ENABLE_KEY =
      "qc.test.perbasegccontent.enable";
  /** Test per base quality scores enable */
  public static final String QC_TEST_PERBASEQUALITYSCORES_ENABLE_KEY =
      "qc.test.perbasequalityscores.enable";
  /** Test per base sequence content enable */
  public static final String QC_TEST_PERBASESEQUENCECONTENT_ENABLE_KEY =
      "qc.test.perbasesequencecontent.enable";
  /** Test percent alignment enable */
  public static final String QC_TEST_PERCENTALIGN_ENABLE_KEY =
      "qc.test.percentalign.enable";
  /** Test percent alignment interval */
  public static final String QC_TEST_PERCENTALIGN_INTERVAL_KEY =
      "qc.test.percentalign.interval";
  /** Test percent in lane sample enable */
  public static final String QC_TEST_PERCENTINLANESAMPLE_ENABLE_KEY =
      "qc.test.percentinlanesample.enable";
  /** Test percent in lane sample distance */
  public static final String QC_TEST_PERCENTINLANESAMPLE_DISTANCE_KEY =
      "qc.test.percentinlanesample.distance";
  /** Test percent intensity cycle 20 enable */
  public static final String QC_TEST_PERCENTINTENSITYCYCLE20_ENABLE_KEY =
      "qc.test.percentintensitycycle20.enable";
  /** Test percent intensity cycle 20 interval */
  public static final String QC_TEST_PERCENTINTENSITYCYCLE20_INTERVAL_KEY =
      "qc.test.percentintensitycycle20.interval";
  /** Test percent passing filter sample enable */
  public static final String QC_TEST_PERCENTPFSAMPLE_ENABLE_KEY =
      "qc.test.percentpfsample.enable";
  /** Test percent passing filter sample interval */
  public static final String QC_TEST_PERCENTPFSAMPLE_INTERVAL_KEY =
      "qc.test.percentpfsample.interval";
  /** Test percent Q30 enable */
  public static final String QC_TEST_PERCENTQ30_ENABLE_KEY =
      "qc.test.percentq30.enable";
  /** Test percent Q30 interval */
  public static final String QC_TEST_PERCENTQ30_INTERVAL_KEY =
      "qc.test.percentq30.interval";
  /** Test per sequence GC content enable */
  public static final String QC_TEST_PERSEQUENCEGCCONTENT_ENABLE_KEY =
      "qc.test.persequencegccontent.enable";
  /** Test per sequence quality scores enable */
  public static final String QC_TEST_PERSEQUENCEQUALITYSCORES_ENABLE_KEY =
      "qc.test.persequencequalityscores.enable";
  /** Test passing filter clusters enable */
  public static final String QC_TEST_PFCLUSTERS_ENABLE_KEY =
      "qc.test.pfclusters.enable";
  /** Test passing filter clusters percent enable */
  public static final String QC_TEST_PFCLUSTERSPERCENT_ENABLE_KEY =
      "qc.test.pfclusterspercent.enable";
  /** Test passing filter clusters percent interval */
  public static final String QC_TEST_PFCLUSTERSPERCENT_INTERVAL_KEY =
      "qc.test.pfclusterspercent.interval";
  /** Test passing filter clusters samples enable */
  public static final String QC_TEST_PFCLUSTERSSAMPLES_ENABLE_KEY =
      "qc.test.pfclusterssamples.enable";
  /** Test passing filter clusters samples interval */
  public static final String QC_TEST_PFCLUSTERSSAMPLES_INTERVAL_KEY =
      "qc.test.pfclusterssamples.interval";
  /** Test phasing prephasing enable */
  public static final String QC_TEST_PHASINGPREPHASING_ENABLE_KEY =
      "qc.test.phasingprephasing.enable";
  /** Test phasing prephasing phasing interval */
  public static final String QC_TEST_PHASINGPREPHASING_PHASING_INTERVAL_KEY =
      "qc.test.phasingprephasing.phasing.interval";
  /** Test phasing prephasing prephasing interval */
  public static final String QC_TEST_PHASINGPREPHASING_PREPHASING_INTERVAL_KEY =
      "qc.test.phasingprephasing.prephasing.interval";
  /** Test raw clusters enable */
  public static final String QC_TEST_RAWCLUSTERS_ENABLE_KEY =
      "qc.test.rawclusters.enable";
  /** Test raw clusters samples enable */
  public static final String QC_TEST_RAWCLUSTERSSAMPLES_ENABLE_KEY =
      "qc.test.rawclusterssamples.enable";
  /** Test sequence length distribution enable */
  public static final String QC_TEST_SEQUENCELENGTHDISTRIBUTION_ENABLE_KEY =
      "qc.test.sequencelengthdistribution.enable";

}
