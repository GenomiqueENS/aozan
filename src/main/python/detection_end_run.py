# -*- coding: utf-8 -*-

import os, time
import stat, hiseq_run, aozan
from xml.etree.ElementTree import ElementTree

import common
from fr.ens.transcriptome.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import HISEQ_CRITICAL_MIN_SPACE_KEY
from fr.ens.transcriptome.aozan.Settings import HISEQ_DATA_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import HISEQ_STEP_KEY
from fr.ens.transcriptome.aozan.Settings import TMP_PATH_KEY
import cmd
from pickle import FALSE

DONE_FILE = 'detection_end_run.done'
 

def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/hiseq.done')


def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf[AOZAN_VAR_PATH_KEY] + '/hiseq.done', conf)

def discovery_run(conf):
    """Discover new ended runs  

    Arguments:
        conf: configuration object  
    """
    
    run_ids_done = load_processed_run_ids(conf)

    if common.is_conf_value_equals_true(HISEQ_STEP_KEY, conf):
        for run_id in (hiseq_run.get_available_run_ids(conf) - run_ids_done):
            aozan.welcome(conf)
            common.log('INFO', 'Discover end run ' + run_id + ' on sequencer ' + common.get_sequencer_type(run_id, conf), conf)
            
            if hiseq_run.create_run_summary_reports(run_id, conf):
                hiseq_run.send_mail_if_recent_run(run_id, 12 * 3600, conf)
                add_run_id_to_processed_run_ids(run_id, conf)
                run_ids_done.add(run_id)
            else:
                raise Exception('Create run summary report for new discovery run ' + run_id)

    return run_ids_done


def check_end_run(run_id, conf):
    """Check the end of a run data transfert.

    Arguments:
        runtId: the run id
        conf: configuration dictionary
    """

    hiseq_data_path = hiseq_run.find_hiseq_run_path(run_id, conf)
    if hiseq_data_path == False:
        return False

    reads_number = hiseq_run.get_reads_number(run_id, conf)

    # if reads_number equals -1, runParameters.xml is missing
    if reads_number == -1:
        return False

    for i in range(reads_number):
        if not os.path.exists(hiseq_data_path + '/' + run_id + '/Basecalling_Netcopy_complete_Read' + str(i + 1) + '.txt'):
            return False

    if not os.path.exists(hiseq_data_path + '/' + run_id + '/RTAComplete.txt'):
        return False

    return True

def check_end_run_since(run_id, secs, conf):
    """Check the end of a run data transfert since a number of seconds.

    Arguments:
        runtId: the run id
        secs: maximal number of seconds
        conf: configuration dictionary
    """

    hiseq_data_path = hiseq_run.find_hiseq_run_path(run_id, conf)
    if hiseq_data_path == False:
        return 0

    reads_number = hiseq_run.get_reads_number(run_id, conf)
    last = 0

    for i in range(reads_number):
        file_to_test = hiseq_data_path + '/' + run_id + '/Basecalling_Netcopy_complete_Read' + str(i + 1) + '.txt'
        if not os.path.exists(file_to_test):
            return 0
        else:
            m_time = os.stat(file_to_test).st_mtime
            if m_time > last:
                last = m_time

    if (time.time() - last) < secs:
        return last
    return 0

