
import common
from java.io import File
from fr.ens.transcriptome.eoulsan.illumina import RunInfo


def estimate(run_id, conf):
    """Estimate space needed in directories : hiseq, bcl and fastq
    if it has not enough space free, mail a warning message 

    Arguments:
        lock_file_path path of the lock file
    """
    common.log('INFO', 'in estimate method', conf)
    
    # ratio space by cycle by informations type
    fastq_space_per_lane_per_cycle = 223666051
    bcl_space_per_lane_per_cycle = 415296917
    hiseq_space_per_lane_per_cycle = 3179924808
    
    hiseq_run_path = conf['hiseq.data.path'] + '/' + run_id
        
    # retrieve data from RunInfo.xml
    run_info_path = hiseq_run_path + "/RunInfo.xml"
    run_info = RunInfo()
    run_info.parse(File(run_info_path))

    # retrieve count lane
    lane_count = run_info.getFlowCellLaneCount()
    common.log('INFO', 'count lane ' + str(lane_count), conf)
    
    # retrieve count cycle
    cycle_count = 0
    for read in run_info.getReads():
        cycle_count = cycle_count + read.getNumberCycles()
    
    common.log('INFO', 'count cycle ' + str(cycle_count), conf)
      
    # retrieve data from runParameters.xml 
    run_info_path = hiseq_run_path + "/runParameters.xml"
    
    # set factor
    factor = lane_count * cycle_count * 1.15 
    
    common.log('INFO', 'factor ' + str(factor), conf)
    
    #
    # Estimate space needed +15%
    #
    
    # for hiseq data
    hiseq_space_needed = hiseq_space_per_lane_per_cycle * factor
    hiseq_space_free = common.df(conf['hiseq.data.path'])
    
    # check if free space is available
    if hiseq_space_needed < hiseq_space_free:
        error(run_id, 'hiseq files', hiseq_space_needed, hiseq_space_free, conf)
    else:
        log_message(run_id, 'hiseq files', hiseq_space_needed, hiseq_space_free, conf)
    
    # for bcl files    
    # bcl file : compressed or not
    compressed_bcl_file = False

    if compressed_bcl_file:
        ratio_bcl_file_compressed = 5.0  # TO CONFIRM RATIO
    else:
        ratio_bcl_file_compressed = 1.0
    
    # quality data for Q30 : compressed or not
    compressed_quality_data = False
    
    if compressed_quality_data:
        ratio_quality_compressed = 5.0  # TO CONFIRM RATIO
    else:
        ratio_quality_compressed = 1.0
    
    common.log('INFO', 'ratio fastq ' + str(ratio_bcl_file_compressed) + ' ratio qty ' + str(ratio_quality_compressed), conf)
    
    bcl_space_needed = bcl_space_per_lane_per_cycle * factor * ratio_bcl_file_compressed * ratio_quality_compressed
    bcl_space_free = common.df(conf['bcl.data.path'])
    
    # check if free space is available
    if bcl_space_needed < bcl_space_free:
        error(run_id, 'bcl files', bcl_space_needed, bcl_space_free, conf)
    else:
        log_message(run_id, 'bcl files', bcl_space_needed, bcl_space_free, conf)
    
    # for fastq files
    fastq_space_needed = fastq_space_per_lane_per_cycle * factor
    fastq_space_free = common.df(conf['fastq.data.path'])
    
    # check if free space is available
    if fastq_space_needed < fastq_space_free:
        error(run_id, 'fastq files', fastq_space_needed, fastq_space_free, conf) 
    else:
        log_message(run_id, 'fastq files', fastq_space_needed, fastq_space_free, conf) 



def error(run_id, type_file, space_needed, space_free, conf):
    """Error handling.

    Arguments:
        run_id: the run id 
        type_file: files concerned
        space_needed: space needed for the run id for a type of data
        space_free: space free for the run id for a type of data
        conf: configuration dictionary
    """

    short_message = "Not enough disk space to store " + type_file + " for run " + run_id
    message = type_file + " : not enough disk space to store files for run " + run_id + '.\n%.2f Gb' % (space_needed / 1024 / 1024 / 1024) 
    message = message + ' is needed, it is free space %.2f Gb ' % (space_free / 1024 / 1024 / 1024)
        
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
    