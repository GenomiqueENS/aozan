# -*- coding: utf-8 -*-

'''
Created on 6 avril 2015

With include NextSeq management, replace the old first_base_report.py script

@author: Laurent Jourdren
@author: Sandrine Perrin
'''

import common, aozan, hiseq_run, detection_end_run
import estimate_space_needed
import os

from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import FIRST_BASE_REPORT_STEP_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_STEP_KEY

DONE_FILE = 'first_base_report.done'
FIRST_BASE_REPORT_FILE = 'First_Base_Report.htm'


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + DONE_FILE)


def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf[AOZAN_VAR_PATH_KEY] + '/' + DONE_FILE, conf)


def get_available_run_ids(conf):
    """Get the list of the available runs.

    Arguments:
        conf: configuration dictionary
    """

    result = set()
    hiseq_run_ids_do_not_process = hiseq_run.load_deny_run_ids(conf)

    for hiseq_data_path in hiseq_run.get_hiseq_data_paths(conf):

        files = os.listdir(hiseq_data_path)
        for f in files:

            # Do not process denied runs
            if f in hiseq_run_ids_do_not_process:
                continue

            if not (os.path.isdir(hiseq_data_path + '/' + f) and hiseq_run.check_run_id(f, conf)):
                # No valid entry
                continue

            # NextSeq sequencer create this file after clusterisation step
            if not os.path.exists(hiseq_data_path + '/' + f + '/RunInfo.xml'):
                continue

            if common.get_rta_major_version(f, conf) == 1 and \
                    not os.path.exists(hiseq_data_path + '/' + FIRST_BASE_REPORT_FILE):
                continue

            if not detection_end_run.check_end_run(f, conf):
                # TODO
                # os.path.exists(hiseq_data_path + '/' + f + '/First_Base_Report.htm'):
                result.add(f)

    return result


def discover_new_run(conf):
    """Discover new runs.

    Arguments:
        conf: configuration object
    """

    #
    # Discover new run
    #

    run_already_discovered = load_processed_run_ids(conf)

    if common.is_conf_value_equals_true(FIRST_BASE_REPORT_STEP_KEY, conf):
        for run_id in (get_available_run_ids(conf) - run_already_discovered):
            aozan.welcome(conf)
            common.log('INFO',
                       'First base report ' + run_id + ' on sequencer ' + common.get_instrument_name(run_id, conf),
                       conf)
            if send_report(run_id, conf):
                add_run_id_to_processed_run_ids(run_id, conf)
                run_already_discovered.add(run_id)

            # Verify space needed during the first base report
            estimate_space_needed.estimate(run_id, conf)

    #
    # Discover hiseq run done
    #

    return detection_end_run.discovery_run(conf)


def send_report(run_id, conf):
    """Send a mail with the first base report.

    Arguments:
        run_id: the run id
        conf: configuration dictionary
    """

    #
    # Retrieve features the current run in RunInfos.xml file
    #

    run_info = hiseq_run.get_run_info(run_id, conf)

    if run_info is None:
        return False

    # TODO ?? add check sample-sheet if demux step enable
    # add warning in report if useful

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
            if cycles_per_reads_not_indexed == 0:
                cycles_per_reads_not_indexed = read.getNumberCycles()

            # Check same cycles count for each reads not indexed
            error_cycles_per_reads_not_indexes_count = cycles_per_reads_not_indexed != read.getNumberCycles()

    # Identification type run according to data in RunInfos.xml : SR or PE
    if reads_not_indexed_count == 1:
        type_run_estimated = "SR-" + str(cycles_per_reads_not_indexed) + " with " + str(
            reads_indexed_count) + " index"
        if reads_indexed_count > 1:
            type_run_estimated += "es"
    elif reads_not_indexed_count == 2:
        type_run_estimated = "PE-" + str(cycles_per_reads_not_indexed) + " with " + str(
            reads_indexed_count) + " index(es)"
        if reads_indexed_count > 1:
            type_run_estimated += "es"
    else:
        type_run_estimated = "Undetermined run type (" + str(reads_not_indexed_count) + " reads with " + str(
            reads_indexed_count) + " index)"
        if reads_indexed_count > 1:
            type_run_estimated += "es"
        type_run_estimated += ")"

    description_run = "Informations about this run:\n"
    description_run += "\t- " + str(run_info.getFlowCellLaneCount()) + " lanes with " + str(
        run_info.alignToPhix.size()) + " aligned to Phix.\n"
    description_run += "\t- " + str(reads_not_indexed_count) + " read"
    if reads_not_indexed_count > 1:
        description_run += "s"
    description_run += " and " + str(reads_indexed_count) + " index"
    if reads_indexed_count > 1:
        description_run += "es"
    description_run += ".\n"

    if error_cycles_per_reads_not_indexes_count or cycles_per_reads_not_indexed == 0:
        description_run += "\t- ERROR : cycles count per reads different between reads (" + str(
            cycles_count) + " total cycles).\n"
    else:
        description_run += "\t- " + str(cycles_per_reads_not_indexed) + " cycles per reads (" + str(
            cycles_count) + " total cycles).\n"

    description_run += "\t- " + "Estimated run type: " + type_run_estimated + ".\n"

    rta_major_version = common.get_rta_major_version(run_id, conf)

    if rta_major_version == 1:
        # With HiSeq send the first base report file
        attachment_file = str(hiseq_run.find_hiseq_run_path(run_id, conf)) + '/' + run_id + '/' + FIRST_BASE_REPORT_FILE

        # Do not send a message if the First base report file does not exists
        if not os.path.isfile(attachment_file):
            return False

        message = 'You will find attached to this message the first base report for the run ' + \
                  run_id + '.\n\n' + description_run
        common.send_msg_with_attachment('[Aozan] First base report for the run ' + type_run_estimated + '  ' + run_id +
                                        ' on ' + common.get_instrument_name(run_id, conf),
                                        message, attachment_file, conf)

    else:
        # With other no attachment file
        message = 'You will find below the parameters of the run ' + run_id + '.\n\n' + description_run
        common.send_msg('[Aozan] New run ' + type_run_estimated + ' ' + run_id + ' on ' +
                        common.get_instrument_name(run_id, conf), message,
                        False, conf)

    return True
