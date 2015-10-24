# -*- coding: utf-8 -*-

'''
Created on 8 déc. 2011

@author: jourdren
'''

import common, hiseq_run, detection_end_run
import os
from java.io import File
from fr.ens.transcriptome.aozan.illumina import RunInfo

from fr.ens.transcriptome.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import HISEQ_DATA_PATH_KEY

def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/first_base_report.done')

def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf[AOZAN_VAR_PATH_KEY] + '/first_base_report.done', conf)


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
                not detection_end_run.check_end_run(f, conf) and \
                os.path.exists(hiseq_data_path + '/' + f + '/First_Base_Report.htm'):

                result.add(f)

    return result


def send_report(run_id, conf):
    """Send a mail with the first base report.

    Arguments:
        run_id: the run id
        conf: configuration dictionary
    """

    #
    # Retrieve features the current run in RunInfos.xml file
    #

    hiseq_run_path = conf[HISEQ_DATA_PATH_KEY] + '/' + run_id

    run_info_path = hiseq_run_path + "/RunInfo.xml"
    run_info = RunInfo.parse(File(run_info_path))

    reads = run_info.getReads()
    error_cycles_per_reads_not_indexes_count = 0
    reads_indexed_count = 0
    reads_not_indexed_count = 0
    cycles_count = 0
    cycles_per_reads_not_indexed = 0

    for read in reads:
        cycles_count += read.getNumberCycles()
        if read.isIndexedRead():
            reads_indexed_count += 1
        else:
            reads_not_indexed_count += 1
            if (cycles_per_reads_not_indexed == 0):
                cycles_per_reads_not_indexed = read.getNumberCycles()

            # Check same cycles count for each reads not indexed
            error_cycles_per_reads_not_indexes_count = cycles_per_reads_not_indexed != read.getNumberCycles()


    # Identification type run according to data in RunInfos.xml : SR or PE
    if reads_not_indexed_count == 1:
        type_run_estimated = "SR-" + str(cycles_per_reads_not_indexed - 1) + " with " + str(reads_indexed_count) + " index(es)"
    elif reads_not_indexed_count == 2:
        type_run_estimated = "PE-" + str(cycles_per_reads_not_indexed - 1) + " with " + str(reads_indexed_count) + " index(es)"
    else :
        type_run_estimated = "Undetermined run type (" + str(reads_not_indexed_count) + " reads with " + str(reads_indexed_count) + " index(es))"

    description_run = "Informations about this run :\n"
    description_run += "\t- " + str(run_info.getFlowCellLaneCount()) + " lanes with " + str(run_info.alignToPhix.size()) + " aligned to Phix.\n"
    description_run += "\t- " + str(reads_not_indexed_count) + " read(s) and " + str(reads_indexed_count) + " index(es).\n"

    if error_cycles_per_reads_not_indexes_count or cycles_per_reads_not_indexed == 0:
        description_run += "\t- ERROR : cycles count per reads different between reads (" + str(cycles_count) + " total cycles).\n"
    else:
        description_run += "\t- " + str(cycles_per_reads_not_indexed) + " cycles per reads (" + str(cycles_count) + " total cycles).\n"

    description_run += "\t- " + "estimated run type : " + type_run_estimated + ".\n"

    attachment_file = str(hiseq_run.find_hiseq_run_path(run_id, conf)) + '/' + run_id + '/First_Base_Report.htm'
    message = 'You will find attached to this message the first base report for the run ' + run_id + '.\n\n' + description_run
    common.send_msg_with_attachment('[Aozan] First base report for HiSeq run ' + type_run_estimated + '  ' + run_id , message, attachment_file, conf)
