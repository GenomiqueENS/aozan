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

package fr.ens.biologie.genomique.aozan;

import java.util.Collections;
import java.util.Map;

/**
 * This class define constants to keys of configuration Aozan file.
 * @since 1.2.1
 * @author Sandrine Perrin
 */
public final class Settings {

  /** Mode Aozan debug. */
  public static final String AOZAN_DEBUG_KEY = "aozan.debug";
  /** Aozan enable. */
  public static final String AOZAN_ENABLE_KEY = "aozan.enable";
  /** Aozan log level. */
  public static final String AOZAN_LOG_LEVEL_KEY = "aozan.log.level";
  /** Aozan log path. */
  public static final String AOZAN_LOG_PATH_KEY = "aozan.log.path";
  /** Aozan directory var path. */
  public static final String AOZAN_VAR_PATH_KEY = "aozan.var.path";

  public static final String AOZAN_CONF_FILE_PATH =
      "aozan.configuration.file.path";

  /** Aozan directory bcl path. */
  public static final String BCL_DATA_PATH_KEY = "bcl.data.path";
  /** Aozan bcl space factor. */
  public static final String BCL_SPACE_FACTOR_KEY = "bcl.space.factor";
  /** Hiseq critical minimum space. */
  public static final String HISEQ_CRITICAL_MIN_SPACE_KEY =
      "hiseq.critical.min.space";
  /** hiseq directory data path. */
  public static final String HISEQ_DATA_PATH_KEY = "hiseq.data.path";
  /** Hiseq SN. */
  public static final String HISEQ_SN_KEY = "hiseq.sn";
  /** Hiseq space factor. */
  public static final String HISEQ_SPACE_FACTOR_KEY = "hiseq.space.factor";
  /** Hiseq step. */
  public static final String HISEQ_STEP_KEY = "hiseq.step";
  /** Hiseq warning minimum space. */
  public static final String HISEQ_WARNING_MIN_SPACE_KEY =
      "hiseq.warning.min.space";
  /** Casava adapter fasta file path. */
  public static final String CASAVA_ADAPTER_FASTA_FILE_PATH_KEY =
      "casava.adapter.fasta.file.path";
  /** Casava additionnal arguments. */
  public static final String CASAVA_ADDITIONNAL_ARGUMENTS_KEY =
      "casava.additionnal.arguments";
  /** Casava compression fastq files. */
  public static final String CASAVA_COMPRESSION_KEY = "casava.compression";
  /** Casava compression level. */
  public static final String CASAVA_COMPRESSION_LEVEL_KEY =
      "casava.compression.level";
  /** Casava fastq cluster count. */
  public static final String CASAVA_FASTQ_CLUSTER_COUNT_KEY =
      "casava.fastq.cluster.count";
  /** Casava mismatches. */
  public static final String CASAVA_MISMATCHES_KEY = "casava.mismatches";
  /** Casava path. */
  public static final String CASAVA_PATH_KEY = "casava.path";
  /** Casava samplesheet format. */
  public static final String CASAVA_SAMPLESHEET_FORMAT_KEY =
      "casava.samplesheet.format";
  /** Casava samplesheet prefix filename. */
  public static final String CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY =
      "casava.samplesheet.prefix.filename";
  /** Casava samplesheet path. */
  public static final String CASAVA_SAMPLESHEETS_PATH_KEY =
      "casava.samplesheets.path";
  /** Casava threads. */
  public static final String CASAVA_THREADS_KEY = "casava.threads";
  /** Casava with failed reads. */
  public static final String CASAVA_WITH_FAILED_READS_KEY =
      "casava.with.failed.reads";
  /** Casava design generator command. */
  public static final String CASAVA_DESIGN_GENERATOR_COMMAND_KEY =
      "casava.design.generator.command";

  /** Set available use container docker. */
  public static final String DEMUX_USE_DOCKER_ENABLE_KEY =
      "demux.use.docker.enable";
  /** Set bcl2fastq version for HiSeq sequencer. */
  public static final String BCL2FASTQ_VERSION_FOR_HISEQ_KEY =
      "bcl2fastq.version.for.hiseq";
  /** Set bcl2fastq version for NextSeq sequencer. */
  public static final String BCL2FASTQ_VERSION_FOR_NEXTSEQ_KEY =
      "bcl2fastq.version.for.nextseq";

  /** Demultiplex space factor. */
  public static final String DEMUX_SPACE_FACTOR_KEY = "demux.space.factor";
  /** Demultiplex step. */
  public static final String DEMUX_STEP_KEY = "demux.step";
  /** Demultiplex use Hiseq output. */
  public static final String DEMUX_USE_HISEQ_OUTPUT_KEY =
      "demux.use.hiseq.output";
  /** Fastq directory data path. */
  public static final String FASTQ_DATA_PATH_KEY = "fastq.data.path";
  /** Fastq space factor. */
  public static final String FASTQ_SPACE_FACTOR_KEY = "fastq.space.factor";

  /** First base report step. */
  public static final String FIRST_BASE_REPORT_STEP_KEY =
      "first.base.report.step";

  /** Index html template path. */
  public static final String INDEX_HTML_TEMPLATE_KEY = "index.html.template";
  /** Index sequences path. */
  public static final String INDEX_SEQUENCES_KEY = "index.sequences";
  /** Lock file. */
  public static final String LOCK_FILE_KEY = "lock.file";
  /** Mail error to. */
  public static final String MAIL_ERROR_TO_KEY = "mail.error.to";
  /** Mail footer. */
  public static final String MAIL_FOOTER_KEY = "mail.footer";
  /** Mail from. */
  public static final String MAIL_FROM_KEY = "mail.from";
  /** Mail header. */
  public static final String MAIL_HEADER_KEY = "mail.header";
  /** Mail to. */
  public static final String MAIL_TO_KEY = "mail.to";
  /** QC report save raw data. */
  public static final String QC_REPORT_SAVE_RAW_DATA_KEY =
      "qc.report.save.raw.data";
  /** QC report save report data. */
  public static final String QC_REPORT_SAVE_REPORT_DATA_KEY =
      "qc.report.save.report.data";
  /** QC report stylesheet path. */
  public static final String QC_REPORT_STYLESHEET_KEY = "qc.report.stylesheet";
  /** QC step. */
  public static final String QC_STEP_KEY = "qc.step";
  /** Report directory data path. */
  public static final String REPORTS_DATA_PATH_KEY = "reports.data.path";
  /** Report url. */
  public static final String REPORTS_URL_KEY = "reports.url";
  /** Send mail. */
  public static final String SEND_MAIL_KEY = "send.mail";
  /** SMTP server. */
  public static final String SMTP_SERVER_KEY = "smtp.server";
  /** Synchronization continuous. */
  public static final String SYNC_CONTINUOUS_SYNC_KEY = "sync.continuous.sync";
  /** Synchronization continuous minimum ages files. */
  public static final String SYNC_CONTINUOUS_SYNC_MIN_AGE_FILES_KEY =
      "sync.continuous.sync.min.age.files";
  /** Synchronization exclude cif files. */
  public static final String SYNC_EXCLUDE_CIF_KEY = "sync.exclude.cif";
  /** Synchronization space factor. */
  public static final String SYNC_SPACE_FACTOR_KEY = "sync.space.factor";
  /** Synchronization step. */
  public static final String SYNC_STEP_KEY = "sync.step";
  /** Directory tmp path. */
  public static final String TMP_PATH_KEY = "tmp.path";

  /** Configuration collector. */
  /** Collector FastQC contaminant file. */
  public static final String QC_CONF_FASTQC_CONTAMINANT_FILE_KEY =
      "qc.conf.fastqc.contaminant.file";
  /** Collector FastQC kmer size. */
  public static final String QC_CONF_FASTQC_KMER_SIZE_KEY =
      "qc.conf.fastqc.kmer.size";
  /** Collector FastQC nogroup. */
  public static final String QC_CONF_FASTQC_NOGROUP_KEY =
      "qc.conf.fastqc.nogroup";
  /** Path to a specific adapter file, replace default file. */
  public static final String QC_CONF_FASTQC_ADAPTER_FILE_KEY =
      "qc.conf.fastqc.adapter.file";
  /** Path to a specific limits file, replace default file. */
  public static final String QC_CONF_FASTQC_LIMITS_FILE_KEY =
      "qc.conf.fastqc.limits.file";
  /** Use exponential base groups in graph. */
  public static final String QC_CONF_FASTQC_EXPGROUP_KEY =
      "qc.conf.fastqc.expgroup";
  /** Format fastq type casava/Illumina. */
  public static final String QC_CONF_FASTQC_CASAVA_KEY =
      "qc.conf.fastqc.casava";
  /** Option for filter fastq file if casava=true for all modules. */
  public static final String QC_CONF_FASTQC_NOFILTER_KEY =
      "qc.conf.fastqc.nofilter";
  /**
   * New option with FastQC 0.11.3 to nanopore technology, use Fast5 file
   * instead of FastQ
   */
  public static final String QC_CONF_FASTQC_NANO_KEY = "qc.conf.fastqc.nano";

  /** Collector cluster density ratio. */
  public static final String QC_CONF_CLUSTER_DENSITY_RATIO_KEY =
      "qc.conf.cluster.density.ratio";
  /** Collector fastqscreen blast arguments. */
  public static final String QC_CONF_FASTQSCREEN_BLAST_ARGUMENTS_KEY =
      "qc.conf.fastqscreen.blast.arguments";
  /** Collector fastqscreen blast database path. */
  public static final String QC_CONF_FASTQSCREEN_BLAST_DB_PATH_KEY =
      "qc.conf.fastqscreen.blast.db.path";
  /** Collector fastqscreen blast enable. */
  public static final String QC_CONF_FASTQSCREEN_BLAST_ENABLE_KEY =
      "qc.conf.fastqscreen.blast.enable";
  /** Collector fastqscreen blast path. */
  public static final String QC_CONF_FASTQSCREEN_BLAST_PATH_KEY =
      "qc.conf.fastqscreen.blast.path";
  /** Collector fastqscreen blast version expected. */
  public static final String QC_CONF_FASTQSCREEN_BLAST_VERSION_EXPECTED_KEY =
      "qc.conf.fastqscreen.blast.version.expected";
  /** Collector fastqscreen fastq max reads parsed. */
  public static final String QC_CONF_FASTQSCREEN_FASTQ_MAX_READS_PARSED_KEY =
      "qc.conf.fastqscreen.fastq.max.reads.parsed";
  /** Collector fastqscreen fastq reads pf used. */
  public static final String QC_CONF_FASTQSCREEN_FASTQ_READS_PF_USED_KEY =
      "qc.conf.fastqscreen.fastq.reads.pf.used";
  /** Collector fastqscreen genomes. */
  public static final String QC_CONF_FASTQSCREEN_GENOMES_KEY =
      "qc.conf.fastqscreen.genomes";
  /** Collector fastqscreen mapper. */
  public static final String QC_CONF_FASTQSCREEN_MAPPER_KEY =
      "qc.conf.fastqscreen.mapper";
  /** Collector fastqscreen mapper argument. */
  public static final String QC_CONF_FASTQSCREEN_MAPPER_ARGUMENT_KEY =
      "qc.conf.fastqscreen.mapper.argument";
  /** Collector fastqscreen mapping ignore paired mode. */
  public static final String QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_MODE_KEY =
      "qc.conf.fastqscreen.mapping.ignore.paired.mode";
  /** Collector fastqscreen mapping skip control lane. */
  public static final String QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY =
      "qc.conf.fastqscreen.mapping.skip.control.lane";
  /** Collector fastqscreen settings genomes. */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_KEY =
      "qc.conf.fastqscreen.settings.genomes";
  /** Collector fastqscreen settings genomes alias path. */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY =
      "qc.conf.fastqscreen.settings.genomes.alias.path";
  /** Collector fastqscreen settings genomes description path. */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_DESC_PATH_KEY =
      "qc.conf.fastqscreen.settings.genomes.desc.path";
  /** Collector fastqscreen settings mappers indexes path. */
  public static final String QC_CONF_FASTQSCREEN_SETTINGS_MAPPERS_INDEXES_PATH_KEY =
      "qc.conf.fastqscreen.settings.mappers.indexes.path";
  /** Collector fastqscreen xsl file. */
  public static final String QC_CONF_FASTQSCREEN_XSL_FILE_KEY =
      "qc.conf.fastqscreen.xsl.file";
  /** Collector project, fastqscreen xsl file */
  public static final String QC_CONF_FASTQSCREEN_PROJECT_XSL_FILE_KEY =
      "qc.conf.fastqscreen.project.xsl.file";
  /** Collector project, fastqscreen xsl file */
  public static final String QC_CONF_FASTQSCREEN_PERCENT_CONTAMINATION_THRESHOLD_KEY =
      "qc.conf.fastqscreen.percent.contamination.threshold";

  /** Collector read xml collector used. */
  public static final String QC_CONF_READ_XML_COLLECTOR_USED_KEY =
      "qc.conf.read.xml.collector.used";
  /** Collector threads. */
  public static final String QC_CONF_THREADS_KEY = "qc.conf.threads";

  /** Collector undetermined indexed xsl file. */
  public static final String QC_CONF_UNDETERMINED_INDEXED_XSL_FILE_KEY =
      "qc.conf.undetermined.indexed.xsl.file";

  /**
   * Collector FastQC, process on undetermined indices samples, default at
   * false.
   */
  public static final String QC_CONF_FASTQC_PROCESS_UNDETERMINED_SAMPLES_KEY =
      "qc.conf.fastqc.process.undetermined.samples";
  /**
   * Collector FastqScreen, process on undetermined indices samples, default at
   * false.
   */
  public static final String QC_CONF_FASTQSCREEN_PROCESS_UNDETERMINED_SAMPLES_KEY =
      "qc.conf.fastqscreen.process.undetermined.samples";

  //
  // Read contains configuration Aozan file
  //
  private static Map<String, String> aozanConfiguration = null;

  /**
   * Sets the globals configuration.
   * @param conf the conf
   */
  public static void setGlobalsConfiguration(final Map<String, String> conf) {

    aozanConfiguration = Collections.unmodifiableMap(conf);
  }

  /**
   * Gets the property from aozan configuration.
   * @param key the key
   * @return the property from aozan configuration
   */
  public static String getPropertyFromAozanConfiguration(final String key) {

    if (aozanConfiguration.isEmpty())
      return null;

    return aozanConfiguration.get(key.trim());
  }

  public static String getLoggerPathFromAozanConfiguration() {
    return aozanConfiguration.get(AOZAN_LOG_PATH_KEY);
  }

  public static String getLoggerLevelFromAozanConfiguration() {
    return aozanConfiguration.get(AOZAN_LOG_LEVEL_KEY);
  }

  public static String getConfigurationFilePathOnAozanConfiguration() {
    return aozanConfiguration.get(AOZAN_CONF_FILE_PATH);
  }
}
