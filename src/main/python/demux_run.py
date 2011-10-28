# -*- coding: utf-8 -*-

'''
Created on 25 oct. 2011

@author: Laurent Jourdren
'''
import os.path
import common, hiseq_run, time
from java.lang import Runtime
from fr.ens.transcriptome.aozan import CasavaDesignXLSToCSV
from fr.ens.transcriptome.eoulsan import EoulsanException

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


def demux(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    start_time = time.time()
    run_number = hiseq_run.get_run_number(run_id)
    design_xls_path = conf['casava.designs.path'] + '/design-%04d.xls' % run_number
    design_csv_path = conf['tmp.path'] + '/design-%04d.csv' % run_number

    #fastq_output_dir = conf['fastq.data.path'] + '/' + run_id
    fastq_output_dir = conf['fastq.data.path'] + '/result_casava_%04d' % hiseq_run.get_run_number(run_id)

    # Check if the xls design exists
    if not os.path.exists(design_xls_path):
        error("no casava design found", "No casava design found for " + run_id + " run.\n" + \
              'You must provide a design-%04d.xls file' % run_number + ' in ' + conf['casava.designs.path'] + \
              ' directory to demultiplex and create fastq files for this run.\n', conf)
        return False

    # Check if the output directory already exists
    if os.path.exists(fastq_output_dir):
        error("fastq output directory already exists for run " + run_id,
              'The fastq output directory already exists for run ' + run_id + ': ' + fastq_output_dir, conf)
        return False

    # Compute disk usage and disk free to check if enough disk space is available 
    input_path_du = common.du(conf['work.data.path'] + '/' + run_id)
    output_df = common.df(fastq_output_dir)
    du_factor = float(conf['sync.space.factor'])
    space_needed = input_path_du * du_factor

    # Check if free space is available 
    if output_df < space_needed:
        error("Not enough disk space to perform synchronization for run " + run_id, "Not enough disk space to perform synchronization for run " + run_id +
              '.\n%.2f Gb' % (space_needed / 1024 / 1024 / 1024) + ' is needed (factor x' + du_factor + ') on ' + fastq_output_dir + '.', conf)

    # Convert design in XLS format to CSV format
    try:
        CasavaDesignXLSToCSV.convertCasavaDesignXLSToCSV(design_xls_path, design_csv_path)
    except EoulsanException, exp:
        error("error while converting design-%04d" % run_number + ".xls to CSV format", exp.getMessage(), conf)
        return False

    # Create casava makefile
    cmd = conf['casava.path'] + '/bin/configureBclToFastq.pl ' + \
          '--fastq-cluster-count ' + conf['casava.fastq.cluster.count'] + ' ' + \
          '--compression ' + conf['casava.compression'] + ' ' + \
          '--gz-level ' + conf['casava.compression.level'] + ' ' \
          '--mismatches ' + conf['casava.mismatches'] + ' ' + \
          '--input-dir ' + conf['work.data.path'] + '/' + run_id + '/Data/Intensities/BaseCalls ' + \
          '--sample-sheet ' + design_csv_path + ' ' + \
          '--output-dir ' + fastq_output_dir
    if os.system(cmd) != 0:
        error("error while creating Casava makefile for run " + run_id, 'Error while creating Casava makefile.\nCommand line:\n' + cmd, conf)
        return False

    # Get the number of cpu
    cpu_count = Runtime.getRuntime().availableProcessors()

    # Launch casava
    cmd = "cd " + fastq_output_dir + " && make -j " + str(cpu_count)
    if os.system(cmd) != 0:
        error("error while running Casava for run " + run_id, 'Error while creating Casava makefile.\nCommand line:\n' + cmd, conf)
        return False

    # Copy design to output directory
    cmd = "cp -p " + design_csv_path + ' ' + fastq_output_dir
    if os.system(cmd) != 0:
        error("error while copying design file to the fastq directory for run " + run_id, 'Error while copying design file to fastq directory.\nCommand line:\n' + cmd, conf)
        return False

    # The output directory must be read only
    cmd = 'chmod -R ugo-w ' + fastq_output_dir
    if os.system(cmd) != 0:
        error("error while setting read only the output fastq directory for run " + run_id, 'Error while setting read only the output fastq directory.\nCommand line:\n' + cmd, conf)
        return False

    # Add design to the archive of designs
    cmd = 'zip ' + conf['casava.designs.path'] + '/designs.zip ' + design_csv_path
    if os.system(cmd) != 0:
        error("error while archiving the design file for " + run_id, 'Error while archiving the design file for.\nCommand line:\n' + cmd, conf)
        return False

    # Remove temporary design file
    os.remove(design_csv_path)

    duration = time.time() - start_time
    df = common.df(fastq_output_dir) / (1024 * 1024 * 1024)
    du = common.du(fastq_output_dir) / (1024 * 1024)

    common.send_msg("[Aozan] End of demultiplexing for run " + run_id, \
                    'End of demultiplexing for run ' + run_id + 'with no error in ' + str(duration) + ' seconds.\nFastq files for this run ' +
                    'can be found in the following directory:\n  ' + fastq_output_dir + \
                    '\n\n%.2f Gb has been used,' % du + ' %.2f Gb still free.' % df, conf)
    return True
