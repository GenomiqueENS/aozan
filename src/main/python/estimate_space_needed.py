# -*- coding: utf-8 -*-
'''
Created on 15 avril 2012

@author: Sandrine Perrin
'''

import common
from java.io import File
from fr.ens.transcriptome.eoulsan.illumina import RunInfo
from xml.dom.minidom import parse

def estimate(run_id, conf):
    """Estimate space needed in directories : hiseq, bcl and fastq
    if it has not enough space free send a warning mail 

    Arguments:
        run_id: the run id 
        conf: configuration dictionary
    """
    
    # ratio space by cycle by informations type
    hiseq_run_path = conf['hiseq.data.path'] + '/' + run_id
        
    # retrieve data from RunInfo.xml
    run_info_path = hiseq_run_path + "/RunInfo.xml"
    run_info = RunInfo()
    run_info.parse(File(run_info_path))

    # retrieve count lane
    lane_count = run_info.getFlowCellLaneCount()
    
    # retrieve count cycle
    cycle_count = 0
    for read in run_info.getReads():
        cycle_count = cycle_count + read.getNumberCycles()
    
    # retrieve data from runParameters.xml 
    run_param_path = hiseq_run_path + "/runParameters.xml"
    
    #
    # Estimate space needed +10%
    #
    
    # set factor
    run_factor = lane_count * cycle_count * 1.10 
        
    # for hiseq data
    hiseq_space_needed = int(conf['hiseq.space.factor']) * run_factor
    hiseq_space_free = common.df(conf['hiseq.data.path'])
    
    # check if the remaining space on the directory is inferior at 5 percent 
    hiseq_space_remaining_not_enough = (hiseq_space_free - hiseq_space_needed) < (long(File(conf['hiseq.data.path']).getTotalSpace()) * 0.05)
    
    print 'hiseq tot ' + str(long(File(conf['hiseq.data.path']).getTotalSpace())) + 'hiseq_space_remaining_not_enough' + str(hiseq_space_remaining_not_enough)
    
    # check if free space is available
    if (hiseq_space_needed > hiseq_space_free) or (hiseq_space_remaining_not_enough):
        error(run_id, 'hiseq files', hiseq_space_needed, hiseq_space_free, hiseq_space_remaining_not_enough, conf)
    else:
        log_message(run_id, 'hiseq files', hiseq_space_needed, hiseq_space_free, conf)
    
    # for bcl files    
    # bcl file : compressed or not, info in runParameters.xml
    ratio_bcl = ratio_bcl_compressed(run_param_path)
    
    # quality data for Q30 : compressed or not
    ratio_quality = compressed_quality_data(run_param_path)
    
    bcl_space_needed = int(conf['bcl.space.factor']) * run_factor * ratio_bcl * ratio_quality
    bcl_space_free = common.df(conf['bcl.data.path'])
    
    # check if the remaining space on the directory is inferior at 5 percent 
    bcl_space_remaining_not_enough = (bcl_space_free - bcl_space_needed) < (long(File(conf['bcl.data.path']).getTotalSpace()) * 0.05)
    
    print 'bcl tot ' + str(long(File(conf['bcl.data.path']).getTotalSpace())) + 'bcl_space_remaining_not_enough ' + str(bcl_space_remaining_not_enough)
    
    # check if free space is available
    if (bcl_space_needed > bcl_space_free) or (bcl_space_remaining_not_enough):
        error(run_id, 'bcl files', bcl_space_needed, bcl_space_free, bcl_space_remaining_not_enough, conf)
    else:
        log_message(run_id, 'bcl files', bcl_space_needed, bcl_space_free, conf)
    
    # for fastq files
    fastq_space_needed = int(conf['fastq.space.factor']) * run_factor
    fastq_space_free = common.df(conf['fastq.data.path'])
    
    # check if the remaining space on the directory is inferior at 5 percent 
    fastq_space_remaining_not_enough = (fastq_space_free - fastq_space_needed) < (long(File(conf['fastq.data.path']).getTotalSpace()) * 0.05)
    
    print 'fastq tot ' + str(long(File(conf['fastq.data.path']).getTotalSpace())) + 'fastq_space_remaining_not_enough ' + str(fastq_space_remaining_not_enough)
    
    # check if free space is available
    if (fastq_space_needed > fastq_space_free) or (fastq_space_remaining_not_enough):
        error(run_id, 'fastq files', fastq_space_needed, fastq_space_free, fastq_space_remaining_not_enough, conf) 
    else:
        log_message(run_id, 'fastq files', fastq_space_needed, fastq_space_free, conf) 


  
def error(run_id, type_file, space_needed, space_free, space_remaining_not_enough, conf):
    """Error handling.

    Arguments:
        run_id: the run id 
        type_file: type file concerned
        space_needed: space needed for the run for a type of data
        space_free: space free for the run for a type of data
        space_remaining_not_enough: true if remaining space in directory at the end is estimated inferior at 5%
        conf: configuration dictionary
    """

    short_message = "Not enough disk space to store " + type_file + " for run " + run_id
    message = type_file + " : not enough disk space to store files for run " + run_id + '.\n%.2f Gb' % (space_needed / 1024 / 1024 / 1024) 
    message = message + ' is needed, it is free space %.2f Gb ' % (space_free / 1024 / 1024 / 1024)
    
    if space_remaining_not_enough:
        message = message + 'Remaining space in the directory at the end is estimated inferior at 5 percent'
        
    # send warning mail
    common.error('[Aozan] estimate space needed : ' + short_message, message, conf['aozan.var.path'] + '/space_estimated.lasterr', conf)
    
    
    
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
