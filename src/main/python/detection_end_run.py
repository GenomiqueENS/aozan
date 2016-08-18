# -*- coding: utf-8 -*-

import os, time
import stat, hiseq_run, aozan
from xml.etree.ElementTree import ElementTree

import common
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_CRITICAL_MIN_SPACE_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_STEP_KEY
from fr.ens.biologie.genomique.aozan.Settings import TMP_PATH_KEY
import cmd
from pickle import FALSE

DONE_FILE = 'hiseq.done'


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + DONE_FILE)


def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    common.add_run_id(run_id, conf[AOZAN_VAR_PATH_KEY] + '/' + DONE_FILE, conf)


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] hiseq done: ' + short_message, message, conf[AOZAN_VAR_PATH_KEY] + '/hiseq.lasterr', conf)


def discovery_run(conf):
    """Discover new ended runs

    Arguments:
        conf: configuration object
    """

    run_ids_done = load_processed_run_ids(conf)

    if common.is_conf_value_equals_true(HISEQ_STEP_KEY, conf):
        for run_id in (hiseq_run.get_available_run_ids(conf) - run_ids_done):

            if run_id is None or len(run_id) == 0:
                # No run id found
                return []

            aozan.welcome(conf)
            common.log('INFO', 'Discover end run ' + str(run_id) + ' on ' + common.get_instrument_name(run_id, conf),
                       conf)

            if create_run_summary_reports(run_id, conf):
                send_mail_if_recent_run(run_id, 12 * 3600, conf)
                add_run_id_to_processed_run_ids(run_id, conf)
                run_ids_done.add(run_id)
            else:
                raise Exception('Create run summary report for new discovery run ' + run_id)

    return run_ids_done


def check_end_run(run_id, conf):
    """Check the end of a run data transfer.

    Arguments:
        run_id: the run id
        conf: configuration dictionary
    """

    hiseq_data_path = hiseq_run.find_hiseq_run_path(run_id, conf)
    reads_number = hiseq_run.get_read_count(run_id, conf)

    # TODO
    if hiseq_data_path is False:
        return False

    # if reads_number equals -1, runParameters.xml is missing
    if reads_number == -1:
        return False

    prefix, suffix = ('Basecalling_Netcopy_complete_Read', '.txt') if (
        common.get_rta_major_version(run_id, conf) == 1) else (
        'RTARead', 'Complete.txt')

    # File generate only by HiSeq sequencer
    for i in range(reads_number):
        filename = prefix + str(i + 1) + suffix

        if not os.path.exists(hiseq_data_path + '/' + run_id + '/' + filename):
            return False

    if not os.path.exists(hiseq_data_path + '/' + run_id + '/RTAComplete.txt'):
        return False

    return True


def check_end_run_since(run_id, secs, conf):
    """Check the end of a run data transfert since a number of seconds.

    Arguments:
        run_id: the run id
        secs: maximal number of seconds
        conf: configuration dictionary
    """

    hiseq_data_path = hiseq_run.find_hiseq_run_path(run_id, conf)
    if hiseq_data_path is False:
        return -1

    reads_number = hiseq_run.get_read_count(run_id, conf)
    last = 0

    for i in range(reads_number):
        file_to_test = hiseq_data_path + '/' + run_id + '/' + build_read_complete(run_id, i, conf)
        if not os.path.exists(file_to_test):
            print "DEBUG: check file on end run is not found " + str(file_to_test)
            return -2
        else:
            m_time = os.stat(file_to_test).st_mtime
            if m_time > last:
                last = m_time

    if (time.time() - last) < secs:
        return last

    return 0


def build_read_complete(run_id, i, conf):
    if common.get_rta_major_version(run_id, conf) == 1:
        return 'Basecalling_Netcopy_complete_Read' + str(i + 1) + '.txt'

    else:
        return 'RTARead' + str(i + 1) + 'Complete.txt'


def send_mail_if_recent_run(run_id, secs, conf):
    run_path = find_hiseq_run_path(run_id, conf)
    if run_path is False:
        return

    last = detection_end_run.check_end_run_since(run_id, secs, conf)

    if last > 0:
        df = common.df(run_path) / (1024 * 1024 * 1024)
        du = common.du(run_path + '/' + run_id) / (1024 * 1024 * 1024)
        common.send_msg('[Aozan] End of the run ' + run_id + ' on ' + common.get_instrument_name(run_id, conf),
                        'A new run (' + run_id + ') has been terminated on ' +
                        common.get_instrument_name(run_id, conf) + ' at ' + common.time_to_human_readable(last) + '.\n' +
                        'Data for this run can be found at: ' + run_path +
                        '\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df), False, conf)


def create_run_summary_reports(run_id, conf):
    """ Copy main files and directory from hiseq run directory to save in report run data directory.
        Save data in two distinct directory on hiseq and on report, and tar.bz2 version

    Arguments:
        run_id: the run id
        conf: configuration dictionary
    """

    hiseq_data_path = find_hiseq_run_path(run_id, conf)
    tmp_base_path = conf[TMP_PATH_KEY]
    reports_data_base_path = conf[REPORTS_DATA_PATH_KEY]

    source_path = hiseq_data_path + '/' + run_id
    reports_data_path = common.get_report_run_data_path(run_id, conf)
    report_prefix = 'report_'
    hiseq_log_prefix = 'hiseq_log_'
    report_archive_file = report_prefix + run_id + '.tar.bz2'
    hiseq_log_archive_file = hiseq_log_prefix + run_id + '.tar.bz2'

    # Save quality control data
    tmp_path = tmp_base_path + '/' + run_id

    # Check if reports_data_path exists
    if not os.path.exists(reports_data_base_path):
        error("Report directory does not exists",
              "Report directory does not exists: " + reports_data_base_path, conf)
        return False

    # Check if temporary directory exists
    if not os.path.exists(tmp_base_path):
        error("Temporary directory does not exists",
              "Temporary directory does not exists: " + tmp_base_path, conf)
        return False

    # Check if reports archive exists
    if os.path.exists(reports_data_path + '/' + report_archive_file):
        error('Report archive already exists for run ' + run_id,
              'Report archive already exists for run ' + run_id + ' : ' + report_archive_file, conf)
        return False

    # Check if hiseq log archive exists
    if os.path.exists(reports_data_path + '/' + hiseq_log_archive_file):
        error('Hiseq log archive already exists for run ' + run_id,
              'Hiseq log archive already exists for run ' + run_id + ' : ' + hiseq_log_archive_file, conf)
        return False

    # Create if not exists archive directory for the run
    if not os.path.exists(reports_data_path):
        os.mkdir(reports_data_path)

    # Create run tmp  directory
    if os.path.exists(tmp_path):
        error('Temporary run data directory already exists for run ' + run_id,
              'Temporary run data directory already exists for run ' + run_id + ' : ' + hiseq_log_archive_file, conf)
    else:
        os.mkdir(tmp_path)

    # Define set file to copy in report archive, check if exists (depend on parameters Illumina)
    files = ['InterOp', 'RunInfo.xml', 'runParameters.xml', 'RunParameters.xml']
    files_to_copy = common.list_existing_files(source_path, files)

    if files_to_copy is None:
        common.log("WARNING",
                   "Archive " + hiseq_log_archive_file + " not create: none file exists " + str(files) +
                   ' in ' + source_path, conf)
    else:
        cmd = 'cd ' + source_path + ' && ' + \
              'cp -rp ' + files_to_copy + tmp_path + ' && ' + \
              'cd ' + tmp_base_path + ' && ' + \
              'mv ' + run_id + ' ' + hiseq_log_prefix + run_id + ' && ' + \
              'tar cjf ' + reports_data_path + '/' + hiseq_log_archive_file + ' ' + hiseq_log_prefix + run_id + \
              ' && ' + 'rm -rf ' + tmp_path
        # + ' && rm -rf ' + hiseq_log_prefix + run_id

        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while saving Illumina quality control for run " + run_id,
                  'Error saving Illumina quality control.\nCommand line:\n' + cmd, conf)
            return False

    # Save html reports
    if os.path.exists(tmp_path):
        cmd = 'rm -rf ' + tmp_path

        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while removing existing temporary directory",
                  'Error while removing existing temporary directory.\nCommand line:\n' + cmd, conf)
            return False

    os.mkdir(tmp_path)

    # Define set file to copy in report archive, check if exists (depend on parameters Illumina)
    if common.get_rta_major_version(run_id, conf) == 1:
        files = ['./Data/Status_Files', './Data/reports', './Data/Status.htm', './First_Base_Report.htm']
    else:
        files = ['./Config', './Recipe', './RTALogs', './RTAConfiguration.xml', './RunCompletionStatus.xml']

    files_to_copy = common.list_existing_files(source_path, files)
    if files_to_copy is None:
        common.log("WARNING", "Archive " + report_archive_file + " not create: none file exists " + str(
            files) + ' in ' + source_path, conf)
    else:
        cmd = 'cd ' + source_path + ' && ' + \
              'cp -rp ' + files_to_copy + tmp_path + ' && ' + \
              'cd ' + tmp_base_path + ' && ' + \
              'mv ' + run_id + ' ' + report_prefix + run_id + ' && ' + \
              'tar cjf ' + reports_data_path + '/' + report_archive_file + ' ' + report_prefix + run_id + ' && ' + \
              'chmod -R u=rwX,go=rX ' + report_prefix + run_id + ' && ' + \
              'mv ' + report_prefix + run_id + ' ' + reports_data_path

        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while saving Illumina html reports for run " + run_id,
                  'Error saving Illumina html reports.\nCommand line:\n' + cmd, conf)
            return False

    # Create index.hml file
    common.create_html_index_file(conf, run_id, [HISEQ_STEP_KEY])

    # Set read only archives files
    os.chmod(reports_data_path + '/' + report_archive_file, stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)
    os.chmod(reports_data_path + '/' + hiseq_log_archive_file, stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)

    return True

