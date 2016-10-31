# -*- coding: utf-8 -*-

#
#                  Aozan development code
#
# This code may be freely distributed and modified under the
# terms of the GNU General Public License version 3 or later
# and CeCILL. This should be distributed with the code. If you
# do not have a copy, see:
#
#      http://www.gnu.org/licenses/gpl-3.0-standalone.html
#      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
#
# Copyright for this code is held jointly by the Genomic platform
# of the Institut de Biologie de l'École Normale Supérieure and
# the individual authors.
#
# For more information on the Aozan project and its aims,
# or to join the Aozan Google group, visit the home page at:
#
#      http://outils.genomique.biologie.ens.fr/aozan
#
#

'''
This script checks if a run has ended sequencing.
'''

import aozan
import hiseq_run
import os
import os.path
import stat

import common
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_STEP_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import TMP_PATH_KEY


def send_failed_run_message(run_id, secs, conf):
    """Send a mail to inform about a failed run.

    Arguments:
        conf: configuration dictionary
    """

    run_path = hiseq_run.find_hiseq_run_path(run_id, conf)
    file_to_test = run_path + '/' + run_id + '/RTAComplete.txt'
    last = os.stat(file_to_test).st_mtime

    df = common.df(run_path) / (1024 * 1024 * 1024)
    du = common.du(run_path + '/' + run_id) / (1024 * 1024 * 1024)

    common.send_msg('[Aozan] Failed run ' + run_id + ' on ' + common.get_instrument_name(run_id, conf),
                    'A run (' + run_id + ') has failed on ' + common.get_instrument_name(run_id, conf) +
                    ' at ' + common.time_to_human_readable(last) + '.\n' + 'Data for this run can be found at: ' +
                    run_path + '\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df),
                    False, conf)


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + common.HISEQ_DONE_FILE)


def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    common.add_run_id(run_id, conf[AOZAN_VAR_PATH_KEY] + '/' + common.HISEQ_DONE_FILE, conf)


def add_run_id_to_denied_run_ids(run_id, conf):
    """Add a denied run id to the list of the run ids.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    common.add_run_id(run_id, conf[AOZAN_VAR_PATH_KEY] + '/' + common.HISEQ_DENY_FILE, conf)


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] hiseq done: ' + short_message, message, conf[AOZAN_VAR_PATH_KEY] + '/hiseq.lasterr', conf)


def get_available_finished_run_ids(conf):
    """Get the list of the available runs.

    Arguments:
        conf: configuration dictionary
    """

    result = set()

    for hiseq_data_path in hiseq_run.get_hiseq_data_paths(conf):

        files = os.listdir(hiseq_data_path)
        for f in files:
            if os.path.isdir(hiseq_data_path + '/' + f) and \
                    hiseq_run.check_run_id(f, conf) and \
                    hiseq_run.check_end_run(f, conf):
                result.add(f)

    return result


def discover_finished_runs(denied_runs, conf):
    """Discover new ended runs

    Arguments:
        conf: configuration object
    """

    run_ids_done = load_processed_run_ids(conf)

    if common.is_conf_value_equals_true(HISEQ_STEP_KEY, conf):
        for run_id in (get_available_finished_run_ids(conf) - run_ids_done - denied_runs):

            if run_id is None or len(run_id) == 0:
                # No run id found
                return []

            aozan.welcome(conf)
            common.log('INFO', 'Ending run detection ' + str(run_id) + ' on ' + common.get_instrument_name(run_id, conf),
                       conf)

            if hiseq_run.get_read_count(run_id, conf) == 0:
                send_failed_run_message(run_id, common.MAX_DELAY_TO_SEND_TERMINATED_RUN_EMAIL, conf)
                add_run_id_to_denied_run_ids(run_id, conf)
                create_run_summary_reports(run_id, conf)
            else:
                if create_run_summary_reports(run_id, conf):
                    send_mail_if_recent_run(run_id, common.MAX_DELAY_TO_SEND_TERMINATED_RUN_EMAIL, conf)
                    add_run_id_to_processed_run_ids(run_id, conf)
                    run_ids_done.add(run_id)

    return run_ids_done


def send_mail_if_recent_run(run_id, secs, conf):
    """Send an email to inform that a new run is finished.

    Arguments:
        run_id: run id
        secs: maximum delay since the end of the run
        conf: configuration object
    """

    run_path = hiseq_run.find_hiseq_run_path(run_id, conf)
    if run_path is False:
        return

    last = hiseq_run.check_end_run_since(run_id, secs, conf)

    if last > 0:
        df = common.df(run_path) / (1024 * 1024 * 1024)
        du = common.du(run_path + '/' + run_id) / (1024 * 1024 * 1024)
        common.send_msg('[Aozan] Ending run ' + run_id + ' on ' + common.get_instrument_name(run_id, conf),
                        'A new run (' + run_id + ') is finished on ' +
                        common.get_instrument_name(run_id, conf) + ' at ' + common.time_to_human_readable(
                            last) + '.\n' +
                        'Data for this run can be found at: ' + run_path +
                        '\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df), False, conf)


def list_existing_files(path, files_array):
    """Return string with existing files from array

    Arguments:
        path: path to directory
        files_array: all files to check
    """

    filename_list = []
    for filename in files_array:
        if os.path.exists(path + '/' + filename):
            filename_list.append('\'' + filename + '\'')

    return ' '.join(filename_list)


def create_run_summary_reports(run_id, conf):
    """ Copy main files and directory from hiseq run directory to save in report run data directory.
        Save data in two distinct directory on hiseq and on report, and tar.bz2 version

    Arguments:
        run_id: the run id
        conf: configuration dictionary
    """

    hiseq_data_path = hiseq_run.find_hiseq_run_path(run_id, conf)
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
        error("Failed to create report archive: Report directory does not exist",
              "Failed to create report archive: Report directory does not exist: " + reports_data_base_path, conf)
        return False

    # Check if temporary directory exists
    if not os.path.exists(tmp_base_path):
        error("Failed to create report archive: Temporary directory does not exist",
              "Failed to create report archive: Temporary directory does not exist: " + tmp_base_path, conf)
        return False

    # Check if reports archive exists
    if os.path.exists(reports_data_path + '/' + report_archive_file):
        error("Failed to create report archive: Report archive already exists for run " + run_id,
              "Failed to create report archive: Report archive already exists for run " + run_id + " : " + report_archive_file, conf)
        return False

    # Check if hiseq log archive exists
    if os.path.exists(reports_data_path + '/' + hiseq_log_archive_file):
        error("Failed to create report archive: Hiseq log archive already exists for run " + run_id,
              "Failed to create report archive: Hiseq log archive already exists for run " + run_id + " : " + hiseq_log_archive_file, conf)
        return False

    # Create if not exist archive directory for the run
    if not os.path.exists(reports_data_path):
        os.mkdir(reports_data_path)

    # Create run tmp  directory
    if os.path.exists(tmp_path):
        error("Failed to create report archive: Temporary run data directory already exists for run " + run_id,
              "Failed to create report archive: Temporary run data directory already exists for run " + run_id + " : " + hiseq_log_archive_file, conf)
    else:
        os.mkdir(tmp_path)

    # Define set file to copy in report archive, check if exists (depend on parameters Illumina)
    files = ['InterOp', 'RunInfo.xml', 'runParameters.xml', 'RunParameters.xml']
    files_to_copy = list_existing_files(source_path, files)

    if len(files_to_copy) == 0:
        common.log("WARNING",
                   "Archive " + hiseq_log_archive_file + " not created: none file exists " + str(files) +
                   ' in ' + source_path, conf)
    else:
        cmd = 'cd \'' + source_path + '\' && ' + \
              'cp -rp ' + files_to_copy + ' \'' + tmp_path + '\' && ' + \
              'cd \'' + tmp_base_path + '\' && ' + \
              'mv \'' + run_id + '\' \'' + hiseq_log_prefix + run_id + '\' && ' + \
              'tar cjf \'' + reports_data_path + '/' + hiseq_log_archive_file + '\' \'' + hiseq_log_prefix + run_id + \
              '\' && ' + 'rm -rf \'' + tmp_path + '\''
        # + ' && rm -rf ' + hiseq_log_prefix + run_id

        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("Failed to create report archive: Error while saving Illumina quality control for run " + run_id,
                  "Failed to create report archive: Error saving Illumina quality control.\nCommand line:\n" + cmd, conf)
            return False

    # Save html reports
    if os.path.exists(tmp_path):
        cmd = 'rm -rf \'' + tmp_path + '\''

        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("Failed to create report archive: Error while removing existing temporary directory",
                  "Failed to create report archive: Error while removing existing temporary directory.\nCommand line:\n" + cmd, conf)
            return False

    os.mkdir(tmp_path)

    # Define set file to copy in report archive, check if exists (depend on parameters Illumina)
    if common.get_rta_major_version(run_id, conf) == 1:
        files = ['./Data/Status_Files', './Data/reports', './Data/Status.htm', './First_Base_Report.htm']
    else:
        files = ['./Config', './Recipe', './RTALogs', './RTAConfiguration.xml', './RunCompletionStatus.xml']

    files_to_copy = common.list_existing_files(source_path, files)
    if len(files_to_copy) == 0:
        common.log("WARNING", "Archive " + report_archive_file + " not created: none file exists " + str(
            files) + ' in ' + source_path, conf)
    else:
        cmd = 'cd \'' + source_path + '\' && ' + \
              'cp -rp ' + files_to_copy + ' \'' + tmp_path + '\' && ' + \
              'cd \'' + tmp_base_path + '\' && ' + \
              'mv \'' + run_id + '\' \'' + report_prefix + run_id + '\' && ' + \
              'tar cjf \'' + reports_data_path + '/' + report_archive_file + '\' \'' + report_prefix + run_id + '\' && ' + \
              'mv \'' + report_prefix + run_id + '\' \'' + reports_data_path + '\''

        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("Failed to create report archive: Error while saving Illumina HTML reports for run " + run_id,
                  "Failed to create report archive: Error saving Illumina HTML reports.\nCommand line:\n" + cmd, conf)
            return False

    # Create index.hml file
    common.create_html_index_file(conf, run_id, [HISEQ_STEP_KEY])

    # Set read only the report directory
    common.chmod_files_in_dir(reports_data_path + '/' + report_prefix, None, conf)

    # Set read only archives files
    common.chmod(reports_data_path + '/' + report_archive_file, conf)
    common.chmod(reports_data_path + '/' + hiseq_log_archive_file, conf)

    return True
