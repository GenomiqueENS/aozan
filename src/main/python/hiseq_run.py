# -*- coding: utf-8 -*-

import os, time
import stat, detection_end_run
import common

from fr.ens.transcriptome.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import HISEQ_CRITICAL_MIN_SPACE_KEY
from fr.ens.transcriptome.aozan.Settings import HISEQ_DATA_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import HISEQ_STEP_KEY
from fr.ens.transcriptome.aozan.Settings import TMP_PATH_KEY
from fr.ens.transcriptome.aozan.illumina import RunInfo


# from pickle import FALSE
# from macpath import pathsep

DENY_FILE = 'sequencer_run.deny'

def load_deny_run_ids(conf):
    """Load the list of the run ids to not process.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/hiseq.deny')


def get_runinfos_file(run_id, conf):
    """Get the RunInfos.xml path.

    Arguments:
        runtId: the run id
        conf: configuration dictionary
    """

    sequencer_path = find_hiseq_run_path(run_id, conf)

    if type(sequencer_path) is bool:
        return None

    path = sequencer_path + '/' + run_id + '/RunInfo.xml'

    if not os.path.exists(path):
        return None

    return path

def get_run_info(run_id, conf):
    """Get the RunInfo object.

    Arguments:
        runtId: the run id
        conf: configuration dictionary
    """

    file_src = get_runinfos_file(run_id, conf)

    if not file_src:
        return None

    return RunInfo.parse(file_src)

def get_read_count(run_id, conf):
    """Get the number of read of a run.

        Arguments:
            runtId: the run id
            conf: configuration dictionary
    """

    run_info = get_run_info(run_id, conf)

    if run_info == None:
        return -1

    return run_info.getReads().size()


def get_lane_count(run_id, conf):
    """Get the number of lanes of a run.

        Arguments:
            runtId: the run id
            conf: configuration dictionary
    """

    run_info = get_run_info(run_id, conf)

    if run_info == None:
        return -1

    return run_info.getFlowCellLaneCount()


def check_run_id(run_id, conf):
    """Check if the run id is valid.

    Arguments:
        runId: the run id
        conf: configuration dictionary
    """

    fields = run_id.strip().split("_")

    if len(fields) != 4:
        return False

    date = fields[0]
    count = fields[2]
    flow_cell_id = fields[3]

    # Test the date
    if not date.isdigit() or len(date) != 6:
        return False

    # Test the run count
    if not count.isdigit() or len(count) != 4:
        return False

    # Test the flow cell id

    if not flow_cell_id.isalnum() or len(flow_cell_id) != 10:
        return False

    return True


def get_available_run_ids(conf):
    """Get the list of the available runs.

    Arguments:
        conf: configuration dictionary
    """

    result = set()

    for hiseq_data_path in get_hiseq_data_paths(conf):

        files = os.listdir(hiseq_data_path)
        for f in files:
            if os.path.isdir(hiseq_data_path + '/' + f) and check_run_id(f, conf) and detection_end_run.check_end_run(f, conf):
                result.add(f)

    return result

def get_working_run_ids(conf):
    """Get the list of the workfing runs.

    Arguments:
        conf: configuration dictionary
    """

    result = set()

    for hiseq_data_path in get_hiseq_data_paths(conf):

        files = os.listdir(hiseq_data_path)
        for f in files:
            if os.path.isdir(hiseq_data_path + '/' + f) and check_run_id(f, conf) and not detection_end_run.check_end_run(f, conf):
                result.add(f)

    return result

def get_run_number(run_id):
    """Get the run number from the run id.

    Arguments:
        run_id: the run id
    """

    return int(run_id.split('_')[2])

def get_flow_cell(run_id):
    """Get the flow cell id from the run id.

    Arguments:
        run_id: the run id
    """

    return run_id.split('_')[3][1:]

def get_instrument_sn(run_id):
    """Get the instrument serial number from the run id.

    Arguments:
        run_id: the run id
    """

    return run_id.split('_')[1]

def send_mail_if_critical_free_space_available(conf):
    """Check if disk free space is critical. If true send a mail.

    Arguments:
        conf: configuration dictionary
    """

    for path in get_hiseq_data_paths(conf):

        if os.path.exists(path):
            df = common.df(path)
            free_space_threshold = long(conf[HISEQ_CRITICAL_MIN_SPACE_KEY])
            if df < free_space_threshold:
                common.send_msg('[Aozan] Critical: Not enough disk space on Hiseq storage for current run',
                            'There is only %.2f' % (df / (1024 * 1024 * 1024)) + ' Gb left for Hiseq run storage in ' + path + '. '
                            ' The current warning threshold is set to %.2f' % (free_space_threshold / (1024 * 1024 * 1024)) + ' Gb.', False, conf)

def send_mail_if_recent_run(run_id, secs, conf):

    run_path = find_hiseq_run_path(run_id, conf)
    if run_path == False:
        return

    last = detection_end_run.check_end_run_since(run_id, secs, conf)

    if last > 0:
        df = common.df(run_path) / (1024 * 1024 * 1024)
        du = common.du(run_path + '/' + run_id) / (1024 * 1024 * 1024)
        common.send_msg('[Aozan] End of the HiSeq run ' + run_id, 'A new run (' + run_id + ') has been terminated at ' +
                    common.time_to_human_readable(last) + '.\n' +
                    'Data for this run can be found at: ' + run_path +
                    '\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df), False, conf)


def get_hiseq_data_paths(conf):

    paths = conf[HISEQ_DATA_PATH_KEY].split(':')
    for i in range(len(paths)):
        paths[i] = paths[i].strip()

    return paths

def find_hiseq_run_path(run_id, conf):

    for path in get_hiseq_data_paths(conf):

        path_to_test = path.strip() + '/' + run_id

        if (os.path.exists(path_to_test)):
            return path.strip()

    return False

def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] hiseq done: ' + short_message, message, conf[AOZAN_VAR_PATH_KEY] + '/hiseq.lasterr', conf)



def create_run_summary_reports(run_id, conf):
#     return True
#
#
# def create_run_summary_reports_real(run_id, conf):

    """ Copy main files and directory from hiseq run directory to save in report run data directory.
        Save data in two distinct directory on hiseq and on report, and tar.bz2 version

    Arguments:
        runId: the run id
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
    files = ['InterOp' , 'RunInfo.xml' , 'runParameters.xml', 'RunParameters.xml']
    files_to_copy = common.list_existing_files(source_path, files)

    if (files_to_copy == None):
        common.log("WARNING", "Archive " + hiseq_log_archive_file + " not create: none file exists " + files + ' in ' + source_path, conf)
    else:
        cmd = 'cd ' + source_path + ' && ' + \
            'cp -rp ' + files_to_copy + tmp_path + ' && ' + \
            'cd ' + tmp_base_path + ' && ' + \
            'mv ' + run_id + ' ' + hiseq_log_prefix + run_id + ' && ' + \
            'tar cjf ' + reports_data_path + '/' + hiseq_log_archive_file + ' ' + hiseq_log_prefix + run_id + ' && ' + \
            'rm -rf ' + tmp_path
            #+ ' && rm -rf ' + hiseq_log_prefix + run_id

        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while saving Illumina quality control for run " + run_id, 'Error saving Illumina quality control.\nCommand line:\n' + cmd, conf)
            return False

    # Save html reports
    if os.path.exists(tmp_path):
        cmd = 'rm -rf ' + tmp_path

        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while removing existing temporary directory", 'Error while removing existing temporary directory.\nCommand line:\n' + cmd, conf)
            return False

    os.mkdir(tmp_path)

    # Define set file to copy in report archive, check if exists (depend on parameters Illumina)
    if common.is_sequencer_hiseq(run_id, conf):
        files = ['./Data/Status_Files', './Data/reports', './Data/Status.htm', './First_Base_Report.htm' ]
    else:
        files = ['./Config', './Recipe', './RTALogs', './RTAConfiguration.xml', './RunCompletionStatus.xml' ]

    files_to_copy = common.list_existing_files(source_path, files)
    if (files_to_copy == None):
        common.log("WARNING", "Archive " + report_archive_file + " not create: none file exists " + str(files) + ' in ' + source_path, conf)
    else:
        cmd = 'cd ' + source_path + ' && ' + \
            'cp -rp ' + files_to_copy + tmp_path + ' && ' + \
            'cd ' + tmp_base_path + ' && ' + \
            'mv ' + run_id + ' ' + report_prefix + run_id + ' && ' + \
            'tar cjf ' + reports_data_path + '/' + report_archive_file + ' ' + report_prefix + run_id + ' && ' + \
            'mv ' + report_prefix + run_id + ' ' + reports_data_path
            # 'cd ' + base_dir_path + ' && ' + \
            # 'cp -p ../First_Base_Report.htm ' + reports_data_path + '/' + run_id + '/ && ' + \

        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while saving Illumina html reports for run " + run_id, 'Error saving Illumina html reports.\nCommand line:\n' + cmd, conf)
            return False


    # Create index.hml file
    common.create_html_index_file(conf, run_id, [HISEQ_STEP_KEY])

    # Set read only archives files
    os.chmod(reports_data_path + '/' + report_archive_file, stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)
    os.chmod(reports_data_path + '/' + hiseq_log_archive_file, stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)

    return True
