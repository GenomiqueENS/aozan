# -*- coding: utf-8 -*-

'''
Created on 25 oct. 2011

@author: Laurent Jourdren
'''
import os.path, stat
import common, hiseq_run, time
from xml.etree.ElementTree import ElementTree
from java.io import IOException
from java.lang import Runtime
from java.util import HashMap
from fr.ens.transcriptome.eoulsan import EoulsanException
from fr.ens.transcriptome.aozan import Settings
from fr.ens.transcriptome.aozan.io import CasavaDesignXLSReader
from fr.ens.transcriptome.eoulsan.illumina import CasavaDesignUtil
from fr.ens.transcriptome.eoulsan.illumina.io import CasavaDesignCSVReader
from fr.ens.transcriptome.eoulsan.illumina.io import CasavaDesignCSVWriter


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf[Settings.AOZAN_VAR_PATH_KEY] + '/demux.done')

def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf[Settings.AOZAN_VAR_PATH_KEY] + '/demux.done', conf)


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] demultiplexer: ' + short_message, message, conf[Settings.AOZAN_VAR_PATH_KEY] + '/demux.lasterr', conf)


def load_index_sequences(conf):
    """Load the map of the index sequences.

    Arguments:
        index_shortcut_path: the path to the index sequences
    """

    result = HashMap()

    if conf[Settings.INDEX_SEQUENCES_KEY] == '' or not os.path.exists(conf[Settings.INDEX_SEQUENCES_KEY]):
            return result



    f = open(conf[Settings.INDEX_SEQUENCES_KEY], 'r')

    for l in f:
        l = l[:-1]
        if len(l) == 0:
            continue
        fields = l.split('=')
        if len(fields) == 2:
            result[fields[0].strip().lower()] = fields[1].strip().upper()

    f.close()

    return result


def get_flowcell_id_in_demultiplex_xml(fastq_output_dir):
    """Get the flowcell id in DemultiplexConfig.xml.

    Arguments:
        fastq_output_dir: the path to the fastq output directory
    """

    tree = ElementTree()
    tree.parse(fastq_output_dir + '/DemultiplexConfig.xml')

    return tree.find("FlowcellInfo").attrib['ID']


def demux(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    start_time = time.time()
    common.log('INFO', 'Demux step: start', conf)

    reports_data_base_path = conf[Settings.REPORTS_DATA_PATH_KEY]
    reports_data_path = reports_data_base_path + '/' + run_id

    input_bcl_path = conf[Settings.BCL_DATA_PATH_KEY] + '/' + run_id
    
    # Check if must use the direct output of the HiSeq
    if conf[Settings.DEMUX_USE_HISEQ_OUTPUT_KEY].lower().strip() == 'true':
        # Retrieve the path of run data directory on HiSeq
        input_bcl_path = hiseq_run.find_hiseq_run_path(run_id, conf)
        # Path not found
        if input_bcl_path == False:
            error("HiSeq data directory does not exists", "HiSeq data directory does not exists for the run : " + run_id, conf)
            return False
    
    run_number = hiseq_run.get_run_number(run_id)
    instrument_sn = hiseq_run.get_instrument_sn(run_id)
    flow_cell_id = hiseq_run.get_flow_cell(run_id)

    input_design_xls_path = conf[Settings.CASAVA_SAMPLESHEETS_PATH_KEY] + '/' + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + '_' + instrument_sn + '_%04d.xls' % run_number
    input_design_csv_path = conf[Settings.CASAVA_SAMPLESHEETS_PATH_KEY] + '/' + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + '_' + instrument_sn + '_%04d.csv' % run_number
    design_csv_path = conf[Settings.TMP_PATH_KEY] + '/' + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + '_' + instrument_sn + '_%04d.csv' % run_number
    fastq_output_dir = conf[Settings.FASTQ_DATA_PATH_KEY] + '/' + run_id

    basecall_stats_prefix = 'basecall_stats_'
    basecall_stats_file = basecall_stats_prefix + run_id + '.tar.bz2'


    common.log("INFO", "Flowcell id: " + flow_cell_id, conf)

    # Check if root input bcl data directory exists
    if not os.path.exists(input_bcl_path):
        error("Basecalling data directory does not exists", "Basecalling data directory does not exists: " + input_bcl_path, conf)
        return False

    # Check if root input fastq data directory exists
    if not os.path.exists(conf[Settings.FASTQ_DATA_PATH_KEY]):
        error("Fastq data directory does not exists", "Fastq data directory does not exists: " + conf[Settings.FASTQ_DATA_PATH_KEY], conf)
        return False

    # Check if casava designs path exists
    if not os.path.exists(conf[Settings.CASAVA_SAMPLESHEETS_PATH_KEY]):
        error("Casava sample sheets directory does not exists", "Casava sample sheets does not exists: " + conf[Settings.CASAVA_SAMPLESHEETS_PATH_KEY], conf)
        return False

    # Check if temporary directory exists
    if not os.path.exists(conf[Settings.TMP_PATH_KEY]):
        error("Temporary directory does not exists", "Temporary directory does not exists: " + conf[Settings.TMP_PATH_KEY], conf)
        return False

    # Check if reports_data_path exists
    if not os.path.exists(reports_data_base_path):
        error("Report directory does not exists", "Report directory does not exists: " + reports_data_base_path, conf)
        return False

    # Check if basecall stats archive exists
    if os.path.exists(reports_data_path + '/' + basecall_stats_file):
        error('Basecall stats archive already exists for run ' + run_id, 'Basecall stats archive already exists for run ' + run_id + ': ' + basecall_stats_file, conf)
        return False

    # Create if not exists archive directory for the run
    if not os.path.exists(reports_data_base_path + '/' + run_id):
        os.mkdir(reports_data_base_path + '/' + run_id)

    # Check if the output directory already exists
    if os.path.exists(fastq_output_dir):
        error("fastq output directory already exists for run " + run_id,
              'The fastq output directory already exists for run ' + run_id + ': ' + fastq_output_dir, conf)
        return False

    # Compute disk usage and disk free to check if enough disk space is available
    input_path_du = common.du(input_bcl_path)
    output_df = common.df(conf[Settings.FASTQ_DATA_PATH_KEY])
    du_factor = float(conf[Settings.DEMUX_SPACE_FACTOR_KEY])
    space_needed = input_path_du * du_factor

    common.log("WARNING", "Demux step: input disk usage: " + str(input_path_du), conf)
    common.log("WARNING", "Demux step: output disk free: " + str(output_df), conf)
    common.log("WARNING", "Demux step: space needed: " + str(space_needed), conf)

    # Check if free space is available
    if output_df < space_needed:
        error("Not enough disk space to perform demultiplexing for run " + run_id, "Not enough disk space to perform demultiplexing for run " + run_id + 
              '.\n%.2f Gb' % (space_needed / 1024 / 1024 / 1024) + ' is needed (factor x' + str(du_factor) + ') on ' + fastq_output_dir + '.', conf)
        return False

    if conf[Settings.CASAVA_SAMPLESHEET_FORMAT_KEY].strip().lower() == 'xls':

        # Convert design in XLS format to CSV format

        # Check if the xls design exists
        if not os.path.exists(input_design_xls_path):
            error("no casava sample sheet found", "No casava sample sheet found for " + run_id + " run.\n" + \
              'You must provide a ' + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + '-%04d.xls file' % run_number + ' in ' + conf[Settings.CASAVA_SAMPLESHEETS_PATH_KEY] + \
              ' directory to demultiplex and create fastq files for this run.\n', conf)
            return False

        try:
            # Load XLS design file
            design = CasavaDesignXLSReader(input_design_xls_path).read()

            # Replace index sequence shortcuts by sequences
            CasavaDesignUtil.replaceIndexShortcutsBySequences(design, load_index_sequences(conf))

            # Write CSV design file
            CasavaDesignCSVWriter(design_csv_path).writer(design)

        except IOException, exp:
            error("error while converting " + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + "-%04d" % run_number + ".xls to CSV format", exp.getMessage(), conf)
            return False
        except EoulsanException, exp:
            error("error while converting " + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + "-%04d" % run_number + ".xls to CSV format", exp.getMessage(), conf)
            return False

    elif conf[Settings.CASAVA_SAMPLESHEET_FORMAT_KEY].strip().lower() == 'csv':

        # Copy the CSV file

        # Check if the xls design exists
        if not os.path.exists(input_design_csv_path):
            error("no casava sample sheet found", "No casava sample sheet found for " + run_id + " run.\n" + \
              'You must provide a ' + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + '-%04d.csv file' % run_number + ' in ' + conf[Settings.CASAVA_SAMPLESHEETS_PATH_KEY] + \
              ' directory to demultiplex and create fastq files for this run.\n', conf)
            return False

        cmd = 'cp ' + input_design_csv_path + ' ' + design_csv_path
        common.log("SEVERE", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while copying Casava CSV sample sheet file to temporary directory for run " + run_id,
                  'Error while copying Casava CSV sample sheet file to temporary directory.\nCommand line:\n' + cmd, conf)
            return False

    elif conf[Settings.CASAVA_SAMPLESHEET_FORMAT_KEY].strip().lower() == 'command':

        if conf['casava.design.generator.command'] == None or conf['casava.design.generator.command'].strip() == '':
            error("error while creating Casava CSV sample sheet file for run " + run_id,
                  'Error while creating Casava CSV sample sheet file, the command is empty.', conf)
            return False

        cmd = conf['casava.design.generator.command'] + ' ' + run_id + ' ' + design_csv_path
        common.log("SEVERE", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while creating Casava CSV sample sheet file for run " + run_id,
                  'Error while creating Casava CSV sample sheet file.\nCommand line:\n' + cmd, conf)

        if not os.path.exists(design_csv_path):
            error("error while creating Casava CSV sample sheet file for run " + run_id,
                  'Error while creating Casava CSV sample sheet file, the external command did not create Casava CSV file:\n' + cmd, conf)
    else:
        error("error while creating Casava CSV sample sheet file for run " + run_id,
                  'No method to get Casava sample sheet file has been defined. Please, set the "casava.samplesheet.format" property.\n', conf)


    # Check if Casava CSV design file has been created
    if not os.path.exists(design_csv_path):
            error("error while reading Casava CSV sample sheet file for run " + run_id,
                  'Error while reading Casava CSV sample sheet file, the sample sheet file does not exist: \n' + design_csv_path, conf)

    # Check Casava CSV design file
    try:
        # Load XLS design file
        design = CasavaDesignCSVReader(design_csv_path).read()

        # Check values of design file
        design_warnings = CasavaDesignUtil.checkCasavaDesign(design, flow_cell_id)

    except IOException, exp:
        error("error while converting " + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + "-%04d" % run_number + ".xls to CSV format", exp.getMessage(), conf)
        return False
    except EoulsanException, exp:
        error("error while converting " + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + "-%04d" % run_number + ".xls to CSV format", exp.getMessage(), conf)
        return False

    # Log Casava design warning
    if (design_warnings > 0):
        msg = ''
        first = True
        for warn in design_warnings:
            if first:
                first = False
            else:
                msg += ' '
            msg += warn
        common.log("WARNING", "casava sample sheet warnings: " + msg, conf)

    # Create casava makefile
    cmd = conf[Settings.CASAVA_PATH_KEY] + '/bin/configureBclToFastq.pl ' + \
          '--fastq-cluster-count ' + conf[Settings.CASAVA_FASTQ_CLUSTER_COUNT_KEY] + ' ' + \
          '--compression ' + conf[Settings.CASAVA_COMPRESSION_KEY] + ' ' + \
          '--gz-level ' + conf[Settings.CASAVA_COMPRESSION_LEVEL_KEY] + ' ' \
          '--mismatches ' + conf[Settings.CASAVA_MISMATCHES_KEY] + ' ' + \
          '--input-dir ' + input_bcl_path + '/Data/Intensities/BaseCalls ' + \
          '--sample-sheet ' + design_csv_path + ' ' + \
          '--output-dir ' + fastq_output_dir
    if conf[Settings.CASAVA_WITH_FAILED_READS_KEY] == 'True':
        cmd = cmd + ' --with-failed-reads'
    if conf[Settings.CASAVA_ADAPTER_FASTA_FILE_PATH_KEY] != '':
        cmd = cmd + ' --adapter-sequence ' + conf[Settings.CASAVA_ADAPTER_FASTA_FILE_PATH_KEY]

    if conf[Settings.CASAVA_ADDITIONNAL_ARGUMENTS_KEY] != '':
        cmd = cmd + ' ' + conf[Settings.CASAVA_ADDITIONNAL_ARGUMENTS_KEY]

    # Retrieve output in file
    cmd = cmd + ' > /tmp/bcl2fastq_output_' + run_id + '.out 2> /tmp/bcl2fastq_output_' + run_id + '.err'
    
    common.log("SEVERE", "exec: " + cmd, conf)
    exit_code = os.system(cmd)
    if exit_code != 0:
        error("error while creating Casava makefile for run " + run_id, 'Error while creating Casava makefile (exit code: ' + str(exit_code) + ').\nCommand line:\n' + cmd, conf)
        return False
    
    # Configuration bcl2fastq success, move command output file in fastq_output_dir
    cmd = 'mv /tmp/bcl2fastq_output_' + run_id + '.*  ' + fastq_output_dir
    common.log("SEVERE", "exec: " + cmd, conf)
    exit_code = os.system(cmd)
    if exit_code != 0:
        error("error while moving command output files for run " + run_id, 'Error while moving command output files (exit code: ' + str(exit_code) + ').\nCommand line:\n' + cmd, conf)
    
    # Get the number of cpu
    cpu_count = int(conf[Settings.CASAVA_THREADS_KEY])
    if cpu_count < 1:
        cpu_count = Runtime.getRuntime().availableProcessors()

    # Launch casava
    cmd = 'cd ' + fastq_output_dir + ' && make -j ' + str(cpu_count) + ' > ' + fastq_output_dir + '/make.out' + ' 2> ' + fastq_output_dir + '/make.err'
    common.log("SEVERE", "exec: " + cmd, conf)
    exit_code = os.system(cmd)
    if exit_code != 0:
        error("error while running Casava for run " + run_id, 'Error while running Casava (exit code: ' + str(exit_code) + ').\nCommand line:\n' + cmd, conf)
        return False

    # Copy design to output directory
    cmd = "cp -p " + design_csv_path + ' ' + fastq_output_dir
    common.log("SEVERE", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while copying sample sheet file to the fastq directory for run " + run_id, 'Error while copying sample sheet file to fastq directory.\nCommand line:\n' + cmd, conf)
        return False

    # Archive basecall stats
    flow_cell_id_in_conf_xml = get_flowcell_id_in_demultiplex_xml(fastq_output_dir)
    cmd = 'cd ' + fastq_output_dir + ' &&  mv Basecall_Stats_' + flow_cell_id_in_conf_xml + ' ' + basecall_stats_prefix + run_id + ' && ' + \
        'tar cjf ' + reports_data_path + '/' + basecall_stats_file + ' ' + basecall_stats_prefix + run_id + ' && ' + \
        'cp -rp ' + basecall_stats_prefix + run_id + ' ' + reports_data_path + ' && ' + \
        'mv ' + basecall_stats_prefix + run_id + ' Basecall_Stats_' + flow_cell_id_in_conf_xml
    common.log("SEVERE", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while saving the basecall stats file for " + run_id, 'Error while saving the basecall stats files.\nCommand line:\n' + cmd, conf)
        return False

    # Set read only basecall stats archives files
    os.chmod(reports_data_path + '/' + basecall_stats_file, stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)

    # The output directory must be read only
    cmd = 'chmod -R ugo-w ' + fastq_output_dir + '/Project_*'
    common.log("SEVERE", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while setting read only the output fastq directory for run " + run_id, 'Error while setting read only the output fastq directory.\nCommand line:\n' + cmd, conf)
        return False



    # Add design to the archive of designs
    if conf[Settings.CASAVA_SAMPLESHEET_FORMAT_KEY].strip().lower() == 'xls':
        cmd = 'cp ' + input_design_xls_path + ' ' + conf[Settings.TMP_PATH_KEY] + \
        ' && cd ' + conf[Settings.TMP_PATH_KEY] + \
        ' && zip -q ' + conf[Settings.CASAVA_SAMPLESHEETS_PATH_KEY] + '/' + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + 's.zip ' + \
        os.path.basename(design_csv_path) + ' ' + os.path.basename(input_design_xls_path)
    else:
        cmd = 'cd ' + conf[Settings.TMP_PATH_KEY] + \
        ' && zip -q ' + conf[Settings.CASAVA_SAMPLESHEETS_PATH_KEY] + '/' + conf[Settings.CASAVA_SAMPLESHEET_PREFIX_FILENAME_KEY] + 's.zip ' + \
        os.path.basename(design_csv_path)

    common.log("SEVERE", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while archiving the sample sheet file for " + run_id, 'Error while archiving the sample sheet file for.\nCommand line:\n' + cmd, conf)
        return False

    # Remove temporary design files
    os.remove(design_csv_path)
    if conf[Settings.CASAVA_SAMPLESHEET_FORMAT_KEY].strip().lower() == 'xls':
        os.remove(conf[Settings.TMP_PATH_KEY] + '/' + os.path.basename(input_design_xls_path))

    # Create index.hml file
    common.create_html_index_file(conf, reports_data_path + '/index.html', run_id, ['sync', 'demux'])

    df_in_bytes = common.df(fastq_output_dir)
    du_in_bytes = common.du(fastq_output_dir)
    df = df_in_bytes / (1024 * 1024 * 1024)
    du = du_in_bytes / (1024 * 1024 * 1024)

    common.log("WARNING", "Demux step: output disk free after demux: " + str(df_in_bytes), conf)
    common.log("WARNING", "Demux step: space used by demux: " + str(du_in_bytes), conf)

    duration = time.time() - start_time

    msg = 'End of demultiplexing for run ' + run_id + '.' + \
        '\nJob finished at ' + common.time_to_human_readable(time.time()) + \
        ' with no error in ' + common.duration_to_human_readable(duration) + '.\n\n' + \
        'Fastq files for this run ' + \
        'can be found in the following directory:\n  ' + fastq_output_dir

    if design_warnings.size() > 0:
        msg += '\n\nSample sheet warnings:'
        for warn in design_warnings:
            msg += "\n  - " + warn

    # Add path to report if reports.url exists
    if conf[Settings.REPORTS_URL_KEY] != None and conf[Settings.REPORTS_URL_KEY] != '':
        msg += '\n\nRun reports can be found at following location:\n  ' + conf[Settings.REPORTS_URL_KEY] + '/' + run_id

    msg += '\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df)

    common.send_msg('[Aozan] End of demultiplexing for run ' + run_id, msg, False, conf)
    common.log('INFO', 'Demux step: success in ' + common.duration_to_human_readable(duration), conf)
    return True
