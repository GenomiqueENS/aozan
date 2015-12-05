# -*- coding: utf-8 -*-
'''
Created on 15 avril 2012

@author: Sandrine Perrin
'''

import common, hiseq_run
from java.io import File
from xml.dom.minidom import parse

from fr.ens.transcriptome.aozan.Settings import HISEQ_DATA_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import AOZAN_VAR_PATH_KEY

def estimate(run_id, conf):
    """Estimate space needed in directories : hiseq, bcl and fastq
    if it has not enough space free send a warning mail

    Arguments:
        run_id: the run id
        conf: configuration dictionary
    """

    # retrieve data from RunInfo.xml
    run_info = hiseq_run.get_run_info(run_id, conf)

    # retrieve count lane
    lane_count = run_info.getFlowCellLaneCount()

    # retrieve count cycle
    cycle_count = 0
    for read in run_info.getReads():
        cycle_count = cycle_count + read.getNumberCycles()

    # retrieve data from runParameters.xml
    run_param_path = common.get_runparameters_path(run_id, conf)

    #
    # Estimate space needed +10%
    #

    # Set factor and ratio util
    run_factor = lane_count * cycle_count * 1.10
    # bcl file : compressed or not, info in runParameters.xml
    ratio_bcl = ratio_bcl_compressed(run_param_path)

    # quality data for Q30 : compressed or not
    ratio_quality = compressed_quality_data(run_param_path)

    # for hiseq data
    check_space_needed_and_free(run_id, 'hiseq', run_factor, conf)

    # for bcl files
    check_space_needed_and_free(run_id, 'bcl', run_factor * ratio_bcl * ratio_quality, conf)

    # for fastq files
    check_space_needed_and_free(run_id, 'fastq', run_factor, conf)


def check_space_needed_and_free(run_id, type_file, run_factor, conf):
    """Compute free and needed space for type file and send warning mail if not enough.

    Arguments:
        run_id: the run id
        type_file: type file concerned
        factor: factor to estimate space needed by current run
        conf: configuration dictionary
    """

    space_unit = int(conf[type_file + '.space.factor'])
    data_paths = conf[type_file + '.data.path']

    for data_path in data_paths.split(':'):

        data_path = data_path.strip()
        space_needed = space_unit * run_factor
        space_free = common.df(data_path)

        # check if the remaining space on the directory is inferior at 5 percent
        space_remaining_not_enough = (space_free - space_needed) < (long(File(data_path).getTotalSpace()) * 0.05)

        # check if free space is available
        if (space_needed > space_free) or (space_remaining_not_enough):
            error(run_id, type_file + ' files', space_needed, space_free, data_path, conf)
        else:
            log_message(run_id, type_file + ' files', space_needed, space_free, conf)


def error(run_id, type_file, space_needed, space_free, dir_path, conf):
    """Error handling.

    Arguments:
        run_id: the run id
        type_file: type file concerned
        space_needed: space needed for the run for a type of data
        space_free: space free for the run for a type of data
        dir_path: directory path
        conf: configuration dictionary
    """

    short_message = "not enough disk space to store " + type_file + " for run " + run_id
    message = type_file + " : not enough disk space to store files for run " + run_id + ' on ' + dir_path + '.\n'
    message = message + '%.2f GB' % (space_needed / 1024 / 1024 / 1024) + ' is needed by Aozan'
    message = message + ' however only %.2f GB' % (space_free / 1024 / 1024 / 1024) + ' of free space is currently available on this storage.'

    # send warning mail
    common.error('[Aozan] Estimate space needed : ' + short_message, message, conf[AOZAN_VAR_PATH_KEY] + '/space_estimated.lasterr', conf)


def log_message(run_id, type_file, space_needed, space_free, conf):
    """log message.

    Arguments:
        run_id: the run id
        type_file: files concerned
        space_needed: space needed for the run id for a type of data
        space_free: space free for the run id for a type of data
        conf: configuration dictionary
    """

    message = type_file + " : enough disk space to store files for run " + run_id + '.\n%.2f Gb' % (space_needed / 1024 / 1024 / 1024)
    message = message + ' is needed, it is free space %.2f Gb ' % (space_free / 1024 / 1024 / 1024)
    common.log('INFO', message, conf)


def ratio_bcl_compressed(run_param_path):
    """
    Arguments:
        run_param_path : path to runParameters.xml
    """

    # for bcl files
    # bcl file : compressed or not, info in runParameters.xml
    compressed_bcl_file = False
    balise = "CompressBcls";

    doc = parse(run_param_path)

    if doc.documentElement.getElementsByTagName(balise).__len__() > 0:
        el = doc.documentElement.getElementsByTagName(balise)[0]
        if el.nodeType == el.ELEMENT_NODE:
            txt = el.childNodes[0].nodeValue
            if txt == 'true':
                compressed_bcl_file = True

    if compressed_bcl_file:
        ratio_bcl_file_compressed = 5.0  # TO DEFINE RATIO
    else:
        ratio_bcl_file_compressed = 1.0

    return ratio_bcl_file_compressed

def compressed_quality_data(run_param_path):
    """
    Arguments:
        run_param_path : path to runParameters.xml
    """

    compressed_quality_data = False

    if compressed_quality_data:
        ratio_quality_compressed = 1.0  # TO DEFINE RATIO
    else:
        ratio_quality_compressed = 1.0

    return ratio_quality_compressed
