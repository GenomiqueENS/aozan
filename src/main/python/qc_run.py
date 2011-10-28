'''
Created on 28 oct. 2011

@author: jourdren
'''

import common

def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf['aozan.var.path'] + '/demux.done')

def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(conf['aozan.var.path'] + '/demux.done')


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] demultiplexer: ' + short_message, message, conf['aozan.var.path'] + '/demux.lasterr', conf)


def qc(run_id, conf):

    print "qc for " + run_id

    reports_data_path = conf['reports.data.path']
    tmp_path = conf['tmp.path']

    if common.df(reports_data_path) < 10 * 1024 * 1024 * 1024:
        error("Not enough disk space to store aozan reports for run " + run_id, "Not enough disk space to store aozan reports for run " + run_id +
              '.\nNeed more than 10 Gb on ' + reports_data_path + '.', conf)