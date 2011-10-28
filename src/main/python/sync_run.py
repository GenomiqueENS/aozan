# -*- coding: utf-8 -*-

import os, time
import common


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf['aozan.var.path'] + '/sync.done')

def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf['aozan.var.path'] + '/sync.done')


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] synchronizer: ' + short_message, message, conf['aozan.var.path'] + '/sync.lasterr', conf)

def sync(run_id, conf):
    """Synchronize a run.

        Arguments:
                runtId: the run id
                conf: configuration dictionary
        """

    start_time = time.time()
    hiseq_data_path = conf['hiseq.data.path']
    work_data_path = conf['work.data.path']
    reports_data_path = conf['reports.data.path']
    tmp_path = conf['tmp.path']

    input_path = hiseq_data_path + '/' + run_id
    input_path_du = common.du(input_path)
    output_df = common.df(work_data_path)
    du_factor = float(conf['sync.space.factor'])
    space_needed = input_path_du * du_factor

    # Check if free space is available on 
    if output_df < space_needed:
        error("Not enough disk space to perform synchronization for run " + run_id, "Not enough disk space to perform synchronization for run " + run_id +
              '.\n%.2f Gb' % (space_needed / 1024 / 1024 / 1024) + ' is needed (factor x' + du_factor + ') on ' + work_data_path + '.', conf)
        return False

    if common.df(reports_data_path) < 10 * 1024 * 1024 * 1024:
        error("Not enough disk space to store aozan reports for run " + run_id, "Not enough disk space to store aozan reports for run " + run_id +
              '.\nNeed more than 10 Gb on ' + reports_data_path + '.', conf)


    # Copy data from hiseq path to work path
    cmd = "rsync  -a --exclude '*.cif' --exclude '*_pos.txt' --exclude '*.errorMap' --exclude '*.FWHMMap' " + input_path + ' ' + work_data_path
    if os.system(cmd) != 0:
        error("error while executing rsync for run " + run_id, 'Error while executing rsync.\nCommand line:\n' + cmd, conf)
        return False

    # Create if not exists archive directory for the run
    if not os.path.exists(reports_data_path + '/' + run_id):
        os.mkdir(reports_data_path + '/' + run_id)

    # Save quality control data
    base_dir_path = os.getcwd()
    os.mkdir(tmp_path + '/' + run_id)
    cmd = 'cd ' + work_data_path + '/' + run_id + ' & ' + \
        'cp -rp InterOp RunInfo.xml runParameters.xml ' + tmp_path + '/' + run_id + ' & ' + \
        'cd ' + base_dir_path + ' & ' + \
        'tar cjf ' + reports_data_path + '/' + run_id + '/qc_' + run_id + '.tar.bz2 ' + run_id + ' & ' + \
        'rm -rf ' + tmp_path + '/' + run_id
    if os.system(cmd) != 0:
        error("error while saving Illumina quality control for run " + run_id, 'Error saving Illumina quality control.\nCommand line:\n' + cmd, conf)
        return False

    # Save html reports
    os.mkdir(tmp_path + '/' + run_id)
    cmd = 'cd ' + base_dir_path + ' & ' + \
        'cp -rp Status_Files reports Status.htm ../First_Base_Report.htm ' + tmp_path + '/' + run_id + ' & ' + \
        'cp -p ../First_Base_Report.htm ' + reports_data_path + '/' + run_id + '/ & ' + \
        'cd ' + tmp_path + ' & ' + \
        'tar cjf ' + reports_data_path + '/' + run_id + '/report_' + run_id + '.tar.bz2 ' + run_id + ' & ' + \
        'cd ' + base_dir_path + ' & ' + \
        'mv ' + tmp_path + '/' + run_id + ' ' + reports_data_path + '/' + run_id + '/report_' + run_id
    if os.system(cmd) != 0:
        error("error while saving Illumina html reports for run " + run_id, 'Error saving Illumina html reports.\nCommand line:\n' + cmd, conf)
        return False

    duration = time.time() - start_time
    df = common.df(work_data_path) / (1024 * 1024 * 1024)
    du = common.du(work_data_path + '/' + run_id) / (1024 * 1024)

    common.send_msg("[Aozan] End of synchronization for run " + run_id, \
                    'End of synchronization for run ' + run_id + 'with no error in ' + str(duration) + ' seconds.\nFastq files for this run ' +
                    'can be found in the following directory:\n  ' + work_data_path + '/' + run_id + \
                    '\n\n%.2f Gb has been used,' % du + ' %.2f Gb still free.' % df, conf)

    return True

