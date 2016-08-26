# -*- coding: utf-8 -*-

import os, os.path, time
import stat, detection_end_run
import common

from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_CRITICAL_MIN_SPACE_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.illumina import RunInfo

# from pickle import FALSE
# from macpath import pathsep

def load_deny_run_ids(conf):
    """Load the list of the run ids to not process.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + common.HISEQ_DENY_FILE)


def get_runinfos_file(run_id, conf):
    """Get the RunInfos.xml path.

    Arguments:
        run_id: the run id
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
        run_id: the run id
        conf: configuration dictionary
    """

    file_src = get_runinfos_file(run_id, conf)

    if file_src is None:
        return None

    return RunInfo.parse(file_src)


def get_read_count(run_id, conf):
    """Get the number of read of a run.

        Arguments:
            run_id: the run id
            conf: configuration dictionary
    """

    run_info = get_run_info(run_id, conf)

    if run_info is None:
        return -1

    return run_info.getReads().size()


def get_lane_count(run_id, conf):
    """Get the number of lanes of a run.

        Arguments:
            run_id: the run id
            conf: configuration dictionary
    """

    run_info = get_run_info(run_id, conf)

    if run_info is None:
        return -1

    return run_info.getFlowCellLaneCount()


def check_run_id(run_id, conf):
    """Check if the run id is valid.

    Arguments:
        run_id: the run id
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


def get_working_run_ids(conf):
    """Get the list of the workfing runs.

    Arguments:
        conf: configuration dictionary
    """

    result = set()

    for hiseq_data_path in get_hiseq_data_paths(conf):

        files = os.listdir(hiseq_data_path)
        for f in files:
            if os.path.isdir(hiseq_data_path + '/' + f) and check_run_id(f,
                                                                         conf) and not check_end_run(
                f, conf):
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
                                'There is only %.2f' % (df / (1024 * 1024 * 1024)) +
                                ' Gb left for run storage in ' + path + '. ' +
                                ' The current warning threshold is set to %.2f' % (
                                    free_space_threshold / (1024 * 1024 * 1024)) + ' Gb.', False, conf)


def get_hiseq_data_paths(conf):
    """Get the hiseq data path from the Aozan configuration.

    Arguments:
        conf: configuration dictionary

    Returns:
        a list of strings with the path of the hiseq output directories
    """

    paths = conf[HISEQ_DATA_PATH_KEY].split(':')

    for i in range(len(paths)):
        paths[i] = paths[i].strip()

    return paths


def find_hiseq_run_path(run_id, conf):
    """Find the path of run in the hiseq output directories.

    Arguments:
        run_id: the run id to search
        conf: configuration dictionary

    Returns:
        The path of the run or None if not found
    """

    for path in get_hiseq_data_paths(conf):

        path_to_test = path.strip() + '/' + run_id

        if os.path.exists(path_to_test):
            return path.strip()

    return False

def check_end_run(run_id, conf):
    """Check the end of a run data transfer.

    Arguments:
        run_id: the run id
        conf: configuration dictionary
    """

    hiseq_data_path = find_hiseq_run_path(run_id, conf)
    reads_number = get_read_count(run_id, conf)

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
        file_to_test = hiseq_data_path + '/' + run_id + '/' + _build_read_complete_filename(run_id, i, conf)

        if not os.path.exists(file_to_test):
            return False

    if not os.path.exists(hiseq_data_path + '/' + run_id + '/RTAComplete.txt'):
        return False

    return True


def check_end_run_since(run_id, secs, conf):
    """Check the end of a run data transfer since a number of seconds.

    Arguments:
        run_id: the run id
        secs: maximal number of seconds
        conf: configuration dictionary
    """

    hiseq_data_path = find_hiseq_run_path(run_id, conf)
    if hiseq_data_path is False:
        return -1

    reads_number = get_read_count(run_id, conf)
    last = 0

    for i in range(reads_number):
        file_to_test = hiseq_data_path + '/' + run_id + '/' + _build_read_complete_filename(run_id, i, conf)
        if not os.path.exists(file_to_test):
            common.log('SEVERE', 'DEBUG: check file on end run is not found ' + str(file_to_test), conf)
            return -2
        else:
            m_time = os.stat(file_to_test).st_mtime
            if m_time > last:
                last = m_time

    if (time.time() - last) < secs:
        return last

    return 0


def _build_read_complete_filename(run_id, i, conf):
    if common.get_rta_major_version(run_id, conf) == 1:
        return 'Basecalling_Netcopy_complete_Read' + str(i + 1) + '.txt'

    else:
        return 'RTARead' + str(i + 1) + 'Complete.txt'
