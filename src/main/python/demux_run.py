# -*- coding: utf-8 -*-

#
#                  Aozan development code
#
# This code may be freely distributed and modified under the
# terms of the GNU General Public License version 3 or later
# and CeCILL. This should be distributed with the code. If you
# do not have a copy, see:
#
#      http://www.gnu.org/licenses/gpl-3.0-standalone.html
#      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
#
# Copyright for this code is held jointly by the Genomic platform
# of the Institut de Biologie de l'École Normale Supérieure and
# the individual authors.
#
# For more information on the Aozan project and its aims,
# or to join the Aozan Google group, visit the home page at:
#
#      http://outils.genomique.biologie.ens.fr/aozan
#
#

'''
This script executes demultiplexing step.
'''

import os.path, stat, sys
import common, hiseq_run, time
import glob
import re
from xml.etree.ElementTree import ElementTree
from java.io import IOException
from java.lang import Runtime, Throwable, Exception
from java.util import HashMap

from fr.ens.biologie.genomique.aozan import AozanException
from fr.ens.biologie.genomique.aozan.util import DockerCommand
from fr.ens.biologie.genomique.aozan.illumina.samplesheet import SampleSheetUtils, \
    SampleSheetCheck

from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import TMP_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_URL_KEY
from fr.ens.biologie.genomique.aozan.Settings import DEMUX_SPACE_FACTOR_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_ADDITIONNAL_ARGUMENTS_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_COMPRESSION_LEVEL_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_MISMATCHES_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_SAMPLESHEET_FORMAT_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_SAMPLESHEETS_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_THREADS_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_WITH_FAILED_READS_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_USE_DOCKER_KEY
from fr.ens.biologie.genomique.aozan.Settings import INDEX_SEQUENCES_KEY
from fr.ens.biologie.genomique.aozan.Settings import FASTQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_SAMPLESHEET_GENERATOR_COMMAND_KEY
from fr.ens.biologie.genomique.aozan import Settings

from fr.ens.biologie.genomique.aozan.util import StringUtils
from fr.ens.biologie.genomique.aozan.illumina import RunInfo
from fr.ens.biologie.genomique.aozan.illumina.samplesheet.io import SampleSheetXLSReader
from fr.ens.biologie.genomique.aozan.illumina.samplesheet.io import SampleSheetCSVWriter
from fr.ens.biologie.genomique.aozan.illumina.samplesheet.io import SampleSheetCSVReader
from fr.ens.biologie.genomique.aozan.illumina.samplesheet.io import SampleSheetXLSXReader

def load_denied_run_ids(conf):
    """Load the list of the denied run ids.

    Arguments:
        conf: configuration dictionary
    """
    return common.load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + common.DEMUX_DENY_FILE)


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + common.DEMUX_DONE_FILE)


def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    common.add_run_id(run_id, conf[AOZAN_VAR_PATH_KEY] + '/' + common.DEMUX_DONE_FILE, conf)


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] demultiplexer: ' + short_message, message, conf[AOZAN_VAR_PATH_KEY] + '/' + common.DEMUX_LASTERR_FILE,
                 conf)


def load_index_sequences(conf):
    """Load the map of the index sequences.

    Arguments:
        index_shortcut_path: the path to the index sequences
    """

    result = HashMap()

    if not common.is_file_exists(INDEX_SEQUENCES_KEY, conf):
        return result

    f = open(conf[INDEX_SEQUENCES_KEY], 'r')

    for l in f:
        l = l[:-1]
        if len(l) == 0:
            continue
        fields = l.split('=')
        if len(fields) == 2:
            result[fields[0].strip().lower()] = fields[1].strip().upper()

    f.close()

    return result


def get_flowcell_id_in_demultiplex_xml(fastq_output_dir):
    """Get the flowcell id in DemultiplexConfig.xml.

    Arguments:
        fastq_output_dir: the path to the fastq output directory
    """

    tree = ElementTree()
    tree.parse(fastq_output_dir + '/DemultiplexConfig.xml')

    return tree.find("Flowcell").attrib['flowcell-id']


def build_samplesheet_filename(run_id, conf):
    """ Return the samplesheet filename on the run.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    run_number = hiseq_run.get_run_number(run_id)
    instrument_sn = hiseq_run.get_instrument_sn(run_id)

    return conf[BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY] + '_' + instrument_sn + '_%04d' % run_number


def load_samplesheet_using_extension(conf,samplesheet_filename,extension,input_run_data_path,run_id):
    """Get samplesheet handle

    Arguments:
        path: file on the partition
    """
    input_samplesheet_path = conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY] + '/' + samplesheet_filename + '.' + extension
    common.log("INFO", "Samplesheet path: " + str(input_samplesheet_path), conf)
    # Check if the xls samplesheet exists
    if not os.path.exists(input_samplesheet_path):

        # Test if the samplesheet exists at the root of the run directory
        input_samplesheet_csv_path = input_run_data_path + '/SampleSheet.csv'

        if not os.path.exists(input_samplesheet_csv_path):
            error("No bcl2fastq samplesheet found for run " + run_id,
                  "No bcl2fastq samplesheet found for " + run_id + " run.\n" +
                  'You must provide a ' + samplesheet_filename + '.' + extension + ' file in ' + conf[
                      BCL2FASTQ_SAMPLESHEETS_PATH_KEY] +
                  ' directory or a SampleSheet.csv file in the root of ' +
                  'the run directory to demultiplex and create FASTQ ' +
                  'files for this run.\n', conf)
            return None, None
        else:
            # Load CSV samplesheet file at the root of the run directory
            return SampleSheetCSVReader(input_samplesheet_csv_path).read(), input_samplesheet_csv_path

    else:
        # Load XLS samplesheet file
        if(extension == 'xls'):
            return SampleSheetXLSReader(input_samplesheet_path).read(), input_samplesheet_path

        elif(extension == 'xlsx'):
            return SampleSheetXLSXReader(input_samplesheet_path).read(), input_samplesheet_path

        elif(extension == 'csv'):
            return SampleSheetCSVReader(input_samplesheet_path).read(), input_samplesheet_path

        else:
            raise Exception("Can't handle " + extension + " samplesheet files. ")


def load_samplesheet(run_id, input_run_data_path, samplesheet_filename, conf):
    """ Load the samplesheet.

    Arguments:
        run id: The run id
        input_run_data_path: The input run data path
        samplesheet_filename: samplesheet filename
        conf: configuration dictionary

    Return:
        a Samplesheet object and the original path of the samplesheet
    """

    run_info_path = input_run_data_path + '/RunInfo.xml'

    if not os.path.isfile(run_info_path):
        error("no RunInfo.xml file found for run " + run_id,
              "No RunInfo.xml file found for run " + run_id + ': ' + run_info_path + '.\n', conf)
        return None, None

    run_info = RunInfo.parse(run_info_path)
    flow_cell_id = run_info.getFlowCell()

    common.log("INFO", "Flowcell id: " + flow_cell_id, conf)
    common.log("INFO", "Samplesheet format: " + str(conf[BCL2FASTQ_SAMPLESHEET_FORMAT_KEY]), conf)

    try:
        if common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'xls', conf):
            return load_samplesheet_using_extension(conf,samplesheet_filename,'xls',input_run_data_path,run_id)

        elif common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'csv', conf):
            return load_samplesheet_using_extension(conf,samplesheet_filename,'csv',input_run_data_path,run_id)

        elif common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'xlsx', conf):
            return load_samplesheet_using_extension(conf,samplesheet_filename,'xlsx',input_run_data_path,run_id)

        elif common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'command', conf):
            action_error_msg = 'Error while creating Bcl2fastq CSV samplesheet file'
            if not common.is_conf_key_exists(BCL2FASTQ_SAMPLESHEET_GENERATOR_COMMAND_KEY, conf):
                error(action_error_msg + ' for run ' + run_id, action_error_msg + ' the command is empty.', conf)
                return None, None

            input_samplesheet_generated_path = conf[TMP_PATH_KEY] + '/' + samplesheet_filename + 'generated.csv'

            cmd = conf[
                      BCL2FASTQ_SAMPLESHEET_GENERATOR_COMMAND_KEY] + ' ' + run_id + ' \'' + input_samplesheet_generated_path + '\''
            common.log("INFO", "exec: " + cmd, conf)
            if os.system(cmd) != 0:
                error(action_error_msg + ' for run ' + run_id,
                      action_error_msg + '.\nCommand line:\n' + cmd, conf)

            if not os.path.exists(input_samplesheet_generated_path):
                error(action_error_msg + ' for run ' + run_id,
                      action_error_msg + ', the external command did not create Bcl2fastq CSV file:\n' + cmd, conf)
                return None, None

            # Load CSV samplesheet file
            samplesheet = SampleSheetCSVReader(input_samplesheet_generated_path).read()

            # Remove generated samplesheet
            os.unlink(input_samplesheet_generated_path)

            return samplesheet, None

        else:
            error('Error while creating Bcl2fastq CSV samplesheet file for run ' + run_id,
                  'No method to get Bcl2fastq samplesheet file has been defined. Please, set the ' +
                  '"bcl2fastq.samplesheet.format" property.\n',
                  conf)
            return None, None

    except AozanException, exp:
        print StringUtils.stackTraceToString(exp)

        error("Error reading samplesheet: " + samplesheet_filename, exp.getMessage(), conf)
        return None, None
    except Exception, exp:
        print StringUtils.stackTraceToString(exp)

        error("Error reading samplesheet: " + samplesheet_filename, exp.getMessage(), conf)
        return None, None

    return None, None


def update_samplesheet(samplesheet, run_id, lane_count, conf):
    common.log("INFO", "Flowcell lane count: " + str(lane_count), conf)

    try:

        # Replace index sequence shortcuts by sequences
        SampleSheetUtils.replaceIndexShortcutsBySequences(samplesheet, load_index_sequences(conf))

        # Set the lane field if does not set
        SampleSheetUtils.duplicateSamplesIfLaneFieldNotSet(samplesheet, lane_count)

    except AozanException, exp:
        print StringUtils.stackTraceToString(exp)

        error("Error while updating samplesheet for run " + run_id, exp.getMessage(), conf)
        return False
    except Exception, exp:
        print StringUtils.stackTraceToString(exp)

        error("Error while updating samplesheet for run " + run_id, exp.getMessage(), conf)
        return False

    return True


def check_samplesheet(samplesheet, run_id, flow_cell_id, conf):
    samplesheet_warnings = []

    try:

        # Check values of samplesheet file
        samplesheet_warnings = SampleSheetCheck.checkSampleSheet(samplesheet, flow_cell_id)

        # TODO: remove lock

    except IOException, exp:
        error("Error while checking samplesheet for run " + run_id, exp.getMessage(), conf)
        return False, samplesheet_warnings
    except AozanException, exp:
        error("Error while checking samplesheet for run " + run_id, exp.getMessage(), conf)
        return False, samplesheet_warnings

    # Log Bcl2fastq samplesheet warning
    if samplesheet_warnings > 0:
        msg = ''
        first = True
        for warn in samplesheet_warnings:
            if first:
                first = False
            else:
                msg += ' '
            msg += warn
        common.log("WARNING", "Bcl2fastq samplesheet warnings: " + msg, conf)

    # Return samplesheet
    return True, samplesheet_warnings


def get_bcl2fastq_mismatches(samplesheet, default_value):
    list = samplesheet.getMetadata('Settings', 'MismatchCount')

    if list is None or len(list) == 0:
        return default_value

    result = default_value

    for value in list:
        try:
            result = value.strip().replace(' ', '')
        except ValueError:
            pass

    return result


def write_bcl2fastq_samplesheet(samplesheet, samplesheet_path, conf):
    try:

        # Write CSV samplesheet file in BCL2FASTQ2 format
        writer = SampleSheetCSVWriter(samplesheet_path)
        writer.setVersion(2)
        writer.writer(samplesheet)

    except AozanException, exp:
        print StringUtils.stackTraceToString(exp)

        error("Error while writing Bcl2fastq samplesheet: " + samplesheet_path, exp.getMessage(), conf)
        return False
    except Exception, exp:
        print StringUtils.stackTraceToString(exp)

        error("Error while writing Bcl2fastq samplesheet: " + samplesheet_path, exp.getMessage(), conf)
        return False

    return True


def get_cpu_count(conf):
    """ Return cpu count.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    # Get the number of cpu
    cpu_count = int(conf[BCL2FASTQ_THREADS_KEY])
    if cpu_count < 1:
        cpu_count = Runtime.getRuntime().availableProcessors()

    return cpu_count


def create_bcl2fastq_command_line(run_id, command_path, input_run_data_path, fastq_output_dir, samplesheet_csv_path,
                                  tmp_path, nb_mismatch, conf):
    nb_threads = str(get_cpu_count(conf))

    if command_path is None:
        final_command_path = 'bcl2fastq'
    else:
        final_command_path = command_path

    #  List arg
    args = []
    args.append(final_command_path)
    args.extend(['--loading-threads', nb_threads])
    args.extend(['--demultiplexing-threads', nb_threads])
    args.extend(['--processing-threads', nb_threads])
    args.extend(['--writing-threads', nb_threads])

    args.extend(['--sample-sheet', samplesheet_csv_path])
    args.extend(['--barcode-mismatches', nb_mismatch])

    # Common parameter, setting per default
    args.extend(['--input-dir', '\'' + input_run_data_path + '/Data/Intensities/BaseCalls\''])
    args.extend(['--output-dir', '\'' + fastq_output_dir + '\''])

    if common.is_conf_value_equals_true(BCL2FASTQ_WITH_FAILED_READS_KEY, conf):
        args.append('--with-failed-reads')

    # Specific parameter
    args.extend(['--runfolder-dir', '\'' + input_run_data_path + '\''])
    args.extend(['--interop-dir', '\'' + fastq_output_dir + '/InterOp\''])
    args.extend(['--min-log-level', 'TRACE'])
    # args.extend(['--stats-dir', fastq_output_dir + '/Stats'])
    # args.extend(['--reports-dir', fastq_output_dir + '/Reports'])

    # Set the compression level
    if common.is_conf_key_exists(BCL2FASTQ_COMPRESSION_LEVEL_KEY, conf):
        level_str = conf[BCL2FASTQ_COMPRESSION_LEVEL_KEY].strip()
        try:
            level_int = int(level_str)
            if level_int > 0 and level_int < 10:
                args.extend(['--fastq-compression-level', str(level_int)])
        except ValueError:
            pass

    if common.is_conf_key_exists(BCL2FASTQ_ADDITIONNAL_ARGUMENTS_KEY, conf):
        additional_args = conf[BCL2FASTQ_ADDITIONNAL_ARGUMENTS_KEY]
        additional_args = re.sub('\\s+', ' ', additional_args).strip()
        args.extends(additional_args.split(' '))

    # Retrieve output in file
    args.extend(['>', '\'' + tmp_path + '/bcl2fastq_output_' + run_id + '.out\''])
    args.extend(['2>', '\'' + tmp_path + '/bcl2fastq_output_' + run_id + '.err\''])

    return " ".join(args)


def demux_run_standalone(run_id, input_run_data_path, fastq_output_dir, samplesheet_csv_path, nb_mismatch, conf):
    """ Demultiplexing the run with bcl2fastq on version parameter.

    Arguments:
        run_id: The run id
        input_run_data_path: input run data path to demultiplexing
        fastq_output_dir: fastq directory to save result on demultiplexing
        samplesheet_csv_path: samplesheet path in csv format, version used by bcl2fastq
        conf: configuration dictionary
    """

    bcl2fastq_executable_path = conf[BCL2FASTQ_PATH_KEY]
    tmp = conf[TMP_PATH_KEY]

    run_id_msg = " for run " + run_id + ' on ' + common.get_instrument_name(run_id, conf)
    bcl2fastq_log_file = tmp + "/bcl2fastq_output_" + run_id + ".err"

    # Check if the bcl2fastq path is OK
    if os.path.isdir(bcl2fastq_executable_path):
        bcl2fastq_executable_path += '/bcl2fastq'
    elif not os.path.isfile(bcl2fastq_executable_path):
        error("Error while setting executable command file bcl2fastq" + run_id_msg + ", invalid bcl2fastq path: " +
              bcl2fastq_executable_path, "Error while setting executable command file bcl2fastq" + run_id_msg +
              ", invalid bcl2fastq path: " + bcl2fastq_executable_path, conf)
        return False

    cmd = create_bcl2fastq_command_line(run_id, bcl2fastq_executable_path, input_run_data_path, fastq_output_dir,
                                        samplesheet_csv_path, tmp, nb_mismatch, conf)

    common.log('INFO', 'Demultiplexing in standalone mode using the following command line: ' + str(cmd), conf)

    exit_code = os.system(cmd)

    if exit_code != 0:
        error("Error while executing bcl2fastq " + run_id_msg,
              'Error while executing bcl2fastq (exit code: ' + str(
                  exit_code) + ').\nCommand line:\n' + cmd, conf)

        msg = 'Error while executing bcl2fastq ' + run_id_msg + ' (exit code: ' + str(
                  exit_code) + ').\nCommand line:\n' + cmd

        # Check if the log file has been generated
        if not os.path.exists(bcl2fastq_log_file):
            error("Error with bcl2fastq log for run " + run_id + ".", "No bcl2fastq log available", conf)
            common.send_msg('[Aozan] Failed demultiplexing ' + run_id_msg, msg, True, conf)
        else:
            msg += "\n\nPlease check the attached bcl2fastq output error file."
            common.send_msg_with_attachment('[Aozan] Failed demultiplexing ' + run_id_msg, msg, bcl2fastq_log_file, True, conf)

        return False

    return True


def demux_run_with_docker(run_id, input_run_data_path, fastq_output_dir, samplesheet_csv_path, nb_mismatch, conf):
    """ Demultiplexing the run with bcl2fastq on version parameter with image Docker.

    Arguments:
        run_id: The run id
        input_run_data_path: input run data path to demultiplexing
        fastq_output_dir: fastq directory to save result on demultiplexing
        samplesheet_csv_path: samplesheet path in csv format, version used by bcl2fastq
        conf: configuration dictionary
    """

    # In docker mount with input_run_data_path
    input_docker = '/data/input'
    input_run_data_path_in_docker = input_docker
    run_id_msg = " for run " + run_id + ' on ' + common.get_instrument_name(run_id, conf)

    # In docker mount with fastq_output_dir
    output_docker = '/data/output'
    fastq_data_path_in_docker = output_docker + '/' + os.path.basename(fastq_output_dir)

    tmp = conf[TMP_PATH_KEY]
    tmp_docker = '/tmp'

    bcl2fastq_log_file = tmp + "/bcl2fastq_output_" + run_id + ".err"
    samplesheet_csv_docker = tmp_docker + '/' + os.path.basename(samplesheet_csv_path)

    cmd = create_bcl2fastq_command_line(run_id, None, input_run_data_path_in_docker, fastq_data_path_in_docker,
                                        samplesheet_csv_docker, tmp_docker, nb_mismatch, conf)

    try:
        # Set working in docker on parent demultiplexing run directory.
        # Demultiplexing run directory will create by bcl2fastq
        docker = DockerCommand(conf[Settings.DOCKER_URI_KEY], ['/bin/bash', '-c', cmd], 'bcl2fastq2', common.BCL2FASTQ2_VERSION)

        common.log("CONFIG", "Demultiplexing using docker image from " + docker.getImageDockerName() +
                   " with command line " + cmd, conf)

        common.log("CONFIG", "Bcl2fastq docker mount: " +
                   str(os.path.dirname(fastq_output_dir)) + ":" + str(output_docker) + "; " +
                   input_run_data_path + ":" + input_docker + "; " + tmp + ":" + tmp_docker, conf)

        # Mount input directory
        docker.addMountDirectory(input_run_data_path, input_docker)
        docker.addMountDirectory(os.path.dirname(fastq_output_dir), output_docker)
        docker.addMountDirectory(tmp, tmp_docker)

        docker.run()
        exit_code = docker.getExitValue()

        if exit_code != 0:
            error("Error while demultiplexing run " + run_id, 'Error while demultiplexing run (exit code: ' +
                  str(exit_code) + ').\nCommand line:\n' + cmd, conf)

            msg = 'Error while executing bcl2fastq ' + run_id_msg + ' (exit code: ' + str(
                  exit_code) + ').\nCommand line:\n' + cmd

            # Check if the log file has been generated
            if not os.path.exists(bcl2fastq_log_file):
                error("Error with bcl2fastq log for run " + run_id + ".", "No bcl2fastq log available " + bcl2fastq_log_file, conf)
                common.send_msg('[Aozan] Failed demultiplexing ' + run_id_msg, msg, True, conf)
            else:
                msg += "\n\nPlease check the attached bcl2fastq output error file."
                common.send_msg_with_attachment('[Aozan] Failed demultiplexing ' + run_id_msg, msg, bcl2fastq_log_file, True, conf)

            return False

    except Throwable, exp:
        error("Error while running Docker image", common.exception_msg(exp, conf), conf)
        return False

    return True


def check_if_output_fastq_files_exists(fastq_output_dir):
    """ Archive demultplexing statistics results file.

    Arguments:
        fastq_output_dir: fastq directory to save result on demultiplexing

        Return true if define at least on FASTQ files
    """

    fastq_files = glob.glob(fastq_output_dir + "/*fastq*")

    return len(fastq_files) > 0


def archive_demux_stat(run_id, fastq_output_dir, reports_data_path, basecall_stats_file,
                       basecall_stats_prefix, samplesheet_csv_path, conf):
    """ Archive demultplexing statistics results file.

    Arguments:
        run_id: The run id
        fastq_output_dir: fastq directory to save result on demultiplexing
        reports_data_path: directory to save archives
        basecall_stats_file: file to archive
        basecall_stats_prefix: prefix file to archive
        samplesheet_csv_path: samplesheet in csv
        conf: configuration dictionary
    """

    archive_run_dirname = basecall_stats_prefix + run_id
    archive_run_dir = reports_data_path + '/' + archive_run_dirname
    archive_run_tar_file = reports_data_path + '/' + basecall_stats_file

    # With bcl2fastq 2
    cmd_list = []
    cmd_list.extend(['cd', '\'' + fastq_output_dir + '\'', '&&'])
    cmd_list.extend(['mkdir', '\'' + archive_run_dir + '\'', '&&'])
    cmd_list.extend(['cp', '-r', 'Reports', 'Stats', 'InterOp', '\'' + samplesheet_csv_path + '\'', '\'' + archive_run_dir + '\'', '&&'])
    cmd_list.extend(['tar cjf', '\'' + archive_run_tar_file + '\'', '-C', '\'' + reports_data_path + '\'', '\'' + archive_run_dirname + '\''])

    cmd = " ".join(cmd_list)

    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("Error while saving the basecall stats files for " + run_id,
              'Error while saving the basecall stats files.\nCommand line:\n' + cmd, conf)
        return False

    # Set read only basecall stats archives files
    common.chmod(archive_run_tar_file, conf)


    return True


def archive_samplesheet(run_id, original_samplesheet_path, samplesheet_csv_path, conf):
    """ Archive samplesheet file in archive samplesheet directory.

    Arguments:
        run_id: The run id
        original_samplesheet_path: samplesheet path in format xls if exists
        samplesheet_csv_path: samplesheet path in format csv
        conf: configuration dictionary
    """

    # Add samplesheet to the archive of samplesheets
    if (common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'xls', conf) \
            and original_samplesheet_path.endswith(".xls")) or \
            (common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'xlsx', conf) \
            and original_samplesheet_path.endswith(".xlsx")):

        cmd = 'cp \'' + original_samplesheet_path + '\' \'' + conf[TMP_PATH_KEY] + \
              '\' && cd \'' + conf[TMP_PATH_KEY] + \
              '\' && zip -q \'' + conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY] + '\'/\'' + \
              conf[BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY] + 's.zip\' \'' + \
              os.path.basename(samplesheet_csv_path) + '\' \'' + os.path.basename(original_samplesheet_path) + '\''
    else:
        cmd = 'cd \'' + conf[TMP_PATH_KEY] + \
              '\' && zip -q \'' + conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY] + '\'/\'' + \
              conf[BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY] + 's.zip\' \'' + \
              os.path.basename(samplesheet_csv_path) + '\''

    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("Error while archiving the samplesheet file for " + run_id,
              'Error while archiving the samplesheet file.\nCommand line:\n' + cmd, conf)
        return False

    # Remove temporary samplesheet files
    if common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'xls', conf) or \
       common.is_conf_value_defined(BCL2FASTQ_SAMPLESHEET_FORMAT_KEY, 'xlsx', conf):
        file_to_remove = conf[TMP_PATH_KEY] + '/' + os.path.basename(original_samplesheet_path)
        if os.path.exists(file_to_remove):
            os.remove(file_to_remove)

    return True


def demux(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    start_time = time.time()
    common.log('INFO', 'Demux step: Starting', conf)

    reports_data_base_path = conf[REPORTS_DATA_PATH_KEY]
    reports_data_path = common.get_report_run_data_path(run_id, conf)

    samplesheet_filename = build_samplesheet_filename(run_id, conf)
    bcl2fastq_samplesheet_path = conf[TMP_PATH_KEY] + '/' + samplesheet_filename + '.csv'

    input_run_data_path = common.get_input_run_data_path(run_id, conf)

    if input_run_data_path is None:
        return False

    fastq_output_dir = conf[FASTQ_DATA_PATH_KEY] + '/' + run_id

    basecall_stats_prefix = 'basecall_stats_'
    basecall_stats_file = basecall_stats_prefix + run_id + '.tar.bz2'

    # Check if root input bcl data directory exists
    if not os.path.exists(input_run_data_path):
        error("Basecalling data directory does not exist",
              "Basecalling data directory does not exist: " + str(input_run_data_path), conf)
        # return False

    # Check if root input fastq data directory exists
    if not common.is_dir_exists(FASTQ_DATA_PATH_KEY, conf):
        error("FASTQ data directory does not exist",
              "FASTQ data directory does not exist: " + conf[FASTQ_DATA_PATH_KEY], conf)
        return False

    # Check if bcl2fastq samplesheets path exists
    if not common.is_dir_exists(BCL2FASTQ_SAMPLESHEETS_PATH_KEY, conf):
        error("Bcl2fastq samplesheet directory does not exist",
              "Bcl2fastq samplesheet directory does not exist: " + conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY], conf)
        return False

    # Check if bcl2fastq basedir path exists
    if not common.is_conf_value_equals_true(BCL2FASTQ_USE_DOCKER_KEY, conf):
        if not common.is_dir_exists(BCL2FASTQ_PATH_KEY, conf):
            error("Bcl2fastq directory does not exist",
                  "Bcl2fastq directory does not exist: " + conf[BCL2FASTQ_PATH_KEY], conf)
            return False

    # Check if temporary directory exists
    if not common.is_dir_exists(TMP_PATH_KEY, conf):
        error("Temporary directory does not exist",
              "Temporary directory does not exist: " + conf[TMP_PATH_KEY], conf)
        return False

    # Check if reports_data_path exists
    if not os.path.exists(reports_data_base_path):
        error("Report directory does not exist",
              "Report directory does not exist: " + reports_data_base_path, conf)
        return False

    # Create if not exist report directory for the run
    if not os.path.exists(reports_data_path):
        os.mkdir(reports_data_path)

    # Check if basecall stats archive exists
    if os.path.exists(reports_data_path + '/' + basecall_stats_file):
        error('Basecall stats archive already exists for run ' + run_id,
              'Basecall stats archive already exists for run ' + run_id + ': ' + basecall_stats_file, conf)
        return False

    # Check if the output directory already exists
    if os.path.exists(fastq_output_dir):
        error("FASTQ output directory already exists for run " + run_id,
              'FASTQ output directory already exists for run ' + run_id + ': ' + fastq_output_dir, conf)
        return False

    # Compute disk usage and disk free to check if enough disk space is available
    input_path_du = common.du(input_run_data_path)
    output_df = common.df(conf[FASTQ_DATA_PATH_KEY])
    du_factor = float(conf[DEMUX_SPACE_FACTOR_KEY])
    space_needed = input_path_du * du_factor

    common.log("WARNING", "Demux step: input disk usage: " + str(input_path_du), conf)
    common.log("WARNING", "Demux step: output disk free: " + str(output_df), conf)
    common.log("WARNING", "Demux step: space needed: " + str(space_needed), conf)

    common.log("CONFIG", "Bcl2fastq Docker mode: " + str(
        common.is_conf_value_equals_true(Settings.BCL2FASTQ_USE_DOCKER_KEY, conf)), conf)

    # Check if free space is available
    if output_df < space_needed:
        error("Not enough disk space to perform demultiplexing for run " + run_id,
              "Not enough disk space to perform demultiplexing for run " + run_id +
              '.\n%.2f Gb' % (space_needed / 1024 / 1024 / 1024) + ' is needed (factor x' + str(
                  du_factor) + ') on ' + fastq_output_dir + '.', conf)
        return False

    # Load RunInfo object
    run_info = RunInfo.parse(input_run_data_path + '/RunInfo.xml')

    # Load samplesheet
    samplesheet, original_samplesheet_path = load_samplesheet(run_id, input_run_data_path, samplesheet_filename, conf)

    if samplesheet is None:
        return False

    # Update samplesheet
    if not update_samplesheet(samplesheet, run_id, run_info.getFlowCellLaneCount(), conf):
        return False

    # Check samplesheet
    check_result, samplesheet_warnings = check_samplesheet(samplesheet, run_id, run_info.getFlowCell(), conf)
    if not check_result:
        return False

    # Get the number of mismatches
    nb_mismatch = get_bcl2fastq_mismatches(samplesheet, conf[BCL2FASTQ_MISMATCHES_KEY])

    # Write final samplesheet
    if not write_bcl2fastq_samplesheet(samplesheet, bcl2fastq_samplesheet_path, conf):
        return False

    # Run demultiplexing
    if common.is_conf_value_equals_true(Settings.BCL2FASTQ_USE_DOCKER_KEY, conf):
        # With image docker
        if not demux_run_with_docker(run_id, input_run_data_path, fastq_output_dir, bcl2fastq_samplesheet_path,
                                     nb_mismatch, conf):
            return False
    else:
        if not demux_run_standalone(run_id, input_run_data_path, fastq_output_dir, bcl2fastq_samplesheet_path,
                                    nb_mismatch, conf):
            return False

    # Check if the output directory has been created
    if not os.path.exists(fastq_output_dir):
        error("Error while demultiplexing run " + run_id + ' on ' + common.get_instrument_name(run_id, conf),
              'Error while demultiplexing run ' + run_id + '.\n' +
              'The output directory of bcl2fastq has been created: ' + fastq_output_dir, conf)
        return False

    # Check if the output directory has been created
    if os.path.isfile(fastq_output_dir):
        error("Error while demultiplexing run " + run_id + ' on ' + common.get_instrument_name(run_id, conf),
              'Error while demultiplexing run ' + run_id + '.\n' +
              'The output directory of bcl2fastq is a file instead of a directory: ' + fastq_output_dir, conf)
        return False

    # Copy bcl2fastq log to output directory
    cmd = 'cp ' + conf[TMP_PATH_KEY] + '/bcl2fastq_output_' + run_id + '.* ' + fastq_output_dir
    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("Error while copying bcl2fastq log to the output FASTQ directory" + run_id_msg,
              'Error while copying bcl2fastq log to the output FASTQ directory.\nCommand line:\n' + cmd, conf)
        return False

    # The output directory must be read only
    if not common.chmod_files_in_dir(fastq_output_dir, ".fastq", conf):
        error("Error while setting the output FASTQ directory to read only" + run_id_msg,
              'Error while setting the output FASTQ directory to read only.\nCommand line:\n' + cmd, conf)
        return False


    if not check_if_output_fastq_files_exists(fastq_output_dir):
        error("Error with bcl2fastq execution for run " + run_id,
              "Error with bcl2fastq execution for run " + run_id + " no FASTQ file found in " + fastq_output_dir,
              conf)
        return False

    # Copy samplesheet to output directory
    cmd = 'cp -p \'' + bcl2fastq_samplesheet_path + '\' \'' + fastq_output_dir + '/SampleSheet.csv\''
    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("Error while copying samplesheet file to FASTQ directory for run " + run_id,
              'Error while copying samplesheet file to FASTQ directory.\nCommand line:\n' + cmd, conf)
        return False

    # Create archives on demultiplexing statistics
    if not archive_demux_stat(run_id, fastq_output_dir, reports_data_path, basecall_stats_file,
                              basecall_stats_prefix, bcl2fastq_samplesheet_path, conf):
        return False

    # Archive samplesheet
    if not archive_samplesheet(run_id, original_samplesheet_path, bcl2fastq_samplesheet_path, conf):
        return False

    # Remove temporary samplesheet files
    if os.path.exists(bcl2fastq_samplesheet_path):
        os.remove(bcl2fastq_samplesheet_path)

    # Create index.hml file
    common.create_html_index_file(conf, run_id, [Settings.HISEQ_STEP_KEY, Settings.DEMUX_STEP_KEY])

    df_in_bytes = common.df(fastq_output_dir)
    du_in_bytes = common.du(fastq_output_dir)
    df = df_in_bytes / (1024 * 1024 * 1024)
    du = du_in_bytes / (1024 * 1024 * 1024)

    common.log("WARNING", "Demux step: output disk free after demux: " + str(df_in_bytes), conf)
    common.log("WARNING", "Demux step: space used by demux: " + str(du_in_bytes), conf)

    duration = time.time() - start_time

    msg = 'Ending demultiplexing with ' + nb_mismatch + ' mismatch(es) for run ' + run_id + '.' + \
          '\nJob finished at ' + common.time_to_human_readable(time.time()) + \
          ' without error in ' + common.duration_to_human_readable(duration) + '.\n\n' + \
          'FASTQ files for this run ' + \
          'can be found in the following directory:\n  ' + fastq_output_dir

    if samplesheet_warnings.size() > 0:
        msg += '\n\nSamplesheet warnings:'
        for warn in samplesheet_warnings:
            msg += "\n  - " + warn

    # Add path to report if reports.url exists
    if common.is_conf_key_exists(REPORTS_URL_KEY, conf):
        msg += '\n\nRun reports can be found at following location:\n  ' + conf[REPORTS_URL_KEY] + '/' + run_id

    msg += '\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df)

    common.send_msg('[Aozan] Ending demultiplexing for run ' + run_id + ' on ' +
                    common.get_instrument_name(run_id, conf), msg, False, conf)
    common.log('INFO', 'Demux step: successful in ' + common.duration_to_human_readable(duration), conf)

    return True
