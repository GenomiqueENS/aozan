# -*- coding: utf-8 -*-

'''
Created on 25 oct. 2011

@author: Laurent Jourdren
'''
import os.path
import common, hiseq, time
from java.lang import Runtime
from fr.ens.transcriptome.aozan import CasavaDesignXLSToCSV
from fr.ens.transcriptome.eoulsan import EoulsanException

def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf['demux.done.file'])

def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf['demux.done.file'])


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error(short_message, message, conf['demux.last.error.file'],conf)


def demux(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    start_time = time.time()
    run_number = hiseq.get_run_number(run_id)
    design_xls_path = conf['casava.designs.path'] + '/design-%04d.xls' % run_number
    design_csv_path = conf['tmp.path'] + '/design-%04d.csv' % run_number

    if not os.path.exists(design_xls_path):
        error("Aozan demultiplexer: No casava design found", "No casava design found for " + run_id + " run.\n" + \
              'You must provide a design-%04d.xls file' % run_number + ' in ' + conf['casava.designs.path'] + \
              ' directory to demultiplex and create fastq files for this run.\n', conf)
        return False

    # Convert design in XLS format to CSV format
    try:
        CasavaDesignXLSToCSV.convertCasavaDesignXLSToCSV(design_xls_path, design_csv_path)
    except EoulsanException, exp:
        error("Aozan demultiplexer: error while converting design-%04d" % run_number + ".xls to CSV format", exp.getMessage(), conf)
        return False

    #fastq_output_dir = conf['fastq.data.path'] + '/' + run_id
    fastq_output_dir = conf['fastq.data.path'] + '/result_casava_%04d' % hiseq.get_run_number(run_id)
    
    if os.path.exists(fastq_output_dir):
        error("Aozan demultiplexer: fastq output directory already exists for run " + run_id,  
              'The fastq output directory already exists for run ' + run_id + ': ' + fastq_output_dir, conf)
        return False

    # Create casava makefile
    cmd = conf['casava.path'] + '/bin/configureBclToFastq.pl ' + \
          '--fastq-cluster-count ' + conf['casava.fastq.cluster.count'] + ' ' + \
          '--compression ' + conf['casava.compression'] + ' ' + \
          '--gz-level ' + conf['casava.compression.level'] + ' ' \
          '--mismatches ' + conf['casava.mismatches'] + ' ' + \
          '--input-dir ' + conf['work.data.path'] + '/' + run_id + '/Data/Intensities/BaseCalls ' + \
          '--sample-sheet ' + design_csv_path  + ' ' + \
          '--output-dir ' + fastq_output_dir
    if os.system(cmd)!=0:
        error("Aozan demultiplexer: error while creating Casava makefile for run " + run_id, 'Error while creating Casava makefile.\nCommand line:\n'+cmd, conf)
        return False

    # Get the number of cpu
    cpu_count = Runtime.getRuntime().availableProcessors() 

    # Launch casava
    cmd = "cd "+ fastq_output_dir + " & make -j " + str(cpu_count)
    if os.system(cmd)!=0:
        error("Aozan demultiplexer: error while running Casava for run " + run_id, 'Error while creating Casava makefile.\nCommand line:\n'+cmd, conf)
        return False

    # Copy design to output directory
    cmd = "cp -p " + design_csv_path + ' ' + fastq_output_dir
    if os.system(cmd)!=0:
        error("Aozan demultiplexer: error while copying design file to the fastq directory for run " + run_id, 'Error while copying design file to fastq directory.\nCommand line:\n'+cmd, conf)
        return False
    
    # The output directory must be read only
    cmd = 'chmod -R ugo-w ' + fastq_output_dir
    if os.system(cmd)!=0:
        error("Aozan demultiplexer: error while setting read only the output fastq directory for run " + run_id, 'Error while setting read only the output fastq directory.\nCommand line:\n'+cmd, conf)
        return False

    # Add design to the archive of designs
    cmd = 'zip '+ conf['casava.designs.path'] + '/designs.zip ' + design_csv_path
    if os.system(cmd)!=0:
        error("Aozan demultiplexer: error while archiving the design file for " + run_id, 'Error while archiving the design file for.\nCommand line:\n'+cmd, conf)
        return False

    duration = time.time() - start_time
    df = common.df(fastq_output_dir)
    du = common.du(fastq_output_dir)
    
    common.send_msg("[Aozan] End of demultiplexing for run " + run_id, 'End of demultiplexing for run ' + run_id + 'with no error.\nFastq files for this run ' + 
                    'can be found in the following directory:\n  ' + fastq_output_dir, conf)
    return True
