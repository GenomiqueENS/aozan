#!/usr/python
# -*- coding: utf-8 -*-

import os
import common



def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf['sync.done.file'])

def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf['sync.done.file'])


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error(short_message, message, conf['sync.last.error.file'],conf)

def sync(run_id, conf):
    """Synchronize a run.

        Arguments:
                runtId: the run id
                conf: configuration dictionary
        """

    hiseq_data_path = conf['hiseq.data.path']
    work_data_path = conf['work.data.path']
    storage_data_path = conf['storage.data.path']
    tmp_path = conf['tmp.path']


    # Copy data from hiseq path to work path
    cmd = "rsync  -a --exclude '*.cif' --exclude '*_pos.txt' --exclude '*.errorMap' --exclude '*.FWHMMap' " + hiseq_data_path + '/' + run_id + ' ' + work_data_path
    if os.system(cmd)!=0:
        # TODO error()
        return False
    print "HiSeq NAS Synchronisation done."

    # Create if not exists archive directory for the run
    if not os.path.exists(storage_data_path + '/' + run_id):
        os.mkdir(storage_data_path + '/' + run_id)

    # Save quality control data
    base_dir_path = os.getcwd()
    os.mkdir(tmp_path + '/' + run_id)
    cmd = 'cd ' + work_data_path + '/' + run_id + ' & ' + \
        'cp -rp InterOp RunInfo.xml runParameters.xml ' + tmp_path + '/' + run_id + ' & ' + \
        'cd ' + base_dir_path + ' & ' + \
        'tar cjf ' + storage_data_path + '/' + run_id + '/qc_' + run_id + '.tar.bz2 ' + run_id + ' & ' + \
        'rm -rf ' + tmp_path + '/' + run_id
    if os.system(cmd)!=0:
        # TODO error()
        return False
    print "HiSeq quality control data saved."


    # Save html reports
    os.mkdir(tmp_path + '/' + run_id)
    cmd = 'cd ' + base_dir_path + ' & ' + \
        'cp -rp Status_Files reports Status.htm ../First_Base_Report.htm ' + tmp_path + '/' + run_id + ' & ' + \
        'cp -p ../First_Base_Report.htm ' + storage_data_path + '/' + run_id + '/ & ' + \
        'cd ' + tmp_path + ' & ' + \
        'tar cjf ' + storage_data_path + '/' + run_id + '/report_' + run_id + '.tar.bz2 ' + run_id + ' & ' + \
        'cd ' + base_dir_path + ' & ' + \
        'mv ' + tmp_path + '/' + run_id + ' ' + storage_data_path + '/' + run_id + '/report_' + run_id
    if os.system(cmd)!=0:
        # TODO error()
        return False
    print "HiSeq report saved."
    
    return True

