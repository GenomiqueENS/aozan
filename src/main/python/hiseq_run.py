# -*- coding: utf-8 -*-

import os, time
import stat, detection_end_run
import common

from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_CRITICAL_MIN_SPACE_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_STEP_KEY
from fr.ens.biologie.genomique.aozan.Settings import TMP_PATH_KEY
from fr.ens.biologie.genomique.aozan.illumina import RunInfo

# from pickle import FALSE
# from macpath import pathsep

DENY_FILE = 'hiseq.deny'


def load_deny_run_ids(conf):
    """Load the list of the run ids to not process.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + DENY_FILE)


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


def get_available_run_ids(conf):
    """Get the list of the available runs.

    Arguments:
        conf: configuration dictionary
    """

    result = set()

    for hiseq_data_path in get_hiseq_data_paths(conf):

        files = os.listdir(hiseq_data_path)
        for f in files:
            if os.path.isdir(hiseq_data_path + '/' + f) and \
                    check_run_id(f, conf) and \
                    detection_end_run.check_end_run(f, conf):
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
            if os.path.isdir(hiseq_data_path + '/' + f) and check_run_id(f,
                                                                         conf) and not detection_end_run.check_end_run(
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
    paths = conf[HISEQ_DATA_PATH_KEY].split(':')

    for i in range(len(paths)):
        paths[i] = paths[i].strip()

    return paths


def find_hiseq_run_path(run_id, conf):
    for path in get_hiseq_data_paths(conf):

        path_to_test = path.strip() + '/' + run_id

        if os.path.exists(path_to_test):
            return path.strip()

    return False
