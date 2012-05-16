# -*- coding: utf-8 -*-

'''
Created on 8 déc. 2011

@author: jourdren
'''

import common, hiseq_run
import os

def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf['aozan.var.path'] + '/first_base_report.done')

def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf['aozan.var.path'] + '/first_base_report.done', conf)


def get_available_run_ids(conf):
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
                not hiseq_run.check_end_run(f, conf) and \
                os.path.exists(hiseq_data_path + '/' + f + '/First_Base_Report.htm'):

                result.add(f)

    return result


def send_report(run_id, conf):
    """Send a mail with the first base report.
    
    Arguments:
        run_id: the run id
        conf: configuration dictionary
    """

    attachment_file = hiseq_run.find_hiseq_run_path(run_id, conf) + '/' + run_id + '/First_Base_Report.htm'
    message = 'You will find attached to this message the first base report for the run ' + run_id + '.'
    common.send_msg_with_attachment('[Aozan] First base report for HiSeq run ' + run_id, message, attachment_file, conf)
